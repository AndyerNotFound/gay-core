package com.gaycore.app.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaycore.app.App
import com.gaycore.app.R
import com.gaycore.app.data.Api
import com.gaycore.app.data.Bootstrap
import com.gaycore.app.data.KeyStoreCrypto
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.data.ServerStore
import com.gaycore.app.theme.ThemeEngine
import com.gaycore.app.ui.UiKit.add
import com.gaycore.app.ui.UiKit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddServerActivity : BaseActivity() {

    
    private val theme: ThemeEngine by lazy {
        val st = App.of(this).store
        val last = st.list().maxByOrNull { e -> e.addedAt }
        val list = last?.bootstrap()?.themes ?: emptyList()
        val info = list.firstOrNull { it.id == st.lastThemeId() } ?: list.firstOrNull()
        ThemeEngine(info, st.lastDarkMode(), st.lastDynamicColors(), st.lastCustomSeed())
    }
    override fun tintTheme(): ThemeEngine? = theme

    private lateinit var container: LinearLayout
    private var mode = "user"

    private var baseUrl = ""
    private var branch = "default"
    private var bootstrap: Bootstrap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra("mode") ?: "user"
        theme.applyToWindow(this)
        container = UiKit.column(this)
        


        container.setPadding(
            UiKit.dp(this, 16f), UiKit.dp(this, 14f),
            UiKit.dp(this, 16f), UiKit.dp(this, 48f),
        )
        container.setBackgroundColor(theme.color(this, "background"))
        val scrollRoot = UiKit.scroll(this, container)
        setContentView(scrollRoot)
        stepUrl()
    }

    private fun show(step: String, build: LinearLayout.() -> Unit) {
        container.removeAllViews()
        container.add(UiKit.header(this, theme, getString(R.string.add_server), step))
        try {
            container.build()
        } catch (e: Throwable) {
            
            container.add(UiKit.text(this, "构建出错: " + e.javaClass.simpleName + ": " + (e.message ?: ""), 13f, true, theme.color(this, "error")), 8)
        }
    }

    
    private fun stepUrl() {
        show(getString(R.string.next) + " 1/3") {
            val (cv, inner) = UiKit.card(this@AddServerActivity, theme)
            val et = MdField(this@AddServerActivity, theme, getString(R.string.server_url_hint))
            inner.add(UiKit.text(this@AddServerActivity, getString(R.string.server_url), 13f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")))
            inner.add(et, 6)
            add(cv, 8)
            val err = UiKit.text(this@AddServerActivity, "", 13f, false, theme.color(this@AddServerActivity, "error"))
            add(err, 6)
            val btn = UiKit.button(this@AddServerActivity, theme, getString(R.string.next))
            btn.setOnClickListener {
                val u = ServerStore.normalizeUrl(et.text.toString())
                btn.isEnabled = false; err.text = ""
                val oldText = btn.text
                btn.text = getString(R.string.connecting)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val b = Api.bootstrap(u)
                        if (!b.ok || b.appApi < 1) throw Exception(getString(R.string.not_gaycore_server))
                        baseUrl = u; bootstrap = b
                        withContext(Dispatchers.Main) { stepVerify() }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { err.text = getString(R.string.fetch_failed, e.message) }
                    } finally {
                        withContext(Dispatchers.Main) { btn.isEnabled = true; btn.text = oldText }
                    }
                }
            }
            add(btn, 14)
        }
    }

    
    private fun stepVerify() {
        val b = bootstrap ?: return stepUrl()
        show(getString(R.string.next) + " 2/3") {
            
            b.serverInfo?.let { si ->
                val (cv, inner) = UiKit.card(this@AddServerActivity, theme)
                inner.add(UiKit.text(this@AddServerActivity, si.name?.ifBlank { b.branch } ?: b.branch, 19f, true, theme.color(this@AddServerActivity, "onSurface")))
                si.description?.takeIf { it.isNotBlank() }?.let { inner.add(UiKit.text(this@AddServerActivity, it, 13.5f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")), 4) }
                
                inner.add(UiKit.text(this@AddServerActivity, "自定义名称", 13f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")), 8)
                etName = MdField(
                    this@AddServerActivity, theme, "名称",
                    si.name?.ifBlank { b.branch } ?: b.branch,
                )
                inner.add(etName!!, 4)
                add(cv, 8)
            }
            
            val (cv2, inner2) = UiKit.card(this@AddServerActivity, theme, getString(R.string.security_verify))
            inner2.add(UiKit.text(this@AddServerActivity, getString(R.string.security_verify_desc), 12.5f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")))
            for (p in b.plugins) {
                val pv = LinearLayout(this@AddServerActivity).apply { orientation = LinearLayout.VERTICAL }
                pv.add(UiKit.text(this@AddServerActivity,
                    "${p.name} v${p.version}" + (if (p.builtin) "  [${getString(R.string.plugin_builtin)}]" else "") + if (p.author.isNotBlank()) "  by ${p.author}" else "",
                    15f, true, theme.color(this@AddServerActivity, "onSurface")))
                if (p.description.isNotBlank()) pv.add(UiKit.text(this@AddServerActivity, p.description, 12.5f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")), 2)
                pv.add(UiKit.text(this@AddServerActivity,
                    if (p.permissions.isEmpty()) getString(R.string.plugin_no_permissions)
                    else getString(R.string.plugin_permissions, p.permissions.joinToString(", ")),
                    12f, false, theme.color(this@AddServerActivity, if (p.permissions.isEmpty()) "onSurfaceVariant" else "error")), 3)
                pv.add(UiKit.text(this@AddServerActivity, getString(R.string.plugin_sha, p.sha256.take(24) + "…"), 11f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")), 2)
                inner2.add(pv, 10)
            }
            if (b.plugins.isEmpty()) inner2.add(UiKit.text(this@AddServerActivity, "(无启用插件)", 13f), 8)
            add(cv2, 8)

            val row = LinearLayout(this@AddServerActivity).apply { orientation = LinearLayout.HORIZONTAL }
            val cancel = UiKit.button(this@AddServerActivity, theme, getString(R.string.cancel), "text")
            cancel.setOnClickListener { finish() }
            val ok = UiKit.button(this@AddServerActivity, theme, getString(R.string.install_trust))
            ok.setOnClickListener { if (mode == "admin") stepAdminKey() else stepLogin() }
            row.addView(cancel)
            row.addView(ok)
            add(row, 14)
        }
    }

    
    private fun stepLogin() {
        val b = bootstrap ?: return stepUrl()
        show(getString(R.string.next) + " 3/3 · " + getString(R.string.login)) {
            
            add(UiKit.text(this@AddServerActivity,
                "诊断: cardkey=${b.auth.cardkey} user=${b.auth.user} 分支=${b.branches.size} 插件=${b.plugins.size}",
                11f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")), 4)

            



            if (b.branches.size > 1) {
                val (cv, inner) = UiKit.card(this@AddServerActivity, theme, getString(R.string.branches))
                var selIdx = b.branches.indexOfFirst { it.name == b.branch }.let { if (it >= 0) it else 0 }
                val refs = ArrayList<Triple<LinearLayout, View, TextView>>()
                fun paintSel() {
                    refs.forEachIndexed { i, r ->
                        val row = r.first; val dot = r.second; val tv = r.third
                        val sel = i == selIdx
                        row.background = if (sel)
                            Md3.shape(this@AddServerActivity, theme.color(this@AddServerActivity, "secondaryContainer"), 14)
                        else
                            Md3.shape(this@AddServerActivity, theme.color(this@AddServerActivity, "surfaceContainerHighest"), 14)
                        dot.background = if (sel)
                            Md3.shape(this@AddServerActivity, theme.color(this@AddServerActivity, "primary"), 999)
                        else
                            Md3.shape(this@AddServerActivity, Color.TRANSPARENT, 999, 2f, theme.color(this@AddServerActivity, "outlineVariant"))
                        tv.setTextColor(theme.color(this@AddServerActivity, if (sel) "onSecondaryContainer" else "onSurface"))
                        tv.setTypeface(if (sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT)
                    }
                }
                b.branches.forEachIndexed { i, br ->
                    val row = LinearLayout(this@AddServerActivity)
                    row.orientation = LinearLayout.HORIZONTAL
                    row.gravity = Gravity.CENTER_VERTICAL
                    val padH = dp(this@AddServerActivity, 14)
                    row.setPadding(padH, dp(this@AddServerActivity, 10), padH, dp(this@AddServerActivity, 10))
                    val dot = View(this@AddServerActivity)
                    dot.layoutParams = ViewGroup.LayoutParams(dp(this@AddServerActivity, 16), dp(this@AddServerActivity, 16))
                    row.addView(dot)
                    val tv = UiKit.text(this@AddServerActivity, br.name, 15f, false, theme.color(this@AddServerActivity, "onSurface"))
                    row.add(tv, 10)
                    row.isClickable = true
                    row.isFocusable = true
                    row.setOnClickListener {
                        selIdx = i
                        branch = br.name
                        paintSel()
                    }
                    inner.add(row, 8)
                    refs.add(Triple(row, dot, tv))
                }
                branch = b.branches[selIdx].name
                paintSel()
                add(cv, 8)
            }

            if (!b.auth.cardkey && !b.auth.user) {
                add(UiKit.centerText(this@AddServerActivity, getString(R.string.no_auth), theme.color(this@AddServerActivity, "onSurfaceVariant")), 8)
                
                val btn = UiKit.button(this@AddServerActivity, theme, getString(R.string.install_trust))
                btn.setOnClickListener { finishSave(null) }
                add(btn, 14)
                return@show
            }

            
            fun plainCard(titleText: String): LinearLayout {
                val c = LinearLayout(this@AddServerActivity)
                c.orientation = LinearLayout.VERTICAL
                c.background = Md3.shape(this@AddServerActivity, theme.color(this@AddServerActivity, "surfaceContainer"), 20)
                c.setPadding(UiKit.dp(this@AddServerActivity, 16f), UiKit.dp(this@AddServerActivity, 14f), UiKit.dp(this@AddServerActivity, 16f), UiKit.dp(this@AddServerActivity, 14f))
                c.add(UiKit.text(this@AddServerActivity, titleText, 16f, true, theme.color(this@AddServerActivity, "onSurface")))
                return c
            }

            val cv = plainCard(getString(R.string.login_cardkey))
            val etKey = MdField(this@AddServerActivity, theme, getString(R.string.cardkey_hint))
            cv.add(etKey)
            val btnKey = UiKit.button(this@AddServerActivity, theme, getString(R.string.login))
            btnKey.setOnClickListener {
                btnKey.isEnabled = false
                btnKey.text = getString(R.string.connecting)
                finishSave(etKey.text.toString().trim().ifEmpty { null })
            }
            cv.add(btnKey, 10)
            add(cv, 8)

            
            add(
                UiKit.button(this@AddServerActivity, theme, "跳过登录 · 先以游客身份浏览", "text").apply {
                    setOnClickListener { finishSave(null) }
                },
                14,
            )

            if (b.auth.user) {
                val cv2 = plainCard(getString(R.string.login_account))
                val etUid = MdField(this@AddServerActivity, theme, getString(R.string.uid_hint))
                val etPw = MdField(this@AddServerActivity, theme, getString(R.string.password_hint)).apply {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                cv2.add(etUid)
                cv2.add(
                    UiKit.text(this@AddServerActivity, getString(R.string.login_id_hint), 11f, false, theme.color(this@AddServerActivity, "onSurfaceVariant")),
                    4,
                )
                cv2.add(etPw, 8)
                val err = UiKit.text(this@AddServerActivity, "", 13f, false, theme.color(this@AddServerActivity, "error"))
                cv2.add(err, 6)
                val btnLogin = UiKit.button(this@AddServerActivity, theme, getString(R.string.login), "tonal")
                btnLogin.setOnClickListener {
                    btnLogin.isEnabled = false
                    val oldLoginText = btnLogin.text
                    btnLogin.text = getString(R.string.connecting)
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val r = Api.login(baseUrl, branch, etUid.text.toString().trim(), etPw.text.toString())
                            val keys = r.getAsJsonArray("keys")
                            val main = keys?.firstOrNull { it.asJsonObject.get("isMain")?.asBoolean == true } ?: keys?.firstOrNull()
                            val key = main?.asJsonObject?.get("key")?.asString ?: throw Exception("该账号没有卡密")
                            val serverUid = r.get("uid")?.takeIf { !it.isJsonNull }?.asString ?: ""
                            withContext(Dispatchers.Main) { finishSave(key, serverUid) }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { err.text = getString(R.string.login_failed, e.message) }
                        } finally { withContext(Dispatchers.Main) { btnLogin.isEnabled = true; btnLogin.text = oldLoginText } }
                    }
                }
                cv2.add(btnLogin, 10)
                
                val btnRotate = UiKit.button(this@AddServerActivity, theme, "主卡泄漏？凭密码轮换", "text")
                btnRotate.setOnClickListener {
                    val rid = etUid.text.toString().trim()
                    val rpw = etPw.text.toString()
                    if (rid.isEmpty() || rpw.isEmpty()) { err.text = "请先填写用户名和密码再轮换"; return@setOnClickListener }
                    UiKit.confirm(this@AddServerActivity, "轮换主卡",
                        "确认轮换主卡？\n· 旧主卡(含泄漏的)立即失效\n· 新主卡保留原额度与权限\n· 此操作不可撤销", theme) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val r = Api.post("$baseUrl/auth/rotate-main", com.google.gson.JsonObject().apply {
                                    addProperty("username", rid); addProperty("password", rpw)
                                })
                                if (r.get("ok")?.takeIf { it.isJsonPrimitive }?.asBoolean != true) {
                                    val m = r.get("error")?.takeIf { it.isJsonPrimitive }?.asString ?: "轮换失败"
                                    withContext(Dispatchers.Main) { err.text = m }
                                    return@launch
                                }
                                val newKey = r.get("key").asString
                                val rUid = r.get("uid")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                withContext(Dispatchers.Main) {
                                    val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("gaycore-key", newKey))
                                    UiKit.toast(this@AddServerActivity, "主卡已轮换，新卡已复制，正在用新卡登录")
                                    finishSave(newKey, rUid)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { err.text = "轮换失败: ${e.message}" }
                            }
                        }
                    }
                }
                cv2.add(btnRotate, 4)
                
                if (b.auth.registration?.enable == true) {
                    val btnReg = UiKit.button(this@AddServerActivity, theme, getString(R.string.register_tab), "text")
                    btnReg.setOnClickListener { stepRegister() }
                    cv2.add(btnReg, 4)
                }
                add(cv2, 8)
            }
        }
    }

    
    private fun stepRegister() {
        val b = bootstrap ?: return stepUrl()
        show(getString(R.string.register_tab)) {
            val diagColor = theme.color(this@AddServerActivity, "onSurfaceVariant")
            add(UiKit.text(this@AddServerActivity,
                "诊断: cardkey=${b.auth.cardkey} user=${b.auth.user} 注册=${b.auth.registration?.enable}",
                11f, false, diagColor), 4)

            
            fun plainCard(titleText: String): LinearLayout {
                val c = LinearLayout(this@AddServerActivity)
                c.orientation = LinearLayout.VERTICAL
                c.background = Md3.shape(this@AddServerActivity, theme.color(this@AddServerActivity, "surfaceContainer"), 20)
                c.setPadding(UiKit.dp(this@AddServerActivity, 16f), UiKit.dp(this@AddServerActivity, 14f), UiKit.dp(this@AddServerActivity, 16f), UiKit.dp(this@AddServerActivity, 14f))
                c.add(UiKit.text(this@AddServerActivity, titleText, 16f, true, theme.color(this@AddServerActivity, "onSurface")))
                return c
            }

            val cv = plainCard(getString(R.string.register_tab))
            val etUser = MdField(this@AddServerActivity, theme, getString(R.string.register_username_hint))
            cv.add(etUser)
            val etPw = MdField(this@AddServerActivity, theme, getString(R.string.register_pw_hint)).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            cv.add(etPw, 8)
            val etPw2 = MdField(this@AddServerActivity, theme, getString(R.string.register_pw2_hint)).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            cv.add(etPw2, 8)
            val err = UiKit.text(this@AddServerActivity, "", 13f, false, theme.color(this@AddServerActivity, "error"))
            cv.add(err, 6)
            val btnReg = UiKit.button(this@AddServerActivity, theme, getString(R.string.register_btn))
            btnReg.setOnClickListener {
                val u = etUser.text.toString().trim()
                val p1 = etPw.text.toString()
                val p2 = etPw2.text.toString()
                if (u.isEmpty()) { err.text = "请输入用户名"; return@setOnClickListener }
                if (p1.length < 8) { err.text = "密码至少8位"; return@setOnClickListener }
                if (p1 != p2) { err.text = getString(R.string.pw_mismatch); return@setOnClickListener }
                btnReg.isEnabled = false
                val oldText = btnReg.text
                btnReg.text = getString(R.string.connecting)
                err.text = ""
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val r = Api.register(baseUrl, branch, u, p1)
                        val key = r.get("key")?.asString
                        if (key.isNullOrEmpty()) throw Exception("注册成功但未返回卡密")
                        val serverUid = r.get("uid")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        withContext(Dispatchers.Main) {
                            UiKit.toast(this@AddServerActivity, getString(R.string.register_success))
                            finishSave(key, serverUid)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            err.text = getString(R.string.register_failed, e.message)
                            btnReg.isEnabled = true; btnReg.text = oldText
                        }
                    }
                }
            }
            cv.add(btnReg, 10)
            
            val btnBack = UiKit.button(this@AddServerActivity, theme, getString(R.string.back), "text")
            btnBack.setOnClickListener { stepLogin() }
            cv.add(btnBack, 4)
            add(cv, 8)
        }
    }

    
    private fun stepAdminKey() {
        show(getString(R.string.next) + " 3/3 · " + getString(R.string.i_am_admin)) {
            val (cv, inner) = UiKit.card(this@AddServerActivity, theme)
            val et = MdField(this@AddServerActivity, theme, getString(R.string.admin_key)).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            inner.add(et)
            val err = UiKit.text(this@AddServerActivity, "", 13f, false, theme.color(this@AddServerActivity, "error"))
            inner.add(err, 6)
            val btn = UiKit.button(this@AddServerActivity, theme, getString(R.string.verify_admin))
            btn.setOnClickListener {
                btn.isEnabled = false
                val oldText = btn.text
                btn.text = getString(R.string.connecting)
                val key = et.text.toString().trim()
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        
                        val tmp = ServerEntry(ServerStore.entryId(baseUrl, "admin", null), baseUrl, "admin", adminKeyEnc = KeyStoreCrypto.encrypt(key))
                        Api.adminGet(tmp, "/admin/api/status")
                        withContext(Dispatchers.Main) { finishSave(null, adminKey = key) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { err.text = getString(R.string.admin_verify_failed, e.message) }
                    } finally { withContext(Dispatchers.Main) { btn.isEnabled = true; btn.text = oldText } }
                }
            }
            inner.add(btn, 10)
            add(cv, 8)
        }
    }

    private var etName: MdField? = null

    private fun finishSave(token: String?, uid: String = "", adminKey: String? = null) {
        val b = bootstrap ?: return
        val store = App.of(this).store
        val siteId = ServerStore.entryId(baseUrl, mode, token)
        val customName = etName?.text?.toString()?.takeIf { it.isNotBlank() }
        val serverName = customName ?: b.serverInfo?.name?.takeIf { it.isNotBlank() } ?: b.branch
        val existing = store.get(siteId)
        val e = if (existing != null) {
            
            if (token != null) existing.tokenEnc = KeyStoreCrypto.encrypt(token)
            if (adminKey != null) existing.adminKeyEnc = KeyStoreCrypto.encrypt(adminKey)
            if (uid.isNotEmpty()) existing.uid = uid
            if (customName != null) existing.name = customName
            existing.branch = branch
            existing
        } else {
            ServerEntry(
                id = siteId, baseUrl = baseUrl, mode = mode,
                name = serverName, branch = branch,
                tokenEnc = token?.let { KeyStoreCrypto.encrypt(it) },
                adminKeyEnc = adminKey?.let { KeyStoreCrypto.encrypt(it) },
                uid = uid,
            )
        }
        store.save(e)
        store.updateBootstrap(e.id, b)
        if (mode == "admin") store.currentAdminServer = e.id else store.currentUserServer = e.id
        val cls = if (mode == "admin") AdminMainActivity::class.java else UserMainActivity::class.java
        startActivity(android.content.Intent(this, cls).putExtra("serverId", e.id))
        finish()
    }
}
