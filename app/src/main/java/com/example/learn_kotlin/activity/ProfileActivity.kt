package com.example.learn_kotlin.activity

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.applySystemBarInsets
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.ChangePasswordRequest
import com.example.learn_kotlin.api.ProfileUpdateRequest
import com.example.learn_kotlin.api.UserProfileResponse
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private val profileApi by lazy { ApiClient.profileApi(this) }
    private val authApi by lazy { ApiClient.authApi(this) }
    private lateinit var tokenStore: TokenStore
    private var profile: UserProfileResponse? = null
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile)
        applySystemBarInsets(findViewById(android.R.id.content))
        tokenStore = TokenStore(this)
        findViewById<View>(R.id.profileBack).setOnClickListener { finish() }
        findViewById<View>(R.id.profileEditButton).setOnClickListener { editProfile() }
        findViewById<View>(R.id.profileAddressesButton).setOnClickListener { startActivity(Intent(this, AddressesActivity::class.java)) }
        findViewById<View>(R.id.profileOrdersButton).setOnClickListener { startActivity(Intent(this, OrdersActivity::class.java)) }
        findViewById<View>(R.id.profilePasswordButton).setOnClickListener { changePassword() }
        findViewById<View>(R.id.profileLogoutButton).setOnClickListener { confirmLogout() }
        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        if (firstResume) firstResume = false else loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val result = profileApi.getProfile().data
                if (result != null) bindProfile(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("PROFILE_API", "Loading profile failed", error)
                Toast.makeText(this@ProfileActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindProfile(value: UserProfileResponse) {
        profile = value
        findViewById<TextView>(R.id.profileName).text = value.fullName ?: "Tài khoản của tôi"
        findViewById<TextView>(R.id.profileEmail).text = value.email.orEmpty()
        findViewById<TextView>(R.id.profilePhone).text = value.phone?.let { "Điện thoại: $it" }.orEmpty()
    }

    private fun editProfile() {
        val current = profile ?: return
        val fullName = EditText(this).apply {
            hint = "Họ và tên"
            setText(current.fullName.orEmpty())
        }
        val phone = EditText(this).apply {
            hint = "Số điện thoại"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(current.phone.orEmpty())
        }
        val fields = dialogFields(fullName, phone)
        MaterialAlertDialogBuilder(this)
            .setTitle("Chỉnh sửa hồ sơ")
            .setView(fields)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu", null)
            .create().apply {
                setOnShowListener {
                    getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = fullName.text.toString().trim()
                        if (name.isBlank()) {
                            fullName.error = "Vui lòng nhập họ tên"
                            return@setOnClickListener
                        }
                        updateProfile(ProfileUpdateRequest(name, phone.text.toString().trim().ifBlank { null }, current.avatarUrl))
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun updateProfile(request: ProfileUpdateRequest) {
        lifecycleScope.launch {
            try {
                val response = profileApi.updateProfile(request)
                response.data?.let(::bindProfile)
                Toast.makeText(this@ProfileActivity, response.message ?: "Đã cập nhật hồ sơ.", Toast.LENGTH_SHORT).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("PROFILE_API", "Updating profile failed", error)
                Toast.makeText(this@ProfileActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun changePassword() {
        val oldPassword = EditText(this).apply {
            hint = "Mật khẩu hiện tại"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val newPassword = EditText(this).apply {
            hint = "Mật khẩu mới (ít nhất 6 ký tự)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Đổi mật khẩu")
            .setView(dialogFields(oldPassword, newPassword))
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Cập nhật") { _, _ ->
                val old = oldPassword.text.toString()
                val new = newPassword.text.toString()
                if (old.isBlank() || new.length < 6) {
                    Toast.makeText(this, "Kiểm tra mật khẩu hiện tại và mật khẩu mới (tối thiểu 6 ký tự).", Toast.LENGTH_LONG).show()
                } else submitPasswordChange(old, new)
            }
            .show()
    }

    private fun submitPasswordChange(oldPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val response = profileApi.changePassword(ChangePasswordRequest(oldPassword, newPassword))
                Toast.makeText(this@ProfileActivity, response.message ?: "Đã đổi mật khẩu.", Toast.LENGTH_SHORT).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("PROFILE_API", "Changing password failed", error)
                Toast.makeText(this@ProfileActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất khỏi tài khoản này?")
            .setNegativeButton("Ở lại", null)
            .setPositiveButton("Đăng xuất") { _, _ -> logout() }
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                authApi.logout()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("PROFILE_API", "Logout request failed", error)
            } finally {
                tokenStore.clear()
                startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finish()
            }
        }
    }

    private fun dialogFields(vararg fields: EditText): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(24), dp(8), dp(24), dp(4))
        fields.forEachIndexed { index, field ->
            addView(field, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)).apply {
                if (index > 0) topMargin = dp(8)
            })
        }
    }
}
