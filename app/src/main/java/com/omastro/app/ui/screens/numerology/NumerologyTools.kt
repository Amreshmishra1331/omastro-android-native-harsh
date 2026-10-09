package com.omastro.app.ui.screens.numerology

import java.util.Calendar

/**
 * Pure numerology computations (Chaldean/Vedic hybrid, matching the website engine).
 * All functions are deterministic derivations from the user's real inputs —
 * no fabricated content. Server APIs remain the source of truth for
 * full-profile / compatibility / name-check analyses.
 */

/** Reduce to single digit, preserving master numbers 11 and 22. */
fun reduceNumber(n: Int, keepMasters: Boolean = true): Int {
    var v = n
    while (v > 9) {
        if (keepMasters && (v == 11 || v == 22)) return v
        v = v.toString().map { it - '0' }.sum()
    }
    return v
}

/** Moolank (Driver) from birth day. */
fun moolank(day: Int): Int = reduceNumber(day)

/** Bhagyank (Life Path) from full DOB. */
fun bhagyank(day: Int, month: Int, year: Int): Int =
    reduceNumber(reduceNumber(day) + reduceNumber(month) + reduceNumber(year))

/** Success Number from day + month. */
fun successNumber(day: Int, month: Int): Int =
    reduceNumber(reduceNumber(day) + reduceNumber(month))

/** Karmic debt numbers present (13, 14, 16, 19) from raw components. */
fun karmicDebts(day: Int, month: Int, year: Int): List<Int> {
    val out = mutableListOf<Int>()
    if (day == 13 || day == 14 || day == 16 || day == 19) out.add(day)
    val full = day + month + year
    var v = full
    while (v > 9) {
        if (v == 13 || v == 14 || v == 16 || v == 19) { out.add(v); break }
        v = v.toString().map { it - '0' }.sum()
    }
    return out.distinct()
}

/** Lo Shu 3x3 grid counts from all DOB digits. Positions: 4,9,2 / 3,5,7 / 8,1,6. */
fun loShuGrid(day: Int, month: Int, year: Int): Map<Int, Int> {
    val digits = "$day$month$year".map { it - '0' }.filter { it in 1..9 }
    return (1..9).associateWith { n -> digits.count { it == n } }
}

/** Lo Shu arrows present in the grid. */
fun loShuArrows(grid: Map<Int, Int>): List<String> {
    val has = { n: Int -> (grid[n] ?: 0) > 0 }
    val arrows = mutableListOf<String>()
    if (has(4) && has(9) && has(2)) arrows.add("Arrow of Prosperity (4-9-2)")
    if (has(3) && has(5) && has(7)) arrows.add("Arrow of Spirituality (3-5-7)")
    if (has(8) && has(1) && has(6)) arrows.add("Arrow of Practicality (8-1-6)")
    if (has(4) && has(3) && has(8)) arrows.add("Arrow of the Planner (4-3-8)")
    if (has(9) && has(5) && has(1)) arrows.add("Arrow of Will (9-5-1)")
    if (has(2) && has(7) && has(6)) arrows.add("Arrow of Action (2-7-6)")
    if (has(4) && has(5) && has(6)) arrows.add("Arrow of Intellect (4-5-6)")
    if (has(2) && has(5) && has(8)) arrows.add("Arrow of Emotion (2-5-8)")
    return arrows
}

/** Personal Year for a calendar year. */
fun personalYear(day: Int, month: Int, year: Int): Int =
    reduceNumber(reduceNumber(day) + reduceNumber(month) + reduceNumber(year))

/** Personal Month. */
fun personalMonth(personalYear: Int, calendarMonth: Int): Int =
    reduceNumber(personalYear + calendarMonth)

/** Personal Day. */
fun personalDay(personalMonth: Int, calendarDay: Int): Int =
    reduceNumber(personalMonth + calendarDay)

/** Kua number. gender: "male" or "female". */
fun kuaNumber(year: Int, gender: String): Int {
    val sum = reduceNumber(year)
    val raw = if (gender.lowercase() == "male") 11 - sum else 4 + sum
    return reduceNumber(raw)
}

/** Kua group + lucky directions. */
fun kuaInfo(kua: Int): Pair<String, List<String>> {
    val east = setOf(1, 3, 4, 9)
    return if (kua in east) {
        "East Group" to listOf("South-East (Sheng Chi)", "East (Tian Yi)", "South (Yan Nian)", "North (Fu Wei)")
    } else {
        "West Group" to listOf("North-East (Sheng Chi)", "West (Tian Yi)", "North-West (Yan Nian)", "South-West (Fu Wei)")
    }
}

/** Lucky weekdays for a Moolank. */
fun luckyDays(moolank: Int): List<String> = when (moolank) {
    1 -> listOf("Sunday", "Monday")
    2 -> listOf("Monday", "Friday")
    3 -> listOf("Thursday", "Friday")
    4 -> listOf("Saturday", "Sunday")
    5 -> listOf("Wednesday", "Friday")
    6 -> listOf("Friday", "Monday")
    7 -> listOf("Monday", "Sunday")
    8 -> listOf("Saturday", "Friday")
    9 -> listOf("Tuesday", "Thursday")
    else -> listOf("Sunday")
}

/** Planet ruler of a number (Vedic). */
fun planetRuler(n: Int): String = when (reduceNumber(n)) {
    1 -> "Sun"
    2 -> "Moon"
    3 -> "Jupiter"
    4 -> "Rahu"
    5 -> "Mercury"
    6 -> "Venus"
    7 -> "Ketu"
    8 -> "Saturn"
    9 -> "Mars"
    11 -> "Moon (Master)"
    22 -> "Saturn (Master)"
    else -> "Sun"
}

/** Gem, color, deity and mantra remedy for a number's ruling planet. */
data class PlanetRemedy(val planet: String, val gem: String, val color: String, val day: String, val mantra: String)

fun planetRemedy(n: Int): PlanetRemedy = when (reduceNumber(n)) {
    1 -> PlanetRemedy("Sun", "Ruby (Manik)", "Copper / Gold", "Sunday", "Om Suryaya Namah")
    2 -> PlanetRemedy("Moon", "Pearl (Moti)", "White / Silver", "Monday", "Om Chandraya Namah")
    3 -> PlanetRemedy("Jupiter", "Yellow Sapphire (Pukhraj)", "Yellow", "Thursday", "Om Gurave Namah")
    4 -> PlanetRemedy("Rahu", "Hessonite (Gomed)", "Brown / Smoky", "Saturday", "Om Rahave Namah")
    5 -> PlanetRemedy("Mercury", "Emerald (Panna)", "Green", "Wednesday", "Om Budhaya Namah")
    6 -> PlanetRemedy("Venus", "Diamond (Heera) / Opal", "White / Pink", "Friday", "Om Shukraya Namah")
    7 -> PlanetRemedy("Ketu", "Cat's Eye (Lehsunia)", "Grey / Smoky", "Tuesday", "Om Ketave Namah")
    8 -> PlanetRemedy("Saturn", "Blue Sapphire (Neelam)", "Dark Blue / Black", "Saturday", "Om Shanicharaya Namah")
    else -> PlanetRemedy("Mars", "Red Coral (Moonga)", "Red", "Tuesday", "Om Mangalaya Namah")
}

/** Mobile/vehicle number compatibility: digit sum vs Moolank. */
fun numberCompatibility(number: String, moolank: Int): Pair<Int, String> {
    val digits = number.filter { it.isDigit() }
    if (digits.isEmpty()) return 0 to "Enter a valid number."
    val sum = reduceNumber(digits.map { it - '0' }.sum())
    val friendly = setOf(
        setOf(1, 1), setOf(2, 2), setOf(3, 3), setOf(5, 5), setOf(6, 6), setOf(9, 9),
        setOf(1, 9), setOf(2, 6), setOf(3, 5), setOf(3, 9), setOf(5, 6)
    )
    val verdict = when {
        sum == moolank -> "Excellent — resonates directly with your Moolank $moolank."
        setOf(sum, moolank) in friendly -> "Good — $sum is friendly with your Moolank $moolank."
        sum == 4 || sum == 8 -> "Caution — $sum carries heavy karmic energy for Moolank $moolank."
        else -> "Neutral — $sum neither supports nor harms Moolank $moolank."
    }
    return sum to verdict
}

/** Parse YYYY-MM-DD into triple, or null. */
fun parseDob(dob: String): Triple<Int, Int, Int>? {
    val m = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""").matchEntire(dob.trim()) ?: return null
    val (y, mo, d) = m.destructured
    return try {
        val cal = Calendar.getInstance()
        cal.isLenient = false
        cal.set(y.toInt(), mo.toInt() - 1, d.toInt())
        cal.time // forces validation
        Triple(d.toInt(), mo.toInt(), y.toInt())
    } catch (e: Exception) { null }
}

/** Today's calendar components. */
fun todayParts(): Triple<Int, Int, Int> {
    val c = Calendar.getInstance()
    return Triple(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
}
