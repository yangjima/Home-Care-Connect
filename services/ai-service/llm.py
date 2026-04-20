"""
LLM 统一封装 - 支持 DashScope (Qwen/Qwen3.5-397B-A17B)
"""
import logging
from typing import Optional

from langchain_openai import ChatOpenAI
from langchain_community.chat_models import ChatZhipuAI

from config import settings

logger = logging.getLogger(__name__)

# 某些部署不包含 Anthropic 依赖；按需导入避免启动失败
try:
    from langchain_anthropic import ChatAnthropic  # type: ignore
except Exception:  # pragma: no cover
    ChatAnthropic = None  # type: ignore

# 缓存 LLM 实例
_llm_instance: Optional[object] = None


def get_llm():
    """获取 LLM 实例"""
    global _llm_instance

    if _llm_instance is not None:
        return _llm_instance

    # 优先使用 DashScope (阿里云百炼)
    if settings.DASHSCOPE_API_KEY:
        _llm_instance = ChatOpenAI(
            model=settings.LLM_MODEL,
            api_key=settings.DASHSCOPE_API_KEY,
            base_url=settings.DASHSCOPE_BASE_URL,
            temperature=settings.LLM_TEMPERATURE,
            max_tokens=settings.LLM_MAX_TOKENS,
        )
        logger.info(f"LLM 初始化: DashScope/{settings.LLM_MODEL}")
        return _llm_instance

    # 备用: Zhipu AI
    zhipu_api_key = settings.DASHSCOPE_API_KEY  # 复用环境变量
    if zhipu_api_key:
        _llm_instance = ChatZhipuAI(
            model="glm-4",
            api_key=zhipu_api_key,
            temperature=settings.LLM_TEMPERATURE,
        )
        logger.info("LLM 初始化: Zhipu/glm-4")
        return _llm_instance

    # 最后的备用: 模拟 LLM（用于开发测试）
    logger.warning("未配置任何 LLM API Key，使用模拟 LLM")
    from langchain_core.messages import AIMessage

    class MockLLM:
        async def ainvoke(self, messages, config=None):
            from langchain_core.messages import HumanMessage
            user_msg = ""
            for msg in messages:
                if isinstance(msg, HumanMessage):
                    user_msg = msg.content

            # 简单的模拟响应
            if "房源" in user_msg or "租房" in user_msg:
                response_text = "【房源助手】根据您的需求，我为您推荐以下房源信息：\n\n1. **位于市中心的精装公寓**\n   - 面积：80㎡ 两室一厅\n   - 价格：4500元/月\n   - 特点：近地铁、配套齐全\n\n2. **城西花园小区**\n   - 面积：95㎡ 三室两厅\n   - 价格：3800元/月\n   - 特点：小区安静、绿化好\n\n如需了解更多信息，请告诉我您的具体需求（位置、预算、面积等）！"
            elif "服务" in user_msg or "家政" in user_msg:
                response_text = "【服务助手】我们提供以下社区服务：\n\n1. **日常家政** - 打扫、收纳整理\n2. **家电维修** - 水电维修、家电安装\n3. **老人陪护** - 专业护理人员\n4. **搬家服务** - 包装、搬运一条龙\n\n请问您需要预约哪项服务？"
            elif "采购" in user_msg or "二手" in user_msg:
                response_text = "【采购助手】欢迎使用采购和二手交易功能：\n\n1. **办公用品采购** - 各类办公设备、耗材\n2. **二手物品交易** - 家具、家电、电子产品\n\n请告诉我您具体需要什么，我会帮您查找！"
            else:
                response_text = "您好！我是居服通 AI 助手 🤖\n\n我可以帮您：\n- 查找合适房源\n- 推荐社区服务\n- 了解采购/二手信息\n- 解答各类问题\n\n请告诉我您的需求！"

            return AIMessage(content=response_text)

    _llm_instance = MockLLM()
    return _llm_instance


def reset_llm():
    """重置 LLM 实例（用于测试）"""
    global _llm_instance
    _llm_instance = None
