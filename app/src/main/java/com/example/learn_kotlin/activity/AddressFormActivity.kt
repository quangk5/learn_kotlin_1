package com.example.learn_kotlin.activity

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.applySystemBarInsets
import com.example.learn_kotlin.api.AddressRequest
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AddressFormActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ADDRESS_ID = "address_id"
        const val EXTRA_RECIPIENT = "recipient_name"
        const val EXTRA_PHONE = "recipient_phone"
        const val EXTRA_PROVINCE = "province"
        const val EXTRA_DISTRICT = "district"
        const val EXTRA_WARD = "ward"
        const val EXTRA_STREET = "street_address"
        const val EXTRA_IS_DEFAULT = "is_default"
    }

    private val profileApi by lazy { ApiClient.profileApi(this) }
    private val addressId by lazy { intent.getStringExtra(EXTRA_ADDRESS_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.address_form)
        applySystemBarInsets(findViewById(android.R.id.content))
        findViewById<View>(R.id.addressFormBack).setOnClickListener { finish() }
        findViewById<View>(R.id.addressSaveButton).setOnClickListener { saveAddress() }
        bindExistingAddress()
    }

    private fun bindExistingAddress() {
        val editing = !addressId.isNullOrBlank()
        findViewById<TextView>(R.id.addressFormTitle).text = if (editing) "Chỉnh sửa địa chỉ" else "Địa chỉ giao hàng"
        findViewById<android.widget.Button>(R.id.addressSaveButton).text = if (editing) "Cập nhật địa chỉ" else "Lưu địa chỉ"
        findViewById<EditText>(R.id.addressRecipient).setText(intent.getStringExtra(EXTRA_RECIPIENT).orEmpty())
        findViewById<EditText>(R.id.addressPhone).setText(intent.getStringExtra(EXTRA_PHONE).orEmpty())
        findViewById<EditText>(R.id.addressProvince).setText(intent.getStringExtra(EXTRA_PROVINCE).orEmpty())
        findViewById<EditText>(R.id.addressDistrict).setText(intent.getStringExtra(EXTRA_DISTRICT).orEmpty())
        findViewById<EditText>(R.id.addressWard).setText(intent.getStringExtra(EXTRA_WARD).orEmpty())
        findViewById<EditText>(R.id.addressStreet).setText(intent.getStringExtra(EXTRA_STREET).orEmpty())
        findViewById<CheckBox>(R.id.addressDefaultCheck).isChecked = intent.getBooleanExtra(EXTRA_IS_DEFAULT, false)
    }

    private fun saveAddress() {
        val recipient = value(R.id.addressRecipient)
        val phone = value(R.id.addressPhone)
        val province = value(R.id.addressProvince)
        val district = value(R.id.addressDistrict)
        val ward = value(R.id.addressWard)
        val street = value(R.id.addressStreet)
        if (listOf(recipient, phone, province, district, ward, street).any(String::isBlank)) {
            showError("Vui lòng nhập đầy đủ thông tin địa chỉ.")
            return
        }
        val request = AddressRequest(
            recipientName = recipient,
            phone = phone,
            province = province,
            district = district,
            ward = ward,
            streetAddress = street,
            isDefault = findViewById<CheckBox>(R.id.addressDefaultCheck).isChecked
        )
        findViewById<View>(R.id.addressSaveButton).isEnabled = false
        lifecycleScope.launch {
            try {
                val response = if (addressId.isNullOrBlank()) profileApi.addAddress(request)
                else profileApi.updateAddress(addressId!!, request)
                Toast.makeText(this@AddressFormActivity, response.message ?: "Đã lưu địa chỉ.", Toast.LENGTH_SHORT).show()
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("ADDRESS_API", "Saving address failed", error)
                showError(error.toUserMessage())
                findViewById<View>(R.id.addressSaveButton).isEnabled = true
            }
        }
    }

    private fun value(id: Int) = findViewById<EditText>(id).text.toString().trim()

    private fun showError(message: String) {
        findViewById<TextView>(R.id.addressFormError).apply {
            text = message
            visibility = View.VISIBLE
        }
    }
}
