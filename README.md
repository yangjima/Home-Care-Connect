# 居服通 Home Care Connect

> 以门店/公寓为锚点的本地社区综合服务平台。一站式承接**房源租售、上门服务、本地商城、跳蚤市场**四大业务，并内置基于 LangGraph 的**多智能体 AI 助手**，用语义理解替代传统关键词匹配，帮用户把"我想租两千左右的房""我要买可乐"直接转化为精准检索与页面跳转。

- **后端**：Spring Boot 3 + Spring Cloud Gateway + Java 21 微服务 · FastAPI + LangGraph AI 服务
- **前端**：Vue 3 + TypeScript + Vite + Element Plus + Pinia
- **中间件**：MySQL · Redis · MinIO · Nacos（可选）
- **一键部署**：Docker Compose + Makefile

---

## 目录

- [业务能力一览](#业务能力一览)
- [用户角色与权限](#用户角色与权限)
- [技术架构](#技术架构)
- [AI 助手的设计亮点](#ai-助手的设计亮点)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [本地开发](#本地开发)
- [默认账号](#默认账号)
- [API 路由约定](#api-路由约定)
- [常用命令](#常用命令)

---

## 业务能力一览

### 1. 房源（租售 + 看房）

- 房源多条件检索：关键词、价格区间、行政区、户型、朝向、装修
- 房源上下架、编辑、图片上传（MinIO）
- 在线预约看房，全流程状态跟踪（待确认 / 已确认 / 已完成 / 已取消）
- 「我的房源」（房东视角）与「我的看房」（租客视角）双入口
- 平台管理员可置顶/推荐，商家仅能管理自己发布的房源

### 2. 社区上门服务

- 服务类型管理：保洁、维修、开锁、陪护、搬家、做饭、家电清洗……
- 服务人员档案（技能、评分、排班）
- 下单 → 派单 → 上门 → 完成 → 评价全链路
- 订单按角色过滤：用户只看自己；门店长/管理员看全局
- 评价模块：星级 + 文字 + 追评

### 3. 本地商城（采购）

- 商品管理、库存管理（按门店维度）
- 商家（supplier）自助上架，平台审核后上线
- 按关键词、价格区间检索
- 下单与订单查询

### 4. 跳蚤市场（二手闲置）

- C2C 发布二手物品：成色、图片、描述、地点
- 「我的发布」管理、下架
- 平台审核机制（上架审批页）
- 公开浏览 + 详情页，登录后才可发布或联系卖家

### 5. AI 智能助手 ⭐

AI 助手不是"聊天机器人"，而是**业务路由 + 结构化检索**的自然语言入口：

- 🧠 **纯语义意图识别**（无关键词列表）：
  - "我要买可乐" → 跳本地商城，搜索"可乐"
  - "租两千左右的房" → 跳房源列表，`minPrice=1500 & maxPrice=2500`
  - "家里灯不亮了" → 跳服务列表，类型=维修
  - "有没有二手冰箱" → 跳跳蚤市场，关键词=冰箱
- 📦 **结构化槽位抽取**：价格、户型、片区、服务类型、商品品类等自动入参
- 🚦 **严格检索约束**：模型只能基于平台实际数据推荐，空结果时如实告知并建议放宽条件——**绝不编造"北京上海的房源"之类的平台外数据**
- 🌊 **流式对话**：HTTP + WebSocket 双通道，逐 token 推送
- 🗂️ **会话持久化**：Redis / 内存两种 checkpoint 后端
- 🔀 **一键跳转**：意图识别完成后返回 `redirect` URL，前端可直接跳对应列表/详情并预填筛选

### 6. 多租户门店

- 门店（store）作为组织维度，承载员工、房源、商品、订单
- 门店长（store_manager）管理自己门店下的全部资源
- 商家（supplier）挂靠在门店下，独立管理自己的发布

### 7. 数据看板

- 管理员首页看板：房源数、订单数、GMV、服务完成率、用户活跃等
- 按角色差异化展示（平台 vs 门店 vs 商家）

---

## 用户角色与权限

| 角色             | 职责                     | 可访问后台页面                                   |
| ---------------- | ------------------------ | ------------------------------------------------ |
| `admin`          | 平台超管                 | 全部                                             |
| `store_manager`  | 门店长（视同平台管理员） | 全部                                             |
| `supplier`       | 商家 / 房东              | 数据看板、房源管理、商品管理                     |
| `tenant` / `user`| 普通用户                 | 无后台，仅个人中心（订单、看房、发布、资料）     |

- 公开注册**仅能**得到 `user` 角色；`supplier` 与管理员角色由平台管理员后台指派（`PATCH /api/user/users/{id}/role`）
- 所有公开浏览页（房源、服务、商品、二手）**仅对 GET 开放匿名访问**，任意写操作必须登录
- JWT 验签**只在网关**完成，下游服务通过 `X-User-Id / X-User-Role / X-User-Store-Id` 请求头获取身份

---

## 技术架构

```
┌─────────────┐
│  Browser    │  Vue 3 + Element Plus
└──────┬──────┘
       │ /api/*  /minio/*
       ▼
┌─────────────────────────────────────┐
│  Spring Cloud Gateway (:8080)       │  ← JWT 鉴权 · 路由转发 · CORS
└─┬────────┬────────┬────────┬──────┬─┘
  │        │        │        │      │
  ▼        ▼        ▼        ▼      ▼
┌─────┐ ┌──────┐ ┌──────┐ ┌─────┐ ┌────────────┐
│user │ │prop. │ │ser.  │ │asset│ │ai-service  │
│8081 │ │8082  │ │order │ │8083 │ │(FastAPI    │
│     │ │      │ │8082  │ │     │ │ +LangGraph)│
└──┬──┘ └──┬───┘ └──┬───┘ └──┬──┘ └─────┬──────┘
   │       │        │        │          │
   └───────┴────┬───┴────────┘          │ 工具调用
                ▼                        │
         ┌──────────────┐                │
         │ MySQL / Redis│ ◄──────────────┘
         │ / MinIO      │
         └──────────────┘
```

### 路由映射（网关）

| 前缀                | 下游服务              | StripPrefix |
| ------------------- | --------------------- | ----------- |
| `/api/user/**`      | user-service          | 2           |
| `/api/property/**`  | property-service      | 2           |
| `/api/service/**`   | service-order-service | 2           |
| `/api/asset/**`     | asset-service         | 2           |
| `/api/ai/**`        | ai-service            | 0（保留前缀）|

### 关键设计

- **鉴权唯一入口**：JWT 只在网关校验，下游服务无条件信任请求头，杜绝身份伪造与漏洞面
- **资源级 RBAC**：在每个服务的 Controller/Service 层做资源归属校验（如"商家只能改自己的房源"），与网关角色白名单解耦
- **AI 服务独立**：FastAPI 而非 Spring，换用 LangGraph 专业编排多智能体；通过 HTTP 回调 Java 服务做真实检索
- **Nacos 可选**：配置中心/注册中心均为 `optional:` 导入，纯本地开发可直接起

---

## AI 助手的设计亮点

居服通的 AI 助手**不是提示词工程堆出来的聊天窗**，而是一套可组合的多智能体图。

### 图结构

```
[START] ──► router ──┬─► property_agent ──┐
                     ├─► service_agent ────┤
                     ├─► procurement_agent ┼─► response_agent ──► [END]
                     └─► (general) ────────┘
```

### router：从关键词匹配到 LLM 语义理解

传统做法是写一堆关键词列表去 `in` 匹配——"我要买可乐"因为词表里没有"可乐"就识别不出购物意图。居服通的 router 用 **Pydantic + `with_structured_output`** 让 LLM 直接输出结构化决策：

```python
class RouterOutput(BaseModel):
    intent: Literal["property", "service", "procurement", "general"]
    confidence: float
    sub_action: Literal["list", "detail", "book", "my"]
    filters: RouterFilters  # keyword / minPrice / maxPrice / district /
                            # bedrooms / serviceType / category / ...
```

Prompt 里**不**给关键词列表，改为描述**业务边界 + 判断原则 + few-shot 边界场景**，让 LLM 真的做语义理解。关键词兜底**仅在 LLM 调用异常时**启用，作为降级路径。

### Agent：硬约束 prompt + 结构化检索

下游 agent 的 system prompt 有两条铁律：

1. **只能基于平台真实检索结果回复**——严禁编造不在结果里的房源/商品
2. 检索返回 `EMPTY_RESULT:` 时**如实告知**并建议放宽条件，不推荐不相关内容凑数

工具层接受 router 抽出的 `filters`，按结构化字段（`keyword / minPrice / maxPrice / district / category`）调各业务服务的列表 API，而不是把整句用户输入塞进 `keyword`——这样"租两千左右"不会因为匹配不到"租两千"字面量而全库未命中。

### 跳转联动

AI 返回结果中带 `redirect` 字段，前端根据它直接跳转到对应列表页并预填筛选参数，例如：

- "长安区三千以下的一室一厅" → `/properties?maxPrice=3000&district=长安区&bedrooms=1`
- "有没有二手冰箱"           → `/secondhand?keyword=冰箱`

---

## 目录结构

```
Home-Care-Connect/
├─ frontend/                          Vue 3 SPA
│  ├─ src/
│  │  ├─ api/         # 各业务的 axios 封装
│  │  ├─ stores/      # Pinia（auth / property / ai / ...）
│  │  ├─ composables/ # useResourceList, useAdminForm
│  │  ├─ views/
│  │  │  ├─ home/ auth/ property/ service/ asset/ ai/
│  │  │  ├─ user/     # 个人中心
│  │  │  └─ admin/    # 后台
│  │  └─ router/
│  └─ vite.config.ts
├─ services/
│  ├─ gateway/                  Spring Cloud Gateway + JwtAuthFilter
│  ├─ user-service/             注册、登录、用户、门店、验证码
│  ├─ property-service/         房源、看房预约
│  ├─ service-order-service/    服务类型、服务人员、订单、评价
│  ├─ asset-service/            商城商品、二手物品
│  └─ ai-service/               FastAPI + LangGraph
│     ├─ agents/     # router / property / service / procurement / response
│     ├─ tools/      # property_tools / service_tools / procurement_tools
│     ├─ graph/      # chat_graph, state, checkpoint
│     └─ prompts/
├─ infrastructure/
│  ├─ docker/docker-compose.yml   # 一站式编排
│  ├─ mysql/init.sql              # 初始库、种子数据
│  ├─ mysql/migrations/           # 增量迁移
│  └─ nacos/
├─ Makefile
└─ CLAUDE.md                      为 AI 协作准备的工程说明
```

---

## 快速开始

### 前置条件

- Docker Desktop 24+（含 Docker Compose v2）
- 如需本地开发：Node 18+、JDK 21、Python 3.11、Maven 3.9+

### 一键启动（Docker）

```bash
# 1. 启动全部服务（构建 + 启动）
make up

# 2. 查看状态
make ps

# 3. 查看网关日志
make logs-svc SVC=gateway
```

启动完成后：

- 前端：http://localhost （或 http://localhost:5173 本地 dev 时）
- 网关：http://localhost:8080
- MinIO 控制台：http://localhost:9001
- AI 服务：http://localhost:8000

### 仅启动基础设施 / 仅启动服务

```bash
make infra       # mysql + redis + minio + nacos
make services    # 网关 + 各业务服务 + 前端
```

---

## 本地开发

### 前端（`frontend/`）

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173，/api 自动代理到网关
npm run type-check
npm run test         # Vitest
npm run build
```

可选环境变量：

- `VITE_DEV_HTTPS=true` 启用本地 HTTPS（`@vitejs/plugin-basic-ssl`）
- `VITE_LOGIN_RSA_PUBLIC_KEY=<PEM>` 启用登录密码 RSA-OAEP 加密（需与后端 `LOGIN_RSA_PRIVATE_KEY` 配对）

### Java 服务（`services/`）

每个模块都支持 `application-local.yml` 本地覆盖（已 gitignore）：

```bash
cd services

# 单模块构建（含依赖）
mvn -pl user-service -am package

# 单模块跑测试
mvn -pl user-service test

# 单测试类
mvn -pl user-service -Dtest=JwtAuthFilterTest test
```

### AI 服务（`services/ai-service/`）

```bash
cd services/ai-service
pip install -r requirements.txt

export DASHSCOPE_API_KEY=sk-xxx   # 阿里云百炼
uvicorn main:app --host 0.0.0.0 --port 8000

pytest                            # 跑 router / chat_graph 等单元测试
```
---

## API 路由约定

- 所有业务走网关 `/api/<service>/**`，`StripPrefix=2`；下游 Controller 感知不到前缀
- AI 路由 `StripPrefix=0`，FastAPI 路径自身保留 `/api/ai/...`
- 媒体访问：`/minio/<bucket>/<object>`（容器/Vite 都通过代理回源 MinIO）
- 网关白名单：登录 / 注册 / 验证码 / 健康检查 / 公开的 **GET** 列表（房源、服务、商品、二手）+ AI 入口
- 写操作全部需 JWT

---

## 常用命令

```bash
make up                         # 构建 + 启动全部
make down                       # 停止全部
make ps                         # 服务状态
make logs                       # 跟踪所有日志
make logs-svc SVC=gateway       # 跟踪单个服务
make clean                      # 停止 + 删除数据卷（谨慎）
make frontend-build             # 前端构建（本地，不走 Docker）
```

---

## 许可证

MIT。详见 [LICENSE](./LICENSE)。
