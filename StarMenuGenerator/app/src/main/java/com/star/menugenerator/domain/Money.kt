package com.star.menugenerator.domain

import java.math.BigInteger

// ---------------------------------------------------------------------------------------------
// Exact money maths. Direct port of shared/calc.js from the original React project.
// All amounts are whole paise (1 rupee = 100 paise) held in BigInteger while calculating, so no
// floating-point error can ever creep into a total. Missing values are never assumed to be 0 or 1.
// ---------------------------------------------------------------------------------------------

/** Largest number accepted in any numeric field. */
const val NUMBER_MAX: Double = 1e7

private val NEGATIVE = Regex("-\\d*\\.?\\d*")
private val PLAIN = Regex("\\d+(\\.\\d+)?")
private val ITEM_SPLIT = Regex("[,\\n\\r]+")

data class NumCheck(
    val empty: Boolean = false,
    val error: String? = null,
    val value: Double = 0.0,
    val scaled: BigInteger? = null,
)

private fun clean(v: String?): String = (v ?: "").trim().replace(",", "")

private fun scaled(s: String, d: Int): BigInteger {
    val whole = s.substringBefore('.')
    val frac = if ('.' in s) s.substringAfter('.') else ""
    return BigInteger(whole + frac.padEnd(d, '0').take(d))
}

/** "Uppu, Kaya Upperi,, \n Pappadam," -> [Uppu, Kaya Upperi, Pappadam] (never dedupes real names). */
fun parseItems(text: String?): List<String> =
    (text ?: "").split(ITEM_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Checks a typed number. Result is one of: empty, error, or a value with its scaled integer form
 * (value x 10^decimals, or the plain integer when [integer] is true).
 */
fun checkNumber(
    v: String?,
    integer: Boolean = false,
    decimals: Int = 2,
    max: Double = NUMBER_MAX,
): NumCheck {
    val s = clean(v)
    if (s.isEmpty()) return NumCheck(empty = true)
    if (NEGATIVE.matches(s) && s.any { it in '0'..'9' }) return NumCheck(error = "Cannot be negative")
    if (!PLAIN.matches(s)) return NumCheck(error = "Enter a valid number")
    if (integer && '.' in s) return NumCheck(error = "Enter a whole number")
    if (!integer && s.substringAfter('.', "").length > decimals) {
        return NumCheck(error = "Use at most $decimals decimal places")
    }
    if (s.toDouble() > max) return NumCheck(error = "Number is too large")
    return NumCheck(value = s.toDouble(), scaled = scaled(s, if (integer) 0 else decimals))
}

/** ok = counted; incomplete = price known but quantity/plates missing; unpriced; invalid. */
enum class EntryStatus { OK, INCOMPLETE, UNPRICED, INVALID }

data class EntryResult(val status: EntryStatus, val reason: String = "", val paise: Long = 0L)

private val THOUSAND = BigInteger.valueOf(1000)
private val FIVE_HUNDRED = BigInteger.valueOf(500)

fun entryTotal(e: Entry): EntryResult {
    val ind = e.type == EntryType.INDIVIDUAL
    val a = if (ind) checkNumber(e.quantity, decimals = 3) else checkNumber(e.plateCount, integer = true)
    val b = checkNumber(if (ind) e.unitPrice else e.pricePerPlate, decimals = 2)
    val la = if (ind) "quantity" else "number of plates"
    val lb = if (ind) "price per unit" else "price per plate"
    if (a.error != null) return EntryResult(EntryStatus.INVALID, "Check the $la: ${a.error.lowercase()}")
    if (b.error != null) return EntryResult(EntryStatus.INVALID, "Check the $lb: ${b.error.lowercase()}")
    if (b.empty) return EntryResult(EntryStatus.UNPRICED, "No $lb entered")
    if (a.empty) return EntryResult(EntryStatus.INCOMPLETE, "Enter the $la to include this in the total")
    val qty = a.scaled!!
    val price = b.scaled!!
    // Individual: quantity has 3 decimals and price 2, so the product is paise x 1000; round half up.
    val p = if (ind) qty.multiply(price).add(FIVE_HUNDRED).divide(THOUSAND) else qty.multiply(price)
    return EntryResult(EntryStatus.OK, "", clampToLong(p))
}

private fun clampToLong(v: BigInteger): Long =
    if (v.bitLength() > 62) Long.MAX_VALUE / 4 else v.toLong()

data class Issue(val id: String, val meal: String, val name: String, val reason: String)

data class Summary(
    val individual: Long,
    val packages: Long,
    val buffets: Long,
    val grand: Long,
    val complete: Boolean,
    val issues: List<Issue>,
    val byEntry: Map<String, EntryResult>,
)

/** Separate Individual / Package / Buffet totals plus the grand total, and what is not counted. */
fun summarize(menu: Menu): Summary {
    var ind = BigInteger.ZERO
    var pkg = BigInteger.ZERO
    var buf = BigInteger.ZERO
    val issues = mutableListOf<Issue>()
    val byEntry = LinkedHashMap<String, EntryResult>()
    for (m in menu.meals) {
        for (e in m.entries) {
            val r = entryTotal(e)
            byEntry[e.id] = r
            if (r.status == EntryStatus.OK) {
                val p = BigInteger.valueOf(r.paise)
                when (e.type) {
                    EntryType.INDIVIDUAL -> ind = ind.add(p)
                    EntryType.PACKAGE -> pkg = pkg.add(p)
                    EntryType.BUFFET -> buf = buf.add(p)
                }
            } else {
                issues += Issue(e.id, m.name, if (e.name.isEmpty()) "Untitled" else e.name, r.reason)
            }
        }
    }
    return Summary(
        individual = clampToLong(ind),
        packages = clampToLong(pkg),
        buffets = clampToLong(buf),
        grand = clampToLong(ind.add(pkg).add(buf)),
        complete = issues.isEmpty(),
        issues = issues,
        byEntry = byEntry,
    )
}

/** 6425000 -> "₹64,250". Indian digit grouping (12,34,567), paise shown only when non-zero. */
fun formatPaise(p: Long): String {
    val rupees = (p / 100).toString()
    val paise = (p % 100).toInt()
    val last3 = if (rupees.length > 3) rupees.takeLast(3) else rupees
    var rest = if (rupees.length > 3) rupees.dropLast(3) else ""
    val groups = ArrayList<String>()
    while (rest.length > 2) {
        groups.add(0, rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) groups.add(0, rest)
    val grouped = if (groups.isEmpty()) last3 else groups.joinToString(",") + "," + last3
    return "₹" + grouped + if (paise != 0) "." + paise.toString().padStart(2, '0') else ""
}

/** Formats a typed price for display; "" if empty, invalid or zero. */
fun formatInput(v: String?): String {
    val sc = checkNumber(v).scaled
    return if (sc != null && sc.signum() > 0) formatPaise(clampToLong(sc)) else ""
}
