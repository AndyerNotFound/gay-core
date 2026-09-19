package com.gaycore.app.data










object Currency {

    
    var symbol: String = "¥"

    
    var rate: Double = 1.0

    

    fun update(symbol: String?, rate: Double?): Boolean {
        var changed = false
        if (!symbol.isNullOrBlank()) {
            val s = symbol.trim().take(8)
            if (s != this.symbol) { this.symbol = s; changed = true }
        }
        if (rate != null && rate > 0 && rate != this.rate) { this.rate = rate; changed = true }
        return changed
    }

    
    fun fmt(v: Long): String = fmt(v.toDouble())

    

    fun fmt(v: Double): String {
        val scaled = rate != 1.0 && rate > 0.0
        val units = if (scaled) v * rate else v
        val body = when {
            units >= 100_000_000 -> String.format("%.1f亿", units / 1e8)
            units >= 10_000 -> String.format("%.1f万", units / 1e4)
            units >= 1 -> String.format("%.2f", units).trimEnd('0').trimEnd('.')
            units > 0 -> String.format("%.6f", units).trimEnd('0').trimEnd('.')
            else -> "0"
        }
        return symbol + body
    }
}
