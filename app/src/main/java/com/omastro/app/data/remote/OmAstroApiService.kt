package com.omastro.app.data.remote

import com.omastro.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface OmAstroApiService {

    // Dynamic Auth Configuration & Login
    @GET("auth/config")
    suspend fun getAuthConfig(): Response<AuthConfigResponse>

    @POST("auth/send-otp")
    suspend fun sendOtp(
        @Body request: SendOtpRequest
    ): Response<SendOtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>

    // Kundali & Horoscopes
    @POST("kundali")
    suspend fun calculateKundali(
        @Body request: KundaliRequest
    ): Response<KundaliResponse>

    @GET("horoscope/daily")
    suspend fun getDailyHoroscope(
        @Query("sign") sign: String,
        @Query("period") period: String = "today"
    ): Response<DailyHoroscopeResponse>

    // Panchang & Muhurat
    @GET("panchang")
    suspend fun getPanchang(
        @Query("date") date: String? = null
    ): Response<PanchangResponse>

    // Matchmaking (Kundali Milan)
    @POST("matchmaking")
    suspend fun calculateMatchmaking(
        @Body request: MatchmakingRequest
    ): Response<MatchmakingResponse>

    // Astrologers & Consultations
    @GET("astrologers")
    suspend fun getAstrologers(
        @Query("category") category: String? = null
    ): Response<List<Astrologer>>

    @GET("chat/{astrologerId}/history")
    suspend fun getChatHistory(
        @Path("astrologerId") astrologerId: String
    ): Response<List<ChatMessage>>

    @POST("chat/send")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<ChatMessage>

    // Wallet & Coin Recharge
    @GET("wallet/balance")
    suspend fun getWalletBalance(): Response<WalletBalanceResponse>

    @POST("wallet/order")
    suspend fun createRechargeOrder(
        @Body request: RazorpayOrderRequest
    ): Response<RazorpayOrderResponse>

    @POST("wallet/verify")
    suspend fun verifyPayment(
        @Body request: PaymentVerifyRequest
    ): Response<PaymentVerifyResponse>
}
