package com.star.menugenerator

import com.star.menugenerator.domain.EntryStatus
import com.star.menugenerator.domain.Section
import com.star.menugenerator.domain.checkNumber
import com.star.menugenerator.domain.entryTotal
import com.star.menugenerator.domain.formatInput
import com.star.menugenerator.domain.formatPaise
import com.star.menugenerator.domain.parseItems
import com.star.menugenerator.domain.summarize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports of the original project's shared/shared.test.js calculation tests (letters match the originals). */
class CalcTest {
    @Test fun a_individual_items() {
        val s = summarize(menuOf(ind("a1", "Dosa", "100", "5"), ind("a2", "Idli", "50", "8")))
        assertEquals(90000L, s.grand)
        assertEquals("₹900", formatPaise(s.grand))
    }

    @Test fun b_sadhya_package() {
        assertEquals(2500000L, summarize(menuOf(pkg("b1", "Sadhya", "x", "250", "100"))).grand)
    }

    @Test fun c_multiple_packages() {
        val s = summarize(menuOf(pkg("c1", "S", "", "250", "100"), pkg("c2", "B", "", "180", "50"), pkg("c3", "V", "", "120", "30")))
        assertEquals(3760000L, s.grand) // 25,000 + 9,000 + 3,600
    }

    @Test fun d_buffet_not_counted_per_item() {
        val s = summarize(menuOf(buf("d1", "G", "450", "100", listOf(Section("s1", "a", "x, y")))))
        assertEquals(4500000L, s.grand)
        assertEquals(4500000L, s.buffets)
    }

    @Test fun e_mixed_and_document_example_total() {
        val s = summarize(
            menuOf(
                ind("e1", "Dosa", "100", "5"), ind("e2", "Idli", "50", "8"), ind("e3", "Vada", "30", "10"),
                pkg("e4", "P", "", "180", "100"), buf("e5", "B", "450", "100", emptyList()),
            ),
        )
        assertEquals("₹64,200", formatPaise(s.grand))
        assertEquals("₹1,200", formatPaise(s.individual))
        assertEquals(1800000L, s.packages)
        assertEquals(4500000L, s.buffets)
    }

    @Test fun g_incomplete_is_flagged_never_assumed_zero_or_one() {
        val s = summarize(menuOf(pkg("g1", "P", "", "180", ""), ind("g2", "Tea", "10", "")))
        assertEquals(0L, s.grand)
        assertFalse(s.complete)
        assertEquals(2, s.issues.size)
        assertEquals(EntryStatus.INCOMPLETE, s.byEntry["g1"]!!.status)
        assertEquals(EntryStatus.UNPRICED, s.byEntry["g2"]!!.status)
    }

    @Test fun decimal_money_is_exact() {
        val s = summarize(menuOf(ind("h1", "a", "3", "0.1"), ind("h2", "b", "2.5", "19.99")))
        assertEquals(30L + 4998L, s.grand)
    }

    @Test fun j_invalid_numbers() {
        for (v in listOf("-1", "abc", "1.234")) assertNotNull(v, checkNumber(v, decimals = 2).error)
        assertNotNull(checkNumber("2.5", integer = true).error)
    }

    @Test fun j_parse_items_handles_commas_newlines_spaces() {
        assertEquals(
            listOf("Uppu", "Kaya Upperi", "Pappadam", "Pazham"),
            parseItems(",, Uppu,  Kaya Upperi ,\n\nPappadam,\r\nPazham,,"),
        )
    }

    // ---- extra edge cases beyond the original tests ----

    @Test fun number_limits_and_messages() {
        assertNull(checkNumber("10000000").error)
        assertEquals("Number is too large", checkNumber("10000001").error)
        assertEquals("Cannot be negative", checkNumber("-5").error)
        assertEquals("Enter a valid number", checkNumber("1e3").error)
        assertEquals("Enter a valid number", checkNumber(".5").error)
        assertEquals("Enter a whole number", checkNumber("2.5", integer = true).error)
        assertEquals("Use at most 2 decimal places", checkNumber("1.234", decimals = 2).error)
        assertEquals("Use at most 3 decimal places", checkNumber("1.2345", decimals = 3).error)
        assertTrue(checkNumber("   ").empty)
        assertTrue(checkNumber(null).empty)
        assertEquals(100000L, checkNumber("1,000", decimals = 2).scaled!!.toLong()) // commas stripped, scaled x100
    }

    @Test fun individual_rounds_half_up_to_the_paisa() {
        assertEquals(1L, entryTotal(ind("x", "a", "0.5", "0.01")).paise)   // 0.5 paise -> 1
        assertEquals(0L, entryTotal(ind("x", "a", "0.499", "0.01")).paise) // 0.499 paise -> 0
        assertEquals(EntryStatus.OK, entryTotal(ind("x", "a", "1", "0")).status) // a price of 0 is a real price
    }

    @Test fun invalid_values_never_produce_a_total() {
        for (e in listOf(ind("x", "a", "-3", "5"), ind("x", "a", "3", "-5"), ind("x", "a", "abc", "5"), pkg("x", "p", "", "10", "1.5"))) {
            val r = entryTotal(e)
            assertEquals(EntryStatus.INVALID, r.status)
            assertEquals(0L, r.paise)
        }
        assertEquals(0L, summarize(menuOf(ind("x", "a", "-3", "5"))).grand)
    }

    @Test fun plates_must_be_whole_numbers() {
        assertEquals(EntryStatus.INVALID, entryTotal(pkg("x", "p", "", "10", "2.5")).status)
        assertEquals(5000L, entryTotal(pkg("x", "p", "", "10", "5")).paise) // 10 x 5 = Rs 50 = 5000 paise
    }

    @Test fun largest_allowed_values_do_not_overflow() {
        val r = entryTotal(ind("x", "a", "10000000", "10000000"))
        assertEquals(EntryStatus.OK, r.status)
        assertEquals(100_000_000_000_000L * 100, r.paise) // 1e14 rupees = 1e16 paise
    }

    @Test fun indian_grouping() {
        assertEquals("₹0", formatPaise(0))
        assertEquals("₹999", formatPaise(99900))
        assertEquals("₹1,000", formatPaise(100000))
        assertEquals("₹1,00,000", formatPaise(10000000))
        assertEquals("₹12,34,567.89", formatPaise(123456789))
        assertEquals("₹2.50", formatPaise(250))
        assertEquals("₹0.05", formatPaise(5))
    }

    @Test fun format_input_hides_empty_invalid_and_zero() {
        assertEquals("", formatInput(""))
        assertEquals("", formatInput("abc"))
        assertEquals("", formatInput("0"))
        assertEquals("₹250", formatInput("250"))
        assertEquals("₹19.99", formatInput("19.99"))
    }

    @Test fun separate_totals_add_up_to_grand_total() {
        val s = summarize(menuOf(ind("a", "a", "2", "10"), pkg("p", "p", "", "100", "3"), buf("b", "b", "50", "4", emptyList())))
        assertEquals(s.individual + s.packages + s.buffets, s.grand)
        assertEquals(2000L, s.individual)
        assertEquals(30000L, s.packages)
        assertEquals(20000L, s.buffets)
    }
}
