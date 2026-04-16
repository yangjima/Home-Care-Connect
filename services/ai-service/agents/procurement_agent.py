"""
采购与二手智能体 - 处理采购和二手交易相关查询
"""
import logging
from typing import Optional

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import ChatState
from llm import get_llm
from tools.procurement_tools import search_procurement_products, search_secondhand_items, create_procurement_order

logger = logging.getLogger(__name__)


PROCUREMENT_AGENT_PROMPT = """你是一个专业的采购顾问，帮助用户查找办公用品采购信息和二手交易物品。

你掌握以下能力：
1. 搜索办公用品采购信息（办公设备、耗材、家具等）
2. 搜索二手物品交易信息
3. 提供物品详情和价格
4. 引导用户下单

注意事项：
- 用 Markdown 格式美化输出
- 标明新旧程度和价格
- 如果信息不足，主动询问
"""


async def procurement_agent(state: ChatState) -> ChatState:
    """
    采购与二手智能体：处理采购和二手交易查询
    """
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    context = state.get("context", {})

    try:
        # 判断是采购还是二手查询
        if "二手" in query or "转让" in query or "闲置" in query:
            results = await search_secondhand_items(query, user_id)
        else:
            results = await search_procurement_products(query, user_id)

        context = {
            "procurement_results": results,
            "query": query,
        }

        llm = get_llm()

        messages = [
            SystemMessage(content=PROCUREMENT_AGENT_PROMPT),
            HumanMessage(content=f"用户查询: {query}\n\n搜索到的采购/二手信息:\n{results}\n\n请根据以上信息，生成专业的采购咨询回复。"),
        ]

        response = await llm.ainvoke(messages)

        return {
            **state,
            "context": context,
            "response": response.content,
            "last_agent": "procurement",
        }

    except Exception as e:
        logger.error(f"采购智能体执行失败: {e}")
        return {
            **state,
            "context": {"error": str(e)},
            "response": f"抱歉，处理采购查询时遇到了问题: {str(e)}",
            "last_agent": "procurement_error",
        }
