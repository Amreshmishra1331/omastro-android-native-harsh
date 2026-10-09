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

    // Kundali
    @POST("kundali")
    suspend fun calculateKundali(
        @Body request: KundaliRequest
    ): Response<KundaliResponse>

    // Rashifal (daily horoscope)
    @POST("rashifal")
    suspend fun getRashifal(
        @Body request: Map<String, String>
    ): Response<DailyHoroscopeResponse>

    // Panchang
    @POST("panchang")
    suspend fun getPanchang(
        @Body request: Map<String, String>
    ): Response<PanchangResponse>

    // Matchmaking (Kundali Milan)
    @POST("matching")
    suspend fun calculateMatchmaking(
        @Body request: MatchmakingRequest
    ): Response<MatchmakingResponse>

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
