package com.omastro.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App UI language preference ("en" | "hi").
 *
 * Hindi covers navigation & drawer menus ([stringsFor]) plus the world-class
 * feature set (voice typing, voice mode, photo AI, Palmistry, Vastu) via
 * [com.omastro.app.ui.i18n.W], which reads this same preference. All new
 * screens auto-switch with the Settings language — no per-screen toggles.
 */
object LangPrefs {
    private const val PREFS = "omastro_lang_prefs"
    private const val KEY_LANG = "app_language"

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            _language.value = prefs!!.getString(KEY_LANG, "en") ?: "en"
        }
    }

    fun setLanguage(lang: String) {
        val l = if (lang == "hi") "hi" else "en"
        _language.value = l
        prefs?.edit()?.putString(KEY_LANG, l)?.apply()
    }

    /**
     * Real Hindi strings for navigation & drawer menus.
     * Key = English label, value = Hindi label.
     */
    private val HI = mapOf(
        // Bottom nav
        "Home" to "होम",
        "Kundali" to "कुंडली",
        "AI Chat" to "AI चैट",
        "Rashifal" to "राशिफल",
        "Astrologers" to "ज्योतिषी",
        "Settings" to "सेटिंग्स",
        // Drawer — Menu
        "Dashboard" to "डैशबोर्ड",
        "Divine Chat" to "दिव्य चैट",
        "Janam Kundali" to "जन्म कुंडली",
        "Panchang" to "पंचांग",
        "Kundali Milan" to "कुंडली मिलान",
        "Numerology" to "अंक ज्योतिष",
        "Gochar" to "गोचर",
        "Muhurat" to "मुहूर्त",
        "Palmistry" to "हस्तरेखा",
        "Face Reading" to "चेहरा विश्लेषण",
        "Vastu Shastra" to "वास्तु शास्त्र",
        "Astro Health" to "ज्योतिषीय स्वास्थ्य",
        // Drawer — Account
        "Wallet" to "वॉलेट",
        "Add Money" to "पैसे जोड़ें",
        "Saved Kundalis" to "सहेजी कुंडलियां",
        "AI Chat History" to "AI चैट इतिहास",
        "Activity History" to "गतिविधि इतिहास",
        "Refer & Earn" to "रेफर करें और कमाएं",
        "Rate this App" to "ऐप को रेट करें",
        // Drawer — Preferences / Legal
        "Language" to "भाषा",
        "Privacy Policy" to "गोपनीयता नीति",
        "Terms of Service" to "सेवा की शर्तें",
        "Report a Bug" to "बग रिपोर्ट करें",
        "Log Out" to "लॉग आउट",
        // Dashboard quick actions (2026-10-10 navigation audit: bilingual quick-action cards)
        "Activity" to "गतिविधि",
        "Edit Profile" to "प्रोफ़ाइल संपादित करें",
        "Change Password" to "पासवर्ड बदलें",
        // Drawer subtitles (Menu)
        "General · Mundane" to "सामान्य · मुंडेन",
        "Spiritual guidance" to "आध्यात्मिक मार्गदर्शन",
        "AI palm reading" to "AI हस्तरेखा विश्लेषण",
        "AI face reading" to "AI चेहरा विश्लेषण",
        "Home-energy analysis" to "गृह-ऊर्जा विश्लेषण",
        "Health astrology" to "ज्योतिषीय स्वास्थ्य विश्लेषण",
        "Planetary transits" to "ग्रह गोचर",
        "Auspicious timings" to "शुभ समय",
        // Drawer subtitles (Account)
        "Balance · Add money · Transactions" to "बैलेंस · पैसे जोड़ें · लेन-देन",
        "Saved conversations" to "सहेजी बातचीत",
        "Coming soon" to "जल्द आ रहा है",
        "English · हिन्दी" to "English · हिन्दी",
        "Email support" to "ईमेल सहायता"
    )

    /** Returns the display label for a navigation/menu title in the active language. */
    fun label(en: String): String =
        if (_language.value == "hi") HI[en] ?: en else en
}
