"""
居服通 AI 服务 - LangGraph 多智能体编排系统
"""
import os
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Query
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import json
import logging
from typing import AsyncGenerator
from pydantic import BaseModel

from graph.chat_graph import create_chat_graph, ChatState
from agents.router import route_query
from config import settings
from langchain_core.messages import HumanMessage

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    logger.info("居服通 AI 服务启动中...")
    logger.info(f"LLM 配置: {settings.DASHSCOPE_BASE_URL}")
    yield
    logger.info("居服通 AI 服务关闭中...")


app = FastAPI(
    title="居服通 AI 服务",
    description="基于 LangGraph 的多智能体 AI 对话服务，为 Home Care Connect 提供智能问答、房源推荐、服务推荐、采购咨询等功能",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 存储活跃的 WebSocket 连接
class ConnectionManager:
    def __init__(self):
        self.active_connections: dict[str, WebSocket] = {}

    async def connect(self, session_id: str, websocket: WebSocket):
        await websocket.accept()
        self.active_connections[session_id] = websocket

    def disconnect(self, session_id: str):
        self.active_connections.pop(session_id, None)

    async def send_text(self, session_id: str, text: str):
        if session_id in self.active_connections:
            await self.active_connections[session_id].send_text(text)

    async def send_event(self, session_id: str, event: dict):
        if session_id in self.active_connections:
            await self.active_connections[session_id].send_json(event)


manager = ConnectionManager()


class ChatRequest(BaseModel):
    message: str
    session_id: str = "default"


@app.get("/health")
async def health_check():
    """健康检查接口"""
    return {"status": "ok", "service": "ai-service"}


def _resolve_redirect(intent: str) -> str | None:
    mapping = {
        "property": "/properties",
        "service": "/services",
        "procurement": "/purchase",
    }
    return mapping.get(intent)


async def _invoke_chat_graph(message: str, session_id: str, user_id=None) -> dict:
    graph = create_chat_graph()
    result = await graph.ainvoke(
        {
            "messages": [HumanMessage(content=message)],
            "session_id": session_id,
            "user_id": user_id,
        },
        config={"configurable": {"thread_id": session_id}},
    )
    intent = result.get("route", "general")
    reply = result.get("response", "")
    if not reply:
        last_msg = result["messages"][-1]
        reply = getattr(last_msg, "content", str(last_msg))

    return {
        "intent": intent,
        "reply": reply,
        "data": result.get("context", {}),
        "redirect": _resolve_redirect(intent),
        "session_id": session_id,
    }


@app.get("/api/ai/chat")
async def chat_get(
    message: str = Query(..., description="用户消息"),
    session_id: str = Query(default="default", description="会话 ID"),
):
    """HTTP GET 方式对话接口（兼容旧版调用）"""
    try:
        payload = await _invoke_chat_graph(message, session_id)
        return {"code": 200, "message": "success", "data": payload}
    except Exception as e:
        logger.error(f"对话处理失败: {e}")
        return {"code": 500, "message": str(e), "data": None}


@app.post("/api/ai/chat")
async def chat_post(req: ChatRequest):
    """HTTP POST 方式对话接口（设计文档标准接口）"""
    try:
        payload = await _invoke_chat_graph(req.message, req.session_id)
        return {"code": 200, "message": "success", "data": payload}
    except Exception as e:
        logger.error(f"对话处理失败: {e}")
        return {"code": 500, "message": str(e), "data": None}


@app.websocket("/api/ai/ws/chat")
async def websocket_chat(websocket: WebSocket, session_id: str = "default"):
    """
    WebSocket 流式对话接口
    支持实时流式输出，支持多轮对话上下文
    """
    await manager.connect(session_id, websocket)
    try:
        graph = create_chat_graph()

        async for event in websocket.iter_text():
            data = json.loads(event)
            user_message = data.get("message", "")
            user_id = data.get("user_id")

            await manager.send_event(session_id, {
                "type": "start",
                "message": user_message,
            })

            # 流式处理
            accumulated_response = ""

            # 第一步：路由
            route = await route_query(user_message, user_id)
            await manager.send_event(session_id, {
                "type": "route",
                "agent": route,
            })

            # 调用图
            async for chunk in stream_graph_response(graph, user_message, session_id, user_id):
                if chunk["type"] == "token":
                    accumulated_response += chunk["content"]
                    await manager.send_event(session_id, {
                        "type": "token",
                        "content": chunk["content"],
                    })
                elif chunk["type"] == "agent":
                    await manager.send_event(session_id, {
                        "type": "agent",
                        "agent": chunk["agent"],
                    })

            await manager.send_event(session_id, {
                "type": "end",
                "content": accumulated_response,
            })

    except WebSocketDisconnect:
        manager.disconnect(session_id)
        logger.info(f"WebSocket 连接断开: {session_id}")
    except Exception as e:
        logger.error(f"WebSocket 错误: {e}")
        await manager.send_event(session_id, {
            "type": "error",
            "message": str(e),
        })
    finally:
        manager.disconnect(session_id)


async def stream_graph_response(graph, user_message: str, session_id: str, user_id=None) -> AsyncGenerator[dict, None]:
    """流式执行图并yield每个 token"""
    try:
        result = await graph.ainvoke(
            {
                "messages": [HumanMessage(content=user_message)],
                "session_id": session_id,
                "user_id": user_id,
            },
            config={"configurable": {"thread_id": session_id}},
        )

        response_content = result.get("response", "")
        if not response_content:
            last_msg = result["messages"][-1]
            if hasattr(last_msg, "content"):
                response_content = last_msg.content
            elif isinstance(last_msg, (tuple, list)) and len(last_msg) >= 2:
                response_content = last_msg[1]
            else:
                response_content = str(last_msg)
        last_agent = result.get("last_agent", "response")

        # 模拟流式输出（逐字发送）
        for char in response_content:
            yield {"type": "token", "content": char}
            import asyncio
            await asyncio.sleep(0.01)  # 控制打字速度

        yield {"type": "agent", "agent": last_agent}

    except Exception as e:
        logger.error(f"图执行失败: {e}")
        yield {"type": "token", "content": f"抱歉，处理您的请求时出现了问题: {str(e)}"}
        yield {"type": "agent", "agent": "error"}


@app.get("/api/ai/sessions/{session_id}/history")
async def get_session_history(session_id: str):
    """获取指定会话的历史记录"""
    try:
        graph = create_chat_graph()
        from langgraph.checkpoint.memory import MemorySaver
        from langgraph.checkpoint.base import BaseCheckpointSaver

        # 获取检查点中的历史消息
        checkpoint_config = {"configurable": {"thread_id": session_id}}
        try:
            checkpoint = await graph.aget_state(checkpoint_config)
            if checkpoint and checkpoint.values:
                messages = checkpoint.values.get("messages", [])
                history = []
                for msg in messages:
                    role = "user" if msg.type == "human" else "assistant"
                    history.append({"role": role, "content": msg.content})
                return {"code": 200, "data": history}
        except Exception:
            pass

        return {"code": 200, "data": []}
    except Exception as e:
        return {"code": 500, "message": str(e), "data": []}


@app.delete("/api/ai/sessions/{session_id}")
async def clear_session(session_id: str):
    """清除指定会话的历史"""
    try:
        graph = create_chat_graph()
        await graph.adelete_state(config={"configurable": {"thread_id": session_id}})
        return {"code": 200, "message": "会话已清除"}
    except Exception as e:
        return {"code": 500, "message": str(e)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=True,
    )
