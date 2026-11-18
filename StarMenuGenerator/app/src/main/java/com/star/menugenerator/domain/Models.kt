package com.star.menugenerator.domain

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

// ---------------------------------------------------------------------------------------------
// Editor-side data model (everything the user types is kept as text, exactly like the original
// React project, so partially typed numbers never get silently changed).
// ---------------------------------------------------------------------------------------------

enum class EntryType { INDIVIDUAL, PACKAGE, BUFFET }

data class Section(
    val id: String,
    val name: String = "",
    val itemsText: String = "",
)

/** One menu entry. Which fields are used depends on [type]; unused fields stay empty. */
data class Entry(
    val id: String,
    val type: EntryType,
    val name: String = "",
    // INDIVIDUAL
    val quantity: String = "",
    val unit: String = "",
    val unitPrice: String = "",
    val description: String = "",
    val note: String = "",
    // PACKAGE (including Sadhya)
    val itemsText: String = "",
    // PACKAGE + BUFFET
    val pricePerPlate: String = "",
    val plateCount: String = "",
    // BUFFET
    val sections: List<Section> = emptyList(),
)

data class Meal(
    val id: String,
    val name: String = "",
    val entries: List<Entry> = emptyList(),
)

const val FONT_ELEGANT = "elegant"
const val FONT_CLEAN = "clean"
const val FONT_TRADITIONAL = "traditional"
const val ALIGN_LEFT = "left"
const val ALIGN_CENTER = "center"
val FONT_CHOICES = listOf(FONT_ELEGANT, FONT_CLEAN, FONT_TRADITIONAL)

/** What is printed on the card. Hiding something never changes stored data or totals. */
data class MenuSettings(
    val font: String = FONT_ELEGANT,
    val fontScale: Double = 1.0,
    val align: String = ALIGN_CENTER,
    val caps: Boolean = true,
    val showQty: Boolean = true,
    val showPrice: Boolean = true,
    val showPackagePrice: Boolean = true,
    val showBuffetPrice: Boolean = true,
    val showBuffetSections: Boolean = true,
    val showDesc: Boolean = true,
    val showNotes: Boolean = true,
    val showGrandTotal: Boolean = false,
)

data class Menu(
    val title: String,
    val meals: List<Meal>,
    val settings: MenuSettings = MenuSettings(),
)

// ---------------------------------------------------------------------------------------------
// Factories (same defaults as the original project)
// ---------------------------------------------------------------------------------------------

fun newId(): String = UUID.randomUUID().toString()

val DEFAULT_MEALS = listOf("Breakfast", "Lunch", "Evening", "Dinner", "Late Night")

fun blankIndividual() = Entry(id = newId(), type = EntryType.INDIVIDUAL)
fun blankPackage() = Entry(id = newId(), type = EntryType.PACKAGE)
fun blankSection() = Section(id = newId())
fun blankBuffet() = Entry(id = newId(), type = EntryType.BUFFET, sections = listOf(blankSection()))
fun blankEntry(type: EntryType): Entry = when (type) {
    EntryType.INDIVIDUAL -> blankIndividual()
    EntryType.PACKAGE -> blankPackage()
    EntryType.BUFFET -> blankBuffet()
}

fun blankMeal(name: String = "") = Meal(id = newId(), name = name)
fun blankMenu() = Menu(title = "Untitled menu", meals = DEFAULT_MEALS.map { blankMeal(it) })

// ---------------------------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------------------------

fun Meal.namedEntries(): List<Entry> = entries.filter { it.name.trim().isNotEmpty() }
fun Menu.hasContent(): Boolean = meals.any { it.namedEntries().isNotEmpty() }

/** Returns a copy with items [i] and [i]+[d] swapped (used for move up / move down). */
fun <T> List<T>.swapped(i: Int, d: Int): List<T> {
    val j = i + d
    if (i !in indices || j !in indices) return this
    val c = toMutableList()
    val t = c[i]; c[i] = c[j]; c[j] = t
    return c
}

fun Entry.withNewIds(): Entry = copy(id = newId(), sections = sections.map { it.copy(id = newId()) })
fun Meal.withNewIds(): Meal = copy(id = newId(), entries = entries.map { it.withNewIds() })

// ---------------------------------------------------------------------------------------------
// Timestamps: fixed-width UTC ISO strings so that plain string comparison sorts correctly.
// ---------------------------------------------------------------------------------------------

private val ISO_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

fun nowIso(): String = ISO_FORMAT.format(Instant.now())

fun normalizeIso(value: String?, fallback: String): String =
    try {
        ISO_FORMAT.format(Instant.parse(value ?: ""))
    } catch (e: Exception) {
        fallback
    }
