# PayPalQrcPaymentResult
The `PayPalQrcPaymentResult` is a Kotlin sealed class, meaning it's always one of the following implementations.

| Name | Description |
|:---|:---|
| `PayPalQrcPaymentResult.Canceled` | Payment was canceled by merchant or customer |
| `PayPalQrcPaymentResult.Failed` | Payment failed |
| `PayPalQrcPaymentResult.Completed` | Payment was successfully completed |

The failed and completed results contain more detailed information regarding the failure reason, or the payload of the successful payment.

## Extracting the failure reason
The `PayPalQrcPaymentResult.Failed` contains an instance of `PayPalQrcPaymentFailureReason` that describes the failure and can contain any of the following reasons.

| Name | Description |
|:---|:---|
| `PayPalQrcPaymentFailureReason.Timeout` | Payment took too long to be completed |
| `PayPalQrcPaymentFailureReason.TechnicalError` | Payment failed because of technical issues such as problems in the communication protocol. |
| `PayPalQrcPaymentFailureReason.NetworkError` | Communication with Zettle servers failed |
| `PayPalQrcPaymentFailureReason.ActivationNotCompleted` | User was not yet onborded to this payment method. |
| `PayPalQrcPaymentFailureReason.SellerDataError` | Found some problems in the merchant information. |
| `PayPalQrcPaymentFailureReason.FeatureNotEnabled` | Currently logged-in user is not allowed to use this payment method, could be the case when merchant is not from one of the supported countries. |
| `PayPalQrcPaymentFailureReason.AboveMaximum` | Requested amount is greater than account limit |
| `PayPalQrcPaymentFailureReason.BellowMinimum` | Requested amount is smaller than allowed minimum |


## Extracting the success payload
A successful payment contains a payload of type `PayPalQrcPayment` with fields describing the payment as listed in the following.

| Name | Type | Description |
|:---|:---|:---|
| `amount` | `Long` | Total transaction amount |
| `type` | `PayPalQrcType` | The QRC flavor used to perform the payment - PayPal or Venmo (USA only) |
| `reference` | `String` | The unique reference string provided when payment was started |
| `transactionId` | `String?` | Transaction Identifier that should be displayed on receipts |
