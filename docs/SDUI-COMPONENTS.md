# Gay Core SDUI 组件契约 (gcui v1)

> 引擎版本：Renderer v0.7.0-expanded（2026-09-11 扩组件）
> 页面 = 一棵 JSON 树。`{"gcui":1, "root":{...}}`。无脚本、无 HTML，颜色只能引用主题令牌。

## 全局规则

| 字段 | 作用 |
|------|------|
| `type` | 组件类型（白名单，未知类型会渲染成红色错误文本，不静默失败） |
| `visible` | 模板表达式，假值则不渲染（`"{{state.signed}}"`） |
| `weight` | 在 row/column 里的占比（`1` = 撑满剩余） |
| `gap` | 容器内子元素间距(dp) |
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

---

## 动作（action）

| type | 说明 |
|------|------|
| `intent` | 调插件端点。`endpoint` + `body`（body 里的模板会先绑定）；`confirm` 可加确认框；`then:"reload"`(默认) 成功后重载当前页 |
| `open` | `target:"page:/xxx"` 打开插件内页（压栈）；`target:"http(s)://"` 外部浏览器 |
| `copy` | 复制 `text` 到剪贴板，`toast` 提示 |

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
