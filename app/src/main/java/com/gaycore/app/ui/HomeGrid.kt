package com.gaycore.app.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ClipData
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gaycore.app.R
import com.gaycore.app.data.DecoCard
import com.gaycore.app.data.HomeSlot
import com.gaycore.app.data.ImageLoader
import com.gaycore.app.theme.ThemeEngine










class HomeCardDef(
    val id: String,
    val title: String,
    val source: String,
    val defaultC: Int = HomeBoard.COLS,
    val defaultR: Int = 1,
    val available: () -> Boolean = { true },
    val render: (Boolean) -> View?,
)






















class HomeBoard(
    private val act: AppCompatActivity,
    private val theme: ThemeEngine,
    private val defs: LinkedHashMap<String, HomeCardDef>,
    private var items: MutableList<HomeSlot>,
    private val hidden: MutableSet<String>,
    private val decos: MutableMap<String, DecoCard>,
    private val defaultHidden: () -> Set<String>,
    private val pickImage: (() -> Unit)?,
    private val onPersist: (List<HomeSlot>, Set<String>, Map<String, DecoCard>) -> Unit,
    private val onToast: (String) -> Unit,
) {
    companion object {
        const val COLS = 5
        const val MAX_SPAN = 5

        
        const val ROW_DP = 104

        
        const val AUTO_R_TOL_DP = 8

        
        const val MAX_AUTO_R = 10

        const val GAP = 8
        const val REORDER_SLOP_DP = 16
        const val MOVE_ANIM_MS = 190L
        const val DECO = "deco:"
        const val FILL_TAG = "gc-fill"

        fun isDeco(id: String): Boolean = id.startsWith(DECO)

        fun clampSpan(v: Int): Int = v.coerceIn(1, MAX_SPAN)

        


        fun packSlots(
            ids: List<String>, cOf: (String) -> Int, rOf: (String) -> Int,
        ): LinkedHashMap<String, IntArray> {
            val occ = HashMap<Int, Int>()
            val out = LinkedHashMap<String, IntArray>()
            var row = 0
            var col = 0
            for (id in ids) {
                val c = cOf(id).coerceIn(1, COLS)
                val r = rOf(id).coerceIn(1, MAX_AUTO_R)
                while (true) {
                    if (col + c > COLS) {
                        row++
                        col = 0
                        continue
                    }
                    var ok = true
                    for (rr in row until row + r) {
                        val m = occ[rr] ?: 0
                        for (cc in col until col + c) {
                            if (m and (1 shl cc) != 0) {
                                ok = false
                                break
                            }
                        }
                        if (!ok) break
                    }
                    if (ok) break
                    col++
                }
                for (rr in row until row + r) {
                    var m = occ[rr] ?: 0
                    for (cc in col until col + c) m = m or (1 shl cc)
                    occ[rr] = m
                }
                out[id] = intArrayOf(row, col, r)
                col += c
            }
            return out
        }


        fun mergeItems(all: List<HomeCardDef>, saved: List<HomeSlot>?, decos: Map<String, DecoCard>): MutableList<HomeSlot> {
            val byId = HashMap<String, HomeCardDef>()
            for (d in all) byId[d.id] = d
            val out = ArrayList<HomeSlot>()
            if (saved != null) {
                for (s in saved) {
                    val alive = if (isDeco(s.id)) decos.containsKey(s.id) else byId.containsKey(s.id)
                    if (!alive) continue
                    if (out.any { it.id == s.id }) continue
                    out.add(HomeSlot(s.id, clampSpan(s.c), clampSpan(s.r), s.auto, s.col, s.row))
                }
            }
            for (d in all) {
                if (out.none { it.id == d.id }) out.add(HomeSlot(d.id, clampSpan(d.defaultC), clampSpan(d.defaultR)))
            }
            return out
        }
    }

    val host: LinearLayout = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }

    var editMode = false
        private set

    var onEditModeChanged: ((Boolean) -> Unit)? = null

    
    var onListEditor: (() -> Unit)? = null

    private val grid = GridBoard()

    
    private val wrapperCache = HashMap<String, FrameLayout>()
    private val contentCache = HashMap<String, View>()
    private val badgeViews = HashMap<String, View>()
    private val pillViews = HashMap<String, TextView>()
    private val cellSpan = HashMap<String, IntArray>()
    private val posOf = HashMap<String, IntArray>()
    
    private var mColW = 0
    private var mUnit = 0
    private var mGap = 0
    
    private val assignedPos = HashMap<String, IntArray>()
    private val wobbles = HashMap<String, ObjectAnimator>()
    private val fillers = ArrayList<TextView>()

    private var draggingId: String? = null
    private var hoverId: String? = null
    private var pendingHover: String? = null
    private var hoverPosted = false
    private var fingerX = 0f
    private var fingerY = 0f
    private var lastReorderX = Float.NaN
    private var lastReorderY = Float.NaN

    init {
        host.setOnDragListener { _, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_LOCATION -> {
                    hoverAt(e.x, e.y)
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    
                    host.post { endDrag() }
                    true
                }
                else -> true
            }
        }
    }

    

    fun render() {
        
        val visible = ArrayList<HomeSlot>(items.filter { isVisibleSlot(it) })
        cellSpan.clear()
        val want = LinkedHashSet<String>()
        for (s in visible) {
            cellSpan[s.id] = intArrayOf(clampSpan(s.c), clampSpan(s.r), if (s.auto) 1 else 0)
            want.add(s.id)
        }
        
        for (f in fillers) if (f.parent === grid) grid.removeView(f)
        for (i in grid.childCount - 1 downTo 0) {
            val v = grid.getChildAt(i)
            val id = idOf(v) ?: continue
            if (id !in want) grid.removeViewAt(i)
        }
        for ((idx, s) in visible.withIndex()) {
            val w = wrapperFor(s) ?: continue
            val cur = grid.getChildAt(idx)
            if (cur !== w) {
                if (w.parent === grid) grid.removeView(w)
                grid.addView(w, if (idx <= grid.childCount) idx else -1)
            }
        }
        
        if (editMode) {
            while (fillers.size < 15) {
                val f = fillerView()
                fillers.add(f)
            }
            for (f in fillers) if (f.parent !== grid) grid.addView(f)
        }
        

        val hasBar = host.childCount > 0 && host.getChildAt(0) !== grid
        if (grid.parent !== host || hasBar != editMode) {
            host.removeAllViews()
            if (editMode) host.addView(editBar())
            host.addView(
                grid,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    if (editMode) topMargin = DemoKit.dp(act, GAP)
                },
            )
        }
        applyEditDecor()
        grid.requestLayout()
    }

    
    private fun isVisibleSlot(s: HomeSlot): Boolean {
        if (s.id in hidden) return false
        if (isDeco(s.id)) return decos.containsKey(s.id)
        val d = defs[s.id] ?: return false
        return d.available()
    }

    private fun isFiller(v: View): Boolean = (v.tag as? String)?.startsWith(FILL_TAG) == true

    private fun idOf(v: View): String? = (v.tag as? String)?.takeIf { it.startsWith("gc-card:") }?.removePrefix("gc-card:")

    
    private fun wrapperFor(slot: HomeSlot): FrameLayout? {
        syncBadge(slot)
        wrapperCache[slot.id]?.let { return it }
        val deco = if (isDeco(slot.id)) decos[slot.id] else null
        if (deco == null && defs[slot.id] == null) return null
        val content = if (deco != null) decoView(deco) else defs[slot.id]?.render(editMode) ?: return null
        contentCache[slot.id] = content
        if (deco != null) {
            content.setOnClickListener { if (editMode) decoEditDialog(slot.id) }
        }
        val wrap = CardWrap()
        wrap.tag = "gc-card:" + slot.id
        




        val needScroll = !slot.auto && !(deco != null && deco.kind == "image")
        if (needScroll) {
            val sv = androidx.core.widget.NestedScrollView(act).apply {
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                isFillViewport = false
                isNestedScrollingEnabled = true
            }
            sv.addView(
                content,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
            sv.setOnLongClickListener {
                startCardDrag(sv, slot.id)
                if (!editMode) enterEdit()
                true
            }
            wrap.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            
            val fade = View(act)
            fade.background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(theme.color(act, "surfaceContainerLowest"), Color.TRANSPARENT),
            )
            fade.isClickable = false
            fade.visibility = View.GONE
            wrap.addView(
                fade,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, DemoKit.dp(act, 26), Gravity.BOTTOM,
                ),
            )
            wrap.fade = fade
        } else {
            wrap.addView(
                content,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }
        val badge = badges(slot)
        wrap.addView(
            badge,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END,
            ).apply {
                topMargin = DemoKit.dp(act, 3)
                marginEnd = DemoKit.dp(act, 2)
            },
        )
        badgeViews[slot.id] = badge
        content.setOnLongClickListener {
            startCardDrag(content, slot.id)
            if (!editMode) enterEdit()
            true
        }
        wrapperCache[slot.id] = wrap
        return wrap
    }

    
    private inner class CardWrap : FrameLayout(act) {
        var fade: View? = null

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            super.onLayout(changed, l, t, r, b)
            val first = if (childCount > 0) getChildAt(0) else null
            fade?.visibility = if (first != null && first.canScrollVertically(1)) View.VISIBLE else View.GONE
        }
    }

    
    private fun syncBadge(slot: HomeSlot) {
        val row = badgeViews[slot.id] as? LinearLayout ?: return
        val vertical = clampSpan(slot.c) <= 1
        row.orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val iv = row.getChildAt(1) as? ImageView ?: return
        val lp = iv.layoutParams as? LinearLayout.LayoutParams ?: return
        lp.marginStart = if (vertical) 0 else DemoKit.dp(act, 3)
        lp.topMargin = if (vertical) DemoKit.dp(act, 3) else 0
        iv.layoutParams = lp
    }

    
    private inner class GridBoard : ViewGroup(act) {
        private val rects = ArrayList<Rect>()

        
        val effRMap = HashMap<String, Int>()

        
        val pockets = ArrayList<IntArray>()

        private fun colWidth(w: Int): Int {
            val gap = DemoKit.dp(act, GAP)
            return ((w - gap * (COLS - 1)) / COLS).coerceAtLeast(1)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val gap = DemoKit.dp(act, GAP)
            val unit = DemoKit.dp(act, ROW_DP)
            val colW = colWidth(w)
            val hMode = MeasureSpec.getMode(heightMeasureSpec)
            val hSize = MeasureSpec.getSize(heightMeasureSpec)
            val childH = if (hMode == MeasureSpec.UNSPECIFIED) {
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            } else {
                MeasureSpec.makeMeasureSpec(hSize, MeasureSpec.AT_MOST)
            }
            val cellW = { c: Int -> colW * c + gap * (c - 1) }
            val cellH = { r: Int -> unit * r + gap * (r - 1) }
            
            mColW = colW; mUnit = unit; mGap = gap

            rects.clear()
            effRMap.clear()
            assignedPos.clear()

            
            val ids = ArrayList<String>()
            val cOf = HashMap<String, Int>()
            val rOf = HashMap<String, Int>()
            val naturalOf = HashMap<String, Int>()
            val slotOf = HashMap<String, HomeSlot>()
            for (i in 0 until childCount) {
                val ch = getChildAt(i)
                if (isFiller(ch)) {
                    rects.add(Rect(0, 0, 0, 0))
                    continue
                }
                val id = idOf(ch)
                if (id == null) {
                    rects.add(Rect(0, 0, 0, 0))
                    continue
                }
                val sl = items.firstOrNull { it.id == id }
                if (sl == null) {
                    rects.add(Rect(0, 0, 0, 0))
                    continue
                }
                slotOf[id] = sl
                val sp = cellSpan[id] ?: intArrayOf(COLS, 1)
                val c = sp[0].coerceIn(1, COLS)
                var r = sp[1].coerceIn(1, MAX_SPAN)
                val autoCell = sp.getOrElse(2) { 1 } == 1
                val autoR = autoCell && !(isDeco(id) && decos[id]?.kind == "image")
                var naturalH = 0
                if (autoR) {
                    ch.measure(MeasureSpec.makeMeasureSpec(cellW(c), MeasureSpec.EXACTLY), childH)
                    naturalH = ch.measuredHeight
                    val tol = DemoKit.dp(act, AUTO_R_TOL_DP)
                    while (r < MAX_AUTO_R && cellH(r) < naturalH - tol) r++
                }
                effRMap[id] = r
                naturalOf[id] = naturalH
                ids.add(id)
                cOf[id] = c
                rOf[id] = r
                rects.add(Rect(0, 0, 0, 0))
            }

            
            val occ = HashMap<Int, Int>()
            val placed = packSlots(ids, { cOf[it] ?: COLS }, { rOf[it] ?: 1 })
            for (id in ids) {
                val p = placed[id] ?: continue
                val c = cOf[id] ?: COLS
                val r = rOf[id] ?: 1
                val col = p[1]
                val row = p[0]
                assignedPos[id] = intArrayOf(col, row)
                for (rr in row until row + r) {
                    var m = occ[rr] ?: 0
                    for (cc in col until col + c) m = m or (1 shl cc)
                    occ[rr] = m
                }
            }

            
            var totalH = 0
            for (i in 0 until childCount) {
                val ch = getChildAt(i)
                if (isFiller(ch)) continue
                val id = idOf(ch) ?: continue
                val pos = assignedPos[id] ?: continue
                val c = cOf[id] ?: COLS
                val r = rOf[id] ?: 1
                val left = pos[0] * (colW + gap)
                val top = pos[1] * (unit + gap)
                val autoCell = (cellSpan[id]?.getOrElse(2) { 1 } ?: 1) == 1
                val autoR = autoCell && !(isDeco(id) && decos[id]?.kind == "image")
                val hh = if (autoR) maxOf(cellH(r), naturalOf[id] ?: 0) else cellH(r)
                rects[i] = Rect(left, top, left + cellW(c), top + hh)
                totalH = maxOf(totalH, top + hh)
            }

            
            pockets.clear()
            var lastRow = 0
            for (k in occ.keys) lastRow = maxOf(lastRow, k)
            val taken = HashMap<Int, Int>()
            var pr = 0
            
            while (pr <= lastRow + 1 && pockets.size < fillers.size) {
                var pc = 0
                while (pc < COLS && pockets.size < fillers.size) {
                    if (((occ[pr] ?: 0) or (taken[pr] ?: 0)) and (1 shl pc) != 0) {
                        pc++
                        continue
                    }
                    var e = pc
                    while (e < COLS && (((occ[pr] ?: 0) or (taken[pr] ?: 0)) and (1 shl e)) == 0) e++
                    val c = e - pc
                    var h = 1
                    while (h < MAX_SPAN && pr + h <= lastRow) {
                        var ok = true
                        for (x in pc until e) {
                            if (((occ[pr + h] ?: 0) or (taken[pr + h] ?: 0)) and (1 shl x) != 0) {
                                ok = false
                                break
                            }
                        }
                        if (!ok) break
                        h++
                    }
                    pockets.add(intArrayOf(pr, pc, c, h))
                    for (y in pr until pr + h) {
                        var tm = taken[y] ?: 0
                        for (x in pc until e) tm = tm or (1 shl x)
                        taken[y] = tm
                    }
                    pc = e
                }
                pr++
            }
            
            for (i in 0 until childCount) {
                val ch = getChildAt(i)
                if (!isFiller(ch)) continue
                val f = ch as TextView
                f.tag = "$FILL_TAG:0:0:0:0"
                f.text = ""
                rects[i] = Rect(0, 0, 0, 0)
            }

            
            for (i in 0 until childCount) {
                val ch = getChildAt(i)
                if (isFiller(ch)) continue
                val rc = rects.getOrNull(i) ?: Rect(0, 0, 0, 0)
                ch.measure(
                    MeasureSpec.makeMeasureSpec(rc.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(rc.height(), MeasureSpec.EXACTLY),
                )
            }
            setMeasuredDimension(w, totalH)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            
            for (i in 0 until childCount) {
                val ch = getChildAt(i)
                if (isFiller(ch)) continue
                val rc = rects.getOrNull(i) ?: continue
                val id = idOf(ch)
                val old = if (id != null) posOf[id] else null
                ch.layout(rc.left, rc.top, rc.right, rc.bottom)
                if (id != null && old != null && (old[0] != rc.left || old[1] != rc.top)) {
                    ch.translationX = (old[0] - rc.left).toFloat()
                    ch.translationY = (old[1] - rc.top).toFloat()
                    ch.animate().translationX(0f).translationY(0f)
                        .setDuration(MOVE_ANIM_MS).setInterpolator(DecelerateInterpolator()).start()
                }
                if (id != null) {
                    if (old == null) posOf[id] = intArrayOf(rc.left, rc.top) else {
                        old[0] = rc.left
                        old[1] = rc.top
                    }
                }
            }
            
            val gap = mGap
            val cw = { c: Int -> mColW * c + gap * (c - 1) }
            val ch = { r: Int -> mUnit * r + gap * (r - 1) }
            var fi = 0
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (!isFiller(child)) continue
                val f = child as TextView
                if (fi < pockets.size) {
                    val p = pockets[fi]
                    f.tag = "$FILL_TAG:${p[0]}:${p[1]}:${p[2]}:${p[3]}"
                    f.text = "\uff0b\n\u5269 ${p[2]} \u683c"
                    val left = p[1] * (mColW + gap)
                    val top = p[0] * (mUnit + gap)
                    f.measure(
                        MeasureSpec.makeMeasureSpec(cw(p[2]), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(ch(p[3]), MeasureSpec.EXACTLY),
                    )
                    f.layout(left, top, left + cw(p[2]), top + ch(p[3]))
                } else {
                    f.tag = "$FILL_TAG:0:0:0:0"
                    f.text = ""
                    f.layout(0, 0, 0, 0)
                }
                fi++
            }
            
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val id = idOf(child) ?: continue
                val pill = pillViews[id] ?: continue
                val sp = cellSpan[id] ?: continue
                val txt = sp[0].toString() + "\u00d7" + (effRMap[id] ?: sp[1])
                if (pill.text != txt) pill.post { pill.text = txt }
            }
        }
    }

    
    private fun decoView(card: DecoCard): View {
        val box = FrameLayout(act)
        box.background = GradientDrawable().apply {
            cornerRadius = DemoKit.dp(act, 16).toFloat()
            setColor(theme.color(act, "surfaceContainerLowest"))
            setStroke(DemoKit.dp(act, 1), theme.color(act, "outlineVariant"))
        }
        box.clipToOutline = true
        if (card.kind == "image") {
            val iv = ImageView(act)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            box.addView(iv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            val t = card.text
            if (t.startsWith("file:")) {
                ImageLoader.loadFile(t.removePrefix("file:"), iv, ImageLoader.Shape.ROUNDED, 0) {}
            } else {
                ImageLoader.load(t, iv, ImageLoader.Shape.ROUNDED, 0) {
                    box.addView(
                        DemoKit.txt(act, theme, "图片加载失败", 11.5f, false, "error").apply { gravity = Gravity.CENTER },
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                    )
                }
            }
        } else {
            val tv = DemoKit.txt(act, theme, card.text, if (card.text.length <= 2) 34f else 17f, true)
            tv.gravity = Gravity.CENTER
            box.addView(tv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        return box
    }

    
    private fun fillerView(): TextView {
        val tv = TextView(act)
        tv.gravity = Gravity.CENTER
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
        tv.setTextColor(theme.color(act, "primary"))
        tv.background = DemoKit.roundedOutline(act, Color.TRANSPARENT, theme.color(act, "primary"), 16, 1)
        tv.tag = "$FILL_TAG:0:0:0:0"
        tv.isClickable = true
        tv.isFocusable = true
        tv.setOnClickListener {
            val t = (it.tag as? String) ?: return@setOnClickListener
            val ps = t.split(":")
            
            val c = ps.getOrNull(ps.size - 2)?.toIntOrNull() ?: 0
            val r = ps.getOrNull(ps.size - 1)?.toIntOrNull() ?: 0
            if (c <= 0) return@setOnClickListener
            addDecoDialog(c, r)
        }
        return tv
    }

    
    private fun badges(slot: HomeSlot): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val pill = TextView(act)
        pill.text = clampSpan(slot.c).toString() + "×" + clampSpan(slot.r)
        pill.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
        pill.setTypeface(pill.typeface, android.graphics.Typeface.BOLD)
        pill.setTextColor(theme.color(act, "primary"))
        pill.background = DemoKit.rounded(act, theme.color(act, "primaryContainer"), 100)
        pill.setPadding(DemoKit.dp(act, 6), DemoKit.dp(act, 2), DemoKit.dp(act, 6), DemoKit.dp(act, 2))
        pill.isClickable = true
        pill.isFocusable = true
        pill.setOnClickListener { sizeDialog(slot.id) }
        pillViews[slot.id] = pill
        row.addView(pill)
        val iv = ImageView(act)
        iv.setImageResource(R.drawable.ic_close)
        iv.setColorFilter(theme.color(act, "onSurfaceVariant"))
        iv.setPadding(DemoKit.dp(act, 6), DemoKit.dp(act, 6), DemoKit.dp(act, 6), DemoKit.dp(act, 6))
        iv.background = DemoKit.rounded(act, theme.color(act, "surfaceContainerHighest"), 100)
        iv.isClickable = true
        iv.isFocusable = true
        iv.setOnClickListener {
            if (isDeco(slot.id)) removeDeco(slot.id) else hideCard(slot.id)
        }
        row.addView(iv, LinearLayout.LayoutParams(DemoKit.dp(act, 22), DemoKit.dp(act, 22)))
        

        row.elevation = DemoKit.dp(act, 4).toFloat()
        row.visibility = if (editMode) View.VISIBLE else View.GONE
        return row
    }

    
    private fun applyEditDecor() {
        for ((id, v) in badgeViews) v.visibility = if (editMode) View.VISIBLE else View.GONE
        if (!editMode) {
            for ((_, a) in wobbles) a.cancel()
            wobbles.clear()
            for ((_, w) in wrapperCache) {
                w.rotation = 0f
                w.elevation = 0f
            }
            return
        }
        var k = 0
        for ((id, w) in wrapperCache) {
            w.elevation = DemoKit.dp(act, 6).toFloat()
            if (wobbles.containsKey(id)) continue
            val a = ObjectAnimator.ofFloat(w, "rotation", -1.2f, 1.2f).apply {
                duration = 280L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                startDelay = (k % 4) * 60L
                start()
            }
            wobbles[id] = a
            k++
        }
    }

    
    private fun editBar(): View {
        val card = DemoKit.panel(act, theme, 14)
        val head = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(
            DemoKit.iconBadge(act, theme, R.drawable.ic_grid, null, 28),
            LinearLayout.LayoutParams(DemoKit.dp(act, 28), DemoKit.dp(act, 28)),
        )
        head.addView(
            DemoKit.txt(act, theme, "编辑首页布局", 14f, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = DemoKit.dp(act, 8) },
        )
        head.addView(DemoKit.chip(act, theme, "完成", R.drawable.ic_check, true) { exitEdit() })
        DemoKit.put(card, head)
        DemoKit.put(
            card,
            DemoKit.txt(
                act, theme,
                "长按卡片拖动摆放 · 点卡上「宽×高」改尺寸 · ✕ 隐藏/删除 · 隐藏的点下面「已隐藏 N 张」可逐个恢复",
                11.5f, false, "onSurfaceVariant",
            ),
            8,
        )
        val a1 = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        a1.addView(DemoKit.chip(act, theme, "＋ 美化卡", R.drawable.ic_add) { addDecoDialog(2, 1) })
        a1.addView(
            DemoKit.chip(act, theme, "列表方式", R.drawable.ic_sort) { onListEditor?.invoke() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = DemoKit.dp(act, 6)
            },
        )
        DemoKit.put(card, a1, 10)
        val a2 = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        a2.addView(DemoKit.chip(act, theme, "恢复默认", R.drawable.ic_refresh) { resetDefault() })
        a2.addView(
            DemoKit.chip(act, theme, "已隐藏 " + hidden.size + " 张", R.drawable.ic_block) { showHiddenDialog() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = DemoKit.dp(act, 6)
            },
        )
        DemoKit.put(card, a2, 8)
        return card
    }

    

    private fun sizeDialog(id: String) {
        val cur = items.firstOrNull { it.id == id } ?: return
        var pickC = clampSpan(cur.c)
        var pickR = clampSpan(cur.r)
        var pickAuto = cur.auto
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 4), DemoKit.dp(act, 8), DemoKit.dp(act, 4), 0)
        }
        val tip = DemoKit.txt(act, theme, "", 12f, false, "onSurfaceVariant")
        var modeChip: TextView? = null
        fun refresh() {
            tip.text = "已选：" + pickC + " 格宽 × " + pickR + " 行高（1 行 = " + ROW_DP + "dp）"
            modeChip?.text = if (pickAuto) "高度：自动撑高（内容高就多占行）" else "高度：固定 " + pickR + " 行（超出卡内滚动，底部渐隐）"
        }
        modeChip = DemoKit.chip(act, theme, "") {
            pickAuto = !pickAuto
            refresh()
        }
        DemoKit.put(col, tip)
        DemoKit.put(col, modeChip, 10)
        val gridV = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, gridV, 12)
        var rebuild: () -> Unit = {}
        rebuild = {
            gridV.removeAllViews()
            for (r in MAX_SPAN downTo 1) {
                val line = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                for (c in 1..MAX_SPAN) {
                    val sel = (c == pickC && r == pickR)
                    val cell = TextView(act)
                    cell.text = c.toString() + "×" + r
                    cell.gravity = Gravity.CENTER
                    cell.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    cell.setTextColor(theme.color(act, if (sel) "onPrimaryContainer" else "onSurfaceVariant"))
                    cell.background = if (sel) {
                        DemoKit.rounded(act, theme.color(act, "primaryContainer"), 10)
                    } else {
                        DemoKit.roundedOutline(act, Color.TRANSPARENT, theme.color(act, "outlineVariant"), 10, 1)
                    }
                    cell.isClickable = true
                    cell.isFocusable = true
                    cell.setOnClickListener {
                        pickC = c
                        pickR = r
                        refresh()
                        rebuild()
                    }
                    line.addView(
                        cell,
                        LinearLayout.LayoutParams(0, DemoKit.dp(act, 38), 1f).apply { if (c > 1) marginStart = DemoKit.dp(act, 4) },
                    )
                }
                DemoKit.put(gridV, line, 6)
            }
        }
        rebuild()
        refresh()
        UiKit.customDialog(act, theme, "卡片尺寸", col, okText = "确定") {
            val i = items.indexOfFirst { it.id == id }
            if (i >= 0) {
                val old = items[i]
                items[i] = items[i].copy(c = pickC, r = pickR, auto = pickAuto)
                cellSpan[id] = intArrayOf(pickC, pickR, if (pickAuto) 1 else 0)
                
                if (old.auto != pickAuto) {
                    val w = wrapperCache.remove(id)
                    if (w != null && w.parent === grid) grid.removeView(w)
                }
                persist()
                render()
            }
        }
    }

    private fun addDecoDialog(prefillC: Int, prefillR: Int) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 4), DemoKit.dp(act, 8), DemoKit.dp(act, 4), 0)
        }
        DemoKit.put(
            col,
            DemoKit.txt(act, theme, "输入 Emoji/文字，或图片 URL；也可以从本地选图。空位点「＋」进来时尺寸已按空位填好。", 12f, false, "onSurfaceVariant"),
        )
        val f = MdField(act, theme, "Emoji / 文字 或 图片 URL", "")
        DemoKit.put(col, f, 12)
        val acts = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        acts.addView(DemoKit.chip(act, theme, "文字/Emoji", R.drawable.ic_star) {
            val t = f.text.trim()
            if (t.isEmpty()) onToast("先输入 Emoji 或文字") else addDeco("emoji", t, prefillC, prefillR)
        })
        acts.addView(
            DemoKit.chip(act, theme, "图片 URL", R.drawable.ic_grid) {
                val t = f.text.trim()
                if (!(t.startsWith("http://") || t.startsWith("https://"))) {
                    onToast("图片 URL 需要 http:// 或 https:// 开头")
                } else {
                    addDeco("image", t, prefillC, prefillR)
                }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = DemoKit.dp(act, 6)
            },
        )
        if (pickImage != null) {
            acts.addView(
                DemoKit.chip(act, theme, "本地图片", R.drawable.ic_add) { pickImage.invoke() },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = DemoKit.dp(act, 6)
                },
            )
        }
        DemoKit.put(col, acts, 12)
        DemoKit.put(
            col,
            DemoKit.txt(act, theme, "添加后插到末尾（" + prefillC + " 格宽 × " + prefillR + " 行），再长按拖到想放的位置。", 11f, false, "onSurfaceVariant"),
            10,
        )
        UiKit.customDialog(act, theme, "添加美化卡", col, okText = "关闭") { }
    }

    private fun decoEditDialog(id: String) {
        val card = decos[id] ?: return
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 4), DemoKit.dp(act, 8), DemoKit.dp(act, 4), 0)
        }
        DemoKit.put(col, DemoKit.txt(act, theme, "当前：" + (if (card.kind == "image") "图片" else "文字/Emoji"), 12f, false, "onSurfaceVariant"))
        val f = MdField(act, theme, "Emoji / 文字 或 图片 URL", card.text)
        DemoKit.put(col, f, 12)
        val acts = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        acts.addView(DemoKit.chip(act, theme, "存为文字", R.drawable.ic_star) {
            val t = f.text.trim()
            if (t.isEmpty()) onToast("不能为空") else updateDeco(id, DecoCard(id, "emoji", t))
        })
        acts.addView(
            DemoKit.chip(act, theme, "存为图片", R.drawable.ic_grid) {
                val t = f.text.trim()
                if (!(t.startsWith("http://") || t.startsWith("https://") || t.startsWith("file:"))) {
                    onToast("图片需要 http(s):// 或本地图")
                } else {
                    updateDeco(id, DecoCard(id, "image", t))
                }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = DemoKit.dp(act, 6)
            },
        )
        DemoKit.put(col, acts, 12)
        UiKit.customDialog(act, theme, "美化卡内容", col, okText = "关闭") { }
    }

    

    private fun addDeco(kind: String, text: String, c: Int, r: Int) {
        val id = DECO + System.currentTimeMillis().toString().takeLast(6)
        decos[id] = DecoCard(id, kind, text)
        items.add(HomeSlot(id, clampSpan(c), clampSpan(r)))
        persist()
        render()
        onToast("已添加美化卡（" + clampSpan(c) + "×" + clampSpan(r) + "，可长按拖动）")
    }

    private fun updateDeco(id: String, card: DecoCard) {
        decos[id] = card
        contentCache.remove(id)
        val w = wrapperCache.remove(id)
        if (w != null && w.parent === grid) grid.removeView(w)
        persist()
        render()
        onToast("已更新")
    }

    private fun removeDeco(id: String) {
        items.removeAll { it.id == id }
        decos.remove(id)
        contentCache.remove(id)
        val w = wrapperCache.remove(id)
        if (w != null && w.parent === grid) grid.removeView(w)
        persist()
        render()
        onToast("已删除美化卡")
    }

    fun onDecoImagePicked(uri: android.net.Uri?) {
        if (uri == null) return
        try {
            val id = DECO + System.currentTimeMillis().toString().takeLast(6)
            val dir = java.io.File(act.filesDir, "home-deco")
            if (!dir.exists()) dir.mkdirs()
            val f = java.io.File(dir, id.replace(":", "_") + ".img")
            act.contentResolver.openInputStream(uri)?.use { input ->
                f.outputStream().use { out -> input.copyTo(out) }
            }
            decos[id] = DecoCard(id, "image", "file:" + f.absolutePath)
            items.add(HomeSlot(id, 2, 2))
            persist()
            render()
            onToast("已添加本地图片（2×2，可调）")
        } catch (e: Exception) {
            onToast("图片读取失败：" + (e.message ?: "未知错误"))
        }
    }

    

    fun enterEdit() {
        if (editMode) return
        editMode = true
        render()
        onEditModeChanged?.invoke(true)
    }

    fun exitEdit() {
        if (!editMode) return
        editMode = false
        render()
        persist()
        onEditModeChanged?.invoke(false)
    }

    fun toggleEdit() {
        if (editMode) exitEdit() else enterEdit()
    }

    fun resetDefault() {
        val decoItems = items.filter { isDeco(it.id) && decos.containsKey(it.id) }
        items = mergeItems(defs.values.toList(), null, decos)
        items.addAll(decoItems)
        hidden.clear()
        hidden.addAll(defaultHidden())
        persist()
        render()
        onToast("已恢复默认布局（美化卡保留在末尾）")
    }

    fun restoreAll() {
        if (hidden.isEmpty()) {
            onToast("没有隐藏的卡片")
            return
        }
        hidden.clear()
        persist()
        render()
        onToast("已恢复隐藏的卡片")
    }

    fun hiddenCount(): Int = hidden.size

    fun titleOf(id: String): String =
        if (isDeco(id)) ("美化卡 " + (decos[id]?.text ?: "")) else (defs[id]?.title ?: id)

    fun defsList(): List<HomeCardDef> = defs.values.toList()

    fun slots(): List<HomeSlot> = items.toList()

    fun hiddenIds(): Set<String> = hidden.toSet()

    fun applyLayout(newItems: List<HomeSlot>, newHidden: Collection<String>) {
        items = newItems.toMutableList()
        hidden.clear()
        hidden.addAll(newHidden)
        persist()
        render()
    }

    fun listEditorView(onSaved: () -> Unit): View {
        val ord = items.toMutableList()
        val hid = hidden.toMutableSet()
        val card = DemoKit.panel(act, theme, 16)
        DemoKit.put(card, DemoKit.txt(act, theme, "列表方式：顺序 / 尺寸 / 显隐", 16f, true))
        DemoKit.put(
            card,
            DemoKit.txt(
                act, theme,
                "▲▼ 调序；点尺寸选 `宽×高`（行数是**最小值**，内容更高会自动多占几行）；右侧开关控制显示。",
                12f, false, "onSurfaceVariant",
            ),
            6,
        )
        val rows = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(card, rows, 12)
        var rebuild: () -> Unit = {}
        rebuild = {
            rows.removeAllViews()
            ord.forEachIndexed { idx, slot ->
                val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                val info = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                DemoKit.put(info, DemoKit.txt(act, theme, titleOf(slot.id), 13.5f, true))
                val src = if (isDeco(slot.id)) "美化卡" else (defs[slot.id]?.source ?: "")
                DemoKit.put(
                    info,
                    DemoKit.txt(
                        act, theme,
                        (if (slot.id in hid) "已隐藏 · " else "") + src,
                        11f, false, if (slot.id in hid) "error" else "onSurfaceVariant",
                    ),
                    2,
                )
                row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(
                    DemoKit.chip(act, theme, clampSpan(slot.c).toString() + "×" + clampSpan(slot.r)) {
                        listSizeDialog(slot.id, ord) { rebuild() }
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = DemoKit.dp(act, 4)
                    },
                )
                val up = DemoKit.iconButton(act, theme, R.drawable.ic_chevron_up, "上移") {
                    if (idx > 0) {
                        val t = ord.removeAt(idx)
                        ord.add(idx - 1, t)
                        rebuild()
                    }
                }
                up.alpha = if (idx == 0) 0.3f else 1f
                up.isEnabled = idx > 0
                row.addView(up, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)).apply {
                    marginStart = DemoKit.dp(act, 4)
                })
                val down = DemoKit.iconButton(act, theme, R.drawable.ic_chevron_down, "下移") {
                    if (idx < ord.size - 1) {
                        val t = ord.removeAt(idx)
                        ord.add(idx + 1, t)
                        rebuild()
                    }
                }
                down.alpha = if (idx == ord.size - 1) 0.3f else 1f
                down.isEnabled = idx < ord.size - 1
                row.addView(down, LinearLayout.LayoutParams(DemoKit.dp(act, 32), DemoKit.dp(act, 32)).apply {
                    marginStart = DemoKit.dp(act, 4)
                })
                val sw = DemoKit.themeSwitch(act, theme, slot.id !in hid).apply {
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) hid.remove(slot.id) else hid.add(slot.id)
                    }
                }
                row.addView(
                    sw,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = DemoKit.dp(act, 6)
                    },
                )
                DemoKit.put(rows, row, 10)
            }
        }
        rebuild()
        DemoKit.put(
            card,
            DemoKit.button(act, theme, "保存", "filled") {
                applyLayout(ord.toList(), hid.toList())
                onSaved()
            },
            14,
        )
        return card
    }

    private fun listSizeDialog(id: String, ord: MutableList<HomeSlot>, after: () -> Unit) {
        val idx = ord.indexOfFirst { it.id == id }
        if (idx < 0) return
        var pickC = clampSpan(ord[idx].c)
        var pickR = clampSpan(ord[idx].r)
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 4), DemoKit.dp(act, 8), DemoKit.dp(act, 4), 0)
        }
        val tip = DemoKit.txt(act, theme, "", 12f, false, "onSurfaceVariant")
        DemoKit.put(col, tip)
        val gridV = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, gridV, 12)
        var rebuild: () -> Unit = {}
        fun refresh() {
            tip.text = "已选：" + pickC + " 格宽 × " + pickR + " 行高"
        }
        rebuild = {
            gridV.removeAllViews()
            for (r in MAX_SPAN downTo 1) {
                val line = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                for (c in 1..MAX_SPAN) {
                    val sel = (c == pickC && r == pickR)
                    val cell = TextView(act)
                    cell.text = c.toString() + "×" + r
                    cell.gravity = Gravity.CENTER
                    cell.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    cell.setTextColor(theme.color(act, if (sel) "onPrimaryContainer" else "onSurfaceVariant"))
                    cell.background = if (sel) {
                        DemoKit.rounded(act, theme.color(act, "primaryContainer"), 10)
                    } else {
                        DemoKit.roundedOutline(act, Color.TRANSPARENT, theme.color(act, "outlineVariant"), 10, 1)
                    }
                    cell.isClickable = true
                    cell.isFocusable = true
                    cell.setOnClickListener {
                        pickC = c
                        pickR = r
                        refresh()
                        rebuild()
                    }
                    line.addView(
                        cell,
                        LinearLayout.LayoutParams(0, DemoKit.dp(act, 38), 1f).apply { if (c > 1) marginStart = DemoKit.dp(act, 4) },
                    )
                }
                DemoKit.put(gridV, line, 6)
            }
        }
        rebuild()
        refresh()
        UiKit.customDialog(act, theme, "卡片尺寸（" + titleOf(id) + "）", col, okText = "确定") {
            val i = ord.indexOfFirst { it.id == id }
            if (i >= 0) {
                ord[i] = ord[i].copy(c = pickC, r = pickR)
                after()
            }
        }
    }

    
    private fun showHiddenDialog() {
        if (hidden.isEmpty()) {
            onToast("没有隐藏的卡片")
            return
        }
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 4), DemoKit.dp(act, 8), DemoKit.dp(act, 4), 0)
        }
        DemoKit.put(col, DemoKit.txt(act, theme, "点「恢复」把它放回首页，再长按拖到想放的位置。", 12f, false, "onSurfaceVariant"))
        val list = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, list, 10)
        var rebuild: () -> Unit = {}
        rebuild = {
            list.removeAllViews()
            val ids = items.map { it.id }.filter { it in hidden }
            if (ids.isEmpty()) {
                DemoKit.put(list, DemoKit.txt(act, theme, "都恢复了 ✓（关掉本窗口即可）", 12.5f, false, "onSurfaceVariant"))
            } else {
                for (id in ids) {
                    val row = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
                    val info = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                    DemoKit.put(info, DemoKit.txt(act, theme, titleOf(id), 13f, true))
                    val src = if (isDeco(id)) "美化卡" else (defs[id]?.source ?: "")
                    DemoKit.put(info, DemoKit.txt(act, theme, src, 11f, false, "onSurfaceVariant"), 2)
                    row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    row.addView(DemoKit.chip(act, theme, "恢复", R.drawable.ic_add) {
                        hidden.remove(id)
                        persist()
                        render()
                        onToast("已恢复「" + titleOf(id) + "」——长按可拖到想放的位置")
                        rebuild()
                    })
                    DemoKit.put(list, row, 10)
                }
            }
        }
        rebuild()
        UiKit.customDialog(act, theme, "隐藏的卡片（" + hidden.size + " 张）", col, okText = "全部恢复") {
            restoreAll()
        }
    }

    private fun hideCard(id: String) {
        hidden.add(id)
        persist()
        render()
        onToast("已隐藏「" + titleOf(id) + "」")
    }

    private fun persist() {
        onPersist(items.toList(), hidden.toSet(), HashMap(decos))
    }

    

    private fun startCardDrag(v: View, id: String) {
        draggingId = id
        hoverId = id
        lastReorderX = Float.NaN
        lastReorderY = Float.NaN
        try {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {
        }
        val data = ClipData.newPlainText("gc-home-card", id)
        v.startDragAndDrop(data, View.DragShadowBuilder(v), id, 0)
    }

    private fun endDrag() {
        if (draggingId == null) return
        draggingId = null
        hoverId = null
        persist()
        render()
    }

    


    private fun hoverAt(x: Float, y: Float) {
        val dragId = draggingId ?: return
        fingerX = x
        fingerY = y
        val gy = y - grid.top
        var hit: String? = null
        for (i in 0 until grid.childCount) {
            val c = grid.getChildAt(i)
            if (gy < c.top.toFloat() || gy > c.bottom.toFloat()) continue
            if (x < c.left.toFloat() || x > c.right.toFloat()) continue
            if (isFiller(c)) {
                val ps = (c.tag as? String)?.split(":")
                if (ps != null && ps.size >= 5) hit = "fill:" + ps[1] + ":" + ps[2]
            } else {
                hit = idOf(c)
            }
            break
        }
        val token = hit ?: return
        if (token == hoverId) return
        if (token == dragId) return
        hoverId = token
        pendingHover = token
        if (hoverPosted) return
        hoverPosted = true
        host.post {
            hoverPosted = false
            val target = pendingHover
            pendingHover = null
            if (target != null) applyHover(target)
        }
    }

    private fun applyHover(target: String) {
        val dragId = draggingId ?: return
        if (!lastReorderX.isNaN()) {
            val slop = DemoKit.dp(act, REORDER_SLOP_DP).toFloat()
            if (Math.abs(fingerX - lastReorderX) < slop && Math.abs(fingerY - lastReorderY) < slop) return
        }
        
        val visIds = items.filter { isVisibleSlot(it) }.map { it.id }.toMutableList()
        val from = visIds.indexOf(dragId)
        if (from < 0) return
        if (target.startsWith("fill:")) {
            visIds.removeAt(from)
            visIds.add(dragId)
        } else {
            val hi = visIds.indexOf(target)
            if (hi < 0 || hi == from) return
            visIds[from] = target
            visIds[hi] = dragId
        }
        var vi = 0
        val out = ArrayList<HomeSlot>(items.size)
        for (s in items) {
            if (isVisibleSlot(s)) {
                val id = visIds[vi++]
                out.add(items.firstOrNull { it.id == id } ?: s)
            } else {
                out.add(s)
            }
        }
        items = out
        lastReorderX = fingerX
        lastReorderY = fingerY
        render()
    }

}
