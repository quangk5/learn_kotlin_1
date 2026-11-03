package com.example.learn_kotlin.activity

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
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

