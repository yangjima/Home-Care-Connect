"""
通用问答智能体 - 处理不属于特定领域的通用查询
"""
import logging

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import ChatState
from llm import get_llm

logger = logging.getLogger(__name__)


GENERAL_AGENT_PROMPT = """你是一个友好、专业、热情的 AI 助手，为"居服通"(Home Care Connect)社区服务平台的用户提供帮助。

平台功能简介：
1. **房源服务**：提供租房、买房、房屋托管等房源相关信息
2. **社区服务**：家政、维修、护理、保洁、搬家等生活服务
3. **采购服务**：办公用品采购、二手物品交易
4. **资产管理**：房屋设施管理、维修记录

回复要求：
- 用 Markdown 格式美化输出，使用 emoji 增加亲和力
- 回答要专业、准确、易懂
- 主动引导用户使用平台功能
- 如果不确定，引导用户联系人工客服
- 保持友好和耐心
- 回答简洁但有信息量
"""


async def response_agent(state: ChatState) -> ChatState:
    """
    通用问答智能体：处理通用查询和最终响应生成
    """
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    route = state.get("route", "general")
    context = state.get("context", {})

    try:
        llm = get_llm()

        system_prompt = GENERAL_AGENT_PROMPT

        # 根据路由添加特定提示
        if route == "property":
            system_prompt += "\n\n注意：用户可能是在询问房源相关的问题，请引导用户使用房源搜索功能。"
        elif route == "service":
            system_prompt += "\n\n注意：用户可能是在询问社区服务，请引导用户使用服务预约功能。"
        elif route == "procurement":
            system_prompt += "\n\n注意：用户可能是在询问采购或二手交易，请引导用户使用相应功能。"

        messages = [
            SystemMessage(content=system_prompt),
            HumanMessage(content=f"用户 ID: {user_id or '游客'}\n用户消息: {query}"),
        ]

        response = await llm.ainvoke(messages)

        return {
            **state,
            "response": response.content,
            "last_agent": "general",
        }

    except Exception as e:
        logger.error(f"通用智能体执行失败: {e}")
        return {
            **state,
            "response": f"抱歉，我遇到了一些问题。请稍后再试，或者联系我们的客服获取帮助。",
            "last_agent": "general_error",
        }
