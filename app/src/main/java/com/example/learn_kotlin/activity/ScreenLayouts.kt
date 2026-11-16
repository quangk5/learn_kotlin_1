package com.example.learn_kotlin.activity

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.view.View.MeasureSpec
import com.example.learn_kotlin.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

/** Kotlin authored View layouts retained for screens whose controllers use view IDs. */
internal object ScreenLayouts {
    private const val BG = 0xFFF7F6FA.toInt()
    private const val INK = 0xFF24212B.toInt()
    private const val MUTED = 0xFF777480.toInt()
    private const val BRAND = 0xFF6846B8.toInt()

    fun adaptive(context: Context, content: View): View = AdaptiveScreenFrame(context, content)

    fun welcome(context: Context): View = context.column().apply {
        id = R.id.main
        gravity = Gravity.CENTER
        setPadding(context.dp(24), context.dp(24), context.dp(24), context.dp(24))
        addView(context.label("Chào mừng bạn đến với app", 26f, INK, true).apply {
            gravity = Gravity.CENTER
            id = R.id.textView
        }, context.matchWrap().apply { bottomMargin = context.dp(24) })
        addView(context.actionButton("Đăng nhập", R.id.login))
        addView(context.actionButton("Tạo tài khoản", R.id.signup), context.matchWrap().apply { topMargin = context.dp(12) })
    }

    fun verifyOtp(context: Context): View = context.column().apply {
        id = R.id.main
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(context.dp(24), context.dp(32), context.dp(24), context.dp(24))
        addView(context.label("Xác thực email", 26f, BRAND, true), context.matchWrap())
        addView(context.label("Mã xác thực đã được gửi đến email của bạn", 15f, MUTED).apply {
            id = R.id.txt_verification_email
            gravity = Gravity.CENTER
        }, context.matchWrap().apply { topMargin = context.dp(12) })
        addView(context.input("Mật khẩu đã đăng ký", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, R.id.txt_verification_password), context.matchWrap().apply { topMargin = context.dp(18) })
        addView(context.input("Mã OTP gồm 6 số", InputType.TYPE_CLASS_NUMBER, R.id.txt_otp).apply { filters = arrayOf(android.text.InputFilter.LengthFilter(6)); gravity = Gravity.CENTER }, context.matchWrap().apply { topMargin = context.dp(12) })
        addView(context.actionButton("Xác thực", R.id.btn_verify_otp), context.matchWrap().apply { topMargin = context.dp(16) })
        addView(context.outlinedButton("Gửi lại mã OTP", R.id.btn_resend_otp), context.matchWrap().apply { topMargin = context.dp(8) })
    }

    fun profile(context: Context): View = context.column().apply {
        addView(context.scrollContent {
            addHeader(context, "Hồ sơ", R.id.profileBack)
            val card = context.cardColumn().apply {
                addView(context.label("👤", 30f, BRAND, true))
                addView(context.label("Đang tải tài khoản…", 21f, INK, true).apply { id = R.id.profileName }, context.matchWrap().apply { topMargin = context.dp(10) })
                addView(context.label("", 14f, MUTED).apply { id = R.id.profileEmail }, context.matchWrap())
                addView(context.label("", 14f, MUTED).apply { id = R.id.profilePhone }, context.matchWrap())
                addView(context.outlinedButton("Chỉnh sửa hồ sơ", R.id.profileEditButton), context.wrapWrap().apply { topMargin = context.dp(12) })
            }
            addView(card, context.matchWrap().apply { bottomMargin = context.dp(16) })
            listOf("Sổ địa chỉ" to R.id.profileAddressesButton, "Đơn hàng của tôi" to R.id.profileOrdersButton, "Đổi mật khẩu" to R.id.profilePasswordButton, "Đăng xuất" to R.id.profileLogoutButton).forEachIndexed { i, (title, id) ->
                addView(context.outlinedButton(title, id), context.matchWrap().apply { bottomMargin = context.dp(if (i == 3) 24 else 10) })
            }
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(context.bottomNavigation(R.id.navAccount), context.matchWrap())
    }

    fun addressForm(context: Context, editing: Boolean): View = context.scrollContent {
        addHeader(context, if (editing) "Chỉnh sửa địa chỉ" else "Địa chỉ giao hàng", R.id.addressFormBack)
        val fields = listOf(
            Triple("Tên người nhận", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, R.id.addressRecipient),
            Triple("Số điện thoại", InputType.TYPE_CLASS_PHONE, R.id.addressPhone),
            Triple("Tỉnh / thành phố", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, R.id.addressProvince),
            Triple("Quận / huyện", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, R.id.addressDistrict),
            Triple("Phường / xã", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, R.id.addressWard),
            Triple("Số nhà, tên đường", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, R.id.addressStreet)
        )
        fields.forEach { (hint, type, id) -> addView(context.input(hint, type, id), context.matchWrap().apply { topMargin = context.dp(10) }) }
        addView(CheckBox(context).apply { id = R.id.addressDefaultCheck; text = "Đặt làm địa chỉ mặc định"; minHeight = context.dp(48) }, context.matchWrap().apply { topMargin = context.dp(8) })
        addView(context.label("", 13f, 0xFFB3261E.toInt()).apply { id = R.id.addressFormError; visibility = View.GONE }, context.matchWrap())
        addView(context.actionButton(if (editing) "Cập nhật địa chỉ" else "Lưu địa chỉ", R.id.addressSaveButton), context.matchWrap().apply { topMargin = context.dp(12) })
    }

    fun addresses(context: Context): View = context.column().apply {
        addHeader(context, "Sổ địa chỉ", R.id.addressesBack)
        addView(context.label("", 14f, MUTED).apply { id = R.id.addressesStatus; gravity = Gravity.CENTER }, context.matchWrap())
        addView(context.scrollContent(R.id.addressesContainer), LinearLayout.LayoutParams(-1, 0, 1f))
        addView(context.actionButton("+ Thêm địa chỉ", R.id.addressAddButton), context.matchWrap().apply { setMargins(context.dp(16), context.dp(8), context.dp(16), context.dp(16)) })
    }

    fun cart(context: Context): View = context.column().apply {
        addHeader(context, "Giỏ hàng", R.id.cartBack, context.label("Xóa tất cả", 13f, BRAND).apply { id = R.id.cartClear })
        addView(context.label("", 14f, MUTED).apply { id = R.id.cartStatus; gravity = Gravity.CENTER }, context.matchWrap())
        addView(context.row().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(18), context.dp(4), context.dp(18), context.dp(4))
            addView(CheckBox(context).apply {
                id = R.id.cartSelectAll
                text = "Chọn tất cả"
                textSize = 13f
                setTextColor(INK)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(context.label("", 12f, MUTED).apply {
                id = R.id.cartSelectionCount
                gravity = Gravity.CENTER_VERTICAL
            })
        }, context.matchWrap())
        addView(context.scrollContent(R.id.cartItemsContainer), LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = context.column().apply {
            setPadding(context.dp(18), context.dp(14), context.dp(18), context.dp(16))
            addView(context.label("Tổng tiền đã chọn", 13f, MUTED))
            addView(context.label("0 ₫", 20f, INK, true).apply { id = R.id.cartTotal }, context.matchWrap().apply { topMargin = context.dp(4) })
            addView(context.actionButton("Tiếp tục đặt hàng", R.id.cartCheckout), context.matchWrap().apply { topMargin = context.dp(12) })
        }
        addView(footer, context.matchWrap())
    }

    fun checkout(context: Context): View = context.column().apply {
        addHeader(context, "Xác nhận đơn hàng", R.id.checkoutBack)
        val form = context.scrollContent {
            addView(context.label("Địa chỉ giao hàng", 18f, INK, true))
            addView(context.label("Đang tải địa chỉ…", 13f, MUTED).apply { id = R.id.checkoutAddressStatus }, context.matchWrap().apply { topMargin = context.dp(8) })
            addView(RadioGroup(context).apply { id = R.id.checkoutAddressChoices; orientation = RadioGroup.VERTICAL }, context.matchWrap())
            addView(context.outlinedButton("+ Thêm địa chỉ mới", R.id.checkoutAddAddress), context.matchWrap().apply { topMargin = context.dp(8) })
            addView(context.label("Phương thức thanh toán", 18f, INK, true), context.matchWrap().apply { topMargin = context.dp(22) })
            addView(RadioGroup(context).apply {
                id = R.id.paymentMethodGroup
                orientation = RadioGroup.VERTICAL
                addView(RadioButton(context).apply { id = R.id.paymentCod; text = "Thanh toán khi nhận hàng (COD)"; isChecked = true })
                addView(RadioButton(context).apply { id = R.id.paymentSepay; text = "Quét VietQR · SePay" })
            }, context.matchWrap())
            addView(context.input("Ghi chú đơn hàng", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, R.id.checkoutNote).apply { minLines = 2; gravity = Gravity.TOP }, context.matchWrap().apply { topMargin = context.dp(14) })
            addView(context.label("", 13f, 0xFFB3261E.toInt()).apply { id = R.id.checkoutStatus; visibility = View.GONE }, context.matchWrap())
        }
        addView(form, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(context.actionButton("Đặt hàng", R.id.checkoutSubmit), context.matchWrap().apply { setMargins(context.dp(16), context.dp(8), context.dp(16), context.dp(16)) })
    }

    fun orders(context: Context): View = context.column().apply {
        addHeader(context, "Đơn hàng của tôi", R.id.ordersBack)
        addView(context.label("", 14f, MUTED).apply { id = R.id.ordersStatus; gravity = Gravity.CENTER }, context.matchWrap())
        addView(context.scrollContent(R.id.ordersContainer), LinearLayout.LayoutParams(-1, 0, 1f))
    }

    fun catalog(context: Context): View = context.column().apply {
        addHeader(context, "Khám phá sản phẩm", R.id.catalogBack, context.label("🛒", 19f, BRAND).apply { id = R.id.catalogCart })
        addView(context.input("Tìm sản phẩm", InputType.TYPE_CLASS_TEXT, R.id.catalogSearch).apply { imeOptions = EditorInfo.IME_ACTION_SEARCH }, context.matchWrap().apply { setMargins(context.dp(16), context.dp(8), context.dp(16), context.dp(8)) })
        addView(ScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(context.row().apply { id = R.id.catalogCategoryContainer }) }, context.matchWrap())
        addView(context.label("", 14f, MUTED).apply { id = R.id.catalogStatus; gravity = Gravity.CENTER }, context.matchWrap())
        addView(context.scrollContent(R.id.catalogProductContainer), LinearLayout.LayoutParams(-1, 0, 1f))
        addView(context.bottomNavigation(R.id.navCategories), context.matchWrap())
    }

    fun detail(context: Context): View = context.column().apply {
        addHeader(context, "Chi tiết sản phẩm", R.id.detailBack, context.label("🛒", 19f, BRAND).apply { id = R.id.detailCart })
        addView(context.scrollContent {
            addView(FrameLayout(context).apply {
                addView(ImageView(context).apply { id = R.id.detailImage; scaleType = ImageView.ScaleType.CENTER_CROP }, FrameLayout.LayoutParams(-1, context.dp(280)))
                addView(context.label("Ảnh sản phẩm", 15f, MUTED).apply { id = R.id.detailImagePlaceholder; gravity = Gravity.CENTER })
            }, context.matchWrap().apply { bottomMargin = context.dp(16) })
            addView(context.label("", 22f, INK, true).apply { id = R.id.detailName }, context.matchWrap())
            addView(context.label("", 13f, MUTED).apply { id = R.id.detailCategory }, context.matchWrap())
            addView(context.label("", 13f, MUTED).apply { id = R.id.detailBrand }, context.matchWrap())
            addView(context.label("", 22f, BRAND, true).apply { id = R.id.detailPrice }, context.matchWrap().apply { topMargin = context.dp(10) })
            addView(context.label("", 13f, 0xFF43845F.toInt()).apply { id = R.id.detailStock }, context.matchWrap())
            addView(context.label("Mô tả", 17f, INK, true), context.matchWrap().apply { topMargin = context.dp(20) })
            addView(context.label("", 14f, MUTED).apply { id = R.id.detailDescription }, context.matchWrap().apply { topMargin = context.dp(6) })
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(context.row().apply {
            addView(context.outlinedButton("Thêm vào giỏ", R.id.detailAddToCart), LinearLayout.LayoutParams(0, context.dp(52), 1f))
            addView(context.actionButton("Mua ngay", R.id.detailBuyNow), LinearLayout.LayoutParams(0, context.dp(52), 1f).apply { marginStart = context.dp(8) })
        }, context.matchWrap().apply { setMargins(context.dp(16), context.dp(8), context.dp(16), context.dp(16)) })
    }

    fun payment(context: Context): View = context.column().apply {
        id = R.id.paymentRoot
        addHeader(context, "Thanh toán an toàn", R.id.paymentBack)
        addView(context.scrollContent {
            val summary = context.cardColumn(BRAND).apply {
                addView(context.label("TỔNG THANH TOÁN", 12f, 0xFFE6DDF8.toInt(), true))
                addView(context.label("Đang tải…", 28f, Color.WHITE, true).apply { id = R.id.paymentAmount }, context.matchWrap())
                addView(context.label("Đơn hàng", 13f, 0xFFF0EAFE.toInt()).apply { id = R.id.paymentOrderCode }, context.matchWrap())
            }
            addView(summary, context.matchWrap().apply { bottomMargin = context.dp(16) })
            addView(context.label("Quét mã VietQR để thanh toán", 19f, INK, true), context.matchWrap())
            addView(FrameLayout(context).apply {
                addView(ImageView(context).apply { id = R.id.paymentQrImage; visibility = View.GONE; scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = "Mã QR thanh toán" }, FrameLayout.LayoutParams(-1, context.dp(270)))
                addView(context.label("Đang tạo mã QR…", 14f, MUTED).apply { id = R.id.paymentQrPlaceholder; gravity = Gravity.CENTER })
            }, context.matchWrap().apply { topMargin = context.dp(12) })
            addView(context.label("Đang chờ xác nhận thanh toán…", 14f, MUTED, true).apply { id = R.id.paymentStatus; gravity = Gravity.CENTER; setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12)) }, context.matchWrap().apply { topMargin = context.dp(12) })
            addView(context.cardColumn().apply {
                addView(context.label("Thông tin chuyển khoản", 16f, INK, true))
                addView(context.label("Ngân hàng", 14f, INK, true).apply { id = R.id.paymentBank }, context.matchWrap().apply { topMargin = context.dp(12) })
                addView(context.label("Số tài khoản: —", 14f, MUTED).apply { id = R.id.paymentAccount }, context.matchWrap())
                addView(context.label("Chủ tài khoản: —", 14f, MUTED).apply { id = R.id.paymentAccountHolder }, context.matchWrap())
                addView(context.label("Nội dung chuyển khoản", 13f, MUTED).apply { id = R.id.paymentTransferContent }, context.matchWrap().apply { topMargin = context.dp(10) })
                addView(context.label("Quét mã QR bằng ứng dụng ngân hàng.", 13f, MUTED).apply { id = R.id.paymentInstruction }, context.matchWrap())
                addView(context.outlinedButton("Sao chép nội dung", R.id.paymentCopyButton), context.matchWrap().apply { topMargin = context.dp(8) })
            }, context.matchWrap().apply { topMargin = context.dp(14) })
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(context.actionButton("Kiểm tra thanh toán", R.id.paymentCheckButton), context.matchWrap().apply { setMargins(context.dp(16), context.dp(8), context.dp(16), context.dp(16)) })
    }

    fun home(context: Context): View = context.column().apply {
        addView(context.row().apply {
            id = R.id.homeHeader
            setPadding(context.dp(18), context.dp(12), context.dp(18), 0)
            addView(context.column().apply {
                addView(context.label("Chào bạn!", 13f, MUTED))
                addView(context.label("Khám phá cửa hàng", 21f, INK, true).apply { id = R.id.userWelcomeText })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(FrameLayout(context).apply {
                id = R.id.searchToggleButton
                background = context.roundedColor(Color.WHITE, 16)
                isClickable = true
                isFocusable = true
                contentDescription = "Mở tìm kiếm"
                addView(ImageView(context).apply {
                    id = R.id.searchToggleIcon
                    setImageResource(R.drawable.ic_search_24)
                    setColorFilter(0xFF5E536A.toInt())
                    contentDescription = null
                }, FrameLayout.LayoutParams(context.dp(23), context.dp(23), Gravity.CENTER))
                addView(context.label("×", 26f, 0xFF5E536A.toInt()).apply {
                    id = R.id.searchToggleClose
                    gravity = Gravity.CENTER
                    visibility = View.GONE
                }, FrameLayout.LayoutParams(-1, -1))
            }, LinearLayout.LayoutParams(context.dp(48), context.dp(48)).apply { marginEnd = context.dp(8) })
            addView(FrameLayout(context).apply {
                id = R.id.cartButton
                background = context.roundedGradient(0xFF7652D0.toInt(), 0xFF5739A8.toInt(), 17)
                isClickable = true
                isFocusable = true
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.ic_shopping_bag_24)
                    setColorFilter(Color.WHITE)
                    contentDescription = "Giỏ hàng"
                }, FrameLayout.LayoutParams(context.dp(25), context.dp(25), Gravity.CENTER))
                addView(context.label("", 10f, Color.WHITE, true).apply {
                    id = R.id.cartBadgeText
                    gravity = Gravity.CENTER
                    background = context.roundedColor(0xFFE84D72.toInt(), 10)
                    visibility = View.GONE
                }, FrameLayout.LayoutParams(context.dp(20), context.dp(20), Gravity.TOP or Gravity.END))
            }, LinearLayout.LayoutParams(context.dp(48), context.dp(48)).apply { marginEnd = context.dp(8) })
            addView(FrameLayout(context).apply {
                id = R.id.notificationButton
                background = context.roundedColor(Color.WHITE, 16)
                isClickable = true
                isFocusable = true
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.ic_notifications_24)
                    setColorFilter(0xFF5E536A.toInt())
                    contentDescription = "Thông báo"
                }, FrameLayout.LayoutParams(context.dp(23), context.dp(23), Gravity.CENTER))
            }, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
        })
        addView(context.row().apply {
            id = R.id.homeSearchBar
            gravity = Gravity.CENTER_VERTICAL
            background = context.roundedColor(Color.WHITE, 14)
            visibility = View.GONE
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_search_24)
                setColorFilter(MUTED)
                contentDescription = "Tìm kiếm"
            }, LinearLayout.LayoutParams(context.dp(22), context.dp(22)).apply { marginStart = context.dp(14) })
            addView(context.input("Tìm sản phẩm", InputType.TYPE_CLASS_TEXT, R.id.searchEditText).apply {
                imeOptions = EditorInfo.IME_ACTION_SEARCH
                background = null
                minHeight = context.dp(52)
                setPadding(context.dp(10), context.dp(10), context.dp(14), context.dp(10))
            }, LinearLayout.LayoutParams(0, context.dp(52), 1f))
            addView(context.label("Xóa toàn bộ", 12f, BRAND, true).apply {
                id = R.id.searchCloseButton
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                contentDescription = "Xóa toàn bộ nội dung tìm kiếm"
                setPadding(0, 0, context.dp(12), 0)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, context.dp(52)))
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(context.dp(18), context.dp(12), context.dp(18), context.dp(12)) })
        val scroll = ScrollView(context).apply { id = R.id.homeScrollView; isFillViewport = true; isVerticalScrollBarEnabled = false }
        isFocusableInTouchMode = true
        descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        scroll.addView(context.column().apply {
            setPadding(context.dp(18), 0, context.dp(18), context.dp(24))
            addView(context.cardColumn().apply {
                id = R.id.promotionBanner
                background = context.roundedGradient(0xFF7450CF.toInt(), 0xFF482B93.toInt(), 24)
                addView(context.label("BỘ SƯU TẬP MỚI  ·  2026", 11f, 0xFFE7DCFF.toInt(), true))
                addView(context.label("Tìm món đồ yêu thích của bạn", 21f, Color.WHITE, true))
                addView(context.label("Ưu đãi và phong cách mới đang chờ bạn.", 14f, 0xFFF2ECFF.toInt()))
                addView(context.actionButton("Mua sắm ngay", R.id.shopNowButton), context.wrapWrap().apply { topMargin = context.dp(10) })
            }, context.matchWrap().apply { topMargin = context.dp(18) })
            addView(context.row().apply {
                gravity = Gravity.CENTER_VERTICAL
                listOf("🚚  Giao hàng tận nơi", "🔒  Thanh toán an toàn").forEach { benefit ->
                    addView(context.cardColumn().apply {
                        setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
                        addView(context.label(benefit, 11f, INK, true))
                    }, LinearLayout.LayoutParams(0, context.dp(56), 1f).apply { if (benefit.startsWith("🚚")) marginEnd = context.dp(8) })
                }
            }, context.matchWrap().apply { topMargin = context.dp(12) })
            addView(context.label("Tìm nhanh theo xu hướng", 14f, INK, true), context.matchWrap().apply { topMargin = context.dp(20) })
            addView(HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(context.row().apply {
                    id = R.id.quickSearchItems
                    listOf("Tai nghe", "Thời trang", "Làm đẹp", "Gia dụng").forEach { term ->
                        addView(context.outlinedButton(term, View.generateViewId()), context.wrapWrap().apply { marginEnd = context.dp(8) })
                    }
                })
            }, context.matchWrap().apply { topMargin = context.dp(6) })
            addSection(context, "Danh mục", "Xem tất cả", R.id.viewAllCategories) {
                val horizontal = HorizontalScrollView(context).apply { id = R.id.categoriesScrollView; isHorizontalScrollBarEnabled = false }
                horizontal.addView(context.row().apply { id = R.id.categoryItemsContainer })
                addView(horizontal, context.matchWrap().apply { topMargin = context.dp(10) })
            }
            addView(context.row().apply {
                addView(context.label("Thương hiệu được yêu thích", 17f, INK, true))
            }, context.matchWrap().apply { topMargin = context.dp(22) })
            addView(HorizontalScrollView(context).apply {
                id = R.id.brandsScrollView
                isHorizontalScrollBarEnabled = false
                addView(context.row().apply { id = R.id.brandItemsContainer })
            }, context.matchWrap().apply { topMargin = context.dp(10) })
            addSection(context, "Sản phẩm nổi bật", "Xem tất cả", R.id.viewAllProducts) {
                addView(context.label("Các lựa chọn đang được quan tâm", 13f, MUTED), context.matchWrap().apply { topMargin = context.dp(3) })
                addView(context.outlinedButton("♡  Lọc sản phẩm yêu thích", R.id.favoriteFilterButton), context.matchWrap().apply { topMargin = context.dp(10) })
                addView(context.label("", 14f, MUTED).apply { id = R.id.emptyProductsText; gravity = Gravity.CENTER }, context.matchWrap())
                addView(context.column().apply { id = R.id.featuredProductsGrid }, context.matchWrap())
            }
        })
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(context.bottomNavigation(R.id.navHome), context.matchWrap())
        id = R.id.main
    }

    fun search(context: Context): View = context.column().apply {
        id = R.id.main
        addView(context.row().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(12), context.dp(8), context.dp(16), context.dp(8))
            addView(context.label("‹", 32f, INK).apply {
                id = R.id.searchPageBack
                gravity = Gravity.CENTER
                contentDescription = "Quay lại"
                isClickable = true
            }, LinearLayout.LayoutParams(context.dp(44), context.dp(48)))
            addView(context.row().apply {
                gravity = Gravity.CENTER_VERTICAL
                background = context.roundedColor(Color.WHITE, 14)
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.ic_search_24)
                    setColorFilter(BRAND)
                    contentDescription = "Tìm kiếm"
                }, LinearLayout.LayoutParams(context.dp(20), context.dp(20)).apply { marginStart = context.dp(12) })
                addView(context.input("Bạn muốn tìm gì?", InputType.TYPE_CLASS_TEXT, R.id.searchPageInput).apply {
                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    background = null
                    minHeight = context.dp(50)
                    setPadding(context.dp(10), context.dp(8), context.dp(4), context.dp(8))
                }, LinearLayout.LayoutParams(0, context.dp(50), 1f))
                addView(context.label("×", 24f, MUTED).apply {
                    id = R.id.searchPageClear
                    gravity = Gravity.CENTER
                    contentDescription = "Xóa từ khóa"
                    isClickable = true
                }, LinearLayout.LayoutParams(context.dp(42), context.dp(50)))
            }, LinearLayout.LayoutParams(0, -2, 1f))
        })
        addView(context.label("Tìm kiếm sản phẩm", 22f, INK, true), context.matchWrap().apply {
            setMargins(context.dp(20), context.dp(12), context.dp(20), context.dp(4))
        })
        addView(context.label("Nhập tên sản phẩm, thương hiệu hoặc danh mục", 13f, MUTED), context.matchWrap().apply {
            setMargins(context.dp(20), 0, context.dp(20), context.dp(16))
        })
        addView(context.label("Tìm kiếm phổ biến", 15f, INK, true), context.matchWrap().apply {
            setMargins(context.dp(20), 0, context.dp(20), context.dp(8))
        })
        addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(context.row().apply {
                id = R.id.searchPageSuggestions
                listOf("Tai nghe", "Thời trang", "Làm đẹp", "Gia dụng").forEach { term ->
                    addView(context.outlinedButton(term, View.generateViewId()), context.wrapWrap().apply { marginEnd = context.dp(8) })
                }
                setPadding(context.dp(20), 0, context.dp(20), 0)
            })
        }, context.matchWrap())
        addView(context.label("", 13f, MUTED).apply {
            id = R.id.searchPageStatus
            gravity = Gravity.CENTER
            setPadding(context.dp(20), context.dp(16), context.dp(20), context.dp(8))
        }, context.matchWrap())
        val scroll = ScrollView(context).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        scroll.addView(context.column().apply {
            id = R.id.searchPageResults
            setPadding(context.dp(16), context.dp(4), context.dp(16), context.dp(24))
        })
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    fun searchResults(context: Context): View = context.column().apply {
        id = R.id.main
        addView(context.row().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(12), context.dp(8), context.dp(18), context.dp(8))
            addView(context.label("‹", 32f, INK).apply {
                id = R.id.searchResultsBack
                gravity = Gravity.CENTER
                contentDescription = "Quay lại tìm kiếm"
                isClickable = true
            }, LinearLayout.LayoutParams(context.dp(44), context.dp(48)))
            addView(context.label("Kết quả tìm kiếm", 19f, INK, true), LinearLayout.LayoutParams(0, -2, 1f))
        })
        addView(context.row().apply {
            gravity = Gravity.CENTER_VERTICAL
            background = context.roundedColor(Color.WHITE, 14)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_search_24)
                setColorFilter(BRAND)
                contentDescription = "Tìm kiếm"
            }, LinearLayout.LayoutParams(context.dp(20), context.dp(20)).apply { marginStart = context.dp(12) })
            addView(context.input("Tìm sản phẩm", InputType.TYPE_CLASS_TEXT, R.id.searchResultsQuery).apply {
                imeOptions = EditorInfo.IME_ACTION_SEARCH
                background = null
                minHeight = context.dp(50)
                setPadding(context.dp(10), context.dp(8), context.dp(4), context.dp(8))
            }, LinearLayout.LayoutParams(0, context.dp(50), 1f))
            addView(context.label("×", 24f, MUTED).apply {
                id = R.id.searchResultsClear
                gravity = Gravity.CENTER
                contentDescription = "Xóa từ khóa"
                isClickable = true
            }, LinearLayout.LayoutParams(context.dp(42), context.dp(50)))
        }, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(context.dp(18), context.dp(6), context.dp(18), context.dp(12))
        })
        addView(context.label("", 13f, MUTED).apply {
            id = R.id.searchResultsStatus
            setPadding(context.dp(20), context.dp(4), context.dp(20), context.dp(10))
        }, context.matchWrap())
        val scroll = ScrollView(context).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        scroll.addView(context.column().apply {
            id = R.id.searchResultsList
            setPadding(context.dp(16), context.dp(2), context.dp(16), context.dp(24))
        })
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun LinearLayout.addSection(context: Context, title: String, action: String, actionId: Int, content: LinearLayout.() -> Unit) {
        addView(context.row().apply {
            addView(context.label(title, 18f, INK, true), LinearLayout.LayoutParams(0, -2, 1f))
            addView(context.label(action, 13f, BRAND, true).apply { id = actionId; gravity = Gravity.CENTER_VERTICAL })
        }, context.matchWrap().apply { topMargin = context.dp(22) })
        content()
    }

    private fun LinearLayout.addHeader(context: Context, title: String, backId: Int, trailing: View? = null) {
        addView(context.row().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(12), context.dp(4), context.dp(16), context.dp(4))
            addView(context.label("‹", 32f, INK).apply { id = backId; gravity = Gravity.CENTER; contentDescription = "Quay lại" }, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
            addView(context.label(title, 18f, INK, true).apply {
                if (backId == R.id.addressFormBack) id = R.id.addressFormTitle
            }, LinearLayout.LayoutParams(0, -2, 1f))
            trailing?.let(::addView)
        }, context.matchWrap())
    }

    private fun Context.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
    private fun Context.row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun Context.scrollContent(containerId: Int? = null, content: LinearLayout.() -> Unit = {}): ScrollView =
        ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(column().apply {
                if (containerId != null) id = containerId
                setPadding(dp(18), dp(12), dp(18), dp(20))
                content()
            })
        }

    private fun Context.cardColumn(color: Int = Color.WHITE) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = roundedColor(color, 20)
    }

    private fun Context.bottomNavigation(selectedId: Int) = row().apply {
        id = R.id.bottomNavigation
        setBackgroundColor(Color.WHITE)
        elevation = dp(8).toFloat()
        setPadding(dp(12), dp(8), dp(12), dp(8))
        listOf(Triple("Trang chủ", R.id.navHome, R.drawable.ic_home_24), Triple("Danh mục", R.id.navCategories, R.drawable.ic_grid_24), Triple("Tài khoản", R.id.navAccount, R.drawable.ic_person_24)).forEach { (title, itemId, iconRes) ->
            val isSelected = itemId == selectedId
            addView(column().apply {
                id = itemId
                gravity = Gravity.CENTER
                if (isSelected) setBackgroundResource(R.drawable.bg_nav_selected)
                setPadding(dp(12), dp(5), dp(12), dp(5))
                addView(ImageView(context).apply {
                    id = when (itemId) { R.id.navHome -> R.id.navHomeIcon; R.id.navCategories -> R.id.navCategoriesIcon; else -> R.id.navAccountIcon }
                    setImageResource(iconRes)
                    setColorFilter(if (isSelected) BRAND else MUTED)
                    contentDescription = title
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
                })
                addView(label(title, 11f, if (isSelected) BRAND else MUTED, isSelected).apply {
                    id = when (itemId) { R.id.navHome -> R.id.navHomeLabel; R.id.navCategories -> R.id.navCategoriesLabel; else -> R.id.navAccountLabel }
                })
            }, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
    }

    private fun Context.roundedGradient(start: Int, end: Int, radiusDp: Int) = android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
        intArrayOf(start, end)
    ).apply { cornerRadius = dp(radiusDp).toFloat() }

    private fun Context.label(text: String, size: Float = 14f, color: Int = INK, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun Context.input(hintText: String, type: Int, viewId: Int) = TextInputEditText(this).apply {
        id = viewId; hint = hintText; inputType = type; textSize = 15f; minHeight = dp(52); setPadding(dp(14), dp(10), dp(14), dp(10))
        background = roundedColor(Color.WHITE, 12)
    }

    private fun Context.actionButton(title: String, viewId: Int) = MaterialButton(this).apply {
        id = viewId; text = title; isAllCaps = false; minHeight = dp(50); minimumHeight = dp(50); cornerRadius = dp(14); insetTop = 0; insetBottom = 0
    }

    private fun Context.outlinedButton(title: String, viewId: Int) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        id = viewId; text = title; isAllCaps = false; minHeight = dp(48); minimumHeight = dp(48); cornerRadius = dp(14); insetTop = 0; insetBottom = 0
    }

    private fun Context.matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun Context.wrapWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}

/** Centers content and caps it on tablets/foldables while letting backgrounds fill the window. */
private class AdaptiveScreenFrame(context: Context, private val content: View) : FrameLayout(context) {
    private val maxContentWidth = context.dp(720)

    init {
        setBackgroundColor(0xFFF7F6FA.toInt())
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val contentWidth = minOf(availableWidth, maxContentWidth)
        val contentHeight = MeasureSpec.getSize(heightMeasureSpec)
        content.measure(
            MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY)
        )
        setMeasuredDimension(resolveSize(availableWidth, widthMeasureSpec), resolveSize(contentHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val childLeft = (right - left - content.measuredWidth) / 2
        content.layout(childLeft, 0, childLeft + content.measuredWidth, content.measuredHeight)
    }
}
