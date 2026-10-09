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
    // Every tool needs DOB — including MOBILE_NUM, whose verdict is compared
    // against the user's real Moolank (previously hardcoded Triple(1,1,2000)).
    fun needsDob(): Boolean = true
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
    // calculate() now always collects DOB (needsDob() == true for every tool),
    // so dobTriple is never the old hardcoded fallback.
        val dobTriple = parseDob(_dob.value)
        if (dobTriple == null) {
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

    // ---------- Server data (POST /numerology) ----------
    // The server computes coreNumbers, personalYear/Month/Day, lifeCycles,
    // pinnacles, challenges, nameAnalysis, loshuGrid, luckyNumbers/Days/Colors.
    // Tools use server values where they map 1:1, with local fallback on failure.
    private suspend fun fetchServerNumerology(): Map<String, Any>? {
        return try {
            val body = repo.getNumerology(mapOf(
                "dob" to _dob.value.trim(),
                "name" to _name.value.trim(),
                "language" to "en"
            )).getOrNull()
            @Suppress("UNCHECKED_CAST")
            body?.get("data") as? Map<String, Any>
        } catch (e: Exception) { null }
    }

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
            ResultSection("Core Numbers (Vedic)", listOf(
                ResultLine("Moolank (Driver)", "$m — ${planetRuler(m)}", true),
                ResultLine("Bhagyank (Life Path)", "$b — ${planetRuler(b)}", true),
                ResultLine("Success Number", "$s — ${planetRuler(s)}", true)
            ))
        )
        // Server core numbers (authoritative) as an extra section.
        val server = fetchServerNumerology()
        (server?.get("coreNumbers") as? Map<String, Any>)?.let { core ->
            sections.add(ResultSection("Server Core Numbers", listOf(
                ResultLine("Life Path", numEntry(core, "lifePathNumber"), true),
                ResultLine("Expression", numEntry(core, "expressionNumber")),
                ResultLine("Soul Urge", numEntry(core, "soulUrgeNumber")),
                ResultLine("Personality", numEntry(core, "personalityNumber")),
                ResultLine("Birthday", numEntry(core, "birthdayNumber")),
                ResultLine("Destiny", numEntry(core, "destinyNumber"))
            )))
        }
        sections.add(ResultSection("How they are derived", listOf(
            ResultLine("Moolank", "From birth day $day → $m. Shows who you are, how you think."),
            ResultLine("Bhagyank", "Day+Month+Year reduced → $b. Shows why you are here."),
            ResultLine("Success No.", "Day+Month reduced → $s. Shows how you achieve most.")
        )))
        return ToolResult("Moolank & Life Path", sections)
    }

    private suspend fun loshuResult(day: Int, month: Int, year: Int): ToolResult {
        val server = fetchServerNumerology()
        @Suppress("UNCHECKED_CAST")
        val sGrid = server?.get("loshuGrid") as? Map<String, Any>
        if (sGrid != null) {
            val present = (sGrid["presentNumbers"] as? List<*>)?.joinToString(", ") ?: "—"
            val absent = (sGrid["absentNumbers"] as? List<*>)?.joinToString(", ") ?: "—"
            val arrows = (sGrid["arrows"] as? Map<String, Any>)
                ?.filter { (_, v) -> v == true }?.keys?.joinToString(", ") ?: ""
            return ToolResult("Lo Shu Grid", listOf(
                ResultSection("Vedic Magic Square (server)", listOf(
                    ResultLine("Present numbers", present, true),
                    ResultLine("Absent numbers", absent, true)
                )),
                ResultSection("Arrows of strength", listOf(
                    ResultLine("Present", arrows.ifBlank { "No complete arrows in this chart." }, true)
                ))
            ))
        }
        // Local fallback
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

    private suspend fun timeCyclesResult(day: Int, month: Int): ToolResult {
        val (td, tm, ty) = todayParts()
        val server = fetchServerNumerology()
        val sPy = (server?.get("personalYear") as? Map<*, *>)
        val sPm = (server?.get("personalMonth") as? Map<*, *>)
        val sPd = (server?.get("personalDay") as? Map<*, *>)
        val pyLine = if (sPy != null)
            ResultLine("Personal Year ($ty)", "${sPy["current"]} — ${(sPy["meaning"] as? String).orEmpty()}", true)
        else {
            val py = personalYear(day, month, ty)
            ResultLine("Personal Year ($ty)", "$py — ${planetRuler(py)}", true)
        }
        val pmLine = if (sPm != null)
            ResultLine("Personal Month", "${sPm["value"]} — ${(sPm["meaning"] as? String).orEmpty()}", true)
        else {
            val py = personalYear(day, month, ty); val pm = personalMonth(py, tm)
            ResultLine("Personal Month", "$pm — ${planetRuler(pm)}", true)
        }
        val pdLine = if (sPd != null)
            ResultLine("Personal Day (today)", "${sPd["value"]} — ${(sPd["meaning"] as? String).orEmpty()}", true)
        else {
            val py = personalYear(day, month, ty); val pm = personalMonth(py, tm); val pd = personalDay(pm, td)
            ResultLine("Personal Day (today)", "$pd — ${planetRuler(pd)}", true)
        }
        val py = personalYear(day, month, ty)
        return ToolResult("Time Cycles", listOf(
            ResultSection("Your current cycles", listOf(pyLine, pmLine, pdLine)),
            ResultSection("Next 12 months", (1..12).map { m ->
                val v = personalMonth(py, m)
                ResultLine("Month $m", "$v")
            })
        ))
    }

    private suspend fun karmicResult(day: Int, month: Int, year: Int): ToolResult {
        val debts = karmicDebts(day, month, year)
        val meanings = mapOf(
            13 to "13/4 — Hard work and discipline; shortcuts backfire.",
            14 to "14/5 — Freedom vs commitment; avoid excess.",
            16 to "16/7 — Ego dismantled; rebuild on spiritual truth.",
            19 to "19/1 — Independence earned through self-reliance."
        )
        val sections = mutableListOf(
            ResultSection("Karmic debt numbers", listOf(
                *(if (debts.isEmpty()) arrayOf(ResultLine("Debts", "None found in this chart — a clear karmic slate.", true))
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
            karmaNumbers?.forEach { k -> lines.add(ResultLine("Karma number", "$k", true)) }
            challenges?.forEach { c ->
                lines.add(ResultLine("Challenge ${c["number"]}", "${c["theme"]} (${c["years"]})"))
            }
            sections.add(ResultSection("Server Karmic Analysis", lines))
        }
        sections.add(ResultSection("Pinnacles (life peaks)", listOf(
            ResultLine("1st Pinnacle", "${reduceNumber(month + day)} — early life"),
            ResultLine("2nd Pinnacle", "${reduceNumber(day + year)} — middle life"),
            ResultLine("3rd Pinnacle", "${reduceNumber(reduceNumber(month + day) + reduceNumber(day + year))} — prime years"),
            ResultLine("4th Pinnacle", "${reduceNumber(month + year)} — later life")
        )))
        return ToolResult("Karmic Debt", sections)
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

    private suspend fun luckyCalResult(day: Int, month: Int): ToolResult {
        val m = moolank(day)
        val (td, tm, ty) = todayParts()
        val py = personalYear(day, month, ty)
        val server = fetchServerNumerology()
        @Suppress("UNCHECKED_CAST")
        val luckyNumbers = (server?.get("luckyNumbers") as? List<*>)?.joinToString(", ")
        @Suppress("UNCHECKED_CAST")
        val luckyDays = (server?.get("luckyDays") as? List<*>)?.joinToString(", ")
        @Suppress("UNCHECKED_CAST")
        val luckyColors = (server?.get("luckyColors") as? List<*>)?.joinToString(", ")
        val dayLines = mutableListOf(
            ResultLine("Weekdays", luckyDays ?: luckyDays(m).joinToString(", "), true),
            ResultLine("Dates", "Dates reducing to $m, ${reduceNumber(m * 2)} in any month")
        )
        if (!luckyNumbers.isNullOrBlank()) dayLines.add(ResultLine("Lucky numbers (server)", luckyNumbers, true))
        if (!luckyColors.isNullOrBlank()) dayLines.add(ResultLine("Lucky colors (server)", luckyColors, true))
        return ToolResult("Lucky Calendar", listOf(
            ResultSection("Lucky days", dayLines),
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
