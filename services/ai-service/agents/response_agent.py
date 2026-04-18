"""
通用问答智能体 - 处理不属于特定领域的通用查询
"""
import logging

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import ChatState
from llm import get_llm
from message_text import coerce_llm_content

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


def _local_fallback_reply(query: str, route: str) -> str:
    """当 LLM 调用失败时，返回可直接使用的本地兜底回复。"""
    if route == "property":
        return (
            "我可以帮您找房。请告诉我这几个条件：\n"
            "1. 预算区间（如 1500-2500 元）\n"
            "2. 位置（如地铁口/小区名）\n"
            "3. 户型和面积（如一室一厅 40㎡）\n"
            "4. 入住时间\n"
            "您也可以直接说：`月租 2000 以下，两室，近地铁`。"
        )
    if route == "service":
        return (
            "我可以帮您预约社区服务。请告诉我：\n"
            "1. 服务类型（保洁/维修/开锁等）\n"
            "2. 服务地址\n"
            "3. 期望上门时间\n"
            "4. 问题描述\n"
            "例如：`明天下午 3 点，xx 小区 2 栋，空调不制冷`。"
        )
    if route == "procurement":
        return (
            "我可以帮您查采购商品和二手信息。请告诉我：\n"
            "1. 商品类别（家具/家电/日用品）\n"
            "2. 预算\n"
            "3. 是否接受二手\n"
            "例如：`想买 500 元以内的办公椅，可接受二手`。"
        )
    return (
        "您好，我是居服通 AI 助手。当前智能模型暂时不可用，但我仍可协助您：\n"
        "- 找房源（预算/位置/户型）\n"
        "- 约服务（保洁/维修/开锁）\n"
        "- 查采购/二手商品\n"
        "请直接告诉我您的具体需求。"
    )


async def response_agent(state: ChatState) -> ChatState:
    """
    通用问答智能体：处理通用查询和最终响应生成
    """
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    route = state.get("route", "general")
    context = state.get("context", {})
    previous_response = state.get("response")
    last_agent = state.get("last_agent", "")

    # 当已经由专业 Agent 生成了有效回答时，避免被通用回答覆盖。
    if route != "general" and previous_response and last_agent in {"property", "service", "procurement"}:
        return {
            **state,
            "response": previous_response,
        }

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

        context_text = ""
        if context:
            context_text = f"\n业务上下文：{context}"

        messages = [
            SystemMessage(content=system_prompt),
            HumanMessage(content=f"用户 ID: {user_id or '游客'}\n用户消息: {query}{context_text}"),
        ]

        response = await llm.ainvoke(messages)
        text = coerce_llm_content(getattr(response, "content", response))

        return {
            **state,
            "response": text,
            "last_agent": "general",
        }

    except Exception:
        logger.exception("通用智能体执行失败，启用本地兜底回复")
        return {
            **state,
            "response": _local_fallback_reply(query, route),
            "last_agent": "general_fallback",
        }
