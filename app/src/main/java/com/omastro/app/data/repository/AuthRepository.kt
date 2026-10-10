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

    suspend fun login(email: String, password: String, remember: Boolean = false): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password, remember))
            val body = response.body()
            val token = body?.data?.token
            if (response.isSuccessful && body != null && !token.isNullOrBlank()) {
                ApiClient.setAuthToken(token)
                body.data?.let { d ->
                    _currentUser.value = UserProfile(
                        id = d.userId,
                        name = d.displayName ?: d.username ?: "",
                        email = d.email,
                        phone = d.mobile
                    )
                }
                Result.success(body)
            } else {
                val msg = body?.message?.takeIf { it.isNotBlank() }
                    ?: "Login failed: HTTP ${response.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        mobile: String? = null,
        otpChannel: String = OtpChannels.DEFAULT
    ): Result<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, password, mobile, otpChannel))
            val body = response.body()
            if (response.isSuccessful && body != null && body.success) {
                Result.success(body)
            } else {
                val msg = body?.message?.takeIf { it.isNotBlank() }
                    ?: "Registration failed: HTTP ${response.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifySignupOtp(
        email: String,
        otp: String,
        otpChannel: String = OtpChannels.DEFAULT
    ): Result<VerifySignupOtpResponse> {
        return try {
            val response = api.verifySignupOtp(VerifySignupOtpRequest(email, otp, otpChannel))
            val body = response.body()
            val token = body?.data?.token
            if (response.isSuccessful && body != null && !token.isNullOrBlank()) {
                ApiClient.setAuthToken(token)
                body.data?.let { d ->
                    _currentUser.value = UserProfile(
                        id = d.userId,
                        name = d.displayName ?: d.username ?: "",
                        email = d.email,
                        phone = d.mobile
                    )
                }
                Result.success(body)
            } else {
                val msg = body?.message?.takeIf { it.isNotBlank() }
                    ?: "OTP verification failed: HTTP ${response.code()}"
                Result.failure(IOException(msg))
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

    suspend fun resendSignupOtp(
        email: String,
        otpChannel: String = OtpChannels.DEFAULT
    ): Result<Unit> {
        return try {
            val response = api.resendOtp(ResendOtpRequest(email, otpChannel))
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

    /**
     * Google Sign-In: posts the Google ID token to POST /auth/firebase.
     * The server verifies the token and returns the OmAstro session token.
     * On success the token is persisted and the user profile is updated.
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseAuthData> {
        return try {
            val response = api.firebaseAuth(FirebaseAuthRequest(firebaseIdToken = idToken))
            val body = response.body()
            val data = body?.data
            if (response.isSuccessful && body?.success == true && data?.token != null) {
                ApiClient.setAuthToken(data.token)
                _currentUser.value = UserProfile(
                    id = data.userId,
                    name = data.displayName ?: "",
                    email = data.email
                )
                Result.success(data)
            } else {
                val msg = body?.message ?: "Google sign-in failed: HTTP ${response.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    /**
     * Delete the signed-in account via DELETE /me (Bearer auth, requires the
     * DELETE confirm phrase). Success is `{"deleted":true}`. API call only —
     * local-data wiping is orchestrated by
     * [com.omastro.app.di.AppContainer.deleteAccountEverywhere] so every
     * repository stays the owner of its own state.
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val res = api.deleteAccount(DeleteAccountRequest(DELETE_ACCOUNT_CONFIRM_PHRASE))
            val body = res.body()
            if (res.isSuccessful && body != null && body.deleted) {
                Result.success(Unit)
            } else {
                val msg = body?.message?.takeIf { it.isNotBlank() }
                    ?: "Account deletion failed: HTTP ${res.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
