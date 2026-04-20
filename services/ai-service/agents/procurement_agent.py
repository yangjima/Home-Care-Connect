"""
采购与二手智能体 - 处理购物和二手交易相关查询

路由分流完全依赖 router 抽取的 filters.category（"商城" / "二手"），
不再用字符串 in 匹配。
"""
import logging

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import ChatState
from llm import get_llm
from message_text import coerce_llm_content
from tools.procurement_tools import search_procurement_products, search_secondhand_items

logger = logging.getLogger(__name__)


PROCUREMENT_AGENT_PROMPT = """你是居服通平台的购物顾问，帮用户在平台商城或二手交易区找商品。

## 工作原则（必须遵守）

1. **只能基于下面提供的"搜索结果"回答**。严禁编造平台之外的商品、价格、店铺、品牌型号。
2. 搜索结果以 `EMPTY_RESULT:` 开头时，说明平台当前没有符合条件的商品：
   - **明确告诉用户"平台商城/二手区暂时没有您想要的商品"**
   - 可以建议用户：尝试更宽泛的关键词、查看相关品类、或留意后续上新
   - **不要**转推荐不相关的商品来凑数（例如用户要可乐，不要去推荐办公用品）
3. 搜索结果为正常列表时，按用户诉求简要点评、给出购买建议，并标明价格与成色（二手）。
4. 如果条件还不够清晰（品类、预算、新旧），可以追问。

## 回复格式

- Markdown，可用 emoji
- 简洁有信息量
- 结尾可引导下一步（查看详情 / 下单 / 调整关键词）
"""


async def procurement_agent(state: ChatState) -> ChatState:
    """采购与二手智能体：按 router 的 category 决定走商城还是二手。"""
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    filters = state.get("filters") or {}
    category = filters.get("category")  # "商城" | "二手" | None

    try:
        if category == "二手":
            results = await search_secondhand_items(query, user_id, filters=filters)
            channel = "二手交易区"
        else:
            # 未明确分类时默认走商城购物——这是更常见的意图
            results = await search_procurement_products(query, user_id, filters=filters)
            channel = "商城"

        context = {
            "procurement_results": results,
            "query": query,
            "filters": filters,
            "channel": channel,
        }

        llm = get_llm()

        messages = [
            SystemMessage(content=PROCUREMENT_AGENT_PROMPT),
            HumanMessage(
                content=(
                    f"用户原话：{query}\n"
                    f"检索渠道：{channel}\n"
                    f"解析出的筛选条件：{filters or '(无)'}\n\n"
                    f"平台搜索结果：\n{results}\n\n"
                    f"请严格基于以上搜索结果回复。如果是 EMPTY_RESULT，如实告知并建议换个关键词或稍后再来，不要编造任何商品。"
                )
            ),
        ]

        response = await llm.ainvoke(messages)

        return {
            **state,
            "context": context,
            "response": coerce_llm_content(getattr(response, "content", response)),
            "last_agent": "procurement",
        }

    except Exception as e:
        logger.error(f"采购智能体执行失败: {e}")
        return {
            **state,
            "context": {"error": str(e)},
            "response": f"抱歉，处理购物查询时遇到了问题: {str(e)}",
            "last_agent": "procurement_error",
        }
