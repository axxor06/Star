package com.star.menugenerator

import com.star.menugenerator.domain.ApiEntry
import com.star.menugenerator.domain.ApiMeal
import com.star.menugenerator.domain.ApiMenu
import com.star.menugenerator.domain.MenuValidationException
import com.star.menugenerator.domain.fromApi
import com.star.menugenerator.domain.prepareMenu
import com.star.menugenerator.domain.toApi
import com.star.menugenerator.domain.validateMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ValidationTest {
    @Test fun j_server_validation() {
        val bad = ApiMenu(
            title = " ",
            meals = listOf(
                ApiMeal(
                    id = "mmmmmm", name = "",
                    entries = listOf(
                        ApiEntry(id = "eeeeee", type = "PACKAGE", name = "", pricePerPlate = "-5", plateCount = "1.5"),
                        ApiEntry(type = "NOPE", name = "x"),
                        ApiEntry(id = "ffffff", type = "BUFFET", name = "B", sections = listOf(apiSection("ssssss", "S", emptyList()))),
                    ),
                ),
            ),
        )
        val v = validateMenu(bad)
        assertFalse(v.ok)
        val fields = v.errors.map { it.field }
        for (x in listOf("title", "name", "pricePerPlate", "plateCount", "type")) assertTrue(x, x in fields)

        // the original passes the items as the string 'a,, b ,'; here the list with blanks/spaces
        val good = validateMenu(apiMenu(ApiEntry(id = "good01", type = "PACKAGE", name = "P", items = listOf("a", "", " b "), pricePerPlate = "10", plateCount = "")))
        assertTrue(good.ok)
        val e = good.clean!!.meals!![0].entries!![0]
        assertEquals(listOf("a", "b"), e.items)
        assertNull(e.plateCount)
    }

    @Test fun limits_match_the_original() {
        val longTitle = validateMenu(ApiMenu(title = "x".repeat(121), meals = emptyList()))
        assertTrue(longTitle.errors.any { it.field == "title" && it.message.contains("max 120") })
        assertTrue(validateMenu(ApiMenu(title = "x".repeat(120), meals = emptyList())).ok)

        val tooManyMeals = validateMenu(ApiMenu(title = "T", meals = List(51) { ApiMeal(name = "m$it", entries = emptyList()) }))
        assertTrue(tooManyMeals.errors.any { it.message.contains("Too many meals") })
        assertEquals(50, tooManyMeals.clean!!.meals!!.size)

        val longName = validateMenu(apiMenu(ApiEntry(type = "INDIVIDUAL", name = "n".repeat(201), quantity = "1", unitPrice = "1")))
        assertTrue(longName.errors.any { it.field == "name" && it.message.contains("max 200") })

        val longMeal = validateMenu(ApiMenu(title = "T", meals = listOf(ApiMeal(name = "m".repeat(81), entries = emptyList()))))
        assertTrue(longMeal.errors.any { it.field == "name" && it.message.contains("max 80") })

        val longUnit = validateMenu(apiMenu(ApiEntry(type = "INDIVIDUAL", name = "a", unit = "u".repeat(31))))
        assertTrue(longUnit.errors.any { it.field == "unit" })
        val longNote = validateMenu(apiMenu(ApiEntry(type = "INDIVIDUAL", name = "a", note = "n".repeat(501))))
        assertTrue(longNote.errors.any { it.field == "note" })
        val longDesc = validateMenu(apiMenu(ApiEntry(type = "INDIVIDUAL", name = "a", description = "d".repeat(501))))
        assertTrue(longDesc.errors.any { it.field == "description" })
    }

    @Test fun item_and_section_limits() {
        val many = List(301) { "i$it" }
        val pkgMany = validateMenu(apiMenu(ApiEntry(type = "PACKAGE", name = "p", items = many)))
        assertTrue(pkgMany.errors.any { it.field == "items" && it.message.contains("Too many items") })
        assertEquals(300, pkgMany.clean!!.meals!![0].entries!![0].items!!.size)

        val longItem = validateMenu(apiMenu(ApiEntry(type = "PACKAGE", name = "p", items = listOf("x".repeat(201)))))
        assertTrue(longItem.errors.any { it.message.contains("too long") })

        val secName = validateMenu(apiMenu(ApiEntry(type = "BUFFET", name = "b", sections = listOf(apiSection("sec001", "s".repeat(101), listOf("a"))))))
        assertTrue(secName.errors.any { it.field == "name" && it.message.contains("max 100") })

        val sections = List(51) { apiSection("sec${100 + it}", "S$it", listOf("a")) }
        val secMany = validateMenu(apiMenu(ApiEntry(type = "BUFFET", name = "b", sections = sections)))
        assertEquals(50, secMany.clean!!.meals!![0].entries!![0].sections!!.size) // extra sections are dropped, as in the original
    }

    @Test fun numbers_are_cleaned_and_checked() {
        val v = validateMenu(apiMenu(ApiEntry(type = "INDIVIDUAL", name = "a", quantity = " 1,000 ", unitPrice = "19.999")))
        assertTrue(v.errors.any { it.field == "unitPrice" && it.message.startsWith("Price per unit:") })
        assertEquals("1000", v.clean!!.meals!![0].entries!![0].quantity)
        val bigQty = validateMenu(apiMenu(ApiEntry(type = "INDIVIDUAL", name = "a", quantity = "10000001", unitPrice = "1")))
        assertTrue(bigQty.errors.any { it.field == "quantity" })
    }

    @Test fun missing_meals_and_input() {
        assertFalse(validateMenu(null).ok)
        assertTrue(validateMenu(ApiMenu(title = "T", meals = null)).errors.any { it.field == "meals" })
    }

    @Test fun ids_are_kept_when_valid_and_regenerated_when_invalid_or_duplicate() {
        var n = 0
        val v = validateMenu(
            apiMenu(
                ApiEntry(id = "same-id-1", type = "INDIVIDUAL", name = "a"),
                ApiEntry(id = "same-id-1", type = "INDIVIDUAL", name = "b"),
                ApiEntry(id = "x", type = "INDIVIDUAL", name = "c"),
            ),
            genId = { "gen-${++n}x" },
        )
        val ids = v.clean!!.meals!![0].entries!!.map { it.id }
        assertEquals("same-id-1", ids[0])
        assertTrue(ids[1]!!.startsWith("gen-"))
        assertTrue(ids[2]!!.startsWith("gen-"))
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun api_round_trip_keeps_data() {
        val m = longMenu()
        val api = toApi(m, "green")
        assertTrue(validateMenu(api).ok)
        val back = fromApi(api.copy(meals = validateMenu(api).clean!!.meals))
        assertEquals("green", back.templateId)
        assertEquals("A, B, C", back.menu.meals[0].entries[2].sections[0].itemsText)
    }

    @Test fun prepare_menu_computes_stored_summary() {
        val p = prepareMenu(toApi(menuOf(ind("a1a1a1", "Dosa", "100", "5"), pkg("b2b2b2", "S", "x", "250", "")), "purple"))
        assertEquals(50000L, p.total)
        assertFalse(p.complete)          // package has no plate count
        assertEquals(2, p.entryCount)
        assertEquals("MIXED", p.type)
        assertEquals("EMPTY", prepareMenu(toApi(menuOf(), "purple")).type)
        assertEquals("INDIVIDUAL", prepareMenu(toApi(menuOf(ind("a1a1a1", "Dosa", "1", "1")), "purple")).type)
    }

    @Test fun prepare_menu_rejects_invalid() {
        try {
            prepareMenu(toApi(menuOf(ind("a1a1a1", "Dosa", "-1", "5")), "purple"))
            fail("expected MenuValidationException")
        } catch (e: MenuValidationException) {
            assertTrue(e.errors.any { it.field == "quantity" })
        }
    }
}
