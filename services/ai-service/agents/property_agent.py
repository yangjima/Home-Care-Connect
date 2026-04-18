"""
房源智能体 - 处理房源相关的用户查询
"""
import logging
from typing import Optional

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_core.runnables import RunnableConfig

from graph.state import ChatState
from llm import get_llm
from message_text import coerce_llm_content
from tools.property_tools import search_properties, get_property_detail, book_viewing

logger = logging.getLogger(__name__)


PROPERTY_AGENT_PROMPT = """你是一个专业的房产顾问助手，帮助用户查找房源信息、预约看房。

你掌握以下能力：
1. 根据用户需求（位置、价格、面积、户型等）搜索房源
2. 提供房源详细信息（地址、租金、配置、周边配套）
3. 预约看房服务
4. 解答租房相关问题

注意事项：
- 用 Markdown 格式美化输出
- 推荐时给出具体理由
- 如果信息不足，主动询问补充条件
- 如果查询失败，给出友好提示
"""


async def property_agent(state: ChatState) -> ChatState:
    """
    房源智能体：处理房源相关查询

    支持的工具：
    - search_properties: 搜索房源列表
    - get_property_detail: 获取房源详情
    - book_viewing: 预约看房
    """
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    context = state.get("context", {})

    try:
        # 使用工具搜索相关房源信息
        search_results = await search_properties(query, user_id)

        # 构建上下文
        context = {
            "search_results": search_results,
            "query": query,
        }

        # 调用 LLM 生成推荐
        llm = get_llm()

        messages = [
            SystemMessage(content=PROPERTY_AGENT_PROMPT),
            HumanMessage(content=f"用户查询: {query}\n\n搜索到的房源信息:\n{search_results}\n\n请根据以上信息，生成专业的房源推荐回复。"),
        ]

        response = await llm.ainvoke(messages)

        return {
            **state,
            "context": context,
            "response": coerce_llm_content(getattr(response, "content", response)),
            "last_agent": "property",
        }

    except Exception as e:
        logger.error(f"房源智能体执行失败: {e}")
        return {
            **state,
            "context": {"error": str(e)},
            "response": f"抱歉，处理房源查询时遇到了问题: {str(e)}",
            "last_agent": "property_error",
        }
