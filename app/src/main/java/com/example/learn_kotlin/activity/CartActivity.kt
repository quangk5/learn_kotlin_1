package com.example.learn_kotlin.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
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
import com.example.learn_kotlin.api.CartItemRequest
import com.example.learn_kotlin.api.CartItemResponse
import com.example.learn_kotlin.api.CartResponse
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CartActivity : AppCompatActivity() {
    private val cartApi by lazy { ApiClient.cartApi(this) }
    private var cart: CartResponse? = null
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.cart)
        applySystemBarInsets(findViewById(android.R.id.content))
        findViewById<View>(R.id.cartBack).setOnClickListener { finish() }
        findViewById<View>(R.id.cartClear).setOnClickListener { clearCart() }
        findViewById<View>(R.id.cartCheckout).setOnClickListener {
            if (cart?.items.orEmpty().isEmpty()) {
                toast("Giỏ hàng đang trống.")
            } else {
                startActivity(Intent(this, CheckoutActivity::class.java))
            }
        }
        loadCart()
    }

    override fun onResume() {
        super.onResume()
        if (firstResume) firstResume = false else loadCart()
    }

    private fun loadCart() {
        lifecycleScope.launch {
            try {
                bindCart(cartApi.getCart().data)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CART_API", "Loading cart failed", error)
                showStatus(error.toUserMessage())
            }
        }
    }

    private fun bindCart(result: CartResponse?) {
        cart = result
        val items = result?.items.orEmpty()
        val container = findViewById<LinearLayout>(R.id.cartItemsContainer)
        container.removeAllViews()
        findViewById<TextView>(R.id.cartTotal).text = formatVnd(result?.totalAmount)
        findViewById<View>(R.id.cartCheckout).isEnabled = items.isNotEmpty()
        if (items.isEmpty()) {
            showStatus("Giỏ hàng của bạn đang trống. Hãy chọn sản phẩm để bắt đầu.")
            return
        }
        showStatus(null)
        items.forEach { item -> container.addView(createCartItem(item)) }
    }

    private fun createCartItem(item: CartItemResponse): View {
        val (card, content) = surfaceCard()
        val name = bodyText(item.productName.orEmpty(), 15f, Color.rgb(36, 33, 43), true)
        content.addView(name)
        content.addView(bodyText("${formatVnd(item.unitPrice)} mỗi sản phẩm", 12f, Color.rgb(119, 116, 128)))
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }
        controls.addView(bodyText("Thành tiền: ${formatVnd(item.subTotal)}", 14f, violet(), true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val quantity = item.quantity ?: 1
        controls.addView(smallAction("−") { changeQuantity(item, quantity - 1) })
        controls.addView(bodyText("  $quantity  ", 14f, Color.rgb(36, 33, 43), true).apply { gravity = Gravity.CENTER })
        controls.addView(smallAction("+") { changeQuantity(item, quantity + 1) })
        controls.addView(bodyText("  Xóa", 12f, Color.rgb(179, 38, 30), true).apply {
            setOnClickListener { removeItem(item) }
        })
        content.addView(controls)
        card.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        }
        return card
    }

    private fun smallAction(label: String, click: () -> Unit) = MaterialButton(this).apply {
        text = label
        textSize = 17f
        minWidth = dp(40)
        minimumWidth = dp(40)
        minHeight = dp(40)
        minimumHeight = dp(40)
        insetTop = 0
        insetBottom = 0
        setPadding(0, 0, 0, 0)
        cornerRadius = dp(12)
        setOnClickListener { click() }
    }

    private fun changeQuantity(item: CartItemResponse, quantity: Int) {
        val id = item.productId ?: return
        lifecycleScope.launch {
            try {
                if (quantity < 1) cartApi.removeItem(id)
                else cartApi.updateItemQuantity(CartItemRequest(id, quantity))
                loadCart()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CART_API", "Updating cart quantity failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun removeItem(item: CartItemResponse) {
        val id = item.productId ?: return
        lifecycleScope.launch {
            try {
                cartApi.removeItem(id)
                loadCart()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CART_API", "Removing cart item failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun clearCart() {
        lifecycleScope.launch {
            try {
                cartApi.clearCart()
                loadCart()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CART_API", "Clearing cart failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun showStatus(message: String?) {
        findViewById<TextView>(R.id.cartStatus).apply {
            text = message.orEmpty()
            visibility = if (message == null) View.GONE else View.VISIBLE
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
