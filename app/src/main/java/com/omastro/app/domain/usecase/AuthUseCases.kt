package com.omastro.app.domain.usecase

import com.omastro.app.data.model.LoginResponse
import com.omastro.app.data.model.OtpChannels
import com.omastro.app.data.model.RegisterResponse
import com.omastro.app.data.model.VerifySignupOtpResponse
import com.omastro.app.data.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, remember: Boolean = false): Result<LoginResponse> =
        repository.login(email, password, remember)
}

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        mobile: String? = null,
        otpChannel: String = OtpChannels.DEFAULT
    ): Result<RegisterResponse> =
        repository.register(name, email, password, mobile, otpChannel)
}

class VerifySignupOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        otp: String,
        otpChannel: String = OtpChannels.DEFAULT
    ): Result<VerifySignupOtpResponse> =
        repository.verifySignupOtp(email, otp, otpChannel)
}

class ForgotPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> =
        repository.forgotPassword(email)
}

class ResendSignupOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        otpChannel: String = OtpChannels.DEFAULT
    ): Result<Unit> =
        repository.resendSignupOtp(email, otpChannel)
}
