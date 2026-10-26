package com.bruce.liquidglass.data

/**
 * Home-screen items are strings: an app key ("pkg/.Activity") or a widget key ("w:<appWidgetId>:<rows>").
 * Widgets take the full width of the grid and `rows` rows; apps take one cell.
 */
object WidgetKey {
    const val MIN_ROWS = 1
    const val MAX_ROWS = 4

    fun make(id: Int, rows: Int) = "w:$id:${rows.coerceIn(MIN_ROWS, MAX_ROWS)}"
    fun isWidget(key: String) = key.startsWith("w:")
    fun id(key: String): Int = key.split(':').getOrNull(1)?.toIntOrNull() ?: -1
    fun rows(key: String): Int = key.split(':').getOrNull(2)?.toIntOrNull()?.coerceIn(MIN_ROWS, MAX_ROWS) ?: 2
    fun withRows(key: String, rows: Int) = make(id(key), rows)
}

data class Placement(val key: String, val row: Int, val col: Int, val spanRows: Int)

data class GridSpec(val columns: Int, val rows: Int, val clockOnFirstPage: Boolean) {
    /** Rows available for items on a page (the built-in clock uses 2 rows of page 1). */
    fun rowsFor(page: Int) = if (page == 0 && clockOnFirstPage) rows - 2 else rows
}

object HomeLayout {

    /** Places items row by row. Widgets always start on a fresh row. Returns placements and rows used. */
    fun place(items: List<String>, columns: Int): Pair<List<Placement>, Int> {
        val out = ArrayList<Placement>(items.size)
        var r = 0
        var c = 0
        for (k in items) {
            if (WidgetKey.isWidget(k)) {
                if (c > 0) { r++; c = 0 }
                val span = WidgetKey.rows(k)
                out += Placement(k, r, 0, span)
                r += span
            } else {
                out += Placement(k, r, c, 1)
                c++
                if (c == columns) { c = 0; r++ }
            }
        }
        return out to (r + if (c > 0) 1 else 0)
    }

    fun fits(items: List<String>, page: Int, grid: GridSpec): Boolean =
        place(items, grid.columns).second <= grid.rowsFor(page)

    /**
     * Makes every page fit: whatever overflows a page is pushed to the start of the next one.
     * Empty pages (other than the first) are removed.
     */
    fun normalize(pages: List<List<String>>, grid: GridSpec): List<List<String>> {
        val out = mutableListOf<List<String>>()
        var carry = emptyList<String>()
        var i = 0
        val src = pages.toMutableList()
        while (i < src.size || carry.isNotEmpty()) {
            val items = carry + (src.getOrNull(i) ?: emptyList())
            var n = items.size
            while (n > 0 && !fits(items.take(n), out.size, grid)) n--
            if (n == 0 && items.isNotEmpty() && out.size > 0) n = 1 // item bigger than a whole page: show it anyway
            out += items.take(n)
            carry = items.drop(n)
            i++
            if (out.size > 200) break // safety
        }
        return out.filterIndexed { idx, p -> idx == 0 || p.isNotEmpty() }.ifEmpty { listOf(emptyList()) }
    }

    /** Appends keys to the last page(s), opening new pages when needed. */
    fun append(pages: List<List<String>>, keys: List<String>, grid: GridSpec): List<List<String>> {
        val out = pages.map { it.toMutableList() }.toMutableList()
        if (out.isEmpty()) out += mutableListOf<String>()
        for (k in keys) {
            val last = out.lastIndex
            if (fits(out[last] + k, last, grid)) out[last] += k
            else out += mutableListOf(k)
        }
        return out
    }
}
