# Gay Core — ai-gateway-core 原生 App

> 原生 Widget 渲染，**不用 WebView**。插件 UI = 服务端驱动的 JSON 声明（SDUI），App 原生 Material 组件渲染。

## 架构

```
插件 (服务端)                          Gay Core (App)
server.js 业务逻辑                      原生壳: 身份分流/服务器管理/动态底栏/设置
ui 端点 → gcui JSON 树 ──────────────→ SDUI 渲染器 (JSON → Material View)
intent 端点 ←── HMAC签名意图 ────────── 意图客户端 (验签/nonce/序列号/幂等)
```

- **客户端零业务状态**：整棵 UI 树都是服务端算的，App 只是渲染器 + 意图回传器
- **插件零客户端代码执行**：JSON 是数据不是代码，比 WebView 沙箱更彻底
- **主题插件只给样式令牌**：内核强制 theme 插件禁带 server.js

## 功能

**用户端**
- 添加服务器：拉 bootstrap → 插件安全验证页（权限 + SHA256 公示）→ 信任并安装 → 登录（卡密/账密）
- 动态底栏（首页/个人/设置 + 插件按键，≤5，布局服务器可配+用户可改）
- 个人中心依赖「用户系统」插件（provides: user-system）
- 布局编辑器（勾选显隐 + 上下移排序 + 恢复默认）
- 主题选择（MD3 默认锁定不可删）
- 调试区（管理员授权单用户限时开启时显示，常驻警示横幅）

**管理端**
- 添加服务器（adminKey 验证）→ 插件公示确认
- 首页：状态总览 + 审计日志预览
- 设置：服务点资料 / 为用户启用调试模式（限 UID+限时）/ 配置用户端默认布局 / 插件启停 / 市场测试模式

## 安全

- 凭据：AndroidKeyStore AES-256-GCM 加密存储
- 站点隔离：`files/sites/<siteId>/`，退出即清除
- 意图签名：HMAC-SHA256 + 时间戳窗口 + nonce 去重 + 序列号防回滚 + 幂等键
- TLS 证书错误不静默降级
- 插件市场：未开测试模式禁止自定义来源

## 构建

```bash
# Termux
cd ~/gay-core
JAVA_HOME=$PREFIX ANDROID_HOME=~/android-sdk ./gradlew :app:assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

## 测试

```bash
# JVM 单测 (22 项: 模板/布局合并/签名跨端一致性)
cd test && kotlinc GayCoreTest.kt ../app/src/main/java/com/gaycore/app/{data/Models,sdui/Template,sdui/LayoutMerger,sdui/IntentSigner}.kt -cp <gson.jar> -include-runtime -d out.jar
java -cp out.jar:<gson.jar> GayCoreTestKt
```

服务端支撑层测试：`cd ~/ai-gateway-core && node test/gaycore.test.js`（26 项）

## 设计文档

见 [DESIGN.md](./DESIGN.md)。插件开发规范见 ai-gateway-core 的 docs/PLUGIN-DESIGN.md §GC 章。
