package com.example.learn_kotlin.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.applySystemBarInsets
import com.example.learn_kotlin.api.AddressResponse
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AddressesActivity : AppCompatActivity() {
    private val profileApi by lazy { ApiClient.profileApi(this) }
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.addresses)
        applySystemBarInsets(findViewById(android.R.id.content))
        findViewById<View>(R.id.addressesBack).setOnClickListener { finish() }
        findViewById<View>(R.id.addressAddButton).setOnClickListener {
            startActivity(Intent(this, AddressFormActivity::class.java))
        }
        loadAddresses()
    }

    override fun onResume() {
        super.onResume()
        if (firstResume) firstResume = false else loadAddresses()
    }

    private fun loadAddresses() {
        showStatus("Đang tải địa chỉ...", true)
        lifecycleScope.launch {
            try {
                renderAddresses(profileApi.getAddresses().data.orEmpty())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("ADDRESS_API", "Loading addresses failed", error)
                showStatus(error.toUserMessage(), true)
            }
        }
    }

    private fun renderAddresses(addresses: List<AddressResponse>) {
        val container = findViewById<LinearLayout>(R.id.addressesContainer)
        container.removeAllViews()
        if (addresses.isEmpty()) {
            showStatus("Bạn chưa lưu địa chỉ giao hàng.", true)
            return
        }
        showStatus(null, false)
        addresses.forEach { address ->
            val (card, content) = surfaceCard()
            val title = address.recipientName.orEmpty().ifBlank { "Người nhận" }
            val place = listOfNotNull(address.streetAddress, address.ward, address.district, address.province).joinToString(", ")
            content.addView(bodyText(title, 16f, Color.rgb(36, 33, 43), true))
            content.addView(bodyText(address.phone.orEmpty(), 13f, Color.rgb(119, 116, 128)))
            content.addView(bodyText(place, 13f, Color.rgb(72, 68, 79)))
            if (address.isDefault == true) {
                content.addView(bodyText("Địa chỉ mặc định", 12f, violet(), true))
            }
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(addressAction("Sửa") { editAddress(address) })
            if (address.isDefault != true) actions.addView(addressAction("Đặt mặc định") { setDefault(address) })
            actions.addView(addressAction("Xóa") { confirmDelete(address) })
            content.addView(actions)
            card.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
            container.addView(card)
        }
    }

    private fun addressAction(label: String, action: () -> Unit) = MaterialButton(
        this,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        text = label
        textSize = 11f
        isAllCaps = false
        setPadding(dp(8), 0, dp(8), 0)
        minHeight = dp(40)
        minimumHeight = dp(40)
        insetTop = 0
        insetBottom = 0
        layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(4) }
        setOnClickListener { action() }
    }

    private fun editAddress(address: AddressResponse) {
        val id = address.id ?: return
        startActivity(Intent(this, AddressFormActivity::class.java).apply {
            putExtra(AddressFormActivity.EXTRA_ADDRESS_ID, id)
            putExtra(AddressFormActivity.EXTRA_RECIPIENT, address.recipientName)
            putExtra(AddressFormActivity.EXTRA_PHONE, address.phone)
            putExtra(AddressFormActivity.EXTRA_PROVINCE, address.province)
            putExtra(AddressFormActivity.EXTRA_DISTRICT, address.district)
            putExtra(AddressFormActivity.EXTRA_WARD, address.ward)
            putExtra(AddressFormActivity.EXTRA_STREET, address.streetAddress)
            putExtra(AddressFormActivity.EXTRA_IS_DEFAULT, address.isDefault ?: false)
        })
    }

    private fun setDefault(address: AddressResponse) {
        val id = address.id ?: return
        lifecycleScope.launch {
            try {
                profileApi.setDefaultAddress(id)
                loadAddresses()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("ADDRESS_API", "Setting default address failed", error)
                Toast.makeText(this@AddressesActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDelete(address: AddressResponse) {
        val id = address.id ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa địa chỉ")
            .setMessage("Bạn có chắc muốn xóa địa chỉ này?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    try {
                        profileApi.deleteAddress(id)
                        loadAddresses()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        android.util.Log.e("ADDRESS_API", "Deleting address failed", error)
                        Toast.makeText(this@AddressesActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun showStatus(message: String?, visible: Boolean) {
        findViewById<TextView>(R.id.addressesStatus).apply {
            text = message.orEmpty()
            visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
