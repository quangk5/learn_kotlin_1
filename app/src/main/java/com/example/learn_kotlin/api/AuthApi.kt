package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<TokenResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<String>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): ApiResponse<Boolean>

    @POST("api/v1/auth/resend-verification-otp")
    suspend fun resendVerificationOtp(@Body request: ResendOtpRequest): ApiResponse<Unit>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ApiResponse<JsonElement>

    @POST("api/v1/auth/verify-reset-otp")
    suspend fun verifyResetOtp(@Body request: VerifyResetOtpRequest): ApiResponse<ResetTokenResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResponse<JsonElement>

    @POST("api/v1/auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<TokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): ApiResponse<JsonElement>

    @POST("api/v1/auth/logout-all")
    suspend fun logoutAll(): ApiResponse<JsonElement>
}
