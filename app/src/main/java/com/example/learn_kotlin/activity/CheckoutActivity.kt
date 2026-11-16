package com.example.learn_kotlin.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.applySystemBarInsets
import com.example.learn_kotlin.api.AddressResponse
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.CartItemRequest
import com.example.learn_kotlin.api.CheckoutRequest
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckoutActivity : AppCompatActivity() {
    private val profileApi by lazy { ApiClient.profileApi(this) }
    private val orderApi by lazy { ApiClient.orderApi(this) }
    private val cartApi by lazy { ApiClient.cartApi(this) }
    private var selectedProductIds: Set<String> = emptySet()
    private var unselectedItemsRestoreFailed = false
    private var addresses: List<AddressResponse> = emptyList()
    private var selectedAddressId: String? = null
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ScreenLayouts.adaptive(this, ScreenLayouts.checkout(this)))
        applySystemBarInsets(findViewById(android.R.id.content))
        selectedProductIds = intent.getStringArrayExtra(EXTRA_SELECTED_PRODUCT_IDS).orEmpty().toSet()
        findViewById<View>(R.id.checkoutBack).setOnClickListener { finish() }
        findViewById<View>(R.id.checkoutAddAddress).setOnClickListener {
            startActivity(Intent(this, AddressesActivity::class.java))
        }
        findViewById<View>(R.id.checkoutSubmit).setOnClickListener { submitOrder() }
        loadAddresses()
    }

    override fun onResume() {
        super.onResume()
        if (firstResume) firstResume = false else loadAddresses()
    }

    private fun loadAddresses() {
        findViewById<TextView>(R.id.checkoutAddressStatus).apply {
            text = "Đang tải địa chỉ..."
            visibility = View.VISIBLE
        }
        lifecycleScope.launch {
            try {
                addresses = profileApi.getAddresses().data.orEmpty()
                renderAddresses()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CHECKOUT_API", "Loading addresses failed", error)
                showError(error.toUserMessage())
            }
        }
    }

    private fun renderAddresses() {
        val group = findViewById<RadioGroup>(R.id.checkoutAddressChoices)
        group.removeAllViews()
        val validAddresses = addresses.filter { !it.id.isNullOrBlank() }
        val defaultAddressId = validAddresses.firstOrNull { it.isDefault == true }?.id
        val addressToSelect = defaultAddressId
            ?: validAddresses.firstOrNull { it.id == selectedAddressId }?.id
            ?: validAddresses.firstOrNull()?.id
        selectedAddressId = addressToSelect
        findViewById<TextView>(R.id.checkoutAddressStatus).apply {
            text = if (validAddresses.isEmpty()) "Bạn chưa lưu địa chỉ giao hàng." else "Chọn địa chỉ nhận hàng"
            visibility = View.VISIBLE
            setTextColor(if (validAddresses.isEmpty()) Color.rgb(179, 38, 30) else Color.rgb(119, 116, 128))
        }
        validAddresses.forEach { address ->
            val id = address.id ?: return@forEach
            val line = listOfNotNull(address.recipientName, address.phone, address.streetAddress, address.ward, address.district, address.province)
                .joinToString(" · ")
            val radio = RadioButton(this).apply {
                text = if (address.isDefault == true) "$line  ·  Mặc định" else line
                tag = id
                setTextColor(Color.rgb(48, 45, 54))
                textSize = 13f
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(6)
                }
            }
            group.addView(radio)
            if (id == addressToSelect) {
                radio.isChecked = true
            }
        }
        group.setOnCheckedChangeListener { checkedGroup, checkedId ->
            selectedAddressId = checkedGroup.findViewById<RadioButton>(checkedId)?.tag as? String
        }
    }

    private fun submitOrder() {
        val addressId = selectedAddressId
        if (addressId.isNullOrBlank()) {
            showError("Hãy chọn hoặc thêm địa chỉ giao hàng.")
            return
        }
        val paymentId = findViewById<RadioGroup>(R.id.paymentMethodGroup).checkedRadioButtonId
        val paymentMethod = when (paymentId) {
            R.id.paymentSepay -> "SEPAY"
            else -> "COD"
        }
        val note = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.checkoutNote)
            .text?.toString()?.trim()?.takeIf(String::isNotBlank)
        findViewById<View>(R.id.checkoutSubmit).isEnabled = false
        lifecycleScope.launch {
            try {
                val (order, responseMessage) = checkoutSelectedItems(addressId, paymentMethod, note)
                if (unselectedItemsRestoreFailed) {
                    Toast.makeText(this@CheckoutActivity,
                        "Đơn hàng đã tạo nhưng một số món chưa chọn chưa thể khôi phục vào giỏ.", Toast.LENGTH_LONG).show()
                }
                val orderCode = order.orderCode.orEmpty()
                if (paymentMethod == "SEPAY") {
                    val orderId = order.id
                    if (orderId.isNullOrBlank()) {
                        throw IllegalStateException("Đơn hàng thiếu mã định danh, chưa thể tạo mã QR thanh toán.")
                    }
                    startActivity(Intent(this@CheckoutActivity, PaymentActivity::class.java)
                        .putExtra(PaymentActivity.EXTRA_ORDER_ID, orderId))
                    finish()
                    return@launch
                }
                MaterialAlertDialogBuilder(this@CheckoutActivity)
                    .setTitle("Đặt hàng thành công")
                    .setMessage(if (orderCode.isBlank()) responseMessage.orEmpty() else "Mã đơn hàng: $orderCode")
                    .setCancelable(false)
                    .setPositiveButton("Xem đơn hàng") { _, _ ->
                        startActivity(Intent(this@CheckoutActivity, OrdersActivity::class.java))
                        finish()
                    }
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CHECKOUT_API", "Submitting order failed", error)
                showError(error.toUserMessage())
                findViewById<View>(R.id.checkoutSubmit).isEnabled = true
            }
        }
    }

    private suspend fun checkoutSelectedItems(
        addressId: String,
        paymentMethod: String,
        note: String?
    ): Pair<com.example.learn_kotlin.api.OrderResponse, String?> {
        val currentItems = cartApi.getCart().data?.items.orEmpty()
        val currentIds = currentItems.mapNotNull { it.productId }.toSet()
        val selectedIds = if (selectedProductIds.isEmpty()) currentIds else selectedProductIds.intersect(currentIds)
        if (selectedIds.isEmpty()) throw IllegalStateException("Giỏ hàng không còn mặt hàng nào được chọn.")

        val unselectedItems = currentItems.filter { it.productId !in selectedIds }
        val removedItems = mutableListOf<com.example.learn_kotlin.api.CartItemResponse>()
        unselectedItemsRestoreFailed = false
        try {
            unselectedItems.forEach { item ->
                val productId = item.productId ?: return@forEach
                val removal = cartApi.removeItem(productId)
                if (!removal.isSuccessful) {
                    throw IllegalStateException(removal.message ?: "Không thể tạm giữ các món chưa chọn.")
                }
                removedItems.add(item)
            }
            val response = orderApi.checkout(CheckoutRequest(addressId, paymentMethod, note))
            if (!response.isSuccessful) {
                throw IllegalStateException(response.message ?: "Đặt hàng không thành công.")
            }
            val order = response.data
                ?: throw IllegalStateException("Máy chủ chưa trả thông tin đơn hàng.")
            return order to response.message
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                removedItems.forEach { item ->
                    val productId = item.productId ?: return@forEach
                    try {
                        val restored = cartApi.addItem(CartItemRequest(productId, item.quantity ?: 1))
                        if (!restored.isSuccessful) {
                            unselectedItemsRestoreFailed = true
                            android.util.Log.e("CHECKOUT_API", "Restoring unselected cart item failed: ${restored.message}")
                        }
                    } catch (error: Exception) {
                        unselectedItemsRestoreFailed = true
                        android.util.Log.e("CHECKOUT_API", "Restoring unselected cart item failed", error)
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        findViewById<TextView>(R.id.checkoutStatus).apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    companion object {
        const val EXTRA_SELECTED_PRODUCT_IDS = "selected_cart_product_ids"
    }
}
