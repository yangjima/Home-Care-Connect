"""
居服通 AI 服务配置
"""
import os
from typing import Optional


class Settings:
    """应用配置"""

    # 服务配置
    HOST: str = os.getenv("AI_HOST", "0.0.0.0")
    PORT: int = int(os.getenv("AI_PORT", "8000"))

    # DashScope (阿里云百炼) LLM 配置
    DASHSCOPE_API_KEY: Optional[str] = os.getenv("DASHSCOPE_API_KEY")
    DASHSCOPE_BASE_URL: str = os.getenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")

    # LLM 模型配置
    LLM_MODEL: str = os.getenv("LLM_MODEL", "qwen-plus")
    LLM_TEMPERATURE: float = float(os.getenv("LLM_TEMPERATURE", "0.7"))
    LLM_MAX_TOKENS: int = int(os.getenv("LLM_MAX_TOKENS", "2000"))

    # Redis 配置（用于检查点持久化）
    REDIS_HOST: str = os.getenv("REDIS_HOST", "127.0.0.1")
    REDIS_PORT: int = int(os.getenv("REDIS_PORT", "6379"))
    REDIS_PASSWORD: Optional[str] = os.getenv("REDIS_PASSWORD")
    REDIS_DB: int = int(os.getenv("REDIS_DB", "0"))

    # 是否使用 Redis 检查点（False 则使用内存检查点）
    USE_REDIS_CHECKPOINT: bool = os.getenv("USE_REDIS_CHECKPOINT", "false").lower() == "true"

    # LangGraph 检查点配置
    CHECKPOINT_ENABLED: bool = True

    # 第三方服务地址（用于工具调用）
    USER_SERVICE_URL: str = os.getenv("USER_SERVICE_URL", "http://127.0.0.1:8081")
    PROPERTY_SERVICE_URL: str = os.getenv("PROPERTY_SERVICE_URL", "http://127.0.0.1:8082")
    SERVICE_ORDER_URL: str = os.getenv("SERVICE_ORDER_URL", "http://127.0.0.1:8082")
    ASSET_SERVICE_URL: str = os.getenv("ASSET_SERVICE_URL", "http://127.0.0.1:8083")

    # 日志级别
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    @property
    def dashscope_configured(self) -> bool:
        return bool(self.DASHSCOPE_API_KEY)


settings = Settings()
