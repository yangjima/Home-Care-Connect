"""
社区服务工具 - 调用 Service Order Service API
"""
import logging
import json
from typing import Optional

import httpx

from config import settings

logger = logging.getLogger(__name__)


async def search_services(
    query: str,
    user_id: Optional[str] = None,
    filters: Optional[dict] = None,
) -> str:
    """搜索社区服务类型（按 router 抽取的 serviceType/keyword）。"""
    filters = filters or {}
    params: dict = {}
    # service-type 后端用 keyword 过滤；优先使用 serviceType，其次 keyword
    if filters.get("serviceType"):
        params["keyword"] = filters["serviceType"]
    elif filters.get("keyword"):
        params["keyword"] = filters["keyword"]

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.SERVICE_ORDER_URL.rstrip("/")
            response = await client.get(
                f"{base}/service-types",
                params=params,
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                services = data.get("data", [])

                if not services:
                    return "EMPTY_RESULT: 当前没有符合用户条件的社区服务。"

                return _format_services(services)
            else:
                return "EMPTY_RESULT: 社区服务暂时不可用。"

    except Exception as e:
        logger.warning(f"服务搜索 API 调用失败: {e}")
        return "EMPTY_RESULT: 社区服务暂时不可用。"


async def create_service_order(
    user_id: str,
    service_type_id: int,
    service_time: str,
    service_address: str,
    remark: str = "",
) -> str:
    """创建服务订单"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.SERVICE_ORDER_URL.rstrip("/")
            response = await client.post(
                f"{base}/orders",
                json={
                    "userId": user_id,
                    "serviceTypeId": service_type_id,
                    "serviceTime": service_time,
                    "serviceAddress": service_address,
                    "remark": remark,
                },
                headers=_build_headers(user_id),
            )

            if response.status_code in (200, 201):
                result = response.json()
                order_no = result.get("data", {}).get("orderNo", "")
                return f"服务订单创建成功！订单号: {order_no}。我们的服务人员会尽快与您联系确认。"

    except Exception as e:
        logger.warning(f"创建服务订单失败: {e}")

    return "服务预约暂时不可用，请稍后再试或联系客服。"


async def get_service_types(category: Optional[str] = None) -> str:
    """获取服务类型列表"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            params = {"category": category} if category else {}
            base = settings.SERVICE_ORDER_URL.rstrip("/")
            response = await client.get(
                f"{base}/service-types",
                params=params,
                headers=_build_headers(None),
            )

            if response.status_code == 200:
                data = response.json()
                services = data.get("data", [])
                return _format_services(services)

    except Exception as e:
        logger.warning(f"获取服务类型失败: {e}")

    return "暂时无法获取服务列表。"


def _build_headers(user_id: Optional[str]) -> dict:
    headers = {"Content-Type": "application/json"}
    if user_id:
        headers["X-User-Id"] = str(user_id)
    return headers


def _format_services(services: list) -> str:
    """格式化服务列表"""
    lines = []
    for s in services:
        lines.append(
            f"- **{s.get('name', '未知服务')}**\n"
            f"  💵 价格: {s.get('price', '?')} 元/{s.get('unit', '次')}\n"
            f"  📝 {s.get('description', '暂无描述')}"
        )
    return "\n\n".join(lines)


