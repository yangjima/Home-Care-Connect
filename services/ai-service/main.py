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
from agents.redirect import build_redirect
from config import settings
from langchain_core.messages import HumanMessage

from message_text import coerce_llm_content

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
    confidence = float(result.get("confidence", 0.5) or 0.5)
    sub_action = str(result.get("sub_action", "list") or "list")
    filters = result.get("filters", {}) or {}
    reply = result.get("response", "")
    if not reply:
        last_msg = result["messages"][-1]
        raw = getattr(last_msg, "content", None)
        reply = coerce_llm_content(raw) if raw is not None else str(last_msg)
    else:
        reply = coerce_llm_content(reply)

    redirect = build_redirect(intent=intent, sub_action=sub_action, filters=filters)

    return {
        "intent": intent,
        "confidence": confidence,
        "sub_action": sub_action,
        "filters": filters,
        "reply": reply,
        "data": result.get("context", {}),
        "redirect": redirect,
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

            # 先跑完整图拿到结构化结果，再按 token 流式吐出（对齐设计文档事件模型）
            result = await graph.ainvoke(
                {
                    "messages": [HumanMessage(content=user_message)],
                    "session_id": session_id,
                    "user_id": user_id,
                },
                config={"configurable": {"thread_id": session_id}},
            )

            intent = result.get("route", "general")
            confidence = float(result.get("confidence", 0.5) or 0.5)
            sub_action = str(result.get("sub_action", "list") or "list")
            filters = result.get("filters", {}) or {}
            redirect = build_redirect(intent=intent, sub_action=sub_action, filters=filters)

            response_content = result.get("response", "")
            if not response_content:
                last_msg = result["messages"][-1]
                if hasattr(last_msg, "content"):
                    response_content = coerce_llm_content(last_msg.content)
                elif isinstance(last_msg, (tuple, list)) and len(last_msg) >= 2:
                    response_content = coerce_llm_content(last_msg[1])
                else:
                    response_content = str(last_msg)
            else:
                response_content = coerce_llm_content(response_content)

            await manager.send_event(session_id, {"type": "start", "message": user_message})

            accumulated = ""
            for char in response_content:
                accumulated += char
                await manager.send_event(session_id, {"type": "token", "content": char})

            await manager.send_event(
                session_id,
                {
                    "type": "intent",
                    "data": {
                        "intent": intent,
                        "confidence": confidence,
                        "sub_action": sub_action,
                        "filters": filters,
                    },
                },
            )

            await manager.send_event(
                session_id,
                {
                    "type": "result",
                    "data": {
                        "redirect": redirect,
                        "filters": filters,
                    },
                },
            )

            await manager.send_event(session_id, {"type": "end", "content": accumulated})

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
    """保留旧函数签名（历史兼容）。当前 websocket 路径不再使用。"""
    try:
        result = await graph.ainvoke(
            {
                "messages": [HumanMessage(content=user_message)],
                "session_id": session_id,
                "user_id": user_id,
            },
            config={"configurable": {"thread_id": session_id}},
        )

        response_content = result.get("response", "") or ""
        response_content = coerce_llm_content(response_content)
        for char in response_content:
            yield {"type": "token", "content": char}
    except Exception as e:
        logger.error(f"图执行失败: {e}")
        yield {"type": "token", "content": f"抱歉，处理您的请求时出现了问题: {str(e)}"}


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
