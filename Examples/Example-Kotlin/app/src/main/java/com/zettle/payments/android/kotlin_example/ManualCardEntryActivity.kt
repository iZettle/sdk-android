package com.zettle.payments.android.kotlin_example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.zettle.sdk.ZettleSDK
import com.zettle.sdk.feature.manualcardentry.ui.ManualCardEntryAction
import com.zettle.sdk.feature.manualcardentry.ui.payments.models.ManualCardEntryPaymentPayload
import com.zettle.sdk.feature.manualcardentry.ui.refunds.models.ManualCardEntryRefundPayload
import com.zettle.sdk.features.charge
import com.zettle.sdk.features.refund
import com.zettle.sdk.features.retrieve
import com.zettle.sdk.features.show
import com.zettle.sdk.ui.ZettleResult
import com.zettle.sdk.ui.zettleResult
import java.lang.Math.abs
import java.util.UUID

class ManualCardEntryActivity : AppCompatActivity() {

    private lateinit var chargeButton: Button
    private lateinit var retrieveButton: Button
    private lateinit var settingsButton: Button
    private lateinit var refundButton: Button
    private lateinit var refundAmountEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var lastPaymentTraceId: MutableLiveData<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ZettleSDK.isInitialized) {
            finish()
        }

        setContentView(R.layout.activity_manual_card_entry)
        chargeButton = findViewById(R.id.charge_btn)
        retrieveButton = findViewById(R.id.retrieve_btn)
        settingsButton = findViewById(R.id.settings_btn)
        refundButton = findViewById(R.id.refund_btn)
        refundAmountEditText = findViewById(R.id.refund_amount_input)
        amountEditText = findViewById(R.id.amount_input)
        lastPaymentTraceId = MutableLiveData()

        chargeButton.setOnClickListener { onChargeClicked() }
        retrieveButton.setOnClickListener { onRetrieveLastPayment() }
        settingsButton.setOnClickListener { onSettingsClicked() }
        refundButton.setOnClickListener { onRefundLastPayment() }
    }

    private fun onRefundLastPayment() {
        val amount = refundAmountEditText.text.toLong()
        val internalTraceId = lastPaymentTraceId.value
        val isDevMode = (application as MainApplication).isDevMode

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken")
            return
        }

        val refundIntent = ManualCardEntryAction.Refund(
            amount = amount ?: 0L,
            paymentReference = internalTraceId ?: "",
            refundReference = UUID.randomUUID().toString()
        )

        refundLauncher.launch(refundIntent.refund(this))
    }

    private val paymentLauncher = registerForActivityResult(StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        when (val result = r.data?.zettleResult()) {
            is ZettleResult.Completed<*> -> {
                val payment: ManualCardEntryPaymentPayload = ManualCardEntryAction.fromPaymentResult(result)
                showResultSheet(payment.toPaymentResultData())
                lastPaymentTraceId.value = payment.referenceId
                refundAmountEditText.text =
                    SpannableStringBuilder().append(payment.amount.toString())
            }
            is ZettleResult.Failed -> showSnackBar("Payment failed ${result.reason}")
            is ZettleResult.Cancelled -> showSnackBar("Payment canceled")
            null -> showSnackBar("Problem... null")
        }
    }

    private val refundLauncher = registerForActivityResult(StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        when (val result = r.data?.zettleResult()) {
            is ZettleResult.Completed<*> -> {
                val refund: ManualCardEntryRefundPayload = ManualCardEntryAction.fromRefundResult(result)
                showResultSheet(refund.toRefundResultData())
            }
            is ZettleResult.Failed -> showSnackBar("Refund failed ${result.reason}")
            is ZettleResult.Cancelled -> showSnackBar("Refund canceled")
            null -> showSnackBar("Problem... null")
        }
    }

    private fun ManualCardEntryPaymentPayload.toPaymentResultData() = PaymentResultData(
        title = "Manual Card Entry Payment",
        amount = formatPaymentAmount(amount),
        reference = referenceId,
        resultList = toResultListItems()
    )

    private fun ManualCardEntryRefundPayload.toRefundResultData() = PaymentResultData(
        title = "Manual Card Entry Refund",
        amount = formatPaymentAmount(-1 * abs(amount)),
        reference = reference,
        resultList = toResultListItems(),
        resultType = ResultType.REFUND
    )

    private fun showResultSheet(resultData: PaymentResultData) {
        PaymentResultBottomSheet.newInstance(resultData)
            .show(supportFragmentManager, PaymentResultBottomSheet.TAG)
    }

    private fun showSnackBar(text: String) {
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).run {
            Snackbar.make(this, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onChargeClicked() {
        val amount = amountEditText.text.toLong()
        if (amount == null) {
            showSnackBar("Invalid amount")
            return
        }

        val uuid = UUID.randomUUID().toString()
        val intent: Intent = ManualCardEntryAction.Payment(amount, uuid).charge(this)
        paymentLauncher.launch(intent)
    }

    private fun onSettingsClicked() {
        val intent = ManualCardEntryAction.Activation.show(this)
        startActivity(intent)
    }

    private fun onRetrieveLastPayment() {

        val internalTraceId = lastPaymentTraceId.value
        val isDevMode = (application as MainApplication).isDevMode

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken")
            return
        }

        val action = ManualCardEntryAction.Transaction(internalTraceId ?: "")

        action.retrieve {
            when (val result = it) {
                is ZettleResult.Completed<*> -> {
                    val payment: ManualCardEntryPaymentPayload = ManualCardEntryAction.fromRetrieveTransactionResult(result)
                    showResultSheet(payment.toPaymentResultData())
                }
                is ZettleResult.Failed -> showSnackBar("Retrieve payment failed ${result.reason}")
                is ZettleResult.Cancelled -> showSnackBar("Retrieve payment canceled")
            }
        }
    }
}