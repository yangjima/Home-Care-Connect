"""
社区服务智能体 - 处理服务订单相关查询
"""
import logging
from typing import Optional

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import ChatState
from llm import get_llm
from tools.service_tools import search_services, create_service_order, get_service_types

logger = logging.getLogger(__name__)


SERVICE_AGENT_PROMPT = """你是一个专业的社区服务顾问，帮助用户了解和预约各类社区服务。

你掌握以下能力：
1. 介绍各类服务（家政、维修、护理、保洁、搬家等）
2. 推荐合适的服务类型
3. 引导用户下单预约
4. 解答服务相关问题

注意事项：
- 用 Markdown 格式美化输出
- 列出价格和预约方式
- 如果需要更多信息，主动询问
- 如果查询失败，给出友好提示
"""


async def service_agent(state: ChatState) -> ChatState:
    """
    社区服务智能体：处理服务相关查询
    """
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    context = state.get("context", {})

    try:
        # 搜索相关服务
        service_results = await search_services(query, user_id)

        context = {
            "service_results": service_results,
            "query": query,
        }

        llm = get_llm()

        messages = [
            SystemMessage(content=SERVICE_AGENT_PROMPT),
            HumanMessage(content=f"用户查询: {query}\n\n搜索到的服务信息:\n{service_results}\n\n请根据以上信息，生成专业的服务推荐回复。"),
        ]

        response = await llm.ainvoke(messages)

        return {
            **state,
            "context": context,
            "response": response.content,
            "last_agent": "service",
        }

    except Exception as e:
        logger.error(f"服务智能体执行失败: {e}")
        return {
            **state,
            "context": {"error": str(e)},
            "response": f"抱歉，处理服务查询时遇到了问题: {str(e)}",
            "last_agent": "service_error",
        }
