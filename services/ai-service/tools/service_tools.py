"""
社区服务工具 - 调用 Service Order Service API
"""
import logging
import json
from typing import Optional

import httpx

from config import settings

logger = logging.getLogger(__name__)


async def search_services(query: str, user_id: Optional[str] = None) -> str:
    """
    搜索社区服务
    """
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{settings.SERVICE_ORDER_URL}/api/service-types",
                params={"keyword": query},
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                services = data.get("data", [])

                if not services:
                    return "暂无符合条件的社区服务。"

                return _format_services(services)
            else:
                return _get_mock_services(query)

    except Exception as e:
        logger.warning(f"服务搜索 API 调用失败: {e}")
        return _get_mock_services(query)


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
            response = await client.post(
                f"{settings.SERVICE_ORDER_URL}/api/orders",
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
            response = await client.get(
                f"{settings.SERVICE_ORDER_URL}/api/service-types",
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


def _get_mock_services(query: str) -> str:
    """返回模拟服务数据"""
    return """我们提供以下热门社区服务：

1. **日常家政保洁** 🧹
   💵 价格: 80 元/小时 起
   📝 专业家政人员，提供日常保洁、深度清洁、开荒保洁等服务

2. **家电维修安装** 🔧
   💵 价格: 100 元/次 起
   📝 专业维修师傅，处理水电故障、家电安装调试

3. **老人陪护服务** 👴
   💵 价格: 200 元/天 起
   📝 持证护理人员，提供日常陪护、健康监测、用药提醒

4. **搬家运输服务** 📦
   💵 价格: 面议
   📝 包装、搬运、运输一条龙服务

---
💡 点击「立即预约」可快速下单，或告诉我您的具体需求！"""
