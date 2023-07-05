package com.zettle.payments.android.kotlin_example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.zettle.sdk.feature.qrc.QrcAction
import com.zettle.sdk.feature.qrc.model.QrcPaymentType
import com.zettle.sdk.feature.qrc.paypal.PayPalQrcAction
import com.zettle.sdk.feature.qrc.ui.payment.QrcPaymentPayload
import com.zettle.sdk.feature.qrc.ui.refund.QrcRefund
import com.zettle.sdk.feature.qrc.venmo.VenmoQrcAction
import com.zettle.sdk.features.charge
import com.zettle.sdk.features.refund
import com.zettle.sdk.features.retrieve
import com.zettle.sdk.features.show
import com.zettle.sdk.ui.ZettleResult
import com.zettle.sdk.ui.zettleResult
import java.util.*
import kotlin.math.abs

class PayPalQrcActivity : AppCompatActivity() {

    private lateinit var chargeButton: Button
    private lateinit var settingsButton: Button
    private lateinit var amountEditText: EditText
    private lateinit var venmoCheckBox: CheckBox
    private lateinit var refundButton: Button
    private lateinit var retrieveButton: Button
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
        retrieveButton = findViewById(R.id.retrieve_btn)
        refundAmountEditText = findViewById(R.id.refund_amount_input)
        lastPaymentTraceId = MutableLiveData()

        chargeButton.setOnClickListener { onChargeClicked() }
        settingsButton.setOnClickListener { onSettingsClicked() }
        refundButton.setOnClickListener { onRefundLastPayment() }
        retrieveButton.setOnClickListener { onRetrieveLastPayment() }
    }

    private val paymentLauncher = registerForActivityResult(StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        when (val result = r.data?.zettleResult()) {
            is ZettleResult.Completed<*> -> {
                val payment: QrcPaymentPayload = QrcAction.fromPaymentResult(result)
                showResultSheet(payment.toPaymentResultData())
                lastPaymentTraceId.value = payment.reference
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
                Log.d("#####", "Refund Completed ${result.payload}")
                val refund: QrcRefund = QrcAction.fromRefundResult(result)
                showResultSheet(refund.toRefundResultData())
            }
            is ZettleResult.Failed -> showSnackBar("Refund failed ${result.reason}")
            is ZettleResult.Cancelled -> showSnackBar("Refund canceled")
            null -> showSnackBar("Problem... null")
        }
    }

    private fun QrcPaymentPayload.toPaymentResultData() = PaymentResultData(
        title = "QRC Payment",
        amount = formatPaymentAmount(amount),
        reference = reference,
        resultList = toResultListItems()
    )

    private fun QrcRefund.toRefundResultData() = PaymentResultData(
        title = "QRC Refund",
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
        val intent: Intent = if (venmoCheckBox.isChecked) {
            VenmoQrcAction.Payment(amount = amount, reference = uuid).charge(context = this)
        } else {
            PayPalQrcAction.Payment(amount = amount, reference = uuid).charge(context = this)
        }
        paymentLauncher.launch(intent)
    }

    private fun onRefundLastPayment() {
        val appearance = if (venmoCheckBox.isChecked) {
            QrcPaymentType.Venmo
        } else {
            QrcPaymentType.PayPal
        }

        val amount = refundAmountEditText.text.toLong()
        val internalTraceId = lastPaymentTraceId.value
        val isDevMode = (application as MainApplication).isDevMode

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken")
            return
        }

        val refundIntent = when (appearance) {
            is QrcPaymentType.PayPal -> PayPalQrcAction.Refund(
                amount = amount ?: 0L,
                paymentReference = internalTraceId ?: "",
                refundReference = UUID.randomUUID().toString()
            )
            is QrcPaymentType.Venmo -> VenmoQrcAction.Refund(
                amount = amount ?: 0L,
                paymentReference = internalTraceId ?: "",
                refundReference = UUID.randomUUID().toString()
            )
            else -> {
                showSnackBar("No QrcPayment type provided")
                return
            }
        }

        refundLauncher.launch(refundIntent.refund(this))
    }

    private fun onRetrieveLastPayment() {
        val appearance = when(venmoCheckBox.isChecked) {
            true -> QrcPaymentType.Venmo
            false -> QrcPaymentType.PayPal
        }

        val internalTraceId = lastPaymentTraceId.value
        val isDevMode = (application as MainApplication).isDevMode

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken")
            return
        }

        val action = when (appearance) {
            is QrcPaymentType.PayPal -> PayPalQrcAction.Transaction(internalTraceId ?: "")
            is QrcPaymentType.Venmo -> VenmoQrcAction.Transaction(internalTraceId ?: "")
            else -> {
                showSnackBar("No QrcPayment type provided")
                return
            }
        }
        action.retrieve {
            when (val result = it) {
                is ZettleResult.Completed<*> -> {
                    val payment: QrcPaymentPayload = QrcAction.fromRetrieveTransactionResult(result)
                    showResultSheet(payment.toPaymentResultData())
                }
                is ZettleResult.Failed -> showSnackBar("Retrieve payment failed ${result.reason}")
                is ZettleResult.Cancelled -> showSnackBar("Retrieve payment canceled")
            }
        }
    }

    private fun onSettingsClicked() {
        val intent = if (venmoCheckBox.isChecked) {
            VenmoQrcAction.Activation
        } else {
            PayPalQrcAction.Activation
        }.show(this)

        startActivity(intent)
    }
}