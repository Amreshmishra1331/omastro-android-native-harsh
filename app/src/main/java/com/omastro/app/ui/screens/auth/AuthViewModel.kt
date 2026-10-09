package com.omastro.app.ui.screens.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.AuthToken
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Authentication flow states.
 *
 * Backend contract (https://www.omastroai.com/wp-json/omastro/v1):
 *  - POST /auth/login {email, password} -> {token}
 *  - POST /auth/register {name, email, password, mobile?} -> signup email OTP sent
 *  - POST /auth/verify-signup-otp {email, otp} -> {token}
 *  - POST /auth/forgot-password {email} -> reset instructions sent
 *
 * Repository signatures consumed here (implemented on the data branch):
 *  - AuthRepository.login(email, password): Result<AuthToken>
 *  - AuthRepository.register(name, email, password, mobile: String?): Result<Unit>
 *  - AuthRepository.verifySignupOtp(email, otp): Result<AuthToken>
 *  - AuthRepository.forgotPassword(email): Result<Unit>
 * where AuthToken exposes the bearer token string via `token`.
 */
enum class AuthMode {
    LOGIN,
    SIGNUP,
    VERIFY_SIGNUP_OTP,
    FORGOT_PASSWORD
}

class AuthViewModel : ViewModel() {

    private val authRepository = AppContainer.authRepository

    private val _mode = MutableStateFlow(AuthMode.LOGIN)
    val mode: StateFlow<AuthMode> = _mode.asStateFlow()

    // --- Login fields ---
    private val _loginEmail = MutableStateFlow("")
    val loginEmail: StateFlow<String> = _loginEmail.asStateFlow()
    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    // --- Signup fields ---
    private val _signupName = MutableStateFlow("")
    val signupName: StateFlow<String> = _signupName.asStateFlow()
    private val _signupEmail = MutableStateFlow("")
    val signupEmail: StateFlow<String> = _signupEmail.asStateFlow()
    private val _signupMobile = MutableStateFlow("")
    val signupMobile: StateFlow<String> = _signupMobile.asStateFlow()
    private val _signupPassword = MutableStateFlow("")
    val signupPassword: StateFlow<String> = _signupPassword.asStateFlow()
    private val _signupConfirmPassword = MutableStateFlow("")
    val signupConfirmPassword: StateFlow<String> = _signupConfirmPassword.asStateFlow()

    // --- OTP / forgot fields ---
    private val _otpEmail = MutableStateFlow("")
    val otpEmail: StateFlow<String> = _otpEmail.asStateFlow()
    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()
    private val _forgotEmail = MutableStateFlow("")
    val forgotEmail: StateFlow<String> = _forgotEmail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Inline form/server error shown under the active form. Null = no error. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** One-shot informational message (e.g. "OTP sent", "Reset email sent"). */
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    /** Seconds remaining before the OTP can be resent. 0 = allowed. */
    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown.asStateFlow()
    private var cooldownJob: Job? = null

    // ---------- field setters ----------

    fun setLoginEmail(v: String) { _loginEmail.value = v; clearMessages() }
    fun setLoginPassword(v: String) { _loginPassword.value = v; clearMessages() }
    fun setSignupName(v: String) { _signupName.value = v; clearMessages() }
    fun setSignupEmail(v: String) { _signupEmail.value = v; clearMessages() }
    fun setSignupMobile(v: String) { _signupMobile.value = v.filter { it.isDigit() }.take(10); clearMessages() }
    fun setSignupPassword(v: String) { _signupPassword.value = v; clearMessages() }
    fun setSignupConfirmPassword(v: String) { _signupConfirmPassword.value = v; clearMessages() }
    fun setOtpCode(v: String) { _otpCode.value = v.filter { it.isDigit() }.take(6); clearMessages() }
    fun setForgotEmail(v: String) { _forgotEmail.value = v; clearMessages() }

    fun switchMode(mode: AuthMode) {
        _mode.value = mode
        clearMessages()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }

    // ---------- validation ----------

    private fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    private fun isValidMobile(mobile: String): Boolean =
        mobile.isBlank() || (mobile.length == 10 && mobile.all { it.isDigit() })

    // ---------- actions ----------

    fun login() {
        val email = _loginEmail.value.trim()
        val password = _loginPassword.value
        if (!isValidEmail(email)) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "Please enter your password."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.login(email, password)
                .onSuccess { authToken: AuthToken ->
                    ApiClient.setAuthToken(authToken.token)
                    _loginSuccess.value = true
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Login failed. Please check your credentials and try again."
                }
            _isLoading.value = false
        }
    }

    fun register() {
        val name = _signupName.value.trim()
        val email = _signupEmail.value.trim()
        val mobile = _signupMobile.value.trim()
        val password = _signupPassword.value
        val confirm = _signupConfirmPassword.value

        if (name.length < 2) {
            _errorMessage.value = "Please enter your full name."
            return
        }
        if (!isValidEmail(email)) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }
        if (!isValidMobile(mobile)) {
            _errorMessage.value = "Mobile number must be 10 digits, or leave it blank."
            return
        }
        if (password.length < 8) {
            _errorMessage.value = "Password must be at least 8 characters."
            return
        }
        if (password != confirm) {
            _errorMessage.value = "Passwords do not match."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.register(
                name = name,
                email = email,
                password = password,
                mobile = mobile.ifBlank { null }
            ).onSuccess {
                _otpEmail.value = email
                _otpCode.value = ""
                _mode.value = AuthMode.VERIFY_SIGNUP_OTP
                _infoMessage.value = "A 6-digit verification code was sent to $email."
                startResendCooldown()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Registration failed. Please try again."
            }
            _isLoading.value = false
        }
    }

    fun verifySignupOtp() {
        val email = _otpEmail.value.trim()
        val code = _otpCode.value.trim()
        if (code.length != 6) {
            _errorMessage.value = "Please enter the 6-digit verification code."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.verifySignupOtp(email, code)
                .onSuccess { authToken: AuthToken ->
                    ApiClient.setAuthToken(authToken.token)
                    _loginSuccess.value = true
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Invalid or expired code. Please try again."
                }
            _isLoading.value = false
        }
    }

    fun resendSignupOtp() {
        if (_resendCooldown.value > 0) return
        val email = _otpEmail.value.trim()
        if (email.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.resendSignupOtp(email).onSuccess {
                _infoMessage.value = "A new verification code was sent to $email."
                startResendCooldown()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Could not resend the code. Please try again."
            }
            _isLoading.value = false
        }
    }

    fun forgotPassword() {
        val email = _forgotEmail.value.trim()
        if (!isValidEmail(email)) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.forgotPassword(email)
                .onSuccess {
                    _infoMessage.value = "Password reset instructions were sent to $email."
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Could not send reset instructions. Please try again."
                }
            _isLoading.value = false
        }
    }

    private fun startResendCooldown(seconds: Int = 30) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _resendCooldown.value = seconds
            while (_resendCooldown.value > 0) {
                delay(1000)
                _resendCooldown.value = _resendCooldown.value - 1
            }
        }
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }
}
