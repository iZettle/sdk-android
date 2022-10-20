package com.izettle.payments.android.kotlin_example

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.widget.Button
import android.widget.EditText
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.izettle.android.qrc.paypal.ui.PayPalQrcType
import com.izettle.android.qrc.paypal.ui.activation.PayPalQrcActivationActivity
import com.izettle.android.qrc.paypal.ui.payment.PayPalQrcPaymentActivity
import com.izettle.android.qrc.paypal.ui.payment.PayPalQrcPaymentResult
import com.izettle.android.qrc.paypal.ui.refund.PayPalQrcRefundActivity
import com.izettle.android.qrc.paypal.ui.refund.PayPalQrcRefundResult
import java.util.UUID

class PayPalQrcActivity : AppCompatActivity() {

    private lateinit var chargeButton: Button
    private lateinit var settingsButton: Button
    private lateinit var amountEditText: EditText
    private lateinit var venmoCheckBox: CheckBox
    private lateinit var refundButton: Button
    private lateinit var refundAmountEditText: EditText
    private lateinit var lastPaymentTraceId: MutableLiveData<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paypal_qrc)
        chargeButton = findViewById(R.id.charge_btn)
        settingsButton = findViewById(R.id.settings_btn)
        amountEditText = findViewById(R.id.amount_input)
        venmoCheckBox = findViewById(R.id.venmo_check_box)
        refundButton = findViewById(R.id.refund_btn)
        refundAmountEditText = findViewById(R.id.refund_amount_input)
        lastPaymentTraceId = MutableLiveData()

        chargeButton.setOnClickListener { onChargeClicked() }
        settingsButton.setOnClickListener { onSettingsClicked() }
        refundButton.setOnClickListener { onRefundLastPayment() }
    }

    private val paymentLauncher = registerForActivityResult(StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        when (val result = PayPalQrcPaymentActivity.fromIntent(r.data!!)) {
            is PayPalQrcPaymentResult.Completed -> {
                showToast("Payment completed")
                lastPaymentTraceId.value = result.payment.reference
                refundAmountEditText.text = SpannableStringBuilder()
                    .append(result.payment.amount.toString())
            }
            is PayPalQrcPaymentResult.Failed -> {
                showToast("Payment failed ${result.reason}")
            }
            is PayPalQrcPaymentResult.Canceled -> {
                showToast("Payment canceled")
            }
        }
    }

    private val refundLauncher = registerForActivityResult(StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        when (val result = PayPalQrcRefundActivity.fromIntent(r.data!!)) {
            is PayPalQrcRefundResult.Completed -> {
                showToast("Refund completed")
            }
            is PayPalQrcRefundResult.Failed -> {
                showToast("Payment failed ${result.reason}")
            }
            is PayPalQrcRefundResult.Canceled -> {
                showToast("Payment canceled")
            }
        }
    }


    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun onChargeClicked() {
        val amount = amountEditText.text.toLong()
        if (amount == null) {
            showToast("Invalid amount")
            return
        }

        val appearance = if (venmoCheckBox.isChecked) {
            PayPalQrcType.Venmo
        } else {
            PayPalQrcType.PayPal
        }

        val uuid = UUID.randomUUID()
        val intent = PayPalQrcPaymentActivity.IntentBuilder(this)
            .appearance(appearance)
            .amount(amount)
            .reference(uuid.toString())
            .build()

        paymentLauncher.launch(intent)
    }

    private fun onRefundLastPayment() {
        val amount = refundAmountEditText.text.toLong()
        val internalTraceId = lastPaymentTraceId.value
        if (internalTraceId == null) {
            showToast("No payment taken")
            return
        }

        val intent = PayPalQrcRefundActivity.IntentBuilder(this)
            .paymentReference(internalTraceId)
            .reference(UUID.randomUUID().toString())
            .apply {
                if (amount != null && amount != 0L) {
                    amount(amount)
                }
            }

            .build()

        refundLauncher.launch(intent)
    }

    private fun onSettingsClicked() {
        val appearance = if (venmoCheckBox.isChecked) {
            PayPalQrcType.Venmo
        } else {
            PayPalQrcType.PayPal
        }

        startActivity(
            PayPalQrcActivationActivity.IntentBuilder(this)
                .appearance(appearance)
                .build()
        )
    }
}