package com.zettle.payments.android.kotlin_example

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zettle.sdk.ZettleSDK
import com.zettle.sdk.core.auth.User

class MainActivity : AppCompatActivity() {

    private lateinit var loginStateText: TextView
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var openCardReaderButton: Button
    private lateinit var openPayPalQrcButton: Button
    private lateinit var openManualCardEntryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginStateText = findViewById(R.id.login_state)
        loginButton = findViewById(R.id.login_btn)
        logoutButton = findViewById(R.id.logout_btn)
        openCardReaderButton = findViewById(R.id.open_card_reader_btn)
        openPayPalQrcButton = findViewById(R.id.open_paypal_btn)
        openManualCardEntryButton = findViewById(R.id.open_mce_btn)

        ZettleSDK.instance?.authState?.observe(this) { state ->
            onAuthStateChanged(state is User.AuthState.LoggedIn)
        }

        loginButton.setOnClickListener {
            ZettleSDK.instance?.login(this)//, Color.WHITE)
        }

        logoutButton.setOnClickListener {
            ZettleSDK.instance?.logout()
        }

        openCardReaderButton.setOnClickListener {
            val intent = Intent(this, CardReaderActivity::class.java)
            startActivity(intent)
        }

        openPayPalQrcButton.setOnClickListener {
            val intent = Intent(this, PayPalQrcActivity::class.java)
            startActivity(intent)
        }

        openManualCardEntryButton.setOnClickListener {
            val intent = Intent(this, ManualCardEntryActivity::class.java)
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