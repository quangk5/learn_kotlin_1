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
import com.example.learn_kotlin.api.RegisterRequest
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.applySystemBarInsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup)
        applySystemBarInsets(findViewById(R.id.main))

        val fullNameInput = findViewById<EditText>(R.id.txt_fullName_signup)
        val emailInput = findViewById<EditText>(R.id.txt_username_signup)
        val passwordInput = findViewById<EditText>(R.id.txt_Password_signup)
        val confirmPasswordInput = findViewById<EditText>(R.id.txt_confirmPassword_signup)
        val signupButton = findViewById<Button>(R.id.btn_signup)

        signupButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            fullNameInput.error = null
            emailInput.error = null
            passwordInput.error = null
            confirmPasswordInput.error = null

            when {
                fullName.isBlank() -> fullNameInput.error = "Vui lòng nhập họ và tên"
                email.isBlank() -> emailInput.error = "Vui lòng nhập email"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    emailInput.error = "Email chưa đúng định dạng"
                password.length < 6 -> passwordInput.error = "Mật khẩu cần ít nhất 6 ký tự"
                confirmPassword.isBlank() ->
                    confirmPasswordInput.error = "Vui lòng nhập lại mật khẩu"
                password != confirmPassword ->
                    confirmPasswordInput.error = "Mật khẩu nhập lại không trùng khớp"
                else -> submitRegistration(fullName, email, password, signupButton)
            }
        }
    }

    private fun submitRegistration(fullName: String, email: String, password: String, button: Button) {
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@SignupActivity).register(
                    RegisterRequest(fullName = fullName, email = email, password = password)
                )
                if (!response.success) {
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
                Toast.makeText(this@SignupActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            } finally {
                button.isEnabled = true
            }
        }
    }
}
