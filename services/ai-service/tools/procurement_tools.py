"""
采购与二手工具 - 调用 Asset Service API
"""
import logging
from typing import Optional

import httpx

from config import settings

logger = logging.getLogger(__name__)


def _page_records(payload: dict) -> list:
    inner = payload.get("data")
    if isinstance(inner, list):
        return inner
    if isinstance(inner, dict):
        return inner.get("records") or inner.get("list") or []
    return []


def _build_search_params(filters: Optional[dict]) -> dict:
    """按 router 抽出的 filters 组装查询参数，仅在明确抽到时传给后端。"""
    filters = filters or {}
    params: dict = {"page": 1, "pageSize": 5}
    if filters.get("keyword"):
        params["keyword"] = filters["keyword"]
    if filters.get("minPrice") is not None:
        params["minPrice"] = filters["minPrice"]
    if filters.get("maxPrice") is not None:
        params["maxPrice"] = filters["maxPrice"]
    return params


async def search_procurement_products(
    query: str,
    user_id: Optional[str] = None,
    filters: Optional[dict] = None,
) -> str:
    """搜索商城商品。"""
    params = _build_search_params(filters)
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.ASSET_SERVICE_URL.rstrip("/")
            response = await client.get(
                f"{base}/procurement-products",
                params=params,
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                products = _page_records(data)

                if not products:
                    return "EMPTY_RESULT: 当前商城没有符合用户条件的商品。"

                return _format_products(products)

    except Exception as e:
        logger.warning(f"采购商品搜索失败: {e}")

    return "EMPTY_RESULT: 商城服务暂时不可用。"


async def search_secondhand_items(
    query: str,
    user_id: Optional[str] = None,
    filters: Optional[dict] = None,
) -> str:
    """搜索二手物品。"""
    params = _build_search_params(filters)
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.ASSET_SERVICE_URL.rstrip("/")
            response = await client.get(
                f"{base}/secondhand-items",
                params=params,
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                items = _page_records(data)

                if not items:
                    return "EMPTY_RESULT: 当前二手交易区没有符合用户条件的物品。"

                return _format_secondhand(items)

    except Exception as e:
        logger.warning(f"二手物品搜索失败: {e}")

    return "EMPTY_RESULT: 二手服务暂时不可用。"


async def create_procurement_order(
    user_id: str,
    product_id: int,
    quantity: int = 1,
    remark: str = "",
) -> str:
    """创建采购订单"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            base = settings.ASSET_SERVICE_URL.rstrip("/")
            # 采购下单若后端未提供独立接口，调用会失败并走下方提示
            response = await client.post(
                f"{base}/procurement-orders",
                json={
                    "userId": user_id,
                    "productId": product_id,
                    "quantity": quantity,
                    "remark": remark,
                },
                headers=_build_headers(user_id),
            )

            if response.status_code in (200, 201):
                return "采购订单创建成功！"

    except Exception as e:
        logger.warning(f"创建采购订单失败: {e}")

    return "采购订单暂时无法创建，请稍后再试。"


def _build_headers(user_id: Optional[str]) -> dict:
    headers = {"Content-Type": "application/json"}
    if user_id:
        headers["X-User-Id"] = str(user_id)
    return headers


def _format_products(products: list) -> str:
    lines = []
    for p in products:
        lines.append(
            f"- **{p.get('name', '未知商品')}**\n"
            f"  💵 价格: {p.get('price', '?')} 元\n"
            f"  📦 库存: {p.get('stock', '?')} 件\n"
            f"  📝 {p.get('description', '')}"
        )
    return "\n\n".join(lines)


def _format_secondhand(items: list) -> str:
    lines = []
    for item in items:
        lines.append(
            f"- **{item.get('title', '未知物品')}**\n"
            f"  💵 价格: {item.get('price', '?')} 元\n"
            f"  🏷️ 成色: {item.get('condition', '未知')}\n"
            f"  📝 {item.get('description', '')}"
        )
    return "\n\n".join(lines)


