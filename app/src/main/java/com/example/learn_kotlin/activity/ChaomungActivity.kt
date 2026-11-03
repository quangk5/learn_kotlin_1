package com.example.learn_kotlin.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.learn_kotlin.R
import com.example.learn_kotlin.applySystemBarInsets

class ChaomungActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.chaomung)

        applySystemBarInsets(findViewById(R.id.main))

        if (!TokenStore(this).accessToken().isNullOrBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        findViewById<Button>(R.id.login).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.signup).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
