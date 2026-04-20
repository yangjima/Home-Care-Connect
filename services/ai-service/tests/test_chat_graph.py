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

    @pytest.mark.asyncio
    async def test_router_node_updates_route(self):
        """路由节点应更新 state 中的 route / filters"""
        # chat_graph 模块顶层已 import route_query，patch 那里的绑定
        with patch("graph.chat_graph.route_query", new_callable=AsyncMock) as mock_route:
            mock_route.return_value = {
                "intent": "property",
                "confidence": 0.9,
                "sub_action": "list",
                "filters": {"maxPrice": 2000},
            }

            state: ChatState = {
                "messages": [HumanMessage(content="我想找房源")],
                "session_id": "test-session",
                "user_id": "user123",
                "route": "",
                "confidence": 0.0,
                "sub_action": "list",
                "filters": {},
                "last_agent": "",
                "query": "",
                "context": {},
                "response": "",
            }

            result = await router_node(state)

            assert result["route"] == "property"
            assert result["confidence"] == 0.9
            assert result["filters"]["maxPrice"] == 2000
            assert result["query"] == "我想找房源"
            assert result["context"] == {}

    def test_route_decision_property(self):
        """路由决策 - property"""
        state: ChatState = {
            "messages": [],
            "session_id": "test",
            "user_id": None,
            "route": "property",
            "confidence": 0.9,
            "sub_action": "list",
            "filters": {},
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
            "confidence": 0.9,
            "sub_action": "list",
            "filters": {},
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
            "confidence": 0.4,
            "sub_action": "list",
            "filters": {},
            "last_agent": "",
            "query": "",
            "context": {},
            "response": "",
        }

        assert route_decision(state) == "general"

    @pytest.mark.asyncio
    async def test_graph_invoke_property_query(self):
        """完整图调用 - 房源查询：patch 掉 router 和 property_agent 在 graph 模块的绑定"""
        with patch("graph.chat_graph.route_query", new_callable=AsyncMock) as mock_route, \
             patch("graph.chat_graph.property_agent", new_callable=AsyncMock) as mock_agent:
            mock_route.return_value = {
                "intent": "property",
                "confidence": 0.9,
                "sub_action": "list",
                "filters": {"maxPrice": 2000},
            }
            mock_agent.side_effect = lambda state: {
                **state,
                "route": "property",
                "last_agent": "property",
                "context": {"search_results": "mock"},
                "response": "以下是为您推荐的房源...",
            }

            # 在 patch 生效后再创建 graph，确保节点用到的是打过补丁的符号
            graph = create_chat_graph()
            result = await graph.ainvoke(
                {
                    "messages": [HumanMessage(content="我想租房")],
                    "session_id": "test-session",
                    "user_id": "user123",
                },
                config={"configurable": {"thread_id": "test-thread-1"}},
            )

            assert "messages" in result
            assert result["route"] == "property"
            assert result["filters"]["maxPrice"] == 2000

    @pytest.mark.asyncio
    async def test_graph_invoke_general_query(self):
        """完整图调用 - 通用查询：patch router + response_agent。"""
        with patch("graph.chat_graph.route_query", new_callable=AsyncMock) as mock_route, \
             patch("graph.chat_graph.response_agent", new_callable=AsyncMock) as mock_response:
            mock_route.return_value = {
                "intent": "general",
                "confidence": 0.4,
                "sub_action": "list",
                "filters": {},
            }
            mock_response.side_effect = lambda state: {
                **state,
                "last_agent": "general",
                "response": "您好！欢迎使用居服通！",
            }

            graph = create_chat_graph()
            result = await graph.ainvoke(
                {
                    "messages": [HumanMessage(content="你好")],
                    "session_id": "test-session",
                    "user_id": "user123",
                },
                config={"configurable": {"thread_id": "test-thread-2"}},
            )

            assert "messages" in result
            assert result["route"] == "general"
