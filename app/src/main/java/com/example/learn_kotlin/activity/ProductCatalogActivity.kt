package com.example.learn_kotlin.activity

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import com.example.learn_kotlin.api.CategoryResponse
import com.example.learn_kotlin.api.ProductResponse
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProductCatalogActivity : AppCompatActivity() {
    private val productApi by lazy { ApiClient.productApi(this) }
    private val categoryApi by lazy { ApiClient.categoryApi(this) }
    private var selectedCategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.product_catalog)
        applySystemBarInsets(findViewById(android.R.id.content))
        findViewById<View>(R.id.catalogBack).setOnClickListener { finish() }
        findViewById<View>(R.id.catalogCart).setOnClickListener { startActivity(android.content.Intent(this, CartActivity::class.java)) }
        findViewById<EditText>(R.id.catalogSearch).setOnEditorActionListener { field, actionId, event ->
            val submit = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)
            if (submit) {
                selectedCategoryId = null
                loadProducts(field.text?.toString()?.trim()?.takeIf(String::isNotBlank), null)
                true
            } else false
        }
        loadCategories()
        loadProducts(null, null)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val categories = categoryApi.getAllCategories().data.orEmpty()
                renderCategories(categories)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CATALOG_API", "Loading categories failed", error)
            }
        }
    }

    private fun renderCategories(categories: List<CategoryResponse>) {
        val container = findViewById<LinearLayout>(R.id.catalogCategoryContainer)
        container.removeAllViews()
        val allButton = categoryButton("Tất cả") {
            selectedCategoryId = null
            loadProducts(findViewById<EditText>(R.id.catalogSearch).text?.toString(), null)
        }
        container.addView(allButton)
        categories.forEach { category ->
            val id = category.id ?: return@forEach
            container.addView(categoryButton(category.name.orEmpty()) {
                selectedCategoryId = id
                findViewById<EditText>(R.id.catalogSearch).setText("")
                loadProducts(null, id)
            })
        }
    }

    private fun categoryButton(label: String, onClick: () -> Unit) = MaterialButton(
        this,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        cornerRadius = dp(18)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).apply {
            marginEnd = dp(8)
        }
    }

    private fun loadProducts(keyword: String?, categoryId: String?) {
        showStatus("Đang tải sản phẩm...", true)
        lifecycleScope.launch {
            try {
                val page = if (keyword.isNullOrBlank() && categoryId.isNullOrBlank()) {
                    productApi.getActiveProducts(page = 0, size = 30).data
                } else {
                    productApi.searchProducts(keyword = keyword, categoryId = categoryId, page = 0, size = 30).data
                }
                renderProducts(page?.items.orEmpty())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("CATALOG_API", "Loading products failed", error)
                showStatus(error.toUserMessage(), true)
            }
        }
    }

    private fun renderProducts(products: List<ProductResponse>) {
        val container = findViewById<LinearLayout>(R.id.catalogProductContainer)
        container.removeAllViews()
        val status = findViewById<TextView>(R.id.catalogStatus)
        if (products.isEmpty()) {
            status.text = "Không tìm thấy sản phẩm phù hợp."
            status.visibility = View.VISIBLE
            return
        }
        status.visibility = View.GONE
        products.forEach { product ->
            val (card, content) = surfaceCard()
            content.addView(bodyText(product.name.orEmpty(), 16f, android.graphics.Color.rgb(36, 33, 43), true))
            content.addView(bodyText(listOfNotNull(product.brandName, product.categoryName).joinToString(" · "), 12f, android.graphics.Color.rgb(119, 116, 128)))
            content.addView(bodyText(formatVnd(product.price), 16f, violet(), true))
            content.addView(bodyText(if ((product.stockQuantity ?: 0) > 0) "Còn hàng" else "Tạm hết hàng", 12f, android.graphics.Color.rgb(67, 132, 95)))
            card.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
            card.setOnClickListener {
                val id = product.id ?: return@setOnClickListener
                startActivity(android.content.Intent(this, ProductDetailActivity::class.java).putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, id))
            }
            container.addView(card)
        }
    }

    private fun showStatus(message: String, visible: Boolean) {
        findViewById<TextView>(R.id.catalogStatus).apply {
            text = message
            visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
