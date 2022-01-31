package com.izettle.payments.android.kotlin_sample

import android.app.Activity
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
import java.util.*

class CardReaderActivity : AppCompatActivity() {

    private lateinit var chargeButton: Button
    private lateinit var refundButton: Button
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
        loginCheckBox = findViewById(R.id.login_check_box)
        installmentsCheckBox = findViewById(R.id.installments_check_box)
        lastPaymentTraceId = MutableLiveData()

        lastPaymentTraceId.observe(this, Observer { value: String? ->
            refundButton.isEnabled = value != null
        })

        chargeButton.setOnClickListener { onChargeClicked() }
        refundButton.setOnClickListener { onRefundClicked() }
        settingsButton.setOnClickListener { onSettingsClicked() }
    }

    private val paymentLauncher =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                activityResult.data?.let {
                    when (val result: CardPaymentResult? =
                        it.getParcelableExtra(CardPaymentActivity.RESULT_EXTRA_PAYLOAD)) {
                        is CardPaymentResult.Completed -> {
                            lastPaymentTraceId.value = result.payload.reference?.id
                            showToast("Payment completed")
                        }
                        is CardPaymentResult.Canceled -> showToast("Payment canceled")
                        is CardPaymentResult.Failed -> showToast("Payment failed ")
                    }
                }
            }
        }

    private val refundLauncher =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                activityResult.data?.let {
                    when (val result: RefundResult? =
                        it.getParcelableExtra(RefundsActivity.RESULT_EXTRA_PAYLOAD)) {
                        is RefundResult.Completed -> showToast("Refund completed")
                        is RefundResult.Canceled -> showToast("Refund canceled")
                        is RefundResult.Failed -> showToast("Refund failed\n(${result.reason})")
                    }
                }
            }
        }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
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

    private fun onSettingsClicked() {
        startActivity(CardReadersActivity.newIntent(this))
    }

    private fun onRefundClicked() {
        val internalTraceId = lastPaymentTraceId.value ?: return
        refundsManager.retrieveCardPayment(internalTraceId, RefundCallback())
    }

    private inner class RefundCallback :
        RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {

        override fun onFailure(reason: RetrieveCardPaymentFailureReason) {
            Toast.makeText(this@CardReaderActivity, "Refund failed", Toast.LENGTH_SHORT).show()
        }

        override fun onSuccess(payload: CardPaymentPayload) {
            val reference = TransactionReference.Builder(payload.referenceId)
                .put("REFUND_EXTRA_INFO", "Started from home screen")
                .build()
            val intent = RefundsActivity.IntentBuilder(this@CardReaderActivity)
                .cardPayment(payload)
                .receiptNumber("#123456")
                .taxAmount(payload.amount / 10)
                .reference(reference)
                .build()

            refundLauncher.launch(intent)
        }
    }
}