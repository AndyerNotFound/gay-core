package com.gaycore.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaycore.app.R
import com.gaycore.app.data.Api
import com.gaycore.app.data.adminTarget
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.theme.ThemeEngine
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext







class AdminPages(
    private val act: AppCompatActivity,
    private val themeProvider: () -> ThemeEngine,
    private val server: ServerEntry,
    private val openConvo: () -> Unit = {},
    private val openChild: (ChildSpec) -> Unit = { },
) {

    
    class ChildSpec(
        val title: String,
        val build: (LinearLayout) -> Unit,
        val topIcon: Int = 0,
        val topDesc: String = "",
        val onTop: (() -> Unit)? = null,
        val footer: ((LinearLayout) -> Unit)? = null,
    )

    private fun child(title: String, build: (LinearLayout) -> Unit) = openChild(ChildSpec(title, build))

    
    
    private fun adminInst(): String = server.adminTarget

    fun instName(): String = adminInst().ifEmpty { "default" }

    
    private fun dCol(): LinearLayout = LinearLayout(act).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
    }

    
    private val theme: ThemeEngine get() = themeProvider()

    
    val playConvos = mutableListOf<String>()

    

    private fun io(block: suspend () -> Unit) {
        act.lifecycleScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { UiKit.toast(act, e.message ?: "操作失败") }
            }
        }
    }

    private fun main(block: () -> Unit) = act.runOnUiThread(block)

    private fun JsonObject.arr(k: String): JsonArray? = get(k)?.takeIf { it.isJsonArray }?.asJsonArray
    private fun JsonObject.obj(k: String): JsonObject? = get(k)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.str(k: String): String = get(k)?.takeIf { !it.isJsonNull }?.asString ?: ""
    private fun JsonObject.bool(k: String): Boolean = get(k)?.takeIf { !it.isJsonNull }?.asBoolean ?: false
    private fun JsonObject.long(k: String): Long = get(k)?.takeIf { !it.isJsonNull }?.asLong ?: 0L
    private fun JsonObject.num(k: String): String =
        get(k)?.takeIf { !it.isJsonNull }?.let { if (it.isJsonPrimitive) it.asString else "" } ?: ""

    
    private fun shortTime(ts: String): String {
        if (ts.length < 16) return ts.ifEmpty { "—" }
        return try {
            ts.substring(5, 10).replace('-', '/') + " " + ts.substring(11, 16)
        } catch (_: Exception) {
            ts
        }
    }

    
    private suspend fun uid(): Long {
        val st = Api.adminGet(server, "/admin/api/status")
        val arr = st.arr("instances") ?: return 1L
        for (e in arr) {
            val o = e.asJsonObject
            if (o.get("name")?.asString == adminInst()) return o.get("uid").asLong
        }
        return arr.firstOrNull()?.asJsonObject?.get("uid")?.asLong ?: 1L
    }

    private suspend fun config(): JsonObject =
        Api.adminGet(server, "/admin/api/config/${uid()}").obj("config") ?: JsonObject()

    private suspend fun saveConfig(cfg: JsonObject) {
        Api.adminPost(server, "/admin/api/config/${uid()}", cfg)
    }

    
    private fun scrollPage(): Pair<LinearLayout, LinearLayout> {
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.color(act, "background"))
        }
        val content = DemoKit.pageColumn(act, theme, bottomSpace = 40)
        root.addView(
            android.widget.ScrollView(act).apply {
                isFillViewport = true
                clipToPadding = false
                addView(content, FrameLayout.LayoutParams(-1, -2))
            },
            LinearLayout.LayoutParams(-1, 0, 1f),
        )
        return root to content
    }

    private fun section(host: LinearLayout, title: String, sub: String = "", top: Int = 22) {
        DemoKit.put(host, DemoKit.sectionTitle(act, theme, title, sub), top)
    }

    private fun chipRow(chips: List<View>, gap: Int = 8): View = DemoKit.hscrollRow(act, chips, gap)

    private fun copyText(label: String, text: String) {
        DemoKit.copy(act, label, text)
        UiKit.toast(act, act.getString(R.string.copied))
    }

    private fun maskKey(k: String) = if (k.length <= 14) k else k.substring(0, 8) + "…" + k.substring(k.length - 4)

    




    private fun isAdminIssued(k: JsonObject): Boolean {
        val by = k.str("by")
        if (by.isNotEmpty()) return by == "admin"
        return !(k.bool("isMain") && k.str("uid").isNotEmpty())
    }

    
    private fun switchRowRef(title: String, desc: String, initial: Boolean): Pair<View, MaterialSwitch> {
        val sw = DemoKit.themeSwitch(act, theme, initial)
        val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(texts, DemoKit.txt(act, theme, title, 14f, true))
        if (desc.isNotEmpty()) DemoKit.put(texts, DemoKit.txt(act, theme, desc, 11.5f, false, "onSurfaceVariant"), 3)
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return row to sw
    }

    private fun field(label: String, value: String = "", numeric: Boolean = false, secret: Boolean = false): MdField =
        MdField(act, theme, label, value, numeric, false).apply {
            if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

    



    fun buildChannels(): View {
        val (root, content) = scrollPage()

        fun reload(): Unit = io {
            val cfg = config()
            val channels = cfg.arr("channels") ?: JsonArray()
            val probe = try { Api.adminGet(server, "/plugins/probe/status") } catch (_: Exception) { null }
            val rates = probe?.obj("channels")
            main {
                content.removeAllViews()
                section(content, "渠道 · " + instName(), "当前实例(" + instName() + ") 共 " + channels.size() + " 个渠道 · 按 modelMap → models → 默认 → 轮询 选择", 2)
                if (channels.size() == 0) {
                    val card = DemoKit.panel(act, theme, 16)
                    DemoKit.put(card, DemoKit.txt(act, theme, "还没有渠道，点下面「新增渠道」开始", 13f, false, "onSurfaceVariant"))
                    DemoKit.put(content, card, 10)
                }
                for (e in channels) {
                    val ch = e.asJsonObject
                    val name = ch.str("name").ifEmpty { "?" }
                    val default = ch.bool("default")
                    val rate = rates?.obj(name)?.get("rate")?.takeIf { !it.isJsonNull }?.asInt
                    val card = DemoKit.panel(act, theme, 16)

                    val top = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                    top.addView(DemoKit.iconBadge(act, theme, R.drawable.ic_grid, null, 40), LinearLayout.LayoutParams(DemoKit.dp(act, 40), DemoKit.dp(act, 40)))
                    val tc = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                    val nameRow = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                    nameRow.addView(DemoKit.txt(act, theme, name, 15f, true))
                    if (default) nameRow.addView(
                        DemoKit.badge(act, theme, "默认", "primary"),
                        LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
                    )
                    nameRow.addView(
                        DemoKit.badge(act, theme, ch.str("type").ifEmpty { "openai" }, if (rate != null && rate < 80) "error" else "neutral"),
                        LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
                    )
                    tc.addView(nameRow)
                    val rateTxt = if (rate != null) " · 成功率 " + rate + "%" else ""
                    DemoKit.put(tc, DemoKit.txt(act, theme, ch.str("baseUrl") + rateTxt, 11.5f, false, "onSurfaceVariant"), 3)
                    top.addView(tc, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 10) })
                    DemoKit.put(card, top)

                    val models = ch.arr("models")
                    val mm = ch.obj("modelMap")
                    DemoKit.put(
                        card,
                        DemoKit.txt(
                            act, theme,
                            "模型 " + (models?.size() ?: 0) + " 个 · 分组 " + (mm?.size() ?: 0) + " 条" +
                                (if (ch.bool("useResponses")) " · ⚡Responses" else "") +
                                (if (ch.str("proxy").isNotEmpty()) " · 代理 " + ch.str("proxy") else ""),
                            11f, false, "onSurfaceVariant",
                        ),
                        8,
                    )

                    DemoKit.put(
                        card,
                        chipRow(
                            listOf(
                                DemoKit.chip(act, theme, "同步模型", R.drawable.ic_refresh) { syncModels(name) { reload() } },
                                DemoKit.chip(act, theme, "查余额", R.drawable.ic_wallet) { queryBalance(name) },
                                DemoKit.chip(act, theme, "编辑", R.drawable.ic_edit) { editChannel(ch) { reload() } },
                                DemoKit.chip(act, theme, "复制名", R.drawable.ic_copy) { copyText("channel", name) },
                                DemoKit.chip(act, theme, "删除", R.drawable.ic_delete) {
                                    UiKit.confirm(act, "删除渠道", "确定删除「" + name + "」？") {
                                        io {
                                            val c = config()
                                            val keep = JsonArray()
                                            for (x in (c.arr("channels") ?: JsonArray())) {
                                                if (x.asJsonObject.str("name") != name) keep.add(x)
                                            }
                                            c.add("channels", keep)
                                            saveConfig(c)
                                            main { UiKit.toast(act, "已删除"); reload() }
                                        }
                                    }
                                },
                            ),
                        ),
                        12,
                    )
                    DemoKit.put(content, card, 8)
                }
                val opRow = LinearLayout(act)
                opRow.addView(
                    DemoKit.button(act, theme, "新增渠道", "filled") { editChannel(null) { reload() } },
                    LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f),
                )
                opRow.addView(
                    DemoKit.button(act, theme, "探测成功率", "outlined") { runProbe { reload() } },
                    LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f).apply { marginStart = DemoKit.dp(act, 10) },
                )
                DemoKit.put(content, opRow, 16)
            }
        }
        reload()
        return root
    }

    




    private var qgData: JsonObject? = null
    private var qgCols = 2
    private val qgSel = LinkedHashSet<String>()      
    private var qgBody: LinearLayout? = null
    private val qgCards = HashMap<String, View>()    
    private var qgHint: TextView? = null             
    private var qgFooterBar: LinearLayout? = null    
    var onRefreshChild: () -> Unit = {}              

    
    fun quickGroupFooter(bar: LinearLayout) {
        qgFooterBar = bar
        drawQGFooter()
    }

    
    private fun drawQGFooter() {
        val bar = qgFooterBar ?: return
        bar.removeAllViews()
        if (qgSel.isEmpty()) return
        val card = DemoKit.panel(act, theme, 0).apply {
            setPadding(DemoKit.dp(act, 14), DemoKit.dp(act, 10), DemoKit.dp(act, 14), DemoKit.dp(act, 14))
        }
        DemoKit.put(card, DemoKit.txt(act, theme, "已选 " + qgSel.size + " 个模型", 13f, true))
        val row = LinearLayout(act)
        row.addView(
            DemoKit.button(act, theme, "设为分组", "filled") { qgBulkGroup() },
            LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f),
        )
        row.addView(
            DemoKit.button(act, theme, "批量改价", "outlined") { qgBulkPrice() },
            LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f).apply { marginStart = DemoKit.dp(act, 8) },
        )
        row.addView(
            DemoKit.button(act, theme, "取消", "text") { qgClearSel() },
            LinearLayout.LayoutParams(-2, DemoKit.dp(act, 40)).apply { marginStart = DemoKit.dp(act, 8) },
        )
        DemoKit.put(card, row, 8)
        bar.addView(card, LinearLayout.LayoutParams(-1, -2))
    }

    fun buildQuickGroup(host: LinearLayout) {
        val body = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(host, body)
        qgBody = body
        loadQuickGroup()
    }

    private fun loadQuickGroup() = io {
        try {
            val r = Api.adminGet(server, "/plugins/model-square/admin/groups")
            main { qgData = r; drawQuickGroup() }
        } catch (e: Exception) {
            main {
                qgBody?.removeAllViews()
                qgBody?.let { DemoKit.put(it, DemoKit.txt(act, theme, "读取失败: " + (e.message ?: "") + "\n（确认 model-square 插件已启用）", 13f, false, "error")) }
            }
        }
    }

    private fun qgRefresh() { qgBody?.let { drawQuickGroup() } }

    


    private fun qgToggle(key: String) {
        if (!qgSel.remove(key)) qgSel.add(key)
        updateQGMode()
    }

    private fun qgClearSel() {
        qgSel.clear()
        updateQGMode()
    }

    private fun qgHeaderText(): String =
        if (qgSel.isNotEmpty()) "多选模式 · 已选 " + qgSel.size + " 个：单击继续加减选择，底部操作条批量处理"
        else "单击卡片编辑模型 · 长按进入多选"

    private fun updateQGMode() {
        for ((k, v) in qgCards) paintQGCard(v, k)
        qgHint?.let {
            it.text = qgHeaderText()
            it.setTextColor(theme.color(act, if (qgSel.isNotEmpty()) "primary" else "onSurfaceVariant"))
        }
        drawQGFooter()
    }

    private fun paintQGCard(card: View, key: String) {
        card.background = if (key in qgSel) DemoKit.selectedBg(act, theme, 12) else DemoKit.panelBg(act, theme, 12)
    }

    private fun drawQuickGroup() {
        val body = qgBody ?: return
        val d = qgData ?: return
        body.removeAllViews()
        qgCards.clear()
        val groups = d.arr("groups") ?: JsonArray()
        val chs = d.arr("channels") ?: JsonArray()
        var total = 0
        for (c in chs) total += (c.asJsonObject.arr("models")?.size() ?: 0)

        
        val head = DemoKit.panel(act, theme, 16)
        DemoKit.put(head, DemoKit.sectionTitle(act, theme, "快速分组", "共 " + total + " 个模型 · " + groups.size() + " 个分组"))
        val hint = DemoKit.txt(act, theme, qgHeaderText(), 11.5f, false, if (qgSel.isNotEmpty()) "primary" else "onSurfaceVariant")
        qgHint = hint
        DemoKit.put(head, hint, 6)
        val colRow = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        colRow.addView(DemoKit.txt(act, theme, "列数", 12.5f, true, "onSurfaceVariant"))
        for (n in listOf(2, 3, 4)) {
            colRow.addView(
                DemoKit.chip(act, theme, n.toString(), null, qgCols == n) { qgCols = n; drawQuickGroup() },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
            )
        }
        colRow.addView(View(act), LinearLayout.LayoutParams(0, 1, 1f))
        colRow.addView(DemoKit.chip(act, theme, "分组管理", R.drawable.ic_settings) { qgGroupSheet() })
        DemoKit.put(head, colRow, 10)
        DemoKit.put(body, head, 2)

        if (total == 0) {
            val c = DemoKit.panel(act, theme, 16)
            DemoKit.put(c, DemoKit.txt(act, theme, "还没有模型 —— 先去「模型列表」页从上游同步。", 13f, false, "onSurfaceVariant"))
            DemoKit.put(body, c, 12)
            return
        }

        val insts = d.arr("instances") ?: JsonArray()
        
        fun layoutGrid(cells: List<View>) {
            var i = 0
            while (i < cells.size) {
                val row = LinearLayout(act).apply { gravity = Gravity.TOP }
                var cIdx = 0
                while (cIdx < qgCols && i < cells.size) {
                    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    if (cIdx > 0) lp.marginStart = DemoKit.dp(act, 8)
                    row.addView(cells[i], lp)
                    cIdx++; i++
                }
                while (cIdx < qgCols) {
                    row.addView(View(act), LinearLayout.LayoutParams(0, 1, 1f).apply { if (cIdx > 0) marginStart = DemoKit.dp(act, 8) })
                    cIdx++
                }
                DemoKit.put(body, row, 8)
            }
        }
        for (ie in insts) {
          val inst = ie.takeIf { it.isJsonObject }?.asJsonObject ?: continue
          val iname = inst.str("name")
          val merged = inst.arr("merged")
          if (merged != null && merged.size() > 0) {
            

            section(body, "实例 · " + iname + (if (inst.bool("current")) "（当前）" else ""),
                "共 " + merged.size() + " 个模型（同名已合并）", 22)
            val cells = ArrayList<View>()
            for (me in merged) cells.add(qgCard(iname, "*", me.asJsonObject))
            layoutGrid(cells)
          } else {
            section(body, "实例 · " + iname + (if (inst.bool("current")) "（当前）" else ""),
                (inst.arr("channels")?.size() ?: 0).toString() + " 个渠道", 22)
            for (ce in (inst.arr("channels") ?: JsonArray())) {
              val c = ce.asJsonObject
              val chName = c.str("name")
              val models = c.arr("models") ?: JsonArray()
              section(body, "  " + chName, models.size().toString() + " 个模型", 14)
              val cells = ArrayList<View>()
              for (me in models) cells.add(qgCard(iname, chName, me.asJsonObject))
              layoutGrid(cells)
            }
          }
        }
    }

    private fun qgCard(iname: String, chName: String, m: JsonObject): View {
        val name = m.str("name")
        val key = iname + "|" + chName + "|" + name
        val group = m.str("group").ifEmpty { "Default" }
        val alias = m.str("alias")
        val selected = key in qgSel
        val card = DemoKit.panel(act, theme, 12, ripple = true)
        qgCards[key] = card
        if (selected) card.background = DemoKit.selectedBg(act, theme, 12)
        val isMerged = chName == "*"
        DemoKit.put(card, DemoKit.txt(act, theme, alias.ifEmpty { name }, 13f, true), 2)
        
        if (isMerged) {
            val cc = m.get("chCount")?.takeIf { !it.isJsonNull }?.asInt ?: 1
            DemoKit.put(card, DemoKit.txt(act, theme, iname + " · " + cc + " 个渠道", 10f, false, "onSurfaceVariant"), 2)
        } else {
            DemoKit.put(card, DemoKit.txt(act, theme, iname + " · " + chName, 10f, false, "onSurfaceVariant"), 2)
        }
        if (alias.isNotEmpty()) DemoKit.put(card, DemoKit.txt(act, theme, name, 10.5f, false, "onSurfaceVariant"), 2)
        val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(DemoKit.badge(act, theme, group, if (group == "Default") "neutral" else "primary"))
        if (isMerged && m.bool("diff")) {
            row.addView(DemoKit.badge(act, theme, "配置不一致", "error"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 4) })
        }
        if (m.get("perCall") != null && !m.get("perCall").isJsonNull) {
            row.addView(DemoKit.badge(act, theme, "按次 $" + m.get("perCall").asDouble, "warning"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 4) })
        } else if (m.obj("price") != null) {
            row.addView(DemoKit.badge(act, theme, "有价格", "success"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 4) })
        }
        DemoKit.put(card, row, 8)
        

        card.setOnClickListener {
            if (qgSel.isNotEmpty()) qgToggle(key) else qgEditModel(iname, chName, m)
        }
        card.setOnLongClickListener { qgToggle(key); true }
        return card
    }

    
    




    





    private fun qgEditModel(iname: String, chName: String, m: JsonObject) {
        val name = m.str("name")
        val uiPath = "/admin/ui/model-edit?instance=" + iname + "&channel=" + chName + "&model=" + name
        val container = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 12), DemoKit.dp(act, 16), DemoKit.dp(act, 20))
            setBackgroundColor(theme.color(act, "surfaceContainer"))
        }
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(act)
        val ctl = SduiController(
            act, act, server, "", theme,
            admin = true,
            adminApiScope = listOf("/admin/"),
            inlineNav = true,
            onClose = { sheet.dismiss() },
            onFallback = { reason -> if (sheet.isShowing) sheet.dismiss(); UiKit.toast(act, "模型编辑页加载失败: $reason") },
        )
        ctl.loadInto(container, uiPath, showTitle = false)
        

        val sv = androidx.core.widget.NestedScrollView(act).apply { addView(container) }
        sheet.setContentView(sv)
        
        sheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        sheet.setOnShowListener {
            sheet.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            sheet.behavior.skipCollapsed = true
            UiKit.retintTree(container, theme)
            
            UiKit.tintDialogWindow(sheet, theme, bottomSheet = true)
        }
        sheet.show()
    }

    
    private fun qgBulkGroup() {
        val d = qgData ?: return
        val groups = (d.arr("groups") ?: JsonArray()).map { it.asJsonObject.str("name") }
        var picked = groups.firstOrNull() ?: "Default"
        val col = dCol()
        DemoKit.put(col, DemoKit.txt(act, theme, "把选中的 " + qgSel.size + " 个模型移到：", 13f, true))
        val box = LinearLayout(act)
        var rebuild: () -> Unit = {}
        rebuild = {
            box.removeAllViews()
            for (g in groups) box.addView(
                DemoKit.chip(act, theme, g, null, g == picked) { picked = g; rebuild() },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
            )
        }
        rebuild()
        DemoKit.put(col, DemoKit.hscrollRow(act, listOf<View>(box)), 10)
        UiKit.customDialog(act, theme, "批量设为分组", col, "应用") {
            qgPost(JsonObject().apply {
                addProperty("action", "bulk"); addProperty("group", picked); add("items", qgItems())
            }, "已移到 " + picked)
        }
    }

    private fun qgBulkPrice() {
        val col = dCol()
        DemoKit.put(col, DemoKit.txt(act, theme, "给选中的 " + qgSel.size + " 个模型套一个价格：", 13f, true))
        val fIn = field("输入（" + com.gaycore.app.data.Currency.symbol + " / 1M）", "0", numeric = true)
        val fOut = field("输出（" + com.gaycore.app.data.Currency.symbol + " / 1M）", "0", numeric = true)
        val fCW = field("缓存写入（" + com.gaycore.app.data.Currency.symbol + " / 1M）", "0", numeric = true)
        val fCR = field("缓存读取（" + com.gaycore.app.data.Currency.symbol + " / 1M）", "0", numeric = true)
        DemoKit.put(col, fIn, 10); DemoKit.put(col, fOut, 8); DemoKit.put(col, fCW, 8); DemoKit.put(col, fCR, 8)
        


        val (swRow, swPerCall) = switchRowRef("同时修改按次计费", "填数值 = 设为按次价；留空 = 清除按次回到按量", false)
        val fPC = field("按次价格（" + com.gaycore.app.data.Currency.symbol + " / 次）", "", numeric = true)
        fPC.edit.isEnabled = false; fPC.alpha = 0.45f
        swPerCall.setOnCheckedChangeListener { _, c -> fPC.edit.isEnabled = c; fPC.alpha = if (c) 1f else 0.45f }
        DemoKit.put(col, swRow, 12); DemoKit.put(col, fPC, 8)
        UiKit.customDialog(act, theme, "批量改价", col, "应用") {
            qgPost(JsonObject().apply {
                addProperty("action", "bulk")
                if (swPerCall.isChecked) {
                    val pc = fPC.text.trim()
                    if (pc.isEmpty()) add("perCall", com.google.gson.JsonNull.INSTANCE)
                    else addProperty("perCall", pc.toDoubleOrNull() ?: 0.0)
                }
                add("price", JsonObject().apply {
                    addProperty("in", fIn.text.trim().toDoubleOrNull() ?: 0.0)
                    addProperty("out", fOut.text.trim().toDoubleOrNull() ?: 0.0)
                    addProperty("cacheWrite", fCW.text.trim().toDoubleOrNull() ?: 0.0)
                    addProperty("cacheRead", fCR.text.trim().toDoubleOrNull() ?: 0.0)
                })
                add("items", qgItems())
            }, "已改价")
        }
    }

    private fun qgItems(): JsonArray {
        val arr = JsonArray()
        for (k in qgSel) {
            val parts = k.split('|')
            if (parts.size < 3) continue
            arr.add(JsonObject().apply {
                addProperty("instance", parts[0])
                addProperty("channel", parts[1])
                addProperty("model", parts.subList(2, parts.size).joinToString("|"))
            })
        }
        return arr
    }

    
    private fun qgGroupSheet() {
        val d = qgData ?: return
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(act)
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), DemoKit.dp(act, 20))
            setBackgroundColor(theme.color(act, "surfaceContainer"))
        }
        var paint: () -> Unit = {}
        paint = {
            box.removeAllViews()
            val gs = (qgData?.arr("groups") ?: JsonArray())
            val counts = qgData?.obj("counts") ?: JsonObject()
            DemoKit.put(box, DemoKit.sectionTitle(act, theme, "分组管理", "共 " + gs.size() + " 个分组"), 4)
            for (ge in gs) {
                val g = ge.asJsonObject
                val nm = g.str("name")
                val rate = g.get("rate")?.asDouble ?: 1.0
                val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                row.addView(DemoKit.txt(act, theme, nm, 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(DemoKit.badge(act, theme, "×" + rate, if (nm == "Default") "neutral" else "primary"))
                row.addView(
                    DemoKit.txt(act, theme, "  " + (counts.get(nm)?.asInt ?: 0) + " 个模型", 11f, false, "onSurfaceVariant"),
                )
                row.addView(DemoKit.chip(act, theme, "编辑", null, false) { qgEditGroup(nm, rate) { paint() } }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) })
                if (nm != "Default") {
                    row.addView(DemoKit.chip(act, theme, "删", null, true) {
                        UiKit.confirm(act, "删除分组", "删除「" + nm + "」？该分组的模型会回到 Default。", theme) {
                            qgPost(JsonObject().apply { addProperty("action", "removeGroup"); addProperty("name", nm) }, "已删除") { paint() }
                        }
                    }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 4) })
                }
                DemoKit.put(box, row, 10)
            }
            val acts = LinearLayout(act)
            acts.addView(DemoKit.button(act, theme, "新建分组", "filled") { qgNewGroup { paint() } }, LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f))
            acts.addView(
                DemoKit.button(act, theme, "完成", "outlined") { sheet.dismiss() },
                LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f).apply { marginStart = DemoKit.dp(act, 8) },
            )
            DemoKit.put(box, acts, 16)
        }
        paint()
        sheet.setContentView(box)
        
        sheet.setOnShowListener { UiKit.tintDialogWindow(sheet, theme, bottomSheet = true) }
        sheet.show()
    }

    private fun qgNewGroup(onDone: () -> Unit) {
        val col = dCol()
        val fName = field("分组名称", "")
        val fRate = field("计费倍率（1 = 不加价）", "1", numeric = true)
        DemoKit.put(col, fName, 6)
        DemoKit.put(col, fRate, 10)
        DemoKit.put(col, DemoKit.txt(act, theme, "倍率用于「只按分组算」的计费：最终价格 = 渠道单价 × 倍率。", 11f, false, "onSurfaceVariant"), 8)
        UiKit.customDialog(act, theme, "新建分组", col, "创建") {
            val n = fName.text.trim()
            if (n.isEmpty()) { UiKit.toast(act, "分组名不能为空"); UiKit.keepOpen(); return@customDialog }
            qgPost(JsonObject().apply {
                addProperty("action", "addGroup"); addProperty("name", n)
                addProperty("rate", fRate.text.trim().toDoubleOrNull() ?: 1.0)
            }, "已新建 " + n) { onDone() }
        }
    }

    private fun qgEditGroup(name: String, rate: Double, onDone: () -> Unit) {
        val col = dCol()
        val fName = field("分组名称", name)
        val fRate = field("计费倍率", rate.toString(), numeric = true)
        DemoKit.put(col, fName, 6)
        DemoKit.put(col, fRate, 10)
        UiKit.customDialog(act, theme, "编辑分组", col, "保存") {
            val body = JsonObject().apply {
                addProperty("action", "setGroupRate"); addProperty("name", name)
                addProperty("rate", fRate.text.trim().toDoubleOrNull() ?: 1.0)
            }
            val nn = fName.text.trim()
            io {
                try {
                    Api.adminPost(server, "/plugins/model-square/admin/groups", body)
                    if (nn.isNotEmpty() && nn != name && name != "Default") {
                        Api.adminPost(server, "/plugins/model-square/admin/groups", JsonObject().apply {
                            addProperty("action", "renameGroup"); addProperty("name", name); addProperty("newName", nn)
                        })
                    }
                    main { UiKit.toast(act, "已保存"); refreshQuick(); onDone() }
                } catch (e: Exception) {
                    main { UiKit.toast(act, "保存失败: " + (e.message ?: "")) }
                }
            }
        }
    }

    



    fun buildProxiesView(host: LinearLayout) {
        val body = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(host, body)

        var draw: () -> Unit = {}
        var reload: () -> Unit = {}

        fun load(): Unit = io {
            try {
                val r = Api.adminGet(server, "/plugins/proxy/admin/list")
                main { drawPx(body, r, { load() }) }
            } catch (e: Exception) {
                main {
                    body.removeAllViews()
                    DemoKit.put(body, DemoKit.txt(act, theme, "读取失败: " + (e.message ?: "") + "\n（确认当前实例已启用 proxy 插件）", 13f, false, "error"))
                }
            }
        }
        draw = { }
        reload = { load() }
        load()
    }

    private fun drawPx(body: LinearLayout, r: JsonObject, onChanged: () -> Unit) {
        body.removeAllViews()
        val list = r.arr("proxies") ?: JsonArray()
        val usedBy = r.obj("usedBy") ?: JsonObject()

        val head = DemoKit.panel(act, theme, 16)
        DemoKit.put(head, DemoKit.sectionTitle(act, theme, "代理设置", "当前实例「" + instName() + "」· 共 " + list.size() + " 个代理"))
        DemoKit.put(
            head,
            DemoKit.txt(act, theme, "渠道的「代理」字段填这里的名字即可引用；内核按名字取用，改一处所有引用它的渠道都生效。", 11.5f, false, "onSurfaceVariant"),
            6,
        )
        DemoKit.put(head, DemoKit.button(act, theme, "新建代理", "filled") { pxEdit(null, onChanged) }, 12)
        DemoKit.put(body, head, 2)

        if (list.size() == 0) {
            val c = DemoKit.panel(act, theme, 16)
            DemoKit.put(c, DemoKit.txt(act, theme, "还没有代理。点上面「新建代理」加一个（例如 127.0.0.1:7890）。", 13f, false, "onSurfaceVariant"))
            DemoKit.put(body, c, 12)
            return
        }

        for (e in list) {
            val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val nm = o.str("name")
            val card = DemoKit.panel(act, theme, 16)
            val top = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
            top.addView(DemoKit.txt(act, theme, nm, 15f, true), LinearLayout.LayoutParams(0, -2, 1f))
            top.addView(DemoKit.badge(act, theme, o.str("type").uppercase(), "primary"))
            val users = usedBy.get(nm)
            if (users != null && users.isJsonArray && users.asJsonArray.size() > 0) {
                top.addView(
                    DemoKit.badge(act, theme, users.asJsonArray.size().toString() + " 渠道", "success"),
                    LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 4) },
                )
            } else {
                top.addView(DemoKit.badge(act, theme, "未引用", "neutral"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 4) })
            }
            DemoKit.put(card, top)
            DemoKit.put(card, DemoKit.txt(act, theme, o.str("host") + ":" + o.long("port") + (if (o.str("username").isNotEmpty()) "  ·  " + o.str("username") else ""), 12f, false, "onSurfaceVariant"), 8)
            if (users != null && users.isJsonArray && users.asJsonArray.size() > 0) {
                DemoKit.put(
                    card,
                    DemoKit.txt(act, theme, "被引用: " + users.asJsonArray.joinToString(", ") { it.asString }, 11f, false, "onSurfaceVariant"),
                    4,
                )
            }
            DemoKit.put(
                card,
                chipRow(
                    listOf(
                        DemoKit.chip(act, theme, "测试", R.drawable.ic_check) { pxTest(nm) },
                        DemoKit.chip(act, theme, "编辑", R.drawable.ic_edit) { pxEdit(o, onChanged) },
                        DemoKit.chip(act, theme, "删除", R.drawable.ic_delete) {
                            UiKit.confirm(act, "删除代理", "删除「" + nm + "」？引用它的渠道会被改成直连。", theme) {
                                pxPost(JsonObject().apply { addProperty("action", "delete"); addProperty("name", nm) }, "已删除", onChanged)
                            }
                        },
                    ),
                ),
                10,
            )
            DemoKit.put(body, card, 8)
        }
    }

    private fun pxEdit(existing: JsonObject?, onChanged: () -> Unit) {
        val col = dCol()
        val fName = field("名字（渠道里填这个，字母数字._-）", existing?.str("name") ?: "")
        val fType = field("类型（socks5 / http）", existing?.str("type") ?: "socks5")
        val fHost = field("主机", existing?.str("host") ?: "127.0.0.1")
        val fPort = field("端口", existing?.let { it.long("port").toString() } ?: "7890", numeric = true)
        val fUser = field("用户名（可空）", existing?.str("username") ?: "")
        val fPass = field(if (existing == null) "密码（可空）" else "密码（留空 = 不改）", "")
        for (v in listOf<View>(fName, fType, fHost, fPort, fUser, fPass)) DemoKit.put(col, v, 10)
        UiKit.customDialog(act, theme, if (existing == null) "新建代理" else "编辑代理", col, "保存") {
            val nm = fName.text.trim()
            if (nm.isEmpty()) { UiKit.toast(act, "名字不能为空"); UiKit.keepOpen(); return@customDialog }
            val pt = fPort.text.trim().toIntOrNull() ?: 0
            if (pt <= 0) { UiKit.toast(act, "端口不合法"); UiKit.keepOpen(); return@customDialog }
            pxPost(JsonObject().apply {
                addProperty("name", nm)
                addProperty("type", fType.text.trim().ifEmpty { "socks5" })
                addProperty("host", fHost.text.trim())
                addProperty("port", pt)
                addProperty("username", fUser.text.trim())
                addProperty("password", fPass.text.trim())
            }, "已保存", onChanged)
        }
    }

    private fun pxTest(nm: String) = io {
        try {
            val r = Api.adminPost(server, "/plugins/proxy/admin/test", JsonObject().apply { addProperty("name", nm) })
            val okk = r.bool("reachable")
            main {
                UiKit.infoDialog(
                    act, theme, if (okk) "代理可达" else "代理不可达",
                    nm + "\n" + (if (okk) "✓ " else "✗ ") + r.str("message") + "\n" + "耗时 " + r.long("ms") + " ms",
                )
            }
        } catch (e: Exception) {
            main { UiKit.toast(act, "测试失败: " + (e.message ?: "")) }
        }
    }

    private fun pxPost(body: JsonObject, okMsg: String, onDone: () -> Unit) = io {
        try {
            Api.adminPost(server, "/plugins/proxy/admin/save", body)
            main { UiKit.toast(act, okMsg); onDone() }
        } catch (e: Exception) {
            main { UiKit.toast(act, "失败: " + (e.message ?: "")) }
        }
    }

    private fun qgPost(body: JsonObject, okMsg: String, onDone: () -> Unit = {}) = io {
        try {
            Api.adminPost(server, "/plugins/model-square/admin/groups", body)
            main { UiKit.toast(act, okMsg); refreshQuick(); onDone() }
        } catch (e: Exception) {
            main { UiKit.toast(act, "失败: " + (e.message ?: "")) }
        }
    }

    private fun refreshQuick() {
        qgSel.clear()
        loadQuickGroup()
        onRefreshChild()
    }

    
    private fun runProbe(onDone: () -> Unit) = io {
        val r = try { Api.adminPost(server, "/plugins/probe/run", JsonObject()) } catch (e: Exception) {
            main { UiKit.toast(act, "探测失败: " + (e.message ?: "") + "（probe 插件可能未启用）") }
            return@io
        }
        val st = try { Api.adminGet(server, "/plugins/probe/status") } catch (_: Exception) { null }
        val sb = StringBuilder()
        val chans = st?.obj("channels")
        if (chans != null) {
            for ((k, v) in chans.entrySet()) {
                val o = v.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                sb.append(k).append("    成功率 ").append(o.long("rate")).append("%")
                sb.append("    ").append(o.long("ok")).append('/').append(o.long("total"))
                val err = o.str("lastErr")
                if (err.isNotEmpty()) sb.append("    ").append(err.take(60))
                sb.append('\n')
            }
        }
        val text = sb.toString().trim().ifEmpty { r.toString() }
        main {
            onDone()
            UiKit.infoDialog(act, theme, "渠道成功率", text)
        }
    }

    private fun queryBalance(ch: String) = io {
        val j = try {
            Api.adminGet(server, "/plugins/balance/query?ch=" + android.net.Uri.encode(ch))
        } catch (e: Exception) {
            main { UiKit.toast(act, "查余额失败: " + (e.message ?: "")) }
            return@io
        }
        main { UiKit.infoDialog(act, theme, "「" + ch + "」余额", j.toString()) }
    }

    fun editChannel(existing: JsonObject?, onDone: () -> Unit) {
        val isNew = existing == null
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        fun g(k: String) = existing?.str(k) ?: ""
        val fName = field("名称（唯一）", g("name"))
        val fType = field("类型 openai | claude | gemini", g("type").ifEmpty { "openai" })
        val fBase = field("Base URL", g("baseUrl"))
        val fKey = field(if (isNew) "API Key" else "API Key（留空 = 不改）", "", secret = true)
        val fModels = field(
            "模型列表（逗号分隔）",
            run {
                val a = existing?.arr("models") ?: return@run ""
                (0 until a.size()).joinToString(",") { a.get(it).asString }
            },
        )
        val fMap = field(
            "模型分组 modelMap（JSON，可空）",
            run {
                val o = existing?.obj("modelMap") ?: return@run ""
                if (o.size() == 0) "" else o.toString()
            },
        )
        val fProxy = field("代理名（可空）", g("proxy"))
        val fDelay = field("延迟 ms", g("delayMs").ifEmpty { "0" }, numeric = true)
        val (swDefRow, swDef) = switchRowRef("设为默认渠道", "无模型命中时走这个", existing?.bool("default") ?: false)
        val (swRespRow, swResp) = switchRowRef("上游用 Responses API", "仅 openai 类型有效", existing?.bool("useResponses") ?: false)

        for (v in listOf<View>(fName, fType, fBase, fKey, fModels, fMap, fProxy, fDelay)) {
            DemoKit.put(col, v, 10)
        }
        DemoKit.put(col, swDefRow, 14)
        DemoKit.put(col, swRespRow, 8)

        UiKit.customDialog(act, theme, if (isNew) "新增渠道" else "编辑渠道", col, "保存") {
            val name = fName.text.trim()
            if (name.isEmpty()) {
                UiKit.toast(act, "名称不能为空"); UiKit.keepOpen(); return@customDialog
            }
            var mapObj: JsonObject? = null
            val mapTxt = fMap.text.trim()
            if (mapTxt.isNotEmpty()) {
                mapObj = try {
                    JsonParser.parseString(mapTxt).asJsonObject
                } catch (_: Exception) {
                    UiKit.toast(act, "modelMap 不是合法 JSON"); UiKit.keepOpen(); return@customDialog
                }
            }
            val modelsArr = JsonArray()
            fModels.text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { modelsArr.add(it) }
            io {
                val c = config()
                val arr = c.arr("channels") ?: JsonArray().also { c.add("channels", it) }
                


                val oldName = existing?.str("name") ?: ""
                val inCfg = if (oldName.isNotEmpty())
                    arr.firstOrNull { it.isJsonObject && it.asJsonObject.str("name") == oldName }?.asJsonObject
                else null
                val ch = inCfg ?: existing ?: JsonObject().apply { addProperty("apiKey", "") }
                if (inCfg == null && existing == null) arr.add(ch)
                ch.addProperty("name", name)
                ch.addProperty("type", fType.text.trim().ifEmpty { "openai" })
                ch.addProperty("baseUrl", fBase.text.trim())
                val k = fKey.text.trim()
                if (k.isNotEmpty()) ch.addProperty("apiKey", k)
                ch.add("models", modelsArr)
                if (mapObj != null) ch.add("modelMap", mapObj) else if (ch.has("modelMap")) ch.remove("modelMap")
                val px = fProxy.text.trim()
                if (px.isEmpty()) ch.remove("proxy") else ch.addProperty("proxy", px)
                ch.addProperty("delayMs", fDelay.text.trim().toIntOrNull() ?: 0)
                ch.addProperty("default", swDef.isChecked)
                ch.addProperty("useResponses", swResp.isChecked)
                if (swDef.isChecked) {
                    for (x in (c.arr("channels") ?: JsonArray())) {
                        if (x.asJsonObject !== ch) x.asJsonObject.addProperty("default", false)
                    }
                }
                saveConfig(c)
                main { UiKit.toast(act, "已保存"); onDone() }
            }
        }
    }

    



    fun buildModels(): View {
        val (root, content) = scrollPage()

        fun reload(): Unit = io {
            val cfg = config()
            val channels = cfg.arr("channels") ?: JsonArray()
            val ms = try { Api.adminGet(server, "/plugins/model-sync/models") } catch (_: Exception) { null }
            val pc = try { Api.adminGet(server, "/admin/api/plugin-config/${uid()}/model-sync") } catch (_: Exception) { null }
            
            val g = try { Api.adminGet(server, "/plugins/model-square/admin/groups") } catch (_: Exception) { null }
            main {
                content.removeAllViews()
                var totalModels = 0
                var totalGroups = (g?.arr("groups")?.size() ?: 0)
                val insts = g?.arr("instances") ?: JsonArray()
                var totalChannels = 0
                for (ie in insts) {
                    val io2 = ie.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                    for (ce in (io2.arr("channels") ?: JsonArray())) {
                        totalChannels++
                        totalModels += (ce.asJsonObject.arr("models")?.size() ?: 0)
                    }
                }
                
                if (insts.size() == 0) {
                    totalChannels = channels.size()
                    for (e in channels) {
                        val ch = e.asJsonObject
                        totalModels += (ch.arr("models")?.size() ?: 0)
                        totalGroups += (ch.obj("modelMap")?.size() ?: 0)
                    }
                }

                section(
                    content,
                    "模型列表 · " + instName(),
                    "共 " + totalChannels + " 个渠道 / " + totalModels + " 个模型（跨 " + maxOf(insts.size(), 1) + " 个实例）· 从上游 /models 拉取并合并进渠道",
                    2,
                )
                DemoKit.put(
                    content,
                    DemoKit.metricRow(
                        act,
                        DemoKit.metricTile(act, theme, totalChannels.toString(), "渠道(全部实例)", "▤"),
                        DemoKit.metricTile(act, theme, totalModels.toString(), "模型总数", "◈"),
                        DemoKit.metricTile(act, theme, totalGroups.toString(), "模型分组", "⇄"),
                    ),
                    10,
                )
                
                if (insts.size() > 0) {
                    val ic = DemoKit.panel(act, theme, 16)
                    DemoKit.put(ic, DemoKit.txt(act, theme, "按实例", 14f, true), 0)
                    for (ie in insts) {
                        val o = ie.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                        val chs = o.arr("channels") ?: JsonArray()
                        var n = 0
                        for (ce in chs) n += (ce.asJsonObject.arr("models")?.size() ?: 0)
                        val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                        row.addView(DemoKit.txt(act, theme, o.str("name"), 12.5f, true), LinearLayout.LayoutParams(0, -2, 1f))
                        if (o.bool("current")) row.addView(DemoKit.badge(act, theme, "当前", "primary"))
                        row.addView(DemoKit.txt(act, theme, "  " + chs.size() + " 渠道 · " + n + " 模型", 11.5f, false, "onSurfaceVariant"))
                        DemoKit.put(ic, row, 8)
                    }
                    DemoKit.put(content, ic, 12)
                }

                
                val op = DemoKit.panel(act, theme, 16)
                DemoKit.put(op, DemoKit.txt(act, theme, "上游同步", 16f, true))
                val last = ms?.obj("lastRun")
                val lastTxt = if (last != null) {
                    "上次: " + shortTime(last.str("at")) + last.str("only").let { if (it.isEmpty()) "" else "（仅 " + it + "）" }
                } else "还没有同步过"
                DemoKit.put(op, DemoKit.txt(act, theme, lastTxt, 11.5f, false, "onSurfaceVariant"), 4)
                if (ms == null) {
                    DemoKit.put(op, DemoKit.txt(act, theme, "当前实例「" + instName() + "」没有启用 model-sync 插件 —— 去「插件」页把它的开关打开（或点「全部启用」）。", 11.5f, false, "error"), 8)
                }
                val acts = LinearLayout(act)
                acts.addView(
                    DemoKit.button(act, theme, "从上游同步模型", "filled") { syncModels(null) { reload() } },
                    LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f),
                )
                acts.addView(
                    DemoKit.button(act, theme, "模型总览", "outlined") { openModelOverview(ms) },
                    LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f).apply { marginStart = DemoKit.dp(act, 8) },
                )
                DemoKit.put(op, acts, 12)
                DemoKit.put(content, op, 12)

                
                val auto = DemoKit.panel(act, theme, 16)
                val mcfg = pc?.obj("config") ?: JsonObject()
                val curIv = mcfg.long("intervalHours").takeIf { it > 0 } ?: 24L
                DemoKit.put(
                    auto,
                    DemoKit.switchRow(
                        act, theme, "自动同步", "服务端按间隔自动拉取上游模型列表并热重载",
                        mcfg.bool("enable"),
                    ) { on -> saveModelSync(mcfg.bool("enable") != on && on, if (on) curIv else 0L) { reload() } },
                )
                DemoKit.put(auto, DemoKit.txt(act, theme, "同步间隔", 13.5f, true), 14)
                DemoKit.put(
                    auto,
                    DemoKit.hscrollRow(
                        act,
                        listOf(6L, 12L, 24L, 72L).map { iv ->
                            DemoKit.chip(act, theme, iv.toString() + " 小时", null, iv == curIv) {
                                saveModelSync(true, iv) { reload() }
                            }
                        },
                    ),
                    8,
                )
                DemoKit.put(content, auto, 14)

                
                section(content, "渠道模型", "共 " + channels.size() + " 个渠道", 20)
                if (channels.size() == 0) {
                    val card = DemoKit.panel(act, theme, 16)
                    DemoKit.put(card, DemoKit.txt(act, theme, "还没有渠道，先去「渠道」页添加。", 12.5f, false, "onSurfaceVariant"))
                    DemoKit.put(content, card, 10)
                }
                for (e in channels) {
                    val ch = e.asJsonObject
                    val name = ch.str("name").ifEmpty { "?" }
                    val mm = ch.obj("modelMap") ?: JsonObject()
                    val models = ch.arr("models") ?: JsonArray()
                    val card = DemoKit.panel(act, theme, 16)
                    val head = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                    head.addView(DemoKit.txt(act, theme, name, 15f, true), LinearLayout.LayoutParams(0, -2, 1f))
                    head.addView(DemoKit.badge(act, theme, models.size().toString() + " 模型", if (models.size() > 0) "primary" else "neutral"))
                    head.addView(
                        DemoKit.badge(act, theme, mm.size().toString() + " 分组", if (mm.size() > 0) "success" else "neutral"),
                        LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
                    )
                    DemoKit.put(card, head)
                    if (models.size() > 0) {
                        val names = (0 until models.size()).take(4).joinToString(", ") { models.get(it).asString }
                        DemoKit.put(
                            card,
                            DemoKit.txt(act, theme, names + if (models.size() > 4) " … 共 " + models.size() + " 个" else "", 11.5f, false, "onSurfaceVariant"),
                            9,
                        )
                    } else {
                        DemoKit.put(card, DemoKit.txt(act, theme, "还没有模型 —— 点「同步该渠道」从上游拉取", 11.5f, false, "onSurfaceVariant"), 9)
                    }
                    DemoKit.put(
                        card,
                        chipRow(
                            listOf(
                                DemoKit.chip(act, theme, "同步该渠道", R.drawable.ic_refresh) { syncModels(name) { reload() } },
                                DemoKit.chip(act, theme, "查看模型", R.drawable.ic_chevron_right) { openModelOverview(ms, name) },
                                DemoKit.chip(act, theme, "加分组", R.drawable.ic_add) { addModelMap(name) { reload() } },
                                DemoKit.chip(act, theme, "编辑渠道", R.drawable.ic_edit) { editChannel(ch) { reload() } },
                            ),
                        ),
                        12,
                    )
                    for ((k, v) in mm.entrySet()) {
                        val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                        row.addView(DemoKit.txt(act, theme, k, 12.5f, false), LinearLayout.LayoutParams(0, -2, 1f))
                        row.addView(DemoKit.txt(act, theme, "→ " + v.asString, 12.5f, true, "primary"))
                        row.addView(
                            DemoKit.chip(act, theme, "删", R.drawable.ic_delete, true) {
                                io {
                                    val c = config()
                                    for (x in (c.arr("channels") ?: JsonArray())) {
                                        val o = x.asJsonObject
                                        if (o.str("name") == name) o.obj("modelMap")?.remove(k)
                                    }
                                    saveConfig(c)
                                    main { UiKit.toast(act, "已删除"); reload() }
                                }
                            },
                            LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 8) },
                        )
                        DemoKit.put(card, row, 10)
                    }
                    DemoKit.put(content, card, 10)
                }
            }
        }
        reload()
        return root
    }

    
    private fun syncModels(only: String?, onDone: () -> Unit) = io {
        try {
            val body = JsonObject()
            if (only != null) body.addProperty("channel", only)
            val r = Api.adminPost(server, "/plugins/model-sync/run", body)
            val results = r.obj("results") ?: JsonObject()
            val sb = StringBuilder()
            var addedTotal = 0
            for ((k, v) in results.entrySet()) {
                val o = v.takeIf { it.isJsonObject }?.asJsonObject
                if (o != null && o.bool("ok")) {
                    addedTotal += o.long("added").toInt()
                    sb.append("✓ ").append(k).append("    上游 ").append(o.long("total")).append(" 个，新增 ").append(o.long("added")).append(" 个\n")
                } else {
                    sb.append("✗ ").append(k).append("    ").append(o?.str("error") ?: "失败").append('\n')
                }
            }
            val text = sb.toString().trim().ifEmpty { "没有可同步的渠道" } + "\n\n本次共新增 " + addedTotal + " 个模型"
            main {
                onDone()
                UiKit.infoDialog(act, theme, if (only != null) "同步「" + only + "」" else "同步全部渠道", text)
            }
        } catch (e: Exception) {
            

            main {
                onDone()
                UiKit.infoDialog(act, theme, "同步失败", (e.message ?: "请求失败") + "\n\n常见原因：model-sync 插件未启用，或该实例端口不通。")
            }
        }
    }

    
    private fun saveModelSync(enable: Boolean, intervalHours: Long, onDone: () -> Unit) = io {
        val iv = if (intervalHours < 1) 24L else intervalHours
        Api.adminPost(
            server, "/admin/api/plugin-config/${uid()}/model-sync",
            JsonObject().apply {
                addProperty("enable", enable)
                addProperty("intervalHours", iv)
            },
        )
        main {
            UiKit.toast(act, if (enable) "已开启自动同步（每 " + iv + " 小时）" else "已关闭自动同步")
            onDone()
        }
    }

    
    private fun openModelOverview(ms: JsonObject?, onlyChannel: String? = null) {
        child(if (onlyChannel != null) "模型 · " + onlyChannel else "模型总览") { host ->
            buildModelOverview(host, ms, onlyChannel)
        }
    }

    private fun buildModelOverview(host: LinearLayout, ms: JsonObject?, onlyChannel: String?) {
        host.removeAllViews()
        val arr = ms?.arr("channels")
        if (arr == null || arr.size() == 0) {
            val card = DemoKit.panel(act, theme, 16)
            DemoKit.put(card, DemoKit.txt(act, theme, "还没有同步过模型，或当前实例「" + instName() + "」没启用 model-sync 插件。", 13f, false, "onSurfaceVariant"))
            DemoKit.put(host, card, 2)
            return
        }
        
        val all = ArrayList<Pair<String, String>>()
        for (e in arr) {
            val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val nm = o.str("name")
            if (onlyChannel != null && nm != onlyChannel) continue
            val models = o.arr("models") ?: continue
            for (i in 0 until models.size()) {
                val m = models.get(i)?.takeIf { !it.isJsonNull }?.asString ?: continue
                all.add(nm to m)
            }
        }
        val uniq = all.map { it.second }.distinct().size
        DemoKit.put(
            host,
            DemoKit.sectionTitle(act, theme, if (onlyChannel != null) onlyChannel else "模型总览", "共 " + all.size + " 条（去重 " + uniq + " 个模型）"),
            0,
        )
        val listHost = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val bar = DemoKit.searchBar(act, theme, "搜索模型名…", onQuery = { q -> drawOverview(listHost, all, q) })
        DemoKit.put(host, bar.view, 12)
        DemoKit.put(host, listHost)
        drawOverview(listHost, all, "")
    }

    private fun drawOverview(listHost: LinearLayout, all: List<Pair<String, String>>, q: String) {
        listHost.removeAllViews()
        val filtered = if (q.isBlank()) all else all.filter { it.second.contains(q, true) || it.first.contains(q, true) }
        if (filtered.isEmpty()) {
            DemoKit.put(listHost, DemoKit.txt(act, theme, "没有匹配的模型", 13f, false, "onSurfaceVariant"))
            return
        }
        val cards = ArrayList<View>()
        for ((ch, m) in filtered.take(200)) {
            val card = DemoKit.panel(act, theme, 16, ripple = true).apply {
                setOnClickListener {
                    DemoKit.copy(act, "model", m)
                    UiKit.toast(act, act.getString(R.string.copied))
                }
            }
            val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(DemoKit.avatar(act, m.take(1).uppercase(), theme.color(act, "primary"), 34), LinearLayout.LayoutParams(DemoKit.dp(act, 34), DemoKit.dp(act, 34)))
            val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            DemoKit.put(col, DemoKit.txt(act, theme, m, 13.5f, true))
            DemoKit.put(col, DemoKit.txt(act, theme, "来自渠道 " + ch, 11f, false, "onSurfaceVariant"), 2)
            row.addView(col, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 10) })
            row.addView(DemoKit.chip(act, theme, "复制", R.drawable.ic_copy) {
                DemoKit.copy(act, "model", m)
                UiKit.toast(act, act.getString(R.string.copied))
            })
            DemoKit.put(card, row)
            DemoKit.put(listHost, card, 8)
            cards.add(card)
        }
        DemoKit.animateInStaggered(cards, 16)
    }

    private fun addModelMap(channel: String, onDone: () -> Unit) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        val fKey = field("客户端模型名（如 gpt-4o）")
        val fVal = field("上游真实模型名（如 deepseek-chat）")
        DemoKit.put(col, fKey, 10)
        DemoKit.put(col, fVal, 10)
        UiKit.customDialog(act, theme, "给「" + channel + "」加分组", col, "保存") {
            val k = fKey.text.trim()
            val v = fVal.text.trim()
            if (k.isEmpty() || v.isEmpty()) {
                UiKit.toast(act, "两项都不能为空"); UiKit.keepOpen(); return@customDialog
            }
            io {
                val c = config()
                for (x in (c.arr("channels") ?: JsonArray())) {
                    val o = x.asJsonObject
                    if (o.str("name") == channel) {
                        val mm = o.obj("modelMap") ?: JsonObject().also { o.add("modelMap", it) }
                        mm.addProperty(k, v)
                    }
                }
                saveConfig(c)
                main { UiKit.toast(act, "已保存"); onDone() }
            }
        }
    }

    



    fun buildPlugins(): View {
        val (root, content) = scrollPage()
        var plugins = JsonArray()

        val fQ = field("搜索插件（名称 / ID / 说明）", "")
        val listBox = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var drawList: () -> Unit = {}
        var reload: () -> Unit = {}

        drawList = {
            listBox.removeAllViews()
            val q = fQ.text.trim().lowercase()
            var shown = 0
            var off = 0
            for (e in plugins) {
                val p = e.asJsonObject
                val pid = p.str("id")
                if (pid.isEmpty()) continue
                if (!p.bool("enable")) off++
                if (q.isNotEmpty()) {
                    val hay = (p.str("name") + " " + pid + " " + p.str("description")).lowercase()
                    if (!hay.contains(q)) continue
                }
                DemoKit.put(listBox, pluginRow(p, pid) { reload() }, 8)
                shown++
            }
            if (shown == 0) {
                val card = DemoKit.panel(act, theme, 16)
                DemoKit.put(
                    card,
                    DemoKit.txt(
                        act, theme,
                        if (plugins.size() == 0) "此实例没有可用插件"
                        else "没有匹配「" + fQ.text.trim() + "」的插件",
                        13f, false, "onSurfaceVariant",
                    ),
                )
                DemoKit.put(listBox, card, 8)
            }
        }

        reload = {
            io {
                val arr = try {
                    Api.adminGet(server, "/admin/api/plugins/${uid()}").arr("plugins") ?: JsonArray()
                } catch (e: Exception) {
                    main {
                        content.removeAllViews()
                        val card = DemoKit.panel(act, theme, 16)
                        DemoKit.put(card, DemoKit.txt(act, theme, "获取插件列表失败: " + (e.message ?: ""), 13f, false, "error"))
                        DemoKit.put(content, card, 2)
                    }
                    return@io
                }
                main {
                    plugins = arr
                    var off = 0
                    for (e in arr) if (e.isJsonObject && !e.asJsonObject.bool("enable")) off++
                    content.removeAllViews()
                    section(
                        content, "插件 · " + instName(),
                        "共 " + arr.size() + " 个 · 已停用 " + off + " 个 · 右侧开关 = 启用 / 停用",
                        2,
                    )
                    DemoKit.put(content, fQ, 8)
                    if (off > 0) {
                        val row = LinearLayout(act)
                        row.addView(
                            DemoKit.button(act, theme, "全部启用", "tonal") { enableAll(true) { reload() } },
                            LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f),
                        )
                        row.addView(
                            DemoKit.button(act, theme, "全部停用", "outlined") { enableAll(false) { reload() } },
                            LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f).apply { marginStart = DemoKit.dp(act, 8) },
                        )
                        DemoKit.put(content, row, 8)
                    }
                    DemoKit.put(content, listBox, 4)
                    drawList()
                }
            }
        }

        
        fQ.edit.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(e: android.text.Editable?) { drawList() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        reload()
        return root
    }

    
    private fun setPluginEnabled(pid: String, enable: Boolean, onDone: () -> Unit) = io {
        try {
            Api.adminPost(server, "/admin/api/plugin-enable", JsonObject().apply {
                addProperty("id", pid); addProperty("enable", enable)
            })
            main { UiKit.toast(act, (if (enable) "已启用 " else "已停用 ") + pid); onDone() }
        } catch (e: Exception) {
            main { UiKit.toast(act, "操作失败: " + (e.message ?: "")) }
        }
    }

    
    private fun enableAll(enable: Boolean, onDone: () -> Unit) = io {
        try {
            Api.adminPost(server, "/admin/api/plugin-enable", JsonObject().apply {
                addProperty("all", true); addProperty("enable", enable)
            })
            main { UiKit.toast(act, if (enable) "已全部启用" else "已全部停用"); onDone() }
        } catch (e: Exception) {
            main { UiKit.toast(act, "操作失败: " + (e.message ?: "")) }
        }
    }


    private fun pluginRow(p: JsonObject, pid: String, onChanged: () -> Unit): View {
        val name = p.str("name").ifEmpty { pid }
        val nCfg = p.arr("schema")?.size() ?: 0
        val enabled = p.bool("enable")
        val running = p.bool("running")
        val hasAdmin = p.bool("hasAdminPage")

        val card = DemoKit.panel(act, theme, 16, ripple = true).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { child(name) { host -> buildPluginDetail(pid, name, host) } }
        }
        val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(
            DemoKit.iconBadge(act, theme, DemoKit.iconRes(p.str("icon")), null, 40),
            LinearLayout.LayoutParams(DemoKit.dp(act, 40), DemoKit.dp(act, 40)),
        )
        val tc = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val h = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        h.addView(DemoKit.txt(act, theme, name, 15f, true))
        h.addView(
            DemoKit.badge(act, theme, if (!enabled) "已禁用" else if (!running) "未运行" else "运行中", if (!enabled || !running) "neutral" else "success"),
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
        )
        h.addView(
            DemoKit.badge(act, theme, if (p.str("type") == "auth") "认证" else "业务", if (p.str("type") == "auth") "primary" else "neutral"),
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
        )
        tc.addView(h)
        val bits = ArrayList<String>()
        bits.add("v" + p.str("version").ifEmpty { "?" })
        bits.add(if (nCfg > 0) nCfg.toString() + " 项配置" else "无需配置")
        if (hasAdmin) bits.add("有管理页")
        DemoKit.put(tc, DemoKit.txt(act, theme, bits.joinToString(" · "), 11.5f, false, "onSurfaceVariant"), 3)
        row.addView(tc, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 10) })
        row.addView(
            DemoKit.themeSwitch(act, theme, enabled).apply {
                setOnCheckedChangeListener { _, v -> setPluginEnabled(pid, v, onChanged) }
            },
            LinearLayout.LayoutParams(-2, -2),
        )
        val chev = android.widget.ImageView(act).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(theme.color(act, "outline")))
        }
        row.addView(chev, LinearLayout.LayoutParams(DemoKit.dp(act, 20), DemoKit.dp(act, 20)))
        DemoKit.put(card, row)
        return card
    }

    fun buildPluginDetail(pluginId: String, name: String, host: LinearLayout) {
        fun reload(): Unit = io {
            val u = uid()
            val plugins = try {
                Api.adminGet(server, "/admin/api/plugins/$u").arr("plugins") ?: JsonArray()
            } catch (_: Exception) {
                JsonArray()
            }
            var meta: JsonObject? = null
            for (e in plugins) if (e.asJsonObject.str("id") == pluginId) meta = e.asJsonObject
            val pc = try { Api.adminGet(server, "/admin/api/plugin-config/$u/$pluginId") } catch (_: Exception) { null }
            main {
                host.removeAllViews()
                if (meta == null) {
                    val card = DemoKit.panel(act, theme, 16)
                    DemoKit.put(card, DemoKit.txt(act, theme, "该插件在当前实例不可用", 13f, false, "error"))
                    DemoKit.put(host, card, 2)
                    return@main
                }
                val m = meta!!
                val info = DemoKit.panel(act, theme, 16)
                val h = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                h.addView(DemoKit.iconBadge(act, theme, DemoKit.iconRes(m.str("icon")), null, 42), LinearLayout.LayoutParams(DemoKit.dp(act, 42), DemoKit.dp(act, 42)))
                val ht = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                DemoKit.put(ht, DemoKit.txt(act, theme, name, 16f, true))
                DemoKit.put(
                    ht,
                    DemoKit.txt(act, theme, "v" + m.str("version") + " · " + pluginId, 11.5f, false, "onSurfaceVariant"),
                    3,
                )
                h.addView(ht, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 10) })
                h.addView(DemoKit.badge(act, theme, if (m.bool("running")) "运行中" else "未运行", if (m.bool("running")) "success" else "neutral"))
                DemoKit.put(info, h)
                if (m.str("description").isNotEmpty()) {
                    DemoKit.put(info, DemoKit.txt(act, theme, m.str("description"), 12.5f, false, "onSurfaceVariant"), 10)
                }
                if (m.bool("hasAdminPage")) {
                    DemoKit.put(
                        info,
                        DemoKit.button(act, theme, "打开管理页面", "tonal") {
                            if (canBuildNative(pluginId)) child(name) { h2 -> buildNativePlugin(pluginId, h2) }
                            else child(name + " 管理页") { h2 -> buildPluginWeb(pluginId, m.str("adminPage"), h2) }
                        },
                        14,
                    )
                }
                DemoKit.put(host, info, 2)

                val schema = pc?.arr("schema")
                if (schema == null || schema.size() == 0) {
                    val card = DemoKit.panel(act, theme, 16)
                    DemoKit.put(
                        card,
                        DemoKit.txt(
                            act, theme,
                            "该插件不需要配置 —— 装上就在工作，没有可调参数。\n" +
                                "（如果它有 App 页面，入口会出现在「用户端」App 的侧边栏里；" +
                                "管理端能操作的插件会在这里显示表单。）",
                            12.5f, false, "onSurfaceVariant",
                        ),
                    )
                    DemoKit.put(host, card, 14)
                    return@main
                }
                val cfgObj = pc.obj("config") ?: JsonObject()
                val card = DemoKit.panel(act, theme, 16)
                DemoKit.put(card, DemoKit.txt(act, theme, "插件配置", 16f, true))
                DemoKit.put(card, DemoKit.txt(act, theme, "修改后点底部「保存并热重载」，无需重启网关。", 12f, false, "onSurfaceVariant"), 6)
                val editors = ArrayList<Pair<String, () -> Any?>>()
                for (sr in schema) {
                    val spec = sr.asJsonObject
                    val key = spec.str("key")
                    if (key.isEmpty()) continue
                    val label = spec.str("label").ifEmpty { key }
                    val type = spec.str("type").ifEmpty { "string" }
                    val secret = spec.bool("secret")
                    val cur = readPath(cfgObj, key)
                    when (type) {
                        "boolean" -> {
                            val (row, sw) = switchRowRef(label, key, cur?.asBoolean ?: false)
                            DemoKit.put(card, row, 14)
                            editors.add(key to { sw.isChecked })
                        }
                        "number" -> {
                            val et = field(label + "  ·  " + key, cur?.asString ?: "", numeric = true)
                            DemoKit.put(card, et, 10)
                            editors.add(key to { et.text.trim().toDoubleOrNull() })
                        }
                        else -> {
                            val et = field(label + "  ·  " + key, cur?.asString ?: "", secret = secret)
                            DemoKit.put(card, et, 10)
                            editors.add(key to { et.text.trim().ifEmpty { null } })
                        }
                    }
                }
                DemoKit.put(
                    card,
                    DemoKit.button(act, theme, "保存并热重载", "filled") {
                        val out = JsonObject()
                        for ((k, get) in editors) writePath(out, k, get())
                        io {
                            try {
                                Api.adminPost(server, "/admin/api/plugin-config/$u/$pluginId", out)
                                main { UiKit.toast(act, name + " 已保存"); reload() }
                            } catch (e: Exception) {
                                main { UiKit.toast(act, "保存失败: " + (e.message ?: "")) }
                            }
                        }
                    },
                    16,
                )
                DemoKit.put(host, card, 14)
            }
        }
        reload()
    }

    private fun readPath(o: JsonObject, path: String): JsonPrimitive? {
        var cur: JsonObject = o
        val parts = path.split('.')
        for (i in 0 until parts.size - 1) {
            cur = cur.obj(parts[i]) ?: return null
        }
        val v = cur.get(parts.last()) ?: return null
        return if (v.isJsonPrimitive) v.asJsonPrimitive else null
    }

    private fun writePath(o: JsonObject, path: String, value: Any?) {
        val parts = path.split('.')
        var cur = o
        for (i in 0 until parts.size - 1) {
            val nx = cur.obj(parts[i]) ?: JsonObject().also { cur.add(parts[i], it) }
            cur = nx
        }
        val last = parts.last()
        when (value) {
            null -> cur.remove(last)
            is Boolean -> cur.addProperty(last, value)
            is Number -> cur.addProperty(last, value)
            else -> cur.addProperty(last, value.toString())
        }
    }

    



    fun canBuildNative(pluginId: String): Boolean =
        pluginId == "auth-user" || pluginId == "tunnel" || pluginId == "proxy"


    fun buildPluginWeb(pluginId: String, adminPage: String, host: LinearLayout) {
        val web = android.webkit.WebView(act)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.setBackgroundColor(theme.color(act, "background"))
        val loading = DemoKit.txt(act, theme, "正在加载插件页面…", 12.5f, false, "onSurfaceVariant").apply {
            gravity = Gravity.CENTER
            setPadding(0, DemoKit.dp(act, 24), 0, DemoKit.dp(act, 24))
        }
        DemoKit.put(host, loading)
        val css = pluginThemeCss()
        web.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView, url: String) {
                loading.visibility = View.GONE
                val js = "(function(){var s=document.getElementById('__gc_theme');" +
                    "if(!s){s=document.createElement('style');s.id='__gc_theme';document.head.appendChild(s);}" +
                    "s.textContent=" + Gson().toJson(css) + ";})();"
                view.evaluateJavascript(js, null)
            }
        }
        
        web.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onShowFileChooser(
                webView: android.webkit.WebView,
                cb: android.webkit.ValueCallback<Array<android.net.Uri>>,
                params: android.webkit.WebChromeClient.FileChooserParams,
            ): Boolean {
                val a = act as? AdminMainActivity ?: return false
                a.launchFileChooser(cb, params.createIntent())
                return true
            }
        }
        val base = Api.base(server)
        val key = server.adminKey() ?: ""
        android.webkit.CookieManager.getInstance().setCookie(base, "adminKey=" + key)
        DemoKit.put(host, web, 8, ViewGroup.LayoutParams.MATCH_PARENT, DemoKit.dp(act, 460))
        web.loadUrl(base + "/plugins/" + pluginId + "/" + adminPage)
    }

    
    private fun pluginThemeCss(): String {
        val m = mapOf(
            "--md-sys-color-primary" to "primary", "--md-sys-color-on-primary" to "onPrimary",
            "--md-sys-color-primary-container" to "primaryContainer", "--md-sys-color-on-primary-container" to "onPrimaryContainer",
            "--md-sys-color-secondary" to "secondary", "--md-sys-color-on-secondary" to "onSecondary",
            "--md-sys-color-secondary-container" to "secondaryContainer", "--md-sys-color-on-secondary-container" to "onSecondaryContainer",
            "--md-sys-color-tertiary" to "tertiary", "--md-sys-color-tertiary-container" to "tertiaryContainer",
            "--md-sys-color-error" to "error", "--md-sys-color-on-error" to "onError",
            "--md-sys-color-error-container" to "errorContainer", "--md-sys-color-on-error-container" to "onErrorContainer",
            "--md-sys-color-surface" to "surface", "--md-sys-color-on-surface" to "onSurface",
            "--md-sys-color-surface-variant" to "surfaceContainerHighest", "--md-sys-color-on-surface-variant" to "onSurfaceVariant",
            "--md-sys-color-surface-container-lowest" to "surfaceContainerLowest",
            "--md-sys-color-surface-container" to "surfaceContainer", "--md-sys-color-surface-container-high" to "surfaceContainerHigh",
            "--md-sys-color-surface-container-highest" to "surfaceContainerHighest",
            "--md-sys-color-outline" to "outline", "--md-sys-color-outline-variant" to "outlineVariant",
            "--md-sys-color-background" to "background", "--md-sys-color-on-background" to "onSurface",
            "--p" to "primary", "--onp" to "onPrimary", "--pc" to "primaryContainer", "--opc" to "onPrimaryContainer",
            "--sc" to "secondaryContainer", "--osc" to "onSecondaryContainer",
            "--ec" to "errorContainer", "--oec" to "onErrorContainer",
            "--sf" to "surface", "--ons" to "onSurface", "--sfc" to "surfaceContainer", "--sfh" to "surfaceContainerHigh",
            "--sfhh" to "surfaceContainerHighest", "--osv" to "onSurfaceVariant", "--olv" to "outlineVariant",
        )
        val sb = StringBuilder(":root{color-scheme:" + (if (DemoKit.isDark(act, theme)) "dark" else "light") + ";")
        for ((k, token) in m) sb.append(k).append(':').append(hex(theme.color(act, token))).append(';')
        sb.append("}")
        sb.append("html,body{background:").append(hex(theme.color(act, "background"))).append(";color:").append(hex(theme.color(act, "onSurface"))).append(";font-family:system-ui,sans-serif;}")
        sb.append("input,select,textarea{background:").append(hex(theme.color(act, "surfaceContainerHighest")))
            .append(";color:").append(hex(theme.color(act, "onSurface")))
            .append(";border:1px solid ").append(hex(theme.color(act, "outlineVariant"))).append(";border-radius:8px;}")
        sb.append(".card,.urow,.krow{border-radius:16px;}")
        sb.append(".btn,.badge{border-radius:999px;}")
        sb.append("::-webkit-scrollbar{width:0;height:0;}")
        return sb.toString()
    }

    private fun hex(c: Int) = String.format("#%06X", 0xFFFFFF and c)

    



    fun buildPlayground(): View {
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.color(act, "background"))
        }
        val stream = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val streamScroll = android.widget.ScrollView(act).apply {
            isFillViewport = false
            addView(stream, FrameLayout.LayoutParams(-1, -2))
        }
        DemoKit.put(root, streamScroll, 0, ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)

        var redraw: () -> Unit = {}
        val msgs = ArrayList<Pair<Boolean, String>>()   
        var model = ""
        var reason = 1
        val reasons = listOf("Low", "Medium", "High", "XHigh", "Max", "Ultra")
        val temps = listOf(0.2, 0.7, 1.0, 1.3, 1.6, 2.0)
        val models = ArrayList<String>()

        val composer = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 12), DemoKit.dp(act, 8), DemoKit.dp(act, 12), DemoKit.dp(act, 12))
            setBackgroundColor(theme.color(act, "surfaceContainer"))
        }
        val inputRow = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        val input = DemoKit.chatInput(act, theme, "输入消息…", 1).apply {
            
            layoutParams = LinearLayout.LayoutParams(0, DemoKit.dp(act, 46), 1f)
        }
        val sendBtn = UiKit.button(act, theme, "", "filled").apply {
            setIconResource(R.drawable.ic_send)
            contentDescription = "发送"
            

            insetTop = 0
            insetBottom = 0
            cornerRadius = DemoKit.dp(act, 23)
            iconSize = DemoKit.dp(act, 20)
            iconPadding = 0
        }
        inputRow.addView(input)
        inputRow.addView(sendBtn, LinearLayout.LayoutParams(DemoKit.dp(act, 46), DemoKit.dp(act, 46)).apply { marginStart = DemoKit.dp(act, 8) })
        val chipLine = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        val modelChip = DemoKit.chip(act, theme, "模型…", R.drawable.ic_tune) {
            if (models.isEmpty()) { UiKit.toast(act, "模型列表加载中…"); return@chip }
            bottomSheet("选择模型", models, models.indexOf(model), searchable = true) { i ->
                model = models[i]
                redraw()
            }
        }
        val reasonChip = DemoKit.chip(act, theme, "推理强度 " + reasons[reason], R.drawable.ic_bolt) {
            bottomSheet("推理强度", reasons, reason) { i ->
                reason = i
                redraw()
            }
        }
        chipLine.addView(modelChip)
        chipLine.addView(reasonChip, LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 8) })
        chipLine.addView(View(act), LinearLayout.LayoutParams(0, 1, 1f))
        chipLine.addView(DemoKit.chip(act, theme, "对话", R.drawable.ic_chat) { openConvo() })
        val clearChip = DemoKit.chip(act, theme, "清空", R.drawable.ic_delete) {
            msgs.clear()
            redraw()
        }
        chipLine.addView(clearChip, LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) })
        composer.addView(inputRow)
        composer.addView(chipLine, LinearLayout.LayoutParams(-1, -2).apply { topMargin = DemoKit.dp(act, 6) })
        root.addView(composer, LinearLayout.LayoutParams(-1, -2))

        redraw = {
            stream.removeAllViews()
            val pad = DemoKit.dp(act, 16)
            stream.setPadding(pad, DemoKit.dp(act, 14), pad, DemoKit.dp(act, 14))
            val maxW = (act.resources.displayMetrics.widthPixels * 0.78f).toInt()
            if (msgs.isEmpty()) {
                DemoKit.put(stream, DemoKit.txt(act, theme, "直接在这里试玩网关。选择模型后输入内容即可。", 12.5f, false, "onSurfaceVariant"))
            }
            for ((mine, text) in msgs) {
                DemoKit.put(stream, DemoKit.chatBubble(act, theme, text, mine, maxW), 10)
            }
            modelChip.text = if (model.isEmpty()) "模型…" else if (model.length > 12) model.take(11) + "…" else model
            reasonChip.text = "推理强度 " + reasons[reason]
        }

        fun send() {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return
            input.setText("")
            msgs.add(true to text)
            msgs.add(false to "…")
            if (playConvos.size == 0 || playConvos[0] != text) {
                playConvos.add(0, text.take(40))
                while (playConvos.size > 12) playConvos.removeAt(playConvos.size - 1)
            }
            redraw()
            streamScroll.post { streamScroll.fullScroll(View.FOCUS_DOWN) }
            val useModel = model.ifEmpty { models.firstOrNull() ?: "" }
            io {
                val reply = try {
                    val body = JsonObject().apply {
                        addProperty("model", useModel)
                        addProperty("temperature", temps[reason])
                        add("messages", JsonArray().apply {
                            for ((mine, t) in msgs) {
                                if (t == "…") continue
                                add(JsonObject().apply {
                                    addProperty("role", if (mine) "user" else "assistant")
                                    addProperty("content", t)
                                })
                            }
                        })
                        addProperty("stream", false)
                    }
                    val r = Api.post(Api.base(server) + "/v1/chat/completions", body, Api.adminUserHeaders(server))
                    r.arr("choices")?.let { c ->
                        c.get(0)?.takeIf { it.isJsonObject }?.asJsonObject?.obj("message")?.str("content")
                    } ?: r.toString()
                } catch (e: Exception) {
                    "请求失败: " + (e.message ?: "")
                }
                main {
                    msgs.removeAt(msgs.size - 1)
                    msgs.add(false to reply)
                    redraw()
                    streamScroll.post { streamScroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }
        sendBtn.setOnClickListener { send() }
        input.setOnEditorActionListener { _, _, _ -> send(); true }

        
        io {
            try {
                val r = Api.get(Api.base(server) + "/v1/models", Api.adminUserHeaders(server))
                val ids = r.arr("data")?.mapNotNull { o ->
                    o.takeIf { it.isJsonObject }?.asJsonObject?.str("id")?.takeIf { it.isNotBlank() }
                } ?: emptyList()
                main {
                    models.clear()
                    models.addAll(ids)
                    if (model.isEmpty()) model = ids.firstOrNull() ?: ""
                    redraw()
                }
            } catch (_: Exception) {
            }
        }
        redraw()
        return root
    }

    



    private fun bottomSheet(title: String, options: List<String>, selected: Int = -1, searchable: Boolean = false, onPick: (Int) -> Unit) {
        val dlg = com.google.android.material.bottomsheet.BottomSheetDialog(act)
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 12), DemoKit.dp(act, 16), DemoKit.dp(act, 12), DemoKit.dp(act, 20))
            setBackgroundColor(theme.color(act, "surfaceContainer"))
        }
        DemoKit.put(col, DemoKit.txt(act, theme, title + if (options.size > 1) "（共 " + options.size + " 项）" else "", 16f, true), 0)

        val listHost = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        



        val sv = androidx.core.widget.NestedScrollView(act).apply { addView(listHost, FrameLayout.LayoutParams(-1, -2)) }
        var filter = ""
        var rowCount = options.size
        fun draw() {
            listHost.removeAllViews()
            val kw = filter.trim().lowercase()
            val shown = options.withIndex().filter { kw.isEmpty() || it.value.lowercase().contains(kw) }
            rowCount = shown.size
            if (shown.isEmpty()) {
                DemoKit.put(listHost, DemoKit.txt(act, theme, "没有匹配的项", 13f, false, "onSurfaceVariant"), 10)
            }
            for ((i, o) in shown) {
                val row = LinearLayout(act).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(DemoKit.dp(act, 12), DemoKit.dp(act, 12), DemoKit.dp(act, 12), DemoKit.dp(act, 12))
                    background = DemoKit.rounded(act, if (i == selected) theme.color(act, "secondaryContainer") else Color.TRANSPARENT, 12)
                    isClickable = true
                    setOnClickListener { dlg.dismiss(); onPick(i) }
                }
                row.addView(DemoKit.txt(act, theme, o, 14f, i == selected), LinearLayout.LayoutParams(0, -2, 1f))
                if (i == selected) {
                    val ic = android.widget.ImageView(act).apply {
                        setImageResource(R.drawable.ic_check)
                        setImageTintList(ColorStateList.valueOf(theme.color(act, "primary")))
                    }
                    row.addView(ic, LinearLayout.LayoutParams(DemoKit.dp(act, 20), DemoKit.dp(act, 20)))
                }
                DemoKit.put(listHost, row, 4)
            }
            
            val maxH = (act.resources.displayMetrics.heightPixels * 0.62f).toInt()
            val want = DemoKit.dp(act, 46) * rowCount + DemoKit.dp(act, 10)
            sv.layoutParams = LinearLayout.LayoutParams(-1, minOf(want, maxH))
        }
        if (searchable) {
            val f = MdField(act, theme, "搜索", "", false, true)
            f.edit.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) { filter = s?.toString() ?: ""; draw() }
            })
            DemoKit.put(col, f, 10)
        }
        draw()
        col.addView(sv)
        dlg.setContentView(col)
        

        dlg.setOnShowListener {
            dlg.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            dlg.behavior.skipCollapsed = true
            UiKit.tintDialogWindow(dlg, theme, bottomSheet = true)
        }
        dlg.show()
    }

    



    fun buildUsers(): View {
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.color(act, "background"))
        }
        val host = DemoKit.pageColumn(act, theme, bottomSpace = 40)
        val scroll = android.widget.ScrollView(act).apply {
            isFillViewport = true
            clipToPadding = false
            addView(host, FrameLayout.LayoutParams(-1, -2))
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        
        val footer = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(DemoKit.dp(act, 12), DemoKit.dp(act, 8), DemoKit.dp(act, 12), DemoKit.dp(act, 12))
            setBackgroundColor(theme.color(act, "surfaceContainer"))
        }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))

        var reload: () -> Unit = {}
        var rebuildFooter: () -> Unit = {}
        var tab = 0
        var query = ""
        var sortDesc = true
        var filterBanned = false
        var showUserKeys = false
        var usersCache = JsonArray()
        var keysCache = JsonArray()
        var listHost: LinearLayout? = null
        var toolsHost: LinearLayout? = null
        var seg: DemoKit.SegmentedButtons? = null

        
        fun renderList() {
            val list = listHost ?: return
            list.removeAllViews()
            val q = query.trim().lowercase()
            val cards = ArrayList<View>()
            if (tab == 0) {
                var uu = (0 until usersCache.size()).map { usersCache.get(it).asJsonObject }
                if (filterBanned) uu = uu.filter { it.bool("banned") }
                if (q.isNotEmpty()) uu = uu.filter { o ->
                    o.str("uid").lowercase().contains(q) || o.str("nickname").lowercase().contains(q) ||
                        o.str("name").lowercase().contains(q) || o.str("email").lowercase().contains(q) || o.str("note").lowercase().contains(q)
                }
                uu = if (sortDesc) uu.sortedByDescending { it.str("uid") } else uu.sortedBy { it.str("uid") }
                DemoKit.put(list, DemoKit.txt(act, theme, "共 " + uu.size + " 条记录 · 点卡片查看详情", 11f, false, "onSurfaceVariant"))
                if (uu.isEmpty()) {
                    DemoKit.put(list, DemoKit.txt(act, theme, if (q.isEmpty()) "还没有用户" else "没有匹配的用户", 13f, false, "onSurfaceVariant"), 12)
                }
                for (o in uu) {
                    val c = userCard(o, { child("用户详情") { h -> userDetail(o, h, { reload() }) } }, { reload() })
                    cards.add(c)
                    DemoKit.put(list, c, 8)
                }
            } else {
                var kk = (0 until keysCache.size()).map { keysCache.get(it).asJsonObject }
                
                val allCount = kk.size
                if (!showUserKeys) kk = kk.filter { isAdminIssued(it) }
                if (q.isNotEmpty()) kk = kk.filter { o ->
                    o.str("key").lowercase().contains(q) || o.str("name").lowercase().contains(q) || o.str("uid").lowercase().contains(q)
                }
                kk = if (sortDesc) kk.sortedByDescending { it.str("name") } else kk.sortedBy { it.str("name") }
                val txt = if (showUserKeys) "共 " + kk.size + " 张卡（含用户卡）· 点卡片管理"
                          else "管理员分发 " + kk.size + " 张 · 另有 " + (allCount - kk.size) + " 张属于用户"
                DemoKit.put(list, DemoKit.txt(act, theme, txt, 11f, false, "onSurfaceVariant"))
                if (kk.isEmpty()) {
                    DemoKit.put(list, DemoKit.txt(act, theme, if (q.isEmpty()) "还没有卡密" else "没有匹配的卡密", 13f, false, "onSurfaceVariant"), 12)
                }
                for (o in kk) {
                    val c = keyCard(o) { reload() }
                    cards.add(c)
                    DemoKit.put(list, c, 8)
                }
            }
            DemoKit.animateInStaggered(cards, 22)
        }

        
        fun drawTools() {
            val tools = toolsHost ?: return
            tools.removeAllViews()
            tools.addView(DemoKit.chip(act, theme, if (sortDesc) "倒序" else "正序", R.drawable.ic_sort, false) {
                sortDesc = !sortDesc
                drawTools()
                renderList()
            })
            if (tab == 0) {
                tools.addView(
                    DemoKit.chip(act, theme, "仅看封禁", R.drawable.ic_filter, filterBanned) {
                        filterBanned = !filterBanned
                        drawTools()
                        renderList()
                    },
                    LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 8) },
                )
            } else {
                tools.addView(
                    DemoKit.chip(act, theme, "含用户卡密", R.drawable.ic_filter, showUserKeys) {
                        showUserKeys = !showUserKeys
                        drawTools()
                        renderList()
                    },
                    LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 8) },
                )
            }
            tools.addView(View(act), LinearLayout.LayoutParams(0, 1, 1f))
            tools.addView(DemoKit.chip(act, theme, "刷新", R.drawable.ic_refresh) { reload() })
        }

        
        fun shell() {
            host.removeAllViews()
            
            val s2 = DemoKit.segmented(act, theme, listOf("用户", "卡密"), tab) { i ->
                tab = i
                renderList()
                drawTools()
                rebuildFooter()
                DemoKit.animateIn(footer, 6)
            }
            seg = s2
            DemoKit.put(host, s2, 2)

            val bar = DemoKit.searchBar(act, theme, if (tab == 0) "搜索 UID / 昵称 / 邮箱…" else "搜索卡号 / 卡名…", onQuery = { q ->
                query = q
                renderList()
            })
            bar.input.setText(query)
            DemoKit.put(host, bar.view, 12)

            val tools = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
            toolsHost = tools
            DemoKit.put(host, tools, 12)
            drawTools()

            val list = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            listHost = list
            DemoKit.put(host, list)
            renderList()
        }

        rebuildFooter = {
            footer.removeAllViews()
            footer.addView(DemoKit.button(act, theme, "设置", "outlined") {
                child("用户系统设置") { h -> buildNativePlugin("auth-user", h) }
            }, LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f))
            val add = if (tab == 0) "添加用户" else "生成卡密"
            footer.addView(
                DemoKit.button(act, theme, add, "filled") {
                    if (tab == 0) newUserDialog { reload() } else newKeyDialog(usersCache) { reload() }
                },
                LinearLayout.LayoutParams(0, DemoKit.dp(act, 40), 1f).apply { marginStart = DemoKit.dp(act, 8) },
            )
        }

        reload = {
            io {
                usersCache = try {
                    Api.adminGet(server, "/plugins/auth-user/admin/users").arr("users") ?: JsonArray()
                } catch (_: Exception) {
                    JsonArray()
                }
                keysCache = try {
                    Api.adminGet(server, "/plugins/auth-cardkey/admin/keys").arr("keys") ?: JsonArray()
                } catch (_: Exception) {
                    JsonArray()
                }
                main {
                    shell()
                    rebuildFooter()
                }
            }
        }
        reload()
        return root
    }

    private fun userCard(u: JsonObject, onOpen: () -> Unit, onChanged: () -> Unit): View {
        val uid = u.str("uid")
        val nick = u.str("nickname").ifEmpty { u.str("name") }.ifEmpty { uid }
        val banned = u.bool("banned")
        val card = DemoKit.panel(act, theme, 16, ripple = true).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { onOpen() }
        }
        val top = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(DemoKit.avatarAuto(act, theme, u.str("avatar"), nick.take(1).uppercase(), 38), LinearLayout.LayoutParams(DemoKit.dp(act, 38), DemoKit.dp(act, 38)))
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val nameRow = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(DemoKit.txt(act, theme, nick, 15f, true))
        nameRow.addView(
            DemoKit.badge(act, theme, if (banned) "已封禁" else "正常", if (banned) "error" else "success"),
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
        )
        col.addView(nameRow)
        val uname = u.str("username")
        val sub = StringBuilder(uid)
        if (uname.isNotEmpty() && uname != nick) sub.append(" · ").append(uname)
        if (u.str("email").isNotEmpty()) sub.append(" · ").append(u.str("email"))
        DemoKit.put(col, DemoKit.txt(act, theme, sub.toString(), 11.5f, false, "onSurfaceVariant"), 3)
        top.addView(col, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 10) })
        top.addView(DemoKit.iconButton(act, theme, R.drawable.ic_chevron_right, "详情") { onOpen() }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
        DemoKit.put(card, top)

        val stats = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        stats.addView(DemoKit.txt(act, theme, "卡 " + u.get("keyCount")?.takeIf { !it.isJsonNull }?.asInt.let { it ?: 0 }, 12f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(DemoKit.txt(act, theme, "额度 " + AdminKeyFmt(u.get("totalQuota")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0), 12f, true))
        stats.addView(DemoKit.txt(act, theme, "  已用 " + AdminKeyFmt(u.get("totalUsed")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0), 12f, false, "onSurfaceVariant"))
        DemoKit.put(card, stats, 10)

        val acts = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_chat, "推送通知") { pushNotice(uid, nick) }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_wallet, "充值") { rechargeUser(uid) }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)).apply { marginStart = DemoKit.dp(act, 8) })
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_key, "重置密码") { resetPassword(uid) }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)).apply { marginStart = DemoKit.dp(act, 8) })
        acts.addView(View(act), LinearLayout.LayoutParams(0, 1, 1f))
        acts.addView(DemoKit.iconButton(act, theme, if (banned) R.drawable.ic_check else R.drawable.ic_block, if (banned) "解封" else "封禁") {
            userAction(if (banned) "unban" else "ban", uid, null) { onChanged() }
        }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_delete, "删除") {
            UiKit.confirm(act, "删除用户", "确定删除「" + nick + "」？其名下卡密也会一并处理。") {
                io {
                    Api.adminPost(server, "/plugins/auth-user/admin/users", JsonObject().apply {
                        addProperty("action", "delete"); addProperty("uid", uid)
                    })
                    main { UiKit.toast(act, "已删除"); onChanged() }
                }
            }
        }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)).apply { marginStart = DemoKit.dp(act, 8) })
        DemoKit.put(card, acts, 12)
        return card
    }

    private fun AdminKeyFmt(v: Double): String = when {
        v < 0 -> "不限"
        v >= 100_000_000 -> String.format("%.1f亿", v / 100000000.0)
        v >= 10_000 -> String.format("%.1f万", v / 10000.0)
        v == v.toLong().toDouble() -> v.toLong().toString()
        v >= 1 -> String.format("%.2f", v).trimEnd('0').trimEnd('.')
        v > 0 -> String.format("%.6f", v).trimEnd('0').trimEnd('.')
        else -> "0"
    }

    private fun userAction(action: String, uid: String, extra: JsonObject?, onDone: () -> Unit) = io {
        val body = JsonObject().apply {
            addProperty("action", action)
            addProperty("uid", uid)
        }
        extra?.let { for ((k, v) in it.entrySet()) body.add(k, v) }
        Api.adminPost(server, "/plugins/auth-user/admin/users", body)
        main { UiKit.toast(act, "已完成"); onDone() }
    }

    




    private fun pushNotice(uid: String, nick: String) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        val f = field("通知内容（一条一行）")
        DemoKit.put(col, f, 10)
        DemoKit.put(col, DemoKit.txt(act, theme, "发给 " + nick + "（UID " + uid + "）\n⚠️ 定向通知写在公告串里，能读 bootstrap 的人可见，别发敏感内容。", 11.5f, false, "onSurfaceVariant"), 10)
        UiKit.customDialog(act, theme, "推送通知", col, "发送") {
            val text = f.text.trim()
            if (text.isEmpty()) {
                UiKit.toast(act, "内容不能为空")
                UiKit.keepOpen(); return@customDialog
            }
            io {
                val info = Api.adminGet(server, "/admin/api/server-info").obj("serverInfo") ?: JsonObject()
                val list = Notices.parse(info.str("announcement")).toMutableList()
                for (line in text.split('\n')) {
                    if (line.trim().isEmpty()) continue
                    list.add(Notices.Item("", uid, line.trim()))
                }
                val encoded = Notices.encode(list)
                if (encoded.length > Notices.MAX_LENGTH) {
                    main { UiKit.toast(act, "公告总长超过 " + Notices.MAX_LENGTH + " 字，请先清理旧公告") }
                    return@io
                }
                Api.adminPost(server, "/admin/api/server-info", JsonObject().apply {
                    addProperty("name", info.str("name"))
                    addProperty("description", info.str("description"))
                    addProperty("icon", info.str("icon"))
                    addProperty("contact", info.str("contact"))
                    addProperty("website", info.str("website"))
                    addProperty("announcement", encoded)
                })
                main { UiKit.toast(act, "已推送") }
            }
        }
    }

    private fun rechargeUser(uid: String) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        val f = field("充值 / 扣减（正数增加，负数扣减）", "", numeric = true)
        DemoKit.put(col, f, 10)
        UiKit.customDialog(act, theme, "充值 / 扣减", col, "提交") {
            val v = f.text.trim().toDoubleOrNull() ?: 0.0
            if (v == 0.0) {
                UiKit.toast(act, "请输入数量"); UiKit.keepOpen(); return@customDialog
            }
            io {
                


                val keys = Api.adminGet(server, "/plugins/auth-cardkey/admin/keys").arr("keys") ?: JsonArray()
                var done: Boolean? = false
                for (e in keys) {
                    val k = e.asJsonObject
                    val ku = k.get("uid")?.takeIf { !it.isJsonNull }?.asString ?: ""
                    if (ku == uid) {
                        val kq = k.get("quotaTokens")?.asDouble ?: 0.0
                        if (kq == -1.0) { done = null; break }
                        Api.adminPost(server, "/plugins/auth-cardkey/admin/keys-update", JsonObject().apply {
                            addProperty("key", k.str("key"))
                            if (v > 0) addProperty("addQuota", v) else addProperty("quotaTokens", kq + v)
                        })
                        done = true
                        break
                    }
                }
                val ok = done
                main {
                    UiKit.toast(
                        act,
                        when (ok) {
                            true -> "已提交"
                            null -> "该用户的主卡是不限额度，无需充值"
                            else -> "该用户还没有卡密，请先生成一张"
                        },
                    )
                }
            }
        }
    }

    private fun resetPassword(uid: String) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        val f = field("新密码（≥8 位）", "", secret = true)
        DemoKit.put(col, f, 10)
        UiKit.customDialog(act, theme, "重置密码", col, "提交") {
            val pw = f.text.trim()
            if (pw.length < 8) {
                UiKit.toast(act, "密码至少 8 位"); UiKit.keepOpen(); return@customDialog
            }
            userAction("resetPassword", uid, JsonObject().apply { addProperty("password", pw) }) { }
        }
    }

    private fun userDetail(u: JsonObject, host: LinearLayout, onDone: () -> Unit = {}) {
        val uid = u.str("uid")
        val nick = u.str("nickname").ifEmpty { u.str("name") }.ifEmpty { uid }
        val head = DemoKit.panel(act, theme, 16)
        val hrow = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        hrow.addView(DemoKit.avatarAuto(act, theme, u.str("avatar"), nick.take(1).uppercase(), 56), LinearLayout.LayoutParams(DemoKit.dp(act, 56), DemoKit.dp(act, 56)))
        val ht = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(ht, DemoKit.txt(act, theme, nick, 19f, true))
        DemoKit.put(ht, DemoKit.txt(act, theme, uid, 12f, false, "onSurfaceVariant"), 4)
        DemoKit.put(ht, DemoKit.badge(act, theme, if (u.bool("banned")) "已封禁" else "正常", if (u.bool("banned")) "error" else "success"), 8)
        hrow.addView(ht, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 12) })
        DemoKit.put(head, hrow)
        DemoKit.put(host, head, 2)

        val f = DemoKit.panel(act, theme, 16)
        DemoKit.put(f, DemoKit.txt(act, theme, "基本资料", 16f, true))
        DemoKit.put(f, DemoKit.valueRow(act, theme, "邮箱", u.str("email").ifEmpty { "未绑定" }), 12)
        DemoKit.put(f, DemoKit.valueRow(act, theme, "备注", u.str("note").ifEmpty { "—" }), 10)
        DemoKit.put(f, DemoKit.valueRow(act, theme, "创建", u.str("createdAt").ifEmpty { "—" }), 10)
        DemoKit.put(f, DemoKit.valueRow(act, theme, "卡数", (u.get("keyCount")?.takeIf { !it.isJsonNull }?.asInt ?: 0).toString()), 10)
        DemoKit.put(f, DemoKit.valueRow(act, theme, "总额度", AdminKeyFmt(u.get("totalQuota")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0)), 10)
        DemoKit.put(f, DemoKit.valueRow(act, theme, "已用", AdminKeyFmt(u.get("totalUsed")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0)), 10)
        DemoKit.put(host, f, 14)

        DemoKit.put(host, DemoKit.txt(act, theme, "用户操作", 12f, true, "onSurfaceVariant"), 20)
        DemoKit.put(host, DemoKit.menuCard(act, theme, "充值 / 扣减额度", "调整该用户主卡的额度", R.drawable.ic_wallet) { rechargeUser(uid) }, 8)
        DemoKit.put(host, DemoKit.menuCard(act, theme, "重置密码", "为用户设置新密码", R.drawable.ic_key) { resetPassword(uid) }, 8)
        DemoKit.put(host, DemoKit.menuCard(act, theme, "编辑资料", "昵称 / 邮箱 / 备注", R.drawable.ic_edit) {
            val col = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
            }
            val fNick = field("昵称（可空）", u.str("nickname"))
            val fMail = field("邮箱（可空）", u.str("email"))
            val fNote = field("备注（给自己看）", u.str("note"))
            DemoKit.put(col, fNick, 10)
            DemoKit.put(col, fMail, 10)
            DemoKit.put(col, fNote, 10)
            UiKit.customDialog(act, theme, "编辑资料", col, "保存") {
                userAction(
                    "update", uid,
                    JsonObject().apply {
                        addProperty("nickname", fNick.text.trim())
                        addProperty("email", fMail.text.trim())
                        addProperty("note", fNote.text.trim())
                    },
                ) { onDone() }
            }
        }, 8)
        DemoKit.put(
            host,
            if (u.bool("banned")) DemoKit.menuCard(act, theme, "解封用户", "恢复该用户登录与调用", R.drawable.ic_check) {
                userAction("unban", uid, null) { }
            }
            else DemoKit.dangerCard(act, theme, "封禁用户", "封禁后无法登录，可随时解封", R.drawable.ic_block) {
                userAction("ban", uid, null) { }
            },
            8,
        )
        DemoKit.put(host, DemoKit.dangerCard(act, theme, "删除用户", "连同其卡密一起删除，不可恢复", R.drawable.ic_delete) {
            UiKit.confirm(act, "删除用户", "确定删除「" + nick + "」？") {
                io {
                    Api.adminPost(server, "/plugins/auth-user/admin/users", JsonObject().apply {
                        addProperty("action", "delete"); addProperty("uid", uid)
                    })
                    main { UiKit.toast(act, "已删除") }
                }
            }
        }, 8)
    }

    private fun newUserDialog(onDone: () -> Unit) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        val fUser = field("用户名（3-32 位：字母 / 数字 / _ / -）")
        val fPwd = field("密码（至少 8 位）", "", secret = true)
        val fMail = field("邮箱（可空）")
        val fNote = field("备注（可空）")
        DemoKit.put(col, fUser, 10)
        DemoKit.put(col, fPwd, 10)
        DemoKit.put(col, fMail, 10)
        DemoKit.put(col, fNote, 10)
        DemoKit.put(col, DemoKit.txt(act, theme, "UID 由服务端自动生成（u + 8 位十六进制），无需填写。", 11.5f, false, "onSurfaceVariant"), 10)
        UiKit.customDialog(act, theme, "添加用户", col, "创建") {
            val username = fUser.text.trim()
            val pwd = fPwd.text
            
            if (!Regex("^[A-Za-z0-9_-]{3,32}$").matches(username)) {
                UiKit.toast(act, "用户名需 3-32 位，只能用字母、数字、下划线、短横线")
                UiKit.keepOpen(); return@customDialog
            }
            if (pwd.length < 8) {
                UiKit.toast(act, "密码至少 8 位")
                UiKit.keepOpen(); return@customDialog
            }
            io {
                val r = Api.adminPost(
                    server, "/plugins/auth-user/admin/users",
                    JsonObject().apply {
                        addProperty("action", "create")
                        addProperty("username", username)
                        addProperty("password", pwd)
                        addProperty("email", fMail.text.trim())
                        addProperty("note", fNote.text.trim())
                    },
                )
                val key = r.str("key")
                main {
                    onDone()
                    if (key.isNotEmpty()) {
                        DemoKit.copy(act, "key", key)
                        UiKit.infoDialog(
                            act, theme, "用户已创建（卡密已复制）",
                            username + " 已创建，并自动发了一张主卡：\n\n" + key +
                                "\n\n额度为 0（零额度）时用户无法调用，去「卡密」标签页或用户详情里充值即可。",
                        )
                    } else {
                        UiKit.toast(act, "已创建用户 " + username)
                    }
                }
            }
        }
    }

    private fun keyCard(k: JsonObject, onDone: () -> Unit): View {
        val key = k.str("key")
        val name = k.str("name").ifEmpty { "(未命名)" }
        val enable = k.bool("enable")
        val quota = k.get("quotaTokens")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
        val used = k.get("usedTokens")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
        val uid = k.str("uid")
        val card = DemoKit.panel(act, theme, 16)
        val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(DemoKit.iconBadge(act, theme, R.drawable.ic_key, null, 42), LinearLayout.LayoutParams(DemoKit.dp(act, 42), DemoKit.dp(act, 42)))
        val text = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val nameRow = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(DemoKit.txt(act, theme, name, 14f, true))
        nameRow.addView(
            DemoKit.badge(
                act, theme,
                
                when {
                    !enable -> "已禁用"
                    quota == 0.0 -> "零额度"
                    quota == -1.0 -> "不限量"
                    else -> "正常"
                },
                when {
                    !enable -> "error"
                    quota == 0.0 -> "warning"
                    else -> "success"
                },
            ),
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
        )
        if (uid.isNotEmpty()) nameRow.addView(
            DemoKit.badge(act, theme, "已绑定", "primary"),
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = DemoKit.dp(act, 6) },
        )
        text.addView(nameRow)
        DemoKit.put(text, DemoKit.txt(act, theme, maskKey(key), 11.5f, false, "onSurfaceVariant"), 3)
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 10) })
        row.addView(DemoKit.iconButton(act, theme, R.drawable.ic_copy, "复制卡号") { copyText("key", key) }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
        DemoKit.put(card, row)

        val quotaLine = when {
            quota == 0.0 -> "零额度（不可用，需充值） · 已用 " + AdminKeyFmt(used)
            quota == -1.0 -> "不限额度 · 已用 " + AdminKeyFmt(used)
            else -> "额度 " + AdminKeyFmt(quota) + " · 已用 " + AdminKeyFmt(used)
        }
        val origin = k.str("expiresAt")
        DemoKit.put(card, DemoKit.txt(act, theme, quotaLine + (if (origin.isNotEmpty()) " · 到期 " + origin else ""), 12f, false, "onSurfaceVariant"), 10)
        if (quota > 0) {
            DemoKit.put(card, DemoKit.progressRow(act, theme, "用量", used.toLong(), quota.toLong(), ((used * 100.0 / quota).toInt()).toString() + "%"), 12)
        }

        val acts = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_edit, "改名") {
            val cur = k.str("name")
            val f = MdField(act, theme, "卡名", cur)
            val col2 = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0) }
            DemoKit.put(col2, f, 10)
            UiKit.customDialog(act, theme, "改名", col2, "保存") {
                io {
                    Api.adminPost(server, "/plugins/auth-cardkey/admin/keys-update", com.google.gson.JsonObject().apply {
                        addProperty("key", k.str("key")); addProperty("name", f.text.trim())
                    })
                    main { UiKit.toast(act, "已改名"); onDone() }
                }
            }
        }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_edit, "编辑") { editKeyDialog(k) { onDone() } }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_wallet, "充值") { rechargeDialog(k) { onDone() } }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)).apply { marginStart = DemoKit.dp(act, 8) })
        acts.addView(View(act), LinearLayout.LayoutParams(0, 1, 1f))
        acts.addView(DemoKit.iconButton(act, theme, if (enable) R.drawable.ic_block else R.drawable.ic_check, if (enable) "禁用" else "启用") {
            io {
                Api.adminPost(server, "/plugins/auth-cardkey/admin/keys-update", JsonObject().apply {
                    addProperty("key", key); addProperty("enable", !enable)
                })
                main { UiKit.toast(act, if (enable) "已禁用" else "已启用"); onDone() }
            }
        }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
        acts.addView(DemoKit.iconButton(act, theme, R.drawable.ic_delete, "删除") {
            UiKit.confirm(act, "删除卡密", "确定删除「" + name + "」？此操作不可恢复。") {
                io {
                    Api.adminDeleteKey(server, key)
                    main { UiKit.toast(act, "已删除"); onDone() }
                }
            }
        }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)).apply { marginStart = DemoKit.dp(act, 8) })
        DemoKit.put(card, acts, 12)
        return card
    }

    fun newKeyDialog(users: JsonArray, onDone: () -> Unit) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        val fName = field("卡名")
        val fQuota = field("额度 tokens（0 或留空 = 零额度；-1 = 不限）", "0", numeric = true)
        val fExp = field("到期日（YYYY-MM-DD，可空）")
        val fNote = field("备注（可空）")
        for (v in listOf<View>(fName, fQuota, fExp, fNote)) DemoKit.put(col, v, 10)
        UiKit.customDialog(act, theme, "生成卡密", col, "生成") {
            val name = fName.text.trim()
            val quotaTxt = fQuota.text.trim()
            val body = JsonObject().apply {
                addProperty("name", name)
                addProperty("quotaTokens", if (quotaTxt.isEmpty()) 0.0 else (quotaTxt.toDoubleOrNull() ?: 0.0))
                addProperty("expiresAt", fExp.text.trim())
                addProperty("note", fNote.text.trim())
            }
            io {
                val r = Api.adminCreateKey(server, body)
                val key = r.str("key")
                main {
                    if (key.isNotEmpty()) {
                        DemoKit.copy(act, "key", key)
                        UiKit.infoDialog(act, theme, "卡密已生成（已复制）", key)
                    } else UiKit.toast(act, "已生成")
                    onDone()
                }
            }
        }
    }

    









    fun editKeyDialog(k: JsonObject, onDone: () -> Unit) {
        val key = k.str("key")
        if (key.isEmpty()) { UiKit.toast(act, "卡密无效"); return }
        val uiPath = "/admin/ui/key-edit?key=" + android.net.Uri.encode(key)
        val container = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 12), DemoKit.dp(act, 16), DemoKit.dp(act, 20))
            setBackgroundColor(theme.color(act, "surfaceContainer"))
        }
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(act)
        val ctl = SduiController(
            act, act, server, "", theme,
            admin = true,
            adminApiScope = listOf("/admin/"),
            inlineNav = true,
            onClose = { sheet.dismiss() },
            onFallback = { reason -> if (sheet.isShowing) sheet.dismiss(); UiKit.toast(act, "卡密编辑页加载失败: $reason") },
        )
        ctl.loadInto(container, uiPath, showTitle = false)
        val sv = androidx.core.widget.NestedScrollView(act).apply { addView(container) }
        sheet.setContentView(sv)
        sheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        sheet.setOnShowListener {
            sheet.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            sheet.behavior.skipCollapsed = true
            UiKit.retintTree(container, theme)
            
            UiKit.tintDialogWindow(sheet, theme, bottomSheet = true)
        }
        
        sheet.setOnDismissListener { onDone() }
        sheet.show()
    }

    private fun rechargeDialog(k: JsonObject, onDone: () -> Unit) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 16), DemoKit.dp(act, 8), DemoKit.dp(act, 16), 0)
        }
        val f = field("充值 / 扣减（正数增加，负数扣减）", "", numeric = true)
        val (row, sw) = switchRowRef("同时重置已用量", "把 usedTokens 清零", false)
        DemoKit.put(col, f, 10)
        DemoKit.put(col, row, 14)
        UiKit.customDialog(act, theme, "充值 / 扣减", col, "提交") {
            val v = f.text.trim().toDoubleOrNull() ?: 0.0
            if (v == 0.0) {
                UiKit.toast(act, "请输入数量"); UiKit.keepOpen(); return@customDialog
            }
            io {
                Api.adminPost(server, "/plugins/auth-cardkey/admin/keys-update", JsonObject().apply {
                    addProperty("key", k.str("key"))
                    addProperty("addQuota", v)
                    if (sw.isChecked) addProperty("resetUsage", true)
                })
                main { UiKit.toast(act, "已提交"); onDone() }
            }
        }
    }

    



    fun buildNativePlugin(pluginId: String, host: LinearLayout): Boolean {
        when (pluginId) {
            "auth-user" -> buildAuthUserSettings(host)
            "tunnel" -> buildTunnel(host)
            "proxy" -> buildProxiesView(host)
            "auth-cardkey" -> {
                val card = DemoKit.panel(act, theme, 16)
                DemoKit.put(card, DemoKit.txt(act, theme, "卡密在「用户管理 → 卡密」标签页统一管理。", 13f, false, "onSurfaceVariant"))
                DemoKit.put(host, card, 2)
            }
            else -> return false
        }
        return true
    }

    
    private fun buildAuthUserSettings(host: LinearLayout) {
        buildPluginDetail("auth-user", "用户系统", host)
    }

    fun buildTunnel(host: LinearLayout) {
        val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(host, box, 2)
        var auto: Runnable? = null

        fun reload() {
            io {
                val r = try {
                    Api.adminGet(server, "/plugins/tunnel/status")
                } catch (e: Exception) {
                    main {
                        box.removeAllViews()
                        val card = DemoKit.panel(act, theme, 16)
                        DemoKit.put(card, DemoKit.txt(act, theme, "隧道插件不可用: " + (e.message ?: ""), 13f, false, "error"))
                        DemoKit.put(box, card, 2)
                    }
                    return@io
                }
                main {
                    box.removeAllViews()
                    val running = r.bool("running")
                    val url = r.str("url")
                    val card = DemoKit.panel(act, theme, 16)
                    val h = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                    h.addView(DemoKit.iconBadge(act, theme, R.drawable.ic_refresh, null, 40), LinearLayout.LayoutParams(DemoKit.dp(act, 40), DemoKit.dp(act, 40)))
                    val ht = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                    DemoKit.put(ht, DemoKit.txt(act, theme, "Cloudflare 公网隧道", 16f, true))
                    DemoKit.put(ht, DemoKit.txt(act, theme, "把本机网关临时暴露到公网，用于远程访问", 11.5f, false, "onSurfaceVariant"), 3)
                    h.addView(ht, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = DemoKit.dp(act, 10) })
                    h.addView(DemoKit.badge(act, theme, if (running) "运行中" else "已停止", if (running) "success" else "neutral"))
                    DemoKit.put(card, h)
                    if (url.isNotEmpty()) {
                        val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                        row.addView(DemoKit.txt(act, theme, url, 12.5f, false, "primary"), LinearLayout.LayoutParams(0, -2, 1f))
                        row.addView(DemoKit.iconButton(act, theme, R.drawable.ic_copy, "复制地址") { copyText("url", url) }, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)))
                        DemoKit.put(card, row, 12)
                    }
                    DemoKit.put(
                        card,
                        chipRow(
                            listOf(
                                DemoKit.chip(act, theme, "启动", R.drawable.ic_check, false) {
                                    io { Api.adminPost(server, "/plugins/tunnel/start", JsonObject()); main { reload() } }
                                },
                                DemoKit.chip(act, theme, "停止", R.drawable.ic_block, false) {
                                    io { Api.adminPost(server, "/plugins/tunnel/stop", JsonObject()); main { reload() } }
                                },
                                DemoKit.chip(act, theme, "刷新", R.drawable.ic_refresh, false) { reload() },
                            ),
                        ),
                        12,
                    )
                    DemoKit.put(box, card, 2)

                    val logCard = DemoKit.panel(act, theme, 16)
                    DemoKit.put(logCard, DemoKit.txt(act, theme, "cloudflared 日志", 15f, true))
                    val logArr = r.arr("log")
                    val lines = if (logArr == null || logArr.size() == 0) listOf("(空)") else
                        (0 until logArr.size()).map { logArr.get(it).asString }.takeLast(12)
                    DemoKit.put(logCard, DemoKit.txt(act, theme, lines.joinToString("\n"), 11f, false, "onSurfaceVariant"), 10)
                    DemoKit.put(box, logCard, 12)
                }
            }
        }
        reload()
        
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        auto = object : Runnable {
            override fun run() {
                if (host.parent == null) return
                reload()
                h.postDelayed(this, 10_000)
            }
        }
        h.postDelayed(auto!!, 10_000)
    }
}
