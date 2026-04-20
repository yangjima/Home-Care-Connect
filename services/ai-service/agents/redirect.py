from __future__ import annotations

from typing import Any
from urllib.parse import urlencode, quote


ALLOWED_PATH_PREFIXES = (
    "/properties",
    "/services",
    "/purchase",
    "/secondhand",
    "/user/orders",
    "/user/viewings",
    "/user/properties",
)


def _is_allowed_path(path: str) -> bool:
    if not path or not path.startswith("/"):
        return False
    return any(path == p or path.startswith(p + "/") for p in ALLOWED_PATH_PREFIXES)


def _encode_query(filters: dict[str, Any]) -> str:
    clean: dict[str, Any] = {}
    for k, v in (filters or {}).items():
        if v is None:
            continue
        if isinstance(v, str) and not v.strip():
            continue
        clean[k] = v
    if not clean:
        return ""
    return urlencode(clean, doseq=True, quote_via=quote)


def build_redirect(intent: str, sub_action: str | None, filters: dict[str, Any] | None) -> str | None:
    """
    Build a frontend redirect URL for a structured intent decision.
    - Enforces a whitelist of real SPA routes
    - Uses querystring for list filters
    """
    filters = filters or {}
    sub_action = sub_action or "list"

    # "我的"相关优先
    if sub_action == "my":
        if intent == "service":
            return "/user/orders"
        if intent == "property":
            my = str(filters.get("my") or "").lower()
            if my == "properties":
                return "/user/properties"
            if my == "viewings":
                return "/user/viewings"
            # 默认：用户说“我的看房/预约”时更常见
            return "/user/viewings"
        # procurement/general 暂不定义 my
        return None

    if intent == "property":
        pid = filters.get("id")
        if sub_action in ("detail", "book") and pid not in (None, ""):
            path = f"/properties/{pid}"
            if sub_action == "book":
                q = _encode_query({"book": 1})
                url = f"{path}?{q}" if q else path
            else:
                url = path
            return url if _is_allowed_path(path) else None

        path = "/properties"
        q = _encode_query(filters)
        url = f"{path}?{q}" if q else path
        return url if _is_allowed_path(path) else None

    if intent == "service":
        sid = filters.get("id")
        if sub_action in ("detail", "book") and sid not in (None, ""):
            path = f"/services/{sid}"
            return path if _is_allowed_path(path) else None

        path = "/services"
        q = _encode_query(filters)
        url = f"{path}?{q}" if q else path
        return url if _is_allowed_path(path) else None

    if intent == "procurement":
        category = str(filters.get("category") or "").strip()
        if category in ("二手", "secondhand", "used"):
            path = "/secondhand"
        else:
            path = "/purchase"
        q = _encode_query({k: v for k, v in filters.items() if k != "category"})
        url = f"{path}?{q}" if q else path
        return url if _is_allowed_path(path) else None

    return None

