package com.zettle.payments.android.kotlin_example

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.zettle.sdk.feature.cardreader.payment.PayPalReaderTippingStyle
import com.zettle.sdk.feature.cardreader.payment.TippingConfiguration
import com.zettle.sdk.feature.cardreader.payment.TippingPercentageOptions
import com.zettle.sdk.feature.cardreader.payment.Transaction
import com.zettle.sdk.feature.cardreader.payment.TransactionReference
import com.zettle.sdk.feature.cardreader.payment.ZettleReaderTippingStyle
import com.zettle.sdk.feature.cardreader.payment.refunds.CardPaymentPayload
import com.zettle.sdk.feature.cardreader.payment.refunds.RefundPayload
import com.zettle.sdk.feature.cardreader.ui.CardReaderAction
import com.zettle.sdk.feature.cardreader.ui.RetrieveResult
import com.zettle.sdk.feature.cardreader.ui.payment.CardPaymentResult
import com.zettle.sdk.feature.cardreader.ui.refunds.RefundResult
import com.zettle.sdk.features.charge
import com.zettle.sdk.features.refund
import com.zettle.sdk.features.retrieve
import com.zettle.sdk.features.show
import com.zettle.sdk.ui.ZettleResult
import com.zettle.sdk.ui.zettleResult
import java.util.*
import kotlin.math.abs

class CardReaderActivity : AppCompatActivity() {

    private lateinit var chargeButton: Button
    private lateinit var refundButton: Button
    private lateinit var retrieveButton: Button
    private lateinit var refundAmountEditText: EditText
    private lateinit var settingsButton: Button
    private lateinit var amountEditText: EditText
    private lateinit var installmentsCheckBox: CheckBox
    private lateinit var lastPaymentTraceId: MutableLiveData<String?>

    private lateinit var selectTippingStyleButton: Button
    private lateinit var ztrTippingStyleLabel: TextView
    private lateinit var pprTippingStyleLabel: TextView
    private lateinit var openPprTippingSettingsButton: Button

    private var tippingConfiguration: TippingConfiguration? = TippingConfiguration(
        ZettleReaderTippingStyle.None,
        PayPalReaderTippingStyle.None
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_reader)
        chargeButton = findViewById(R.id.charge_btn)
        refundButton = findViewById(R.id.refund_btn)
        retrieveButton = findViewById(R.id.retrieve_btn)
        settingsButton = findViewById(R.id.settings_btn)
        amountEditText = findViewById(R.id.amount_input)
        refundAmountEditText = findViewById(R.id.refund_amount_input)
        installmentsCheckBox = findViewById(R.id.installments_check_box)

        selectTippingStyleButton = findViewById(R.id.select_tipping_style_btn)
        selectTippingStyleButton.setOnClickListener { selectTippingStyle() }
        ztrTippingStyleLabel = findViewById(R.id.ztr_tipping_style_label)
        pprTippingStyleLabel = findViewById(R.id.ppr_tipping_style_label)
        openPprTippingSettingsButton = findViewById(R.id.open_ppr_tipping_settings_btn)
        openPprTippingSettingsButton.setOnClickListener { onTippingSettingsClicked() }

        lastPaymentTraceId = MutableLiveData()
        lastPaymentTraceId.observe(this) { value: String? ->
            refundButton.isEnabled = value != null
            retrieveButton.isEnabled = value != null
        }

        chargeButton.setOnClickListener { onChargeClicked() }
        refundButton.setOnClickListener { onRefundClicked() }
        retrieveButton.setOnClickListener { onRetrieveClicked() }
        settingsButton.setOnClickListener { onSettingsClicked() }

        setTippingStyleTitle()
    }

    private val paymentLauncher =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode != Activity.RESULT_OK) {
                return@registerForActivityResult
            }

            when(val result = activityResult.data?.zettleResult()) {
                is ZettleResult.Completed<*> -> {
                    val payment: CardPaymentResult.Completed = CardReaderAction.fromPaymentResult(result)
                    showResultSheet(payment.payload.toPaymentResultData())
                    lastPaymentTraceId.value = payment.payload.reference?.id
                    refundAmountEditText.text = SpannableStringBuilder().append(payment.payload.amount.toString())
                }
                is ZettleResult.Cancelled -> showSnackBar("Payment canceled")
                is ZettleResult.Failed -> showSnackBar("Payment failed Reason#${result.reason.javaClass.simpleName}")
                null -> showSnackBar("Problem... null")
            }
        }

    private val refundLauncher =
        registerForActivityResult(StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode != Activity.RESULT_OK) {
                return@registerForActivityResult
            }

            when(val result = activityResult.data?.zettleResult()) {
                is ZettleResult.Completed<*> -> {
                    val refund : RefundResult.Completed = CardReaderAction.fromRefundResult(result)
                    showResultSheet(refund.payload.toPaymentResultData())
                }
                is ZettleResult.Cancelled -> showSnackBar("Refund canceled")
                is ZettleResult.Failed -> showSnackBar("Refund failed Reason#${result.reason}")
                null -> showSnackBar("Problem... null")
            }
        }

    private fun Transaction.ResultPayload.toPaymentResultData() = PaymentResultData(
        title = "Card Payment",
        amount = formatPaymentAmount(amount),
        reference = reference?.id,
        resultList = toResultListItems()
    )

    private fun RefundPayload.toPaymentResultData() = PaymentResultData(
        title = "Card Refund",
        amount = formatPaymentAmount(-1 * abs(refundedAmount)),
        reference = null,
        resultList = toResultListItems(),
        resultType = ResultType.REFUND
    )

    private fun CardPaymentPayload.toPaymentResultData() = PaymentResultData(
        title = "Card Payment",
        amount = formatPaymentAmount(amount),
        reference = referenceId,
        resultList = toResultListItems()
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
        val amountEditTextContent = amountEditText.text.toString()
        if (amountEditTextContent == "") {
            return
        }
        val internalTraceId = UUID.randomUUID().toString()
        val amount = amountEditTextContent.toLong()
        val enableInstallments = installmentsCheckBox.isChecked
        val reference = TransactionReference.Builder(internalTraceId)
            .put("PAYMENT_EXTRA_INFO", "Started from home screen")
            .build()

        val intent: Intent = CardReaderAction.Payment(
            reference = reference,
            amount = amount,
            tippingConfiguration = tippingConfiguration,
            enableInstallments = enableInstallments
        ).charge(this)

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
        val reference = TransactionReference.Builder(UUID.randomUUID().toString())
            .put("REFUND_EXTRA_INFO", "Started from home screen")
            .build()

        val intent = CardReaderAction.Refund(
            amount = amount,
            paymentReferenceId = internalTraceId,
            refundReference = reference
        ).refund(this)
        refundLauncher.launch(intent)
    }

    private fun onRetrieveClicked() {
        val isDevMode = (application as MainApplication).isDevMode
        if (lastPaymentTraceId.value == null && !isDevMode) {
            showSnackBar("No payment taken")
            return
        }
        val internalTraceId = lastPaymentTraceId.value ?: ""
        CardReaderAction.Transaction(internalTraceId).retrieve {
            when (it) {
                is ZettleResult.Completed<*> -> {
                    val transaction: RetrieveResult.Completed = CardReaderAction.fromRetrieveTransactionResult(it)
                    showResultSheet(transaction.payload.toPaymentResultData())
                }
                is ZettleResult.Cancelled -> showSnackBar("Retrieve canceled")
                is ZettleResult.Failed -> showSnackBar("Retrieve failed Reason#${it.reason}")
            }
        }
    }

    private fun onSettingsClicked() {
        startActivity(CardReaderAction.Settings.show(this))
    }

    private val selectTippingStyleLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                return@registerForActivityResult
            }

            val data = result.data ?: return@registerForActivityResult
            data.parcelableExtra<TippingConfiguration>(
                SelectTippingStyleActivity.TIPPING_CONFIGS_KEY
            )?.let {
                tippingConfiguration = it
                setTippingStyleTitle()
            }
        }

    private fun selectTippingStyle() {
        val intent = Intent(this, SelectTippingStyleActivity::class.java)
        intent.putExtra(SelectTippingStyleActivity.TIPPING_CONFIGS_KEY, tippingConfiguration)
        selectTippingStyleLauncher.launch(intent)
    }

    private fun setTippingStyleTitle() {
        tippingConfiguration?.let {
            ztrTippingStyleLabel.text = "Zettle Reader: ${it.zettleReaderTippingStyle.name}"
            val pprStyle = when (val style = it.payPalReaderTippingStyle) {
                is PayPalReaderTippingStyle.PredefinedPercentage -> style.javaClass.simpleName + style.options.displayText()
                else -> style.javaClass.simpleName
            }
            pprTippingStyleLabel.text = "PayPal Reader: $pprStyle"
        }
    }

    private fun onTippingSettingsClicked() {
        val intent = CardReaderAction.TippingSettings().show(this)
        startActivity(intent)
    }

    private fun TippingPercentageOptions?.displayText() = when (this) {
        null -> "(Options = Null)"
        else -> "($option1, $option2, $option3)"
    }
}

inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}