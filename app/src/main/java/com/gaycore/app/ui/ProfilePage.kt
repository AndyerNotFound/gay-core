package com.gaycore.app.ui

import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaycore.app.App
import com.gaycore.app.R
import com.gaycore.app.data.Api
import com.gaycore.app.data.KeyStoreCrypto
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.theme.ThemeEngine
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext










object ProfilePage {

    fun build(
        act: AppCompatActivity,
        theme: ThemeEngine,
        server: ServerEntry,
        host: LinearLayout,
        asAdmin: Boolean,
        onLogout: (() -> Unit)? = null,
        onOpenSiteInfo: (() -> Unit)? = null,
    ) {
        val headers = if (asAdmin) Api.adminUserHeaders(server) else Api.authHeaders(server)
        host.removeAllViews()
        DemoKit.put(host, DemoKit.txt(act, theme, "读取中…", 13f, false, "onSurfaceVariant"))
        act.lifecycleScope.launch(Dispatchers.IO) {
            
            val me = try { Api.get(server.baseUrl + "/auth/me", headers) } catch (_: Exception) { null }
            val credits = try { Api.get(server.baseUrl + "/credits", headers) } catch (_: Exception) { null }
            withContext(Dispatchers.Main) {
                render(act, theme, server, host, asAdmin, headers, me, credits, onLogout, onOpenSiteInfo)
            }
        }
    }

    

    private fun render(
        act: AppCompatActivity,
        theme: ThemeEngine,
        server: ServerEntry,
        host: LinearLayout,
        asAdmin: Boolean,
        headers: Map<String, String>,
        me: JsonObject?,
        credits: JsonObject?,
        onLogout: (() -> Unit)?,
        onOpenSiteInfo: (() -> Unit)?,
    ) {
        host.removeAllViews()
        val uid = me?.str("uid") ?: ""
        val username = me?.str("username").orEmpty()
        val nick = me?.str("nickname").orEmpty().ifEmpty { username }.ifEmpty { if (asAdmin) "管理员" else "未命名用户" }
        val avatar = me?.str("avatar") ?: ""
        val role = if (asAdmin) "管理员" else "用户"

        
        val head = DemoKit.panel(act, theme, 16)
        val hrow = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        hrow.addView(
            DemoKit.avatarAuto(act, theme, avatar, nick.take(1).uppercase(), 56),
            LinearLayout.LayoutParams(DemoKit.dp(act, 56), DemoKit.dp(act, 56)),
        )
        val ht = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(ht, DemoKit.txt(act, theme, nick, 19f, true))
        val sub = StringBuilder()
        if (username.isNotEmpty() && username != nick) sub.append(username)
        if (uid.isNotEmpty()) { if (sub.isNotEmpty()) sub.append(" · "); sub.append("UID ").append(uid) }
        DemoKit.put(ht, DemoKit.txt(act, theme, sub.toString().ifEmpty { "—" }, 12f, false, "onSurfaceVariant"), 4)
        val badges = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        badges.addView(DemoKit.badge(act, theme, role, if (asAdmin) "primary" else "success"))
        val unlimited = credits?.bool("unlimited") ?: false
        if (credits != null) {
            badges.addView(
                DemoKit.badge(act, theme, if (unlimited) "不限额度" else "额度 " + fmt(credits.dbl("remainingTokens")), if (unlimited) "success" else "neutral"),
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
            )
        }
        if (avatar.isNotEmpty()) {
            badges.addView(
                DemoKit.badge(act, theme, "已设头像", "neutral"),
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
            )
        }
        DemoKit.put(ht, badges, 8)
        hrow.addView(ht, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 12) })
        DemoKit.put(head, hrow)
        DemoKit.put(host, head, 2)

        
        val cred = DemoKit.panel(act, theme, 16)
        DemoKit.put(cred, DemoKit.txt(act, theme, "凭据", 16f, true))
        DemoKit.put(cred, DemoKit.valueRow(act, theme, "服务点", server.baseUrl), 12)
        DemoKit.put(cred, DemoKit.valueRow(act, theme, "分支", server.branch), 10)
        val key = if (asAdmin) server.adminKey().orEmpty() else server.token().orEmpty()
        if (key.isNotEmpty()) {
            val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(
                DemoKit.txt(act, theme, if (asAdmin) "管理密钥" else "卡密", 12.5f, false, "onSurfaceVariant"),
                LinearLayout.LayoutParams(DemoKit.dp(act, 64), ViewGroup.LayoutParams.WRAP_CONTENT),
            )
            row.addView(DemoKit.txt(act, theme, mask(key), 13.5f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(DemoKit.chip(act, theme, "复制", R.drawable.ic_copy) {
                DemoKit.copy(act, "key", key)
                UiKit.toast(act, act.getString(R.string.copied))
            })
            DemoKit.put(cred, row, 12)
        }
        if (asAdmin) {
            DemoKit.put(
                cred,
                DemoKit.txt(act, theme, "管理密钥同时可作为用户密钥使用（不限额度），所以这个界面和管理端/用户端通用。", 11.5f, false, "onSurfaceVariant"),
                12,
            )
        }
        if (onOpenSiteInfo != null) {
            DemoKit.put(cred, DemoKit.chip(act, theme, "站点信息", R.drawable.ic_info) { onOpenSiteInfo() }, 12)
        }
        DemoKit.put(host, cred, 14)

        
        if (credits != null) {
            val quota = DemoKit.panel(act, theme, 16)
            DemoKit.put(quota, DemoKit.txt(act, theme, "额度", 16f, true))
            DemoKit.put(quota, DemoKit.valueRow(act, theme, "名称", credits.str("name").ifEmpty { "—" }), 12)
            DemoKit.put(quota, DemoKit.valueRow(act, theme, "已用", fmt(credits.dbl("usedTokens"))), 10)
            DemoKit.put(
                quota,
                DemoKit.valueRow(act, theme, "剩余", if (unlimited) "不限" else fmt(credits.dbl("remainingTokens"))),
                10,
            )
            DemoKit.put(quota, DemoKit.valueRow(act, theme, "到期", credits.str("expiresAt").ifEmpty { "长期有效" }), 10)
            val q = credits.dbl("quotaTokens")
            if (!unlimited && q > 0.0) {
                val used = credits.dbl("usedTokens")
                DemoKit.put(
                    quota,
                    DemoKit.progressRow(act, theme, "已用 / 总额度", used.toLong(), q.toLong(), ((used * 100.0 / q).toInt()).toString() + "%"),
                    14,
                )
            }
            DemoKit.put(host, quota, 14)
        } else {
            val card = DemoKit.panel(act, theme, 16)
            DemoKit.put(
                card,
                DemoKit.txt(
                    act, theme,
                    if (asAdmin) "读取额度失败：服务端需要启用 auth-adminkey 插件，才能把管理密钥当用户密钥用。"
                    else "读取额度失败：卡密可能已失效，或服务端未启用卡密插件。",
                    12.5f, false, "onSurfaceVariant",
                ),
            )
            DemoKit.put(host, card, 14)
        }

        
        val edit = DemoKit.panel(act, theme, 16)
        DemoKit.put(edit, DemoKit.txt(act, theme, "编辑资料", 16f, true))
        val editable = me != null
        if (!editable) {
            DemoKit.put(edit, DemoKit.txt(act, theme, "当前身份无法读取用户资料（缺少用户系统插件或凭证无效）。", 12f, false, "onSurfaceVariant"), 8)
        }
        val fNick = MdField(act, theme, "昵称", nick, false, false)
        val fAvatar = MdField(act, theme, "头像 URL（可空）", avatar, false, false)
        DemoKit.put(edit, fNick, 14)
        DemoKit.put(edit, fAvatar, 12)
        DemoKit.put(
            edit,
            DemoKit.button(act, theme, "保存资料", "filled") {
                if (!editable) {
                    UiKit.toast(act, "当前身份不能修改资料")
                    return@button
                }
                act.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        Api.post(
                            server.baseUrl + "/auth/profile",
                            JsonObject().apply {
                                addProperty("nickname", fNick.text.trim())
                                addProperty("avatar", fAvatar.text.trim())
                            },
                            headers,
                        )
                        withContext(Dispatchers.Main) {
                            UiKit.toast(act, "已保存")
                            build(act, theme, server, host, asAdmin, onLogout, onOpenSiteInfo)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { UiKit.toast(act, "保存失败: " + (e.message ?: "")) }
                    }
                }
            },
            14,
        )
        DemoKit.put(host, edit, 14)

        





        val pwd = DemoKit.panel(act, theme, 16)
        if (asAdmin) {
            DemoKit.put(pwd, DemoKit.txt(act, theme, "修改管理密钥", 16f, true))
            DemoKit.put(
                pwd,
                DemoKit.txt(act, theme, "管理密钥 = 管理端 / 网页端的访问密码。修改后本 App 会自动同步，请记牢新密钥。", 12f, false, "onSurfaceVariant"),
                6,
            )
            val aOld = MdField(act, theme, "当前管理密钥", "", false, false).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val aNew = MdField(act, theme, "新密钥（≥8 位）", "", false, false).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val aNew2 = MdField(act, theme, "再输一次新密钥", "", false, false).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            DemoKit.put(pwd, aOld, 14)
            DemoKit.put(pwd, aNew, 12)
            DemoKit.put(pwd, aNew2, 12)
            DemoKit.put(
                pwd,
                DemoKit.button(act, theme, "提交修改", "outlined") {
                    val np = aNew.text
                    if (np.length < 8) {
                        UiKit.toast(act, "新密钥至少 8 位")
                        return@button
                    }
                    if (np != aNew2.text) {
                        UiKit.toast(act, "两次输入的新密钥不一致")
                        return@button
                    }
                    UiKit.confirm(
                        act,
                        "修改管理密钥",
                        "改完立即生效：网页端和其它设备都要改用新密钥。\n\n本 App 会自动同步，不需要重新登录。确定继续？",
                        theme,
                    ) {
                        act.lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                Api.adminPost(
                                    server, "/admin/api/settings/admin-key",
                                    JsonObject().apply {
                                        addProperty("oldKey", aOld.text)
                                        addProperty("newKey", np)
                                    },
                                )
                                
                                server.adminKeyEnc = KeyStoreCrypto.encrypt(np)
                                App.of(act).store.save(server)
                                withContext(Dispatchers.Main) { UiKit.toast(act, "管理密钥已修改") }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { UiKit.toast(act, "修改失败: " + (e.message ?: "")) }
                            }
                        }
                    }
                },
                14,
            )
        } else {
            DemoKit.put(pwd, DemoKit.txt(act, theme, "修改登录密码", 16f, true))
            DemoKit.put(pwd, DemoKit.txt(act, theme, "需要先输入当前密码；新密码至少 8 位。", 12f, false, "onSurfaceVariant"), 6)
            val fOld = MdField(act, theme, "当前密码", "", false, false).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val fNew = MdField(act, theme, "新密码（≥8 位）", "", false, false).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            DemoKit.put(pwd, fOld, 14)
            DemoKit.put(pwd, fNew, 12)
            DemoKit.put(
                pwd,
                DemoKit.button(act, theme, "提交修改", "outlined") {
                    val np = fNew.text
                    if (np.length < 8) {
                        UiKit.toast(act, "新密码至少 8 位")
                        return@button
                    }
                    act.lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            Api.post(
                                server.baseUrl + "/auth/password",
                                JsonObject().apply {
                                    addProperty("oldPassword", fOld.text)
                                    addProperty("newPassword", np)
                                },
                                headers,
                            )
                            withContext(Dispatchers.Main) { UiKit.toast(act, "密码已修改") }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { UiKit.toast(act, "修改失败: " + (e.message ?: "")) }
                        }
                    }
                },
                14,
            )
        }
        DemoKit.put(host, pwd, 14)

        
        if (onLogout != null) {
            DemoKit.put(
                host,
                DemoKit.button(act, theme, if (asAdmin) "退出管理端" else "切换账号", "outlined") { onLogout() },
                6,
            )
        }
    }

    

    private fun mask(k: String) = if (k.length <= 12) k else k.take(9) + "…" + k.takeLast(4)

    
    private fun fmt(v: Double): String = com.gaycore.app.data.AdminKey.fmtTokens(v)

    private fun JsonObject.str(k: String): String = get(k)?.takeIf { !it.isJsonNull }?.asString ?: ""
    private fun JsonObject.bool(k: String): Boolean = get(k)?.takeIf { !it.isJsonNull }?.asBoolean ?: false
    private fun JsonObject.long(k: String): Long = get(k)?.takeIf { !it.isJsonNull }?.asLong ?: 0L
    private fun JsonObject.dbl(k: String): Double = get(k)?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
}
