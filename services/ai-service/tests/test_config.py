"""
配置模块单元测试
"""
import os
from config import Settings


class TestSettings:
    """配置测试"""

    def test_default_values(self):
        """测试默认配置值"""
        settings = Settings()

        assert settings.HOST == "0.0.0.0"
        assert settings.PORT == 8000
        assert settings.LLM_MODEL == "Qwen/Qwen3.5-397B-A17B"
        assert settings.LLM_TEMPERATURE == 0.7
        assert settings.LLM_MAX_TOKENS == 2000
        assert settings.REDIS_HOST == "127.0.0.1"
        assert settings.REDIS_PORT == 6379

    def test_env_override(self):
        """测试环境变量覆盖"""
        os.environ["AI_HOST"] = "127.0.0.1"
        os.environ["AI_PORT"] = "9000"
        os.environ["LLM_MODEL"] = "Qwen/Qwen3.5-397B-A17B"
        os.environ["USE_REDIS_CHECKPOINT"] = "true"

        # 创建新实例以获取更新的环境变量
        settings = Settings()

        assert settings.HOST == "127.0.0.1"
        assert settings.PORT == 9000
        assert settings.LLM_MODEL == "Qwen/Qwen3.5-397B-A17B"
        assert settings.USE_REDIS_CHECKPOINT is True

        # 清理
        del os.environ["AI_HOST"]
        del os.environ["AI_PORT"]
        del os.environ["LLM_MODEL"]
        del os.environ["USE_REDIS_CHECKPOINT"]

    def test_dashscope_configured_without_key(self):
        """无 API Key 时 dashscope_configured 应为 False"""
        os.environ.pop("DASHSCOPE_API_KEY", None)
        settings = Settings()
        assert settings.dashscope_configured is False

    def test_dashscope_configured_with_key(self):
        """有 API Key 时 dashscope_configured 应为 True"""
        os.environ["DASHSCOPE_API_KEY"] = "test-key"
        settings = Settings()
        assert settings.dashscope_configured is True
        del os.environ["DASHSCOPE_API_KEY"]
