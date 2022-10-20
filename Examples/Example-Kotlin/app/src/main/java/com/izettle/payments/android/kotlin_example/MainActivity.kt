package com.izettle.payments.android.kotlin_example

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.izettle.android.commons.ext.state.toLiveData
import com.izettle.payments.android.sdk.IZettleSDK
import com.izettle.payments.android.sdk.User.AuthState.LoggedIn

class MainActivity : AppCompatActivity() {

    private lateinit var loginStateText: TextView
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var openCardReaderButton: Button
    private lateinit var openPayPalQrcButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginStateText = findViewById(R.id.login_state)
        loginButton = findViewById(R.id.login_btn)
        logoutButton = findViewById(R.id.logout_btn)
        openCardReaderButton = findViewById(R.id.open_card_reader_btn)
        openPayPalQrcButton = findViewById(R.id.open_paypal_btn)

        IZettleSDK.user.state.toLiveData().observe(this) { state ->
            onAuthStateChanged(state is LoggedIn)
        }

        loginButton.setOnClickListener {
            IZettleSDK.user.login(this, Color.WHITE)
        }

        logoutButton.setOnClickListener {
            IZettleSDK.user.logout()
        }

        openCardReaderButton.setOnClickListener {
            val intent = Intent(this, CardReaderActivity::class.java)
            startActivity(intent)
        }

        openPayPalQrcButton.setOnClickListener {
            val intent = Intent(this, PayPalQrcActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onAuthStateChanged(isLoggedIn: Boolean) {
        loginStateText.text = "State: ${if (isLoggedIn) "Authenticated" else "Unauthenticated"}"
        loginButton.isEnabled = !isLoggedIn
        logoutButton.isEnabled = isLoggedIn
    }
}

internal fun Editable.toLong(): Long? = try {
    val content = this.toString()
    content.toLong()
} catch (e: Exception) {
    null
}