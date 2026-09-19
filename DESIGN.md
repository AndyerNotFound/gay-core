# Gay Core — ai-gateway-core 原生 App 前端设计（定稿）

> 版本: v1 草案定稿 2026-09-10
> 核心决策: **不用 WebView**。插件 UI = JSON 声明（SDUI），App 原生 Widget 渲染。
> 安全模型按《APP插件系统设计_补充.md》落地：客户端不可信 / 意图 vs 状态 / 分层审查 / 主题令牌化。

---

## 1. 架构总览

```
ai-gateway-core (服务端)                    Gay Core (Android App)
├─ 插件 server.js  ── 业务逻辑/状态          ├─ 原生壳: 身份分流/服务器管理/底栏/设置
├─ 插件 UI 端点    ── 返回 gcui JSON 树  ──→ ├─ SDUI 渲染器: JSON → Material View
├─ 插件 intent 端点←── POST 签名意图 ──────  ├─ 意图客户端: HMAC签名+nonce+序列号
├─ bootstrap API   ── 服务点/插件公示 ───→   ├─ 安全验证页: 插件清单+权限+SHA256
└─ 审计日志        ── 启用/停用/布局/调试     └─ 凭据: AndroidKeyStore 加密, 站点隔离
```

原则：**App 是"渲染器 + 意图回传器"，没有任何业务状态**。状态全部在服务端。

## 2. GCUI Schema v1（`"gcui": 1`）

### 2.1 页面结构

```jsonc
{
  "gcui": 1,
  "title": "每日签到",            // 可选, App 标题栏
  "state": { "signed": false },   // 可选, 供模板绑定 {{state.xxx}}
  "root": { "type": "column", "children": [ ... ] }
}
```

### 2.2 组件集（v1 固定 14 种，插件不能发明新类型）

| type | 关键字段 | 原生映射 |
|---|---|---|
| `column` | children, gap, padding | LinearLayout(V) |
| `row` | children, gap, gravity | LinearLayout(H) |
| `card` | children, title? | MaterialCardView |
| `text` | text, style(title/body/caption/mono), color? | TextView |
| `image` | url, width?, height? | ImageView (异步加载+缓存) |
| `icon` | name(msym:xxx 仅预置映射表) | ImageView(矢量) |
| `progress` | value, max | LinearProgressIndicator |
| `kv` | label, value | 行: label+value |
| `divider` | — | View 1dp |
| `spacer` | height | Space |
| `button` | text, icon?, style(filled/tonal/outlined/text), action | MaterialButton |
| `input` | key, label, hint?, type(text/number/password), value? | TextInputLayout |
| `switch` | key, label, checked?, action? | MaterialSwitch |
| `list` | items="{{state.list}}", template(子树, 绑定 {{item.xxx}}) | LinearLayout 循环 |

**边界**：组件属性只能是字符串/数字/布尔/嵌套组件；无脚本、无 URL 加载任意 HTML、无样式注入（颜色只能从主题令牌引用 `"$primary"` 等，禁止裸 hex——防止插件视觉欺骗）。

### 2.3 模板绑定

字符串值里支持 `{{state.path.to.field}}` `{{input.<key>}}` `{{item.xxx}}`（list 内）`{{user.uid}}` `{{user.nickname}}`（App 注入的登录态）。
渲染时替换；input 的值在提交意图时收集。

### 2.4 动作 action

```jsonc
// 提交意图 (核心): POST 到插件端点, 服务端重算
{ "type": "intent", "endpoint": "./checkin", "method": "POST",
  "body": { "note": "{{input.note}}" },     // 可选, 额外参数
  "confirm": "确定签到吗?",                    // 可选, 弹确认
  "then": "reload" }                          // reload(默认)|toast|none
// 打开页面/链接
{ "type": "open", "target": "page:xxx" }      // 插件声明的另一页
{ "type": "open", "target": "https://..." }   // 外部链接(系统浏览器)
// 复制
{ "type": "copy", "text": "{{state.key}}", "toast": "已复制" }
```

**意图响应**（服务端返回）：
```jsonc
{ "ok": true, "toast": "签到成功 +1000", "ui": { ...新树... }, "state": {...} }
// ui 存在 → App 整树替换渲染; 只有 toast → 提示后 reload
```

### 2.5 UI 来源（manifest 声明）

```jsonc
"appUi": {
  "personal":  { "title": "个人", "icon": "person", "ui": "/ui/personal" },   // 能力页
  "home":      { "title": "签到", "icon": "check",  "ui": "/ui/home" },        // 首页组件
  "bottomBar": { "id": "signin", "title": "签到", "icon": "check", "ui": "/ui/home" },
  "settings":  { "title": "签到设置", "ui": "/ui/settings" },                  // 用户可见设置
  "pages":     [ { "id": "rank", "title": "排行榜", "icon": "list", "ui": "/ui/rank" } ]
}
```

- `ui` 为相对路径 → App 请求 `GET /<分支>/plugins/<id><ui>`，插件 `ctx.registerRoute('GET', '/ui/personal', ...)` 动态返回 gcui JSON（可含静态文件 `pages/personal.gcui.json` 由内核直接服务）。
- **鉴权**：App 带 `Authorization: Bearer <卡密>`；插件端点用 `params.token` + `ctx.gateway.findKey` 校验（现状不变）。

## 3. 意图安全协议 v1（落地补充文档 §7）

App 提交意图时：

| Header | 值 |
|---|---|
| `Authorization` | `Bearer <卡密>`（唯一凭据，不落业务字段） |
| `X-GC-Timestamp` | 毫秒时间戳（±5 分钟窗口） |
| `X-GC-Nonce` | 16 字节随机 hex |
| `X-GC-Seq` | 单调递增序列号（per 站点×卡×插件实例） |
| `X-GC-Intent-Id` | UUID（幂等键） |
| `X-GC-Sign` | `HMAC_SHA256(卡密, ts+"\n"+nonce+"\n"+seq+"\n"+intentId+"\n"+METHOD+"\n"+path+"\n"+sha256(body))` |

内核提供 `plugins.js` → `ctx.security.verifyIntent(req, params)`：
1. 验签（token 即密钥，服务端独立计算比对）
2. 时间戳窗口校验
3. nonce 去重（内存 LRU + 落盘）
4. 序列号防回滚（per key 存 lastSeq）
5. 幂等键冲突 → 返回上次缓存响应
6. 插件实例归属校验（endpoint 路径里的插件 id 必须=当前插件）

存储：`plugins-data/<uid>/.gc-security.json`（nonce/seq/idempotency 记录，定期裁剪）。

> v1 说明：签名基于"卡密即共享密钥"。管理端卡密泄露场景与现状一致（卡密本身就能发请求），签名防的是**重放/篡改/回滚**，符合 §7 目标。

## 4. 布局系统（§13）

```jsonc
// 服务端默认布局 (admin API 可改, schemaVersion 强制)
{ "version": 1, "bottomBar": ["home","personal","settings"], "hidden": [],
  "homeOrder": [], "theme": "theme-md3" }
```

- 底栏项 id：`home` `personal` `settings` 为内置；插件项 = `plugin:<id>`（底栏上限 **5**，超出 App/服务端都拒绝保存）。
- **合并策略**：服务端下发为基线，用户本地存 patch（`{move:[],hide:[],show:[],theme?}`），App 合并：基线 → 应用 hide/show → 应用 move。用户"恢复默认"= 清空 patch。
- **插件缺失三级降级**：占位卡片（提示缺失）→（用户点隐藏）→（回退）默认布局。
- 设置项 key 带插件命名空间：`<pluginId>.<key>`。

## 5. 多站点与数据隔离（§9）

- App 内每个服务器 = 独立命名空间：`files/sites/<sha256(baseUrl+cfgUid)>/`（prefs、本地布局 patch、本地主题、缓存）。
- 隔离键 = **站点 × 卡密 × 插件实例**（序列号/nonce 按此维度）。
- 凭据：卡密用 **AndroidKeyStore** AES-GCM 加密后存 SharedPreferences（系统 API 零依赖）。
- 退出服务器 → 删除整个站点目录 + Keystore 条目。
- TLS：HTTPS 证书错误**禁止静默降级**（OkHttp 默认行为即拒绝；不提供"忽略证书"开关）。

## 6. 审查与来源（§2/§10）

- bootstrap 公示所有启用插件：id/name/version/author/**permissions**/**sha256**/builtin → App「添加服务器」第二步整页展示，用户确认才保存。
- 服务端 `cfg.marketTestMode`（默认 **false**）：false 时管理 API 拒绝添加自定义插件索引/自定义 zipURL 安装（官方索引预留常量）；true 放行（现状），开关动作写审计。
- 分层：签名(预留字段) + 权限明示 + 社区标记(预留 `warnings` 字段) + 吊销列表(预留)。
- 主题插件：内核强制 `type:"theme"` 不得 `hasServer`（带 server.js 拒绝加载）——纯令牌。

## 7. 调试模式硬约束（§15）

- 仅管理端可开：`POST /admin/api/client-config/:uid {userDebug:{uid, minutes}}`。
- **限定单个用户 uid + 限时自动失效**（服务端存 `{uid, until}`，bootstrap 判定）。
- App 开启期间：所有页面顶部常驻警示横幅。
- 开/关写审计日志。

## 8. 审计日志（§14）

内核写 `log/audit-<uid>.jsonl`：`{ts, actor, action, detail}`。
v1 覆盖动作：plugin.enable/disable/install/uninstall、layout.save、serverInfo.save、userDebug.on/off、market.testMode.on/off。

## 9. App 页面地图

```
Welcome(身份分流)
├─ 我是用户 → ServerList(已加入服务器, 默认展开) + 添加服务器
│    └─ 添加流程: 输URL → GET bootstrap → 插件安全验证页(权限+SHA256) → 登录(卡密/账密) → 完成
│    └─ UserMain: 动态底栏(≤5): 首页 | 个人* | 插件按键... | 设置
│         首页=服务器信息头+插件home组件(原生卡片流)
│         个人=provides:user-system 插件的 appUi.personal 页(无→占位提示)
│         设置=切换服务器/插件管理/布局编辑/主题/调试区(授权时)/关于
├─ 我是管理员 → AdminServerList + 添加(URL+adminKey → 验证 → 插件公示确认)
│    └─ AdminMain: 首页(状态/实例) | 设置(服务点资料/用户调试模式/默认布局/插件管理/审计查看)
└─ PluginPageActivity: 通用 SDUI 页面容器(渲染+意图回传+失败提示)
```

## 10. 分期

- **v1.0**：GCUI 渲染器(14 组件)、意图签名协议、身份分流、服务器管理、动态底栏+布局合并、主题令牌引擎、KeyStore 凭据、站点隔离、服务端全部支撑 API、auth-user 个人中心 SDUI 化、审计、调试模式。
- **v1.1**：插件包签名校验（Ed25519）、社区标记源、吊销列表、图片加载缓存策略优化。
- **v1.2**：灰度发布、性能监控面板、举报通道。
