package com.star.menugenerator

import com.star.menugenerator.domain.Block
import com.star.menugenerator.domain.BlockType
import com.star.menugenerator.domain.MenuSettings
import com.star.menugenerator.domain.buildBlocks
import com.star.menugenerator.domain.paginate
import com.star.menugenerator.domain.summarize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocksTest {
    private fun text(blocks: List<Block>) = blocks.joinToString("\n") { listOf(it.text, it.price, it.name).joinToString(" ") }

    @Test fun f_hidden_prices_removed_from_output_data_and_totals_untouched() {
        var m = longMenu()
        assertTrue(Regex("₹250 / Plate").containsMatchIn(text(buildBlocks(m))))
        assertTrue(Regex("₹450 / Plate").containsMatchIn(text(buildBlocks(m))))
        assertTrue(Regex("₹20 each").containsMatchIn(text(buildBlocks(m))))
        m = m.copy(settings = m.settings.copy(showPrice = false, showPackagePrice = false, showBuffetPrice = false, showGrandTotal = false))
        assertFalse(text(buildBlocks(m)).contains("₹"))
        assertEquals("250", m.meals[0].entries[0].pricePerPlate)
        assertEquals(2500000L + 20000L + 4500000L, summarize(m).grand)
    }

    @Test fun i_long_sadhya_is_chunked_nothing_lost_empty_section_hidden_paginates() {
        val blocks = buildBlocks(longMenu())
        val all = blocks.joinToString(" ") { it.text }
        for (i in 0 until 44) assertTrue("Dish$i", all.contains("Dish$i"))
        assertFalse(blocks.any { it.text == "Empty" })
        assertTrue(blocks.any { it.text == "Starters" })
        val h = blocks.associate { it.k to 240f }
        val pages = paginate(blocks, h, 880f)
        assertTrue(pages.size > 1)
        assertEquals(blocks.size, pages.flatten().count { !it.k.startsWith("ch") })
        for (p in pages) assertNotEquals("a heading is never the last block on a page", true, p.last().keep)
    }

    // Golden layouts produced by running the ORIGINAL JavaScript buildBlocks()/paginate() on the same menu.
    private fun signature(pages: List<List<Block>>) = pages.map { p ->
        p.joinToString(" ") { it.type.name.lowercase() + ":" + (if (it.k.startsWith("ch")) "CONTD" else it.k) }
    }

    @Test fun blocks_match_original_javascript() {
        val blocks = buildBlocks(longMenu())
        val sig = blocks.map { "${it.k}|${it.type.name.lowercase()}|keep=${it.keep}|price=${it.price}" }
        assertEquals(
            listOf(
                "hm1|head|keep=true|price=",
                "p1|name|keep=true|price=",
                "p1:s0c0|list|keep=false|price=",
                "p1:s0c1|list|keep=false|price=",
                "p1:s0c2|list|keep=false|price=",
                "p1:s0c3|list|keep=false|price=₹250 / Plate",
                "i1|item|keep=false|price=₹20 each",
                "b1|name|keep=true|price=",
                "b1:s0|sec|keep=true|price=",
                "b1:s0c0|list|keep=false|price=",
                "b1:s1|sec|keep=true|price=",
                "b1:s1c0|list|keep=false|price=₹450 / Plate",
            ),
            sig,
        )
        assertEquals("LUNCH", blocks[0].text)
        assertEquals("Dish0, Dish1, Dish2, Dish3, Dish4, Dish5, Dish6, Dish7, Dish8, Dish9, Dish10, Dish11,", blocks[2].text)
    }

    @Test fun pagination_matches_original_javascript() {
        val blocks = buildBlocks(longMenu())
        val expected240 = listOf(
            "head:hm1 name:p1 list:p1:s0c0",
            "head:CONTD list:p1:s0c1 list:p1:s0c2",
            "head:CONTD list:p1:s0c3 item:i1",
            "name:b1 sec:b1:s0 list:b1:s0c0",
            "head:CONTD sec:b1:s1 list:b1:s1c0",
        )
        assertEquals(expected240, signature(paginate(blocks, blocks.associate { it.k to 240f }, 880f)))
        assertEquals(expected240, signature(paginate(blocks, blocks.associate { it.k to 300f }, 950f)))
        assertEquals(
            listOf(
                "head:hm1 name:p1 list:p1:s0c0 list:p1:s0c1 list:p1:s0c2 list:p1:s0c3 item:i1",
                "head:CONTD name:b1 sec:b1:s0 list:b1:s0c0 sec:b1:s1 list:b1:s1c0",
            ),
            signature(paginate(blocks, blocks.associate { it.k to 100f }, 880f)),
        )
    }

    @Test fun grand_total_block_and_partial_note() {
        val full = longMenu().let { it.copy(settings = it.settings.copy(showGrandTotal = true)) }
        val g = buildBlocks(full).last()
        assertEquals(BlockType.GRAND, g.type)
        assertEquals("Grand Total: ₹70,200", g.text) // 25,000 + 200 + 45,000
    }

    @Test fun grand_total_hidden_by_default_and_when_zero() {
        assertFalse(buildBlocks(longMenu()).any { it.type == BlockType.GRAND })
        val none = menuOf(ind("a", "Tea", "1", "")).let { it.copy(settings = MenuSettings(showGrandTotal = true)) }
        assertFalse(buildBlocks(none).any { it.type == BlockType.GRAND })
        val partial = menuOf(ind("a", "Tea", "1", "10"), pkg("b", "P", "x", "100", "")).let { it.copy(settings = MenuSettings(showGrandTotal = true)) }
        val g = buildBlocks(partial).last()
        assertEquals("Grand Total: ₹10", g.text)
        assertTrue(g.note.startsWith("Partial total"))
    }

    @Test fun headings_caps_and_unnamed_entries() {
        val m = menuOf(ind("a", "Dosa", "1", "5"), ind("b", "   ", "1", "5"))
        val caps = buildBlocks(m)
        assertEquals("LUNCH", caps[0].text)
        assertEquals(2, caps.size) // the unnamed entry is skipped
        val lower = buildBlocks(m.copy(settings = m.settings.copy(caps = false)))
        assertEquals("Lunch", lower[0].text)
    }

    @Test fun quantity_unit_and_hide_flags_for_individual_items() {
        val m = menuOf(ind("a", "Tea", "10", "20").copy(unit = "ltr", description = "Hot", note = "No sugar"))
        val b = buildBlocks(m)[1]
        assertEquals("10 ltr", b.qty)
        assertEquals("₹20 each", b.price)
        assertEquals("Hot", b.desc)
        assertEquals("No sugar", b.note)
        val hidden = buildBlocks(m.copy(settings = m.settings.copy(showQty = false, showPrice = false, showDesc = false, showNotes = false)))[1]
        assertEquals("", hidden.qty); assertEquals("", hidden.price); assertEquals("", hidden.desc); assertEquals("", hidden.note)
    }

    @Test fun buffet_section_names_can_be_hidden() {
        val m = longMenu()
        val hidden = buildBlocks(m.copy(settings = m.settings.copy(showBuffetSections = false)))
        assertFalse(hidden.any { it.type == BlockType.SEC })
    }
}
