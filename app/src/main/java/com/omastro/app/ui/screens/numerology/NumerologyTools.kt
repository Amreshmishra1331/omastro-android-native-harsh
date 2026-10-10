package com.omastro.app.ui.screens.numerology

import com.omastro.app.ui.i18n.W
import java.util.Calendar
import kotlin.math.abs

/**
 * Pure numerology computations (Vedic/Chaldean reductions, client-side).
 * Parity with the website engine (Cycle 9, 2026-10-10): Kua, Pinnacles and
 * Challenges now follow the web formulas exactly; where the server exposes a
 * per-tab analysis (`POST /numerology` with `tab=synthesis|kua|pinnacles|
 * mobile|namefix`), the tool ViewModel renders the server's `analysis_text`
 * as the authoritative section and these local functions are the honest
 * on-device fallback. No fabricated content — server text or real math only.
 *
 * Display strings are bilingual via [W] and follow the Settings language.
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

/**
 * Challenge numbers (Decoz method): |month-day|, |day-year|, |c1-c2|,
 * |month-year| — matching the website engine (Cycle 9, 2026-10-10), which
 * leaves the challenge DIFFERENCES UNREDUCED (0 is possible — the
 * "all-challenge"). Master numbers are NOT kept for challenges.
 */
fun challengeNumbers(day: Int, month: Int, year: Int): List<Int> {
    val c1 = abs(month - day)
    val c2 = abs(day - year)
    return listOf(c1, c2, abs(c1 - c2), abs(month - year))
}

/** Lo Shu 3x3 grid counts from all DOB digits. Positions: 4,9,2 / 3,5,7 / 8,1,6. */
fun loShuGrid(day: Int, month: Int, year: Int): Map<Int, Int> {
    val digits = "$day$month$year".map { it - '0' }.filter { it in 1..9 }
    return (1..9).associateWith { n -> digits.count { it == n } }
}

/** Lo Shu arrows of strength present in the grid (W string keys). */
fun loShuArrows(grid: Map<Int, Int>): List<String> {
    val has = { n: Int -> (grid[n] ?: 0) > 0 }
    val arrows = mutableListOf<String>()
    if (has(4) && has(9) && has(2)) arrows.add("num_arrow_1")
    if (has(3) && has(5) && has(7)) arrows.add("num_arrow_2")
    if (has(8) && has(1) && has(6)) arrows.add("num_arrow_3")
    if (has(4) && has(3) && has(8)) arrows.add("num_arrow_4")
    if (has(9) && has(5) && has(1)) arrows.add("num_arrow_5")
    if (has(2) && has(7) && has(6)) arrows.add("num_arrow_6")
    if (has(4) && has(5) && has(6)) arrows.add("num_arrow_7")
    if (has(2) && has(5) && has(8)) arrows.add("num_arrow_8")
    return arrows
}

/** Lo Shu arrows of weakness — lines fully ABSENT from the grid (W string keys). */
fun loShuWeakArrows(grid: Map<Int, Int>): List<String> {
    val missing = { n: Int -> (grid[n] ?: 0) == 0 }
    val arrows = mutableListOf<String>()
    if (missing(4) && missing(9) && missing(2)) arrows.add("num_arrow_weak_1")
    if (missing(3) && missing(5) && missing(7)) arrows.add("num_arrow_weak_2")
    if (missing(8) && missing(1) && missing(6)) arrows.add("num_arrow_weak_3")
    if (missing(4) && missing(3) && missing(8)) arrows.add("num_arrow_weak_4")
    if (missing(9) && missing(5) && missing(1)) arrows.add("num_arrow_weak_5")
    if (missing(2) && missing(7) && missing(6)) arrows.add("num_arrow_weak_6")
    if (missing(4) && missing(5) && missing(6)) arrows.add("num_arrow_weak_7")
    if (missing(2) && missing(5) && missing(8)) arrows.add("num_arrow_weak_8")
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

/**
 * Kua number, matching the website engine EXACTLY (Cycle 9, 2026-10-10).
 * gender: "male" or "female".
 *
 * Web formula (post-2000 rule, applied by the server to all years):
 * take the LAST TWO digits of the birth year, sum them, then
 * male: 10 − sum · female: sum + 9. Reduce a two-digit result to one digit
 * (a non-positive male result wraps by adding 9). Kua 5 does not exist in
 * Feng Shui: males convert to 2, females to 8.
 */
fun kuaNumber(year: Int, gender: String): Int {
    val isMale = gender.lowercase() == "male"
    val lastTwo = abs(year) % 100
    val sum = lastTwo / 10 + lastTwo % 10
    var k = if (isMale) 10 - sum else sum + 9
    while (k > 9) k = k.toString().map { it - '0' }.sum()
    while (k <= 0) k += 9
    return when {
        k == 5 && isMale -> 2
        k == 5 -> 8
        else -> k
    }
}

/** Last-two-digit sum used by the web Kua formula (for the "show your work" line). */
fun kuaYearDigitSum(year: Int): Int {
    val lastTwo = abs(year) % 100
    return lastTwo / 10 + lastTwo % 10
}

/** Reduce to a single digit, preserving master numbers 11, 22 AND 33. */
fun reduceFinal(n: Int): Int {
    var v = n
    while (v > 9) {
        if (v == 11 || v == 22 || v == 33) return v
        v = v.toString().map { it - '0' }.sum()
    }
    return v
}

/**
 * The four Pinnacles, matching the website engine (Cycle 9, 2026-10-10):
 * day/month/year components are flattened FIRST (no masters kept), then
 * each pinnacle sum keeps master numbers 11/22/33 only on the final sums.
 * 1st = month+day · 2nd = day+year · 3rd = 1st+2nd · 4th = month+year.
 */
fun pinnacleNumbers(day: Int, month: Int, year: Int): List<Int> {
    val d = reduceNumber(day, keepMasters = false)
    val m = reduceNumber(month, keepMasters = false)
    val y = reduceNumber(year, keepMasters = false)
    val p1 = reduceFinal(d + m)
    val p2 = reduceFinal(d + y)
    val p3 = reduceFinal(p1 + p2)
    val p4 = reduceFinal(m + y)
    return listOf(p1, p2, p3, p4)
}

/**
 * Kua group + lucky directions. Returns the group W-key and a list of
 * (direction W-key, classical epithet) pairs. Epithets (Sheng Chi, Tian Yi,
 * Yan Nian, Fu Wei) are proper nouns and stay untranslated.
 */
fun kuaInfo(kua: Int): Pair<String, List<Pair<String, String>>> {
    val east = setOf(1, 3, 4, 9)
    return if (kua in east) {
        "num_kua_group_east" to listOf(
            "num_dir_se" to "Sheng Chi",
            "num_dir_e" to "Tian Yi",
            "num_dir_s" to "Yan Nian",
            "num_dir_n" to "Fu Wei"
        )
    } else {
        "num_kua_group_west" to listOf(
            "num_dir_ne" to "Sheng Chi",
            "num_dir_w" to "Tian Yi",
            "num_dir_nw" to "Yan Nian",
            "num_dir_sw" to "Fu Wei"
        )
    }
}

/** Kua element (Five-Element association). Returns a W-key. */
fun kuaElement(kua: Int): String = when (kua) {
    1 -> "num_elem_water"
    2, 8 -> "num_elem_earth"
    3, 4 -> "num_elem_wood"
    6, 7 -> "num_elem_metal"
    else -> "num_elem_fire"
}

/** Lucky weekdays for a Moolank. */
fun luckyDays(moolank: Int): List<String> = when (reduceNumber(moolank)) {
    1 -> listOf("num_day_sunday", "num_day_monday")
    2 -> listOf("num_day_monday", "num_day_friday")
    3 -> listOf("num_day_thursday", "num_day_friday")
    4 -> listOf("num_day_saturday", "num_day_sunday")
    5 -> listOf("num_day_wednesday", "num_day_friday")
    6 -> listOf("num_day_friday", "num_day_monday")
    7 -> listOf("num_day_monday", "num_day_sunday")
    8 -> listOf("num_day_saturday", "num_day_friday")
    9 -> listOf("num_day_tuesday", "num_day_thursday")
    else -> listOf("num_day_sunday")
}.map { W.t(it) }

/** Dates in [year]/[month] whose digit-sum reduces to [moolank]. */
fun luckyDatesInMonth(moolank: Int, year: Int, month: Int): List<Int> {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (1..max).filter { reduceNumber(it) == moolank }
}

/** Planet ruler of a number (Vedic), bilingual. */
fun planetRuler(n: Int): String = W.t(
    when (reduceNumber(n)) {
        1 -> "num_planet_1"
        2 -> "num_planet_2"
        3 -> "num_planet_3"
        4 -> "num_planet_4"
        5 -> "num_planet_5"
        6 -> "num_planet_6"
        7 -> "num_planet_7"
        8 -> "num_planet_8"
        9 -> "num_planet_9"
        11 -> "num_planet_11"
        22 -> "num_planet_22"
        else -> "num_planet_1"
    }
)

/** Gem, color, metal, finger, weekday and mantra remedy for a number's ruling planet. */
data class PlanetRemedy(
    val planet: String,
    val gem: String,
    val color: String,
    val mantra: String,
    val metalKey: String,
    val fingerKey: String,
    val dayKey: String
)

fun planetRemedy(n: Int): PlanetRemedy = when (reduceNumber(n)) {
    1 -> PlanetRemedy(planetRuler(1), "Ruby (Manik)", "Copper / Gold", "Om Suryaya Namah", "num_metal_gold", "num_finger_ring", "num_day_sunday")
    2 -> PlanetRemedy(planetRuler(2), "Pearl (Moti)", "White / Silver", "Om Chandraya Namah", "num_metal_silver", "num_finger_little", "num_day_monday")
    3 -> PlanetRemedy(planetRuler(3), "Yellow Sapphire (Pukhraj)", "Yellow", "Om Gurave Namah", "num_metal_gold", "num_finger_index", "num_day_thursday")
    4 -> PlanetRemedy(planetRuler(4), "Hessonite (Gomed)", "Brown / Smoky", "Om Rahave Namah", "num_metal_silver", "num_finger_middle", "num_day_saturday")
    5 -> PlanetRemedy(planetRuler(5), "Emerald (Panna)", "Green", "Om Budhaya Namah", "num_metal_gold", "num_finger_little", "num_day_wednesday")
    6 -> PlanetRemedy(planetRuler(6), "Diamond (Heera) / Opal", "White / Pink", "Om Shukraya Namah", "num_metal_platinum", "num_finger_ring", "num_day_friday")
    7 -> PlanetRemedy(planetRuler(7), "Cat's Eye (Lehsunia)", "Grey / Smoky", "Om Ketave Namah", "num_metal_silver", "num_finger_middle", "num_day_tuesday")
    8 -> PlanetRemedy(planetRuler(8), "Blue Sapphire (Neelam)", "Dark Blue / Black", "Om Shanicharaya Namah", "num_metal_iron", "num_finger_middle", "num_day_saturday")
    else -> PlanetRemedy(planetRuler(9), "Red Coral (Moonga)", "Red", "Om Mangalaya Namah", "num_metal_copper", "num_finger_ring", "num_day_tuesday")
}

/** Compatibility tier of a number pair (symmetric). */
enum class PairTier { EXCELLENT, GOOD, NEUTRAL, CAUTION }

private val friendlyPairs = setOf(
    setOf(1, 1), setOf(2, 2), setOf(3, 3), setOf(5, 5), setOf(6, 6), setOf(9, 9),
    setOf(1, 9), setOf(2, 6), setOf(3, 5), setOf(3, 9), setOf(5, 6)
)

fun numberPairTier(a: Int, b: Int): PairTier {
    val x = reduceNumber(a)
    val y = reduceNumber(b)
    return when {
        x == y -> PairTier.EXCELLENT
        setOf(x, y) in friendlyPairs -> PairTier.GOOD
        x == 4 || x == 8 || y == 4 || y == 8 -> PairTier.CAUTION
        else -> PairTier.NEUTRAL
    }
}

/** W-key for a [PairTier]. */
fun pairTierKey(tier: PairTier): String = when (tier) {
    PairTier.EXCELLENT -> "num_tier_excellent"
    PairTier.GOOD -> "num_tier_good"
    PairTier.NEUTRAL -> "num_tier_neutral"
    PairTier.CAUTION -> "num_tier_caution"
}

/** W-key for a [PairTier]'s one-line explanation. */
fun pairTierDescKey(tier: PairTier): String = when (tier) {
    PairTier.EXCELLENT -> "num_tier_excellent_d"
    PairTier.GOOD -> "num_tier_good_d"
    PairTier.NEUTRAL -> "num_tier_neutral_d"
    PairTier.CAUTION -> "num_tier_caution_d"
}

/**
 * Mobile/vehicle number analysis: digit sum vs Moolank.
 * Returns (digitSum, tier). The caller builds the verdict text (bilingual).
 */
fun numberCompatibility(number: String, moolank: Int): Pair<Int, PairTier> {
    val digits = number.filter { it.isDigit() }
    if (digits.isEmpty()) return 0 to PairTier.NEUTRAL
    val sum = reduceNumber(digits.map { it - '0' }.sum())
    return sum to numberPairTier(sum, moolank)
}

// ---------- Name numerology (deterministic, on-device) ----------

/** Pythagorean letter value: A=1..I=9, J=1..R=9, S=1..Z=8. */
fun pythagoreanValue(c: Char): Int {
    val u = c.uppercaseChar()
    return when (u) {
        in 'A'..'I' -> u - 'A' + 1
        in 'J'..'R' -> u - 'J' + 1
        in 'S'..'Z' -> u - 'S' + 1
        else -> 0
    }
}

/** Chaldean letter value (9 is sacred — no letter carries it). */
fun chaldeanValue(c: Char): Int = when (c.uppercaseChar()) {
    'A', 'I', 'J', 'Q', 'Y' -> 1
    'B', 'K', 'R' -> 2
    'C', 'G', 'L', 'S' -> 3
    'D', 'M', 'T' -> 4
    'E', 'H', 'N', 'X' -> 5
    'U', 'V', 'W' -> 6
    'O', 'Z' -> 7
    'F', 'P' -> 8
    else -> 0
}

private val NAME_VOWELS = setOf('A', 'E', 'I', 'O', 'U')

data class NameNumbers(
    val expression: Int,
    val soulUrge: Int,
    val personality: Int,
    val chaldean: Int
)

/**
 * Name numbers from a Latin-script name. Vowels = A E I O U (Y counts as a
 * consonant — the simplified convention most calculators use). Master
 * numbers 11/22 are preserved.
 */
fun nameNumbers(name: String): NameNumbers {
    val letters = name.uppercase().filter { it in 'A'..'Z' }
    val vowels = letters.filter { it in NAME_VOWELS }
    val consonants = letters.filter { it !in NAME_VOWELS }
    return NameNumbers(
        expression = reduceNumber(letters.sumOf { pythagoreanValue(it) }),
        soulUrge = reduceNumber(vowels.sumOf { pythagoreanValue(it) }),
        personality = reduceNumber(consonants.sumOf { pythagoreanValue(it) }),
        chaldean = reduceNumber(letters.sumOf { chaldeanValue(it) })
    )
}

/** W-key for a core-number trait line (1-9, 11, 22). */
fun traitKey(n: Int): String = "num_trait_${reduceNumber(n)}"

/** W-key for a personal cycle (year/month/day) meaning 1-9. */
fun cycleMeaningKey(n: Int): String = "num_py_${reduceNumber(n, keepMasters = false).coerceIn(1, 9)}"

/** W-key for a Lo Shu number meaning 1-9. */
fun loShuMeaningKey(n: Int): String = "num_loshu_$n"

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
