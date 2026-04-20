"""
房源智能体 - 处理房源相关的用户查询
"""
import logging

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import ChatState
from llm import get_llm
from message_text import coerce_llm_content
from tools.property_tools import search_properties

logger = logging.getLogger(__name__)


PROPERTY_AGENT_PROMPT = """你是居服通平台的房产顾问助手，服务于平台注册的房源。

## 工作原则（重要，必须遵守）

1. **只能基于下面提供的"搜索结果"回答用户**。搜索结果之外的房源你完全不知道，**严禁编造、联想或推荐任何不在结果中的房源**（比如北京/上海/其他城市的房源、虚构的小区名）。
2. 如果搜索结果以 `EMPTY_RESULT:` 开头，说明平台当前没有匹配的房源：
   - **明确告诉用户"当前没有符合您条件的房源"**
   - 建议用户**放宽条件**（扩大价格区间、更换片区、调整户型），或提供更多线索
   - **不要**转而推荐其他房源来凑数
3. 搜索结果为正常列表时，可以结合用户原始诉求做简要点评和推荐理由。
4. 如果用户的需求还不够具体（例如没说预算、没说地段），可以主动追问，但不要替用户"假设"条件。

## 回复格式

- 使用 Markdown，必要时用 emoji 增加亲和力
- 控制在简洁有信息量的长度
- 结尾可引导下一步动作（查看详情 / 预约看房 / 调整条件）
"""


async def property_agent(state: ChatState) -> ChatState:
    """房源智能体：处理房源相关查询。"""
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    filters = state.get("filters") or {}

    try:
        search_results = await search_properties(query, user_id, filters=filters)

        context = {
            "search_results": search_results,
            "query": query,
            "filters": filters,
        }

        llm = get_llm()

        messages = [
            SystemMessage(content=PROPERTY_AGENT_PROMPT),
            HumanMessage(
                content=(
                    f"用户原话：{query}\n"
                    f"解析出的筛选条件：{filters or '(无)'}\n\n"
                    f"平台搜索结果：\n{search_results}\n\n"
                    f"请严格基于以上搜索结果回复用户。如果是 EMPTY_RESULT，如实告知并建议放宽条件，不要编造任何房源。"
                )
            ),
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
