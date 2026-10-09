package com.omastro.app.data.repository

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.GocharRequest
import com.omastro.app.data.remote.GocharResponse
import com.omastro.app.data.remote.OmAstroApiService
import java.io.IOException

class AstroRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {

    suspend fun getKundali(request: KundaliRequest): Result<KundaliResponse> {
        return try {
            val res = api.calculateKundali(request)
            val body = res.body()
            val data = body?.data
            if (res.isSuccessful && body != null && data != null) {
                Result.success(data)
            } else if (res.isSuccessful && body != null) {
                Result.failure(IOException("Kundali request failed: empty data envelope"))
            } else {
                Result.failure(IOException("Kundali request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyHoroscope(sign: String, period: String = "today"): Result<RashifalData> {
        return try {
            val res = api.getRashifal(mapOf("sign" to sign, "period" to period))
            val body = res.body()
            val data = body?.data
            if (res.isSuccessful && body != null && body.success && data != null) {
                Result.success(data)
            } else {
                Result.failure(IOException("Rashifal request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPanchang(date: String? = null): Result<PanchangResponse> {
        return try {
            val params = mutableMapOf<String, String>()
            val dateApi = date ?: PanchangLookups.todayApiDate()
            params["date"] = dateApi
            val res = api.getPanchang(params)
            val body = res.body()
            val uiData = if (res.isSuccessful && body != null && body.success) {
                PanchangLookups.toUiData(body.data, dateApi)
            } else null
            if (uiData != null) {
                Result.success(uiData)
            } else {
                Result.failure(IOException("Panchang request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMuhurat(request: MuhuratRequest): Result<MuhuratData> {
        return try {
            val res = api.getMuhurat(request)
            val body = res.body()
            val data = body?.data
            if (res.isSuccessful && body != null && body.success && data != null) {
                Result.success(data)
            } else {
                Result.failure(IOException("Muhurat request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHora(date: String? = null): Result<HoraData> {
        return try {
            val params = mutableMapOf<String, String>()
            params["date"] = date ?: PanchangLookups.todayApiDate()
            val res = api.getHora(params)
            val body = res.body()
            if (res.isSuccessful && body != null && body.success && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(IOException("Hora request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateMatchmaking(request: MatchmakingRequest): Result<MatchmakingResponse> {
        return try {
            val res = api.calculateMatchmaking(request)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Matchmaking request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGochar(request: GocharRequest): Result<GocharResponse> {
        return try {
            val res = api.getGochar(request)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Gochar request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNumerology(params: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.getNumerology(params)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Numerology request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNumerologyCompatibility(params: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.getNumerologyCompatibility(params)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Compatibility request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNumerologyNameCheck(params: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.getNumerologyNameCheck(params)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Name check request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBnnAnalysis(request: KundaliRequest): Result<Map<String, Any>> {
        return try {
            val res = api.getBnnAnalysis(request)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("BNN analysis request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Typed wrapper around the same POST /kundali/bnn envelope. */
    private suspend fun getReport(
        label: String,
        call: suspend () -> retrofit2.Response<ReportEnvelope>
    ): Result<ReportData> {
        return try {
            val res = call()
            val data = res.body()?.data
            if (res.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(IOException("$label request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPersonality(request: KundaliRequest): Result<ReportData> =
        getReport("Personality") { api.getPersonality(request) }

    suspend fun getBnnFull(request: KundaliRequest): Result<ReportData> =
        getReport("Bhrigu Nadi") { api.getBnnReport(request) }

    suspend fun getLalKitabFull(request: KundaliRequest): Result<ReportData> =
        getReport("Lal Kitab") { api.getLalKitabFull(request) }

    suspend fun getJaimini(request: KundaliRequest): Result<JaiminiData> {
        return try {
            val res = api.getJaimini(request)
            val data = res.body()?.data
            if (res.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(IOException("Jaimini request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAstroHealth(request: KundaliRequest): Result<Map<String, Any>> {
        return try {
            val res = api.getAstroHealth(request)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Astro-health request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
