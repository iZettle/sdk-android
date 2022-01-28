![andriod:5.0+](https://img.shields.io/badge/android-5.0%2B-green?style=flat)

# Getting started with Zettle Payments SDK for Android

The SDK is compatible with apps supporting Android API level 21 and above. The SDK itself is written in Kotlin which is the preferred language, however, we provide examples both in Kotlin and in Java.

The SDK requires location permission to function properly. During the integration process, you will be required to present your GitHub access token and your OAuth credentials. Ensure you have these at hand in advance, see Prerequisites.

Currently there are two ways of handling payments with the SDK: Card payments and PayPal QRC.
The following table describes the availability in different markets.

| Market         | Card Payments | PayPal QRC |
| :------------- | :------------ | :--------- |
| United States  | ✅             | ✅          |
| United Kingdom | ✅             | ✅          |
| Sweden         | ✅             | -          |
| Brazil         | ✅             | -          |
| Norway         | ✅             | -          |
| Denmark        | ✅             | -          |
| Finland        | ✅             | -          |
| Germany        | ✅             | ✅          |
| Mexico         | ✅             | -          |
| Netherlands    | ✅             | -          |
| France         | ✅             | -          |
| Spain          | ✅             | -          |
| Italy          | ✅             | -          |

Regardless of where you are located, you can only integrate your point of sales (POS) with the Zettle SDK in supported markets.

> **Note:** To test your integration with the _SDK_, you need to be located in one of the supported markets to be able to order a card reader.



## Prerequisites

### Generating your GitHub token

1. Click on your profile picture in GitHub.
2. Go to **Settings**.
3. Click on **Developer Settings**.
4. Select Personal access token and Generate a new token.
5. Select the scope read:packages and generate your token.

### Generating OAuth credentials for your app

User authorization in the SDK is performed through the implementation of OAuth 2.0. This means that the SDK requires Client ID and a Redirect URI from your integrating app.

Do the following to obtain a Client ID:

1. Go to the [Developer Portal](https://developer.zettle.com/register "Register on the Zettle Developer Portal") and create an account.
2. Verify your email address to be able to create new apps.
3. Create a new app from the Dashboard and choose the **Payments SDK for Android** option.
4. Once you submitted the form, you'll be given a Client ID which can be used to initialize the SDK.

## Setup

### Add a dependency

First you need to add a dependency.

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
    implementation 'com.izettle.payments:android-sdk-ui:1.25.0'
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

### Targeting Android 12

A change in Android 12 requires an app to explicitly declare `exported` in the manifest. Therefore you need to add the following to your `AndroidManifest.xml` when targeting Android 12 (API 31).

```xml
<service
    android:name="com.izettle.android.auth.sync.ZettleSyncService"
    android:exported="false"/>
<service
    android:name="com.izettle.android.auth.ZettleAuthService"
    android:exported="false"/>
<receiver
    android:name="com.izettle.android.auth.sync.ZettleSyncAlarmReceiver"
    android:exported="false"/>
```



### Configure authentication

To be able to log in a user through the Zettle SDK user interface, you must add the `OAuthActivity` to your manifest. The snippet below shows how to do it. Don't forget to replace the redirect URL data with your own in the intent filter, to be able to receive the result from the login (which will take place in a web view).

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

> **Note:** This setup is also mandatory when authorizing with token, since it is used when authorizing refunds. For more information about this login option, see [Initiate authorization with token](#initiate-authorization-with-token-since-1240).

### Initialize and start the SDK

The best place to initialize the `SDK` is in your `Application` class. If you don't have one, we recommend that you create one. 

After initialization you need to start the SDK. Preferably this is done by adding an observer on behalf of the SDK, to the `ProcessLifecycleOwner`. This way the SDK will automatically start and stop itself. You can also handle it manually by calling `start()` and `stop()`.

```kotlin
class MyApplication : Application() {

    fun onCreate() {
        // Initialize the SDK with your credentials
        val sdk = IZettleSDK.init(this, <Client ID>, <Redirect URL>)

        // Attach the SDKs lifecycle observer to your lifecycle. It allows the SDK to
        // manage bluetooth connection in a more graceful way
        ProcessLifecycleOwner.get().lifecycle.addObserver(SdkLifecycle(IZettleSDK))

        // Alternatively, start the SDK manually, but remember to also stop it manually.
        sdk.start()
    }
    // ...
}
```

### Authorize the user

Your application is responsible for user authorization. The SDK itself doesn't track authorization state. However, it will return `NotAuthorized` errors if you try to take payments or make refunds without valid user authorization.

#### Observe authorization state

To track if the user is authorized or not you need to create an observer.

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

Then you need to observe the user state.

```kotlin
fun onStart() {
    super.onStart()
    IZettleSDK.user.state.addObserver(authObserver)
}
```

Remember to remove the observer when it's not used anymore.

```kotlin
override fun onStop() {
    super.onStop()
    IZettleSDK.user.state.removeObserver(authObserver)
}
```

You can also use `LiveData` and `Observer` from AndroidX to observe the authorization state.

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

The `User.AuthState.LoggedIn` instance has an `info` property with the following fields.

| Name             | Type               | Description                                    |
| :--------------- | :----------------- | :--------------------------------------------- |
| `publicName`     | `String?`          | Public company or merchant name.               |
| `imageUrl`       | `ProfileImageUrl?` | Profile image in small, medium and large size. |
| `userId`         | `String?`          | Unique user ID.                                |
| `organizationId` | `String?`          | Unique organisation ID                         |
| `timeZone`       | `TimeZoneId`       | Merchant time zone.                            |
| `country`        | `CountryId?`       | Merchant country.                              |
| `currency`       | `CurrencyId`       | Currency used for all payments and refunds.    |



#### Initiate authorization with provided UI

Authorizing a user is simple. Just call the `login` method from your Activity and provide a toolbar color compatible with your color theme.

```kotlin
private fun doProvidedUILogin() {
    IZettleSDK.user.login(this, ResourcesCompat.getColor(resources, R.color.colorAccent, null))
}
```



#### Initiate authorization with token

The SDK requires a proof key for code exchange (PKCE) flow for the user to authorize.
If you have a PKCE flow setup to get access and refresh tokens, you can call the `login` method with the refresh token as parameter. For information about setting up a PKCE flow, see [OAuth PKCE method](https://oauth.net/2/pkce/).

```kotlin
private fun doTokenLogin() {
    IZettleSDK.user.login("pre-authorized-refresh-token")
}
```

> **Note:** You still need to declare the `OAuthActivity` in your manifest since it's used when performing refunds. 



## Card payments

### Starting card payments

First you need to create a `TransactionReference` object using the builder provided.

> **Important**: The transaction reference object must contain at least one unique field.

```kotlin
val internalUniqueTraceId = UUID.randomUUID().toString()
val reference = TransactionReference.Builder(internalUniqueTraceId)
    .put("PAYMENT_EXTRA_INFO", "Started from home screen")
    .build()
```

In the constructor of the builder you can provide your own ID with a maximum of 128 characters to identify the payment. This ID can be used to perform payment refunds.

Using the `put` method you can add any value you want to this object. However, the total data size (including key names) in this object can't be bigger than 4 kilobytes.

Next step is to start the `CardPaymentActivity`. To do this you can use our helper which creates configured `Intent` object.

```kotlin
val intent = CardPaymentActivity.IntentBuilder(this)
    // MANDATORY: Transaction amount in account currency
    .amount(20000L)
    // MANDATORY, Reference object created in previous step
    .reference(reference)
    // MANDATORY, enable login prompt in the payment flow if user is not yet logged-in
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

> **Note:** If setting `enableLogin` to `true` you need to setup auth for provided UI. Otherwise the user will be asked to login, but the app will never receive the result of the authentication.

**Note on tipping**

Setting `enableTipping` to `true` does not guarantee that tipping flow will be displayed. This is because tipping is not supported by all accounts and all card readers. Tipping is only supported with the Zettle Card Reader. The function is introduced market by market. If card reader software doesn’t support tipping, users will be prompted to either skip tipping or update card reader software.

Total tip amount is presented in `CardPaymentResult.Completed` completion with `gratuityAmount` property.

For more information on the tipping flow, see [SDK tippping support documentation](Documentation/SDK_Tipping_Support_Documentation.md).


### Processing card payment results

You will receive the payment result as an Activity result. The result `Bundle` contains two values:

| Name                                       | Type                | Description                              |
| :----------------------------------------- | :------------------ | :--------------------------------------- |
| `CardPaymentActivity.RESULT_EXTRA_REQUEST` | `Bundle`            | The `extras` bundle from request intent. |
| `CardPaymentActivity.RESULT_EXTRA_PAYLOAD` | `CardPaymentResult` | The payment result.                      |

The payment result is an instance of one of the following classes:

- `CardPaymentResult.Canceled`. Payment was canceled by merchant or customer. Doesn't contain any additional data.
- `CardPaymentResult.Failed`. Payment failed. The failure reason is defined by the `reason` field.
- `CardPaymentResult.Completed`. Card payment was successfully completed. Contains transaction info in the `payload` field.

See [CardPaymentResult documentation](Documentation/SDK_CardPaymentResult.md) for more information about the card payment result outcome.


### Retrieving card payments

To retrieve a card payment you need to provide the same unique reference ID provided to create the `TransactionReference` for the payment itself ([see Starting card payments](#starting-card-payments)).

```kotlin
IZettleSDK.refundsManager.retrieveCardPayment(internalTraceId, object : RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {
    override fun onFailure(reason: RetrieveCardPaymentFailureReason) { /*...*/ }
    override fun onSuccess(cardPaymentPayload: CardPaymentPayload) { /*...*/ }
})
```



### Refunding card payments

A refund works similar to a payment with the exception that you will need to provide the same unique reference ID provided to create the TransactionReference for the payment. [See Retrieving card payments](#retrieving-card-payments) on how to do that.

When you have your payment object, you need to create a new unique `TransactionReference` string for each refund. This reference ID can be used to trace the refund in our system if needed.

> **Important**: The transaction reference object must contain at least one unique field.

```kotlin
val internalTraceId = UUID.randomUUID().toString()
val reference = TransactionReference.Builder(internalTraceId)
    .put("REFUND_EXTRA_INFO", "Started from home screen")
    .build()
```

In the constructor builder you must provide the `CardPayment` retrieved from previous step.

Using the `put` method you can add any value to this object. However, the total data size (including key names) in this object can not be bigger than 4 kilobytes. You will get this reference back with transaction data, and can always request it back from our servers.

Next step is to start `RefundsActivity`. Use the `IntenBuilder` to create create the configured `Intent` for the refund.

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



### Processing card refund results

You will receive the refund result as an Activity result. The result `Bundle` contains two values:


| Name                                   | Type           | Description                              |
| :------------------------------------- | :------------- | :--------------------------------------- |
| `RefundsActivity.RESULT_EXTRA_REQUEST` | `Bundle`       | The `extras` bundle from request intent. |
| `RefundsActivity.RESULT_EXTRA_PAYLOAD` | `RefundResult` | The refund result.                       |


The refund result is an instance of one of the following classes:

- `RefundResult.Canceled`. Refund was canceled by merchant. Doesn't contain any additional data.
- `RefundResult.Failed`. Refund failed. The failure reason is defined by the `reason` field.
- `RefundResult.Completed`. Card refund was successfully completed. Contains transaction info in the `payload` field.

See [RefundResult documentation](Documentation/SDK_CardPaymentRefundResult.md) for more information on the card refund outcome result.



## PayPal QRC Payments

### Starting PayPal QRC payments

First you need to create an unique transaction reference string for each payment. This is used to trace this payment in our system.

> **Important:** This unique reference string must be UTF-8 and up to 128 characters, otherwise payment will be denied.

```kotlin
val uiniqueReference = UUID.randomUUID().toString()
```


Then you need to start `PayPalQrcPaymentActivity` and use the `IntentBuilder` to inject the desired configuration for this payment.

```kotlin
val intent = PayPalQrcPaymentActivity.IntentBuilder(this)
    // MANDATORY: Transaction amount in account currency 
    .amount(20000L)
    // MANDATORY, Reference object created in previous step        
    .reference(uiniqueReference)
    // OPTIONAL, Payment flow appearance - valid only for US market
    .appearance(PayPalQrcType.PayPal)
    .build()
            
// Start activity with the intent        
startActivityForResult(intent, 0)
```

**Note on appearance** 
When using PayPal QRC payments, merchants located in the US will have an option to start the payment using PayPal or Venmo UI appearances. This is because consumers can scan the QR code using either the Venmo or PayPal applications. In the payment result, you will be able to extract which payment method was used by the consumer.



### Processing PayPal QRC payment results

You will receive the payment result as an Activity result. The result `Bundle` contains two values:


| Name                                            | Type                     | Description                              |
| :---------------------------------------------- | :----------------------- | :--------------------------------------- |
| `PayPalQrcPaymentActivity.RESULT_EXTRA_REQUEST` | `Bundle`                 | The `extras` bundle from request intent. |
| `PayPalQrcPaymentActivity.RESULT_EXTRA_PAYLOAD` | `PayPalQrcPaymentResult` | The payment result.                      |


The payment result is an instance of one of the following classes:

- `PayPalQrcPaymentResult.Canceled`. Payment was canceled. Doesn't contain any additional data.
- `PayPalQrcPaymentResult.Failed`. Payment failed. The failure reason is defined by the `reason` field.
- `PayPalQrcPaymentResult.Completed`. PayPal QRC payment was successfully completed. Contains transaction info in the `payload` field.

See [PayPalQrcPaymentResult documentation](Documentation/SDK_PayPalQrcPaymentResult.md) for more information about the PayPal QRC payment result outcome.



### Retrieving PayPal QRC payments

To retrieve a PayPal QRC payment you need to provide the same unique reference ID provided for the payment itself ([see Starting PayPal QRC payments](#starting-paypal-qrc-payments)).

```kotlin 
IZettleSDK.refundsManager.retrievePayPalQrc(activity, uniqueReference, object : RefundsManager.Callback<PayPalQrcPayment, RetrievePayPalQrcFailureReason> {
    override fun onFailure(reason: RetrievePayPalQrcFailureReason) {/*...*/}
    override fun onSuccess(payment: PayPalQrcPayment) {/*...*/}
})
```



### Refunding PayPal QRC payments

A refund works similar to a payment with the exception that you will need to provide the original payment when starting the refund. See [Retrieving PayPal QRC payments](#retrieving-paypal-qrc-payments) on how to do that.

When you have your payment object, you need to create a new unique transaction reference string for each refund. This reference string can be used to trace the refund in our system if needed.

```kotlin
val uniqueRefundReference = UUID.randomUUID().toString()
```

> **Important**: This unique reference string must be UTF-8 and up to 128 characters, otherwise payment will be denied.

Next step is to start `PayPalQrcPaymentActivity`. Use the `IntentBuilder` to inject the desired configuration for this refund.

```kotlin
val intent = PayPalQrcRefundActivity.IntentBuilder(context)
    // OPTIONAL: Refund amount in account currency
    // This amount must be less or equal to the original card payment.
    // If not provided it will use original card payment amount
    // NOTE: if not provided, then it will perform a full refund.
    .amount(20000L)
    // Reference object created in previous step        
    .reference(uniqueRefundReference)
    // Unique reference for the payment that you want to perform refund.
    .paymentReference(paymentUniqueRerence)
    .build()
            
// Start activity with the intent        
startActivityForResult(intent, 0)
```



### Processing PayPal QRC refund results

You will receive the refund result as an Activity result. The result `Bundle` contains two values:


| Name                                           | Type                    | Description                              |
| :--------------------------------------------- | :---------------------- | :--------------------------------------- |
| `PayPalQrcRefundActivity.RESULT_EXTRA_REQUEST` | `Bundle`                | The `extras` bundle from request intent. |
| `PayPalQrcRefundActivity.RESULT_EXTRA_PAYLOAD` | `PayPalQrcRefundResult` | The refund result.                       |

The refund result is an instance of one of the following classes:

- `PayPalQrcRefundResult.Canceled`. Refund was canceled. Doesn't contain any additional data.
- `PayPalQrcRefundResult.Failed`. Refund failed. The failure reason is defined by the `reason` field.
- `PayPalQrcRefundResult.Completed`. PayPal QRC refund was successfully completed. Contains transaction info in the `payload` field.

See [PayPalQrcRefundResult documentation](Documentation/SDK_PayPalQrcRefundResult.md) for more information about the card refund outcome result.


## Settings

### Card reader settings

If you would like to provide a way to access card reader settings from outside the payment flow, you can use the following static method to get the intent.

```kotlin
val intent = CardReadersActivity.newIntent(context)
startActivity(intent)
```



### PayPal QRC activation

To start taking PayPal QRC payments, users must first read and accept the terms and conditions from the settings screen.

```kotlin
val intent = PayPalQrcActivationActivity.IntentBuilder(context).build()
startActivity(intent)
```



## Get help

Contact our [Integrations team](mailto:sdk@zettle.com) for more information.
