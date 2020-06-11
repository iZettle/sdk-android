package com.izettle.payments.android.kotlin_sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.izettle.payments.android.architecturecomponents.toLiveData
import com.izettle.payments.android.kotlin_sample.MainActivity
import com.izettle.payments.android.payment.TransactionReference
import com.izettle.payments.android.payment.refunds.CardPaymentPayload
import com.izettle.payments.android.payment.refunds.RefundsManager
import com.izettle.payments.android.payment.refunds.RetrieveCardPaymentFailureReason
import com.izettle.payments.android.sdk.IZettleSDK.Instance.refundsManager
import com.izettle.payments.android.sdk.IZettleSDK.Instance.user
import com.izettle.payments.android.sdk.User
import com.izettle.payments.android.sdk.User.AuthState.LoggedIn
import com.izettle.payments.android.ui.payment.CardPaymentActivity
import com.izettle.payments.android.ui.payment.CardPaymentResult
import com.izettle.payments.android.ui.readers.CardReadersActivity
import com.izettle.payments.android.ui.refunds.RefundResult
import com.izettle.payments.android.ui.refunds.RefundsActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var chargeButton: Button
    private lateinit var refundButton: Button
    private lateinit var settingsButton: Button
    private lateinit var amountEditText: EditText
    private lateinit var tippingCheckBox: CheckBox
    private lateinit var lastPaymentTraceId: MutableLiveData<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loginButton = findViewById(R.id.login_btn)
        logoutButton = findViewById(R.id.logout_btn)
        chargeButton = findViewById(R.id.charge_btn)
        refundButton = findViewById(R.id.refund_btn)
        settingsButton = findViewById(R.id.settings_btn)
        amountEditText = findViewById(R.id.amount_input)
        tippingCheckBox = findViewById(R.id.tipping_check_box)
        lastPaymentTraceId = MutableLiveData(null)

        user.state.toLiveData().observe(this, Observer { authState: User.AuthState? ->
            onUserAuthStateChanged(authState is LoggedIn)
        })

        lastPaymentTraceId.observe(this, Observer { value: String? ->
            refundButton.isEnabled = value != null
        })

        loginButton.setOnClickListener { onLoginClicked() }
        logoutButton.setOnClickListener { onLogoutClicked() }
        chargeButton.setOnClickListener { onChargeClicked() }
        refundButton.setOnClickListener { onRefundClicked() }
        settingsButton.setOnClickListener { onSettingsClicked() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PAYMENT && data != null) {
            val result: CardPaymentResult? = data.getParcelableExtra(CardPaymentActivity.RESULT_EXTRA_PAYLOAD)
            if (result is CardPaymentResult.Completed) {
                Toast.makeText(this, "Payment completed", Toast.LENGTH_SHORT).show()
            } else if (result is CardPaymentResult.Canceled) {
                Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show()
            } else if (result is CardPaymentResult.Failed) {
                Toast.makeText(this, "Payment failed ", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == REQUEST_CODE_REFUND && data != null) {
            val result: RefundResult? = data.getParcelableExtra(RefundsActivity.RESULT_EXTRA_PAYLOAD)
            if (result is RefundResult.Completed) {
                Toast.makeText(this, "Refund completed", Toast.LENGTH_SHORT).show()
            } else if (result is RefundResult.Canceled) {
                Toast.makeText(this, "Refund canceled", Toast.LENGTH_SHORT).show()
            } else if (result is RefundResult.Failed) {
                Toast.makeText(this, "Refund failed ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onUserAuthStateChanged(isLoggedIn: Boolean) {
        loginButton.isEnabled = !isLoggedIn
        logoutButton.isEnabled = isLoggedIn
        chargeButton.isEnabled = isLoggedIn
        refundButton.isEnabled = isLoggedIn && lastPaymentTraceId.value != null
        settingsButton.isEnabled = isLoggedIn
        amountEditText.isEnabled = isLoggedIn
        tippingCheckBox.isEnabled = isLoggedIn
    }

    private fun onLoginClicked() {
        val toolbarColor = ResourcesCompat.getColor(resources, R.color.white, theme)
        user.login(this, toolbarColor)
    }

    private fun onLogoutClicked() {
        user.logout()
    }

    private fun onChargeClicked() {
        val amountEditTextContent = amountEditText.text.toString()
        if (amountEditTextContent == "") {
            return
        }
        val internalTraceId = UUID.randomUUID().toString()
        val amount = amountEditTextContent.toLong()
        val enableTipping = tippingCheckBox.isChecked
        val reference = TransactionReference.Builder(internalTraceId)
                .put("PAYMENT_EXTRA_INFO", "Started from home screen")
                .build()
        val intent = CardPaymentActivity.IntentBuilder(this)
                .amount(amount)
                .reference(reference)
                .enableTipping(enableTipping)
                .build()
        startActivityForResult(intent, REQUEST_CODE_PAYMENT)
        lastPaymentTraceId.value = internalTraceId
    }

    private fun onSettingsClicked() {
        startActivity(CardReadersActivity.newIntent(this))
    }

    private fun onRefundClicked() {
        val internalTraceId = lastPaymentTraceId.value ?: return
        refundsManager.retrieveCardPayment(internalTraceId, RefundCallback())
    }

    private inner class RefundCallback : RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {

        override fun onFailure(reason: RetrieveCardPaymentFailureReason) {
            Toast.makeText(this@MainActivity, "Refund failed", Toast.LENGTH_SHORT).show()
        }

        override fun onSuccess(payload: CardPaymentPayload) {
            val reference = TransactionReference.Builder(payload.referenceId)
                    .put("REFUND_EXTRA_INFO", "Started from home screen")
                    .build()
            val intent = RefundsActivity.IntentBuilder(this@MainActivity)
                    .cardPayment(payload)
                    .receiptNumber("#123456")
                    .taxAmount(payload.amount / 10)
                    .reference(reference)
                    .build()
            startActivityForResult(intent, REQUEST_CODE_REFUND)
        }
    }

    companion object {
        private const val REQUEST_CODE_PAYMENT = 1001
        private const val REQUEST_CODE_REFUND = 1002
    }
}