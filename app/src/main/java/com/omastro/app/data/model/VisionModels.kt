package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

// ------------------------------------------------------------------
// Vastu Shastra engine (POST /vastu/analyze, GET /vastu/reference)
// ------------------------------------------------------------------

data class VastuRoomInput(
    @SerializedName("type") val type: String,
    @SerializedName("direction") val direction: String
)

data class VastuAnalyzeRequest(
    @SerializedName("facing") val facing: String,
    @SerializedName("entrance_pada") val entrancePada: Int? = null,
    @SerializedName("plot_shape") val plotShape: String? = null,
    @SerializedName("plot_length") val plotLength: Double? = null,
    @SerializedName("plot_breadth") val plotBreadth: Double? = null,
    @SerializedName("slope") val slope: String? = null,
    @SerializedName("road") val road: String? = null,
    @SerializedName("water_source") val waterSource: String? = null,
    @SerializedName("overhead_tank") val overheadTank: String? = null,
    @SerializedName("septic_tank") val septicTank: String? = null,
    @SerializedName("staircase") val staircase: String? = null,
    @SerializedName("rooms") val rooms: List<VastuRoomInput> = emptyList()
)

/** One dosha found by the classical engine. */
data class VastuDosha(
    val category: String = "",
    val title: String = "",
    val severity: String = "low", // high | medium | low
    val rule: String = "",
    val detail: String = "",
    val remedies: List<String> = emptyList()
)

/** One directional/placement check verdict. */
data class VastuCheck(
    val key: String = "",
    val title: String = "",
    val value: String = "",
    val verdict: String = "unknown", // ideal | acceptable | needs_care | dosha | auspicious | inauspicious | unknown
    val rule: String = "",
    val detail: String = ""
)

/** Parsed Vastu analysis for UI rendering. Built defensively from the raw map. */
data class VastuAnalysis(
    val score: Int = 0,
    val grade: String = "",
    val checks: List<VastuCheck> = emptyList(),
    val doshas: List<VastuDosha> = emptyList(),
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val disclaimer: String = ""
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>?): VastuAnalysis {
            if (data == null) return VastuAnalysis()
            fun num(m: Map<String, Any?>?, k: String): Int =
                (m?.get(k) as? Number)?.toInt() ?: 0
            fun str(m: Map<String, Any?>?, k: String): String =
                (m?.get(k) as? String).orEmpty()

            val doshas = (data["doshas"] as? List<Map<String, Any?>>).orEmpty().map { d ->
                VastuDosha(
                    category = str(d, "category"),
                    title = str(d, "title"),
                    severity = str(d, "severity").ifBlank { "low" },
                    rule = str(d, "rule"),
                    detail = str(d, "detail"),
                    remedies = (d["remedies"] as? List<String>).orEmpty()
                )
            }
            val counts = data["dosha_counts"] as? Map<String, Any?>

            val checksRaw = data["checks"] as? Map<String, Any?> ?: emptyMap()
            val checks = checksRaw.mapNotNull { (key, v) ->
                val m = v as? Map<String, Any?> ?: return@mapNotNull null
                if (key == "rooms") {
                    // rooms is a list — flatten each room into its own check
                    null
                } else {
                    VastuCheck(
                        key = key,
                        title = key.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        value = str(m, "value").ifBlank { str(m, "label") },
                        verdict = str(m, "verdict").ifBlank { "unknown" },
                        rule = str(m, "rule"),
                        detail = str(m, "detail").ifBlank { str(m, "effect") }
                    )
                }
            }.toMutableList()
            // Flatten room checks
            (checksRaw["rooms"] as? List<Map<String, Any?>>).orEmpty().forEachIndexed { i, r ->
                checks.add(
                    VastuCheck(
                        key = "room_$i",
                        title = str(r, "label").ifBlank { "Room ${i + 1}" },
                        value = str(r, "direction_name").ifBlank { str(r, "direction") },
                        verdict = str(r, "verdict").ifBlank { "unknown" },
                        rule = str(r, "rule"),
                        detail = ""
                    )
                )
            }

            return VastuAnalysis(
                score = num(data, "score"),
                grade = str(data, "grade"),
                checks = checks,
                doshas = doshas.sortedBy {
                    when (it.severity) { "high" -> 0; "medium" -> 1; else -> 2 }
                },
                highCount = num(counts, "high"),
                mediumCount = num(counts, "medium"),
                lowCount = num(counts, "low"),
                disclaimer = str(data, "disclaimer")
            )
        }
    }

    /**
     * FULL Vastu analysis rendered as chat context for follow-up AI questions.
     *
     * Includes score + grade, dosha counts, the inputs that were analyzed
     * (manual form values, or the parsed inputs from a house-map photo),
     * every direction/placement verdict, every dosha with severity, classical
     * rule, detail and all non-demolition remedies, and the disclaimer —
     * so the AI can explain anything in the report
     * ("ye dosh kaise theek karun?").
     *
     * @param request the manual-form request (null for photo mode)
     * @param photoParse the `photo_parse` map from /vastu/analyze-photo
     * @param photoNotes user notes sent with the photo
     */
    fun toFullContextBlock(
        request: VastuAnalyzeRequest?,
        photoParse: Map<String, Any?>?,
        photoNotes: String?
    ): String {
        val sb = StringBuilder("=== VASTU ANALYSIS ===\n")
        sb.append("Score: ").append(score).append("/100\n")
        if (grade.isNotBlank()) sb.append("Grade: ").append(grade).append('\n')
        sb.append("Dosha counts: high=").append(highCount)
            .append(", medium=").append(mediumCount)
            .append(", low=").append(lowCount).append('\n')

        if (request != null) {
            sb.append("\n--- Inputs analyzed (manual entry) ---\n")
            sb.append("Facing: ").append(request.facing).append('\n')
            request.entrancePada?.let { sb.append("Entrance pada: ").append(it).append('\n') }
            request.plotShape?.let { sb.append("Plot shape: ").append(it).append('\n') }
            request.plotLength?.let { sb.append("Plot length (ft): ").append(it).append('\n') }
            request.plotBreadth?.let { sb.append("Plot breadth (ft): ").append(it).append('\n') }
            request.slope?.let { sb.append("Slope towards: ").append(it).append('\n') }
            request.road?.let { sb.append("Road direction: ").append(it).append('\n') }
            request.waterSource?.let { sb.append("Water source: ").append(it).append('\n') }
            request.overheadTank?.let { sb.append("Overhead tank: ").append(it).append('\n') }
            request.septicTank?.let { sb.append("Septic tank: ").append(it).append('\n') }
            request.staircase?.let { sb.append("Staircase: ").append(it).append('\n') }
            if (request.rooms.isNotEmpty()) {
                sb.append("Rooms:\n")
                request.rooms.forEach { r ->
                    sb.append("- ").append(r.type).append(" → ").append(r.direction).append('\n')
                }
            }
        }
        if (!photoParse.isNullOrEmpty()) {
            sb.append("\n--- Inputs parsed from house-map photo ---\n")
            renderGeneric(sb, "", photoParse)
        }
        if (!photoNotes.isNullOrBlank()) {
            sb.append("Photo notes: ").append(photoNotes.trim()).append('\n')
        }

        if (checks.isNotEmpty()) {
            sb.append("\n--- Direction / placement verdicts ---\n")
            checks.forEach { c ->
                sb.append("- ").append(c.title)
                if (c.value.isNotBlank()) sb.append(" (").append(c.value).append(')')
                sb.append(": ").append(c.verdict.replace('_', ' ')).append('\n')
                if (c.rule.isNotBlank()) sb.append("  Rule: ").append(c.rule).append('\n')
                if (c.detail.isNotBlank()) sb.append("  Detail: ").append(c.detail).append('\n')
            }
        }

        if (doshas.isNotEmpty()) {
            sb.append("\n--- Doshas (by severity) ---\n")
            doshas.forEach { d ->
                sb.append('[').append(d.severity).append("] ").append(d.title).append('\n')
                if (d.category.isNotBlank()) sb.append("  Category: ").append(d.category).append('\n')
                if (d.rule.isNotBlank()) sb.append("  Classical rule: ").append(d.rule).append('\n')
                if (d.detail.isNotBlank()) sb.append("  Detail: ").append(d.detail).append('\n')
                if (d.remedies.isNotEmpty()) {
                    sb.append("  Remedies (non-demolition):\n")
                    d.remedies.forEach { sb.append("  - ").append(it).append('\n') }
                }
            }
        } else {
            sb.append("\nNo doshas found. The layout follows classical Vastu principles well.\n")
        }

        if (disclaimer.isNotBlank()) {
            sb.append("\nDisclaimer: ").append(disclaimer.trim()).append('\n')
        }
        sb.append("\nAnswer follow-up questions strictly from this analysis. ")
        sb.append("When the user asks how to fix a dosha, suggest the listed ")
        sb.append("non-demolition remedies first. Do not invent details that are not in it.")
        return sb.toString().trim()
    }
}

/** Renders an arbitrary parsed-facts map as key: value lines. */
internal fun renderGeneric(sb: StringBuilder, prefix: String, v: Any?) {
    when (v) {
        null -> Unit
        is String -> if (v.isNotBlank()) sb.append(prefix).append(v).append('\n')
        is Number, is Boolean -> sb.append(prefix).append(v).append('\n')
        is Map<*, *> -> v.forEach { (k, vv) -> renderGeneric(sb, "$prefix$k: ", vv) }
        is List<*> -> v.forEach { renderGeneric(sb, "$prefix- ", it) }
        else -> sb.append(prefix).append(v.toString()).append('\n')
    }
}

/** One pada entry from GET /vastu/reference (for the pada picker helper). */
data class VastuPadaInfo(
    val pada: Int = 0,
    val direction: String = "",
    val devata: String = "",
    val verdict: String = "",
    val effect: String = ""
)

/** 8 directions + center for pickers. */
val VASTU_DIRECTIONS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

val VASTU_DIRECTION_NAMES = mapOf(
    "N" to "North", "NE" to "North-East", "E" to "East", "SE" to "South-East",
    "S" to "South", "SW" to "South-West", "W" to "West", "NW" to "North-West",
    "C" to "Centre (Brahmasthan)"
)

/** 20 room types accepted by the engine. */
val VASTU_ROOM_TYPES = listOf(
    "pooja" to "Pooja Room",
    "kitchen" to "Kitchen",
    "master_bedroom" to "Master Bedroom",
    "bedroom" to "Bedroom",
    "children_room" to "Children's Room",
    "guest_room" to "Guest Room",
    "living_room" to "Living Room",
    "drawing_room" to "Drawing Room",
    "dining" to "Dining Room",
    "study" to "Study Room",
    "home_office" to "Home Office",
    "toilet" to "Toilet",
    "bathroom" to "Bathroom",
    "store" to "Store Room",
    "garage" to "Garage",
    "balcony" to "Balcony",
    "staircase" to "Staircase",
    "water_source" to "Water Source",
    "overhead_tank" to "Overhead Tank",
    "septic_tank" to "Septic Tank"
)

// ------------------------------------------------------------------
// Vision / photo AI (POST /ai/vision, POST /palmistry/analyze,
// POST /vastu/analyze-photo)
// ------------------------------------------------------------------

/** Result of POST /ai/vision. */
data class VisionAnalysis(
    val analysis: String = "",
    val parsedFacts: Map<String, Any?> = emptyMap(),
    val model: String = ""
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>?): VisionAnalysis {
            if (data == null) return VisionAnalysis()
            return VisionAnalysis(
                analysis = (data["analysis"] as? String).orEmpty(),
                parsedFacts = (data["parsed_facts"] as? Map<String, Any?>).orEmpty(),
                model = (data["model"] as? String).orEmpty()
            )
        }
    }

    /** Renders parsed_facts as a compact context block for AI chat. */
    fun toContextBlock(label: String): String {
        if (parsedFacts.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("=== PHOTO ANALYSIS: ").append(label).append(" ===\n")
        fun render(prefix: String, v: Any?) {
            when (v) {
                null -> Unit
                is String -> if (v.isNotBlank()) sb.append(prefix).append(v).append('\n')
                is Number, is Boolean -> sb.append(prefix).append(v).append('\n')
                is Map<*, *> -> v.forEach { (k, vv) -> render("$prefix$k: ", vv) }
                is List<*> -> v.forEach { render("$prefix- ", it) }
                else -> sb.append(prefix).append(v.toString()).append('\n')
            }
        }
        render("", parsedFacts)
        return sb.toString().trim()
    }
}

/** One palm line assessment. */
data class PalmLine(
    val name: String = "",
    val quality: String = "",
    val note: String = ""
)

/** One mount assessment. */
data class PalmMount(
    val name: String = "",
    val state: String = "",
    val note: String = ""
)

/** Result of POST /palmistry/analyze. */
data class PalmistryReading(
    val reading: String = "",
    val hand: String = "",
    val handType: String = "",
    val lines: List<PalmLine> = emptyList(),
    val mounts: List<PalmMount> = emptyList(),
    val keyObservations: List<String> = emptyList(),
    val guidance: List<String> = emptyList(),
    val uncertainties: List<String> = emptyList(),
    val disclaimer: String = "",
    val parsedFactsRaw: Map<String, Any?> = emptyMap()
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>?): PalmistryReading {
            if (data == null) return PalmistryReading()
            val pf = (data["parsed_facts"] as? Map<String, Any?>).orEmpty()
            fun strList(v: Any?): List<String> =
                (v as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            return PalmistryReading(
                reading = (data["reading"] as? String).orEmpty(),
                hand = (pf["hand"] as? String).orEmpty(),
                handType = (pf["hand_type"] as? String).orEmpty(),
                lines = (pf["lines"] as? List<Map<String, Any?>>).orEmpty().map {
                    PalmLine(
                        name = (it["name"] as? String).orEmpty(),
                        quality = (it["quality"] as? String).orEmpty(),
                        note = (it["note"] as? String).orEmpty()
                    )
                },
                mounts = (pf["mounts"] as? List<Map<String, Any?>>).orEmpty().map {
                    PalmMount(
                        name = (it["name"] as? String).orEmpty(),
                        state = (it["state"] as? String).orEmpty(),
                        note = (it["note"] as? String).orEmpty()
                    )
                },
                keyObservations = strList(pf["key_observations"]),
                guidance = strList(pf["guidance"]),
                uncertainties = strList(pf["uncertainties"]),
                disclaimer = (data["disclaimer"] as? String).orEmpty(),
                parsedFactsRaw = pf
            )
        }
    }

    /**
     * FULL palmistry reading rendered as chat context for follow-up AI questions.
     *
     * Includes the hand summary, the complete reading text, every line, every
     * mount, all key observations, all guidance, uncertainties, the disclaimer,
     * and any extra parsed_facts — so the AI can explain or expand on anything
     * in the report ("meri health line kya kehti hai?").
     */
    fun toContextBlock(): String {
        val sb = StringBuilder("=== PALMISTRY READING (from palm photo) ===\n")
        if (hand.isNotBlank()) {
            sb.append("Hand: ").append(hand)
            if (handType.isNotBlank()) sb.append(" (").append(handType).append(')')
            sb.append('\n')
        }
        if (reading.isNotBlank()) {
            sb.append("\n--- Full reading ---\n").append(reading.trim()).append('\n')
        }
        if (lines.isNotEmpty()) {
            sb.append("\n--- Lines ---\n")
            lines.forEach { l ->
                sb.append("- ").append(l.name)
                if (l.quality.isNotBlank()) sb.append(" [").append(l.quality).append(']')
                if (l.note.isNotBlank()) sb.append(": ").append(l.note)
                sb.append('\n')
            }
        }
        if (mounts.isNotEmpty()) {
            sb.append("\n--- Mounts ---\n")
            mounts.forEach { m ->
                sb.append("- ").append(m.name)
                if (m.state.isNotBlank()) sb.append(" [").append(m.state).append(']')
                if (m.note.isNotBlank()) sb.append(": ").append(m.note)
                sb.append('\n')
            }
        }
        if (keyObservations.isNotEmpty()) {
            sb.append("\n--- Key observations ---\n")
            keyObservations.forEach { sb.append("- ").append(it).append('\n') }
        }
        if (guidance.isNotEmpty()) {
            sb.append("\n--- Guidance ---\n")
            guidance.forEach { sb.append("- ").append(it).append('\n') }
        }
        if (uncertainties.isNotEmpty()) {
            sb.append("\n--- Unclear in photo ---\n")
            uncertainties.forEach { sb.append("- ").append(it).append('\n') }
        }
        // Any extra parsed_facts not covered above (raw key: value).
        val covered = setOf(
            "hand", "hand_type", "lines", "mounts",
            "key_observations", "guidance", "uncertainties"
        )
        val extras = parsedFactsRaw.filterKeys { it !is String || (it as String) !in covered }
        if (extras.isNotEmpty()) {
            sb.append("\n--- Additional parsed facts ---\n")
            renderGeneric(sb, "", extras)
        }
        if (disclaimer.isNotBlank()) {
            sb.append("\nDisclaimer: ").append(disclaimer.trim()).append('\n')
        }
        sb.append("\nAnswer follow-up questions strictly from this reading. ")
        sb.append("Do not invent details that are not in it.")
        return sb.toString().trim()
    }
}

/**
 * A photo-analysis attachment for AI chat context (like BirthDataAttachment,
 * but for photo/vision parsed facts). Stored in-memory per chat mode.
 */
data class PhotoContextAttachment(
    val label: String,
    val contextBlock: String,
    val id: String = java.util.UUID.randomUUID().toString()
)

/**
 * Cross-screen handoff: palmistry / vastu / vision results can attach their
 * parsed facts to AI chat. Consumed once by AiChatViewModel on next open.
 */
object PendingPhotoContextHolder {
    @Volatile private var pending: PhotoContextAttachment? = null
    fun set(attachment: PhotoContextAttachment) { pending = attachment }
    fun consume(): PhotoContextAttachment? {
        val p = pending
        pending = null
        return p
    }
}
