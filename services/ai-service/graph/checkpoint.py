"""
Redis 检查点配置
"""
import logging
from typing import Optional

from langgraph.checkpoint.redis import RedisSaver

from config import settings

logger = logging.getLogger(__name__)


def create_redis_checkpointer() -> Optional[RedisSaver]:
    """创建 Redis 检查点持久化器"""
    if not settings.USE_REDIS_CHECKPOINT:
        return None

    try:
        redis_saver = RedisSaver(
            host=settings.REDIS_HOST,
            port=settings.REDIS_PORT,
            password=settings.REDIS_PASSWORD,
            db=settings.REDIS_DB,
        )
        logger.info("Redis 检查点持久化器创建成功")
        return redis_saver
    except Exception as e:
        logger.error(f"Redis 检查点初始化失败: {e}")
        return None
