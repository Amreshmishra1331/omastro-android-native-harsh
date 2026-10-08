package com.omastro.app.domain.usecase

import com.omastro.app.data.model.AuthConfigResponse
import com.omastro.app.data.model.SendOtpResponse
import com.omastro.app.data.model.VerifyOtpResponse
import com.omastro.app.data.repository.AuthRepository

class GetAuthConfigUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<AuthConfigResponse> = repository.fetchAuthConfig()
}

class SendOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(destination: String, channel: String): Result<SendOtpResponse> =
        repository.sendOtp(destination, channel)
}

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(destination: String, otp: String, channel: String): Result<VerifyOtpResponse> =
        repository.verifyOtp(destination, otp, channel)
}
