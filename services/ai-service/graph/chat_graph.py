"""
LangGraph 聊天图定义
"""
import logging
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import START

from graph.state import ChatState
from agents.router import route_query
from agents.property_agent import property_agent
from agents.service_agent import service_agent
from agents.procurement_agent import procurement_agent
from agents.response_agent import response_agent
from config import settings

logger = logging.getLogger(__name__)


def create_checkpoint_saver():
    """创建检查点持久化器"""
    if settings.USE_REDIS_CHECKPOINT and settings.dashscope_configured:
        try:
            # 某些 langgraph 版本不包含 redis checkpointer；运行时探测并降级
            from langgraph.checkpoint.redis import RedisSaver  # type: ignore

            redis_saver = RedisSaver(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                password=settings.REDIS_PASSWORD,
                db=settings.REDIS_DB,
            )
            logger.info("使用 Redis 检查点持久化")
            return redis_saver
        except ImportError as e:
            logger.warning(f"当前 langgraph 版本不支持 Redis 检查点，使用内存检查点: {e}")
        except Exception as e:
            logger.warning(f"Redis 检查点初始化失败，使用内存检查点: {e}")
    return MemorySaver()


def create_chat_graph():
    """创建聊天图"""
    workflow = StateGraph(ChatState)

    # 添加节点
    workflow.add_node("router", router_node)
    workflow.add_node("property", property_agent)
    workflow.add_node("service", service_agent)
    workflow.add_node("procurement", procurement_agent)
    workflow.add_node("response", response_agent)

    # 设置入口点
    workflow.add_edge(START, "router")

    # 路由条件边
    workflow.add_conditional_edges(
        "router",
        route_decision,
        {
            "property": "property",
            "service": "service",
            "procurement": "procurement",
            "general": "response",
        }
    )

    # 所有 Agent 都连接到 response
    workflow.add_edge("property", "response")
    workflow.add_edge("service", "response")
    workflow.add_edge("procurement", "response")
    workflow.add_edge("response", END)

    # 编译图
    checkpointer = create_checkpoint_saver()
    graph = workflow.compile(checkpointer=checkpointer)

    return graph


async def router_node(state: ChatState) -> ChatState:
    """路由节点"""
    messages = state["messages"]
    user_message = messages[-1].content
    user_id = state.get("user_id")

    route = await route_query(user_message, user_id)

    return {
        **state,
        "route": route,
        "query": user_message,
        "context": {},
    }


def route_decision(state: ChatState) -> str:
    """路由决策函数"""
    route = state.get("route", "general")
    return route
