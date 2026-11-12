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
import android.view.MotionEvent
import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.LogoutRequest
import com.example.learn_kotlin.api.CartApi
import com.example.learn_kotlin.api.CartItemRequest
import com.example.learn_kotlin.api.CartResponse
import com.example.learn_kotlin.api.CategoryApi
import com.example.learn_kotlin.api.CategoryResponse
import com.example.learn_kotlin.api.BrandApi
import com.example.learn_kotlin.api.BrandResponse
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
    private lateinit var brandApi: BrandApi
    private lateinit var cartApi: CartApi
    private lateinit var orderApi: OrderApi
    private lateinit var profileApi: ProfileApi
    private var categories: List<CategoryResponse> = emptyList()
    private var brands: List<BrandResponse> = emptyList()
    private var products: List<ProductResponse> = emptyList()
    private var allProducts: List<ProductResponse> = emptyList()
    private var showingFavorites = false
    private var loginRedirectInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ScreenLayouts.adaptive(this, ScreenLayouts.home(this)))
        applySystemBarInsets(findViewById(android.R.id.content))

        tokenStore = TokenStore(this)
        if (tokenStore.accessToken().isNullOrBlank()) {
            openLogin()
            return
        }

        productApi = ApiClient.productApi(this)
        categoryApi = ApiClient.categoryApi(this)
        brandApi = ApiClient.brandApi(this)
        cartApi = ApiClient.cartApi(this)
        orderApi = ApiClient.orderApi(this)
        profileApi = ApiClient.profileApi(this)
        setupSearch()
        setupHeaderActions()
        setupBottomNavigation()
        setupHomeInteractions()
        animateHomeEntrance()
        findViewById<EditText>(R.id.searchEditText).clearFocus()
        findViewById<ScrollView>(R.id.homeScrollView).post {
            findViewById<ScrollView>(R.id.homeScrollView).scrollTo(0, 0)
            findViewById<LinearLayout>(R.id.main).requestFocus()
        }
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
        loadGreeting()
        loadCategories()
        loadBrands()
        loadProducts()
        refreshCart(showError = false)
    }

    private fun loadGreeting() {
        lifecycleScope.launch {
            try {
                val name = profileApi.getProfile().data?.fullName?.trim()?.takeIf(String::isNotBlank)
                if (!name.isNullOrBlank()) {
                    val givenName = name.substringAfterLast(' ').ifBlank { name }
                    findViewById<TextView>(R.id.userWelcomeText).text = "Chào $givenName!"
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // The home screen remains usable if profile data is temporarily unavailable.
                android.util.Log.e("HOME_API", "Load customer greeting failed", error)
            }
        }
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

    private fun loadBrands() {
        lifecycleScope.launch {
            try {
                brands = brandApi.getAllBrands().data.orEmpty()
                renderBrands()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HOME_API", "Load brands failed", error)
                renderBrands()
            }
        }
    }

    private fun renderBrands() {
        val container = findViewById<LinearLayout>(R.id.brandItemsContainer)
        container.removeAllViews()
        val availableBrands = brands.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
        if (availableBrands.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Thương hiệu sẽ xuất hiện tại đây"
                setTextColor(Color.rgb(119, 116, 128))
                textSize = 13f
            })
            return
        }

        availableBrands.forEachIndexed { index, brand ->
            val colors = listOf(R.color.brand_primary_container, R.color.success_container, R.color.surface_background)
            val pill = MaterialCardView(this).apply {
                radius = dp(18).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(getColor(R.color.surface))
                strokeWidth = dp(1)
                setStrokeColor(getColor(R.color.outline_variant))
                isClickable = true
                isFocusable = true
                foreground = selectableBackground()
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(58)).apply {
                    marginEnd = dp(10)
                }
            }
            val content = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(9), dp(7), dp(14), dp(7))
            }
            val initial = TextView(this).apply {
                text = brand.name.orEmpty().trim().take(1).uppercase(Locale.getDefault())
                gravity = Gravity.CENTER
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(R.color.brand_primary))
                background = roundedBackground(getColor(colors[index % colors.size]), dp(15))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            }
            val name = TextView(this).apply {
                text = brand.name.orEmpty()
                maxLines = 1
                textSize = 13f
                setTextColor(getColor(R.color.on_surface))
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dp(9)
                }
            }
            content.addView(initial)
            content.addView(name)
            pill.addView(content)
            pill.setOnClickListener {
                val brandId = brand.id ?: return@setOnClickListener
                findViewById<EditText>(R.id.searchEditText).setText("")
                searchProducts(brandId = brandId)
                scrollToProducts()
            }
            container.addView(pill)
        }
    }

    private fun renderCategories() {
        val container = findViewById<LinearLayout>(R.id.categoryItemsContainer)
        container.removeAllViews()
        categories.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }.forEachIndexed { index, category ->
            val colors = listOf(R.color.brand_primary_container, R.color.success_container, R.color.surface_background)
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
            val icon = ImageView(this).apply {
                setImageResource(categoryIcon(category.name.orEmpty()))
                contentDescription = null
                setColorFilter(getColor(R.color.brand_primary))
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = roundedBackground(getColor(colors[index % colors.size]), dp(18))
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            }
            val label = TextView(this).apply {
                text = category.name.orEmpty()
                gravity = Gravity.CENTER
                maxLines = 2
                textSize = 11f
                setTextColor(getColor(R.color.on_surface_variant))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(7)
                }
            }
            item.addView(icon)
            item.addView(label)
            item.contentDescription = "Danh mục ${category.name}"
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
                text = "Danh mục sẽ xuất hiện tại đây"
                setTextColor(Color.rgb(119, 116, 128))
                textSize = 13f
                gravity = Gravity.CENTER_VERTICAL
                minHeight = dp(64)
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

    private fun setupHomeInteractions() {
        val quickSearches = findViewById<LinearLayout>(R.id.quickSearchItems)
        listOf("Tai nghe", "Thời trang", "Làm đẹp", "Gia dụng").forEachIndexed { index, term ->
            (quickSearches.getChildAt(index) as? View)?.setOnClickListener {
                findViewById<EditText>(R.id.searchEditText).setText(term)
                searchProducts(keyword = term)
                scrollToProducts()
            }
        }
        findViewById<View>(R.id.favoriteFilterButton).setOnClickListener {
            val favoriteIds = savedFavoriteIds()
            if (!showingFavorites && favoriteIds.isEmpty()) {
                toast("Chạm vào biểu tượng ♡ trên sản phẩm để lưu sản phẩm yêu thích.")
                return@setOnClickListener
            }
            showingFavorites = !showingFavorites
            val button = findViewById<MaterialButton>(R.id.favoriteFilterButton)
            button.text = if (showingFavorites) "♥  Đang xem yêu thích · Bỏ lọc" else "♡  Lọc sản phẩm yêu thích"
            renderProductGrid(if (showingFavorites) allProducts.filter { it.id in favoriteIds } else allProducts)
            if (showingFavorites) scrollToProducts()
        }
    }

    private fun animateHomeEntrance() {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        listOf(R.id.homeHeader, R.id.promotionBanner, R.id.bottomNavigation).forEachIndexed { index, id ->
            findViewById<View>(id)?.let { view ->
                view.post {
                    view.alpha = 0f
                    view.translationY = dp(12).toFloat()
                    view.animate().alpha(1f).translationY(0f).setStartDelay(index * 55L).setDuration(280L).start()
                }
            }
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
            openRootTab(ProductCatalogActivity::class.java)
        }
        findViewById<View>(R.id.navAccount).setOnClickListener {
            selectNavigation(R.id.navAccount)
            openRootTab(ProfileActivity::class.java)
        }
    }

    private fun selectNavigation(selectedId: Int) {
        val items = listOf(
            Triple(R.id.navHome, R.id.navHomeIcon, R.id.navHomeLabel),
            Triple(R.id.navCategories, R.id.navCategoriesIcon, R.id.navCategoriesLabel),
            Triple(R.id.navAccount, R.id.navAccountIcon, R.id.navAccountLabel)
        )
        items.forEach { (itemId, iconId, labelId) ->
            val selected = itemId == selectedId
            val navItem = findViewById<View>(itemId)
            navItem.setBackgroundResource(
                if (selected) R.drawable.bg_nav_selected else android.R.color.transparent
            )
            if (selected && ValueAnimator.areAnimatorsEnabled()) {
                navItem.animate().cancel()
                navItem.scaleX = 0.94f
                navItem.scaleY = 0.94f
                navItem.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            }
            val tint = getColor(if (selected) R.color.brand_primary else R.color.on_surface_variant)
            findViewById<ImageView>(iconId).setColorFilter(tint)
            findViewById<TextView>(labelId).apply {
                setTextColor(tint)
                setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
            }
        }
    }

    private fun categoryIcon(name: String): Int {
        val value = name.lowercase(Locale("vi", "VN"))
        return when {
            listOf("điện thoại", "công nghệ", "điện tử", "laptop", "máy tính").any { value.contains(it) } -> R.drawable.ic_devices_24
            listOf("thời trang", "quần áo", "giày", "phụ kiện").any { value.contains(it) } -> R.drawable.ic_fashion_24
            listOf("mỹ phẩm", "làm đẹp", "beauty").any { value.contains(it) } -> R.drawable.ic_beauty_24
            listOf("gia dụng", "nhà cửa", "nội thất").any { value.contains(it) } -> R.drawable.ic_home_24
            else -> R.drawable.ic_grid_24
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

    private fun searchProducts(keyword: String? = null, categoryId: String? = null, brandId: String? = null) {
        showProductStatus("Đang tìm sản phẩm...")
        lifecycleScope.launch {
            try {
                val response = productApi.searchProducts(
                    keyword = keyword,
                    categoryId = categoryId,
                    brandId = brandId,
                    page = 0,
                    size = 12
                )
                renderProducts(response.data?.items.orEmpty())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showProductStatus("Tìm kiếm thất bại. Chạm để thử lại.")
                findViewById<TextView>(R.id.emptyProductsText).setOnClickListener {
                    searchProducts(keyword, categoryId, brandId)
                }
                android.util.Log.e("HOME_API", "Search products failed", error)
                toast(error.toUserMessage())
            }
        }
    }

    private fun renderProducts(items: List<ProductResponse>) {
        products = items
        allProducts = items
        if (showingFavorites) {
            renderProductGrid(items.filter { it.id in savedFavoriteIds() })
        } else {
            renderProductGrid(items)
        }
    }

    private fun renderProductGrid(items: List<ProductResponse>) {
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
            rowItems.indices.forEach { index -> animateEntrance(row.getChildAt(index), rowIndex * 2 + index) }
        }
    }

    private fun createProductCard(product: ProductResponse): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(getColor(R.color.surface))
            strokeWidth = dp(1)
            setStrokeColor(getColor(R.color.outline_variant))
            isClickable = true
            isFocusable = true
            foreground = selectableBackground()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val imageFrame = FrameLayout(this).apply {
            background = roundedBackground(getColor(R.color.surface_background), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132))
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = product.name.orEmpty()
            visibility = View.GONE
        }
        imageFrame.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val placeholder = TextView(this).apply {
            text = "Chưa có ảnh"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.on_surface_variant))
        }
        imageFrame.addView(placeholder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val favorite = TextView(this).apply {
            val active = product.id in savedFavoriteIds()
            text = if (active) "♥" else "♡"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(if (active) 0xFFD94D70.toInt() else 0xFF5E536A.toInt())
            background = roundedBackground(Color.WHITE, dp(22))
            elevation = dp(3).toFloat()
            contentDescription = if (active) "Bỏ yêu thích ${product.name}" else "Lưu ${product.name} vào yêu thích"
            setOnClickListener { toggleFavorite(product, this) }
        }
        imageFrame.addView(favorite, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(7)
            marginEnd = dp(7)
        })

        val name = TextView(this).apply {
            text = product.name.orEmpty()
            maxLines = 2
            minLines = 2
            textSize = 13f
            setTextColor(getColor(R.color.on_surface))
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(9)
            }
        }
        val details = TextView(this).apply {
            text = listOfNotNull(product.brandName, product.categoryName).distinct().joinToString(" · ").ifBlank { "Sản phẩm cửa hàng" }
            maxLines = 1
            textSize = 10f
            setTextColor(getColor(R.color.on_surface_variant))
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
            setTextColor(getColor(R.color.brand_primary))
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val add = MaterialButton(this).apply {
            text = "+"
            textSize = 20f
            minWidth = dp(48)
            minimumWidth = dp(48)
            minHeight = dp(48)
            minimumHeight = dp(48)
            insetTop = 0
            insetBottom = 0
            setPadding(0, 0, 0, 0)
            cornerRadius = dp(12)
            contentDescription = "Thêm ${product.name.orEmpty()} vào giỏ"
            isEnabled = (product.stockQuantity ?: 0) > 0
            text = if (isEnabled) "+" else "−"
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
        card.setOnTouchListener { view, event ->
            if (ValueAnimator.areAnimatorsEnabled()) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.985f).scaleY(0.985f).setDuration(100).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }
            }
            false
        }

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
        findViewById<TextView>(R.id.cartBadgeText).apply {
            text = if (count > 99) "99+" else count.toString()
            visibility = if (count > 0) View.VISIBLE else View.GONE
            if (count > 0 && ValueAnimator.areAnimatorsEnabled()) {
                scaleX = 0.7f
                scaleY = 0.7f
                animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            }
        }
        findViewById<View>(R.id.cartButton).contentDescription =
            if (count == 0) "Giỏ hàng, đang trống" else "Giỏ hàng, $count sản phẩm"
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
                ApiClient.authApi(this@MainActivity).logout(LogoutRequest(TokenStore(this@MainActivity).refreshToken()))
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
        val grid = findViewById<LinearLayout>(R.id.featuredProductsGrid)
        grid.removeAllViews()
        val status = findViewById<TextView>(R.id.emptyProductsText)
        status.apply {
            text = message
            visibility = if (message.startsWith("Đang tải", ignoreCase = true) || message.startsWith("Đang tìm", ignoreCase = true)) View.GONE else View.VISIBLE
            setOnClickListener { if (message.contains("thử lại", ignoreCase = true)) loadProducts() }
        }
        if (status.visibility == View.GONE) {
            repeat(2) { rowIndex ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    if (rowIndex > 0) setPadding(0, dp(10), 0, 0)
                }
                row.addView(createProductSkeleton(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
                row.addView(createProductSkeleton(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) })
                grid.addView(row)
            }
        }
    }

    private fun createProductSkeleton(): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(getColor(R.color.surface))
            strokeWidth = dp(1)
            setStrokeColor(getColor(R.color.outline_variant))
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(12))
        }
        val placeholder = View(this).apply {
            background = roundedBackground(getColor(R.color.surface_background), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(116))
        }
        body.addView(placeholder)
        repeat(2) { index ->
            val line = View(this).apply {
                background = roundedBackground(getColor(R.color.surface_background), dp(6))
                layoutParams = LinearLayout.LayoutParams(if (index == 0) dp(130) else dp(82), dp(10)).apply {
                    topMargin = dp(if (index == 0) 12 else 8)
                }
            }
            body.addView(line)
        }
        card.addView(body)
        if (ValueAnimator.areAnimatorsEnabled()) {
            fun pulse() {
                if (!card.isAttachedToWindow) return
                card.animate().alpha(0.56f).setDuration(580).withEndAction {
                    if (card.isAttachedToWindow) card.animate().alpha(1f).setDuration(580).withEndAction { pulse() }.start()
                }.start()
            }
            card.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) = pulse()
                override fun onViewDetachedFromWindow(view: View) { view.animate().cancel() }
            })
        }
        return card
    }

    private fun savedFavoriteIds(): Set<String> = getSharedPreferences("storefront_preferences", MODE_PRIVATE)
        .getStringSet("favorite_product_ids", emptySet()).orEmpty()

    private fun toggleFavorite(product: ProductResponse, button: TextView) {
        val id = product.id ?: return
        val updated = savedFavoriteIds().toMutableSet()
        val wasFavorite = updated.remove(id)
        if (!wasFavorite) updated.add(id)
        getSharedPreferences("storefront_preferences", MODE_PRIVATE).edit()
            .putStringSet("favorite_product_ids", updated).apply()
        button.text = if (wasFavorite) "♡" else "♥"
        button.setTextColor(if (wasFavorite) 0xFF5E536A.toInt() else 0xFFD94D70.toInt())
        button.contentDescription = if (wasFavorite) "Lưu ${product.name} vào yêu thích" else "Bỏ yêu thích ${product.name}"
        button.animate().scaleX(1.18f).scaleY(1.18f).setDuration(100).withEndAction {
            button.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
        if (showingFavorites && wasFavorite) renderProductGrid(allProducts.filter { it.id in updated })
    }

    private fun animateEntrance(view: View, index: Int) {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        view.post {
            view.alpha = 0f
            view.translationY = dp(14).toFloat()
            view.animate().alpha(1f).translationY(0f)
                .setStartDelay((index.coerceAtMost(6) * 45L))
                .setDuration(260L)
                .start()
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
