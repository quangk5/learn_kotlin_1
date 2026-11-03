package com.example.learn_kotlin.activity

import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.applySystemBarInsets
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.CartItemRequest
import com.example.learn_kotlin.api.ProductResponse
import com.example.learn_kotlin.api.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class ProductDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"
    }

    private val productApi by lazy { ApiClient.productApi(this) }
    private val cartApi by lazy { ApiClient.cartApi(this) }
    private var product: ProductResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.product_detail)
        applySystemBarInsets(findViewById(android.R.id.content))
        findViewById<View>(R.id.detailBack).setOnClickListener { finish() }
        findViewById<View>(R.id.detailCart).setOnClickListener { openCart() }
        findViewById<View>(R.id.detailAddToCart).setOnClickListener { addToCart(false) }
        findViewById<View>(R.id.detailBuyNow).setOnClickListener { addToCart(true) }

        val id = intent.getStringExtra(EXTRA_PRODUCT_ID)
        if (id.isNullOrBlank()) {
            Toast.makeText(this, "Thiếu mã sản phẩm.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadProduct(id)
    }

    private fun loadProduct(id: String) {
        lifecycleScope.launch {
            try {
                val result = productApi.getProductById(id).data
                if (result == null) {
                    Toast.makeText(this@ProductDetailActivity, "Không tìm thấy sản phẩm.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                product = result
                bindProduct(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("PRODUCT_API", "Loading product details failed", error)
                Toast.makeText(this@ProductDetailActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindProduct(item: ProductResponse) {
        findViewById<TextView>(R.id.detailName).text = item.name.orEmpty()
        findViewById<TextView>(R.id.detailCategory).text = item.categoryName.orEmpty()
        findViewById<TextView>(R.id.detailBrand).text = item.brandName?.let { "Thương hiệu: $it" }.orEmpty()
        findViewById<TextView>(R.id.detailPrice).text = formatVnd(item.price)
        val stock = item.stockQuantity ?: 0
        findViewById<TextView>(R.id.detailStock).apply {
            text = if (stock > 0) "Còn $stock sản phẩm" else "Tạm hết hàng"
            setTextColor(if (stock > 0) android.graphics.Color.rgb(67, 132, 95) else android.graphics.Color.rgb(179, 38, 30))
        }
        findViewById<TextView>(R.id.detailDescription).text = item.description?.takeIf(String::isNotBlank) ?: "Chưa có mô tả cho sản phẩm này."
        findViewById<View>(R.id.detailAddToCart).isEnabled = stock > 0
        findViewById<View>(R.id.detailBuyNow).isEnabled = stock > 0
        (item.thumbnailUrl ?: item.images.orEmpty().firstOrNull())?.let(::loadImage)
    }

    private fun loadImage(path: String) {
        val parsed = runCatching { URL(path) }.getOrNull()
        if (parsed?.host?.equals("localhost", true) == true || parsed?.host == "127.0.0.1") {
            findViewById<TextView>(R.id.detailImagePlaceholder).text = "Ảnh chưa được cung cấp công khai"
            return
        }
        lifecycleScope.launch {
            val bitmap = try {
                withContext(Dispatchers.IO) {
                    val address = if (path.startsWith("http://") || path.startsWith("https://")) path
                    else "https://ecommerce-backend-q1wt.onrender.com/${path.trimStart('/')}"
                    URL(address).openConnection().apply { connectTimeout = 12_000; readTimeout = 20_000 }
                        .getInputStream().use { BitmapFactory.decodeStream(it) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (bitmap != null) {
                findViewById<ImageView>(R.id.detailImage).apply {
                    setImageBitmap(bitmap)
                    visibility = View.VISIBLE
                }
                findViewById<TextView>(R.id.detailImagePlaceholder).visibility = View.GONE
            }
        }
    }

    private fun addToCart(buyNow: Boolean) {
        val item = product ?: return
        val id = item.id ?: return
        if ((item.stockQuantity ?: 0) <= 0) return
        lifecycleScope.launch {
            try {
                val response = cartApi.addItem(CartItemRequest(id, 1))
                Toast.makeText(this@ProductDetailActivity, response.message ?: "Đã thêm vào giỏ hàng.", Toast.LENGTH_SHORT).show()
                if (buyNow) openCart()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CART_API", "Adding product to cart failed", error)
                Toast.makeText(this@ProductDetailActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openCart() = startActivity(android.content.Intent(this, CartActivity::class.java))
}
