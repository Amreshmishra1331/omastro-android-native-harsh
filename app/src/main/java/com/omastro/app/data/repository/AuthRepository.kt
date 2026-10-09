package com.omastro.app.data.repository

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class AuthRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            val body = response.body()
            if (response.isSuccessful && body != null && body.token != null) {
                ApiClient.setAuthToken(body.token)
                body.user?.let { _currentUser.value = it }
                Result.success(body)
            } else {
                Result.failure(IOException("Login failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        mobile: String? = null
    ): Result<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, password, mobile))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Registration failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifySignupOtp(email: String, otp: String): Result<VerifySignupOtpResponse> {
        return try {
            val response = api.verifySignupOtp(VerifySignupOtpRequest(email, otp))
            val body = response.body()
            if (response.isSuccessful && body != null && body.token != null) {
                ApiClient.setAuthToken(body.token)
                body.user?.let { _currentUser.value = it }
                Result.success(body)
            } else {
                Result.failure(IOException("OTP verification failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val response = api.forgotPassword(mapOf("email" to email))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Request failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendSignupOtp(email: String): Result<Unit> {
        return try {
            val response = api.resendOtp(mapOf("email" to email))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Request failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateCoinBalance(newBalance: Int) {
        _currentUser.value = _currentUser.value?.copy(coinBalance = newBalance)
    }

    suspend fun updateFcmToken(token: String): Result<Boolean> {
        return try {
            val response = api.updateFcmToken(UpdateFcmTokenRequest(token))
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        ApiClient.setAuthToken(null)
        _currentUser.value = null
    }
}
