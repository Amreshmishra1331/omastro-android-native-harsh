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
    @SerializedName("password") val password: String,
    /**
     * "Remember Password" checkbox (Flutter login). When true the server
     * issues a 90-day token instead of the default 60-day token.
     */
    @SerializedName("remember") val remember: Boolean = false
)

/**
 * Real API envelope for POST /auth/login:
 * {"success": true, "data": {"token": "...", "user_id": 123, "display_name": "...", ...}}
 * The session token lives inside `data`, NOT at the top level.
 */
data class LoginResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: LoginData? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("code") val code: String? = null
)

data class LoginData(
    @SerializedName("token") val token: String? = null,
    @SerializedName("expires") val expires: Long? = null,
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("avatar") val avatar: String? = null
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

/**
 * Real API envelope for POST /auth/verify-signup-otp — same {"success","data"} shape
 * as login; the session token lives inside `data`.
 */
data class VerifySignupOtpResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: LoginData? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("code") val code: String? = null
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

// ─── Google Sign-In (POST /auth/firebase) ────────────────────────────────
// The app obtains a Google ID token via Credential Manager and posts it as
// `firebase_id_token`. The server verifies the token (Google OAuth path) and
// returns the OmAstro session token. Response envelope:
// {"success": true, "data": {"token": ..., "user_id": ..., ...}, "meta": {...}}

data class FirebaseAuthRequest(
    @SerializedName("firebase_id_token") val firebaseIdToken: String,
    @SerializedName("fcm_token") val fcmToken: String? = null
)

data class FirebaseAuthData(
    @SerializedName("token") val token: String? = null,
    @SerializedName("expires") val expires: Long? = null,
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("email") val email: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("is_new_user") val isNewUser: Boolean = false
)

data class FirebaseAuthEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: FirebaseAuthData? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("code") val code: String? = null
)
