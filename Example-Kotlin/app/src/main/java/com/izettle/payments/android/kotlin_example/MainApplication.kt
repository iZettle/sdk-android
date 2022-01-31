package com.izettle.payments.android.kotlin_example

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.izettle.payments.android.sdk.IZettleSDK
import com.izettle.payments.android.sdk.IZettleSDK.Instance.init
import com.izettle.payments.android.ui.SdkLifecycle

class MainApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        initIZettleSDK()
    }

    private fun initIZettleSDK() {
        val clientId = getString(R.string.client_id)
        val scheme = getString(R.string.redirect_url_scheme)
        val host = getString(R.string.redirect_url_host)
        val redirectUrl = "$scheme://$host"
        init(this, clientId, redirectUrl)
        ProcessLifecycleOwner.get().lifecycle.addObserver(SdkLifecycle(IZettleSDK))
    }
}