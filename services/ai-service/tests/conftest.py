"""
居服通 AI 服务 pytest 配置
"""
import os
import sys

# 确保项目根目录在 Python 路径中
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 测试环境变量配置
os.environ.setdefault("DASHSCOPE_API_KEY", "")
os.environ.setdefault("USE_REDIS_CHECKPOINT", "false")
os.environ.setdefault("REDIS_HOST", "127.0.0.1")
os.environ.setdefault("REDIS_PORT", "6379")
os.environ.setdefault("LOG_LEVEL", "WARNING")
