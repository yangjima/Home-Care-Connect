"""
LangGraph 聊天图单元测试
"""
import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from langchain_core.messages import HumanMessage, AIMessage

from graph.chat_graph import create_chat_graph, router_node, route_decision
from graph.state import ChatState


class TestChatGraph:
    """聊天图测试"""

    def test_create_graph(self):
        """验证图可以正常创建"""
        graph = create_chat_graph()
        assert graph is not None

    def test_router_node_updates_route(self):
        """路由节点应更新 state 中的 route"""
        with patch("agents.router.route_query", new_callable=AsyncMock) as mock_route:
            mock_route.return_value = "property"

            state: ChatState = {
                "messages": [HumanMessage(content="我想找房源")],
                "session_id": "test-session",
                "user_id": "user123",
                "route": "",
                "last_agent": "",
                "query": "",
                "context": {},
                "response": "",
            }

            result = router_node(state)

            assert result["route"] == "property"
            assert result["query"] == "我想找房源"
            assert result["context"] == {}

    def test_route_decision_property(self):
        """路由决策 - property"""
        state: ChatState = {
            "messages": [],
            "session_id": "test",
            "user_id": None,
            "route": "property",
            "last_agent": "",
            "query": "",
            "context": {},
            "response": "",
        }

        assert route_decision(state) == "property"

    def test_route_decision_service(self):
        """路由决策 - service"""
        state = {
            "messages": [],
            "session_id": "test",
            "user_id": None,
            "route": "service",
            "last_agent": "",
            "query": "",
            "context": {},
            "response": "",
        }

        assert route_decision(state) == "service"

    def test_route_decision_general(self):
        """路由决策 - general"""
        state = {
            "messages": [],
            "session_id": "test",
            "user_id": None,
            "route": "general",
            "last_agent": "",
            "query": "",
            "context": {},
            "response": "",
        }

        assert route_decision(state) == "general"

    @pytest.mark.asyncio
    async def test_graph_invoke_property_query(self):
        """完整图调用 - 房源查询"""
        graph = create_chat_graph()

        with patch("agents.router.route_query", new_callable=AsyncMock) as mock_route:
            mock_route.return_value = "property"

            with patch("agents.property_agent.property_agent", new_callable=AsyncMock) as mock_agent:
                mock_agent.return_value = {
                    "messages": [HumanMessage(content="test")],
                    "session_id": "test-session",
                    "user_id": "user123",
                    "route": "property",
                    "last_agent": "property",
                    "query": "我想租房",
                    "context": {"search_results": "mock"},
                    "response": "以下是为您推荐的房源...",
                }

                result = await graph.ainvoke(
                    {
                        "messages": [HumanMessage(content="我想租房")],
                        "session_id": "test-session",
                        "user_id": "user123",
                    },
                    config={"configurable": {"thread_id": "test-thread"}},
                )

                assert "messages" in result
                assert result["route"] == "property"

    @pytest.mark.asyncio
    async def test_graph_invoke_general_query(self):
        """完整图调用 - 通用查询"""
        graph = create_chat_graph()

        with patch("agents.router.route_query", new_callable=AsyncMock) as mock_route:
            mock_route.return_value = "general"

            with patch("agents.response_agent.response_agent", new_callable=AsyncMock) as mock_response:
                mock_response.return_value = {
                    "messages": [HumanMessage(content="test")],
                    "session_id": "test-session",
                    "user_id": "user123",
                    "route": "general",
                    "last_agent": "general",
                    "query": "你好",
                    "context": {},
                    "response": "您好！欢迎使用居服通！",
                }

                result = await graph.ainvoke(
                    {
                        "messages": [HumanMessage(content="你好")],
                        "session_id": "test-session",
                        "user_id": "user123",
                    },
                    config={"configurable": {"thread_id": "test-thread"}},
                )

                assert "messages" in result
                assert result["route"] == "general"
