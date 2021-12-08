# CardPaymentResult
The `CardPaymentResult` is a Kotlin sealed class, meaning it's always one of the following implementations.

| Name | Description |
|:---|:---|
| `CardPaymentResult.Canceled` | Payment was canceled by merchant or customer |
| `CardPaymentResult.Failed`  | Payment failed |
| `CardPaymentResult.Completed` | Payment was successfully completed |

The payment-failed result contains more details about failure reason. The payment-completed result contains more details about the successfully completed payment.

## Extracting the failure reason
The `CardPaymentResult.Failed` contains an instance of `FailureReason`. This describes the failure, which can be caused by any of the following reasons.

| Name | Description |
|:---|:---|
| `FailureReason.TechnicalError` | Payment failed because of technical issues. Can be caused by a Bluetooth communication problem or other technical issue. |
| `FailureReason.NetworkError` | Communication with iZettle servers failed |
| `FailureReason.NotAuthorized` | There is no authorized user to process payment request |
| `FailureReason.AboveMaximum` | Requested amount is greater than account limit |
| `FailureReason.BellowMinimum` | Requested amount is smaller than allowed minimum |


## Extracting the success payload
A successful payment contains a payload of type `ResultPayload` with many fields describing the payment as listed in the following.

| Name | Type | Description |
|:---|:---|:---|
| `amount` | `Long` | Total transaction amount (also includes tip amount if applicable) |
| `gratuityAmount` | `Long?` | Contains total tip amount if tipping is performed, `null` otherwise |
| `cardType` | `String?` | Card brand: VISA, MASTERCARD and so on. |
| `cardPaymentEntryMode` | `String?` | EMV, CONTACTLESS_EMV, MAGSTRIPE_CONTACTLESS, MAGSTRIPE etc. More entry modes might be added independent of SDK version |
| `cardholderVerificationMethod` | `String?` |  |
| `tsi` | `String?` | EMV tags |
| `tvr` | `String?` | EMV tags |
| `applicationIdentifier` | `String?` | EMV tags (aid) |
| `cardIssuingBank` | `String?` | Card issuing bank if provided |
| `maskedPan` | `String?` | For example "**** **** **** 1111" |
| `panHash` | `String?` | Card pan hash |
| `applicationName` | `String?` ||
| `authorizationCode`| `String?` ||
| `installmentAmount` | `Long` | Value of each installment |
| `nrOfInstallments` | `Int` | Number of installment chosen |
| `mxFiid` | `String?` | Mexico-specific data |
| `mxCardType` | `String?` | Mexico specific data |
| `reference` | `TransactionReference?` | Your reference object |
