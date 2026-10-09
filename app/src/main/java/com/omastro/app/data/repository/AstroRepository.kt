package com.omastro.app.data.repository

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import java.io.IOException

class AstroRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {

    suspend fun getKundali(request: KundaliRequest): Result<KundaliResponse> {
        return try {
            val res = api.calculateKundali(request)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Kundali request failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyHoroscope(sign: String, period: String = "today"): Result<DailyHoroscopeResponse> {
        return try {
            val res = api.getRashifal(mapOf("sign" to sign, "period" to period))
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
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
            date?.let { params["date"] = it }
            val res = api.getPanchang(params)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Panchang request failed: HTTP ${res.code()}"))
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
