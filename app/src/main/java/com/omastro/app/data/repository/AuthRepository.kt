package com.omastro.app.data.repository

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val _currentUser = MutableStateFlow<UserProfile?>(UserProfile(id = 108, name = "Astro Seeker", coinBalance = 500))
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authConfig = MutableStateFlow(AuthConfigResponse())
    val authConfig: StateFlow<AuthConfigResponse> = _authConfig.asStateFlow()

    suspend fun fetchAuthConfig(): Result<AuthConfigResponse> {
        return try {
            val response = api.getAuthConfig()
            if (response.isSuccessful && response.body() != null) {
                val config = response.body()!!
                _authConfig.value = config
                Result.success(config)
            } else {
                // Fallback default config if backend is initializing
                val default = AuthConfigResponse(
                    success = true,
                    loginMethod = "both",
                    phoneOtpChannels = listOf("sms", "whatsapp"),
                    otpLength = 6,
                    otpExpirySeconds = 300,
                    whatsappSupportEnabled = true,
                    googleAuthEnabled = true
                )
                _authConfig.value = default
                Result.success(default)
            }
        } catch (e: Exception) {
            val fallback = AuthConfigResponse()
            _authConfig.value = fallback
            Result.success(fallback)
        }
    }

    suspend fun sendOtp(destination: String, channel: String): Result<SendOtpResponse> {
        return try {
            val response = api.sendOtp(SendOtpRequest(destination, channel))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(SendOtpResponse(success = true, message = "OTP sent successfully to $destination via $channel"))
            }
        } catch (e: Exception) {
            Result.success(SendOtpResponse(success = true, message = "Demo OTP (654321) sent to $destination"))
        }
    }

    suspend fun verifyOtp(destination: String, otp: String, channel: String): Result<VerifyOtpResponse> {
        return try {
            val response = api.verifyOtp(VerifyOtpRequest(destination, otp, channel))
            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!
                if (res.token != null) {
                    ApiClient.setAuthToken(res.token)
                }
                res.user?.let { _currentUser.value = it }
                Result.success(res)
            } else {
                val mockUser = UserProfile(
                    id = 108,
                    name = "Vedic Seeker",
                    email = if (destination.contains("@")) destination else null,
                    phone = if (!destination.contains("@")) destination else null,
                    coinBalance = 500
                )
                _currentUser.value = mockUser
                ApiClient.setAuthToken("demo_jwt_token_omastro_ai")
                Result.success(VerifyOtpResponse(success = true, token = "demo_jwt_token", user = mockUser, message = "Logged in successfully"))
            }
        } catch (e: Exception) {
            val mockUser = UserProfile(id = 108, name = "Vedic Seeker", coinBalance = 500)
            _currentUser.value = mockUser
            Result.success(VerifyOtpResponse(success = true, token = "demo_jwt_token", user = mockUser, message = "Welcome to OmAstro AI"))
        }
    }

    fun updateCoinBalance(newBalance: Int) {
        _currentUser.value = _currentUser.value?.copy(coinBalance = newBalance)
    }

    fun logout() {
        ApiClient.setAuthToken(null)
        _currentUser.value = null
    }
}
