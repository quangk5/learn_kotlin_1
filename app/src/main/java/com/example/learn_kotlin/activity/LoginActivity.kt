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
import com.example.learn_kotlin.api.LoginRequest
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.ui.auth.AuthTheme
import com.example.learn_kotlin.ui.auth.LoginScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EMAIL = "registered_email"
        const val EXTRA_IDENTITY = "recovery_identity"
    }

    private lateinit var tokenStore: TokenStore
    private var loading by mutableStateOf(false)
    private var serverError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        tokenStore = TokenStore(this)

        setContent {
            AuthTheme {
                LoginScreen(
                    loading = loading,
                    serverError = serverError,
                    initialEmail = intent.getStringExtra(EXTRA_EMAIL).orEmpty(),
                    onLogin = ::submitLogin,
                    onSignup = { startActivity(Intent(this, SignupActivity::class.java)) },
                    onForgotPassword = { identity ->
                        startActivity(
                            Intent(this, ForgotPasswordActivity::class.java)
                                .putExtra(ForgotPasswordActivity.EXTRA_IDENTITY, identity)
                        )
                    }
                )
            }
        }
    }

    private fun submitLogin(email: String, password: String) {
        if (loading) return
        loading = true
        serverError = null
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@LoginActivity)
                    .login(LoginRequest(email = email, password = password))
                val tokens = response.data
                if (!response.isSuccessful || tokens?.accessToken.isNullOrBlank()) {
                    throw IllegalStateException(response.message ?: "Đăng nhập thất bại.")
                }

                tokenStore.save(tokens)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("API_ERROR", "Lỗi đăng nhập", error)
                serverError = error.toUserMessage()
            } finally {
                loading = false
            }
        }
    }
}
