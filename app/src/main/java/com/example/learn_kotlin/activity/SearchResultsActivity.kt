package com.example.learn_kotlin.activity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.animation.ValueAnimator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.CartApi
import com.example.learn_kotlin.api.CartItemRequest
import com.example.learn_kotlin.api.ProductResponse
import com.example.learn_kotlin.api.toUserMessage
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.example.learn_kotlin.applySystemBarInsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class SearchResultsActivity : AppCompatActivity() {
    private val productApi by lazy { ApiClient.productApi(this) }
    private val cartApi by lazy { ApiClient.cartApi(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ScreenLayouts.adaptive(this, ScreenLayouts.searchResults(this)))
        applySystemBarInsets(findViewById(android.R.id.content))

        val queryInput = findViewById<EditText>(R.id.searchResultsQuery)
        queryInput.setText(intent.getStringExtra(SearchActivity.EXTRA_QUERY).orEmpty())
        findViewById<View>(R.id.searchResultsBack).setOnClickListener { finish() }
        findViewById<View>(R.id.searchResultsClear).setOnClickListener {
            queryInput.text?.clear()
            queryInput.requestFocus()
            showKeyboard(queryInput)
        }
        queryInput.setOnEditorActionListener { _, actionId, event ->
            val submit = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_UP)
            if (submit) {
                search(queryInput.text?.toString().orEmpty())
                hideKeyboard(queryInput)
                true
            } else false
        }

        animatePageEntrance()
        search(queryInput.text?.toString().orEmpty())
    }

    private fun search(rawQuery: String) {
        val query = rawQuery.trim()
        val status = findViewById<TextView>(R.id.searchResultsStatus)
        val results = findViewById<LinearLayout>(R.id.searchResultsList)
        results.removeAllViews()
        if (query.isBlank()) {
            status.text = "Nhập từ khóa để tìm sản phẩm."
            return
        }
        status.text = "Đang tìm ‘$query’…"
        lifecycleScope.launch {
            try {
                val products = productApi.searchProducts(keyword = query, page = 0, size = 30).data?.items.orEmpty()
                renderResults(query, products)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                status.text = error.toUserMessage()
            }
        }
    }

    private fun renderResults(query: String, products: List<ProductResponse>) {
        val container = findViewById<LinearLayout>(R.id.searchResultsList)
        val status = findViewById<TextView>(R.id.searchResultsStatus)
        container.removeAllViews()
        if (products.isEmpty()) {
            status.text = "Không tìm thấy sản phẩm cho ‘$query’. Thử từ khóa khác nhé."
            return
        }
        status.text = "Tìm thấy ${products.size} sản phẩm cho ‘$query’"
        products.chunked(2).forEachIndexed { rowIndex, rowProducts ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    if (rowIndex > 0) topMargin = dp(12)
                }
            }
            rowProducts.forEachIndexed { index, product ->
                val card = createProductCard(product)
                row.addView(card, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (index == 0) marginEnd = dp(6) else marginStart = dp(6)
                })
                animateResultCard(card, rowIndex * 2 + index)
            }
            if (rowProducts.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = dp(6) })
            container.addView(row)
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
            setPadding(dp(9), dp(9), dp(9), dp(10))
        }
        val imageFrame = FrameLayout(this).apply {
            background = roundedBackground(0xFFF2EFF8.toInt(), dp(14))
            layoutParams = LinearLayout.LayoutParams(-1, dp(126))
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = product.name.orEmpty()
            visibility = View.GONE
        }
        imageFrame.addView(image, FrameLayout.LayoutParams(-1, -1))
        val placeholder = TextView(this).apply {
            text = "Ảnh sản phẩm"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(0xFF777480.toInt())
        }
        imageFrame.addView(placeholder, FrameLayout.LayoutParams(-1, -1))
        val title = bodyText(product.name.orEmpty(), 12f, 0xFF24212B.toInt(), true).apply {
            maxLines = 2
            minLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        }
        val subtitle = bodyText(listOfNotNull(product.brandName, product.categoryName)
            .distinct().joinToString(" · ").ifBlank { "Sản phẩm cửa hàng" }, 10f, 0xFF777480.toInt()).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) }
        }
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
        }
        footer.addView(bodyText(formatVnd(product.price), 12f, violet(), true).apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(9, 12, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        })
        footer.addView(MaterialButton(this).apply {
            text = if ((product.stockQuantity ?: 0) > 0) "+" else "−"
            textSize = 18f
            minWidth = dp(42)
            minimumWidth = dp(42)
            minHeight = dp(42)
            minimumHeight = dp(42)
            isEnabled = (product.stockQuantity ?: 0) > 0
            insetTop = 0
            insetBottom = 0
            setPadding(0, 0, 0, 0)
            cornerRadius = dp(12)
            contentDescription = "Thêm ${product.name.orEmpty()} vào giỏ"
            setOnClickListener { addToCart(product, this) }
        })
        content.addView(imageFrame)
        content.addView(title)
        content.addView(subtitle)
        content.addView(footer)
        card.addView(content)
        card.setOnClickListener {
            val id = product.id ?: return@setOnClickListener
            startActivity(Intent(this, ProductDetailActivity::class.java)
                .putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, id))
        }
        val imagePath = product.thumbnailUrl ?: product.images.orEmpty().firstOrNull()
        if (!imagePath.isNullOrBlank()) loadImage(imagePath, image, placeholder)
        return card
    }

    private fun loadImage(path: String, image: ImageView, placeholder: TextView) {
        if (runCatching { URL(path).host }.getOrNull().let { it.equals("localhost", true) || it == "127.0.0.1" }) return
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
                android.widget.Toast.makeText(this@SearchResultsActivity,
                    response.message ?: "Đã thêm vào giỏ hàng.", android.widget.Toast.LENGTH_SHORT).show()
                button.text = "✓"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.widget.Toast.makeText(this@SearchResultsActivity, error.toUserMessage(), android.widget.Toast.LENGTH_LONG).show()
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

    private fun roundedBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun selectableBackground(): android.graphics.drawable.Drawable? = android.util.TypedValue().let { value ->
        theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
        if (value.resourceId != 0) getDrawable(value.resourceId) else null
    }

    private fun animatePageEntrance() {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        findViewById<View>(R.id.main).apply {
            alpha = 0f
            translationY = dp(12).toFloat()
            animate().alpha(1f).translationY(0f).setDuration(280L).start()
        }
    }

    private fun animateResultCard(card: View, index: Int) {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        card.alpha = 0f
        card.translationY = dp(12).toFloat()
        card.post {
            card.animate().alpha(1f).translationY(0f)
                .setStartDelay(index.coerceAtMost(7) * 45L)
                .setDuration(240L)
                .start()
        }
    }

    private fun showKeyboard(input: EditText) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(input: EditText) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
    }
}
