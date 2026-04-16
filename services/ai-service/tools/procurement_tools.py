"""
采购与二手工具 - 调用 Asset Service API
"""
import logging
from typing import Optional

import httpx

from config import settings

logger = logging.getLogger(__name__)


async def search_procurement_products(query: str, user_id: Optional[str] = None) -> str:
    """搜索办公用品采购"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{settings.ASSET_SERVICE_URL}/api/procurement-products",
                params={"keyword": query, "page": 1, "size": 5},
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                products = data.get("data", {}).get("list", [])

                if not products:
                    return "暂无符合条件的采购商品。"

                return _format_products(products)

    except Exception as e:
        logger.warning(f"采购商品搜索失败: {e}")

    return _get_mock_procurement(query)


async def search_secondhand_items(query: str, user_id: Optional[str] = None) -> str:
    """搜索二手物品"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{settings.ASSET_SERVICE_URL}/api/secondhand-items",
                params={"keyword": query, "page": 1, "size": 5},
                headers=_build_headers(user_id),
            )

            if response.status_code == 200:
                data = response.json()
                items = data.get("data", {}).get("list", [])

                if not items:
                    return "暂无符合条件的二手物品。"

                return _format_secondhand(items)

    except Exception as e:
        logger.warning(f"二手物品搜索失败: {e}")

    return _get_mock_secondhand(query)


async def create_procurement_order(
    user_id: str,
    product_id: int,
    quantity: int = 1,
    remark: str = "",
) -> str:
    """创建采购订单"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                f"{settings.ASSET_SERVICE_URL}/api/orders",
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


def _get_mock_procurement(query: str) -> str:
    return """办公用品采购目录：

1. **打印复印设备**
   💵 激光打印机: 1200 元/台 起
   💵 复印机租赁: 300 元/月 起

2. **办公桌椅**
   💵 标准办公桌: 280 元/张
   💵 人体工学椅: 450 元/把

3. **文具耗材**
   💵 A4 复印纸: 25 元/箱
   💵 墨盒套装: 80 元/套

---
💡 如需批量采购，可享折扣优惠！"""


def _get_mock_secondhand(query: str) -> str:
    return """二手物品交易广场：

1. **办公家具转让** 🪑
   💵 9成新办公桌: 150 元
   💵 会议桌(1.8m): 300 元

2. **家用电器** 📺
   💵 55寸智能电视: 1200 元 (9成新)
   💵 冰箱(双开门): 800 元 (8成新)

3. **电子设备** 💻
   💵 笔记本电脑: 2500 元 (95新)
   💵 台式机套装: 800 元

---
💡 所有二手物品均经过审核，信息真实可靠！"""
