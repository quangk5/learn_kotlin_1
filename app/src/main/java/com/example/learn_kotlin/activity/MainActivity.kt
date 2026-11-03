package com.example.learn_kotlin.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.CartApi
import com.example.learn_kotlin.api.CartItemRequest
import com.example.learn_kotlin.api.CartResponse
import com.example.learn_kotlin.api.CategoryApi
import com.example.learn_kotlin.api.CategoryResponse
import com.example.learn_kotlin.api.CheckoutRequest
import com.example.learn_kotlin.api.OrderApi
import com.example.learn_kotlin.api.OrderResponse
import com.example.learn_kotlin.api.ProfileApi
import com.example.learn_kotlin.api.ProductApi
import com.example.learn_kotlin.api.ProductResponse
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.applySystemBarInsets
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var tokenStore: TokenStore
    private lateinit var productApi: ProductApi
    private lateinit var categoryApi: CategoryApi
    private lateinit var cartApi: CartApi
    private lateinit var orderApi: OrderApi
    private lateinit var profileApi: ProfileApi
    private var categories: List<CategoryResponse> = emptyList()
    private var products: List<ProductResponse> = emptyList()
    private var loginRedirectInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)
        applySystemBarInsets(findViewById(android.R.id.content))

        tokenStore = TokenStore(this)
        if (tokenStore.accessToken().isNullOrBlank()) {
            openLogin()
            return
        }

        productApi = ApiClient.productApi(this)
        categoryApi = ApiClient.categoryApi(this)
        cartApi = ApiClient.cartApi(this)
        orderApi = ApiClient.orderApi(this)
        profileApi = ApiClient.profileApi(this)
        setupSearch()
        setupHeaderActions()
        setupBottomNavigation()
        loadHomeData()
    }

    override fun onResume() {
        super.onResume()
        if (::tokenStore.isInitialized && tokenStore.accessToken().isNullOrBlank()) {
            openLogin()
        } else if (::tokenStore.isInitialized) {
            selectNavigation(R.id.navHome)
        }
    }

    private fun loadHomeData() {
        loadCategories()
        loadProducts()
        refreshCart(showError = false)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                categories = categoryApi.getAllCategories().data.orEmpty()
                renderCategories()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val container = findViewById<LinearLayout>(R.id.categoryItemsContainer)
                container.removeAllViews()
                container.addView(TextView(this@MainActivity).apply {
                    text = "Không tải được danh mục"
                    setTextColor(Color.rgb(119, 116, 128))
                    textSize = 13f
                })
                android.util.Log.e("HOME_API", "Load categories failed", error)
            }
        }
    }

    private fun renderCategories() {
        val container = findViewById<LinearLayout>(R.id.categoryItemsContainer)
        container.removeAllViews()
        categories.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }.forEachIndexed { index, category ->
            val colors = listOf("#EEE9FA", "#FFF0E8", "#E8F4EF", "#FCEAF1", "#FFF6DF")
            val symbols = listOf("▣", "◇", "⌂", "✦", "◉")
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                background = selectableBackground()
                layoutParams = LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(10)
                }
                setPadding(dp(2), dp(2), dp(2), dp(4))
            }
            val icon = TextView(this).apply {
                text = symbols[index % symbols.size]
                gravity = Gravity.CENTER
                textSize = 23f
                setTextColor(Color.rgb(104, 70, 184))
                background = roundedBackground(Color.parseColor(colors[index % colors.size]), dp(18))
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            }
            val label = TextView(this).apply {
                text = category.name.orEmpty()
                gravity = Gravity.CENTER
                maxLines = 2
                textSize = 11f
                setTextColor(Color.rgb(81, 77, 89))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(7)
                }
            }
            item.addView(icon)
            item.addView(label)
            item.setOnClickListener {
                val categoryId = category.id ?: return@setOnClickListener
                findViewById<EditText>(R.id.searchEditText).setText("")
                searchProducts(categoryId = categoryId)
                scrollToProducts()
            }
            container.addView(item)
        }
        if (categories.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Chưa có danh mục"
                setTextColor(Color.rgb(119, 116, 128))
                textSize = 13f
            })
        }
    }

    private fun setupSearch() {
        findViewById<EditText>(R.id.searchEditText).setOnEditorActionListener { input, actionId, event ->
            val searchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            val enterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (searchAction || enterKey) {
                searchProducts(keyword = input.text?.toString()?.trim()?.takeIf(String::isNotBlank))
                true
            } else false
        }
    }

    private fun setupHeaderActions() {
        findViewById<View>(R.id.notificationButton).setOnClickListener {
            toast("Thông báo sẽ được bật khi API thông báo được cấu hình.")
        }
        findViewById<View>(R.id.cartButton).setOnClickListener { openCart() }
        findViewById<View>(R.id.promotionBanner).setOnClickListener { scrollToProducts() }
        findViewById<View>(R.id.shopNowButton).setOnClickListener { scrollToProducts() }
        findViewById<View>(R.id.viewAllCategories).setOnClickListener { scrollToCategories() }
        findViewById<View>(R.id.viewAllProducts).setOnClickListener {
            startActivity(Intent(this, ProductCatalogActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            selectNavigation(R.id.navHome)
            findViewById<ScrollView>(R.id.homeScrollView).smoothScrollTo(0, 0)
        }
        findViewById<View>(R.id.navCategories).setOnClickListener {
            selectNavigation(R.id.navCategories)
            startActivity(Intent(this, ProductCatalogActivity::class.java))
        }
        findViewById<View>(R.id.navAccount).setOnClickListener {
            selectNavigation(R.id.navAccount)
            showAccountActions()
        }
    }

    private fun selectNavigation(selectedId: Int) {
        val ids = listOf(R.id.navHome, R.id.navCategories, R.id.navAccount)
        ids.forEach { id ->
            findViewById<TextView>(id).setTextColor(
                if (id == selectedId) Color.rgb(104, 70, 184) else Color.rgb(150, 147, 157)
            )
        }
    }

    private fun loadProducts() {
        showProductStatus("Đang tải sản phẩm...")
        lifecycleScope.launch {
            try {
                val response = productApi.getActiveProducts(page = 0, size = 12)
                renderProducts(response.data?.items.orEmpty())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showProductStatus("Không tải được sản phẩm. Chạm để thử lại.")
                findViewById<TextView>(R.id.emptyProductsText).setOnClickListener { loadProducts() }
                android.util.Log.e("HOME_API", "Load products failed", error)
            }
        }
    }

    private fun searchProducts(keyword: String? = null, categoryId: String? = null) {
        showProductStatus("Đang tìm sản phẩm...")
        lifecycleScope.launch {
            try {
                val response = productApi.searchProducts(
                    keyword = keyword,
                    categoryId = categoryId,
                    page = 0,
                    size = 12
                )
                renderProducts(response.data?.items.orEmpty())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showProductStatus("Tìm kiếm thất bại. Chạm để thử lại.")
                findViewById<TextView>(R.id.emptyProductsText).setOnClickListener {
                    searchProducts(keyword, categoryId)
                }
                android.util.Log.e("HOME_API", "Search products failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun renderProducts(items: List<ProductResponse>) {
        products = items
        val grid = findViewById<LinearLayout>(R.id.featuredProductsGrid)
        grid.removeAllViews()
        findViewById<TextView>(R.id.emptyProductsText).visibility = View.GONE

        if (items.isEmpty()) {
            showProductStatus("Chưa có sản phẩm phù hợp.")
            return
        }

        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    if (rowIndex > 0) topMargin = dp(12)
                }
            }
            rowItems.forEachIndexed { index, product ->
                row.addView(createProductCard(product).apply {
                    val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    if (index == 0) params.marginEnd = dp(6) else params.marginStart = dp(6)
                    layoutParams = params
                })
            }
            if (rowItems.size == 1) {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = dp(6) }
                })
            }
            grid.addView(row)
        }
    }

    private fun createProductCard(product: ProductResponse): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = dp(1)
            setStrokeColor(Color.rgb(239, 237, 243))
            isClickable = true
            isFocusable = true
            foreground = selectableBackground()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val imageFrame = FrameLayout(this).apply {
            background = roundedBackground(Color.rgb(242, 239, 248), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132))
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = product.name.orEmpty()
            visibility = View.GONE
        }
        imageFrame.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val placeholder = TextView(this).apply {
            text = "Ảnh sản phẩm"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(145, 140, 155))
        }
        imageFrame.addView(placeholder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val name = TextView(this).apply {
            text = product.name.orEmpty()
            maxLines = 2
            minLines = 2
            textSize = 13f
            setTextColor(Color.rgb(48, 45, 54))
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(9)
            }
        }
        val details = TextView(this).apply {
            text = listOfNotNull(product.brandName, product.categoryName).distinct().joinToString(" · ").ifBlank { "Sản phẩm cửa hàng" }
            maxLines = 1
            textSize = 10f
            setTextColor(Color.rgb(130, 125, 137))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(5)
            }
        }
        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(7)
            }
        }
        val price = TextView(this).apply {
            text = formatPrice(product.price ?: 0.0)
            textSize = 14f
            setTextColor(Color.rgb(104, 70, 184))
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val add = MaterialButton(this).apply {
            text = "+"
            textSize = 20f
            minWidth = dp(38)
            minimumWidth = dp(38)
            minHeight = dp(38)
            minimumHeight = dp(38)
            insetTop = 0
            insetBottom = 0
            setPadding(0, 0, 0, 0)
            cornerRadius = dp(12)
            contentDescription = "Thêm ${product.name.orEmpty()} vào giỏ"
            setOnClickListener { addToCart(product) }
        }
        bottom.addView(price)
        bottom.addView(add)
        content.addView(imageFrame)
        content.addView(name)
        content.addView(details)
        content.addView(bottom)
        card.addView(content)
        card.setOnClickListener { loadProductDetails(product) }

        val imageUrl = product.thumbnailUrl ?: product.images.orEmpty().firstOrNull()
        if (!imageUrl.isNullOrBlank()) loadProductImage(imageUrl, image, placeholder)
        return card
    }

    private fun loadProductImage(path: String, image: ImageView, placeholder: TextView) {
        val parsedUrl = runCatching { URL(path) }.getOrNull()
        if (parsedUrl?.host?.equals("localhost", ignoreCase = true) == true || parsedUrl?.host == "127.0.0.1") {
            placeholder.text = "Ảnh chưa có trên máy chủ"
            return
        }
        lifecycleScope.launch {
            val bitmap = try {
                withContext(Dispatchers.IO) {
                    val fullUrl = if (path.startsWith("http://") || path.startsWith("https://")) path
                    else "https://ecommerce-backend-q1wt.onrender.com/${path.trimStart('/')}"
                    URL(fullUrl).openConnection().apply {
                        connectTimeout = 15_000
                        readTimeout = 20_000
                    }.getInputStream().use { stream -> BitmapFactory.decodeStream(stream) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (bitmap != null) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
            }
        }
    }

    private fun loadProductDetails(product: ProductResponse) {
        val id = product.id
        if (id.isNullOrBlank()) return
        startActivity(Intent(this, ProductDetailActivity::class.java).putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, id))
    }

    private fun addToCart(product: ProductResponse) {
        val productId = product.id
        if (productId.isNullOrBlank()) {
            toast("Sản phẩm thiếu mã định danh nên chưa thể thêm vào giỏ.")
            return
        }
        if ((product.stockQuantity ?: 0) <= 0) {
            toast("Sản phẩm hiện không còn hàng.")
            return
        }
        lifecycleScope.launch {
            try {
                val response = cartApi.addItem(CartItemRequest(productId, 1))
                response.data?.let(::updateCartBadge)
                toast(response.message ?: "Đã thêm sản phẩm vào giỏ hàng.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Add cart item failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun refreshCart(showError: Boolean) {
        lifecycleScope.launch {
            try {
                cartApi.getCart().data?.let(::updateCartBadge)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Load cart failed", error)
                if (showError) toast(error.toUserMessage())
            }
        }
    }

    private fun updateCartBadge(cart: CartResponse) {
        val countFromItems = cart.items.orEmpty().sumOf { it.quantity ?: 0 }
        val count = maxOf(cart.totalItems ?: 0, countFromItems)
        findViewById<MaterialButton>(R.id.cartButton).text = if (count == 0) "🛒" else "🛒 $count"
    }

    private fun openCart() {
        startActivity(Intent(this, CartActivity::class.java))
    }

    private fun clearCart() {
        lifecycleScope.launch {
            try {
                cartApi.clearCart()
                refreshCart(showError = false)
                toast("Đã xóa sản phẩm khỏi giỏ hàng.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Clear cart failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun showAccountActions() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    private fun chooseShippingAddress() {
        lifecycleScope.launch {
            try {
                val addresses = profileApi.getAddresses().data.orEmpty()
                    .filter { !it.id.isNullOrBlank() }
                if (addresses.isEmpty()) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Chưa có địa chỉ giao hàng")
                        .setMessage("Tài khoản chưa có địa chỉ. Hãy thêm địa chỉ giao hàng rồi thử đặt hàng lại.")
                        .setPositiveButton("Đóng", null)
                        .show()
                    return@launch
                }
                val labels = addresses.map { address ->
                    listOfNotNull(address.recipientName, address.streetAddress, address.ward, address.district, address.province)
                        .joinToString(", ")
                }.toTypedArray()
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Chọn địa chỉ giao hàng")
                    .setItems(labels) { _, index ->
                        val addressId = addresses[index].id ?: return@setItems
                        placeOrder(addressId)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Load shipping addresses failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun placeOrder(addressId: String) {
        lifecycleScope.launch {
            try {
                val response = orderApi.checkout(CheckoutRequest(addressId, paymentMethod = "COD"))
                val order = response.data
                refreshCart(showError = false)
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Đặt hàng thành công")
                    .setMessage(order?.orderCode?.let { "Mã đơn hàng: $it" } ?: response.message.orEmpty())
                    .setPositiveButton("Đóng", null)
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Checkout failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun showMyOrders() {
        lifecycleScope.launch {
            try {
                val orders = orderApi.getMyOrders(page = 0, size = 20).data?.items.orEmpty()
                if (orders.isEmpty()) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Đơn hàng của tôi")
                        .setMessage("Bạn chưa có đơn hàng nào.")
                        .setPositiveButton("Đóng", null)
                        .show()
                    return@launch
                }
                val labels = orders.map { order ->
                    "${order.orderCode ?: "Đơn hàng"} · ${order.status ?: "Đang xử lý"} · ${formatPrice(order.finalAmount ?: 0.0)}"
                }.toTypedArray()
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Đơn hàng của tôi")
                    .setItems(labels) { _, index -> showOrderDetails(orders[index]) }
                    .setNegativeButton("Đóng", null)
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Load orders failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun showOrderDetails(order: OrderResponse) {
        val items = order.items.orEmpty().joinToString("\n") {
            "${it.productName.orEmpty()} ×${it.quantity ?: 0} · ${formatPrice(it.subTotal ?: 0.0)}"
        }
        val details = "$items\n\nTrạng thái: ${order.status ?: "—"}\nTổng tiền: ${formatPrice(order.finalAmount ?: 0.0)}"
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(order.orderCode ?: "Chi tiết đơn hàng")
            .setMessage(details)
            .setNegativeButton("Đóng", null)
        val orderId = order.id
        if (order.status.equals("PENDING", ignoreCase = true) && !orderId.isNullOrBlank()) {
            dialog.setPositiveButton("Hủy đơn") { _, _ -> cancelOrder(orderId) }
        }
        dialog.show()
    }

    private fun cancelOrder(orderId: String) {
        lifecycleScope.launch {
            try {
                val response = orderApi.cancelOrder(orderId)
                toast(response.message ?: "Đã gửi yêu cầu hủy đơn.")
                showMyOrders()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Cancel order failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                ApiClient.authApi(this@MainActivity).logout()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Logout request failed", error)
            } finally {
                tokenStore.clear()
                openLogin()
            }
        }
    }

    private fun showProductStatus(message: String) {
        findViewById<LinearLayout>(R.id.featuredProductsGrid).removeAllViews()
        findViewById<TextView>(R.id.emptyProductsText).apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun scrollToCategories() {
        val target = findViewById<View>(R.id.categoriesScrollView)
        findViewById<ScrollView>(R.id.homeScrollView).smoothScrollTo(0, target.top)
    }

    private fun scrollToProducts() {
        val target = findViewById<View>(R.id.featuredProductsGrid)
        findViewById<ScrollView>(R.id.homeScrollView).smoothScrollTo(0, target.top)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun formatPrice(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)

    private fun roundedBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun selectableBackground(): android.graphics.drawable.Drawable? =
        android.util.TypedValue().let { value ->
            theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
            if (value.resourceId != 0) getDrawable(value.resourceId) else null
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun openLogin() {
        if (loginRedirectInProgress) return
        loginRedirectInProgress = true
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
