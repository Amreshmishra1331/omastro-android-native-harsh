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

    /** parsed_facts rendered as chat context for follow-up AI questions. */
    fun toContextBlock(): String {
        if (parsedFactsRaw.isEmpty()) return ""
        val sb = StringBuilder("=== PALMISTRY READING (from palm photo) ===\n")
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
        render("", parsedFactsRaw)
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
