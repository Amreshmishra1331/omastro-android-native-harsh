package com.omastro.app.ui.screens.numerology

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun setName(v: String) { _name.value = v; _formError.value = null }
    fun setDob(v: String) { _dob.value = v; _formError.value = null }
    fun setName2(v: String) { _name2.value = v; _formError.value = null }
    fun setDob2(v: String) { _dob2.value = v; _formError.value = null }
    fun setPhone(v: String) { _phone.value = v; _formError.value = null }
    fun setGender(v: String) { _gender.value = v }

    fun needsName(): Boolean = tool in setOf(
        NumerologyTool.MOOLANK, NumerologyTool.NAME_NUM, NumerologyTool.KARMIC,
        NumerologyTool.COMPAT, NumerologyTool.NAME_CORRECT, NumerologyTool.PLANETARY
    )
    fun needsDob(): Boolean = tool != NumerologyTool.MOBILE_NUM
    fun needsSecondPerson(): Boolean = tool == NumerologyTool.COMPAT
    fun needsPhone(): Boolean = tool == NumerologyTool.MOBILE_NUM
    fun needsGender(): Boolean = tool == NumerologyTool.KUA

    fun calculate() {
        _formError.value = null
        if (needsName() && _name.value.trim().length < 2 && tool != NumerologyTool.MOOLANK) {
            // Moolank works on DOB alone; name optional
            if (tool != NumerologyTool.MOOLANK) {
                _formError.value = "Please enter the full name."
                return
            }
        }
        val dobTriple = if (needsDob()) parseDob(_dob.value) else Triple(1, 1, 2000)
        if (needsDob() && dobTriple == null) {
            _formError.value = "Date of birth must be YYYY-MM-DD (e.g. 1995-08-15)."
            return
        }
        if (needsSecondPerson()) {
            if (_name2.value.trim().length < 2) { _formError.value = "Please enter the second person's name."; return }
            if (parseDob(_dob2.value) == null) { _formError.value = "Second date of birth must be YYYY-MM-DD."; return }
        }
        if (needsPhone() && _phone.value.filter { it.isDigit() }.length < 6) {
            _formError.value = "Please enter a valid mobile/vehicle number."
            return
        }
        viewModelScope.launch {
            _uiState.value = ToolUiState.Loading
            try {
                val result = compute(dobTriple!!)
                _uiState.value = ToolUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = ToolUiState.Error(e.message ?: "Something went wrong. Please try again.")
            }
        }
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

    // ---------- Tool computations ----------

    private fun moolankResult(day: Int, month: Int, year: Int): ToolResult {
        val m = moolank(day); val b = bhagyank(day, month, year); val s = successNumber(day, month)
        return ToolResult("Moolank & Life Path", listOf(
            ResultSection("Core Numbers", listOf(
                ResultLine("Moolank (Driver)", "$m — ${planetRuler(m)}", true),
                ResultLine("Bhagyank (Life Path)", "$b — ${planetRuler(b)}", true),
                ResultLine("Success Number", "$s — ${planetRuler(s)}", true)
            )),
            ResultSection("How they are derived", listOf(
                ResultLine("Moolank", "From birth day $day → $m. Shows who you are, how you think."),
                ResultLine("Bhagyank", "Day+Month+Year reduced → $b. Shows why you are here."),
                ResultLine("Success No.", "Day+Month reduced → $s. Shows how you achieve most.")
            ))
        ))
    }

    private fun loshuResult(day: Int, month: Int, year: Int): ToolResult {
        val grid = loShuGrid(day, month, year)
        val arrows = loShuArrows(grid)
        val gridLine = (1..9).joinToString(" ") { n -> "$n:${grid[n]}" }
        return ToolResult("Lo Shu Grid", listOf(
            ResultSection("Vedic Magic Square (counts per number)", listOf(
                ResultLine("Grid", gridLine, true)
            )),
            ResultSection("Arrows of strength", listOf(
                *(if (arrows.isEmpty()) arrayOf(ResultLine("Arrows", "No complete arrows in this chart."))
                else arrows.map { ResultLine("Present", it, true) }.toTypedArray())
            )),
            ResultSection("Missing numbers", listOf(
                ResultLine("Absent", (1..9).filter { (grid[it] ?: 0) == 0 }.joinToString(", ").ifEmpty { "None — all numbers present" })
            ))
        ))
    }

    private suspend fun nameNumResult(): ToolResult {
        val res = repo.getNumerologyNameCheck(mapOf("name" to _name.value.trim(), "dob" to _dob.value.trim()))
        val body = res.getOrThrow()
        val data = body["data"] as? Map<*, *> ?: throw IllegalStateException("Empty response from server.")
        val text = (data["analysis_text"] as? String).orEmpty()
        return ToolResult("Name Numerology", listOf(
            ResultSection("Name Numbers", listOf(
                ResultLine("Expression", "${data["expression"]} — ${planetRuler((data["expression"] as? Number)?.toInt() ?: 1)}"),
                ResultLine("Soul Urge", "${data["soul"]} — ${planetRuler((data["soul"] as? Number)?.toInt() ?: 1)}"),
                ResultLine("Personality", "${data["personality"]} — ${planetRuler((data["personality"] as? Number)?.toInt() ?: 1)}"),
                ResultLine("Chaldean", "${data["chaldean"]}")
            )),
            ResultSection("Server Analysis", listOf(ResultLine("Detail", text.ifBlank { "No analysis returned." })))
        ))
    }

    private fun timeCyclesResult(day: Int, month: Int): ToolResult {
        val (td, tm, ty) = todayParts()
        val py = personalYear(day, month, ty)
        val pm = personalMonth(py, tm)
        val pd = personalDay(pm, td)
        return ToolResult("Time Cycles", listOf(
            ResultSection("Your current cycles", listOf(
                ResultLine("Personal Year ($ty)", "$py — ${planetRuler(py)}", true),
                ResultLine("Personal Month", "$pm — ${planetRuler(pm)}", true),
                ResultLine("Personal Day (today)", "$pd — ${planetRuler(pd)}", true)
            )),
            ResultSection("Next 12 months", (1..12).map { m ->
                val v = personalMonth(py, m)
                ResultLine("Month $m", "$v")
            })
        ))
    }

    private fun karmicResult(day: Int, month: Int, year: Int): ToolResult {
        val debts = karmicDebts(day, month, year)
        val meanings = mapOf(
            13 to "13/4 — Hard work and discipline; shortcuts backfire.",
            14 to "14/5 — Freedom vs commitment; avoid excess.",
            16 to "16/7 — Ego dismantled; rebuild on spiritual truth.",
            19 to "19/1 — Independence earned through self-reliance."
        )
        return ToolResult("Karmic Debt", listOf(
            ResultSection("Karmic debt numbers", listOf(
                *(if (debts.isEmpty()) arrayOf(ResultLine("Debts", "None found in this chart — a clear karmic slate.", true))
                else debts.map { ResultLine("$it", meanings[it] ?: "", true) }.toTypedArray())
            )),
            ResultSection("Pinnacles (life peaks)", listOf(
                ResultLine("1st Pinnacle", "${reduceNumber(month + day)} — early life"),
                ResultLine("2nd Pinnacle", "${reduceNumber(day + year)} — middle life"),
                ResultLine("3rd Pinnacle", "${reduceNumber(reduceNumber(month + day) + reduceNumber(day + year))} — prime years"),
                ResultLine("4th Pinnacle", "${reduceNumber(month + year)} — later life")
            ))
        ))
    }

    private suspend fun compatResult(): ToolResult {
        val res = repo.getNumerologyCompatibility(mapOf(
            "name1" to _name.value.trim(), "dob1" to _dob.value.trim(),
            "name2" to _name2.value.trim(), "dob2" to _dob2.value.trim()
        ))
        val body = res.getOrThrow()
        val data = body["data"] as? Map<*, *> ?: throw IllegalStateException("Empty response from server.")
        val text = (data["analysis_text"] as? String).orEmpty()
        return ToolResult("Compatibility", listOf(
            ResultSection("Overall Score", listOf(
                ResultLine("Match", "${data["score"]}%", true)
            )),
            ResultSection("Detailed Analysis", listOf(ResultLine("Detail", text.ifBlank { "No analysis returned." })))
        ))
    }

    private fun gemsResult(day: Int): ToolResult {
        val m = moolank(day)
        val r = planetRemedy(m)
        return ToolResult("Gems & Remedies", listOf(
            ResultSection("For Moolank $m — ${r.planet}", listOf(
                ResultLine("Gemstone", r.gem, true),
                ResultLine("Lucky Color", r.color, true),
                ResultLine("Power Day", r.day),
                ResultLine("Mantra", r.mantra, true)
            )),
            ResultSection("Donation (Daan)", listOf(
                ResultLine("Donate", "On ${r.day}, donate ${r.color.lowercase()} items to the needy.")
            ))
        ))
    }

    private fun luckyCalResult(day: Int, month: Int): ToolResult {
        val m = moolank(day)
        val (td, tm, ty) = todayParts()
        val py = personalYear(day, month, ty)
        return ToolResult("Lucky Calendar", listOf(
            ResultSection("Lucky days", listOf(
                ResultLine("Weekdays", luckyDays(m).joinToString(", "), true),
                ResultLine("Dates", "Dates reducing to $m, ${reduceNumber(m * 2)} in any month")
            )),
            ResultSection("This year ($ty)", listOf(
                ResultLine("Personal Year", "$py — ${if (py in setOf(1, 3, 5, 6, 9)) "Favorable" else "Average"} energy"),
                ResultLine("Best months", (1..12).filter { personalMonth(py, it) == m }.joinToString(", ").ifEmpty { "—" })
            ))
        ))
    }

    private fun mobileResult(day: Int): ToolResult {
        val m = moolank(day)
        val (sum, verdict) = numberCompatibility(_phone.value, m)
        return ToolResult("Mobile / Lucky Number", listOf(
            ResultSection("Number analysis", listOf(
                ResultLine("Digit sum", "$sum — ${planetRuler(sum)}", true),
                ResultLine("Your Moolank", "$m — ${planetRuler(m)}"),
                ResultLine("Verdict", verdict, true)
            ))
        ))
    }

    private suspend fun nameCorrectResult(): ToolResult {
        val res = repo.getNumerologyNameCheck(mapOf("name" to _name.value.trim(), "dob" to _dob.value.trim()))
        val body = res.getOrThrow()
        val data = body["data"] as? Map<*, *> ?: throw IllegalStateException("Empty response from server.")
        val text = (data["analysis_text"] as? String).orEmpty()
        val expr = (data["expression"] as? Number)?.toInt() ?: 0
        val dob = parseDob(_dob.value)
        val lucky = dob?.let { moolank(it.first) } ?: 0
        return ToolResult("Name Correction", listOf(
            ResultSection("Current name numbers", listOf(
                ResultLine("Expression", "$expr"),
                ResultLine("Lucky Moolank target", "$lucky")
            )),
            ResultSection("Server Analysis", listOf(ResultLine("Detail", text.ifBlank { "No analysis returned." }))),
            ResultSection("Guidance", listOf(
                ResultLine("Tip", "A name whose Expression matches your Moolank ($lucky) or Bhagyank is considered most harmonious. Consult the full analysis above before changing spellings.")
            ))
        ))
    }

    private fun kuaResult(year: Int): ToolResult {
        val kua = kuaNumber(year, _gender.value)
        val (group, dirs) = kuaInfo(kua)
        return ToolResult("Kua Number", listOf(
            ResultSection("Your Kua", listOf(
                ResultLine("Kua Number", "$kua", true),
                ResultLine("Group", group, true)
            )),
            ResultSection("Lucky directions", dirs.map { ResultLine("Direction", it) })
        ))
    }

    private fun planetaryResult(day: Int, month: Int, year: Int): ToolResult {
        val grid = loShuGrid(day, month, year)
        val missing = (1..9).filter { (grid[it] ?: 0) == 0 }
        val planetLines = (1..9).map { n ->
            val count = grid[n] ?: 0
            val status = if (count > 0) "Present x$count" else "Missing"
            ResultLine("$n - ${planetRuler(n)}", status, count == 0)
        }
        val missingText = if (missing.isEmpty()) "None - complete chart"
            else missing.joinToString(", ") { n -> "$n (${planetRemedy(n).gem})" }
        val topNum = (1..9).maxByOrNull { grid[it] ?: 0 } ?: 1
        return ToolResult(
            "Planetary Numbers",
            listOf(
                ResultSection("9 planets and your numbers", planetLines),
                ResultSection(
                    "Missing number remedies",
                    listOf(ResultLine("Absent", missingText, true))
                ),
                ResultSection(
                    "Career hint",
                    listOf(ResultLine(
                        "Strongest",
                        "$topNum - ${planetRuler(topNum)} energy dominates; careers aligned with ${planetRuler(topNum)} suit you."
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
