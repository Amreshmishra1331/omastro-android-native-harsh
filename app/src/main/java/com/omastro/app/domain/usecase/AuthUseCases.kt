package com.omastro.app.domain.usecase

import com.omastro.app.data.model.LoginResponse
import com.omastro.app.data.model.RegisterResponse
import com.omastro.app.data.model.VerifySignupOtpResponse
import com.omastro.app.data.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<LoginResponse> =
        repository.login(email, password)
}

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        mobile: String? = null
    ): Result<RegisterResponse> =
        repository.register(name, email, password, mobile)
}

class VerifySignupOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Result<VerifySignupOtpResponse> =
        repository.verifySignupOtp(email, otp)
}

class ForgotPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> =
        repository.forgotPassword(email)
}

class ResendSignupOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> =
        repository.resendSignupOtp(email)
}
