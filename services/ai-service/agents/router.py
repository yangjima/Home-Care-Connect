"""
路由智能体 - 分析用户查询，决定应该由哪个专业 Agent 处理
"""
import logging
import json
from typing import Optional

from langchain_core.messages import HumanMessage, AIMessage
from langchain_core.outputs import ChatGeneration
from langchain_core.runnables import RunnableConfig

from llm import get_llm
from config import settings
from prompts.router_prompt import ROUTER_PROMPT

logger = logging.getLogger(__name__)


async def route_query(query: str, user_id: Optional[str] = None) -> str:
    """
    分析用户查询，返回路由决策

    返回值:
        - property: 房源相关（找房、租房、买房、看房预约等）
        - service: 社区服务相关（家政、维修、护理、保洁等）
        - procurement: 采购/二手相关（办公用品采购、二手交易等）
        - general: 通用问答
    """
    try:
        llm = get_llm()

        messages = [
            HumanMessage(content=ROUTER_PROMPT.format(query=query, user_id=user_id or "guest"))
        ]

        response = await llm.ainvoke(messages)

        content = response.content.strip().lower()

        # 解析 LLM 返回的路由决策
        if "property" in content:
            return "property"
        elif "service" in content:
            return "service"
        elif "procurement" in content:
            return "procurement"
        else:
            return "general"

    except Exception as e:
        logger.error(f"路由分析失败: {e}")
        return "general"


def parse_route_response(content: str) -> str:
    """从 LLM 响应中解析路由决策"""
    content = content.lower()

    # 尝试提取 JSON
    try:
        if "{" in content:
            json_str = content[content.find("{"):content.rfind("}") + 1]
            data = json.loads(json_str)
            return data.get("route", "general")
    except json.JSONDecodeError:
        pass

    # 关键词匹配
    if "property" in content or "房源" in content or "租房" in content or "买房" in content:
        return "property"
    elif "service" in content or "服务" in content or "家政" in content or "维修" in content:
        return "service"
    elif "procurement" in content or "采购" in content or "二手" in content:
        return "procurement"

    return "general"
