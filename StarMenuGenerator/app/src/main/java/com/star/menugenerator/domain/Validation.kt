package com.star.menugenerator.domain

import kotlinx.serialization.json.JsonObject

// ---------------------------------------------------------------------------------------------
// Port of shared/validate.js. Validates the API-shaped menu, returns field errors and a cleaned copy.
// Limits are the original ones: title 120, meal name 80, entry name 200, section name 100,
// unit 30, description/note 500, item name 200, 50 meals, 300 entries per meal, 50 sections,
// 300 items per list.
// ---------------------------------------------------------------------------------------------

private val ID_PATTERN = Regex("[A-Za-z0-9_-]{6,64}")
private val ENTRY_TYPES = setOf("INDIVIDUAL", "PACKAGE", "BUFFET")

data class FieldError(val id: String, val field: String, val message: String)

data class ValidationResult(val ok: Boolean, val errors: List<FieldError>, val clean: ApiMenu?)

private fun numStr(v: String?): String? {
    val s = (v ?: "").trim().replace(",", "")
    return if (s.isEmpty()) null else s
}

fun validateMenu(input: ApiMenu?, genId: () -> String = ::newId): ValidationResult {
    val errors = mutableListOf<FieldError>()
    val seen = HashSet<String>()

    fun err(id: String, field: String, message: String) {
        errors += FieldError(id, field, message)
    }

    fun cid(x: String?): String {
        val id = if (x != null && ID_PATTERN.matches(x) && x !in seen) x else genId()
        seen += id
        return id
    }

    fun text(id: String, field: String, v: String?, required: Boolean = false, max: Int = 200, label: String = "This"): String {
        val s = v?.trim() ?: ""
        if (required && s.isEmpty()) err(id, field, "$label cannot be empty")
        if (s.length > max) err(id, field, "$label is too long (max $max characters)")
        return s
    }

    fun num(id: String, field: String, v: String?, label: String, integer: Boolean = false, decimals: Int = 2): String? {
        val r = checkNumber(v, integer = integer, decimals = decimals)
        if (r.error != null) err(id, field, "$label: ${r.error}")
        return numStr(v)
    }

    if (input == null) {
        return ValidationResult(false, listOf(FieldError("menu", "body", "Menu data is missing")), null)
    }

    val title = text("menu", "title", input.title, required = true, max = 120, label = "Menu name")
    val eventDetails = input.eventDetails?.trim()?.take(500)?.ifEmpty { null }
    val settings: JsonObject = input.settings ?: JsonObject(emptyMap())

    val rawMeals = input.meals
    if (rawMeals == null) {
        err("menu", "meals", "Meals must be a list")
        return ValidationResult(false, errors, ApiMenu(title, eventDetails, settings, emptyList()))
    }
    if (rawMeals.size > 50) err("menu", "meals", "Too many meals (max 50)")

    val cleanMeals = mutableListOf<ApiMeal>()
    for (m in rawMeals.take(50)) {
        val mealId = cid(m.id)
        val mealName = text(mealId, "name", m.name, required = true, max = 80, label = "Meal name")
        val entries = mutableListOf<ApiEntry>()
        for (e in (m.entries ?: emptyList()).take(300)) {
            val type = e.type
            if (type == null || type !in ENTRY_TYPES) {
                err(e.id ?: mealId, "type", "Unknown entry type \"${type ?: "undefined"}\"")
                continue
            }
            val id = cid(e.id)
            val name = text(id, "name", e.name, required = true, label = "Name")
            if (type == "INDIVIDUAL") {
                entries += ApiEntry(
                    id = id, type = type, name = name,
                    quantity = num(id, "quantity", e.quantity, "Quantity", decimals = 3),
                    unit = text(id, "unit", e.unit, max = 30, label = "Unit").ifEmpty { null },
                    unitPrice = num(id, "unitPrice", e.unitPrice, "Price per unit"),
                    description = text(id, "description", e.description, max = 500, label = "Description").ifEmpty { null },
                    note = text(id, "note", e.note, max = 500, label = "Note").ifEmpty { null },
                )
            } else {
                val pricePerPlate = num(id, "pricePerPlate", e.pricePerPlate, "Price per plate")
                val plateCount = num(id, "plateCount", e.plateCount, "Number of plates", integer = true)
                if (type == "PACKAGE") {
                    val items = (e.items ?: emptyList()).map { it.trim() }.filter { it.isNotEmpty() }
                    if (items.size > 300) err(id, "items", "Too many items (max 300)")
                    if (items.any { it.length > 200 }) err(id, "items", "An item name is too long")
                    entries += ApiEntry(
                        id = id, type = type, name = name,
                        items = items.take(300), pricePerPlate = pricePerPlate, plateCount = plateCount,
                    )
                } else {
                    val sections = mutableListOf<ApiSection>()
                    for (s in (e.sections ?: emptyList()).take(50)) {
                        val sid = cid(s.id)
                        val items = (s.items ?: emptyList()).map { it.trim() }.filter { it.isNotEmpty() }
                        if (items.any { it.length > 200 }) err(sid, "items", "An item name is too long")
                        if (items.size > 300) err(sid, "items", "Too many items in one section (max 300)")
                        sections += ApiSection(
                            id = sid,
                            name = text(sid, "name", s.name, required = true, max = 100, label = "Section name"),
                            items = items.take(300),
                        )
                    }
                    entries += ApiEntry(
                        id = id, type = type, name = name,
                        pricePerPlate = pricePerPlate, plateCount = plateCount, sections = sections,
                    )
                }
            }
        }
        cleanMeals += ApiMeal(id = mealId, name = mealName, entries = entries)
    }
    return ValidationResult(errors.isEmpty(), errors, ApiMenu(title, eventDetails, settings, cleanMeals))
}

// ---------------------------------------------------------------------------------------------
// prepareMenu: validate + compute the stored summary values (menuService.js prepareMenu/menuTypeOf)
// ---------------------------------------------------------------------------------------------

class MenuValidationException(val errors: List<FieldError>) :
    Exception(errors.firstOrNull()?.message ?: "Some menu fields are not valid")

data class PreparedMenu(
    val title: String,
    val settings: JsonObject,
    val meals: List<ApiMeal>,
    val type: String,
    val entryCount: Int,
    val total: Long,
    val complete: Boolean,
)

/** EMPTY | INDIVIDUAL | PACKAGE | BUFFET | MIXED */
fun menuTypeOf(meals: List<ApiMeal>): String {
    val types = meals.flatMap { m -> (m.entries ?: emptyList()).mapNotNull { it.type } }.toSet()
    return when {
        types.isEmpty() -> "EMPTY"
        types.size == 1 -> types.first()
        else -> "MIXED"
    }
}

fun prepareMenu(input: ApiMenu?, genId: () -> String = ::newId): PreparedMenu {
    val v = validateMenu(input, genId)
    if (!v.ok || v.clean == null) throw MenuValidationException(v.errors)
    val clean = v.clean
    val summary = summarize(fromApi(clean).menu)
    val meals = clean.meals ?: emptyList()
    return PreparedMenu(
        title = clean.title ?: "",
        settings = clean.settings ?: JsonObject(emptyMap()),
        meals = meals,
        type = menuTypeOf(meals),
        entryCount = meals.sumOf { (it.entries ?: emptyList()).size },
        total = summary.grand,
        complete = summary.complete,
    )
}
