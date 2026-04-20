"""
房源工具 - 调用 Property Service API
"""
import logging
import json
from typing import Optional, Any

import httpx

from config import settings

logger = logging.getLogger(__name__)


def _page_records(payload: dict) -> list:
    """解析 Spring Result<PageResult<T>> 或 list 为记录列表。"""
    inner = payload.get("data")
    if isinstance(inner, list):
        return inner
    if isinstance(inner, dict):
        return inner.get("records") or inner.get("list") or []
    return []


async def search_properties(
    query: str,
    user_id: Optional[str] = None,
    filters: Optional[dict] = None,
) -> str:
    """
    搜索房源。

    Args:
        query: 用户原始查询（仅在 filters 中无 keyword 时作为兜底）
        user_id: 用户 ID
        filters: router 抽取的结构化筛选条件（keyword/minPrice/maxPrice/district/bedrooms）

    Returns:
        格式化的房源列表文本；无结果时返回明确的空结果提示，调用方必须如实告知用户。
    """
    filters = filters or {}
    params: dict[str, Any] = {"page": 1, "pageSize": 5}

    # 仅当 router 抽到了核心词才传 keyword；抽不到就走无关键词的列表查询，
    # 避免把整句"我要租两千左右的房子"塞给后端当 keyword 匹配。
    if filters.get("keyword"):
        params["keyword"] = filters["keyword"]
    if filters.get("minPrice") is not None:
        params["minPrice"] = filters["minPrice"]
    if filters.get("maxPrice") is not None:
        params["maxPrice"] = filters["maxPrice"]
    if filters.get("district"):
        params["district"] = filters["district"]
    if filters.get("bedrooms") is not None:
        params["bedrooms"] = filters["bedrooms"]

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.PROPERTY_SERVICE_URL.rstrip("/")
            response = await client.get(
                f"{base}/properties",
                params=params,
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                properties = _page_records(data)

                if not properties:
                    return "EMPTY_RESULT: 当前平台没有符合用户条件的房源。"

                return _format_properties(properties)
            else:
                logger.warning(f"房源搜索 API 返回: {response.status_code}")
                return "EMPTY_RESULT: 房源服务暂时不可用。"

    except Exception as e:
        logger.warning(f"房源服务调用失败: {e}")
        return "EMPTY_RESULT: 房源服务暂时不可用。"


async def get_property_detail(property_id: int, user_id: Optional[str] = None) -> str:
    """获取房源详情"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.PROPERTY_SERVICE_URL.rstrip("/")
            response = await client.get(
                f"{base}/properties/{property_id}",
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                property_data = data.get("data", {})
                return _format_property_detail(property_data)

    except Exception as e:
        logger.warning(f"获取房源详情失败: {e}")

    return "暂时无法获取房源详情，请稍后再试。"


async def book_viewing(
    property_id: int,
    user_id: str,
    viewing_time: str,
    contact_phone: str,
    remark: str = "",
) -> str:
    """预约看房"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.PROPERTY_SERVICE_URL.rstrip("/")
            response = await client.post(
                f"{base}/viewings",
                json={
                    "propertyId": property_id,
                    "userId": user_id,
                    "viewingTime": viewing_time,
                    "contactPhone": contact_phone,
                    "remark": remark,
                },
                headers=_build_headers(user_id),
            )

            if response.status_code in (200, 201):
                return "看房预约成功！我们的工作人员会尽快与您联系确认。"
            else:
                return f"预约失败: {response.status_code}"

    except Exception as e:
        logger.warning(f"预约看房失败: {e}")
        return f"预约服务暂时不可用: {str(e)}"


def _build_headers(user_id: Optional[str]) -> dict:
    """构建请求头"""
    headers = {"Content-Type": "application/json"}
    if user_id:
        headers["X-User-Id"] = str(user_id)
    return headers


def _format_properties(properties: list) -> str:
    """格式化房源列表"""
    lines = []
    for i, p in enumerate(properties, 1):
        lines.append(
            f"{i}. **{p.get('title', '未知标题')}**\n"
            f"   📍 {p.get('address', '未知地址')}\n"
            f"   💰 {p.get('price', '面议')} 元/月\n"
            f"   📐 {p.get('area', '?')}㎡ | {p.get('rooms', '?')}室{p.get('livingRooms', '?')}厅\n"
            f"   🏷️ {p.get('type', '普通住宅')}"
        )
    return "\n\n".join(lines)


def _format_property_detail(property_data: dict) -> str:
    """格式化房源详情"""
    return (
        f"**{property_data.get('title', '未知')}**\n\n"
        f"📍 地址: {property_data.get('address', '未知')}\n"
        f"💰 价格: {property_data.get('price', '面议')} 元/月\n"
        f"📐 面积: {property_data.get('area', '?')} 平方米\n"
        f"🛏️ 户型: {property_data.get('rooms', '?')}室{property_data.get('livingRooms', '?')}厅{property_data.get('bathrooms', '?')}卫\n"
        f"🏢 楼层: {property_data.get('floor', '?')}/{property_data.get('totalFloors', '?')}\n"
        f"🏗️ 装修: {property_data.get('decoration', '未知')}\n"
        f"🏷️ 类型: {property_data.get('type', '普通住宅')}\n"
        f"📝 描述: {property_data.get('description', '暂无描述')}"
    )


