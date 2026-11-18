package com.star.menugenerator

import com.star.menugenerator.domain.ApiEntry
import com.star.menugenerator.domain.ApiMeal
import com.star.menugenerator.domain.ApiMenu
import com.star.menugenerator.domain.ApiSection
import com.star.menugenerator.domain.Entry
import com.star.menugenerator.domain.EntryType
import com.star.menugenerator.domain.Meal
import com.star.menugenerator.domain.Menu
import com.star.menugenerator.domain.Section

// Same helpers as the original shared.test.js (ind / pkg / buf / M), in editor shape.
fun ind(id: String, name: String, quantity: String, unitPrice: String) =
    Entry(id = id, type = EntryType.INDIVIDUAL, name = name, quantity = quantity, unitPrice = unitPrice)

fun pkg(id: String, name: String, items: String, pricePerPlate: String, plateCount: String) =
    Entry(id = id, type = EntryType.PACKAGE, name = name, itemsText = items, pricePerPlate = pricePerPlate, plateCount = plateCount)

fun buf(id: String, name: String, pricePerPlate: String, plateCount: String, sections: List<Section>) =
    Entry(id = id, type = EntryType.BUFFET, name = name, pricePerPlate = pricePerPlate, plateCount = plateCount, sections = sections)

fun menuOf(vararg entries: Entry) = Menu("T", listOf(Meal("m1", "Lunch", entries.toList())))

/** Same long menu as the original tests (44-dish Sadhya, a priced item, a buffet with an empty section). */
fun longMenu(): Menu {
    val names = (0 until 44).joinToString(", ") { "Dish$it" }
    val entries = listOf(
        Entry(id = "p1", type = EntryType.PACKAGE, name = "Onam Sadhya", itemsText = names, pricePerPlate = "250", plateCount = "100"),
        Entry(id = "i1", type = EntryType.INDIVIDUAL, name = "Water", quantity = "10", unit = "", unitPrice = "20"),
        Entry(
            id = "b1", type = EntryType.BUFFET, name = "Grand", pricePerPlate = "450", plateCount = "100",
            sections = listOf(
                Section("s1", "Starters", "A, B, C"),
                Section("s2", "Empty", " , ,"),
                Section("s3", "Desserts", "Ice Cream"),
            ),
        ),
    )
    return Menu("Untitled menu", listOf(Meal("m1", "lunch", entries)))
}

fun apiMenu(vararg entries: ApiEntry) =
    ApiMenu(title = "T", meals = listOf(ApiMeal(id = "m1xxxx", name = "Lunch", entries = entries.toList())))

fun apiSection(id: String, name: String, items: List<String>) = ApiSection(id = id, name = name, items = items)
