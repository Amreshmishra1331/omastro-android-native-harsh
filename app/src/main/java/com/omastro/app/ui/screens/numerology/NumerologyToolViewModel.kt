package com.omastro.app.ui.screens.numerology

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.OmAstroApp
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.repository.MyDataRepository
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Thrown by fully server-driven tools (Synthesis) with no on-device fallback. */
class NumerologyServerUnavailable : Exception()

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
        NumerologyTool.NAME_NUM, NumerologyTool.COMPAT, NumerologyTool.NAME_CORRECT,
        NumerologyTool.SYNTHESIS
    )
    // Every tool needs DOB — including MOBILE_NUM, whose verdict is compared
    // against the user's real Moolank (previously hardcoded Triple(1,1,2000)).
    fun needsDob(): Boolean = true
    fun needsSecondPerson(): Boolean = tool == NumerologyTool.COMPAT
    fun needsPhone(): Boolean = tool == NumerologyTool.MOBILE_NUM
    fun needsGender(): Boolean = tool == NumerologyTool.KUA

    fun calculate(saveLocal: Boolean = true) {
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
                // App storage: auto-save the completed result on-device
                // (My Data). Fire-and-forget — language re-renders and stored
                // re-opens never write (saveLocal = false).
                if (saveLocal) {
                    try {
                        MyDataRepository(OmAstroApp.instance.applicationContext)
                            .saveNumerology(
                                toolId = tool.id,
                                toolName = W.t("num_tool_${tool.id}_title"),
                                inputs = currentInputs(),
                                result = result
                            )
                    } catch (_: Exception) {
                    }
                }
            } catch (e: NumerologyServerUnavailable) {
                _uiState.value = ToolUiState.Error(W.t("num_server_analysis_unavailable"))
            } catch (e: Exception) {
                _uiState.value = ToolUiState.Error(W.t("num_err_generic"))
            }
        }
    }

    /** Re-runs the last calculation (e.g. after a Settings language change). */
    fun recalculate() {
        if (_uiState.value is ToolUiState.Success) calculate(saveLocal = false)
    }

    /** Snapshot of the form inputs for local storage / stored re-open. */
    fun currentInputs(): Map<String, String> = mapOf(
        "name" to _name.value,
        "dob" to _dob.value,
        "name2" to _name2.value,
        "dob2" to _dob2.value,
        "phone" to _phone.value,
        "gender" to _gender.value
    )

    /**
     * Instant re-open of a locally saved result (from My Data): re-renders
     * the stored result verbatim and restores the inputs — no re-compute.
     */
    fun loadStoredResult(inputs: Map<String, String>, result: ToolResult) {
        _name.value = inputs["name"] ?: ""
        _dob.value = inputs["dob"] ?: ""
        _name2.value = inputs["name2"] ?: ""
        _dob2.value = inputs["dob2"] ?: ""
        _phone.value = inputs["phone"] ?: ""
        _gender.value = inputs["gender"]?.ifBlank { "male" } ?: "male"
        _formError.value = null
        _uiState.value = ToolUiState.Success(result)
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
            NumerologyTool.SYNTHESIS -> synthesisResult()
        }
    }

    // ---------- Server data (POST /numerology, per-tab) ----------
    // The website engine computes every tool's full analysis server-side.
    // Each `tab=` returns {success, data:{analysis_text, context}} — the app
    // renders analysis_text verbatim (bilingual via the `language` param) and
    // keeps its own on-device math as the honest fallback, never fake data.
    // NOTE (Cycle 9): the old code read structured fields the server never
    // returns (coreNumbers, loshuGrid, personalYear/Month/Day, lifeCycles,
    // karmaNumbers, challenges, luckyNumbers/Days/Colors) — all removed.
    private suspend fun tabAnalysisText(tab: String, extra: Map<String, String> = emptyMap()): String? {
        return try {
            val params = mutableMapOf(
                "tab" to tab,
                "dob" to _dob.value.trim(),
                "name" to _name.value.trim(),
                "language" to LangPrefs.language.value
            )
            params.putAll(extra)
            val body = repo.getNumerology(params).getOrNull()
            @Suppress("UNCHECKED_CAST")
            val data = body?.get("data") as? Map<*, *>
            (data?.get("analysis_text") as? String)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
    }

    /** Honest "server unavailable" section appended when the server tab fetch failed. */
    private fun serverUnavailableSection(): ResultSection = ResultSection(
        W.t("num_sec_note"),
        listOf(ResultLine("", W.t("num_server_analysis_unavailable")))
    )

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
        // The website's full Moolank/Life-Path write-up is the Overview tab.
        tabAnalysisText("overview")?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
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
        return ToolResult(W.t("num_tool_moolank_title"), sections)
    }

    private suspend fun loshuResult(day: Int, month: Int, year: Int): ToolResult {
        val sections = mutableListOf<ResultSection>()
        // Local computation — always shown (it is real math from the DOB).
        // The website's full Lo Shu reading is the `loshu` tab.
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
        // The website's full Lo Shu reading is the `loshu` tab.
        tabAnalysisText("loshu")?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        }
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
        val py = personalYear(day, month, ty)
        val pmNow = personalMonth(py, tm)
        val pdNow = personalDay(pmNow, td)
        val sections = mutableListOf(
            ResultSection(W.t("num_sec_cycles"), listOf(
                ResultLine(
                    "${W.t("num_lbl_personal_year")} ($ty)",
                    "$py — ${planetRuler(py)}: ${W.t(cycleMeaningKey(py))}",
                    true
                ),
                ResultLine(
                    W.t("num_lbl_personal_month"),
                    "$pmNow — ${planetRuler(pmNow)}: ${W.t(cycleMeaningKey(pmNow))}",
                    true
                ),
                ResultLine(
                    W.t("num_lbl_personal_day"),
                    "$pdNow — ${planetRuler(pdNow)}: ${W.t(cycleMeaningKey(pdNow))}",
                    true
                )
            )),
            ResultSection(W.t("num_sec_next12"), (1..12).map { m ->
                val v = personalMonth(py, m)
                ResultLine(
                    W.t("num_mon_$m"),
                    "$v — ${planetRuler(v)}: ${W.t(cycleMeaningKey(v))}"
                )
            })
        )
        // The website's full cycles reading is the `cycles` tab.
        tabAnalysisText("cycles")?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        }
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
        // The website's authoritative Pinnacles reading is the `pinnacles` tab.
        tabAnalysisText("pinnacles")?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        }
        // Local Pinnacles — web formula (components flattened first, masters
        // 11/22/33 kept only on the final sums). Shown even when the server
        // text is present, as the on-device cross-check.
        val pins = pinnacleNumbers(day, month, year)
        val pinLabels = listOf("num_pinnacle_1", "num_pinnacle_2", "num_pinnacle_3", "num_pinnacle_4")
        sections.add(ResultSection(W.t("num_sec_pinnacles"), pins.mapIndexed { i, p ->
            ResultLine(W.t(pinLabels[i]), "$p — ${planetRuler(p)}", true)
        }))
        // Challenges — web formula: raw diffs, unreduced, 0 possible. Meaning
        // lines exist only for single digits; larger raw diffs are shown
        // honestly as-is with no fabricated meaning.
        val ch = challengeNumbers(day, month, year)
        val chLabels = listOf("num_challenge_first", "num_challenge_second", "num_challenge_third", "num_challenge_main")
        sections.add(ResultSection(W.t("num_sec_challenges"), ch.mapIndexed { i, v ->
            val value = if (v in 0..9) "$v — ${W.t("num_challenge_$v")}" else "$v"
            ResultLine(W.t(chLabels[i]), value, true)
        }))
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
        val dayLines = mutableListOf(
            ResultLine(
                W.t("num_lbl_weekdays"),
                luckyDays(m).joinToString(", "),
                true
            ),
            ResultLine(
                W.t("num_lbl_dates"),
                if (isHi) "किसी भी माह में $m, ${reduceNumber(m * 2)} तक घटने वाली तिथियां"
                else "Dates reducing to $m, ${reduceNumber(m * 2)} in any month"
            )
        )
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
        // The website's full lucky-numbers reading is the `lucky` tab.
        tabAnalysisText("lucky")?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        }
        return ToolResult(W.t("num_tool_lucky_cal_title"), sections)
    }

    /**
     * Mobile number — the website computes the full server-side analysis via
     * `tab=mobile` with `name=<mobile digits>` (previously the app never
     * called it). Server text is authoritative; the on-device digit-sum
     * verdict stays as the honest fallback.
     */
    private suspend fun mobileResult(day: Int): ToolResult {
        val digits = _phone.value.filter { it.isDigit() }
        val sections = mutableListOf<ResultSection>()
        tabAnalysisText("mobile", mapOf("name" to digits))?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        }
        val m = moolank(day)
        val (sum, tier) = numberCompatibility(_phone.value, m)
        val verdict = when {
            sum == 0 -> W.t("num_err_phone")
            tier == PairTier.EXCELLENT && sum == m -> W.t("num_verdict_resonates")
            else -> "${W.t(pairTierKey(tier))} — ${W.t(pairTierDescKey(tier))}"
        }
        sections.add(ResultSection(W.t("num_sec_number_analysis"), listOf(
            ResultLine(W.t("num_lbl_digit_sum"), "$sum — ${planetRuler(sum)}", true),
            ResultLine(W.t("num_lbl_your_moolank"), "$m — ${planetRuler(m)}"),
            ResultLine(W.t("num_lbl_verdict"), verdict, true)
        )))
        sections.add(ResultSection(W.t("num_sec_digit_influences"), digits.map { it - '0' }.distinct().map { d ->
            ResultLine(
                "${W.t("num_lbl_digit")} $d",
                if (d == 0) W.t("num_zero_note") else planetRuler(d)
            )
        }))
        return ToolResult(W.t("num_tool_mobile_num_title"), sections)
    }

    /**
     * Name correction — current name numbers computed ON-DEVICE (the
     * 2026-10-10 honesty fix holds: a missing server "expression" never
     * renders as a fabricated 0). The website's correction guidance —
     * Harmony Score, add/remove-letter guidance, office-use suggestion —
     * comes from `tab=namefix` (NOT `/numerology/name-check`, which is
     * numbers-only), rendered verbatim when available.
     */
    private suspend fun nameCorrectResult(): ToolResult {
        val nn = nameNumbers(_name.value.trim())
        val dob = parseDob(_dob.value)
        val luckyM = dob?.let { moolank(it.first) }
        val luckyB = dob?.let { bhagyank(it.first, it.second, it.third) }
        val sections = mutableListOf<ResultSection>()
        tabAnalysisText("namefix")?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        } ?: sections.add(serverUnavailableSection())
        sections.add(ResultSection(W.t("num_sec_current_name"), listOf(
            ResultLine(W.t("num_lbl_expression"), "${nn.expression} — ${planetRuler(nn.expression)}", true),
            ResultLine(W.t("num_lbl_soul_urge"), "${nn.soulUrge} — ${planetRuler(nn.soulUrge)}"),
            ResultLine(W.t("num_lbl_personality"), "${nn.personality} — ${planetRuler(nn.personality)}"),
            ResultLine(W.t("num_lbl_chaldean"), "${nn.chaldean} — ${planetRuler(nn.chaldean)}")
        )))
        sections.add(ResultSection(W.t("num_sec_lucky_targets"), listOf(
            ResultLine(W.t("num_lbl_moolank_driver"), luckyM?.toString() ?: W.t("honesty_not_available"), true),
            ResultLine(W.t("num_lbl_bhagyank"), luckyB?.toString() ?: W.t("honesty_not_available"), true)
        )))
        sections.add(ResultSection(W.t("num_sec_guidance"), listOf(
            ResultLine(W.t("num_lbl_tip"), W.t("num_tip_match"))
        )))
        return ToolResult(W.t("num_tool_name_correct_title"), sections)
    }

    /**
     * Kua — server-authoritative via `tab=kua` (dob, gender, language);
     * the local section below is the on-device cross-check using the web
     * formula exactly (last-two-digits rule), not the old full-year one.
     */
    private suspend fun kuaResult(year: Int): ToolResult {
        val isMale = _gender.value.lowercase() == "male"
        val digitSum = kuaYearDigitSum(year)
        val lastTwo = abs(year) % 100
        val kua = kuaNumber(year, _gender.value)
        val (groupKey, dirs) = kuaInfo(kua)
        val formula = if (isMale)
            "${W.t("num_form_male")}: 10 − ($lastTwo → $digitSum) = $kua"
        else
            "${W.t("num_form_female")}: $digitSum + 9 = $kua"
        val sections = mutableListOf<ResultSection>()
        tabAnalysisText("kua", mapOf("gender" to _gender.value.lowercase()))?.let { text ->
            sections.add(ResultSection(
                W.t("num_sec_server_analysis"),
                listOf(ResultLine(W.t("num_lbl_detail"), text))
            ))
        } ?: sections.add(serverUnavailableSection())
        sections.add(ResultSection(W.t("num_sec_kua"), listOf(
            ResultLine(W.t("num_lbl_kua_number"), "$kua", true),
            ResultLine(W.t("num_lbl_group"), W.t(groupKey), true),
            ResultLine(W.t("num_lbl_element"), W.t(kuaElement(kua)), true),
            ResultLine(W.t("num_lbl_formula"), formula)
        )))
        sections.add(ResultSection(W.t("num_sec_directions"), dirs.map { (dirKey, epithet) ->
            ResultLine(W.t("num_lbl_direction"), "${W.t(dirKey)} ($epithet)")
        }))
        return ToolResult(W.t("num_tool_kua_title"), sections)
    }

    /**
     * Complete Synthesis — the 13th tool. Fully server-driven:
     * `tab=synthesis` with dob + name + language. No on-device fallback
     * exists for a combined profile, so an unreachable server is an honest
     * error (never fabricated numbers).
     */
    private suspend fun synthesisResult(): ToolResult {
        val text = tabAnalysisText("synthesis")
        return if (text != null) {
            ToolResult(W.t("num_tool_synthesis_title"), listOf(
                ResultSection(
                    W.t("num_sec_server_analysis"),
                    listOf(ResultLine(W.t("num_lbl_detail"), text))
                )
            ))
        } else {
            throw NumerologyServerUnavailable()
        }
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
