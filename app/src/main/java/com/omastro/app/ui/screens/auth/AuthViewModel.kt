package com.omastro.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.AuthConfigResponse
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val getAuthConfigUseCase = AppContainer.getAuthConfigUseCase
    private val sendOtpUseCase = AppContainer.sendOtpUseCase
    private val verifyOtpUseCase = AppContainer.verifyOtpUseCase

    val authConfig: StateFlow<AuthConfigResponse> = AppContainer.authRepository.authConfig

    private val _destination = MutableStateFlow("")
    val destination: StateFlow<String> = _destination.asStateFlow()

    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp.asStateFlow()

    private val _selectedChannel = MutableStateFlow("sms") // "sms", "whatsapp", "email"
    val selectedChannel: StateFlow<String> = _selectedChannel.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            getAuthConfigUseCase()
        }
    }

    fun setDestination(value: String) {
        _destination.value = value
    }

    fun setOtp(value: String) {
        _otp.value = value
    }

    fun setChannel(channel: String) {
        _selectedChannel.value = channel
    }

    fun requestOtp() {
        if (_destination.value.isBlank()) {
            _statusMessage.value = "Please enter your mobile number or email"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null
            val result = sendOtpUseCase(_destination.value, _selectedChannel.value)
            result.onSuccess { res ->
                _otpSent.value = true
                _statusMessage.value = res.message
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to send OTP"
            }
            _isLoading.value = false
        }
    }

    fun verifyOtp() {
        if (_otp.value.length < 4) {
            _statusMessage.value = "Please enter the valid OTP"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null
            val result = verifyOtpUseCase(_destination.value, _otp.value, _selectedChannel.value)
            result.onSuccess { res ->
                if (res.success) {
                    _loginSuccess.value = true
                    _statusMessage.value = res.message ?: "Login Successful"
                } else {
                    _statusMessage.value = res.message ?: "Invalid OTP"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Verification failed"
            }
            _isLoading.value = false
        }
    }
}
