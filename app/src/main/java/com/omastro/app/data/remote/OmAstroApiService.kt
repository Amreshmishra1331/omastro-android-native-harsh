package com.omastro.app.data.remote

import com.google.gson.annotations.SerializedName
import com.omastro.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface OmAstroApiService {

    // Authentication (real backend contract)
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("auth/verify-signup-otp")
    suspend fun verifySignupOtp(
        @Body request: VerifySignupOtpRequest
    ): Response<VerifySignupOtpResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("auth/resend-otp")
    suspend fun resendOtp(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    // Kundali (envelope: {"success": true, "data": {...}})
    @POST("kundali")
    suspend fun calculateKundali(
        @Body request: KundaliRequest
    ): Response<KundaliEnvelope>

    // Rashifal (daily horoscope) — envelope: {"success":true,"data":{...}}
    @POST("rashifal")
    suspend fun getRashifal(
        @Body request: Map<String, String>
    ): Response<RashifalEnvelope>

    // Panchang (real contract: {"success":true,"data":{"core_data":{...},"analysis_text":"..."}})
    @POST("panchang")
    suspend fun getPanchang(
        @Body request: Map<String, String>
    ): Response<PanchangEnvelope>

    // Hora — planetary hours (real contract: {"success":true,"data":{"day":[...],"night":[...]}})
    @POST("hora")
    suspend fun getHora(
        @Body request: Map<String, String>
    ): Response<HoraEnvelope>

    // Matchmaking (Kundali Milan)
    @POST("matching")
    suspend fun calculateMatchmaking(
        @Body request: MatchmakingRequest
    ): Response<MatchmakingResponse>

    // Gochar / Planetary Transits (birth chart + transit date analysis)
    @POST("gochar")
    suspend fun getGochar(
        @Body request: GocharRequest
    ): Response<GocharResponse>

    // Astrologers
    @GET("astrologers")
    suspend fun getAstrologers(
        @Query("category") category: String? = null
    ): Response<List<Astrologer>>

    @GET("astrologers/chat/history")
    suspend fun getChatHistory(
        @Query("astrologer_id") astrologerId: String
    ): Response<List<ChatMessage>>

    @POST("astrologers/chat/open")
    suspend fun openChat(
        @Body request: OpenChatRequest
    ): Response<OpenChatResponse>

    @POST("astrologers/chat/close")
    suspend fun closeChat(
        @Body request: CloseChatRequest
    ): Response<Map<String, Any>>

    // Astrologer consultation billing session (real per-minute billing)
    @POST("astrologers/session/start")
    suspend fun startAstroSession(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("astrologers/session/tick")
    suspend fun tickAstroSession(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("astrologers/session/end")
    suspend fun endAstroSession(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    // Wallet (real INR wallet)
    @GET("wallet/transactions")
    suspend fun getWalletTransactions(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<WalletTransaction>>

    @POST("wallet/topup-order")
    suspend fun createTopupOrder(
        @Body request: TopupOrderRequest
    ): Response<TopupOrderResponse>

    @POST("wallet/topup-verify")
    suspend fun verifyTopup(
        @Body request: TopupVerifyRequest
    ): Response<TopupVerifyResponse>

    // AI Chat (personas: general, divine, mundane, ...)
    @POST("ai/chat")
    suspend fun aiChat(
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any>>

    // FCM Device Token Registration
    @POST("auth/fcm-token")
    suspend fun updateFcmToken(
        @Body request: UpdateFcmTokenRequest
    ): Response<Map<String, Any>>

    // Astro Health Deep Analysis
    @POST("astro-health")
    suspend fun getAstroHealth(
        @Body request: KundaliRequest
    ): Response<Map<String, Any>>

    // BNN Deep Analysis
    @POST("kundali/bnn")
    suspend fun getBnnAnalysis(
        @Body request: KundaliRequest
    ): Response<Map<String, Any>>

    // Numerology
    @POST("numerology")
    suspend fun getNumerology(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    // Numerology compatibility (two-person number matching)
    @POST("numerology/compatibility")
    suspend fun getNumerologyCompatibility(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    // Numerology name check (expression / soul urge / personality numbers)
    @POST("numerology/name-check")
    suspend fun getNumerologyNameCheck(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    // Geocode place search (city autocomplete for birth places)
    @POST("geocode/search")
    suspend fun searchPlaces(
        @Body request: GeocodeSearchRequest
    ): Response<GeocodeSearchResponse>

    // Muhurat (auspicious dates for life events)
    @POST("muhurat")
    suspend fun getMuhurat(
        @Body request: com.omastro.app.data.model.MuhuratRequest
    ): Response<com.omastro.app.data.model.MuhuratResponse>

    // Saved Charts (user's saved kundalis)
    @GET("me/charts")
    suspend fun getSavedCharts(): Response<SavedChartsResponse>

    @POST("me/charts")
    suspend fun saveChart(
        @Body request: SaveChartRequest
    ): Response<SaveChartResponse>

    @DELETE("me/charts/{id}")
    suspend fun deleteChart(
        @Path("id") chartId: String
    ): Response<Map<String, Any>>
}

// Astrologer chat session models (real backend contract)
data class OpenChatRequest(
    @SerializedName("astrologer_id") val astrologerId: String
)

data class OpenChatResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("message") val message: String? = null
)

data class CloseChatRequest(
    @SerializedName("astrologer_id") val astrologerId: String,
    @SerializedName("session_id") val sessionId: String? = null
)

// Geocode place search models (POST /geocode/search)
data class GeocodeSearchRequest(
    @SerializedName("query") val query: String
)

data class GeocodePlace(
    @SerializedName("name") val name: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("country") val country: String? = null,
    @SerializedName("state") val state: String? = null
) {
    /** Human-readable display name: "City, State, Country" (parts that exist). */
    val displayName: String
        get() = listOfNotNull(name, state, country)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
}

data class GeocodeSearchResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("places") val places: List<GeocodePlace> = emptyList(),
    @SerializedName("results") val results: List<GeocodePlace>? = null,
    @SerializedName("message") val message: String? = null
) {
    /** Accepts either "places" or "results" envelope keys. */
    val allPlaces: List<GeocodePlace>
        get() = if (places.isNotEmpty()) places else (results ?: emptyList())
}

// Saved Charts models (GET/POST/DELETE /me/charts)
data class SavedChart(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("birth_date") val birthDate: String = "",
    @SerializedName("birth_time") val birthTime: String = "",
    @SerializedName("birth_place") val birthPlace: String = "",
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("gender") val gender: String = "male",
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    /** Convert to KundaliRequest for loading the chart. */
    fun toKundaliRequest(): com.omastro.app.data.model.KundaliRequest {
        val dob = birthDate.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) } ?: "1990-01-01"
        val tobParts = birthTime.split(":")
        val hr = tobParts.getOrNull(0)?.toIntOrNull() ?: 8
        val mn = tobParts.getOrNull(1)?.toIntOrNull() ?: 30
        return com.omastro.app.data.model.KundaliRequest(
            name = name,
            dob = dob,
            tobHr = hr,
            tobMn = mn,
            tob = "%02d:%02d".format(hr, mn),
            pob = birthPlace.ifBlank { "New Delhi" },
            lat = latitude.takeIf { it != 0.0 } ?: 28.6139,
            lon = longitude.takeIf { it != 0.0 } ?: 77.2090,
            gender = gender
        )
    }
}

data class SavedChartsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("charts") val charts: List<SavedChart> = emptyList(),
    @SerializedName("data") val data: List<SavedChart>? = null,
    @SerializedName("message") val message: String? = null
) {
    val allCharts: List<SavedChart>
        get() = if (charts.isNotEmpty()) charts else (data ?: emptyList())
}

data class SaveChartRequest(
    @SerializedName("name") val name: String,
    @SerializedName("birth_date") val birthDate: String,
    @SerializedName("birth_time") val birthTime: String,
    @SerializedName("birth_place") val birthPlace: String = "",
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("gender") val gender: String = "male"
)

data class SaveChartResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("chart") val chart: SavedChart? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("message") val message: String? = null
)

// Gochar / Planetary Transits models (POST /gochar)
// Birth details drive the natal chart; transit_date selects the transit period.
data class GocharRequest(
    @SerializedName("name") val name: String,
    @SerializedName("dob") val dob: String, // YYYY-MM-DD
    @SerializedName("tob") val tob: String = "08:30", // HH:MM
    @SerializedName("pob") val pob: String = "New Delhi",
    @SerializedName("lat") val lat: Double = 28.6139,
    @SerializedName("lon") val lon: Double = 77.2090,
    @SerializedName("gender") val gender: String = "male",
    @SerializedName("transit_date") val transitDate: String // YYYY-MM-DD
)

// Defensive envelope: {success, data{...}}. Nested detail blocks are kept as
// generic maps because the backend transit payload shape is rich and evolving;
// the screen renders only keys that are present, never invented values.
data class GocharResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: Map<String, Any>? = null
)
