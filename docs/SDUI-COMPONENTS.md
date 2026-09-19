# Gay Core SDUI 组件契约 (gcui v1)

> 引擎版本：Renderer v0.7.0-expanded（2026-09-11 扩组件）
> 页面 = 一棵 JSON 树。`{"gcui":1, "root":{...}}`。无脚本、无 HTML，颜色只能引用主题令牌。

## 全局规则

| 字段 | 作用 |
|------|------|
| `type` | 组件类型（白名单，未知类型会渲染成红色错误文本，不静默失败） |
| `visible` | 模板表达式，假值则不渲染（`"{{state.signed}}"`） |
| `weight` | 在 row/column 里的占比（`1` = 撑满剩余） |
| `gap` | 容器内子元素间距(dp)。**竖向容器没写 gap、且没有 `spacer` 子节点时默认 8dp**（2026-09-19 起；写了就用写的，含 `gap:0`）。横向 row 不设默认 |
| `padding` | 容器内边距(dp) |
| `color` | 仅 `$令牌` 形式，见下方令牌表 |

**模板绑定**：字符串里 `{{path}}` 取值。scope 有四层：
`state`（页面状态）/ `input`（输入框当前值）/ `item`（list 项）/ `user`（登录态）。
整串就是一个 `{{}}` 时保留原始类型（数字/布尔），供 `visible`/`checked`/`selected` 等用。

## 主题令牌（`$` 前缀）

`primary` `onPrimary` `primaryContainer` `onPrimaryContainer`
`secondary` `onSecondary` `secondaryContainer` `onSecondaryContainer`
`tertiary` `onTertiary` `tertiaryContainer` `onTertiaryContainer`
`surface` `surfaceContainerLowest` `surfaceContainer` `surfaceContainerHigh` `surfaceContainerHighest`
`background` `onSurface` `onSurfaceVariant`
`inverseSurface` `inverseOnSurface`
`outline` `outlineVariant`
`error` `onError` `errorContainer` `onErrorContainer`
`success` `successContainer` `onSuccessContainer`

## 形状令牌（`shape` 字段）

| 值 | 圆角 | 用途 |
|----|------|------|
| `pill` / `full` | 全圆 | 药丸按钮 / chip / 搜索框 |
| `extraLarge` | 16dp | 卡片（**card 默认**） |
| `large` | 12dp | 按钮 / 输入框（**button/input 默认**） |
| `medium` | 8dp | 小控件 |
| `small` | 4dp | 标签 |
| `none` | 0 | 直角 |

---

## 布局

### column / row
```json
{"type":"row","gap":8,"children":[
  {"type":"text","text":"左","weight":1},
  {"type":"chip","text":"右"}
]}
```
`row` 子元素默认垂直居中。`weight` 为 1 → 撑满剩余空间（配合空 `spacer` 可做右对齐）。

### spacer
```json
{"type":"spacer","height":12}
```
`height` 默认 8(dp)。在 row 里配 `weight` 当弹性空白用：`{"type":"spacer","weight":1}`

### divider
```json
{"type":"divider"}
```

---

## 展示

### text
```json
{"type":"text","text":"服务器名称","style":"title3","color":"$onSurfaceVariant"}
```
`style`：`title`(20sp粗) / `title3`(16sp粗) / `body`(15sp，默认) / `caption`(12.5sp) / `mono`(13sp等宽)

### icon
```json
{"type":"icon","name":"msym:search","size":24}
```
**白名单**：`home` `person` `settings` `check` `apps` `add` `close` `refresh` `key` `info`
`palette` `edit` `star` `group` `copy` `wallet` `bug` `search` `chevron` `delete`
`block` `sort` `filter` `grid` `chat` `schedule` `more`

### kv（键值行）
```json
{"type":"kv","label":"UID","value":"{{item.uid}}"}
```
label 靠左(caption 色)，value 靠右。

### badge ⭐新增
状态药丸（不可点）：
```json
{"type":"badge","text":"运行中","tone":"success"}
```
`tone`：`success`(绿) / `error`(红) / `warning`(琥珀) / `primary`(紫) / `tertiary`(粉) / `neutral`(灰，默认)

### avatar ⭐新增
```json
{"type":"avatar","text":"WF","size":44}
```
圆形，主色→第三色渐变，文字自动按 size 缩放。

### image
```json
{"type":"image","url":"{{item.avatar}}","height":120}
```
异步加载 + LruCache，无第三方依赖。

### progress
```json
{"type":"progress","value":"{{state.used}}","max":"{{state.quota}}"}
```

### card
```json
{"type":"card","variant":"outlined","shape":"extraLarge","title":"服务状态",
 "action":{"type":"intent","endpoint":"refresh"},
 "children":[ ... ]}
```
- `variant`：`filled`(默认，surfaceContainer) / `outlined`(描边卡片)
- `title`：可选标题（title3）
- `action`：点击整卡；`longAction`：长按
- **卡内默认间距 8dp**（2026-09-19）：卡本身就是竖向容器，没写 `gap` 且没用 `spacer` 时子项默认 8dp 间距 —— 依赖“子项贴着”的老页面请显式写 `gap:0`。卡内边距固定 16/14dp。

---

## 交互

### button
```json
{"type":"button","text":"保存","style":"filled","shape":"pill","icon":"msym:check"}
```
`style`：`filled`(默认/主色) / `tonal`(次色容器) / `outlined`(描边) / `text`(纯文字)

**纯图标按钮**（无文字 + 有图标 → 正方形，配 `shape:pill` 即正圆）：
```json
{"type":"button","icon":"msym:more","shape":"pill","size":36,"style":"text"}
```
`size` 默认 36dp；`iconSize` 默认 18dp。

### chip ⭐新增
药丸小按钮。**有 `selected` = 可切换**（点击本地翻转高亮 + 注入 `_selected`）；无 `selected` = 一次性按钮。
```json
{"type":"chip","text":"排序","icon":"msym:sort","selected":"{{state.sortAsc}}",
 "action":{"type":"intent","endpoint":"list","body":{"asc":"{{_selected}}"}}}
```

### segmented ⭐新增
分段控件：
```json
{"type":"segmented","selected":"{{state.tab}}",
 "options":[{"value":"users","text":"用户"},{"value":"keys","text":"卡密"}],
 "action":{"type":"intent","endpoint":"switch","body":{"tab":"{{item.value}}"}}}
```
点击：本地高亮立即切换 + 注入 `_value`；`{{item.value}}` / `{{item.text}}` 可在 action body 里用。

### input
```json
{"type":"input","key":"keyword","label":"搜索","value":"","inputType":"text",
 "shape":"pill","leadingIcon":"msym:search"}
```
- `inputType`：`text`(默认) / `number` / `password`
- `shape`：`pill` + `leadingIcon` = 搜索框
- `multiline`:`true` + `height` → 多行文本域
- 当前值可从 `{{input.<key>}}` 读到

### switch
```json
{"type":"switch","label":"启用","checked":"{{item.enabled}}",
 "action":{"type":"intent","endpoint":"toggle","body":{"on":"{{_checked}}"}}}
```

### radio
```json
{"type":"radio","text":"选项A","checked":"{{state.pickA}}","action":{...}}
```

### list
```json
{"type":"list","items":"{{state.models}}","columns":2,
 "template":{"type":"card","children":[{"type":"text","text":"{{item.name}}"}]}}
```
- `columns`：默认 1（竖向）。>1 时按等宽网格排列（Demo 模型广场的网格视图）
- `template` 里的 `{{item.xxx}}` 指向当前项

### pagination ⭐新增（2026-09-15 阶段2）
分页控件（管理端列表页刚需）：
```json
{"type":"pagination","page":"{{state.page}}","total":"{{state.total}}","pageSize":20,
 "action":{"type":"open","target":"page:/admin/ui/users?page={{_page}}","nav":"replace"}}
```
- 点击时把**目标页号**注入动作的 `{{_page}}`（同时写进 `inputs["_page"]`）
- `page` / `pageSize` / `total` 都支持模板；到边界时按钮自动禁用变灰

### checkbox ⭐新增（2026-09-15 阶段2）
勾选框（批量操作用）：
```json
{"type":"checkbox","value":"{{item.uid}}","label":"选择","checked":"{{item.picked}}","action":{...}}
```
- 勾上后 `value` 进入渲染器的多选集合 → 页面里可用 `{{selected}}`（数组）、`{{selectedCount}}`（数量）
- 动作里另外注入 `_checked`（布尔）与 `_value`

### form ⭐新增（2026-09-15 阶段2）
动态表单（把字段值打包成一个对象提交）：
```json
{"type":"form","submitText":"保存",
 "submit":{"type":"adminApi","method":"POST","path":"/admin/api/x","body":"{{form}}"},
 "fields":[{"type":"input","key":"name","label":"名称"},
           {"type":"switch","key":"enabled","label":"启用","checked":"{{state.enabled}}"}]}
```
- **只打包本表单声明过 `key` 的字段**（不会把页面上别的输入框顺手带走）
- 可打包的字段类型：`input` / `switch` / `segmented`
- 提交时这些值作为对象绑给 `{{form}}`

### chart ⭐新增（2026-09-15 阶段2）
极简图表（柱状 / 折线，纯 Canvas 自绘，零依赖）：
```json
{"type":"chart","kind":"bar|line","items":"{{state.hourly}}","y":"count","height":140}
```
- `kind` 默认 `bar`；`y` 指定取值字段名；`height` 默认 140dp
- 颜色取 `$primary`（线/柱）与 `$tertiary`（折线的点）

---

## 动作（action）

| type | 说明 |
|------|------|
| `intent` | 调插件端点。`endpoint` + `body`（body 里的模板会先绑定）；`confirm` 可加确认框；`then:"reload"`(默认) 成功后重载当前页 |
| `open` | `target:"page:/xxx"` 打开插件内页；`target:"http(s)://"` 外部浏览器 |
| `copy` | 复制 `text` 到剪贴板，`toast` 提示 |
| `close` | 关闭宿主弹窗（仅弹窗模式有效；内联页里是空操作） |
| `toggleSelect` ⭐新增（2026-09-19） | 本地增删多选集合（`{{selected}}` / `{{selectedCount}}`）并就地重渲染。给 `card.longAction` 用即可做「长按卡片 = 选中/取消」。`value` 支持模板（如 `{{item.key}}`）；集合跨原地换页保留 |

### `open` 的导航语义（`nav` 字段，2026-09-15 起）

| 情形 | 行为 |
|------|------|
| 同页面换 query（`page:square?cols=2`，当前就在 `/ui/square`） | **原地替换**，不压栈 |
| 换页面（`page:detail?model=x`） | 压栈，返回键逐级弹回 |
| `nav:"push"` | 强制压栈（给"有独立返回语义"的子视图用，如筛选页） |
| `nav:"replace"` | 强制原地替换 |
| `nav:"pop"` | 退回上一层再显示（"完成 / 返回列表"按钮用，避免多按一次返回） |

> ⚠️ 同页面换 query **不要**靠压栈：每点一次芯片就多套一层，返回键要按 N 次才出得去，
> 而且返回用的是旧页面快照，和服务端当前筛选状态会不一致（表现为"切视图点了没反应"）。
> App ≥ 0.17.16 支持 `nav`；旧版 App 忽略该字段（行为不劣化）。
> 可用请求头 `X-GCUI-Nav: push,replace,pop` 探测支持情况。

### 模板绑定时机

`action` 里的 `{{...}}` 在**点击时用该节点渲染时的作用域**解析（含 `{{item.xxx}}`）——
card / button / switch / radio / iconButton / listRow / input.submit / chip / segmented 都支持。
（App < 0.17.16 只有 chip / segmented 支持，其余组件里的 `{{item.x}}` 会静默变空串。）


## 与 HTML 原型（waterflar-ui）的对应

| 原型组件 | gcui 表达 |
|---------|-----------|
| ServerCard | `card` + `row` + `icon`/`avatar` + `text` + `badge` |
| TicketCard | `list` + `card`（`text` 用 `$primary`/`$success`/`$warning` 做圆点） |
| StatusCard（三列） | `row` + 三个 `card`（各 `weight:1`） |
| NoticeCard | `card` + `text` |
| cp-chip / mv-pill / pill | `chip` |
| seg（分段切换） | `segmented` |
| searchbox | `input` + `shape:pill` + `leadingIcon:msym:search` |
| mv-card-status（运行中/已禁用） | `badge` |
| ic-btn（圆形图标按钮） | `button`（无 text + `icon` + `shape:pill`） |
| 头像 | `avatar` |
| 模型卡 | `card` + `row`(badge + icon + text[weight] + 操作按钮) |
| **底部固定操作栏** | ⚠️ 暂缺（页面在 ScrollView 里，需架构层支持，待定） |
