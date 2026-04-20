"""
路由智能体 - 用 LLM structured output 做意图识别 + 槽位抽取

设计原则：
- 意图识别交给 LLM 做语义理解，不在代码里做关键词匹配
- 使用 Pydantic + with_structured_output 保证输出稳定、可直接消费
- 关键词兜底仅在 LLM 调用异常时启用（真实故障降级），LLM 正常返回就相信它
"""
import logging
from typing import Optional, Any, TypedDict, Literal

from langchain_core.messages import HumanMessage
from pydantic import BaseModel, Field

from llm import get_llm
from prompts.router_prompt import ROUTER_PROMPT

logger = logging.getLogger(__name__)

Intent = Literal["property", "service", "procurement", "general"]
SubAction = Literal["list", "detail", "book", "my"]


class RouterDecision(TypedDict, total=False):
    intent: Intent
    confidence: float
    sub_action: SubAction
    filters: dict[str, Any]


class RouterFilters(BaseModel):
    """路由抽取的筛选条件。所有字段都是可选的——用户没明确说就不填。"""

    keyword: Optional[str] = Field(
        default=None,
        description="用户真正要找的核心词（如商品名、房源类型）。不要把整句话塞进来。",
    )
    minPrice: Optional[float] = Field(default=None, description="价格下限（数字，元）")
    maxPrice: Optional[float] = Field(default=None, description="价格上限（数字，元）")
    district: Optional[str] = Field(default=None, description="房源行政区/片区")
    bedrooms: Optional[int] = Field(default=None, description="房源卧室数")
    id: Optional[str] = Field(default=None, description="明确某条记录的 ID")
    my: Optional[Literal["viewings", "properties"]] = Field(
        default=None, description="property 场景下查询自己的记录类型"
    )
    serviceType: Optional[str] = Field(default=None, description="服务类型（保洁/维修/开锁…）")
    date: Optional[str] = Field(default=None, description="日期 YYYY-MM-DD")
    timeRange: Optional[str] = Field(default=None, description="时间段（如'下午'）")
    category: Optional[Literal["商城", "二手"]] = Field(
        default=None, description="procurement 下细分：商城购物 or 二手交易"
    )


class RouterOutput(BaseModel):
    """路由决策的结构化输出。"""

    intent: Literal["property", "service", "procurement", "general"] = Field(
        description="最终意图分类"
    )
    confidence: float = Field(ge=0.0, le=1.0, description="意图判断置信度")
    sub_action: Literal["list", "detail", "book", "my"] = Field(
        default="list", description="子动作"
    )
    filters: RouterFilters = Field(
        default_factory=RouterFilters, description="从用户话术中抽取的筛选条件"
    )


def _keyword_fallback_route(query: str) -> Intent:
    """
    关键词兜底：**仅在 LLM 调用异常时**使用。正常流程不应进入这里。
    保持最小覆盖，仅识别最明显的场景，宁可误判为 general 也不要误判业务类型。
    """
    text = (query or "").lower()
    if any(k in text for k in ("租房", "找房", "房源", "公寓", "看房")):
        return "property"
    if any(k in text for k in ("保洁", "维修", "家政", "开锁", "陪护")):
        return "service"
    if any(k in text for k in ("采购", "买", "购物", "二手", "转让", "闲置")):
        return "procurement"
    return "general"


def _default_decision(intent: Intent, confidence: float = 0.3) -> RouterDecision:
    return {
        "intent": intent,
        "confidence": confidence,
        "sub_action": "list",
        "filters": {},
    }


def _filters_to_dict(filters: RouterFilters) -> dict[str, Any]:
    """Pydantic → dict，剔除 None 值，避免下游把 None 当真实条件。"""
    raw = filters.model_dump() if hasattr(filters, "model_dump") else filters.dict()
    return {k: v for k, v in raw.items() if v is not None}


# 各 intent 能用到的 filter 字段白名单。LLM 有时会"把所有字段都填上"——
# 这里按 intent 裁掉无关字段，作为 prompt 之外的硬性保险。
_INTENT_FILTER_WHITELIST: dict[Intent, set[str]] = {
    "property": {"keyword", "minPrice", "maxPrice", "district", "bedrooms", "id", "my"},
    "service": {"keyword", "serviceType", "date", "timeRange", "id"},
    "procurement": {"keyword", "category", "minPrice", "maxPrice", "id"},
    "general": set(),  # general 意图不带任何筛选条件
}


def _sanitize_filters(intent: Intent, filters: dict[str, Any]) -> dict[str, Any]:
    """按 intent 白名单裁剪 filters，顺带过滤掉明显无意义的值（空字符串、占位符、0）。"""
    allowed = _INTENT_FILTER_WHITELIST.get(intent, set())
    cleaned: dict[str, Any] = {}
    for k, v in (filters or {}).items():
        if k not in allowed or v is None:
            continue
        if isinstance(v, str):
            s = v.strip()
            # 剔除明显的占位/垃圾值
            if not s or s in {"[", "]", "[,", "],", ","}:
                continue
            cleaned[k] = s
        elif isinstance(v, (int, float)):
            # 价格/卧室数用 0 或负数当筛选无意义，剔除
            if v <= 0:
                continue
            cleaned[k] = v
        else:
            cleaned[k] = v
    return cleaned


def parse_route_response(content: str) -> RouterDecision:
    """
    仅用于兼容旧流程/测试——从 LLM 的纯文本响应中解析决策。
    新主流程走 structured output，不经过这里。
    """
    import json

    raw = (content or "").strip()
    lower = raw.lower()

    try:
        if "{" in raw and "}" in raw:
            json_str = raw[raw.find("{") : raw.rfind("}") + 1]
            data = json.loads(json_str)
            if isinstance(data, dict):
                intent = data.get("intent") or data.get("route")
                if intent in ("property", "service", "procurement", "general"):
                    sub_action = data.get("sub_action") or "list"
                    if sub_action not in ("list", "detail", "book", "my"):
                        sub_action = "list"
                    filters = data.get("filters") if isinstance(data.get("filters"), dict) else {}
                    conf_raw = data.get("confidence", 0.5)
                    try:
                        conf = max(0.0, min(1.0, float(conf_raw)))
                    except (TypeError, ValueError):
                        conf = 0.5
                    return {
                        "intent": intent,  # type: ignore[typeddict-item]
                        "confidence": conf,
                        "sub_action": sub_action,  # type: ignore[typeddict-item]
                        "filters": _sanitize_filters(intent, filters),  # type: ignore[arg-type]
                    }
    except Exception:
        pass

    if lower in ("property", "service", "procurement", "general"):
        return _default_decision(lower, confidence=0.5)  # type: ignore[arg-type]

    return _default_decision("general", confidence=0.3)


async def route_query(query: str, user_id: Optional[str] = None) -> RouterDecision:
    """
    分析用户查询，返回结构化路由决策（intent + filters）。

    主路径：LLM structured output（语义理解）。
    降级路径：LLM 抛异常时走关键词兜底，尽量不返回 general。
    """
    llm = get_llm()
    prompt = ROUTER_PROMPT.format(query=query, user_id=user_id or "guest")

    # 优先使用 structured output（真实 LLM）
    try:
        structured = llm.with_structured_output(RouterOutput)
    except (AttributeError, NotImplementedError):
        structured = None

    if structured is not None:
        try:
            result: RouterOutput = await structured.ainvoke([HumanMessage(content=prompt)])
            raw_filters = _filters_to_dict(result.filters)
            return {
                "intent": result.intent,
                "confidence": float(result.confidence),
                "sub_action": result.sub_action,
                "filters": _sanitize_filters(result.intent, raw_filters),
            }
        except Exception as e:
            logger.warning(f"structured output 调用失败，降级到纯文本解析: {e}")

    # 降级路径 1：LLM 可用但不支持 structured output（例如 MockLLM）
    try:
        response = await llm.ainvoke([HumanMessage(content=prompt)])
        content = (getattr(response, "content", "") or "").strip()
        return parse_route_response(content)
    except Exception:
        # 降级路径 2：LLM 完全不可用 —— 关键词兜底
        logger.exception("LLM 路由调用失败，启用关键词兜底")
        intent = _keyword_fallback_route(query)
        return _default_decision(intent, confidence=0.35 if intent != "general" else 0.25)
