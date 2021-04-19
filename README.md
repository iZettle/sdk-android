![andriod:4.4+](https://img.shields.io/badge/android-4.4%2B-green?style=flat)

## Getting started with Zettle Payments SDK for Android

### Intro

The `SDK` is compatible with apps supporting Android API level 19 and above. The `SDK` itself is written in Kotlin which is the preferred language, however, we provide examples both in Kotlin and in Java.

The `SDK` requires location permission to function properly. It is needed to scan and connect Bluetooth devices, in this case, the Zettle Readers. During the integration process, you will be required to present your Github access token and your oAuth credentials. It is best to have these in advance. 

As card payments with Zettle is currently accepted in the following regions, the `SDK` can be used only for the available regions.

-   United States
-   Great Britain
-   Norway
-   Denmark
-   France
-   Sweden
-   Brazil
-   Finland
-   Germany
-   Italy
-   Mexico
-   Spain
-   The Netherlands

> **Tip:** You can make integrations with the `SDK` for the available regions, no matter where you live.

### Generating your Github token 
1. Click on your profile picture in Github
2. Go to Settings
3. Click on Developer Settings
4. Select Personal access token and Generate a new token
5. Select the scope read:packages and generate your token

### Generating OAuth credentials for your app

User authorization in the SDK is performed through the implementation of OAuth 2.0. This means that the SDK requires Client ID and a Redirect URI from your integrating app.

To obtain Client ID, create an account in the Zettle Developer Portal and create an Android SDK developer application by completing the following steps:
1. Go to https://developer.zettle.com/register and create an account
2. Verify your email address to be able to create new apps
3. Create a new app from the Dashboard and choose _Payments SDK for Android_ option
4. Once you submitted the form, you'll be given a Client ID which can be used to initialize the SDK

### Step 1: Add a dependency

First of all you need to add a dependency
```groovy
maven {
    url = uri("https://maven.pkg.github.com/iZettle/sdk-android")
    credentials(HttpHeaderCredentials) {
        name "Authorization"
        value "Bearer <Your GitHub Token>" // More about auth tokens https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line
    }
    authentication {
        header(HttpHeaderAuthentication)
    }
}

dependencies {
    implementation 'com.izettle.payments:android-sdk-ui:1.14.13'
}
```

Also you may face some conflicts in the .kotlin_module META-INF, so we suggest to exclude META-INF by adding

```groovy
android {
    packagingOptions {
        exclude 'META-INF/*.kotlin_module'
    }
}
```

We are using AndroidX in some libraries so you will get them as dependencies as well.

### Step 2: Configure your app

To be able to login a user through Zettle you must add callback activity to your manifest. The snippet below shows how
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
    IZettleSDK.user.state.addObserver(authObserver)
}
```
And unsubscribe if you need to
```kotlin
override fun onStop() {
    super.onStop()
    IZettleSDK.user.state.removeObserver(authObserver)
}
```
Or you can use `LiveData` and `Observer` from AndroidX
```kotlin
private val authObserver = Observer<User.AuthState> {
    when (it) {
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

User.AuthState.LoggedIn has `info` object with usable fields.

* `publicName` - public company or merchant public name
* `imageUrl` - profile image in small, medium and large size
* `userId` - unique user id. can be used to join iZettle user and your data for it
* `organizationId` - unique organisation id
* `timeZone` - merchant time zone
* `country` - merchant country
* `currency` - currency used for all payments & refunds

Authorizing a user is simple. Just call login method from your Activity and provide a toolbar color compatible with
your color theme.
```kotlin
private fun doLogin() {
    IZettleSDK.user.login(this, ResourcesCompat.getColor(resources, R.color.colorAccent, null))
}
```

### Step 5: Starting payment

First of all you need to create a TransactionReference object using the builder provided.

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
    // MANDATORY: Transaction amount in account currency 
    .amount(20000L)
    // MANDATORY, Reference object created in previous step        
    .reference(reference)
    // MANDATORY, you can enable login prompt in the payment flow if user is not yet logged-in
    .enableLogin(enableLogin)
    // OPTIONAL, you can enable tipping (disabled by default)       
    // This option will only work for markets with tipping support
    .enableTipping(true)
    // OPTIONAL, you can enable installments (enabled by default)
    // This option will only work for markets with installments support
    .enableInstalments(enableInstallments)
    .build()
            
// Start activity with the intent        
startActivityForResult(intent, 0)
```
### Step 6: Processing payment result

You will receive the payment result as an activity result. Result `Bundle` contains two values:


1.  `CardPaymentActivity.RESULT_EXTRA_REQUEST` contains all extras from request intent
2.  `CardPaymentActivity.RESULT_EXTRA_PAYLOAD` contains payment result

The payment result is an instance of one of the following classes:

#### CardPaymentResult.Canceled

Payment was canceled by merchant or customer. Doesn't contain any additional data.

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
* `tsi` - EMV tags
* `tvr` - EMV tags
* `applicationIdentifier` - EMV tags (aid)
* `cardIssuingBank` - card issuing bank if provided
* `maskedPan` - e.g. "**** **** **** 1111"
* `panHash` - Card pan hash
* `applicationName`
* `authorizationCode`
* `installmentAmount` - Value of each installment
* `nrOfInstallments` - Number of installment chosen 
* `mxFiid` - Mexico specific data
* `mxCardType` - Mexico specific data
* `reference` - your reference object

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

First of all you need to create a TransactionReference object using the builder provider since a refund is basically a transaction with negative value. 

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
    // This amount must be less or equal to the original card payment.
    // If not provided it will use original card payment amount
    .refundAmount(20000L)
    // Reference object created in previous step        
    .reference(reference)
    // Optional, you can provide tax amount of this card payment to be displayed in the UI
    .taxAmount(100L)
    // Optional, you can provide the receipt number of this card payment to be displayed in the UI
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

Refund was canceled by merchant. Doesn't contain any additional data.

#### RefundResult.Failed

Refund failed. The failure reason is defined by reason field and be one of the following:

1. `RefundFailureReason.Failed` - Failure due to unknown reasons
2. `RefundFailureReason.NotAuthorized` - There is no authorized user to process payment request
3. `RefundFailureReason.NotFound` - Payment with given reference id was not found
4. `RefundFailureReason.NotRefundable` - Payment is not refundable
5. `RefundFailureReason.NetworkError` - Communication with Zettle servers failed
6. `RefundFailureReason.TechnicalError` - Payment failed because of technical issues
7. `RefundFailureReason.AlreadyRefunded` - Payment was already refunded
8. `RefundFailureReason.AmountTooHigh` - Trying to perform refund with amount higher than original payment
9. `RefundFailureReason.RefundExpired` - Payment refund is too old to be refunded
10. `RefundFailureReason.InsufficientFunds` - Account does not have sufficient funds to perform refund
11. `RefundFailureReason.PartialRefundNotSupported` - Partial refund is not allowed for this payment

#### RefundResult.Completed

Card payment was successfully completed. Contains transaction info in `payload` field.

* `originalAmount` - Total original card payment amount (also includes tip amount if applicable)
* `cardType` - card brand: VISA, MASTERCARD and so on
* `cardIssuingBank` - card issuing bank if provided
* `maskedPan` - e.g. "**** **** **** 1111"
* `reference` - your reference object

### Open card reader settings

If you would like to provide a way to access a card reader settings from outside the payment flow you can use the following static method to get the intent.

```kotlin
val intent = CardReadersActivity.newIntent(context)
startActivity(intent)
```

## Get help
Contact our [Integrations team](mailto:sdk@zettle.com) for more information.
