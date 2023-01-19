package com.izettle.payments.android.kotlin_example

import android.app.Activity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.izettle.payments.android.payment.TransactionReference
import com.izettle.payments.android.payment.refunds.CardPaymentPayload
import com.izettle.payments.android.payment.refunds.RefundsManager
import com.izettle.payments.android.payment.refunds.RetrieveCardPaymentFailureReason
import com.izettle.payments.android.sdk.IZettleSDK.Instance.refundsManager
import com.izettle.payments.android.ui.payment.CardPaymentActivity
import com.izettle.payments.android.ui.payment.CardPaymentResult
import com.izettle.payments.android.ui.readers.CardReadersActivity
import com.izettle.payments.android.ui.refunds.RefundResult
import com.izettle.payments.android.ui.refunds.RefundsActivity
import java.util.UUID

class CardReaderActivity : AppCompatActivity() {

    private lateinit var chargeButton: Button
    private lateinit var refundButton: Button
    private lateinit var refundAmountEditText: EditText
    private lateinit var settingsButton: Button
    private lateinit var amountEditText: EditText
    private lateinit var tippingCheckBox: CheckBox
    private lateinit var installmentsCheckBox: CheckBox
    private lateinit var loginCheckBox: CheckBox
    private lateinit var lastPaymentTraceId: MutableLiveData<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_reader)
        chargeButton = findViewById(R.id.charge_btn)
        refundButton = findViewById(R.id.refund_btn)
        settingsButton = findViewById(R.id.settings_btn)
        amountEditText = findViewById(R.id.amount_input)
        tippingCheckBox = findViewById(R.id.tipping_check_box)
        refundAmountEditText = findViewById(R.id.refund_amount_input)
        loginCheckBox = findViewById(R.id.login_check_box)
        installmentsCheckBox = findViewById(R.id.installments_check_box)
        lastPaymentTraceId = MutableLiveData()

        lastPaymentTraceId.observe(this) { value: String? ->
            refundButton.isEnabled = value != null
        }

        chargeButton.setOnClickListener { onChargeClicked() }
        refundButton.setOnClickListener { onRefundClicked() }
        settingsButton.setOnClickListener { onSettingsClicked() }
    }

    private val paymentLauncher =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                activityResult.data
                    ?.getParcelableExtra<CardPaymentResult>(CardPaymentActivity.RESULT_EXTRA_PAYLOAD)
                    ?.let { result ->
                        when (result) {
                        is CardPaymentResult.Completed -> {
                            lastPaymentTraceId.value = result.payload.reference?.id
                            showSnackBar("Payment completed")
                            refundAmountEditText.text = SpannableStringBuilder()
                                .append(result.payload.amount.toString())
                        }
                            is CardPaymentResult.Canceled -> showSnackBar("Payment canceled")
                            is CardPaymentResult.Failed -> showSnackBar("Payment failed Reason#${result.reason.javaClass.simpleName}")
                    }
                }
            }
        }

    private val refundLauncher =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                activityResult.data
                    ?.getParcelableExtra<RefundResult>(RefundsActivity.RESULT_EXTRA_PAYLOAD)
                    ?.let { result ->
                        when (result) {
                        is RefundResult.Completed -> showSnackBar("Refund completed")
                        is RefundResult.Canceled -> showSnackBar("Refund canceled")
                        is RefundResult.Failed -> showSnackBar("Refund failed Reason#${result.reason}")
                    }
                }
            }
        }

    private fun showSnackBar(text: String) {
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).run {
            Snackbar.make(this, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onChargeClicked() {
        val amountEditTextContent = amountEditText.text.toString()
        if (amountEditTextContent == "") {
            return
        }
        val internalTraceId = UUID.randomUUID().toString()
        val amount = amountEditTextContent.toLong()
        val enableTipping = tippingCheckBox.isChecked
        val enableInstallments = installmentsCheckBox.isChecked
        val enableLogin = loginCheckBox.isChecked
        val reference = TransactionReference.Builder(internalTraceId)
            .put("PAYMENT_EXTRA_INFO", "Started from home screen")
            .build()

        val intent = CardPaymentActivity.IntentBuilder(this)
            .amount(amount)
            .reference(reference)
            .enableInstalments(enableInstallments)
            .enableTipping(enableTipping)
            .enableLogin(enableLogin)
            .build()

        paymentLauncher.launch(intent)
    }

    private fun onRefundClicked() {
        val amount = refundAmountEditText.text.toLong() ?: 0L
        val isDevMode = (application as MainApplication).isDevMode

        if (lastPaymentTraceId.value == null && !isDevMode) {
            showSnackBar("No payment taken")
            return
        }

        val internalTraceId = lastPaymentTraceId.value ?: ""
        refundsManager.retrieveCardPayment(internalTraceId, RefundCallback(amount))
    }

    private inner class RefundCallback(val amount: Long = 0L) :
        RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {

        override fun onFailure(reason: RetrieveCardPaymentFailureReason) {
            showSnackBar("Refund failed Reason#$reason")
        }

        override fun onSuccess(payload: CardPaymentPayload) {
            refundPayment(amount, payload)
        }
    }

    private fun refundPayment(amount: Long = 0L, payload: CardPaymentPayload) {
            val reference = TransactionReference.Builder(UUID.randomUUID().toString())
                .put("REFUND_EXTRA_INFO", "Started from home screen")
                .build()
            val intent = RefundsActivity.IntentBuilder(this@CardReaderActivity)
                .cardPayment(payload)
                .receiptNumber("#123456")
                .taxAmount(amount)
                .refundAmount(amount)
                .reference(reference)
                .build()
            refundLauncher.launch(intent)
    }

    private fun onSettingsClicked() {
        startActivity(CardReadersActivity.newIntent(this))
    }

}