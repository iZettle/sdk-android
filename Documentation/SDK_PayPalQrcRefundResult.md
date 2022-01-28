# PayPalQrcRefundResult
The `PayPalQrcRefundResult` is a Kotlin sealed class, meaning it's always one of the following implementations.

| Name | Description |
|:---|:---|
| `PayPalQrcRefundResult.Canceled` | Refund was canceled by merchant or customer |
| `PayPalQrcRefundResult.Failed` | Refund failed |
| `PayPalQrcRefundResult.Completed` | Refund was successfully completed |

The failed and completed results contain more detailed information regarding the failure reason, or the payload of a successful refund.


## Extracting the failure reason

The `PayPalQrcRefundResult.Failed` contains an instance of `PayPalQrcPaymentFailureReason` that describes the failure and can contain any of the following reasons.

| Name | Description |
|:---|:---|
| `PayPalQrcPaymentFailureReason.NotAuthenticated` | Authentication flow was cancelled or interrupted |
| `PayPalQrcPaymentFailureReason.NotAuthorized` | There is no authorized user to process payment request |
| `PayPalQrcPaymentFailureReason.AlreadyRefunded` | Payment was already refunded |
| `PayPalQrcPaymentFailureReason.NotFound` | Payment with given reference ID was not found |
| `PayPalQrcPaymentFailureReason.AmountTooHigh` | Trying to perform refund with amount higher than original payment |
| `PayPalQrcPaymentFailureReason.PartialRefundNotSupported` | Partial refund is not allowed for this payment |
| `PayPalQrcPaymentFailureReason.InsufficientFunds` | Account does not have sufficient funds to perform refund |
| `PayPalQrcPaymentFailureReason.RefundExpired` | Payment refund is too old to be refunded |
| `PayPalQrcPaymentFailureReason.TechnicalError` | Payment failed because of technical issues |
| `PayPalQrcPaymentFailureReason.NetworkError` | Communication with Zettle servers failed |
| `PayPalQrcPaymentFailureReason.Failed` | Failure due to unknown reasons |



## Extracting the success payload

A successful refund contains a payload of type `PayPalQrcRefund` with fields describing the refund as listed in the following.

| Name | Type | Description |
|:---|:---|:---|
| `amount` | `Long` | Total refund amount |
| `originalAmount` | `Long?` | Contains the original payment amount |
| `reference` | `String` | The unique reference string provided when refund was started |
| `transactionId` | `String?` | Transaction Identifier that should be displayed on receipts |
