package com.example.learn_kotlin.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.learn_kotlin.R
import com.example.learn_kotlin.api.ApiClient
import com.example.learn_kotlin.api.PaymentQrResponse
import com.example.learn_kotlin.api.toUserMessage
import com.example.learn_kotlin.applySystemBarInsets
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ORDER_ID = "sepay_order_id"
        private const val POLL_INTERVAL_MS = 5_000L
        private const val MAX_POLL_ATTEMPTS = 120
    }

    private val paymentApi by lazy { ApiClient.paymentApi(this) }
    private var orderId: String? = null
    private var pollingJob: Job? = null
    private var hasPaid = false
    private var paymentInfoLoaded = false
    private var checkingStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ScreenLayouts.adaptive(this, ScreenLayouts.payment(this)))
        applySystemBarInsets(findViewById(android.R.id.content))

        orderId = intent.getStringExtra(EXTRA_ORDER_ID)
        if (orderId.isNullOrBlank()) {
            Toast.makeText(this, "Không tìm thấy đơn hàng cần thanh toán.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<View>(R.id.paymentBack).setOnClickListener { finish() }
        findViewById<View>(R.id.paymentCopyButton).setOnClickListener { copyTransferContent() }
        findViewById<MaterialButton>(R.id.paymentCheckButton).setOnClickListener {
            if (hasPaid) {
                startActivity(Intent(this, OrdersActivity::class.java))
                finish()
            } else {
                lifecycleScope.launch { checkPaymentStatus(showPendingMessage = true) }
            }
        }
        loadPaymentInformation()
    }

    override fun onStart() {
        super.onStart()
        if (paymentInfoLoaded && orderId != null && !hasPaid && pollingJob == null) {
            startStatusPolling()
        }
    }

    override fun onStop() {
        pollingJob?.cancel()
        pollingJob = null
        super.onStop()
    }

    private fun loadPaymentInformation() {
        setStatus("Đang lấy thông tin thanh toán…", isPaid = false)
        lifecycleScope.launch {
            try {
                val response = paymentApi.getSepayQrInfo(orderId!!)
                if (!response.isSuccessful) throw IllegalStateException(response.message ?: "Không lấy được thông tin QR.")
                val info = response.data ?: throw IllegalStateException("Máy chủ chưa trả thông tin thanh toán.")
                renderPaymentInformation(info)
                paymentInfoLoaded = true
                checkPaymentStatus(showPendingMessage = false)
                if (!hasPaid && pollingJob == null) startStatusPolling()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("PAYMENT_API", "Load SePay QR failed", error)
                setStatus(error.toUserMessage(), isPaid = false)
                findViewById<MaterialButton>(R.id.paymentCheckButton).isEnabled = true
            }
        }
    }

    private fun renderPaymentInformation(info: PaymentQrResponse) {
        findViewById<TextView>(R.id.paymentOrderCode).text = info.orderCode ?: "Đơn hàng của bạn"
        findViewById<TextView>(R.id.paymentAmount).text = formatPrice(info.amount ?: 0.0)
        findViewById<TextView>(R.id.paymentBank).text = info.bankName ?: "Chưa có thông tin ngân hàng"
        findViewById<TextView>(R.id.paymentAccount).text = info.accountNumber ?: "—"
        findViewById<TextView>(R.id.paymentAccountHolder).text = info.accountHolder ?: "—"
        findViewById<TextView>(R.id.paymentTransferContent).text = info.transferContent ?: info.orderCode ?: "—"
        findViewById<TextView>(R.id.paymentInstruction).text = info.paymentInstruction
            ?: "Quét mã QR bằng ứng dụng ngân hàng và giữ nguyên nội dung chuyển khoản."
        findViewById<TextView>(R.id.paymentQrPlaceholder).text = "Đang tải mã QR…"
        val imageUrl = info.qrImageUrl
        if (imageUrl.isNullOrBlank()) {
            findViewById<TextView>(R.id.paymentQrPlaceholder).text = "Máy chủ chưa cung cấp mã QR"
        } else {
            loadQrImage(imageUrl)
        }
    }

    private fun loadQrImage(imageUrl: String) {
        lifecycleScope.launch {
            val bitmap = try {
                withContext(Dispatchers.IO) {
                    URL(imageUrl).openConnection().apply {
                        connectTimeout = 15_000
                        readTimeout = 20_000
                    }.getInputStream().use { stream -> BitmapFactory.decodeStream(stream) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("PAYMENT_IMAGE", "Could not load QR image", error)
                null
            }

            val image = findViewById<ImageView>(R.id.paymentQrImage)
            if (bitmap != null) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                findViewById<TextView>(R.id.paymentQrPlaceholder).visibility = View.GONE
            } else {
                findViewById<TextView>(R.id.paymentQrPlaceholder).text = "Không tải được QR. Bạn vẫn có thể chuyển khoản theo thông tin bên dưới."
            }
        }
    }

    private fun copyTransferContent() {
        val content = findViewById<TextView>(R.id.paymentTransferContent).text.toString()
        if (content.isBlank() || content == "—") {
            Toast.makeText(this, "Chưa có nội dung chuyển khoản.", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nội dung chuyển khoản", content))
        Toast.makeText(this, "Đã sao chép nội dung chuyển khoản.", Toast.LENGTH_SHORT).show()
    }

    private fun startStatusPolling() {
        if (pollingJob?.isActive == true || hasPaid) return
        pollingJob = lifecycleScope.launch {
            repeat(MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                if (hasPaid) return@launch
                checkPaymentStatus(showPendingMessage = false)
            }
        }
    }

    private suspend fun checkPaymentStatus(showPendingMessage: Boolean) {
        if (checkingStatus || orderId.isNullOrBlank()) return
        checkingStatus = true
        val button = findViewById<MaterialButton>(R.id.paymentCheckButton)
        if (showPendingMessage) button.isEnabled = false
        try {
            val response = paymentApi.getPaymentStatus(orderId!!)
            if (!response.isSuccessful) throw IllegalStateException(response.message ?: "Chưa kiểm tra được trạng thái thanh toán.")
            val paymentStatus = response.data?.paymentStatus?.uppercase(Locale.ROOT)
            when (paymentStatus) {
                "PAID" -> {
                    hasPaid = true
                    pollingJob?.cancel()
                    setStatus("Đã nhận thanh toán thành công. Đơn hàng đang được xử lý.", isPaid = true)
                    button.text = "Xem đơn hàng"
                    button.isEnabled = true
                    if (showPendingMessage) Toast.makeText(this@PaymentActivity, "Thanh toán thành công!", Toast.LENGTH_LONG).show()
                }
                "FAILED", "REFUNDED" -> {
                    setStatus("Trạng thái thanh toán: $paymentStatus", isPaid = false)
                    if (showPendingMessage) Toast.makeText(this@PaymentActivity, "Thanh toán chưa hoàn tất: $paymentStatus", Toast.LENGTH_LONG).show()
                }
                else -> {
                    setStatus("Đang chờ ngân hàng xác nhận thanh toán…", isPaid = false)
                    if (showPendingMessage) Toast.makeText(this@PaymentActivity, "Chưa thấy giao dịch. Nếu bạn vừa chuyển, hãy đợi một chút rồi kiểm tra lại.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("PAYMENT_API", "Check payment status failed", error)
            if (showPendingMessage) Toast.makeText(this@PaymentActivity, error.toUserMessage(), Toast.LENGTH_LONG).show()
        } finally {
            checkingStatus = false
            if (!hasPaid) button.isEnabled = true
        }
    }

    private fun setStatus(message: String, isPaid: Boolean) {
        findViewById<TextView>(R.id.paymentStatus).apply {
            text = message
            setTextColor(if (isPaid) 0xFF16804A.toInt() else 0xFF645C70.toInt())
            setBackgroundColor(if (isPaid) 0xFFE7F5EC.toInt() else 0xFFF0ECF8.toInt())
        }
    }

    private fun formatPrice(amount: Double): String =
        NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
}
