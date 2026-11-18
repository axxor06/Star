package com.star.menugenerator.domain

// ---------------------------------------------------------------------------------------------
// Print blocks + pagination. Port of buildBlocks() / paginate() from menuModel.js.
// ALL show/hide, capitalisation and price formatting happen in buildBlocks, so preview, PNG and
// PDF always agree. `keep` = "keep with the next block" (a heading never ends a page).
// ---------------------------------------------------------------------------------------------

enum class BlockType { HEAD, ITEM, NAME, SEC, LIST, GRAND }

data class Block(
    val k: String,
    val type: BlockType,
    val keep: Boolean = false,
    val mealId: String? = null,
    val head: String = "",
    val headContd: String = "",
    val text: String = "",
    val price: String = "",
    val name: String = "",
    val qty: String = "",
    val desc: String = "",
    val note: String = "",
)

private const val CHUNK = 12

fun buildBlocks(menu: Menu): List<Block> {
    val st = menu.settings
    val out = mutableListOf<Block>()
    val sum = summarize(menu)
    for (meal in menu.meals) {
        val entries = meal.namedEntries()
        if (entries.isEmpty()) continue
        val mealName = meal.name.trim()
        val head = if (mealName.isNotEmpty()) (if (st.caps) mealName.uppercase() else mealName) else ""
        val headContd = if (head.isNotEmpty()) head + (if (st.caps) " (CONTD.)" else " (contd.)") else ""
        if (head.isNotEmpty()) {
            out += Block(k = "h" + meal.id, type = BlockType.HEAD, keep = true, mealId = meal.id, text = head)
        }
        for (e in entries) {
            val name = e.name.trim()
            if (e.type == EntryType.INDIVIDUAL) {
                val qty = if (st.showQty && e.quantity.trim().isNotEmpty()) {
                    e.quantity.trim() + (if (e.unit.trim().isNotEmpty()) " " + e.unit.trim() else "")
                } else ""
                val p = if (st.showPrice) formatInput(e.unitPrice) else ""
                out += Block(
                    k = e.id, type = BlockType.ITEM, mealId = meal.id, head = head, headContd = headContd,
                    name = name, qty = qty, price = if (p.isNotEmpty()) "$p each" else "",
                    desc = if (st.showDesc) e.description.trim() else "",
                    note = if (st.showNotes) e.note.trim() else "",
                )
                continue
            }
            val pp = formatInput(e.pricePerPlate)
            val showIt = if (e.type == EntryType.PACKAGE) st.showPackagePrice else st.showBuffetPrice
            val priceText = if (pp.isNotEmpty() && showIt) "$pp / Plate" else ""
            // groups: package = one unlabeled group; buffet = its non-empty sections
            val groups: List<Pair<String, List<String>>> = if (e.type == EntryType.PACKAGE) {
                listOf("" to parseItems(e.itemsText))
            } else {
                e.sections.map { it.name.trim() to parseItems(it.itemsText) }.filter { it.second.isNotEmpty() }
            }
            val lists = mutableListOf<Block>()
            groups.forEachIndexed { gi, g ->
                if (g.first.isNotEmpty() && e.type == EntryType.BUFFET && st.showBuffetSections) {
                    lists += Block(
                        k = "${e.id}:s$gi", type = BlockType.SEC, keep = true, mealId = meal.id,
                        head = head, headContd = headContd, text = g.first,
                    )
                }
                val cs = g.second.chunked(CHUNK)
                cs.forEachIndexed { ci, c ->
                    lists += Block(
                        k = "${e.id}:s${gi}c$ci", type = BlockType.LIST, mealId = meal.id,
                        head = head, headContd = headContd,
                        text = c.joinToString(", ") + (if (ci < cs.size - 1) "," else ""),
                    )
                }
            }
            if (lists.isEmpty()) {
                out += Block(
                    k = e.id, type = BlockType.NAME, mealId = meal.id, head = head, headContd = headContd,
                    text = name, price = priceText,
                )
                continue
            }
            out += Block(
                k = e.id, type = BlockType.NAME, keep = true, mealId = meal.id, head = head,
                headContd = headContd, text = name, price = "",
            )
            lists.forEach { out += it }
            // the price lives inside the last block: it is never orphaned on a page of its own
            if (priceText.isNotEmpty()) out[out.lastIndex] = out.last().copy(price = priceText)
        }
    }
    if (st.showGrandTotal && sum.grand > 0) {
        out += Block(
            k = "grand", type = BlockType.GRAND, mealId = null,
            text = "Grand Total: " + formatPaise(sum.grand),
            note = if (sum.complete) "" else "Partial total — some entries are missing a price or quantity",
        )
    }
    return out
}

/**
 * Greedy pagination from real measured heights. Headings stay with what follows; when a meal
 * continues on a new page it gets a "(contd.)" heading. Text is never shrunk — long menus simply
 * continue on more pages. [heights] maps Block.k to its measured height in template pixels.
 */
fun paginate(blocks: List<Block>, heights: Map<String, Float>, boxH: Float): List<List<Block>> {
    fun h(b: Block?): Float = if (b == null) 0f else (heights[b.k] ?: 60f)
    val pages = mutableListOf(mutableListOf<Block>())
    var used = 0f
    var last: String? = null
    blocks.forEachIndexed { i, b ->
        var need = 0f
        var j = i
        while (blocks.getOrNull(j)?.keep == true && j < blocks.size - 1) {
            need += h(blocks[j])
            j++
        }
        need += h(blocks.getOrNull(j))
        if (used + need > boxH && pages.last().isNotEmpty()) {
            pages += mutableListOf<Block>()
            used = 0f
        }
        // a "(contd.)" heading is only added if it still leaves room for the whole chain
        val hc = heights["h" + b.mealId] ?: 60f
        if (b.type != BlockType.HEAD && b.headContd.isNotEmpty() && last == b.mealId &&
            used == 0f && pages.size > 1 && hc + need <= boxH
        ) {
            pages.last().add(Block(k = "ch" + b.mealId + pages.size, type = BlockType.HEAD, mealId = b.mealId, text = b.headContd))
            used += hc
        }
        pages.last().add(b)
        used += h(b)
        last = b.mealId
    }
    return pages
}
