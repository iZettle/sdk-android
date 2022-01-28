# CardPayment RefundResult
The `RefundResult` is a Kotlin sealed class, meaning it's always one of the following implementations.

| Name | Description |
|:---|:---|
| `RefundResult.Canceled` | Refund was canceled by merchant |
| `RefundResult.Failed` | Refund failed |
| `RefundResult.Completed` | Refund was successfully completed |

The failed and successful results contain more detailed information regarding failure reason, and the payload of a successful refund.

## Extracting the failure reason
The `RefundResult.Failed` contains an instance of `RefundFailureReason` that describes the failure and can contain any of the following reasons.

| Name | Description |
|:---|:---|
| `RefundFailureReason.Failed` | Failure due to unknown reasons |
| `RefundFailureReason.NotAuthorized` | There is no authorized user to process payment request |
| `RefundFailureReason.NotFound` | Payment with given reference id was not found |
| `RefundFailureReason.NotRefundable` | Payment is not refundable |
| `RefundFailureReason.NetworkError` | Communication with Zettle servers failed |
| `RefundFailureReason.TechnicalError` | Payment failed because of technical issues |
| `RefundFailureReason.AlreadyRefunded` | Payment was already refunded |
| `RefundFailureReason.AmountTooHigh` | Trying to perform refund with amount higher than original payment |
| `RefundFailureReason.RefundExpired` | Payment refund is too old to be refunded |
| `RefundFailureReason.InsufficientFunds` | Account does not have sufficient funds to perform refund |
| `RefundFailureReason.PartialRefundNotSupported` | Partial refund is not allowed for this payment |



## Extracting the success payload
A successfull refund contains a payload of type `RefundPayload` which has fields describing the refund. They are listed in the table below.

| Name | Type | Description |
|:---|:---|:---|
| `originalAmount` | `Long` | Total original card payment amount (also includes tip amount if applicable) |
| `refundedAmount` | `Long` |  |
| `cardType` | `String?` | card brand: VISA, MASTERCARD and so on |
| `maskedPan` | `String?` | e.g. "**** **** **** 1111" |
| `cardPaymentUUID` | `String` |  |

