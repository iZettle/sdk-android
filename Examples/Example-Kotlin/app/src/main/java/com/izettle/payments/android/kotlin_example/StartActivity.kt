package com.izettle.payments.android.kotlin_example

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class StartActivity : AppCompatActivity() {

    private lateinit var devMode: SwitchCompat
    private lateinit var startButton: Button

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        if((application as MainApplication).started) {
            progress()
            return
        }

        startButton = findViewById(R.id.start_btn)
        devMode = findViewById(R.id.dev_mode_switch)

        startButton.setOnClickListener {
            (application as MainApplication).initIZettleSDK(devMode = devMode.isChecked)
            progress()
        }
    }

    private fun progress() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}