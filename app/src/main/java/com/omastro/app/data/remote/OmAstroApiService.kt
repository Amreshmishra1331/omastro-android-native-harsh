package com.omastro.app.data.remote

import com.google.gson.annotations.SerializedName
import com.omastro.app.data.model.*
import okhttp3.MultipartBody
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
        @Body request: ResendOtpRequest
    ): Response<Map<String, Any>>

    /**
     * Google Sign-In → WordPress login/register.
     * Posts the Google ID token as `firebase_id_token`; the server verifies it
     * (Google OAuth path) and returns the OmAstro session token.
     * Envelope: {"success": true, "data": {"token": ..., "user_id": ..., ...}}
     */
    @POST("auth/firebase")
    suspend fun firebaseAuth(
        @Body request: FirebaseAuthRequest
    ): Response<FirebaseAuthEnvelope>

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

    // Astrologers — real envelope: {"success":true,"data":{"astrologers":[...]}}
    @GET("astrologers")
    suspend fun getAstrologers(
        @Query("category") category: String? = null
    ): Response<AstrologersResponse>

    // Astrologer chat — real backend contract (verified 2026-10-09):
    //  open  POST {slug}            -> {success,data:{registered,session_id}}
    //  history GET ?session_id=&astrologer_slug= -> {success,data:{messages,astrologer,birth_context}}
    //  close POST {session_id}
    @GET("astrologers/chat/history")
    suspend fun getChatHistory(
        @Query("session_id") sessionId: String? = null,
        @Query("astrologer_slug") astrologerSlug: String? = null
    ): Response<ChatHistoryResponse>

    @POST("astrologers/chat/open")
    suspend fun openChat(
        @Body request: OpenChatRequest
    ): Response<OpenChatResponse>

    @POST("astrologers/chat/close")
    suspend fun closeChat(
        @Body request: CloseChatRequest
    ): Response<Map<String, Any>>

    // Astrologer consultation billing session (real per-minute billing).
    // start/tick/end -> {success,data:{session_uuid,rate_paise,billed_seconds,
    //   free_seconds_used,free_seconds_left,wallet_total_paise,wallet_covers_sec,status}}
    // NOTE: insufficient balance is signalled as success:true with data.insufficient=true
    // (the AJAX error is re-wrapped by kg_rest_astro_bridge) — check the flag, not HTTP status.
    @POST("astrologers/session/start")
    suspend fun startAstroSession(
        @Body request: AstroSessionStartRequest
    ): Response<AstroSessionResponse>

    @POST("astrologers/session/tick")
    suspend fun tickAstroSession(
        @Body request: AstroSessionTickRequest
    ): Response<AstroSessionResponse>

    @POST("astrologers/session/end")
    suspend fun endAstroSession(
        @Body request: AstroSessionTickRequest
    ): Response<AstroSessionResponse>

    // Astrologer review — POST {slug, stars 1-5, review <=500 chars, session_id}.
    // One review per session (UNIQUE on session_id, server-enforced).
    @POST("astrologers/review")
    suspend fun submitAstrologerReview(
        @Body request: AstrologerReviewRequest
    ): Response<Map<String, Any>>

    // Wallet (real INR wallet)
    @GET("wallet/balance")
    suspend fun getWalletBalance(): Response<WalletBalanceResponse>

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

    // AI Chat saved sessions
    // GET /ai/chats -> list saved chat sessions (50 latest)
    @GET("ai/chats")
    suspend fun listAiChats(): Response<SavedChatListResponse>

    // POST /ai/chats -> save/update a chat session
    @POST("ai/chats")
    suspend fun saveAiChat(
        @Body request: SaveChatRequest
    ): Response<Map<String, Any>>

    // GET /ai/chats/{id} -> get one saved chat (history + context)
    @GET("ai/chats/{id}")
    suspend fun getAiChat(
        @Path("id") chatId: String
    ): Response<SavedChatDetailResponse>

    // DELETE /ai/chats/{id} -> delete one chat
    @DELETE("ai/chats/{id}")
    suspend fun deleteAiChat(
        @Path("id") chatId: String
    ): Response<Map<String, Any>>

    // DELETE /ai/chats -> clear all chats
    @DELETE("ai/chats")
    suspend fun clearAiChats(): Response<Map<String, Any>>

    // User profile (website dashboard parity)
    // GET /me -> user info with plan and chat quota
    @GET("me")
    suspend fun getMyProfile(): Response<MeResponse>

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

    // Notifications (website parity: bell icon + list + mark-read + clear)
    @GET("notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @POST("notifications/mark-read")
    suspend fun markNotificationsRead(
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any>>

    @POST("notifications/clear")
    suspend fun clearNotifications(): Response<Map<String, Any>>

    // Dedicated kundali report endpoints (typed envelopes, verified live 2026-10-09).
    // ReportEnvelope/JaiminiEnvelope are defined in data/model/KundaliModels.kt
    // (committed) — the typed envelopes are required by AstroRepository's
    // `.data` access.
    // NOTE: POST kundali/bnn is declared ONCE (getBnnReport). The older
    // Map-based duplicate was removed 2026-10-09 (no callers).
    @POST("kundali/personality")
    suspend fun getPersonality(
        @Body request: KundaliRequest
    ): Response<ReportEnvelope>

    @POST("kundali/bnn")
    suspend fun getBnnReport(
        @Body request: KundaliRequest
    ): Response<ReportEnvelope>

    @POST("kundali/lal-kitab")
    suspend fun getLalKitabFull(
        @Body request: KundaliRequest
    ): Response<ReportEnvelope>

    @POST("kundali/jaimini")
    suspend fun getJaimini(
        @Body request: KundaliRequest
    ): Response<JaiminiEnvelope>

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
    // Backend: GET /geocode/search?q=<query> -> {success, data: {results: [...]}}
    @GET("geocode/search")
    suspend fun searchPlaces(
        @Query("q") query: String
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

    // ------------------------------------------------------------------
    // Vastu Shastra engine + Vision / Photo AI (NEW world-class systems)
    // ------------------------------------------------------------------

    /** POST /vastu/analyze — classical Vastu analysis from manual inputs (auth). */
    @POST("vastu/analyze")
    suspend fun analyzeVastu(
        @Body request: com.omastro.app.data.model.VastuAnalyzeRequest
    ): Response<Map<String, Any>>

    /** GET /vastu/reference — directions, pada chart, ideal placements (public). */
    @GET("vastu/reference")
    suspend fun getVastuReference(): Response<Map<String, Any>>

    /** POST /vastu/analyze-photo — house-map photo → vision parse → engine (auth, multipart). */
    @Multipart
    @POST("vastu/analyze-photo")
    suspend fun analyzeVastuPhoto(
        @Part photo: MultipartBody.Part,
        @Part("notes") notes: okhttp3.RequestBody? = null
    ): Response<Map<String, Any>>

    /** POST /ai/vision — image + question → Gemini vision analysis + parsed_facts (auth, multipart). */
    @Multipart
    @POST("ai/vision")
    suspend fun analyzeVision(
        @Part image: MultipartBody.Part,
        @Part("question") question: okhttp3.RequestBody,
        @Part("context") context: okhttp3.RequestBody? = null
    ): Response<Map<String, Any>>

    /** POST /palmistry/analyze — palm photo → structured reading (auth, multipart). */
    @Multipart
    @POST("palmistry/analyze")
    suspend fun analyzePalmistry(
        @Part image: MultipartBody.Part,
        @Part("hand") hand: okhttp3.RequestBody? = null,
        @Part("notes") notes: okhttp3.RequestBody? = null
    ): Response<Map<String, Any>>

    /**
     * POST /face-reading/analyze — face photo → Samudrika Shastra reading
     * (auth, multipart). Returns per-feature observation + interpretation,
     * overall_personality, career, relationships, health_indications, guidance,
     * parsed_facts, and an honesty refusal when no clear face is visible.
     */
    @Multipart
    @POST("face-reading/analyze")
    suspend fun analyzeFaceReading(
        @Part image: MultipartBody.Part,
        @Part("notes") notes: okhttp3.RequestBody? = null
    ): Response<Map<String, Any>>
}

// (Astrologer chat session models now live in
//  com.omastro.app.data.model.AstrologerModels — OpenChatRequest/Response,
//  CloseChatRequest, AstroSessionStartRequest, AstroSessionTickRequest,
//  AstroSessionState/Response, AstrologerReviewRequest, ChatHistoryResponse.)

// Geocode place search models (GET /geocode/search?q=)
// Real response: {"success":true,"data":{"results":[{"name","country","state","display","lat","lon","tz"}]}}
data class GeocodePlace(
    @SerializedName("name") val name: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("country") val country: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("display") val display: String? = null,
    @SerializedName("tz") val tz: Double? = null
) {
    /** Human-readable display name: prefers server "display", falls back to "City, State, Country". */
    val displayName: String
        get() = display?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(name, state, country)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
}

data class GeocodeSearchData(
    @SerializedName("results") val results: List<GeocodePlace> = emptyList()
)

data class GeocodeSearchResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: GeocodeSearchData? = null,
    // Legacy flat keys (kept for tolerance)
    @SerializedName("places") val places: List<GeocodePlace> = emptyList(),
    @SerializedName("results") val results: List<GeocodePlace>? = null,
    @SerializedName("message") val message: String? = null
) {
    /** Accepts data.results (real), or legacy flat "places"/"results" keys. */
    val allPlaces: List<GeocodePlace>
        get() = data?.results?.takeIf { it.isNotEmpty() }
            ?: if (places.isNotEmpty()) places else (results ?: emptyList())
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
