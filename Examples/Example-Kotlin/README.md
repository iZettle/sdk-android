Android SDK example app
===

- [Introduction](#introduction)
- [Prerequisites](#prerequisites)
- [Step 1: Configure the example app](#step-1-configure-the-example-app)
- [Step 2: Test the example app](#step-2-test-the-example-app)

## Introduction

The Android SDK example app is to help you get started with Zettle Payments SDK for Android.

You can use the example to learn about SDK. For example, SDK structure, input data format, and payment and refund payload.

For information about Zettle Payments SDK for Android, see [Android SDK docs on Zettle Developer Portal](https://developer.zettle.com/docs/android-sdk).

## Prerequisites

- Developer environment, for example Android studio
- Android version 5 (API level 21) or higher
- GitHub personal access token with scope `read:packages`, See [Generate a GitHub token](https://developer.zettle.com/docs/android-sdk/get-started#generate-a-github-token)
- Credentials for the app that include a client ID and a redirect URL (callback URL). If you don't have these, see [create credentials for an SDK app](https://developer.zettle.com/docs/get-started/user-guides/create-app-credentials/create-credentials-sdk-app).

## Step 1: Configure the example app

Fill in the `iZettleSDK.gradle` file with the data that you have prepared for the prerequisites.

```

// Personal access token with read rights to github package registry
ext.iZettleSDK.githubAccessToken = ""

// Auto generated client id from developer portal for this application
ext.iZettleSDK.clientId = ""

// OAuth redirect url scheme set on developer portal for this application
ext.iZettleSDK.redirectUrlScheme = ""

// OAuth redirect url scheme set on developer portal for this application
ext.iZettleSDK.redirectUrlHost = ""

```

## Step 2: Test the example app

Regardless of where you are located, developer mode lets you quickly test the SDK in certain scenarios without a Zettle merchant account and real transactions. For example, taking payments and making refunds for card transactions.

> **Note:** Developer Mode is not meant to be used or accessed in a production environment. This is only meant to be used while integrating the SDK. Make sure that you do not release your app with this option enabled.

Developer mode is disabled by default. Enable it to test the example app.

```Kotlin
IZettleSDK.init(
    ...,
    isDevMode = true
)
```

> **Tip:** You can use developer mode in the example app to test payment and refund responses for card and QRC, see [Try taking payments](https://developer.zettle.com/docs/get-started/user-guides/try-taking-payments).