package com.example.learn_kotlin.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.LoginRequest
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.applySystemBarInsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EMAIL = "registered_email"
    }
    private lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)
        applySystemBarInsets(findViewById(R.id.main))

        tokenStore = TokenStore(this)
        val emailInput = findViewById<EditText>(R.id.txt_Username_login)
        intent.getStringExtra(EXTRA_EMAIL)?.let(emailInput::setText)
        val passwordInput = findViewById<EditText>(R.id.txt_Password_login)
        val loginButton = findViewById<Button>(R.id.btn_Login)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            emailInput.error = null
            passwordInput.error = null

            when {
                email.isBlank() -> emailInput.error = "Vui lòng nhập email"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    emailInput.error = "Email chưa đúng định dạng"
                password.isBlank() -> passwordInput.error = "Vui lòng nhập mật khẩu"
                else -> submitLogin(email, password, loginButton)
            }
        }
    }

    private fun submitLogin(email: String, password: String, button: Button) {
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@LoginActivity)
                    .login(LoginRequest(email = email, password = password))
                val tokens = response.data
                if (!response.success || tokens?.accessToken.isNullOrBlank()) {
                    throw IllegalStateException(response.message ?: "Đăng nhập thất bại.")
                }

                tokenStore.save(tokens!!)
                Toast.makeText(this@LoginActivity, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("API_ERROR", "Lỗi đăng nhập", error)
                Toast.makeText(this@LoginActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            } finally {
                button.isEnabled = true
            }
        }
    }
}