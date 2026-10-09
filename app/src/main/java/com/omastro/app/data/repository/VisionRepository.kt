package com.omastro.app.data.repository

import com.google.gson.Gson
import com.omastro.app.data.model.PalmistryReading
import com.omastro.app.data.model.VastuAnalysis
import com.omastro.app.data.model.VastuAnalyzeRequest
import com.omastro.app.data.model.VastuPadaInfo
import com.omastro.app.data.model.VisionAnalysis
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

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

    // ------------------------------------------------------------------
    // Vastu
    // ------------------------------------------------------------------

    /** POST /vastu/analyze — classical engine from manual inputs. */
    suspend fun analyzeVastu(request: VastuAnalyzeRequest): Result<VastuAnalysis> {
        return try {
            val res = api.analyzeVastu(request)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty Vastu response."))
            Result.success(VastuAnalysis.fromMap(data))
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
     * Returns the parsed inputs (for editable review) plus the analysis.
     */
    suspend fun analyzeVastuPhoto(
        photo: File,
        notes: String? = null
    ): Result<Pair<Map<String, Any?>, VastuAnalysis>> {
        return try {
            val part = filePart("photo", photo)
            val notesBody = notes?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val res = api.analyzeVastuPhoto(part, notesBody)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty Vastu photo response."))
            @Suppress("UNCHECKED_CAST")
            val photoParse = data["photo_parse"] as? Map<String, Any?> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val analysisMap = data["vastu_analysis"] as? Map<String, Any?>
            Result.success(photoParse to VastuAnalysis.fromMap(analysisMap))
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
            val res = api.analyzeVision(part, qBody, ctxBody)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty vision response."))
            Result.success(VisionAnalysis.fromMap(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** POST /palmistry/analyze — palm photo → structured reading. */
    suspend fun analyzePalmistry(
        image: File,
        hand: String? = null,
        notes: String? = null
    ): Result<PalmistryReading> {
        return try {
            val part = filePart("image", image)
            val handBody = hand?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val notesBody = notes?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val res = api.analyzePalmistry(part, handBody, notesBody)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty palmistry response."))
            Result.success(PalmistryReading.fromMap(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** POST /face-reading/analyze — face photo → Samudrika Shastra reading. */
    suspend fun analyzeFaceReading(
        image: File,
        notes: String? = null
    ): Result<FaceReading> {
        return try {
            val part = filePart("image", image)
            val notesBody = notes?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val res = api.analyzeFaceReading(part, notesBody)
            val body = res.body()
            // The endpoint is still being built server-side: surface an honest
            // "not available yet" instead of a raw HTTP 404.
            if (res.code() == 404) {
                return Result.failure(
                    IOException(com.omastro.app.ui.i18n.W.t("face_err_unavailable"))
                )
            }
            if (!res.isSuccessful || body == null) {
                return Result.failure(IOException(parseError(res.code(), body)))
            }
            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Empty face-reading response."))
            Result.success(FaceReading.fromMap(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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
        return when (code) {
            401 -> "Please sign in to use this feature."
            429 -> "Too many photo analyses. Please wait a minute and try again."
            422 -> msg ?: "The photo could not be understood. Try a clearer photo."
            else -> msg ?: "Request failed (HTTP $code)."
        }
    }
}
