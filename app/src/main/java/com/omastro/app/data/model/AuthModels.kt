package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class AuthConfigResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("login_method") val loginMethod: String = "both", // "phone_only", "email_only", "both"
    @SerializedName("phone_otp_channels") val phoneOtpChannels: List<String> = listOf("sms", "whatsapp"),
    @SerializedName("otp_length") val otpLength: Int = 6,
    @SerializedName("otp_expiry_seconds") val otpExpirySeconds: Int = 300,
    @SerializedName("whatsapp_support_enabled") val whatsappSupportEnabled: Boolean = true,
    @SerializedName("google_auth_enabled") val googleAuthEnabled: Boolean = true
)

data class SendOtpRequest(
    @SerializedName("destination") val destination: String,
    @SerializedName("channel") val channel: String = "sms" // "sms", "whatsapp", "email"
)

data class SendOtpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("request_id") val requestId: String? = null
)

data class VerifyOtpRequest(
    @SerializedName("destination") val destination: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("channel") val channel: String = "sms"
)

data class UserProfile(
    @SerializedName("id") val id: Long = 1,
    @SerializedName("name") val name: String = "Astro Seeker",
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("coin_balance") val coinBalance: Int = 500
)

data class VerifyOtpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: UserProfile?,
    @SerializedName("message") val message: String?
)

data class UpdateFcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)
