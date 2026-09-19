# 管理端 SDUI 化方案（草案 v1 · 2026-09-15）

> 状态：**待用户确认**，尚未改动任何代码。
> 目标版本线：gay-core v0.18.x（起始 versionCode 133 → 之后每次部署递增）

---

## 0. 目标与非目标

**目标**

1. 管理端界面由**服务端 gcui JSON** 驱动 —— 改界面不再需要重编 APK。
2. 第三方插件能提供**原生风格的管理界面**（不再只能 WebView 加载自己的 HTML）。
3. 核心业务页最终也可搬到服务端（按阶段推进）。
4. **全程保留原生实现作为回退保底**，任何一页出问题都能一键切回。

**非目标**

- 不删除 `AdminPages.kt` / `AdminMainActivity.kt` 的原生实现（它们是保底）。
- 不改变现有 `/admin/api/*` 的语义与鉴权方式。
- 不发明新的鉴权协议。

---

## 1. 现状（附证据）

| 部分 | 实现 | 位置 |
|---|---|---|
| 用户端 | ✅ SDUI（gcui v1） | `sdui/Renderer.kt`、`ui/SduiController.kt` |
| 管理端菜单 | ❌ 硬编码 12 项 | `AdminMainActivity.kt:258 shellItems()` |
| 管理端页面分发 | ❌ 硬编码 `when(id)` | `AdminMainActivity.kt:278 buildPage()` |
| 管理端页面实现 | ❌ Kotlin 拼 View，2694 行 | `ui/AdminPages.kt` |
| 插件管理页 | ⚠️ 白名单 4 个插件走硬编码原生，其余 WebView | `AdminPages.kt:1656 canBuildNative()`、`1661 buildPluginWeb()` |

当前插件管理页的三条腿：

```kotlin
// AdminMainActivity.kt:404
if (!pages.canBuildNative(pid)) pages.buildPluginWeb(pid, adminPage, card)  // WebView
else pages.buildNativePlugin(pid, card)                                     // 硬编码 Kotlin
// 白名单：auth-user / auth-cardkey / tunnel / proxy
```

---

## 2. 关键事实（决定方案走向）

1. **管理端鉴权 = `x-admin-key` 头**，不是用户 token 签名。
   `src/auth.js:156 checkAdmin()` 支持 Bearer / `x-admin-key` / `?adminKey=` / cookie。
   App 侧 `Api.adminGet/adminPost`（`Api.kt:110-111`）已带 `x-admin-key`。
   → **管理端 SDUI 的读写通道不需要发明新协议**，比用户端那套 HMAC 意图简单得多。

2. **用户端 SDUI 的 intent 用卡密 HMAC 签名**（`IntentClient.doSigned` → 服务端
   `plugins.js:502 verifyIntent`）。管理端**不复用**这条链路。

3. **数据源**：内核 `/admin/api/*`（实例/配置/插件/市场）+ 插件 `/plugins/<id>/admin/*`
   （用户、卡密、渠道…）。

4. **引擎能力缺口**：现有 24 组件 / 6 动作，**缺**表格、分页、搜索框、批量多选、
   动态表单、图表、流式聊天 —— 这些正是管理端重交互页（用户管理 / 使用 / PlayGround）依赖的。

---

## 3. 架构设计

### 3.1 管理端 SDUI 模式（App 侧）

`SduiController` 增加 `mode`：

```kotlin
enum class Mode { USER, ADMIN }
```

| | USER（现状） | ADMIN（新增） |
|---|---|---|
| 请求头 | `Authorization: Bearer <token>` | `x-admin-key: <adminKey>` |
| 路径前缀 | `<base>/plugins/<id>` | `<adminBase>` |
| 写动作 | `intent`（HMAC 签名） | `adminApi`（GET/POST，走 adminKey） |
| 页面来源 | `/plugins/<id>/ui/*` | `/admin/ui/*` |

新增动作：

```json
{"type":"adminApi","method":"POST","path":"/admin/api/instance/1/enable",
 "body":{"enable":true},"confirm":"确认切换？","then":"reload"}
```

### 3.2 权限边界（安全核心）

**按页面来源裁剪可用动作**，防止第三方插件借管理端通道越权：

| 页面来源 | 允许的动作 |
|---|---|
| 内核页 `/admin/ui/*` | `adminApi` / `open` / `copy` / `toast` / `refresh` / `setState` / `intent` |
| 插件页 `/plugins/<id>/ui/*` | `intent`（只能打自己插件）/ `open` / `copy` / `toast` / `refresh` / `setState` — **禁止 `adminApi`** |

服务端渲染页面时必须对 `adminKey`、上游 API key、卡密等字段做脱敏（可复用
`src/redact-cache.js` 的思路）。

### 3.3 菜单动态化

服务端 bootstrap 增加 `adminUi` 注册点：

```json
{"adminUi":{"version":1,"pages":[
  {"id":"channels","title":"渠道","icon":"apps","ui":"/admin/ui/channels"},
  {"id":"instances","title":"实例","icon":"grid","builtin":true}
]}}
```

App 侧规则：

- 菜单 = **内置保底清单** + 服务端 `adminUi.pages` 覆盖（按 `id` 匹配）。
- 某页有 `ui` 且「服务端界面」开关开启 → SDUI 渲染；否则 → 原生 `buildPage(id)`。
- `builtin:true` 强制走原生。

### 3.4 回退保底（三重，用户明确要求）

| 层 | 机制 | 行为 |
|---|---|---|
| ① 本地总开关 | 设置页新增「管理端使用服务端界面」 | 关闭 = **全原生**，立即生效，无需联网 |
| ② 页级 | 服务端未提供 `ui` / 开关关闭 / `builtin:true` | 该页走原生 |
| ③ 运行时降级 | 加载失败、3s 超时、JSON 非法、`gcui` 版本过高、渲染异常 | 自动切该页原生 + toast 提示；该页 5 分钟内不再重试 |

**原生实现全程保留**，是三层回退的物理基础。

---

## 4. 阶段计划

| 阶段 | 内容 | App 重编 | 风险 | 产出 |
|---|---|---|---|---|
| **1 框架** | ADMIN 模式 + `adminApi` 动作 + 菜单动态化 + 三重回退 + 服务端 `adminUi` 注册点与 `/admin/ui` 路由（先只挂 1 个测试页） | 1 次 | **低** | 通道打通，核心页仍全原生 |
| **2 引擎扩容** | 新增 `searchBar` / `pagination` / `table` / `checkbox` / `actionSheet` / `form` / `chart` / 流式输出 | 1 次 | 中 | 管理端页面所需的组件齐备 |
| **3 简单页迁移** | 公告、主题、实例、设置 | 0 | 低 | 首批真实页面跑在服务端 |
| **4 中等页迁移** | 渠道、模型分组、插件、使用 | 0 | 中 | — |
| **5 复杂页迁移** | 用户/卡密、PlayGround、首页 | 0 | 高 | 完成 |

> 阶段 2 之后**不需要再重编 App**（这正是本方案的核心收益）。

每阶段验证流程（沿用既有规矩）：

```
沙箱改 → bash /workspace/gc-cp/check.sh（类型检查）
       → 沙箱逻辑测试（真实服务端数据）
       → 手机 grep 三步对齐
       → 攒够一批 → 重编 APK（升 versionCode + versionName）
       → 真机验证 → grep 版本号核对
```

---

## 5. 风险与对策

| 风险 | 对策 |
|---|---|
| 管理端白屏 → 无法运维 | 三重回退 + 原生实现不删 + 开关默认保守 |
| adminKey 混进页面 JSON | 服务端渲染时脱敏；App 侧二次扫描越权字段 |
| 插件借管理端 SDUI 越权调用 admin API | 3.2 的来源裁剪白名单 |
| 引擎扩容引入回归（用户端也被影响） | gc-cp 类型检查 + 样机页（UiDemoActivity）+ 用户端页面回归抽查 |
| 两套 UI 逻辑并存导致行为不一致 | 每迁一页，服务端页面成为唯一真源；原生页冻结不改 |

---

## 6. 待确认的决策点

1. **本地总开关的默认值**：
   - (a) 默认**关**（最保守：装完还是原生，手动开一页试一页）
   - (b) 默认**开**（体验优先：服务端给了 ui 就用）
2. **插件管理页是否允许 `adminApi`**：建议**否**（插件只能 intent 打自己）。
3. **阶段 1 的服务端测试页**选哪一页：建议「公告」（只读、结构最简单）。
4. 「设置」页里的危险操作（改 adminKey、删实例）是否纳入 SDUI 迁移范围：
   建议**永不迁移**，长期保留原生。
