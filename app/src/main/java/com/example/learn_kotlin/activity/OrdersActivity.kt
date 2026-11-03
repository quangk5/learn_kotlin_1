package com.example.learn_kotlin.activity

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
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.OrderResponse
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class OrdersActivity : AppCompatActivity() {
    private val orderApi by lazy { ApiClient.orderApi(this) }
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.orders)
        applySystemBarInsets(findViewById(android.R.id.content))
        findViewById<View>(R.id.ordersBack).setOnClickListener { finish() }
        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        if (firstResume) firstResume = false else loadOrders()
    }

    private fun loadOrders() {
        setStatus("Đang tải đơn hàng...", true)
        lifecycleScope.launch {
            try {
                val items = orderApi.getMyOrders(page = 0, size = 30).data?.items.orEmpty()
                renderOrders(items)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("ORDER_API", "Loading orders failed", error)
                setStatus(error.toUserMessage(), true)
            }
        }
    }

    private fun renderOrders(orders: List<OrderResponse>) {
        val container = findViewById<LinearLayout>(R.id.ordersContainer)
        container.removeAllViews()
        if (orders.isEmpty()) {
            setStatus("Bạn chưa có đơn hàng nào.", true)
            return
        }
        setStatus(null, false)
        orders.forEach { order ->
            val (card, content) = surfaceCard()
            content.addView(bodyText(order.orderCode ?: "Đơn hàng", 16f, Color.rgb(36, 33, 43), true))
            content.addView(bodyText("Trạng thái: ${order.status ?: "Đang xử lý"}", 13f, violet()))
            content.addView(bodyText("${order.items.orEmpty().sumOf { it.quantity ?: 0 }} mặt hàng", 12f, Color.rgb(119, 116, 128)))
            content.addView(bodyText("Tổng cộng: ${formatVnd(order.finalAmount)}", 15f, Color.rgb(36, 33, 43), true))
            card.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
            card.setOnClickListener { loadOrderDetails(order) }
            container.addView(card)
        }
    }

    private fun loadOrderDetails(order: OrderResponse) {
        val id = order.id ?: return
        lifecycleScope.launch {
            try {
                showOrderDetails(orderApi.getOrderById(id).data ?: order)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("ORDER_API", "Loading order details failed", error)
                Toast.makeText(this@OrdersActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showOrderDetails(order: OrderResponse) {
        val itemText = order.items.orEmpty().joinToString("\n") {
            "${it.productName.orEmpty()} ×${it.quantity ?: 0}  ${formatVnd(it.subTotal)}"
        }
        val address = order.shippingAddress?.let {
            listOfNotNull(it.streetAddress, it.ward, it.district, it.province).joinToString(", ")
        }.orEmpty()
        val message = buildString {
            append(itemText.ifBlank { "Không có chi tiết sản phẩm." })
            append("\n\nTrạng thái: ${order.status ?: "—"}")
            append("\nThanh toán: ${order.paymentMethod ?: "—"}")
            append("\nĐịa chỉ: ${address.ifBlank { "—" }}")
            append("\nTổng tiền: ${formatVnd(order.finalAmount)}")
            order.note?.takeIf(String::isNotBlank)?.let { append("\nGhi chú: $it") }
        }
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(order.orderCode ?: "Chi tiết đơn hàng")
            .setMessage(message)
            .setNegativeButton("Đóng", null)
        val id = order.id
        if (order.status.equals("PENDING", ignoreCase = true) && !id.isNullOrBlank()) {
            builder.setPositiveButton("Hủy đơn") { _, _ -> cancelOrder(id) }
        }
        builder.show()
    }

    private fun cancelOrder(id: String) {
        lifecycleScope.launch {
            try {
                val response = orderApi.cancelOrder(id)
                Toast.makeText(this@OrdersActivity, response.message ?: "Đã hủy đơn hàng.", Toast.LENGTH_SHORT).show()
                loadOrders()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("ORDER_API", "Cancelling order failed", error)
                Toast.makeText(this@OrdersActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setStatus(message: String?, visible: Boolean) {
        findViewById<TextView>(R.id.ordersStatus).apply {
            text = message.orEmpty()
            visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
