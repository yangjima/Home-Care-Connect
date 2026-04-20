"""
社区服务智能体 - 处理服务订单相关查询
"""
import logging

from langchain_core.messages import HumanMessage, SystemMessage

from graph.state import ChatState
from llm import get_llm
from message_text import coerce_llm_content
from tools.service_tools import search_services

logger = logging.getLogger(__name__)


SERVICE_AGENT_PROMPT = """你是居服通平台的社区服务顾问。

## 工作原则（必须遵守）

1. **只能基于下面提供的"服务信息"回复**。严禁编造平台上没有的服务类目、价格。
2. 搜索结果以 `EMPTY_RESULT:` 开头时，说明平台当前没有匹配的服务：
   - 明确告诉用户"暂无此类服务"
   - 可列出平台上常见的服务大类作为替代参考（仅凭平台实际存在的服务，不要虚构）
3. 正常列表时，简要介绍、给出价格与预约方式。
4. 信息不够具体（服务地址、时间）时可以追问。

## 回复格式

- Markdown，可用 emoji
- 列出价格与预约方式
- 结尾引导下一步（下单 / 选择具体服务）
"""


async def service_agent(state: ChatState) -> ChatState:
    """社区服务智能体：处理服务相关查询。"""
    query = state.get("query", state["messages"][-1].content)
    user_id = state.get("user_id")
    filters = state.get("filters") or {}

    try:
        service_results = await search_services(query, user_id, filters=filters)

        context = {
            "service_results": service_results,
            "query": query,
            "filters": filters,
        }

        llm = get_llm()

        messages = [
            SystemMessage(content=SERVICE_AGENT_PROMPT),
            HumanMessage(
                content=(
                    f"用户原话：{query}\n"
                    f"解析出的筛选条件：{filters or '(无)'}\n\n"
                    f"平台服务信息：\n{service_results}\n\n"
                    f"请严格基于以上信息回复。如果是 EMPTY_RESULT，如实告知，不要编造任何服务。"
                )
            ),
        ]

        response = await llm.ainvoke(messages)

        return {
            **state,
            "context": context,
            "response": coerce_llm_content(getattr(response, "content", response)),
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
