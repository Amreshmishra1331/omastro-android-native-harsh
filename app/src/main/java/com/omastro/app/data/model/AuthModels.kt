package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * OTP delivery channel for signup verification codes.
 *
 * HOW TO ENABLE MOBILE / WHATSAPP OTP LATER (backend + app, no other app changes needed):
 * 1. Server: make `POST /wp-json/omastro/v1/auth/register`,
 *    `POST .../auth/verify-signup-otp` and `POST .../auth/resend-otp` accept the
 *    `otp_channel` body field ("email" | "mobile" | "whatsapp") and route the OTP
 *    through the chosen channel (SMS gateway / WhatsApp Cloud API).
 * 2. App: change [OtpChannels.DEFAULT] below from [OtpChannels.EMAIL] to the desired
 *    channel, or bind a user-facing selector to `AuthViewModel.otpChannel`.
 *    The channel is already threaded through [RegisterRequest], [VerifySignupOtpRequest]
 *    and [ResendOtpRequest], so nothing else needs to change.
 *
 * Until the server honours the field, it keeps sending the OTP by email.
 */
object OtpChannels {
    const val EMAIL = "email"
    const val MOBILE = "mobile"
    const val WHATSAPP = "whatsapp"

    /** Active channel. Keep [EMAIL] until the backend enables mobile/whatsapp delivery. */
    const val DEFAULT: String = EMAIL
}

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserProfile? = null,
    @SerializedName("message") val message: String? = null
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("mobile") val mobile: String? = null,
    /**
     * OTP delivery channel: "email" | "mobile" | "whatsapp".
     * Defaults to [OtpChannels.EMAIL]. The server ignores it until mobile/whatsapp
     * delivery is enabled — see [OtpChannels] for the enablement steps.
     */
    @SerializedName("otp_channel") val otpChannel: String = OtpChannels.DEFAULT
)

data class RegisterResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null
)

data class VerifySignupOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    /**
     * Must match the channel the OTP was requested on.
     * See [OtpChannels] for how to enable mobile/whatsapp later.
     */
    @SerializedName("otp_channel") val otpChannel: String = OtpChannels.DEFAULT
)

data class ResendOtpRequest(
    @SerializedName("email") val email: String,
    /**
     * OTP delivery channel: "email" | "mobile" | "whatsapp".
     * See [OtpChannels] for how to enable mobile/whatsapp later.
     */
    @SerializedName("otp_channel") val otpChannel: String = OtpChannels.DEFAULT
)

data class VerifySignupOtpResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserProfile? = null,
    @SerializedName("message") val message: String? = null
)

data class UserProfile(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("coin_balance") val coinBalance: Int = 0
)

data class UpdateFcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)
