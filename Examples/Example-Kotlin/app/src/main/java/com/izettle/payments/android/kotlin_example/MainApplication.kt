package com.izettle.payments.android.kotlin_example

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.izettle.payments.android.sdk.IZettleSDK
import com.izettle.payments.android.ui.SdkLifecycle

class MainApplication : MultiDexApplication() {

    var started: Boolean = false
        private set

    var isDevMode: Boolean = false
        private set

    fun initIZettleSDK(devMode: Boolean) {
        if(started) return
        started = true
        isDevMode = devMode

        val clientId = getString(R.string.client_id)
        val scheme = getString(R.string.redirect_url_scheme)
        val host = getString(R.string.redirect_url_host)
        val redirectUrl = "$scheme://$host"
        IZettleSDK.init(
            context = this,
            clientId = clientId,
            redirectUrl = redirectUrl,
            isDevMode = isDevMode
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(SdkLifecycle(IZettleSDK))
    }
}