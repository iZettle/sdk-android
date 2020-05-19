## Getting started with Card Payments SDK

### Intro

`SDK` is compatible with apps supporting Android API level 19 and above. `SDK` itself is written in Kotlin that's why
all examples in this HOWTO are written in Kotlin and Kotlin is the preferred language for the host app.

`SDK` requires location permission to function properly. It's mostly needed to scan and connect bluetooth low energy
devices, <but also required to take payments?>.

### Step 1: Add a dependency

First of all you need to add a dependency
```groovy
maven {
    url = uri("https://maven.pkg.github.com/iZettle/sdk-android")
}

dependencies {
    implementation 'com.izettle.payments:android-sdk-ui:<latest version>' 
}
```

We are using AndroidX in some libraries so you will get them as dependencies as well.

### Step 2: Configure your app

To be able to login a user through iZettle you must add callback activity to your manifest. The snippet below shows how
you should do it, but don't forget to replace the redirect url data with your own in the intent filter.
```xml
<activity 
    android:name="com.izettle.android.auth.OAuthActivity" 
    android:launchMode="singleTask"
    android:taskAffinity="@string/oauth_activity_task_affinity"> 
    <intent-filter> 
        <data 
            android:host="[redirect url host]" 
            android:scheme="[redirect url scheme]" />
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
    </intent-filter>
</activity>
```
### Step 3: Initialize SDK

(here about obtaining oauth data)

The best place to initialize `SDK` is your `Application` class. If you don't have one, we recommend that you create one.
```kotlin
class MyApplication : Application() {
    
    // ...
        
    fun onCreate() {
        
        // Initialize SDK by your credentials    
        IZettleSDK.init(this, <Client ID>, <Redirect URL>)
            
        // Attach SDK lifecycle observer to your lifecycle. It allows SDK to
        // manage bluetooth connection in a more graceful way                   
        ProcessLifecycleOwner.get().lifecycle.addObserver(SdkLifecycle(IZettleSDK))
      
    }    
    
        // ...    
    
}
```
### Step 4: Authorize user

Your application is responsible for user authorization. The SDK itself doesn't track auth state, but will return
NotAuthorized errors if you try to take payments or make refunds without valid user authorization.

It's quite easy to track if the user is authorized or not. To do so you need to create an observer first.
```kotlin
private val authObserver = object : StateObserver<User.AuthState> {
    override fun onNext(state: User.AuthState) {
        when (state) {
            is User.AuthState.LoggedIn -> // User authorized
            is User.AuthState.LoggedOut -> // There is no authorized use 
        }
    }
}
```
And then subscribe to user state
```kotlin
fun onStart() {
    super.onStart()    
    IZettleSDK.user.state.observe(authObserver)
}
```
And unsubscribe if you need to
```kotlin
override fun onStop() {
    super.onStop()
    IZettleSDK.user.state.removeObserver(authObserver)
}
```
Or you can user `LiveData` and `Observer` from AndroidX
```kotlin
private val authObserver = Observer<User.AuthState> {
    when (state) {
        is User.AuthState.LoggedIn -> // User authorized
        is User.AuthState.LoggedOut -> // There is no authorized use
    }
}
    
override fun onCreate(savedInstanceState: Bundle?) {
    // ...
        
    IZettleSDK.user.state.toLiveData().observe(this, authObserver)
        
    //...        
}
    
```
<I'll add User.AuthState.LoggedIn description later when we will decide which info we will share with integrators>

Authorizing a user is fairly simple. Just call login method from your Activity and provide a toolbar color compatible with
your color theme.
```kotlin
private fun doLogin() {
    ZettleSDK.user.login(this, ResourcesCompat.getColor(resources, R.color.colorAccent, null))
}
```

### Step 5: Starting payment

First of all you need to create TransactionReference object using the builder provided.

IMPORTANT: The transaction reference object must contain at least one unique field
```kotlin
val internalTraceId = UUID.randomUUID().toString()
val reference = TransactionReference.Builder(internalTraceId)
    .put("PAYMENT_EXTRA_INFO", "Started from home screen")
    .build()
```
In the constructor builder you can provide your own id with a maximum of 128 characters to identify this payment, which can be used to perform payment refunds.  

Using the `put` method you can add whatever you want to this object, but keep in mind that the total data size (including key names) in this object can't be bigger than 4 kilobytes. You will get this reference back with transaction data and can always request it back from our servers.

Then you need to start CardPaymentActivity. To do so you may use our helper which creates configured `Intent` object
```kotlin
val intent = CardPaymentActivity.IntentBuilder(this)
    // Transaction amount in account currency 
    .amount(20000L)
    // Reference object created in previous step        
    .reference(reference)
    // Optional, you can enable tipping (disabled by default)         
    .enableTipping(true)
    .build()
            
// Start activity with the intent        
startActivityForResult(intent, 0)
```
### Step 6: Processing payment result

You will receive the payment result as activity result. Result `Bundle` contains two values:


1.  `CardPaymentActivity.RESULT_EXTRA_REQUEST` contains all extras from request intent
2.  `CardPaymentActivity.RESULT_EXTRA_PAYLOAD` contains payment result

The payment result is an instance of one of the following classes:

#### CardPaymentResult.Canceled

Payment was canceled by merchant or customer. Doesn't contain any additional data

#### CardPaymentResult.Failed

Payment failed. The failure reason is defined by reason field and be one of the following:

1. `FailureReason.TechnicalError` - payment failed because of technical issues. Can happen because of bluetooth
   communication problem or other technical issue
2. `FailureReason.NetworkError` - Communication with iZettle servers failed
3. `FailureReason.NotAuthorized` - There is no authorized user to process payment request
4. `FailureReason.AboveMaximum` - Requested amount is greater than account limit
5. `FailureReason.BellowMinimum` - Requested amount is smaller than allowed minimum

#### CardPaymentResult.Completed

Card payment was successfully completed. Contains transaction info in `payload` field.

* `amount` - Total transaction amount (also includes tip amount if applicable)
* `gratuityAmount` - Contains total tip amount if tipping is performed, `null` otherwise
* `cardType` - card brand: VISA, MASTERCARD and so on
* `cardPaymentEntryMode` - EMV, CONTACTLESS_EMV, MAGSTRIPE_CONTACTLESS, MAGSTRIPE etc. More entry modes might be added independent of SDK version
* `tsi`
* `tvr`
* `cardIssuingBank` - card issuing bank if provided
* `maskedPan` - e.g. "**** **** **** 1111"
* `applicationName`
* `authorizationCode`
* `installmentAmount`
* `nrOfInstallments`
* `mxFiid`
* `mxCardType`
* `reference` - your reference object

<I'll add payload description later when we will decide what should be public>

### Step 7: Performing refund


To perform a refund you first need to find a CardPayment that matches a given id provided in the `TransactionReference.Builder(internalTraceId)`.

```kotlin
IZettleSDK.refundsManager.retrieveCardPayment(internalTraceId, object : RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {
    override fun onFailure(reason: RetrieveCardPaymentFailureReason) {
        // ...
    }

    override fun onSuccess(cardPaymentPayload: CardPaymentPayload) {
        // ...
    }
})
```

First of all you need to create TransactionReference object using the builder provider since a refund is basically a transaction with negative value. 

IMPORTANT: The transaction reference object must contain at least one unique field
```kotlin
val internalTraceId = UUID.randomUUID().toString()
val reference = TransactionReference.Builder(internalTraceId)
    .put("REFUND_EXTRA_INFO", "Started from home screen")
    .build()
```

In the constructor builder you must provide the CardPayment retrieved from previous step

Using the `put` method you can add whatever you want to this object, but keep in mind that the total data size (including key names) in this object can't be bigger than 4 kilobytes. You will get this reference back with transaction data and can always request it back from our servers.

Then you need to start RefundsActivity. To do so you may use our helper which creates configured `Intent` object
```kotlin
val intent = RefundsActivity.IntentBuilder(cardPaymentPayload)
    // Refund amount in account currency
    // This amount must be less or equals to the original card payment.
    // If not provided it will use original card payment amount
    .refundAmount(20000L)
    // Reference object created in previous step        
    .reference(reference)
    // Optional, you can provide tax amount of this card payment to be displayed in the UI
    .taxAmount(100L)
    // Optional, you can provide receipt number of this card payment to be displayed in the UI
    .receiptNumber("#12345")
    .build()
            
// Start activity with the intent        
startActivityForResult(intent, 0)
```
### Step 8: Processing refund result

You will receive the payment result as activity result. Result `Bundle` contains two values:

1.  `RefundsActivity.RESULT_EXTRA_REQUEST` contains all extras from request intent
2.  `RefundsActivity.RESULT_EXTRA_PAYLOAD` contains refund result

The payment result is an instance of one of the following classes:

#### RefundResult.Canceled

Refund was canceled by merchant. Doesn't contain any additional data

#### RefundResult.Failed

Refund failed. The failure reason is defined by reason field and be one of the following:

1. `RefundFailureReason.Failed` - Failure due to unknown reasons
2. `RefundFailureReason.NotAuthorized` - There is no authorized user to process payment request
3. `RefundFailureReason.NotFound` - Payment with given reference id was not found
4. `RRefundFailureReason.NotRefundable` - Payment is not refundable
5. `RefundFailureReason.NetworkError` - Communication with iZettle servers failed
6. `RefundFailureReason.TechnicalError` - Payment failed because of technical issues
7. `RefundFailureReason.AlreadyRefunded` - Payment was already refunded
8. `RefundFailureReason.AmountTooHigh` - Trying to perform refund with amount higher than original payment
9. `RefundFailureReason.RefundExpired` - Payment refund is too old to be refunded
10. `RefundFailureReason.InsufficientFunds` - Account has no sufficient funds to perform refund
11. `RefundFailureReason.PartialRefundNotSupported` - Partial refund is not allowed for this payment

#### RefundResult.Completed

Card payment was successfully completed. Contains transaction info in `payload` field.

* `originalAmount` - Total original card payment amount (also includes tip amount if applicable)
* `cardType` - card brand: VISA, MASTERCARD and so on
* `cardIssuingBank` - card issuing bank if provided
* `maskedPan` - e.g. "**** **** **** 1111"
* `reference` - your reference object

<I'll add payload description later when we will decide what should be public>
