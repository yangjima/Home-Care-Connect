# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

居服通 (Home Care Connect) — local-community platform anchored on stores/apartments. MVP covers property listings + viewings, community services (cleaning, repair, etc.) with order lifecycle, local mall + secondhand marketplace, and an AI assistant. Stack: Spring Boot 3 / Java 21 microservices + Spring Cloud Gateway + FastAPI/LangGraph AI service + Vue 3 SPA. MySQL / Redis / MinIO, optional Nacos for registry and shared config.

## Common commands

Docker (from repo root, via `Makefile`):

- `make up` — build + start the full stack via `infrastructure/docker/docker-compose.yml`.
- `make infra` / `make services` — start only infra (mysql/redis/minio/nacos) or only the app services + frontend.
- `make ps` / `make logs` / `make logs-svc SVC=gateway` — status, tail all logs, tail a single service.
- `make down` / `make clean` — stop / stop + delete volumes.

Frontend (`frontend/`):

- `npm install && npm run dev` — Vite dev server on `:5173`. Proxies `/api` → `http://127.0.0.1:8080` (gateway) and `/minio` → `http://127.0.0.1:9001` (MinIO API).
- `npm run build` — runs `vue-tsc --noEmit` then `vite build`.
- `npm run type-check` / `npm run lint` / `npm run test` (Vitest) / `npm run test:ui`.
- Dev HTTPS: set `VITE_DEV_HTTPS=true` to enable `@vitejs/plugin-basic-ssl`.
- Optional login-password RSA encryption: set `VITE_LOGIN_RSA_PUBLIC_KEY` (PEM) — login payload is then sent as `rsa_oaep_sha256:<base64>`; must pair with backend `LOGIN_RSA_PRIVATE_KEY`.

Backend Java services (`services/`, parent POM at `services/pom.xml`):

- `cd services && mvn -pl user-service -am package` — build one module with its deps. Swap module name as needed (`gateway`, `user-service`, `property-service`, `service-order-service`, `asset-service`).
- `cd services && mvn -pl user-service test` — run tests for one module. `mvn -pl user-service -Dtest=JwtAuthFilterTest test` for a single test class.
- Local override: each Java service reads `application-local.yml` if present (gitignored) — use it for JDBC URL, Redis password, JWT secret, `LOGIN_RSA_PRIVATE_KEY`, etc.

AI service (`services/ai-service/`, Python 3.11 + FastAPI + LangGraph):

- `pip install -r requirements.txt && uvicorn main:app --host 0.0.0.0 --port 8000`.
- `pytest` (config in `pytest.ini`, tests under `tests/`, asyncio auto).
- Requires `DASHSCOPE_API_KEY` (Aliyun Bailian / DashScope).

## Architecture

### Request flow

Browser → **Gateway (`:8080`, Spring Cloud Gateway)** → downstream service. Routes in `services/gateway/src/main/resources/application.yml`:

- `/api/user/**` → `user-service` (`:8081`)
- `/api/property/**` → `property-service` (`:8082`)
- `/api/service/**` → `service-order-service` (`:8082` locally — in Docker it binds its own container port; the two services only collide in non-containerized local runs, override one via `SERVER_PORT`)
- `/api/asset/**` → `asset-service` (`:8083`)
- `/api/ai/**` → `ai-service` (`:8000`, no load-balancer, direct URL)

All routes use `StripPrefix=2`, so downstream controllers see paths without the `/api/<svc>` prefix. The AI route uses `StripPrefix=0` — AI endpoints keep the `/api/ai/...` prefix in FastAPI routes.

### Auth — single source of truth at the gateway

`services/gateway/.../JwtAuthFilter.java` is the only place that validates JWTs. After validation it rewrites the request with these headers before forwarding:

- `X-User-Id`, `X-Username`, `X-User-Role`, optional `X-User-Store-Id`

Downstream services **must not** re-validate the JWT and **must not** trust a body-supplied `userId`. Each service has a `util/GatewayHeaders.java` helper — use `GatewayHeaders.userId(request)` / `.role(request)` / `.storeId(request)` to read the authenticated caller. Do not fall back to a hardcoded `userId=1`; several past bugs stemmed from this.

The gateway's whitelist (anonymous access) is narrow and method-aware — login/register/send-code, health, a few AI paths, and **GET-only** access to public property listings, service types, service staff, procurement products, and secondhand items. Any write path (POST/PUT/PATCH/DELETE) on those resources requires a token. When adding a new public GET endpoint, update `JwtAuthFilter.WHITE_LIST` or the method-specific matchers (`isPublicProcurementAssetGet`, `isPublicSecondhandAssetGet`).

### Roles / RBAC

Roles live in `sys_user.role` (MySQL ENUM) and the JWT `role` claim. Values:

- `admin` — platform super-admin
- `store_manager` — equivalent to `admin` on the admin console (jointly "platform admin")
- `supplier` — merchant: manages own properties + procurement products; admin console limited to dashboard + property pages
- `tenant` / `user` — regular user (browse, book, order, post secondhand)

Public registration only grants `tenant`/`user` — privileged roles are assigned via `PATCH /api/user/users/{id}/role` (platform admin only).

Frontend role constants + helpers: `frontend/src/constants/roles.ts` (`isPlatformAdmin`, `canAccessAdmin`, `hasAdminRouteRole`). Router guards in `frontend/src/router/index.ts` enforce both `requiresAuth` and per-route `meta.adminRoles`. Admin routes live under `/admin/*` with `AdminLayout`; user-side pages under `/user/*` with `UserLayout`.

Per-resource rules (enforced in each service's controllers/services, not the gateway):

- **property-service**: create → admin/store_manager/supplier; update/delete/list/unlist → owner or platform admin; **feature/recommend → platform admin only**. Viewings list is role-filtered (admin: all; supplier: requires `propertyId` + must own it; regular user: own bookings only).
- **service-order-service**: order list — regular user is forced to own records; admin/supplier see full list. Confirm / complete / delete → platform admin only.
- **asset-service**: procurement products POST/PUT/DELETE/PATCH stock → admin/store_manager/supplier. Secondhand posting is regular-user capability.
- **user-service**: `GET /users`, status change, role change → platform admin. `GET/PUT /users/{id}` → self or platform admin.

### Frontend structure

- `src/api/*.ts` — typed wrappers over axios (`auth.ts`, `property.ts`, `service.ts`, `asset.ts`, `users.ts`, `stores.ts`, `ai.ts`, `resource.ts`, `pagination.ts`).
- `src/stores/*.ts` — Pinia stores (`auth`, `property`, `service`, `secondhand`, `ai`). `auth.ts` holds user/JWT/role and is consulted by the router guard.
- `src/composables/` — shared logic (`useResourceList.ts`, `useAdminForm.ts`).
- `src/views/admin/` — admin console pages; `src/views/user/` — personal-center pages; top-level `property/`, `service/`, `asset/`, `home/`, `ai/`, `auth/` for the public site.
- Element Plus components and icons are auto-registered via `unplugin-vue-components` + `unplugin-auto-import` — no need to import `ElButton`, `ref`, `useRouter`, etc. manually. Generated types in `src/auto-imports.d.ts` / `src/components.d.ts`.

### AI service

`services/ai-service/` is a LangGraph multi-agent graph (not a simple chat). Structure:

- `agents/` — `router.py` classifies intent, then delegates to `property_agent`, `service_agent`, `procurement_agent`, or `response_agent`.
- `graph/chat_graph.py` + `graph/state.py` + `graph/checkpoint.py` — LangGraph state graph with Redis-backed checkpointing.
- `tools/` — tool implementations that call the Java services over HTTP (`property_tools.py`, `service_tools.py`, `procurement_tools.py`).
- `prompts/` — prompt templates per agent.
- `main.py` exposes REST + WebSocket endpoints (`/api/ai/chat`, `/api/ai/ws`), both on the gateway whitelist.

LLM backend is Aliyun DashScope via `langchain-openai` (OpenAI-compatible API). Keep `DASHSCOPE_API_KEY` out of commits.

### Data + infra

- MySQL seed data: `infrastructure/mysql/init.sql` — stores, users (`admin`, `manager1/2`, `supplier1`, `user1`, commonly password `123456` BCrypt), sample properties. Incremental migrations under `infrastructure/mysql/migrations/`.
- Nacos is optional — Spring config imports are `optional:nacos:...`, services start standalone against `application-local.yml`.
- MinIO buckets are initialized by the `minio-init` one-shot container in `docker-compose.yml`. Public media URLs take the form `/minio/<bucket>/<object>` in prod (nginx) and dev (Vite proxy).

## Conventions worth knowing

- The Docker compose file is the authoritative source for environment wiring — prefer reading it over guessing hostnames. Inside compose, services address each other by service name (`mysql`, `redis`, `minio`, `user-service`, ...).
- Frontend API layer: paginated endpoints share `src/api/pagination.ts` helpers; generic admin lists use `useResourceList` + `api/resource.ts`. When adding a new admin list page, prefer these over ad-hoc fetching.
- Order status on the frontend is a **string** (not a numeric code) — see `src/api/service.ts`.
- JWT secret must match between gateway and any service that signs tokens (`user-service`). Env var is `JWT_SECRET`.
