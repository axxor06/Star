package com.star.menugenerator.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

/** One JSON configuration for the whole app (database rows and backup files). */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

// ---------------------------------------------------------------------------------------------
// "API shape": the stored / backup shape, identical to the original project's toApi() output
// (package items and buffet section items are lists; optional values are null when empty).
// ---------------------------------------------------------------------------------------------

@Serializable
data class ApiSection(
    val id: String? = null,
    val name: String? = null,
    val items: List<String>? = null,
)

@Serializable
data class ApiEntry(
    val id: String? = null,
    val type: String? = null,
    val name: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val unitPrice: String? = null,
    val description: String? = null,
    val note: String? = null,
    val items: List<String>? = null,
    val pricePerPlate: String? = null,
    val plateCount: String? = null,
    val sections: List<ApiSection>? = null,
)

@Serializable
data class ApiMeal(
    val id: String? = null,
    val name: String? = null,
    val entries: List<ApiEntry>? = null,
)

@Serializable
data class ApiMenu(
    val title: String? = null,
    val eventDetails: String? = null,
    val settings: JsonObject? = null,
    val meals: List<ApiMeal>? = null,
)

// ---------------------------------------------------------------------------------------------
// Settings <-> JSON
// ---------------------------------------------------------------------------------------------

fun MenuSettings.toJson(templateId: String? = null): JsonObject = buildJsonObject {
    put("font", font)
    put("fontScale", fontScale)
    put("align", align)
    put("caps", caps)
    put("showQty", showQty)
    put("showPrice", showPrice)
    put("showPackagePrice", showPackagePrice)
    put("showBuffetPrice", showBuffetPrice)
    put("showBuffetSections", showBuffetSections)
    put("showDesc", showDesc)
    put("showNotes", showNotes)
    put("showGrandTotal", showGrandTotal)
    if (templateId != null) put("templateId", templateId)
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
private fun JsonObject.dbl(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

/** Missing or unreadable keys fall back to defaults (same as `{...defaultSettings(), ...stored}`). */
fun settingsFromJson(o: JsonObject?): MenuSettings {
    val d = MenuSettings()
    if (o == null) return d
    return MenuSettings(
        font = o.str("font")?.takeIf { it in FONT_CHOICES } ?: d.font,
        fontScale = (o.dbl("fontScale") ?: d.fontScale).coerceIn(0.7, 1.4),
        align = o.str("align")?.takeIf { it == ALIGN_LEFT || it == ALIGN_CENTER } ?: d.align,
        caps = o.bool("caps") ?: d.caps,
        showQty = o.bool("showQty") ?: d.showQty,
        showPrice = o.bool("showPrice") ?: d.showPrice,
        showPackagePrice = o.bool("showPackagePrice") ?: d.showPackagePrice,
        showBuffetPrice = o.bool("showBuffetPrice") ?: d.showBuffetPrice,
        showBuffetSections = o.bool("showBuffetSections") ?: d.showBuffetSections,
        showDesc = o.bool("showDesc") ?: d.showDesc,
        showNotes = o.bool("showNotes") ?: d.showNotes,
        showGrandTotal = o.bool("showGrandTotal") ?: d.showGrandTotal,
    )
}

// ---------------------------------------------------------------------------------------------
// Editor <-> API conversion (ports toApi / fromApi from menuModel.js)
// ---------------------------------------------------------------------------------------------

private fun nz(v: String): String? = v.trim().ifEmpty { null }

private fun entryToApi(e: Entry): ApiEntry = when (e.type) {
    EntryType.INDIVIDUAL -> ApiEntry(
        id = e.id, type = "INDIVIDUAL", name = e.name,
        quantity = nz(e.quantity), unit = nz(e.unit), unitPrice = nz(e.unitPrice),
        description = nz(e.description), note = nz(e.note),
    )
    EntryType.PACKAGE -> ApiEntry(
        id = e.id, type = "PACKAGE", name = e.name,
        items = parseItems(e.itemsText), pricePerPlate = nz(e.pricePerPlate), plateCount = nz(e.plateCount),
    )
    EntryType.BUFFET -> ApiEntry(
        id = e.id, type = "BUFFET", name = e.name,
        pricePerPlate = nz(e.pricePerPlate), plateCount = nz(e.plateCount),
        sections = e.sections.map { ApiSection(id = it.id, name = it.name, items = parseItems(it.itemsText)) },
    )
}

fun toApi(menu: Menu, templateId: String): ApiMenu = ApiMenu(
    title = menu.title,
    settings = menu.settings.toJson(templateId),
    meals = menu.meals.map { m -> ApiMeal(id = m.id, name = m.name, entries = m.entries.map(::entryToApi)) },
)

private fun entryFromApi(e: ApiEntry): Entry? {
    val id = e.id ?: newId()
    val name = e.name ?: ""
    return when (e.type) {
        "INDIVIDUAL" -> Entry(
            id = id, type = EntryType.INDIVIDUAL, name = name,
            quantity = e.quantity ?: "", unit = e.unit ?: "", unitPrice = e.unitPrice ?: "",
            description = e.description ?: "", note = e.note ?: "",
        )
        "PACKAGE" -> Entry(
            id = id, type = EntryType.PACKAGE, name = name,
            itemsText = (e.items ?: emptyList()).joinToString(", "),
            pricePerPlate = e.pricePerPlate ?: "", plateCount = e.plateCount ?: "",
        )
        "BUFFET" -> Entry(
            id = id, type = EntryType.BUFFET, name = name,
            pricePerPlate = e.pricePerPlate ?: "", plateCount = e.plateCount ?: "",
            sections = (e.sections ?: emptyList()).map {
                Section(id = it.id ?: newId(), name = it.name ?: "", itemsText = (it.items ?: emptyList()).joinToString(", "))
            },
        )
        else -> null
    }
}

data class LoadedMenu(val templateId: String, val menu: Menu)

fun fromApi(api: ApiMenu): LoadedMenu {
    val templateId = getTemplate(api.settings?.str("templateId")).id
    return LoadedMenu(
        templateId = templateId,
        menu = Menu(
            title = api.title ?: "",
            settings = settingsFromJson(api.settings),
            meals = (api.meals ?: emptyList()).map { m ->
                Meal(
                    id = m.id ?: newId(),
                    name = m.name ?: "",
                    entries = (m.entries ?: emptyList()).mapNotNull(::entryFromApi),
                )
            },
        ),
    )
}
