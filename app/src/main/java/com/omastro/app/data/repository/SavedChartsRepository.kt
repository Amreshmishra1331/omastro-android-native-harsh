package com.omastro.app.data.repository

import com.omastro.app.data.model.KundaliRequest
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import com.omastro.app.data.remote.SaveChartRequest
import com.omastro.app.data.remote.SavedChart
import java.io.IOException

/**
 * Saved kundali charts backed by GET/POST/DELETE /me/charts.
 * Charts are stored server-side per user; nothing is fabricated locally.
 */
class SavedChartsRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    suspend fun getSavedCharts(): Result<List<SavedChart>> {
        return try {
            val res = api.getSavedCharts()
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(
                    IOException("Could not load saved charts (HTTP ${res.code()}).")
                )
            }
            Result.success(body.allCharts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveChart(
        name: String,
        request: KundaliRequest
    ): Result<SavedChart?> {
        return try {
            val payload = SaveChartRequest(
                name = name,
                birthDate = request.dob,
                birthTime = request.tob,
                birthPlace = request.pob,
                latitude = request.lat,
                longitude = request.lon,
                gender = request.gender
            )
            val res = api.saveChart(payload)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(
                    IOException("Could not save chart (HTTP ${res.code()}).")
                )
            }
            if (!body.success && body.chart == null) {
                return Result.failure(
                    IOException(body.message ?: "Could not save chart.")
                )
            }
            Result.success(body.chart)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChart(chartId: String): Result<Unit> {
        return try {
            val res = api.deleteChart(chartId)
            if (!res.isSuccessful) {
                return Result.failure(
                    IOException("Could not delete chart (HTTP ${res.code()}).")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
