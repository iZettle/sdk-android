package com.zettle.payments.android.kotlin_example

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.zettle.sdk.feature.qrc.paypal.PayPalQrcFeature
import com.zettle.sdk.feature.qrc.venmo.VenmoQrcFeature
import com.zettle.sdk.feature.cardreader.ui.CardReaderFeature
import com.zettle.sdk.ZettleSDK
import com.zettle.sdk.ZettleSDKLifecycle
import com.zettle.sdk.config
import com.zettle.sdk.feature.manualcardentry.ui.ManualCardEntryFeature

class MainApplication : MultiDexApplication() {

    var started: Boolean = false
        private set

    var isDevMode: Boolean = false
        private set

    fun initZettleSDK(devMode: Boolean) {
        if(started) return
        started = true
        isDevMode = devMode

        val clientId = getString(R.string.client_id)
        val scheme = getString(R.string.redirect_url_scheme)
        val host = getString(R.string.redirect_url_host)
        val redirectUrl = "$scheme://$host"

        val config = config(applicationContext) {
            isDevMode = devMode
            auth {
                this.clientId = clientId
                this.redirectUrl = redirectUrl
            }
            logging {
                allowWhileRoaming = false
            }
            addFeature(CardReaderFeature)
            addFeature(PayPalQrcFeature)
            addFeature(VenmoQrcFeature)
            addFeature(ManualCardEntryFeature)
        }
        ZettleSDK.configure(config)
        //ZettleSDK.start()
        ProcessLifecycleOwner.get().lifecycle.addObserver(ZettleSDKLifecycle())
    }
}