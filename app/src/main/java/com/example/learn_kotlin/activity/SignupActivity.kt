package com.example.learn_kotlin.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.RegisterRequest
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.ui.auth.AuthTheme
import com.example.learn_kotlin.ui.auth.SignupScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private var loading by mutableStateOf(false)
    private var serverError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        setContent {
            AuthTheme {
                SignupScreen(
                    loading = loading,
                    serverError = serverError,
                    onSignup = ::submitRegistration,
                    onLogin = {
                        val loginIntent = Intent(this, LoginActivity::class.java)
                        startActivity(loginIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun submitRegistration(fullName: String, email: String, password: String) {
        if (loading) return
        loading = true
        serverError = null
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@SignupActivity).register(
                    RegisterRequest(fullName = fullName, email = email, password = password)
                )
                if (!response.isSuccessful) {
                    throw IllegalStateException(response.message ?: "Đăng ký thất bại.")
                }

                startActivity(
                    Intent(this@SignupActivity, VerifyOtpActivity::class.java)
                        .putExtra(VerifyOtpActivity.EXTRA_EMAIL, email)
                )
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("API_ERROR", "Lỗi đăng ký", error)
                serverError = error.toUserMessage()
            } finally {
                loading = false
            }
        }
    }
}
