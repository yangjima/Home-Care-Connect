# 居服通（Home Care Connect）— 项目说明

本文档描述 **Home-Care-Connect** 仓库的整体定位、技术架构、目录结构，以及 **角色权限（RBAC）** 与 **系统后台 / 个人中心** 等近期实现要点。详细设计见仓库内 `docs/设计文档.md`，部署见 `docs/部署指南.md`。

---

## 1. 项目是什么

**居服通（Home Care Connect）** 是一个以公寓/门店为节点的本地化平台，MVP 阶段聚焦：

- **房源**：展示、预约看房、房东/商家上架维护（与门店场景结合）。
- **社区服务**：服务类型、下单、订单状态流转（保洁、维修等）。
- **资产 / 本地商城**：采购商品（本地商城）、二手跳蚤市场。
- **AI 助手**：对话入口，意图路由到房源 / 服务 / 采购等能力（见设计文档 AI 部分）。

技术形态为 **微服务 + 统一网关 + Vue 3 单页应用**，数据层 MySQL，缓存 Redis，对象存储 MinIO，可选 Nacos 做注册与配置。

---

## 2. 系统架构一览

```
用户浏览器
    → API Gateway（JWT 校验，注入 X-User-Id / X-User-Role）
        → user-service（用户、JWT、角色）
        → property-service（房源、看房预约）
        → service-order-service（服务类型、订单、评价、人员展示）
        → asset-service（采购商品、二手物品）
        → ai-service（对话与 Agent，按需）
```

| 组件 | 技术 | 默认职责 |
|------|------|-----------|
| API Gateway | Spring Cloud Gateway | 路由、JWT、白名单（公开 GET 等） |
| User Service | Spring Boot | 注册登录、用户信息、用户列表与角色维护（管理端） |
| Property Service | Spring Boot | 房源 CRUD、媒体上传、看房预约与状态 |
| Service Order Service | Spring Boot | 服务订单、确认/完成/支付等流程接口 |
| Asset Service | Spring Boot | 采购商品、二手发布与列表 |
| AI Service | Langgraph + FastAPI | 多 Agent 对话 |
| Frontend | Vue 3 + Vite + Pinia + Element Plus | 用户端 + 系统后台 |

基础设施常见组合：**MySQL、Redis、MinIO**；本地/容器编排可参考 `infrastructure/docker/docker-compose.yml`。

---

## 3. 仓库目录（高层）

| 路径 | 说明 |
|------|------|
| `services/gateway` | 网关：JWT 解析，向下游传递用户头 |
| `services/user-service` | 用户与认证 |
| `services/property-service` | 房源与看房 |
| `services/service-order-service` | 服务订单与相关接口 |
| `services/asset-service` | 采购与二手 |
| `services/ai-service` | AI 服务 |
| `frontend` | Vue 3 前端工程 |
| `infrastructure` | Docker、MySQL 初始化脚本等 |
| `docs` | 设计文档、部署指南等 |
| `prototypes` | HTML 原型（含店长后台、用户个人页等） |

---

## 4. 前端应用结构

- **用户端**：首页、房源列表/详情、服务列表/下单、本地商城、跳蚤市场、AI、登录注册等。
- **个人中心**（`/user/profile`）：参考原型 `prototypes/group1-user-pages/03-profile.html`，含渐变头图、快捷入口、常用功能；资料编辑与改密码在弹窗中完成。
- **用户子站**（`UserLayout`，`/user/*`）：侧栏含个人中心、我的订单、我的看房、跳蚤市场发布；**商家/管理员**额外显示「我的房源」「系统后台」入口。
- **系统后台**（`/admin/*`）：采用统一左侧菜单布局，包含：
  - 数据看板 `/admin/dashboard`
  - 房源管理 `/admin/properties`
  - 商品管理 `/admin/products`
  - 添加商品 `/admin/products/new`
  - 订单管理 `/admin/orders`
  - 服务管理 `/admin/services`
  - 添加服务 `/admin/services/new`
  - 员工管理 `/admin/staff`

路由守卫：需登录；进入 `/admin` 需具备后台角色；**商家**仅允许「数据看板 + 房源管理」，访问订单/服务/员工子路由会被重定向到看板。

前端角色常量：`frontend/src/constants/roles.ts`。  
新增/相关 API 示例：`frontend/src/api/users.ts`（用户列表、改角色）、`property.ts`（上架/下架/推荐）、`asset.ts`（采购商品维护）、`service.ts`（订单列表 `status` 为字符串）。

---

## 5. 角色与权限（RBAC）

角色与数据库表 **`sys_user.role`**（MySQL ENUM）及 JWT 中的 **`role` 声明**一致，避免与现有初始化数据冲突。

| 业务称呼 | `role` 字段值 | 说明 |
|----------|----------------|------|
| 超级管理员 | `admin` | 全平台管理权限（后台全菜单、用户与角色、订单处理、推荐房源、采购商品维护等） |
| 店长 | `store_manager` | 与 `admin` 在权限模型上等价（平台侧统一称「平台管理员」） |
| 商家 | `supplier` | 可发布/维护**自有房源**、维护**本地商城采购商品**；后台仅开放看板 + 房源；其余能力与租户一致走用户端 |
| 普通用户 | `tenant` / `user` | 浏览与预约、下单、发布二手等；不能管理他人订单/全站用户；不能写采购商品 |

**注册**：公开注册仅允许普通角色（`tenant` / `user`），不可自选 `admin` 等特权角色。  
**赋权**：平台管理员通过 **`PATCH /api/user/users/{id}/role`** 修改他人角色（需携带 JWT，网关注入头）。

### 5.1 网关与下游

- 网关校验 JWT 后向下游设置：**`X-User-Id`**、**`X-Username`**、**`X-User-Role`**。
- 各业务服务从请求头读取当前操作者，做资源级校验（禁止仅靠「默认 userId=1」冒充登录）。

### 5.2 各服务要点（实现摘要）

**property-service**

- 创建房源：仅 `admin`、`store_manager`、`supplier`。
- 更新/删除/上架/下架：房源 **owner** 或平台管理员。
- **推荐房源**：仅平台管理员。
- 看房：列表按角色过滤（管理员看全量；商家需带 `propertyId` 且为房源 owner；普通用户仅看自己的预约）；详情与状态变更按「预约人 / 房东 / 管理员」组合校验。

**service-order-service**

- 订单列表：普通用户强制只看本人；管理员与商家可看全量列表（商家侧后续可按门店细化）。
- 订单详情 / 支付 / 取消：本人或管理员；**确认、完成、删除订单**仅平台管理员。

**asset-service**

- 采购商品 **POST/PUT/DELETE/PATCH 库存**：仅 `admin`、`store_manager`、`supplier`。  
- 二手发布等仍面向已登录用户（与普通用户能力一致）。

**user-service**

- `GET /users`、用户状态变更、`PATCH /users/{id}/role`：**平台管理员**。
- `GET/PUT /users/{id}`：本人或平台管理员。

---

## 6. 初始化与测试账号

MySQL 初始化脚本：`infrastructure/mysql/init.sql`（含门店、用户、房源样例等）。

示例用户（密码与脚本中 BCrypt 一致，常见为 **`123456`**，请以你本地 `docs/部署指南.md` 或脚本注释为准）：

| 用户名 | 角色 | 用途 |
|--------|------|------|
| `admin` | 超级管理员 | 全后台 + 用户角色分配 |
| `manager1` / `manager2` | 店长 | 与 admin 同级后台能力 |
| `supplier1` | 商家 | 房源 + 采购商品 + 用户端 |
| `user1` | 普通用户 | 租户端行为验证 |

---

## 7. 本地开发提示

- 前端：`frontend` 目录执行 `npm install`、`npm run dev` / `npm run build`；API 基地址见 `VITE_API_BASE_URL`（默认常指向 `/api` 经网关代理）。
- 后端：各 `services/*` 为独立 Maven 模块；需本机 JDK、Maven（或 IDE）构建。
- 网关白名单包含登录注册等路径；部分 **GET** 房源/服务/商品接口可无 JWT，写操作需 Token。

### 7.1 登录密码“密文传输”（可选）

默认情况下，前端登录请求会发送明文密码（**仍建议使用 HTTPS/TLS** 来保护传输）。如果你希望在应用层将密码以密文传输，可以启用 **RSA-OAEP(SHA-256)**：

- **前端**：设置环境变量 `VITE_LOGIN_RSA_PUBLIC_KEY`（PEM 格式公钥，`-----BEGIN PUBLIC KEY-----` ...）。设置后，`POST /api/user/auth/login` 的 `password` 会发送为 `rsa_oaep_sha256:<base64>`。
- **后端（user-service）**：设置环境变量 `LOGIN_RSA_PRIVATE_KEY`（PKCS#8 PEM 私钥，`-----BEGIN PRIVATE KEY-----` ...）。后端会在检测到前缀 `rsa_oaep_sha256:` 时自动解密后再进行密码校验；未带前缀则按旧逻辑兼容处理。

---

## 8. 相关文档与原型

| 资源 | 路径 |
|------|------|
| 详细设计 | `docs/设计文档.md` |
| 系统后台联调清单 | `docs/系统后台联调清单.md` |
| 部署说明 | `docs/部署指南.md` |
| 用户个人页原型 | `prototypes/group1-user-pages/03-profile.html` |
| 店长后台原型 | `prototypes/group1-user-pages/01-dashboard.html` 等 |

---

## 9. 版本说明（文档维护）

本 `read.md` 随仓库演进可继续补充：**环境变量清单、OpenAPI 入口、各服务端口表、与 Nacos 配置键对照** 等。若与 `docs/设计文档.md` 冲突，以设计文档与代码为准，并建议将差异记在本节。

---

*文档生成说明：涵盖当前仓库内微服务划分、前端「居服通」用户端与店长后台、以及基于 `sys_user.role` 的 RBAC 与网关透传头约定。*
