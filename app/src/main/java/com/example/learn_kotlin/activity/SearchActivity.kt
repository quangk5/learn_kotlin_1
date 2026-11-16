package com.example.learn_kotlin.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.learn_kotlin.R
import com.example.learn_kotlin.applySystemBarInsets

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ScreenLayouts.adaptive(this, ScreenLayouts.search(this)))
        applySystemBarInsets(findViewById(android.R.id.content))

        val input = findViewById<EditText>(R.id.searchPageInput)
        val initialQuery = intent.getStringExtra(EXTRA_QUERY).orEmpty()
        input.setText(initialQuery)
        findViewById<View>(R.id.searchPageBack).setOnClickListener { finish() }
        findViewById<View>(R.id.searchPageClear).setOnClickListener {
            input.text?.clear()
            input.requestFocus()
            showKeyboard(input)
        }
        input.setOnEditorActionListener { field, actionId, event ->
            val submit = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_UP)
            if (submit) {
                search(field.text?.toString().orEmpty())
                hideKeyboard(input)
                true
            } else false
        }
        val suggestions = findViewById<LinearLayout>(R.id.searchPageSuggestions)
        for (index in 0 until suggestions.childCount) {
            suggestions.getChildAt(index).setOnClickListener { view ->
                val term = (view as? com.google.android.material.button.MaterialButton)?.text?.toString().orEmpty()
                input.setText(term)
                search(term)
                hideKeyboard(input)
            }
        }

        if (initialQuery.isNotBlank()) {
            search(initialQuery)
        } else {
            input.requestFocus()
            input.post { showKeyboard(input) }
        }
    }

    private fun search(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isBlank()) return
        hideKeyboard(findViewById(R.id.searchPageInput))
        startActivity(Intent(this, SearchResultsActivity::class.java).putExtra(EXTRA_QUERY, query))
        if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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

    companion object {
        const val EXTRA_QUERY = "search_query"
    }
}
