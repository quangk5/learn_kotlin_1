package com.example.learn_kotlin.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.ForgotPasswordRequest
import com.example.learn_kotlin.api.ResetPasswordRequest
import com.example.learn_kotlin.api.VerifyResetOtpRequest
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.ui.auth.AuthTheme
import com.example.learn_kotlin.ui.auth.ForgotPasswordScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_IDENTITY = "forgot_password_identity"
    }

    private var otpRequested by mutableStateOf(false)
    private var otpVerified by mutableStateOf(false)
    private var resetToken: String? = null
    private var requestLoading by mutableStateOf(false)
    private var verifyLoading by mutableStateOf(false)
    private var resetLoading by mutableStateOf(false)
    private var serverError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        setContent {
            AuthTheme {
                ForgotPasswordScreen(
                    initialIdentity = intent.getStringExtra(EXTRA_IDENTITY).orEmpty(),
                    otpRequested = otpRequested,
                    otpVerified = otpVerified,
                    requestLoading = requestLoading,
                    verifyLoading = verifyLoading,
                    resetLoading = resetLoading,
                    serverError = serverError,
                    onRequestOtp = ::requestOtp,
                    onVerifyOtp = ::verifyOtp,
                    onResetPassword = ::resetPassword,
                    onBackToLogin = ::returnToLogin
                )
            }
        }
    }

    private fun requestOtp(identity: String) {
        if (requestLoading || verifyLoading || resetLoading) return
        requestLoading = true
        serverError = null
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@ForgotPasswordActivity)
                    .forgotPassword(ForgotPasswordRequest(email = identity))
                if (!response.isSuccessful) {
                    throw IllegalStateException(response.message ?: "Không gửi được mã xác thực.")
                }
                otpRequested = true
                otpVerified = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("API_ERROR", "Requesting password reset OTP failed", error)
                serverError = error.toUserMessage()
            } finally {
                requestLoading = false
            }
        }
    }

    private fun verifyOtp(identity: String, otp: String) {
        if (requestLoading || verifyLoading || resetLoading) return
        verifyLoading = true
        serverError = null
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@ForgotPasswordActivity)
                    .verifyResetOtp(VerifyResetOtpRequest(identity = identity, otp = otp))
                val token = response.data?.resetToken
                if (!response.isSuccessful || token.isNullOrBlank()) {
                    throw IllegalStateException(response.message ?: "Mã OTP không hợp lệ hoặc đã hết hạn.")
                }
                resetToken = token
                otpVerified = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("API_ERROR", "Verifying password reset OTP failed", error)
                serverError = error.toUserMessage()
            } finally {
                verifyLoading = false
            }
        }
    }

    private fun resetPassword(identity: String, newPassword: String) {
        if (requestLoading || verifyLoading || resetLoading || !otpVerified) return
        resetLoading = true
        serverError = null
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@ForgotPasswordActivity)
                    .resetPassword(ResetPasswordRequest(resetToken = resetToken.orEmpty(), newPassword = newPassword))
                if (!response.isSuccessful) {
                    throw IllegalStateException(response.message ?: "OTP không hợp lệ hoặc đã hết hạn.")
                }
                Toast.makeText(this@ForgotPasswordActivity, "Đặt lại mật khẩu thành công. Hãy đăng nhập.", Toast.LENGTH_LONG).show()
                returnToLogin()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("API_ERROR", "Resetting password failed", error)
                serverError = error.toUserMessage()
            } finally {
                resetLoading = false
            }
        }
    }

    private fun returnToLogin() = finish()
}
