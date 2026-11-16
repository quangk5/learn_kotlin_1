package com.example.learn_kotlin.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

internal fun Context.formatVnd(value: Double?): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(value ?: 0.0)

internal fun Context.bodyText(textValue: CharSequence, sizeSp: Float = 14f, color: Int = Color.rgb(48, 45, 54), bold: Boolean = false): TextView =
    TextView(this).apply {
        text = textValue
        textSize = sizeSp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

internal fun Context.surfaceCard(paddingDp: Int = 16): Pair<MaterialCardView, LinearLayout> {
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(paddingDp), dp(paddingDp), dp(paddingDp), dp(paddingDp))
    }
    val card = MaterialCardView(this).apply {
        radius = dp(16).toFloat()
        cardElevation = dp(1).toFloat()
        setCardBackgroundColor(Color.WHITE)
        strokeWidth = dp(1)
        setStrokeColor(Color.rgb(238, 236, 242))
        addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    return card to content
}

internal fun Context.roundedColor(color: Int, radiusDp: Int = 12): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

internal fun LinearLayout.addGap(context: Context, heightDp: Int) {
    addView(android.view.View(context), LinearLayout.LayoutParams(1, context.dp(heightDp)))
}

internal fun Context.violet(): Int = Color.rgb(104, 70, 184)

internal fun Context.screenPadding(): Int = dp(16)

internal fun LinearLayout.addSectionTitle(context: Context, title: String) {
    addView(context.bodyText(title, 17f, Color.rgb(36, 33, 43), true))
}

internal fun AppCompatActivity.openRootTab(destination: Class<out AppCompatActivity>) {
    if (javaClass == destination) return
    startActivity(Intent(this, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}

internal fun AppCompatActivity.bindBottomNavigation(
    onHome: () -> Unit,
    onCategories: () -> Unit,
    onAccount: () -> Unit
) {
    findViewById<android.view.View>(com.example.learn_kotlin.R.id.navHome)?.setOnClickListener { onHome() }
    findViewById<android.view.View>(com.example.learn_kotlin.R.id.navCategories)?.setOnClickListener { onCategories() }
    findViewById<android.view.View>(com.example.learn_kotlin.R.id.navAccount)?.setOnClickListener { onAccount() }
}

internal class RootTabSwipeController(
    context: Context,
    private val onSwipe: (direction: Int) -> Unit
) {
    private val swipeThreshold = maxOf(
        context.dp(64).toFloat(),
        context.resources.displayMetrics.widthPixels * 0.8f
    )
    private var downX = 0f
    private var downY = 0f

    fun observe(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - downX
                val deltaY = event.y - downY
                if (kotlin.math.abs(deltaX) >= swipeThreshold && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 1.3f) {
                    onSwipe(if (deltaX < 0) 1 else -1)
                }
            }
        }
    }
}

internal fun AppCompatActivity.rootTabForSwipe(direction: Int, currentTab: Int) {
    val tabs = listOf(MainActivity::class.java, ProductCatalogActivity::class.java, ProfileActivity::class.java)
    val target = (currentTab + direction).coerceIn(0, tabs.lastIndex)
    if (target != currentTab) openRootTab(tabs[target])
}

