package com.example.learn_kotlin.activity

import android.os.Bundle
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.example.learn_kotlin.api.CartItemRequest
import com.example.learn_kotlin.api.ProductResponse
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class ProductCatalogActivity : AppCompatActivity() {
    private val productApi by lazy { ApiClient.productApi(this) }
    private val categoryApi by lazy { ApiClient.categoryApi(this) }
    private val cartApi by lazy { ApiClient.cartApi(this) }
    private var selectedCategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ScreenLayouts.adaptive(this, ScreenLayouts.catalog(this)))
        applySystemBarInsets(findViewById(android.R.id.content))
        findViewById<View>(R.id.catalogBack).setOnClickListener { finish() }
        findViewById<View>(R.id.catalogCart).setOnClickListener { startActivity(android.content.Intent(this, CartActivity::class.java)) }
        bindBottomNavigation(
            onHome = { openRootTab(MainActivity::class.java) },
            onCategories = {
                selectedCategoryId = null
                findViewById<EditText>(R.id.catalogSearch).setText("")
                loadProducts(null, null)
            },
            onAccount = { openRootTab(ProfileActivity::class.java) }
        )
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
            status.text = "Chưa tìm thấy sản phẩm phù hợp. Thử từ khóa khác nhé."
            status.visibility = View.VISIBLE
            return
        }
        status.visibility = View.GONE
        products.chunked(2).forEachIndexed { rowIndex, rowProducts ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    if (rowIndex > 0) topMargin = dp(12)
                }
            }
            rowProducts.forEachIndexed { index, product ->
                row.addView(createProductCard(product), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index == 0) marginEnd = dp(6) else marginStart = dp(6)
                })
            }
            if (rowProducts.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = dp(6) })
            container.addView(row)
            rowProducts.indices.forEach { index ->
                val card = row.getChildAt(index)
                card.alpha = 0f
                card.translationY = dp(12).toFloat()
                card.post { card.animate().alpha(1f).translationY(0f).setStartDelay(index * 45L).setDuration(240L).start() }
            }
        }
    }

    private fun createProductCard(product: ProductResponse): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = dp(1)
            setStrokeColor(0xFFEAE7F0.toInt())
            isClickable = true
            isFocusable = true
            foreground = selectableBackground()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val imageFrame = FrameLayout(this).apply {
            background = roundedBackground(0xFFF2EFF8.toInt(), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132))
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = product.name.orEmpty()
            visibility = View.GONE
        }
        imageFrame.addView(image, FrameLayout.LayoutParams(-1, -1))
        val placeholder = TextView(this).apply {
            text = "Ảnh sản phẩm"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF777480.toInt())
        }
        imageFrame.addView(placeholder, FrameLayout.LayoutParams(-1, -1))

        val favorite = TextView(this).apply {
            val selected = product.id in favoriteIds()
            text = if (selected) "♥" else "♡"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(if (selected) 0xFFD94D70.toInt() else 0xFF5E536A.toInt())
            background = roundedBackground(Color.WHITE, dp(22))
            elevation = dp(3).toFloat()
            contentDescription = if (selected) "Bỏ yêu thích ${product.name}" else "Lưu ${product.name} vào yêu thích"
            setOnClickListener { toggleFavorite(product, this) }
        }
        imageFrame.addView(favorite, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(7)
            marginEnd = dp(7)
        })

        val title = bodyText(product.name.orEmpty(), 13f, 0xFF24212B.toInt(), true).apply {
            maxLines = 2
            minLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(9) }
        }
        val subtitle = bodyText(listOfNotNull(product.brandName, product.categoryName).distinct().joinToString(" · ").ifBlank { "Sản phẩm cửa hàng" }, 10f, 0xFF777480.toInt()).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(5) }
        }
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(7) }
        }
        val price = bodyText(formatVnd(product.price), 14f, violet(), true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }
        val addButton = MaterialButton(this).apply {
            text = if ((product.stockQuantity ?: 0) > 0) "+" else "−"
            textSize = 20f
            minWidth = dp(48)
            minimumWidth = dp(48)
            minHeight = dp(48)
            minimumHeight = dp(48)
            isEnabled = (product.stockQuantity ?: 0) > 0
            insetTop = 0
            insetBottom = 0
            setPadding(0, 0, 0, 0)
            cornerRadius = dp(12)
            contentDescription = "Thêm ${product.name.orEmpty()} vào giỏ"
            setOnClickListener { addToCart(product, this) }
        }
        footer.addView(price)
        footer.addView(addButton)
        content.addView(imageFrame)
        content.addView(title)
        content.addView(subtitle)
        content.addView(footer)
        card.addView(content)
        card.setOnClickListener {
            val id = product.id ?: return@setOnClickListener
            startActivity(android.content.Intent(this, ProductDetailActivity::class.java).putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, id))
        }
        val imagePath = product.thumbnailUrl ?: product.images.orEmpty().firstOrNull()
        if (!imagePath.isNullOrBlank()) loadImage(imagePath, image, placeholder)
        return card
    }

    private fun loadImage(path: String, image: ImageView, placeholder: TextView) {
        val host = runCatching { URL(path).host }.getOrNull()
        if (host.equals("localhost", true) || host == "127.0.0.1") {
            placeholder.text = "Ảnh chưa có trên máy chủ"
            return
        }
        lifecycleScope.launch {
            val bitmap = try {
                withContext(Dispatchers.IO) {
                    val url = if (path.startsWith("http://") || path.startsWith("https://")) path
                    else "https://ecommerce-backend-q1wt.onrender.com/${path.trimStart('/')}"
                    URL(url).openConnection().apply { connectTimeout = 12_000; readTimeout = 20_000 }
                        .getInputStream().use(BitmapFactory::decodeStream)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (bitmap != null && !isDestroyed) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
            }
        }
    }

    private fun addToCart(product: ProductResponse, button: MaterialButton) {
        val id = product.id ?: return
        if ((product.stockQuantity ?: 0) <= 0) return
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = cartApi.addItem(CartItemRequest(id, 1))
                Toast.makeText(this@ProductCatalogActivity, response.message ?: "Đã thêm vào giỏ hàng.", Toast.LENGTH_SHORT).show()
                button.text = "✓"
                button.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).withEndAction {
                    button.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(this@ProductCatalogActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
            } finally {
                button.postDelayed({
                    if (!isDestroyed) {
                        button.isEnabled = (product.stockQuantity ?: 0) > 0
                        button.text = if (button.isEnabled) "+" else "−"
                    }
                }, 800L)
            }
        }
    }

    private fun favoriteIds(): Set<String> = getSharedPreferences("storefront_preferences", MODE_PRIVATE)
        .getStringSet("favorite_product_ids", emptySet()).orEmpty()

    private fun toggleFavorite(product: ProductResponse, button: TextView) {
        val id = product.id ?: return
        val updated = favoriteIds().toMutableSet()
        val wasFavorite = updated.remove(id)
        if (!wasFavorite) updated.add(id)
        getSharedPreferences("storefront_preferences", MODE_PRIVATE).edit()
            .putStringSet("favorite_product_ids", updated).apply()
        button.text = if (wasFavorite) "♡" else "♥"
        button.setTextColor(if (wasFavorite) 0xFF5E536A.toInt() else 0xFFD94D70.toInt())
        button.contentDescription = if (wasFavorite) "Lưu ${product.name} vào yêu thích" else "Bỏ yêu thích ${product.name}"
        button.animate().scaleX(1.16f).scaleY(1.16f).setDuration(100).withEndAction {
            button.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun roundedBackground(color: Int, radiusPx: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx.toFloat()
    }

    private fun selectableBackground(): android.graphics.drawable.Drawable? = android.util.TypedValue().let { value ->
        theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
        if (value.resourceId != 0) getDrawable(value.resourceId) else null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showStatus(message: String, visible: Boolean) {
        findViewById<TextView>(R.id.catalogStatus).apply {
            text = message
            visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
