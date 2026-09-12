import com.gaycore.app.data.AdminKey
import com.gaycore.app.data.AdminUser
import com.gaycore.app.data.Bootstrap
import com.gaycore.app.data.AppUi
import com.gaycore.app.data.LayoutConfig
import com.gaycore.app.data.PluginInfo
import com.gaycore.app.data.TabItem
import com.gaycore.app.data.UiRef
import com.gaycore.app.sdui.IntentSigner
import com.gaycore.app.sdui.LayoutMerger
import com.gaycore.app.sdui.Template
import com.gaycore.app.data.UserFilter
import com.google.gson.JsonParser

                                              
var passed = 0; var failed = 0
fun T(name: String, fn: () -> Unit) {
    try { fn(); passed++; println("  ✅ $name") } catch (e: Throwable) { failed++; println("  ❌ $name — ${e.message}") }
}
fun <T> eq(a: T, b: T) { if (a != b) throw AssertionError("expected=$b actual=$a") }
fun ok(v: Boolean, msg: String = "") { if (!v) throw AssertionError("assert failed $msg") }

fun main() {
    println("[1] Template 模板绑定")
    val state = JsonParser.parseString("""{"user":{"nickname":"小明","level":3},"signed":false,"quota":9500}""").asJsonObject
    val scope = mapOf<String, Any?>("state" to state, "input" to mapOf("name" to "输入值"), "item" to null)

    T("基础插值") { eq(Template.bind("你好 {{state.user.nickname}}", scope), "你好 小明") }
    T("数字插值") { eq(Template.bind("剩余 {{state.quota}}", scope), "剩余 9500") }
    T("无模板原样") { eq(Template.bind("没有模板", scope), "没有模板") }
    T("缺失路径变空串") { eq(Template.bind("[{{state.not.exist}}]", scope), "[]") }
    T("input 绑定") { eq(Template.bind("{{input.name}}", scope), "输入值") }
    T("bindRaw 整串布尔保类型") { eq(Template.bindRaw("{{state.signed}}", scope), false) }
    T("bindRaw 数字保类型") { eq(Template.bindRaw("{{state.quota}}", scope), 9500L) }
    T("bindRaw 非整串走字符串") { eq(Template.bindRaw("等级: {{state.user.level}}", scope), "等级: 3") }
    T("truthy 判定") {
        ok(!Template.truthy(false)); ok(!Template.truthy("false")); ok(!Template.truthy("")); ok(!Template.truthy(null))
        ok(!Template.truthy(0)); ok(Template.truthy(true)); ok(Template.truthy("1")); ok(Template.truthy("x"))
    }
    T("bindJson 深度替换") {
        val body = JsonParser.parseString("""{"a":"{{input.name}}","b":{"c":"{{state.quota}}"},"d":[1,"{{state.signed}}"]}""")
        val out = Template.bindJson(body, scope).asJsonObject
        eq(out.get("a").asString, "输入值")
        eq(out.getAsJsonObject("b").get("c").asString, "9500")
        ok(out.getAsJsonArray("d")[1].asString == "false")
    }

    println("[2] LayoutMerger 布局合并")
    val pluginX = PluginInfo("x", "签到X", appUi = AppUi(bottomBar = UiRef(title = "签到", icon = "check", ui = "/ui/home")))
    val pluginY = PluginInfo("y", "商店Y", appUi = AppUi(bottomBar = UiRef(title = "商店", ui = "/ui/shop")))
    val boot = Bootstrap(ok = true, plugins = listOf(pluginX, pluginY))

    T("默认布局: home/personal/settings") {
        val tabs = LayoutMerger.mergeTabs(null, null, boot)
        eq(tabs.map { it.id }, listOf("home", "personal", "settings"))
    }
    T("服务器默认布局生效") {
        val tabs = LayoutMerger.mergeTabs(LayoutConfig(1, listOf("home", "plugin:x", "settings")), null, boot)
        eq(tabs.map { it.id }, listOf("home", "plugin:x", "settings"))
        ok((tabs[1] as TabItem.Plugin).pluginId == "x")
    }
    T("用户本地覆盖优先于服务器") {
        val tabs = LayoutMerger.mergeTabs(LayoutConfig(1, listOf("home", "plugin:x", "settings")), LayoutConfig(1, listOf("home", "settings")), boot)
        eq(tabs.map { it.id }, listOf("home", "settings"))
    }
    T("hidden 过滤") {
        val tabs = LayoutMerger.mergeTabs(LayoutConfig(1, listOf("home", "personal", "settings"), hidden = listOf("personal")), null, boot)
        eq(tabs.map { it.id }, listOf("home", "settings"))
    }
    T("插件缺失→占位不崩") {
        val tabs = LayoutMerger.mergeTabs(LayoutConfig(1, listOf("home", "plugin:gone", "settings")), null, boot)
        eq(tabs.size, 3)
        eq(tabs[1].title, "缺失")
    }
    T("超5裁剪") {
        val tabs = LayoutMerger.mergeTabs(LayoutConfig(1, listOf("home", "personal", "plugin:x", "plugin:y", "settings")), null, boot)
        ok(tabs.size <= 5, "tabs=${tabs.size}")
        ok(tabs.any { it.id == "home" }, "home 保底")
        ok(tabs.any { it.id == "settings" }, "settings 保底")
    }
    T("保底: 配置缺 home/settings 自动补") {
        val tabs = LayoutMerger.mergeTabs(LayoutConfig(1, listOf("plugin:x")), null, boot)
        eq(tabs[0].id, "home")
        ok(tabs.any { it.id == "settings" })
    }
    T("validate 超5拒绝") {
        ok(LayoutMerger.validate(listOf("1", "2", "3", "4", "5", "6")) != null)
        ok(LayoutMerger.validate(listOf("1", "2", "3")) == null)
    }
    T("homeOrder: 排序+未声明补上") {
        val order = LayoutMerger.mergeHomeOrder(
            LayoutConfig(1, homeOrder = listOf("y", "x")), null,
            Bootstrap(ok = true, plugins = listOf(
                PluginInfo("x", "x", appUi = AppUi(home = UiRef(ui = "/ui/a"))),
                PluginInfo("y", "y", appUi = AppUi(home = UiRef(ui = "/ui/b"))),
                PluginInfo("z", "z", appUi = AppUi(home = UiRef(ui = "/ui/c"))),
            )))
        eq(order, listOf("y", "x", "z"))
    }

    println("[3] 意图签名跨端一致性 (与服务端 plugins.js verifyIntent 对拍)")
    T("固定向量: node 侧计算值一致") {
        val sign = IntentSigner.sign("sk-gc-main", 1789040000000, "aabbccdd", 7, "test-intent-1", "POST", "/plugins/intent-test/intent/ping", """{"msg":"hi"}""")
        eq(sign, "901c59437985dee4b9c76216da9e685a74a66dcb129fadd9c2e10b3c86804e05")
    }
    T("sha256 格式") { eq(IntentSigner.sha256Hex("abc"), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad") }
    T("方法大写归一") {
        val a = IntentSigner.sign("k", 1, "n", 1, "i", "post", "/p", "")
        val b = IntentSigner.sign("k", 1, "n", 1, "i", "POST", "/p", "")
        eq(a, b)
    }

    println("[4] 用户/卡密搜索过滤 UserFilter")
    run {
        val u1 = AdminUser(uid = "alice", name = "Alice", nickname = "小艾", email = "a@x.com")
        val u2 = AdminUser(uid = "bob", name = "Bob")
        val k1 = AdminKey(key = "sk-abcd1234", name = "主卡", uid = "alice")
        val k2 = AdminKey(key = "sk-zzzz9999", name = "子卡", uid = "bob")
        val ko = AdminKey(key = "sk-orphan00", name = "野生卡", uid = "")
        val users = listOf(u1, u2); val keys = listOf(k1, k2, ko); val orphans = listOf(ko)

        T("空查询返回全部") { val r = UserFilter.filter(users, keys, orphans, ""); eq(r.users.size, 2); eq(r.orphanKeys.size, 1) }
        T("按 uid 命中") { eq(UserFilter.filter(users, keys, orphans, "ali").users, listOf(u1)) }
        T("按昵称命中") { eq(UserFilter.filter(users, keys, orphans, "小艾").users, listOf(u1)) }
        T("按邮箱命中") { eq(UserFilter.filter(users, keys, orphans, "a@x").users, listOf(u1)) }
        T("按卡号命中持卡人") {
            val r = UserFilter.filter(users, keys, orphans, "zzzz")
            eq(r.users, listOf(u2)); eq(r.orphanKeys.size, 0)
        }
        T("按卡名命中持卡人") { eq(UserFilter.filter(users, keys, orphans, "子卡").users, listOf(u2)) }
        T("孤儿卡按卡号命中") {
            val r = UserFilter.filter(users, keys, orphans, "orphan")
            eq(r.users.size, 0); eq(r.orphanKeys, listOf(ko))
        }
        T("孤儿卡按卡名命中") { eq(UserFilter.filter(users, keys, orphans, "野生").orphanKeys, listOf(ko)) }
        T("无匹配") { val r = UserFilter.filter(users, keys, orphans, "不存在"); eq(r.users.size, 0); eq(r.orphanKeys.size, 0) }
        T("大小写不敏感") { eq(UserFilter.filter(users, keys, orphans, "SK-ABCD").users, listOf(u1)) }
        T("额度标签") { eq(AdminKey(quotaTokens = -1).quotaLabel(), "零额度"); eq(AdminKey(quotaTokens = 0).quotaLabel(), "不限"); eq(AdminKey(quotaTokens = 50000).quotaLabel(), "5.0万") }
        T("卡号脱敏") { eq(AdminKey(key = "sk-abcdefgh12345678").masked(), "sk-abcde…5678") }
    }

    println("\n=== 结果: $passed 通过, $failed 失败 ===")
    if (failed > 0) kotlin.system.exitProcess(1)
}
