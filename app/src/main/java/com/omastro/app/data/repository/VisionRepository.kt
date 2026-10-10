package com.omastro.app.data.repository

import com.google.gson.Gson
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.FaceReading
import com.omastro.app.data.model.PalmistryReading
import com.omastro.app.data.model.VastuAnalysis
import com.omastro.app.data.model.VastuAnalyzeRequest
import com.omastro.app.data.model.VastuPadaInfo
import com.omastro.app.data.model.VisionAnalysis
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Thrown when a paid vision endpoint answers HTTP 402
 * `{code: "insufficient_balance", data: {price_paise, balance_paise}}`.
 * Carries the authoritative server-side price and the user's current
 * wallet balance (both paise) so the UI can show an Add-Money dialog
 * instead of retrying the analysis.
 */
class InsufficientBalanceException(
    val pricePaise: Int,
    val balancePaise: Int,
    message: String
) : IOException(message)

/**
 * Repository for the NEW world-class backend systems:
 * Vastu Shastra engine + Vision/Photo AI + Palmistry.
 *
 * All responses are parsed defensively from Map envelopes
 * ({success, data}) — shapes are rich and evolving.
 */
class VisionRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val gson = Gson()

    companion object {
        /**
         * Server-side default prices for the paid vision endpoints, in paise.
         * Source: backend options `kg_rate_palmistry_paise`,
         * `kg_rate_facereading_paise`, `kg_rate_vastu_photo_paise`
         * (all default 2000 = ₹20, admin-editable in WP Admin).
         * Shown on Analyze buttons; the HTTP 402 response always carries the
         * live authoritative price, which the insufficient-balance dialog uses.
         * There is no /vision/prices endpoint yet — these stay defaults.
         */
        const val PRICE_PALMISTRY_PAISE = 2000
        const val PRICE_FACE_READING_PAISE = 2000
        const val PRICE_VASTU_PHOTO_PAISE = 2000
    }

    // ------------------------------------------------------------------
    // Vastu
    // ------------------------------------------------------------------

    /** POST /vastu/analyze — classical engine from manual inputs.
     *
     * Returns the parsed analysis plus the full server `data` JSON
     * (verbatim) so completed analyses can be saved to reading history and
     * re-rendered exactly as the server returned them. */
    suspend fun analyzeVastu(request: VastuAnalyzeRequest): Result<Pair<VastuAnalysis, String>> {
        return try {
            val res = api.analyzeVastu(request)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty Vastu response."))
            Result.success(VastuAnalysis.fromMap(data) to gson.toJson(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * GET /vastu/reference — pada chart for the entrance-pada picker.
     * Returns pada list (1-32 with devata + verdict). Falls back to empty
     * on failure; the picker works with plain numbers then.
     */
    suspend fun getVastuReference(): Result<List<VastuPadaInfo>> {
        return try {
            val res = api.getVastuReference()
            val body = res.body()
            if (!res.isSuccessful || body == null) return Result.success(emptyList())
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?> ?: return Result.success(emptyList())
            @Suppress("UNCHECKED_CAST")
            val groups = data["pada_chart"] as? List<Map<String, Any?>> ?: emptyList()
            val out = mutableListOf<VastuPadaInfo>()
            for (g in groups) {
                val dir = (g["direction"] as? String).orEmpty()
                @Suppress("UNCHECKED_CAST")
                val padas = g["padas"] as? List<Map<String, Any?>> ?: emptyList()
                for (p in padas) {
                    out.add(
                        VastuPadaInfo(
                            pada = (p["pada"] as? Number)?.toInt() ?: 0,
                            direction = dir,
                            devata = (p["devata"] as? String).orEmpty(),
                            verdict = (p["verdict"] as? String).orEmpty(),
                            effect = (p["effect"] as? String).orEmpty()
                        )
                    )
                }
            }
            Result.success(out.sortedBy { it.pada })
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    /**
     * POST /vastu/analyze-photo — house-map photo → vision parse → engine.
     * Returns the parsed inputs (for editable review) plus the analysis plus
     * the full server `data` JSON (verbatim) for reading-history storage.
     */
    suspend fun analyzeVastuPhoto(
        photo: File,
        notes: String? = null
    ): Result<Triple<Map<String, Any?>, VastuAnalysis, String>> {
        return try {
            val part = filePart("photo", photo)
            val notesBody = notes?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val res = api.analyzeVastuPhoto(part, notesBody, langBody())
            val body = res.body()
            parseInsufficient(res.code(), body)?.let { return Result.failure(it) }
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty Vastu photo response."))
            applyVisionBilling(data)
            @Suppress("UNCHECKED_CAST")
            val photoParse = data["photo_parse"] as? Map<String, Any?> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val analysisMap = data["vastu_analysis"] as? Map<String, Any?>
            Result.success(Triple(photoParse, VastuAnalysis.fromMap(analysisMap), gson.toJson(data)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Vision / photo AI
    // ------------------------------------------------------------------

    /** POST /ai/vision — image + question → analysis + parsed_facts. */
    suspend fun analyzeVision(
        image: File,
        question: String,
        context: String? = null
    ): Result<VisionAnalysis> {
        return try {
            val part = filePart("image", image)
            val qBody = question.toRequestBody("text/plain".toMediaTypeOrNull())
            val ctxBody = context?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val res = api.analyzeVision(part, qBody, ctxBody, langBody())
            val body = res.body()
            parseInsufficient(res.code(), body)?.let { return Result.failure(it) }
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty vision response."))
            applyVisionBilling(data)
            Result.success(VisionAnalysis.fromMap(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** POST /palmistry/analyze — palm photo → structured reading.
     *
     * Returns the parsed reading plus the full server `data` JSON
     * (verbatim) so completed readings can be saved to reading history and
     * re-rendered exactly as the server returned them. */
    suspend fun analyzePalmistry(
        image: File,
        hand: String? = null,
        notes: String? = null
    ): Result<Pair<PalmistryReading, String>> {
        return try {
            val part = filePart("image", image)
            val handBody = hand?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val notesBody = notes?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val res = api.analyzePalmistry(part, handBody, notesBody, langBody())
            val body = res.body()
            parseInsufficient(res.code(), body)?.let { return Result.failure(it) }
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty palmistry response."))
            applyVisionBilling(data)
            Result.success(PalmistryReading.fromMap(data) to gson.toJson(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** POST /face-reading/analyze — face photo → Samudrika Shastra reading.
     *
     * Returns the parsed reading plus the full server `data` JSON
     * (verbatim) so completed readings can be saved to reading history and
     * re-rendered exactly as the server returned them. */
    suspend fun analyzeFaceReading(
        image: File,
        notes: String? = null
    ): Result<Pair<FaceReading, String>> {
        return try {
            val part = filePart("image", image)
            val notesBody = notes?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val res = api.analyzeFaceReading(part, notesBody, langBody())
            val body = res.body()
            // The endpoint is still being built server-side: surface an honest
            // "not available yet" instead of a raw HTTP 404.
            if (res.code() == 404) {
                return Result.failure(
                    IOException(W.t("face_err_unavailable"))
                )
            }
            parseInsufficient(res.code(), body)?.let { return Result.failure(it) }
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty face-reading response."))
            applyVisionBilling(data)
            Result.success(FaceReading.fromMap(data) to gson.toJson(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** The app's Settings language ("en"|"hi") as a multipart part,
     * so the server replies in the seeker's own language. */
    private fun langBody() =
        LangPrefs.language.value.toRequestBody("text/plain".toMediaTypeOrNull())

    /**
     * HTTP 402 → [InsufficientBalanceException]. The server answers
     * `{code: "insufficient_balance", message, data: {status: 402,
     * price_paise, balance_paise}}`; the server's own (bilingual) message is
     * used when present. Returns null when the response is not a 402.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseInsufficient(
        code: Int,
        body: Map<String, Any?>?
    ): InsufficientBalanceException? {
        if (code != 402) return null
        val data = body?.get("data") as? Map<String, Any?>
        val pricePaise = (data?.get("price_paise") as? Number)?.toInt() ?: 0
        val balancePaise = (data?.get("balance_paise") as? Number)?.toInt() ?: 0
        val msg = (body?.get("message") as? String)
            ?: (data?.get("message") as? String)
            ?: W.t("vision_err_402")
        return InsufficientBalanceException(pricePaise, balancePaise, msg)
    }

    /**
     * Paid-vision success responses carry `price_charged_paise` and
     * `wallet_balance_paise` — refresh the shared wallet balance from the
     * server's authoritative post-charge figure so the wallet display and
     * any subsequent 402 dialog stay in sync without another round-trip.
     */
    private fun applyVisionBilling(data: Map<String, Any?>) {
        val newBalance = (data["wallet_balance_paise"] as? Number)?.toInt()
        if (newBalance != null) {
            AppContainer.walletRepository.applyServerBalance(newBalance)
        }
    }

    private fun filePart(name: String, file: File): MultipartBody.Part {
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val body = file.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, file.name, body)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseError(code: Int, body: Map<String, Any?>?): String {
        val data = body?.get("data") as? Map<String, Any?>
        val msg = (data?.get("message") as? String)
            ?: (body?.get("message") as? String)
        // Bilingual user-facing errors (auto-switch with Settings language);
        // a server-provided message still takes precedence when present.
        return when (code) {
            401 -> W.t("vision_err_auth")
            429 -> W.t("vision_err_rate")
            422 -> msg ?: W.t("vision_err_photo")
            else -> msg ?: "${W.t("vision_err_http")} (HTTP $code)."
        }
    }
}
