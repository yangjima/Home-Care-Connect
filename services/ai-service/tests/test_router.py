"""
路由器智能体单元测试
"""
import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from agents.router import route_query, parse_route_response
from langchain_core.messages import AIMessage


class TestRouterAgent:
    """路由智能体测试"""

    @pytest.mark.asyncio
    async def test_route_property_query(self):
        """房源查询应路由到 property"""
        result = await route_query("我想找一套两室一厅的公寓", "user123")
        assert result == "property"

    @pytest.mark.asyncio
    async def test_route_service_query(self):
        """服务查询应路由到 service"""
        result = await route_query("需要预约家政保洁服务", "user123")
        assert result == "service"

    @pytest.mark.asyncio
    async def test_route_procurement_query(self):
        """采购查询应路由到 procurement"""
        result = await route_query("想买一些办公桌椅", "user123")
        assert result == "procurement"

    @pytest.mark.asyncio
    async def test_route_general_query(self):
        """通用查询应路由到 general"""
        result = await route_query("你好，请问你们平台是做什么的", "user123")
        assert result == "general"

    @pytest.mark.asyncio
    async def test_route_greeting(self):
        """问候语应路由到 general"""
        result = await route_query("你好")
        assert result == "general"

    @pytest.mark.asyncio
    async def test_route_secondhand_query(self):
        """二手查询应路由到 procurement"""
        result = await route_query("有没有二手冰箱转让")
        assert result == "procurement"

    @pytest.mark.asyncio
    async def test_route_nursing_query(self):
        """护理服务查询应路由到 service"""
        result = await route_query("老人陪护服务怎么收费")
        assert result == "service"

    @pytest.mark.asyncio
    async def test_route_rental_query(self):
        """租房查询应路由到 property"""
        result = await route_query("我想租一个一室一厅的房子")
        assert result == "property"

    @pytest.mark.asyncio
    async def test_route_no_api_key_uses_mock(self):
        """无 API Key 时使用模拟 LLM"""
        with patch("agents.router.get_llm") as mock_get_llm:
            mock_llm = MagicMock()
            mock_llm.ainvoke = AsyncMock(
                return_value=AIMessage(content="property")
            )
            mock_get_llm.return_value = mock_llm

            result = await route_query("附近有哪些房源")
            assert result in ["property", "service", "procurement", "general"]

    @pytest.mark.asyncio
    async def test_route_llm_exception_returns_general(self):
        """LLM 调用异常时返回 general"""
        with patch("agents.router.get_llm") as mock_get_llm:
            mock_llm = MagicMock()
            mock_llm.ainvoke = AsyncMock(side_effect=Exception("API Error"))
            mock_get_llm.return_value = mock_llm

            result = await route_query("测试查询")
            assert result == "general"


class TestParseRouteResponse:
    """路由响应解析测试"""

    def test_parse_property(self):
        assert parse_route_response("The best route is property") == "property"

    def test_parse_service(self):
        assert parse_route_response("route: service") == "service"

    def test_parse_procurement(self):
        assert parse_route_response("procurement is the right choice") == "procurement"

    def test_parse_json_response(self):
        response = '{"route": "property", "confidence": 0.9}'
        assert parse_route_response(response) == "property"

    def test_parse_chinese_keyword_property(self):
        assert parse_route_response("这个是关于租房的问题") == "property"

    def test_parse_chinese_keyword_service(self):
        assert parse_route_response("家政服务很好") == "service"

    def test_parse_fallback(self):
        assert parse_route_response("random text") == "general"
