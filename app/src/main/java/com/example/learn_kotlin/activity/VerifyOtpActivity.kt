package com.example.learn_kotlin.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.ResendOtpRequest
import com.example.learn_kotlin.api.VerifyOtpRequest
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.applySystemBarInsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class VerifyOtpActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EMAIL = "verification_email"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ScreenLayouts.adaptive(this, ScreenLayouts.verifyOtp(this)))
        applySystemBarInsets(findViewById(R.id.main))

        val email = intent.getStringExtra(EXTRA_EMAIL)?.trim().orEmpty()
        val emailLabel = findViewById<TextView>(R.id.txt_verification_email)
        val otpInput = findViewById<EditText>(R.id.txt_otp)
        val passwordInput = findViewById<EditText>(R.id.txt_verification_password)
        val verifyButton = findViewById<Button>(R.id.btn_verify_otp)
        val resendButton = findViewById<Button>(R.id.btn_resend_otp)

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Không tìm thấy email cần xác thực. Vui lòng đăng ký lại.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        emailLabel.text = "Mã xác thực đã được gửi đến $email"

        verifyButton.setOnClickListener {
            val otp = otpInput.text.toString().trim()
            otpInput.error = null
            if (otp.length != 6 || otp.any { !it.isDigit() }) {
                otpInput.error = "Vui lòng nhập đủ 6 chữ số trong email"
            } else if (passwordInput.text.isNullOrBlank()) {
                passwordInput.error = "Nhập mật khẩu đã đăng ký để xác thực"
            } else {
                verify(email, otp, passwordInput.text.toString(), otpInput, verifyButton)
            }
        }
        resendButton.setOnClickListener { resend(email, resendButton) }
    }

    private fun verify(email: String, otp: String, password: String, otpInput: EditText, button: Button) {
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@VerifyOtpActivity)
                    .verifyOtp(VerifyOtpRequest(email = email, otp = otp, password = password))

                if (!response.isSuccessful || response.data != true) {
                    otpInput.error = response.message ?: "Mã OTP không đúng hoặc đã hết hạn"
                    Toast.makeText(
                        this@VerifyOtpActivity,
                        response.message ?: "Mã OTP không đúng hoặc đã hết hạn",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Toast.makeText(this@VerifyOtpActivity, "Email đã được xác thực. Hãy đăng nhập.", Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(this@VerifyOtpActivity, LoginActivity::class.java)
                        .putExtra(LoginActivity.EXTRA_EMAIL, email)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("API_ERROR", "Lỗi xác thực OTP", error)
                Toast.makeText(this@VerifyOtpActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            } finally {
                button.isEnabled = true
            }
        }
    }

    private fun resend(email: String, button: Button) {
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi(this@VerifyOtpActivity)
                    .resendVerificationOtp(ResendOtpRequest(email))
                if (!response.isSuccessful) throw IllegalStateException(response.message ?: "Chưa gửi lại được OTP.")
                Toast.makeText(this@VerifyOtpActivity, response.message ?: "Nếu tài khoản chưa kích hoạt, OTP mới đã được gửi.", Toast.LENGTH_LONG).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(this@VerifyOtpActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            } finally {
                button.isEnabled = true
            }
        }
    }
}
