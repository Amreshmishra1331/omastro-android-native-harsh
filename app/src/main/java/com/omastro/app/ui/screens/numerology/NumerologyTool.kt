package com.omastro.app.ui.screens.numerology

/**
 * The 12 numerology tools on omastroai.com, in website order.
 */
enum class NumerologyTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val glyph: String
) {
    MOOLANK("moolank", "Moolank & Life Path", "Driver · Life Path · Success", "1"),
    LOSHU("loshu", "Lo Shu Grid", "Vedic Magic Square + Arrows", "3x3"),
    NAME_NUM("name_num", "Name Numerology", "Expression · Soul Urge · Personality", "Aa"),
    TIME_CYCLES("time_cycles", "Time Cycles", "Personal Year · Month · Day", "Cal"),
    KARMIC("karmic", "Karmic Debt", "13·14·16·19 + Pinnacles", "K"),
    COMPAT("compat", "Compatibility", "Number Matching for Two", "2"),
    GEMS("gems", "Gems & Remedies", "Mantra · Gem · Donation · Color", "Gem"),
    LUCKY_CAL("lucky_cal", "Lucky Calendar", "Lucky Days · Monthly · Yearly", "Cal"),
    MOBILE_NUM("mobile_num", "Mobile / Lucky Number", "Mobile · Vehicle · Lucky Number Analysis", "#"),
    NAME_CORRECT("name_correct", "Name Correction", "Name Analysis, Suggestions & Correction", "Ab"),
    KUA("kua", "Kua Number", "Feng Shui · Ba Gua · Lucky Direction", "Kua"),
    PLANETARY("planetary", "Planetary Numbers", "9 Planets · Missing · Career · Remedy", "9");

    companion object {
        fun fromId(id: String): NumerologyTool =
            values().firstOrNull { it.id == id } ?: MOOLANK
    }
}
