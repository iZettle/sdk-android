package com.zettle.payments.android.kotlin_example

import android.os.Parcelable
import com.zettle.sdk.feature.qrc.ui.payment.QrcPaymentPayload
import com.zettle.sdk.feature.qrc.ui.refund.QrcRefund
import com.zettle.sdk.feature.cardreader.payment.Transaction
import com.zettle.sdk.feature.cardreader.payment.refunds.CardPaymentPayload
import com.zettle.sdk.feature.cardreader.payment.refunds.RefundPayload
import com.zettle.sdk.feature.manualcardentry.ui.payments.models.ManualCardEntryPaymentPayload
import com.zettle.sdk.feature.manualcardentry.ui.refunds.models.ManualCardEntryRefundPayload
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.text.DecimalFormat


@Parcelize
class PaymentResultData(
    val title: String,
    val amount: String,
    val reference: String?,
    val resultList: List<ResultItem>,
    val resultType: ResultType? = ResultType.PAYMENT
) : Parcelable

@Parcelize
class ResultItem(
    val name: String,
    val value: String?
) : Parcelable

enum class ResultType {
    PAYMENT, REFUND
}

fun QrcPaymentPayload.toResultListItems(): List<ResultItem> = listOf(
    ResultItem("transactionId", transactionId),
    ResultItem("referenceNumber", referenceNumber),
    ResultItem("Type", type.toString())
)

fun QrcRefund.toResultListItems(): List<ResultItem> = listOf(
    ResultItem("originalAmount", formatPaymentAmount(originalAmount)),
    ResultItem("transactionId", transactionId),
    ResultItem("referenceNumber", referenceNumber),
    ResultItem("Type", type.toString())
)

fun ManualCardEntryPaymentPayload.toResultListItems(): List<ResultItem> = listOf(
    ResultItem("transactionId", transactionId),
    ResultItem("referenceNumber", referenceNumber),
)

fun ManualCardEntryRefundPayload.toResultListItems(): List<ResultItem> = listOf(
    ResultItem("originalAmount", formatPaymentAmount(originalAmount)),
    ResultItem("transactionId", transactionId),
    ResultItem("referenceNumber", referenceNumber),
)

fun Transaction.ResultPayload.toResultListItems(): List<ResultItem> = listOf(
    ResultItem("applicationIdentifier", applicationIdentifier),
    ResultItem("applicationName", applicationName),
    ResultItem("authorizationCode", authorizationCode),
    ResultItem("cardholderVerificationMethod", cardholderVerificationMethod),
    ResultItem("cardIssuingBank", cardIssuingBank),
    ResultItem("cardPaymentEntryMode", cardPaymentEntryMode),
    ResultItem("cardType", cardType),
    ResultItem("gratuityAmount", formatPaymentAmount(gratuityAmount ?: 0)),
    ResultItem("installmentAmount", installmentAmount.toString()),
    ResultItem("maskedPan", maskedPan),
    ResultItem("mxCardType", mxCardType),
    ResultItem("mxFiid", mxFiid),
    ResultItem("nrOfInstallments", nrOfInstallments.toString()),
    ResultItem("panHash", panHash),
    ResultItem("reference", reference?.id),
    ResultItem("referenceNumber", referenceNumber),
    ResultItem("transactionId", transactionId),
    ResultItem("tsi", tsi),
    ResultItem("tvr", tvr)
)

fun RefundPayload.toResultListItems(): List<ResultItem> = listOf(
    ResultItem("transactionId", transactionId),
    ResultItem("originalAmount", formatPaymentAmount(originalAmount)),
    ResultItem("refundedAmount", formatPaymentAmount(refundedAmount)),
    ResultItem("cardType", cardType),
    ResultItem("maskedPan", maskedPan)
)

fun CardPaymentPayload.toResultListItems(): List<ResultItem> = listOf(
    ResultItem("transactionId", transactionId),
    ResultItem("applicationName", applicationName),
    ResultItem("cardPaymentEntryMode", cardPaymentEntryMode),
    ResultItem("cardType", cardType),
    ResultItem("reference", referenceId),
    ResultItem("referenceNumber", referenceNumber),
)

fun formatPaymentAmount(currentAmount: Long): String = DecimalFormat("0.00").format(
    BigDecimal(currentAmount.toString()).divide(BigDecimal("100"))
)
