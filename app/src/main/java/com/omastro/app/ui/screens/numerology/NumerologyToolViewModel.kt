package com.omastro.app.ui.screens.numerology

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface ToolUiState {
    data object Idle : ToolUiState
    data object Loading : ToolUiState
    data class Success(val result: ToolResult) : ToolUiState
    data class Error(val message: String) : ToolUiState
}

/** Structured result per tool. */
data class ToolResult(
    val title: String,
    val sections: List<ResultSection>
)

data class ResultSection(
    val heading: String,
    val lines: List<ResultLine>
)

data class ResultLine(
    val label: String,
    val value: String,
    val highlight: Boolean = false
)

class NumerologyToolViewModel(val toolId: String) : ViewModel() {

    private val repo = AppContainer.astroRepository
    private val tool = NumerologyTool.fromId(toolId)

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    private val _dob = MutableStateFlow("")
    val dob: StateFlow<String> = _dob.asStateFlow()
    private val _name2 = MutableStateFlow("")
    val name2: StateFlow<String> = _name2.asStateFlow()
    private val _dob2 = MutableStateFlow("")
    val dob2: StateFlow<String> = _dob2.asStateFlow()
    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()
    private val _gender = MutableStateFlow("male")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _uiState = MutableStateFlow<ToolUiState>(ToolUiState.Idle)
    val uiState: StateFlow<ToolUiState> = _uiState.asStateFlow()
    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    private val isHi: Boolean get() = LangPrefs.language.value == "hi"

    fun setName(v: String) { _name.value = v; _formError.value = null }
    fun setDob(v: String) { _dob.value = v; _formError.value = null }
    fun setName2(v: String) { _name2.value = v; _formError.value = null }
    fun setDob2(v: String) { _dob2.value = v; _formError.value = null }
    fun setPhone(v: String) { _phone.value = v; _formError.value = null }
    fun setGender(v: String) { _gender.value = v }

    /**
     * Only tools that actually USE the name may demand it. Moolank, Karmic
     * and Planetary are DOB-only computations — requiring a name there was
     * dishonest friction (fixed 2026-10-10).
     */
    fun needsName(): Boolean = tool in setOf(
        NumerologyTool.NAME_NUM, NumerologyTool.COMPAT, NumerologyTool.NAME_CORRECT
    )
    // Every tool needs DOB — including MOBILE_NUM, whose verdict is compared
    // against the user's real Moolank (previously hardcoded Triple(1,1,2000)).
    fun needsDob(): Boolean = true
    fun needsSecondPerson(): Boolean = tool == NumerologyTool.COMPAT
    fun needsPhone(): Boolean = tool == NumerologyTool.MOBILE_NUM
    fun needsGender(): Boolean = tool == NumerologyTool.KUA

    fun calculate() {
        _formError.value = null
        if (needsName()) {
            if (_name.value.trim().length < 2) {
                _formError.value = W.t("num_err_name")
                return
            }
            if (_name.value.none { it.isLetter() }) {
                _formError.value = W.t("num_err_name_letters")
                return
            }
        }
        val dobTriple = parseDob(_dob.value)
        if (dobTriple == null) {
            _formError.value = W.t("num_err_dob")
            return
        }
        if (needsSecondPerson()) {
            if (_name2.value.trim().length < 2) { _formError.value = W.t("num_err_name2"); return }
            if (_name2.value.none { it.isLetter() }) { _formError.value = W.t("num_err_name_letters"); return }
            if (parseDob(_dob2.value) == null) { _formError.value = W.t("num_err_dob2"); return }
        }
        if (needsPhone() && _phone.value.filter { it.isDigit() }.length < 6) {
            _formError.value = W.t("num_err_phone")
            return
        }
        viewModelScope.launch {
            _uiState.value = ToolUiState.Loading
            try {
                val result = compute(dobTriple)
                _uiState.value = ToolUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = ToolUiState.Error(W.t("num_err_generic"))
            }
        }
    }

    /** Re-runs the last calculation (e.g. after a Settings language change). */
    fun recalculate() {
        if (_uiState.value is ToolUiState.Success) calculate()
    }

    private suspend fun compute(dob: Triple<Int, Int, Int>): ToolResult {
        val (day, month, year) = dob
        return when (tool) {
            NumerologyTool.MOOLANK -> moolankResult(day, month, year)
            NumerologyTool.LOSHU -> loshuResult(day, month, year)
            NumerologyTool.NAME_NUM -> nameNumResult()
            NumerologyTool.TIME_CYCLES -> timeCyclesResult(day, month)
            NumerologyTool.KARMIC -> karmicResult(day, month, year)
            NumerologyTool.COMPAT -> compatResult()
            NumerologyTool.GEMS -> gemsResult(day)
            NumerologyTool.LUCKY_CAL -> luckyCalResult(day, month)
            NumerologyTool.MOBILE_NUM -> mobileResult(day)
            NumerologyTool.NAME_CORRECT -> nameCorrectResult()
            NumerologyTool.KUA -> kuaResult(year)
            NumerologyTool.PLANETARY -> planetaryResult(day, month, year)
        }
    }

    // ---------- Server data (POST /numerology) ----------
    // The server computes coreNumbers, personalYear/Month/Day, lifeCycles,
    // pinnacles, challenges, nameAnalysis, loshuGrid, luckyNumbers/Days/Colors.
    // Tools use server values where they map 1:1, with local fallback on failure.
    // A null return is surfaced honestly ("server unavailable" note) — never
    // silently papered over.
    private suspend fun fetchServerNumerology(): Map<String, Any>? {
        return try {
            val body = repo.getNumerology(mapOf(
                "dob" to _dob.value.trim(),
                "name" to _name.value.trim(),
                "language" to LangPrefs.language.value
            )).getOrNull()
            @Suppress("UNCHECKED_CAST")
            body?.get("data") as? Map<String, Any>
        } catch (e: Exception) { null }
    }

    /** Honest "server unavailable" section appended when the server fetch failed. */
    private fun serverNoteSection(server: Map<String, Any>?): ResultSection? =
        if (server == null) ResultSection(
            W.t("num_sec_note"),
            listOf(ResultLine("", W.t("num_server_note")))
        ) else null

    private fun numEntry(map: Map<String, Any>?, key: String): String {
        val e = map?.get(key) as? Map<*, *> ?: return "—"
        val v = e["value"] ?: e["number"] ?: "—"
        val meaning = (e["meaning"] as? String).orEmpty()
        return if (meaning.isBlank()) "$v" else "$v — $meaning"
    }

    // ---------- Tool computations ----------

    private suspend fun moolankResult(day: Int, month: Int, year: Int): ToolResult {
        val m = moolank(day); val b = bhagyank(day, month, year); val s = successNumber(day, month)
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_core"), listOf(
                ResultLine(W.t("num_lbl_moolank_driver"), "$m — ${planetRuler(m)}", true),
                ResultLine(W.t("num_lbl_bhagyank"), "$b — ${planetRuler(b)}", true),
                ResultLine(W.t("num_lbl_success"), "$s — ${planetRuler(s)}", true)
            )),
            ResultSection(W.t("num_sec_traits"), listOf(
                ResultLine("$m — ${planetRuler(m)}", W.t(traitKey(m))),
                ResultLine("$b — ${planetRuler(b)}", W.t(traitKey(b)))
            ))
        )
        // Server core numbers (authoritative) as an extra section.
        val server = fetchServerNumerology()
        (server?.get("coreNumbers") as? Map<String, Any>)?.let { core ->
            sections.add(ResultSection(W.t("num_sec_server_core"), listOf(
                ResultLine(W.t("num_lbl_life_path"), numEntry(core, "lifePathNumber"), true),
                ResultLine(W.t("num_lbl_expression"), numEntry(core, "expressionNumber")),
                ResultLine(W.t("num_lbl_soul_urge"), numEntry(core, "soulUrgeNumber")),
                ResultLine(W.t("num_lbl_personality"), numEntry(core, "personalityNumber")),
                ResultLine(W.t("num_lbl_birthday"), numEntry(core, "birthdayNumber")),
                ResultLine(W.t("num_lbl_destiny"), numEntry(core, "destinyNumber"))
            )))
        }
        sections.add(ResultSection(W.t("num_sec_derived"), listOf(
            ResultLine(
                W.t("num_lbl_moolank_driver"),
                "${W.t("num_from_birth_day")} $day → $m. ${W.t("num_deriv_moolank_desc")}"
            ),
            ResultLine(
                W.t("num_lbl_bhagyank"),
                "${W.t("num_day_month_year")} → $b. ${W.t("num_deriv_bhagyank_desc")}"
            ),
            ResultLine(
                W.t("num_lbl_success"),
                "${W.t("num_day_month")} → $s. ${W.t("num_deriv_success_desc")}"
            )
        )))
        serverNoteSection(server)?.let { sections.add(it) }
        return ToolResult(W.t("num_tool_moolank_title"), sections)
    }

    private suspend fun loshuResult(day: Int, month: Int, year: Int): ToolResult {
        val sections = mutableListOf<ResultSection>()
        val server = fetchServerNumerology()
        @Suppress("UNCHECKED_CAST")
        val sGrid = server?.get("loshuGrid") as? Map<String, Any>
        if (sGrid != null) {
            val present = (sGrid["presentNumbers"] as? List<*>)?.joinToString(", ") ?: "—"
            val absent = (sGrid["absentNumbers"] as? List<*>)?.joinToString(", ") ?: "—"
            val arrows = (sGrid["arrows"] as? Map<String, Any>)
                ?.filter { (_, v) -> v == true }?.keys?.joinToString(", ") ?: ""
            sections.add(ResultSection(W.t("num_sec_grid_server"), listOf(
                ResultLine(W.t("num_lbl_present_numbers"), present, true),
                ResultLine(W.t("num_lbl_absent_numbers"), absent, true),
                ResultLine(
                    W.t("num_sec_arrows"),
                    arrows.ifBlank { W.t("num_no_arrows") },
                    true
                )
            )))
        }
        // Local computation — always shown (it is real math from the DOB).
        val grid = loShuGrid(day, month, year)
        val rows = listOf(listOf(4, 9, 2), listOf(3, 5, 7), listOf(8, 1, 6))
        val gridLines = rows.map { row ->
            ResultLine("", row.joinToString("    ") { n -> "$n:${grid[n]}" })
        } + ResultLine("", W.t("num_grid_legend"))
        sections.add(ResultSection(W.t("num_sec_grid_local"), gridLines))
        val arrows = loShuArrows(grid)
        sections.add(ResultSection(
            W.t("num_sec_arrows"),
            if (arrows.isEmpty()) listOf(ResultLine(W.t("num_lbl_arrows"), W.t("num_no_arrows")))
            else arrows.map { ResultLine(W.t("num_lbl_present"), W.t(it), true) }
        ))
        val weak = loShuWeakArrows(grid)
        sections.add(ResultSection(
            W.t("num_sec_weak_arrows"),
            if (weak.isEmpty()) listOf(ResultLine(W.t("num_lbl_absent"), W.t("num_no_weak_arrows")))
            else weak.map { ResultLine(W.t("num_lbl_absent"), W.t(it), true) }
        ))
        sections.add(ResultSection(
            W.t("num_sec_missing"),
            listOf(ResultLine(
                W.t("num_lbl_absent_numbers"),
                (1..9).filter { (grid[it] ?: 0) == 0 }.joinToString(", ").ifEmpty { W.t("num_all_present") }
            ))
        ))
        sections.add(ResultSection(
            W.t("num_sec_number_meanings"),
            (1..9).map { n -> ResultLine("$n — ${planetRuler(n)}", W.t(loShuMeaningKey(n))) }
        ))
        serverNoteSection(server)?.let { sections.add(it) }
        return ToolResult(W.t("num_tool_loshu_title"), sections)
    }

    /**
     * Name numerology — computed ON-DEVICE with real Pythagorean/Chaldean
     * math (previously 100% server-dependent: dead whenever the server was
     * unreachable). Server analysis text is appended as an enhancement when
     * available, never required.
     */
    private suspend fun nameNumResult(): ToolResult {
        val nn = nameNumbers(_name.value.trim())
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_name_numbers"), listOf(
                ResultLine(W.t("num_lbl_expression"), "${nn.expression} — ${planetRuler(nn.expression)}", true),
                ResultLine(W.t("num_lbl_soul_urge"), "${nn.soulUrge} — ${planetRuler(nn.soulUrge)}"),
                ResultLine(W.t("num_lbl_personality"), "${nn.personality} — ${planetRuler(nn.personality)}"),
                ResultLine(W.t("num_lbl_chaldean"), "${nn.chaldean} — ${planetRuler(nn.chaldean)}")
            )),
            ResultSection(W.t("num_sec_traits"), listOf(
                ResultLine(
                    "${W.t("num_lbl_expression")} ${nn.expression}",
                    W.t(traitKey(nn.expression))
                )
            ))
        )
        appendServerNameAnalysis(sections)
        return ToolResult(W.t("num_tool_name_num_title"), sections)
    }

    /** Appends the server's name-check analysis text when available, else an honest note. */
    private suspend fun appendServerNameAnalysis(sections: MutableList<ResultSection>) {
        val text: String? = try {
            val body = repo.getNumerologyNameCheck(
                mapOf("name" to _name.value.trim(), "dob" to _dob.value.trim())
            ).getOrNull()
            @Suppress("UNCHECKED_CAST")
            val data = body?.get("data") as? Map<*, *>
            (data?.get("analysis_text") as? String)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
        if (text != null) {
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        } else {
            sections.add(ResultSection(
                W.t("num_sec_note"),
                listOf(ResultLine("", W.t("num_server_analysis_unavailable")))
            ))
        }
    }

    private suspend fun timeCyclesResult(day: Int, month: Int): ToolResult {
        val (td, tm, ty) = todayParts()
        val server = fetchServerNumerology()
        val sPy = (server?.get("personalYear") as? Map<*, *>)
        val sPm = (server?.get("personalMonth") as? Map<*, *>)
        val sPd = (server?.get("personalDay") as? Map<*, *>)
        val py = personalYear(day, month, ty)
        val pyLine = if (sPy != null)
            ResultLine(
                "${W.t("num_lbl_personal_year")} ($ty)",
                "${sPy["current"]} — ${(sPy["meaning"] as? String).orEmpty()}",
                true
            )
        else
            ResultLine(
                "${W.t("num_lbl_personal_year")} ($ty)",
                "$py — ${planetRuler(py)}: ${W.t(cycleMeaningKey(py))}",
                true
            )
        val pmLine = if (sPm != null)
            ResultLine(
                W.t("num_lbl_personal_month"),
                "${sPm["value"]} — ${(sPm["meaning"] as? String).orEmpty()}",
                true
            )
        else {
            val pm = personalMonth(py, tm)
            ResultLine(
                W.t("num_lbl_personal_month"),
                "$pm — ${planetRuler(pm)}: ${W.t(cycleMeaningKey(pm))}",
                true
            )
        }
        val pdLine = if (sPd != null)
            ResultLine(
                "${W.t("num_lbl_personal_day")}",
                "${sPd["value"]} — ${(sPd["meaning"] as? String).orEmpty()}",
                true
            )
        else {
            val pm = personalMonth(py, tm); val pd = personalDay(pm, td)
            ResultLine(
                W.t("num_lbl_personal_day"),
                "$pd — ${planetRuler(pd)}: ${W.t(cycleMeaningKey(pd))}",
                true
            )
        }
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_cycles"), listOf(pyLine, pmLine, pdLine)),
            ResultSection(W.t("num_sec_next12"), (1..12).map { m ->
                val v = personalMonth(py, m)
                ResultLine(
                    W.t("num_mon_$m"),
                    "$v — ${planetRuler(v)}: ${W.t(cycleMeaningKey(v))}"
                )
            })
        )
        serverNoteSection(server)?.let { sections.add(it) }
        return ToolResult(W.t("num_tool_time_cycles_title"), sections)
    }

    private suspend fun karmicResult(day: Int, month: Int, year: Int): ToolResult {
        val debts = karmicDebts(day, month, year)
        val meanings = mapOf(
            13 to W.t("num_karmic_13"),
            14 to W.t("num_karmic_14"),
            16 to W.t("num_karmic_16"),
            19 to W.t("num_karmic_19")
        )
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_karmic"), listOf(
                *(if (debts.isEmpty()) arrayOf(ResultLine(W.t("num_lbl_debts"), W.t("num_karmic_none"), true))
                else debts.map { ResultLine("$it", meanings[it] ?: "", true) }.toTypedArray())
            ))
        )
        // Server karma numbers + challenges when available.
        val server = fetchServerNumerology()
        @Suppress("UNCHECKED_CAST")
        val nameAnalysis = server?.get("nameAnalysis") as? Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val karmaNumbers = nameAnalysis?.get("karmaNumbers") as? List<*>
        @Suppress("UNCHECKED_CAST")
        val challenges = server?.get("challenges") as? List<Map<String, Any>>
        if (!karmaNumbers.isNullOrEmpty() || !challenges.isNullOrEmpty()) {
            val lines = mutableListOf<ResultLine>()
            karmaNumbers?.forEach { k -> lines.add(ResultLine(W.t("num_lbl_debts"), "$k", true)) }
            challenges?.forEach { c ->
                lines.add(ResultLine(
                    "${W.t("num_lbl_challenge")} ${c["number"]}",
                    "${c["theme"]} (${c["years"]})"
                ))
            }
            sections.add(ResultSection(W.t("num_sec_server_karmic"), lines))
        }
        val p1 = reduceNumber(month + day)
        val p2 = reduceNumber(day + year)
        sections.add(ResultSection(W.t("num_sec_pinnacles"), listOf(
            ResultLine(W.t("num_pinnacle_1"), "$p1 — ${planetRuler(p1)}"),
            ResultLine(W.t("num_pinnacle_2"), "$p2 — ${planetRuler(p2)}"),
            ResultLine(W.t("num_pinnacle_3"), "${reduceNumber(p1 + p2)} — ${planetRuler(reduceNumber(p1 + p2))}"),
            ResultLine(W.t("num_pinnacle_4"), "${reduceNumber(month + year)} — ${planetRuler(reduceNumber(month + year))}")
        )))
        val ch = challengeNumbers(day, month, year)
        val chLabels = listOf("num_challenge_first", "num_challenge_second", "num_challenge_third", "num_challenge_main")
        sections.add(ResultSection(W.t("num_sec_challenges"), ch.mapIndexed { i, v ->
            ResultLine(W.t(chLabels[i]), "$v — ${W.t("num_challenge_$v")}", true)
        }))
        serverNoteSection(server)?.let { sections.add(it) }
        return ToolResult(W.t("num_tool_karmic_title"), sections)
    }

    /**
     * Compatibility — computed ON-DEVICE from both birth dates and names
     * (previously 100% server-dependent). Three deterministic axes
     * (Moolank, Bhagyank, Expression) scored from the shared number-pair
     * tier table; the server score/analysis is appended when available.
     */
    private suspend fun compatResult(): ToolResult {
        val (d1, m1, y1) = parseDob(_dob.value)!!
        val (d2, m2, y2) = parseDob(_dob2.value)!!
        val axes = listOf(
            Triple(W.t("num_lbl_axis_moolank"), moolank(d1), moolank(d2)),
            Triple(W.t("num_lbl_axis_bhagyank"), bhagyank(d1, m1, y1), bhagyank(d2, m2, y2)),
            Triple(W.t("num_lbl_axis_expression"), nameNumbers(_name.value).expression, nameNumbers(_name2.value).expression)
        )
        fun axisScore(t: PairTier) = when (t) {
            PairTier.EXCELLENT -> 50
            PairTier.GOOD -> 38
            PairTier.NEUTRAL -> 28
            PairTier.CAUTION -> 12
        }
        val tiers = axes.map { (_, a, b) -> numberPairTier(a, b) }
        val score = (tiers.sumOf { axisScore(it) } / 3.0 * 2).roundToInt().coerceIn(0, 100)
        val verdictKey = when {
            score >= 80 -> "num_compat_v_high"
            score >= 60 -> "num_compat_v_good"
            score >= 40 -> "num_compat_v_avg"
            else -> "num_compat_v_low"
        }
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_local_score"), listOf(
                ResultLine(W.t("num_lbl_match"), "$score / 100", true),
                ResultLine(W.t("num_lbl_verdict"), W.t(verdictKey), true)
            )),
            ResultSection(W.t("num_sec_breakdown"), axes.mapIndexed { i, (label, a, b) ->
                val t = tiers[i]
                ResultLine(label, "$a ${W.t("num_vs")} $b — ${W.t(pairTierKey(t))}: ${W.t(pairTierDescKey(t))}")
            })
        )
        // Server score + analysis when available.
        val serverText: String? = try {
            val body = repo.getNumerologyCompatibility(mapOf(
                "name1" to _name.value.trim(), "dob1" to _dob.value.trim(),
                "name2" to _name2.value.trim(), "dob2" to _dob2.value.trim()
            )).getOrNull()
            @Suppress("UNCHECKED_CAST")
            val data = body?.get("data") as? Map<*, *>
            val scoreLine = (data?.get("score") as? Number)?.let { "${it.toInt()} / 100" }
            val text = (data?.get("analysis_text") as? String)?.takeIf { it.isNotBlank() }
            when {
                scoreLine != null && text != null -> "$scoreLine\n\n$text"
                scoreLine != null -> scoreLine
                else -> text
            }
        } catch (e: Exception) { null }
        if (serverText != null) {
            sections.add(ResultSection(
                W.t("num_sec_server_score"),
                listOf(ResultLine(W.t("num_lbl_detail"), serverText))
            ))
        } else {
            sections.add(ResultSection(
                W.t("num_sec_note"),
                listOf(ResultLine("", W.t("num_server_analysis_unavailable")))
            ))
        }
        return ToolResult(W.t("num_tool_compat_title"), sections)
    }

    private fun gemsResult(day: Int): ToolResult {
        val m = moolank(day)
        val r = planetRemedy(m)
        val dayName = W.t(r.dayKey)
        val wear = if (isHi)
            "$dayName को ${W.t(r.fingerKey)} में ${W.t(r.metalKey)} में जड़वाकर धारण करें।"
        else
            "Wear on $dayName, ${W.t(r.fingerKey)} — set in ${W.t(r.metalKey)}."
        val daan = if (isHi)
            "$dayName को ${r.color} वस्तुएं जरूरतमंदों को दान करें।"
        else
            "On $dayName, donate ${r.color} items to the needy."
        return ToolResult(W.t("num_tool_gems_title"), listOf(
            ResultSection("${W.t("num_sec_gems_for")} $m — ${r.planet}", listOf(
                ResultLine(W.t("num_lbl_gemstone"), r.gem, true),
                ResultLine(W.t("num_lbl_lucky_color"), r.color, true),
                ResultLine(W.t("num_lbl_power_day"), dayName),
                ResultLine(W.t("num_lbl_mantra"), r.mantra, true)
            )),
            ResultSection(W.t("num_sec_how_to_wear"), listOf(
                ResultLine(W.t("num_lbl_metal"), W.t(r.metalKey), true),
                ResultLine(W.t("num_lbl_finger"), W.t(r.fingerKey), true),
                ResultLine(W.t("num_lbl_tip"), wear)
            )),
            ResultSection(W.t("num_sec_daan"), listOf(
                ResultLine(W.t("num_lbl_donate"), daan)
            ))
        ))
    }

    private suspend fun luckyCalResult(day: Int, month: Int): ToolResult {
        val m = moolank(day)
        val (td, tm, ty) = todayParts()
        val py = personalYear(day, month, ty)
        val server = fetchServerNumerology()
        @Suppress("UNCHECKED_CAST")
        val luckyNumbers = (server?.get("luckyNumbers") as? List<*>)?.joinToString(", ")
        @Suppress("UNCHECKED_CAST")
        val luckyDaysSrv = (server?.get("luckyDays") as? List<*>)?.joinToString(", ")
        @Suppress("UNCHECKED_CAST")
        val luckyColors = (server?.get("luckyColors") as? List<*>)?.joinToString(", ")
        val dayLines = mutableListOf(
            ResultLine(
                W.t("num_lbl_weekdays"),
                luckyDaysSrv ?: luckyDays(m).joinToString(", "),
                true
            ),
            ResultLine(
                W.t("num_lbl_dates"),
                if (isHi) "किसी भी माह में $m, ${reduceNumber(m * 2)} तक घटने वाली तिथियां"
                else "Dates reducing to $m, ${reduceNumber(m * 2)} in any month"
            )
        )
        if (!luckyNumbers.isNullOrBlank()) dayLines.add(ResultLine(W.t("num_lbl_lucky_numbers_srv"), luckyNumbers, true))
        if (!luckyColors.isNullOrBlank()) dayLines.add(ResultLine(W.t("num_lbl_lucky_colors_srv"), luckyColors, true))
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_lucky_days"), dayLines),
            ResultSection(W.t("num_sec_lucky_dates"), listOf(
                ResultLine(
                    "${W.t("num_mon_$tm")} $ty",
                    luckyDatesInMonth(m, ty, tm).joinToString(", "),
                    true
                )
            )),
            ResultSection("${W.t("num_sec_this_year")} ($ty)", listOf(
                ResultLine(
                    W.t("num_lbl_personal_year"),
                    "$py — ${if (py in setOf(1, 3, 5, 6, 9)) W.t("num_py_energy_good") else W.t("num_py_energy_avg")} ${W.t("num_lbl_energy")}"
                ),
                ResultLine(
                    W.t("num_lbl_best_months"),
                    (1..12).filter { personalMonth(py, it) == m }.map { W.t("num_mon_$it") }.joinToString(", ").ifEmpty { "—" }
                )
            ))
        )
        serverNoteSection(server)?.let { sections.add(it) }
        return ToolResult(W.t("num_tool_lucky_cal_title"), sections)
    }

    private fun mobileResult(day: Int): ToolResult {
        val m = moolank(day)
        val (sum, tier) = numberCompatibility(_phone.value, m)
        val verdict = when {
            sum == 0 -> W.t("num_err_phone")
            tier == PairTier.EXCELLENT && sum == m -> W.t("num_verdict_resonates")
            else -> "${W.t(pairTierKey(tier))} — ${W.t(pairTierDescKey(tier))}"
        }
        val digits = _phone.value.filter { it.isDigit() }.map { it - '0' }.distinct()
        return ToolResult(W.t("num_tool_mobile_num_title"), listOf(
            ResultSection(W.t("num_sec_number_analysis"), listOf(
                ResultLine(W.t("num_lbl_digit_sum"), "$sum — ${planetRuler(sum)}", true),
                ResultLine(W.t("num_lbl_your_moolank"), "$m — ${planetRuler(m)}"),
                ResultLine(W.t("num_lbl_verdict"), verdict, true)
            )),
            ResultSection(W.t("num_sec_digit_influences"), digits.map { d ->
                ResultLine(
                    "${W.t("num_lbl_digit")} $d",
                    if (d == 0) W.t("num_zero_note") else planetRuler(d)
                )
            })
        ))
    }

    /**
     * Name correction — current name numbers computed ON-DEVICE (the
     * 2026-10-10 honesty fix holds: a missing server "expression" never
     * renders as a fabricated 0). Server analysis appended when available.
     */
    private suspend fun nameCorrectResult(): ToolResult {
        val nn = nameNumbers(_name.value.trim())
        val dob = parseDob(_dob.value)
        val luckyM = dob?.let { moolank(it.first) }
        val luckyB = dob?.let { bhagyank(it.first, it.second, it.third) }
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_current_name"), listOf(
                ResultLine(W.t("num_lbl_expression"), "${nn.expression} — ${planetRuler(nn.expression)}", true),
                ResultLine(W.t("num_lbl_soul_urge"), "${nn.soulUrge} — ${planetRuler(nn.soulUrge)}"),
                ResultLine(W.t("num_lbl_personality"), "${nn.personality} — ${planetRuler(nn.personality)}"),
                ResultLine(W.t("num_lbl_chaldean"), "${nn.chaldean} — ${planetRuler(nn.chaldean)}")
            )),
            ResultSection(W.t("num_sec_lucky_targets"), listOf(
                ResultLine(W.t("num_lbl_moolank_driver"), luckyM?.toString() ?: W.t("honesty_not_available"), true),
                ResultLine(W.t("num_lbl_bhagyank"), luckyB?.toString() ?: W.t("honesty_not_available"), true)
            ))
        )
        appendServerNameAnalysis(sections)
        sections.add(ResultSection(W.t("num_sec_guidance"), listOf(
            ResultLine(W.t("num_lbl_tip"), W.t("num_tip_match"))
        )))
        return ToolResult(W.t("num_tool_name_correct_title"), sections)
    }

    private fun kuaResult(year: Int): ToolResult {
        val isMale = _gender.value.lowercase() == "male"
        val sum = reduceNumber(year, keepMasters = false)
        val kua = kuaNumber(year, _gender.value)
        val (groupKey, dirs) = kuaInfo(kua)
        val formula = "${W.t(if (isMale) "num_form_male" else "num_form_female")}: " +
            "${if (isMale) "11" else "4"} ${if (isMale) "−" else "+"} $sum = $kua"
        return ToolResult(W.t("num_tool_kua_title"), listOf(
            ResultSection(W.t("num_sec_kua"), listOf(
                ResultLine(W.t("num_lbl_kua_number"), "$kua", true),
                ResultLine(W.t("num_lbl_group"), W.t(groupKey), true),
                ResultLine(W.t("num_lbl_element"), W.t(kuaElement(kua)), true),
                ResultLine(W.t("num_lbl_formula"), formula)
            )),
            ResultSection(W.t("num_sec_directions"), dirs.map { (dirKey, epithet) ->
                ResultLine(W.t("num_lbl_direction"), "${W.t(dirKey)} ($epithet)")
            })
        ))
    }

    private fun planetaryResult(day: Int, month: Int, year: Int): ToolResult {
        val grid = loShuGrid(day, month, year)
        val missing = (1..9).filter { (grid[it] ?: 0) == 0 }
        val planetLines = (1..9).map { n ->
            val count = grid[n] ?: 0
            val status = if (count > 0) "${W.t("num_present_x")}$count" else W.t("num_lbl_absent")
            ResultLine("$n — ${planetRuler(n)}", status, count == 0)
        }
        val missingText = if (missing.isEmpty()) W.t("num_complete_chart")
            else missing.joinToString(", ") { n -> "$n (${planetRemedy(n).gem})" }
        val topNum = (1..9).maxByOrNull { grid[it] ?: 0 } ?: 1
        val topPlanet = planetRuler(topNum)
        val career = if (isHi)
            "$topNum — $topPlanet ऊर्जा प्रबल है; $topPlanet से जुड़े करियर उपयुक्त रहते हैं।"
        else
            "$topNum — $topPlanet energy dominates; careers aligned with $topPlanet suit you."
        return ToolResult(
            W.t("num_tool_planetary_title"),
            listOf(
                ResultSection(W.t("num_sec_planets"), planetLines),
                ResultSection(
                    W.t("num_sec_number_meanings"),
                    (1..9).map { n -> ResultLine("$n — ${planetRuler(n)}", W.t(loShuMeaningKey(n))) }
                ),
                ResultSection(
                    W.t("num_sec_missing_remedies"),
                    listOf(ResultLine(W.t("num_lbl_absent"), missingText, true))
                ),
                ResultSection(
                    W.t("num_sec_career"),
                    listOf(ResultLine(
                        W.t("num_lbl_strongest"),
                        career
                    ))
                )
            )
        )
    }

    fun reset() {
        _uiState.value = ToolUiState.Idle
        _formError.value = null
    }
}
