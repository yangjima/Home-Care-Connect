"""
LangGraph 聊天状态定义
"""
from typing import TypedDict, Annotated, Sequence, Any
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
import operator


class ChatState(TypedDict):
    """聊天图状态"""

    messages: Annotated[Sequence[BaseMessage], operator.add]
    """消息历史"""

    session_id: str
    """会话 ID"""

    user_id: str | None
    """用户 ID"""

    route: str
    """路由结果（兼容字段）：property | service | procurement | general"""

    confidence: float
    """意图置信度（0-1）"""

    sub_action: str
    """子动作：list | detail | book | my"""

    filters: dict[str, Any]
    """从用户话术抽取的筛选条件"""

    last_agent: str
    """最后执行的 Agent 名称"""

    query: str
    """原始查询"""

    context: dict
    """从各服务获取的上下文数据"""

    response: str
    """最终响应"""
