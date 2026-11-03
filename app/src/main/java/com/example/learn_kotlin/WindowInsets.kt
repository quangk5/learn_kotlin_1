package com.example.learn_kotlin

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Chừa chỗ cho thanh trạng thái, thanh điều hướng, tai thỏ/camera cutout và bàn phím,
 * để nội dung không bị che trên mọi thiết bị (điện thoại, tablet, máy gập, nhiều cửa sổ).
 *
 * Gọi enableEdgeToEdge() TRƯỚC setContentView(), rồi gọi hàm này SAU setContentView().
 */
fun applySystemBarInsets(root: View) {
    val initialLeft = root.paddingLeft
    val initialTop = root.paddingTop
    val initialRight = root.paddingRight
    val initialBottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime()
        )
        view.setPadding(
            initialLeft + bars.left,
            initialTop + bars.top,
            initialRight + bars.right,
            initialBottom + bars.bottom
        )
        insets
    }
}
