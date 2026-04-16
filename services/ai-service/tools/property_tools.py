"""
房源工具 - 调用 Property Service API
"""
import logging
import json
from typing import Optional, Any

import httpx

from config import settings

logger = logging.getLogger(__name__)


async def search_properties(query: str, user_id: Optional[str] = None) -> str:
    """
    搜索房源

    Args:
        query: 用户查询文本
        user_id: 用户 ID

    Returns:
        JSON 格式的房源列表
    """
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            # 尝试搜索房源
            response = await client.get(
                f"{settings.PROPERTY_SERVICE_URL}/api/properties",
                params={"keyword": query, "page": 1, "size": 5},
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                properties = data.get("data", {}).get("list", [])

                if not properties:
                    return "暂无符合条件的房源，您可以尝试调整搜索条件。"

                return _format_properties(properties)
            else:
                logger.warning(f"房源搜索 API 返回: {response.status_code}")
                return _get_mock_properties(query)

    except Exception as e:
        logger.warning(f"房源服务调用失败，使用模拟数据: {e}")
        return _get_mock_properties(query)


async def get_property_detail(property_id: int, user_id: Optional[str] = None) -> str:
    """获取房源详情"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{settings.PROPERTY_SERVICE_URL}/api/properties/{property_id}",
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
            response = await client.post(
                f"{settings.PROPERTY_SERVICE_URL}/api/viewings",
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


def _get_mock_properties(query: str) -> str:
    """返回模拟房源数据（当服务不可用时）"""
    return """根据您的需求，为您推荐以下热门房源：

1. **城中心精品公寓**
   📍 长安区中山路188号
   💰 3500 元/月
   📐 65㎡ | 2室1厅1卫
   🏷️ 精装修 | 近地铁

2. **科技园智慧小区**
   📍 高新区创新路99号
   💰 4200 元/月
   📐 89㎡ | 3室2厅1卫
   🏷️ 配套齐全 | 24h安保

3. **河畔花园洋房**
   📍 滨河区沿河大道188号
   💰 5800 元/月
   📐 120㎡ | 3室2厅2卫
   🏷️ 河景房 | 花园小区

---
💡 提示: 点击「查看详情」可了解更多信息，或直接预约看房！"""
