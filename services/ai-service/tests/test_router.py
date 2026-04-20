"""
路由器智能体单元测试

测试策略：stub 掉 LLM 层（structured output），直接验证 router 对结构化结果的包装，
以及 LLM 异常时的关键词降级路径。不依赖真实 LLM 或 MockLLM 的自然语言输出。
"""
import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from agents.router import route_query, parse_route_response, RouterOutput, RouterFilters


def _make_structured_llm(output: RouterOutput):
    """构造一个"支持 structured output"的假 LLM。"""
    structured = MagicMock()
    structured.ainvoke = AsyncMock(return_value=output)

    llm = MagicMock()
    llm.with_structured_output = MagicMock(return_value=structured)
    return llm


class TestRouterStructuredOutput:
    """验证 structured output 主路径。LLM 返什么，router 就如实返回。"""

    @pytest.mark.asyncio
    async def test_route_property_with_price(self):
        """"租两千左右的房" → property + minPrice/maxPrice"""
        output = RouterOutput(
            intent="property",
            confidence=0.95,
            sub_action="list",
            filters=RouterFilters(minPrice=1500, maxPrice=2500),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(output)):
            result = await route_query("租两千左右的房子", "user123")

        assert result["intent"] == "property"
        assert result["filters"]["minPrice"] == 1500
        assert result["filters"]["maxPrice"] == 2500
        # None 字段不应出现在 filters
        assert "keyword" not in result["filters"]

    @pytest.mark.asyncio
    async def test_route_procurement_cola(self):
        """"我要买可乐" → procurement + keyword=可乐 + category=商城"""
        output = RouterOutput(
            intent="procurement",
            confidence=0.95,
            sub_action="list",
            filters=RouterFilters(keyword="可乐", category="商城"),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(output)):
            result = await route_query("我要买可乐", "user1")

        assert result["intent"] == "procurement"
        assert result["filters"]["keyword"] == "可乐"
        assert result["filters"]["category"] == "商城"

    @pytest.mark.asyncio
    async def test_route_procurement_secondhand(self):
        """"有没有二手冰箱" → procurement + category=二手"""
        output = RouterOutput(
            intent="procurement",
            confidence=0.95,
            sub_action="list",
            filters=RouterFilters(keyword="冰箱", category="二手"),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(output)):
            result = await route_query("有没有二手冰箱")

        assert result["intent"] == "procurement"
        assert result["filters"]["category"] == "二手"

    @pytest.mark.asyncio
    async def test_route_service_repair(self):
        """非字面关键词也能识别成 service —— "家里灯不亮了" """
        output = RouterOutput(
            intent="service",
            confidence=0.9,
            sub_action="list",
            filters=RouterFilters(serviceType="维修"),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(output)):
            result = await route_query("家里灯不亮了")

        assert result["intent"] == "service"
        assert result["filters"]["serviceType"] == "维修"

    @pytest.mark.asyncio
    async def test_route_my_viewings(self):
        """"我的看房记录" → property + sub_action=my + my=viewings"""
        output = RouterOutput(
            intent="property",
            confidence=0.9,
            sub_action="my",
            filters=RouterFilters(my="viewings"),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(output)):
            result = await route_query("我的看房记录", "user1")

        assert result["intent"] == "property"
        assert result["sub_action"] == "my"
        assert result["filters"]["my"] == "viewings"

    @pytest.mark.asyncio
    async def test_route_greeting_general(self):
        """"你好" → general"""
        output = RouterOutput(
            intent="general", confidence=0.9, sub_action="list", filters=RouterFilters()
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(output)):
            result = await route_query("你好")

        assert result["intent"] == "general"
        assert result["filters"] == {}

    @pytest.mark.asyncio
    async def test_general_intent_strips_polluted_filters(self):
        """LLM 在 general 下错误地把所有 few-shot 字段都填了——代码侧必须清空。"""
        polluted = RouterOutput(
            intent="general",
            confidence=0.9,
            sub_action="list",
            filters=RouterFilters(
                keyword="冰箱",
                minPrice=1500,
                maxPrice=3000,
                district="长安区",
                bedrooms=1,
                serviceType="保洁",
                category="二手",
            ),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(polluted)):
            result = await route_query("hello")

        assert result["intent"] == "general"
        assert result["filters"] == {}  # general 必须空

    @pytest.mark.asyncio
    async def test_property_intent_drops_non_property_fields(self):
        """property 意图下，service/procurement 独有的字段会被白名单过滤掉。"""
        mixed = RouterOutput(
            intent="property",
            confidence=0.9,
            sub_action="list",
            filters=RouterFilters(
                minPrice=1500,
                maxPrice=2500,
                serviceType="保洁",   # 应被剔除
                category="商城",       # 应被剔除
            ),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(mixed)):
            result = await route_query("租两千左右的房")

        assert result["filters"] == {"minPrice": 1500, "maxPrice": 2500}

    @pytest.mark.asyncio
    async def test_zero_numeric_filters_are_dropped(self):
        """bedrooms=0、maxPrice=0 这类无意义数值不能作为筛选条件。"""
        output = RouterOutput(
            intent="property",
            confidence=0.9,
            sub_action="list",
            filters=RouterFilters(minPrice=1500, maxPrice=2500, bedrooms=0),
        )
        with patch("agents.router.get_llm", return_value=_make_structured_llm(output)):
            result = await route_query("租两千左右")

        assert "bedrooms" not in result["filters"]
        assert result["filters"]["minPrice"] == 1500
        assert result["filters"]["maxPrice"] == 2500


class TestRouterFallback:
    """验证 LLM 异常时的降级路径。"""

    @pytest.mark.asyncio
    async def test_structured_exception_falls_back_to_text(self):
        """structured output 抛异常 → 降级到纯文本 ainvoke + parse_route_response。"""
        structured = MagicMock()
        structured.ainvoke = AsyncMock(side_effect=Exception("structured 400"))

        llm = MagicMock()
        llm.with_structured_output = MagicMock(return_value=structured)
        llm.ainvoke = AsyncMock(
            return_value=MagicMock(
                content='{"intent": "procurement", "confidence": 0.8, "sub_action": "list", "filters": {"keyword": "可乐"}}'
            )
        )

        with patch("agents.router.get_llm", return_value=llm):
            result = await route_query("我要买可乐")

        assert result["intent"] == "procurement"
        assert result["filters"]["keyword"] == "可乐"

    @pytest.mark.asyncio
    async def test_llm_total_failure_uses_keyword_fallback(self):
        """LLM 彻底不可用 → 关键词兜底。"""
        llm = MagicMock()
        llm.with_structured_output = MagicMock(side_effect=NotImplementedError())
        llm.ainvoke = AsyncMock(side_effect=Exception("API down"))

        with patch("agents.router.get_llm", return_value=llm):
            result = await route_query("我想租房")

        assert result["intent"] == "property"  # 关键词兜底命中 "租房"

    @pytest.mark.asyncio
    async def test_llm_total_failure_unknown_query_is_general(self):
        """LLM 不可用 + 关键词也不命中 → general。"""
        llm = MagicMock()
        llm.with_structured_output = MagicMock(side_effect=NotImplementedError())
        llm.ainvoke = AsyncMock(side_effect=Exception("API down"))

        with patch("agents.router.get_llm", return_value=llm):
            result = await route_query("随便聊聊天气")

        assert result["intent"] == "general"


class TestParseRouteResponse:
    """解析纯文本响应（兼容降级路径）。"""

    def test_parse_json_full(self):
        raw = '{"intent": "property", "confidence": 0.9, "sub_action": "list", "filters": {"maxPrice": 2000}}'
        parsed = parse_route_response(raw)
        assert parsed["intent"] == "property"
        assert parsed["confidence"] == 0.9
        assert parsed["filters"]["maxPrice"] == 2000

    def test_parse_json_wrapped_in_text(self):
        """容错：前后带无关文字，也能截取 JSON。"""
        raw = '一些解释文字 {"intent": "service", "confidence": 0.8} 末尾'
        parsed = parse_route_response(raw)
        assert parsed["intent"] == "service"

    def test_parse_single_word(self):
        assert parse_route_response("property")["intent"] == "property"
        assert parse_route_response("procurement")["intent"] == "procurement"

    def test_parse_garbage_falls_to_general(self):
        """解析失败时保守归 general，不做子串猜测。"""
        assert parse_route_response("这里随便写点中文")["intent"] == "general"
        assert parse_route_response("")["intent"] == "general"
