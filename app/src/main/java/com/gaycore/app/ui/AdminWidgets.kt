package com.gaycore.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaycore.app.R
import com.gaycore.app.data.AdminKey
import com.gaycore.app.data.AdminUser
import com.gaycore.app.data.Api
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.theme.ThemeEngine
import com.gaycore.app.ui.UiKit.add
import com.gaycore.app.ui.UiKit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object UserDialogs {

    private fun io(act: AppCompatActivity, onDone: () -> Unit, block: () -> Unit) {
        act.lifecycleScope.launch(Dispatchers.IO) {
            try { block(); withContext(Dispatchers.Main) { onDone() } }
            catch (e: Exception) { withContext(Dispatchers.Main) { UiKit.toast(act, e.message ?: "失败") } }
        }
    }

    private fun form(act: Context, theme: ThemeEngine, title: String): Pair<LinearLayout, MaterialAlertDialogBuilder> {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(act, 20)
            setPadding(p, dp(act, 8), p, 0)
        }
        


        return col to UiKit.ThemedDialogBuilder(act, theme).setTitle(title).setView(col)
    }

    
    private fun input(act: Context, theme: ThemeEngine, hint: String, text: String = "", number: Boolean = false): MdField =
        MdField(act, theme, hint, text, number)

    
    fun createUser(act: AppCompatActivity, theme: ThemeEngine, server: ServerEntry, onDone: () -> Unit) {
        val (col, dlg) = form(act, theme, act.getString(R.string.um_new_user))
        val uname = input(act, theme, act.getString(R.string.um_name_hint))
        val pass = input(act, theme, act.getString(R.string.um_password_hint))
        val email = input(act, theme, act.getString(R.string.um_email_hint))
        col.add(uname, 6); col.add(pass, 6); col.add(email, 6)
        dlg.setPositiveButton(R.string.ok) { _, _ ->
            io(act, onDone) {
                Api.adminUserAction(server, JsonObject().apply {
                    addProperty("username", uname.text.toString().trim())
                    addProperty("password", pass.text.toString()); addProperty("email", email.text.toString().trim())
                })
            }
        }.setNegativeButton(R.string.cancel, null).show()
    }

    
    fun editUser(act: AppCompatActivity, theme: ThemeEngine, server: ServerEntry, u: AdminUser, onDone: () -> Unit) {
        val (col, dlg) = form(act, theme, act.getString(R.string.um_edit_user))
        val name = input(act, theme, act.getString(R.string.um_name_hint), u.name)
        val nick = input(act, theme, act.getString(R.string.um_nickname_hint), u.nickname)
        val email = input(act, theme, act.getString(R.string.um_email_hint), u.email)
        val note = input(act, theme, act.getString(R.string.um_note_hint), u.note)
        col.add(name, 6); col.add(nick, 6); col.add(email, 6); col.add(note, 6)
        dlg.setPositiveButton(R.string.ok) { _, _ ->
            io(act, onDone) {
                Api.adminUserAction(server, JsonObject().apply {
                    addProperty("action", "update"); addProperty("uid", u.uid)
                    addProperty("name", name.text.toString().trim()); addProperty("nickname", nick.text.toString().trim())
                    addProperty("email", email.text.toString().trim()); addProperty("note", note.text.toString().trim())
                })
            }
        }.setNegativeButton(R.string.cancel, null).show()
    }

    
    fun resetPassword(act: AppCompatActivity, theme: ThemeEngine, server: ServerEntry, u: AdminUser, onDone: () -> Unit) {
        val (col, dlg) = form(act, theme, act.getString(R.string.um_reset_password))
        val pass = input(act, theme, act.getString(R.string.um_new_password_hint))
        col.add(pass, 6)
        dlg.setPositiveButton(R.string.ok) { _, _ ->
            io(act, onDone) {
                Api.adminUserAction(server, JsonObject().apply {
                    addProperty("action", "resetPassword"); addProperty("uid", u.uid); addProperty("password", pass.text.toString())
                })
            }
        }.setNegativeButton(R.string.cancel, null).show()
    }

    
    fun toggleBan(act: AppCompatActivity, theme: ThemeEngine, server: ServerEntry, u: AdminUser, onDone: () -> Unit) {
        if (u.banned) {
            UiKit.confirm(act, act.getString(R.string.um_unban), u.uid) {
                io(act, onDone) { Api.adminUserAction(server, JsonObject().apply { addProperty("action", "unban"); addProperty("uid", u.uid) }) }
            }
            return
        }
        val (col, dlg) = form(act, theme, act.getString(R.string.um_ban))
        val reason = input(act, theme, act.getString(R.string.um_ban_reason))
        col.add(reason, 6)
        dlg.setPositiveButton(R.string.ok) { _, _ ->
            io(act, onDone) {
                Api.adminUserAction(server, JsonObject().apply {
                    addProperty("action", "ban"); addProperty("uid", u.uid); addProperty("reason", reason.text.toString())
                })
            }
        }.setNegativeButton(R.string.cancel, null).show()
    }

    
    fun deleteUser(act: AppCompatActivity, server: ServerEntry, u: AdminUser, onDone: () -> Unit) {
        UiKit.confirm(act, act.getString(R.string.um_delete_user), act.getString(R.string.um_delete_user_confirm, u.uid)) {
            io(act, onDone) { Api.adminUserAction(server, JsonObject().apply { addProperty("action", "delete"); addProperty("uid", u.uid) }) }
        }
    }

    
    fun createKey(act: AppCompatActivity, theme: ThemeEngine, server: ServerEntry, uid: String?, onDone: () -> Unit) {
        val (col, dlg) = form(act, theme, act.getString(R.string.um_create_key))
        val name = input(act, theme, act.getString(R.string.um_key_name_hint))
        val quota = input(act, theme, act.getString(R.string.um_quota_hint), "", number = true)
        val zero = CheckBox(act).apply { text = act.getString(R.string.um_quota_zero) }
        val expires = input(act, theme, act.getString(R.string.um_expires_hint))
        val note = input(act, theme, act.getString(R.string.um_note_hint))
        col.add(name, 6); col.add(quota, 6); col.add(zero, 2); col.add(expires, 6); col.add(note, 6)
        dlg.setPositiveButton(R.string.ok) { _, _ ->
            val q = if (zero.isChecked) -1L else (quota.text.toString().toLongOrNull() ?: 0L)
            io(act, onDone) {
                Api.adminCreateKey(server, JsonObject().apply {
                    addProperty("name", name.text.toString().trim().ifEmpty { act.getString(R.string.um_key_default_name) })
                    addProperty("quotaTokens", q)
                    if (!uid.isNullOrEmpty()) addProperty("uid", uid)
                    addProperty("expiresAt", expires.text.toString().trim())
                    addProperty("note", note.text.toString().trim())
                })
            }
        }.setNegativeButton(R.string.cancel, null).show()
    }

    
    fun editKey(act: AppCompatActivity, theme: ThemeEngine, server: ServerEntry, k: AdminKey, onDone: () -> Unit) {
        val (col, dlg) = form(act, theme, act.getString(R.string.um_edit_key))
        val name = input(act, theme, act.getString(R.string.um_key_name_hint), k.name)
        val enable = CheckBox(act).apply { text = act.getString(R.string.um_key_enable); isChecked = k.enable }
        val quota = input(act, theme, act.getString(R.string.um_quota_hint), if (k.quotaTokens > 0) k.quotaTokens.toString() else "", number = true)
        val zero = CheckBox(act).apply { text = act.getString(R.string.um_quota_zero); isChecked = k.quotaTokens == -1L }
        val addQuota = input(act, theme, act.getString(R.string.um_add_quota_hint), "", number = true)
        val resetUsage = CheckBox(act).apply { text = act.getString(R.string.um_reset_usage) }
        val expires = input(act, theme, act.getString(R.string.um_expires_hint), k.expiresAt)
        val note = input(act, theme, act.getString(R.string.um_note_hint), k.note)
        col.add(name, 6); col.add(enable, 2); col.add(quota, 6); col.add(zero, 2)
        col.add(addQuota, 6); col.add(resetUsage, 2); col.add(expires, 6); col.add(note, 6)
        dlg.setPositiveButton(R.string.ok) { _, _ ->
            val body = JsonObject().apply {
                addProperty("key", k.key)
                addProperty("name", name.text.toString().trim())
                addProperty("enable", enable.isChecked)
                val quotaText = quota.text.toString()
                if (zero.isChecked) addProperty("quotaTokens", -1)
                else if (quotaText.isNotBlank()) addProperty("quotaTokens", quotaText.toLongOrNull() ?: k.quotaTokens)
                addQuota.text.toString().toLongOrNull()?.takeIf { it > 0 }?.let { addProperty("addQuota", it) }
                if (resetUsage.isChecked) addProperty("resetUsage", true)
                addProperty("expiresAt", expires.text.toString().trim())
                addProperty("note", note.text.toString().trim())
            }
            io(act, onDone) { Api.adminUpdateKey(server, body) }
        }.setNeutralButton(R.string.um_copy_key) { _, _ ->
            val cm = act.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("key", k.key))
            UiKit.toast(act, act.getString(R.string.copied))
        }.setNegativeButton(R.string.cancel, null).show()
    }

    
    fun deleteKey(act: AppCompatActivity, server: ServerEntry, k: AdminKey, onDone: () -> Unit) {
        UiKit.confirm(act, act.getString(R.string.um_delete_key), k.masked() + " (" + k.name + ")") {
            io(act, onDone) { Api.adminDeleteKey(server, k.key) }
        }
    }
}
