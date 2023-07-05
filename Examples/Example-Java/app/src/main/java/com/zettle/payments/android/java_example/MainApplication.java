package com.zettle.payments.android.java_example;

import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.zettle.sdk.AuthConfig;
import com.zettle.sdk.Configuration;
import com.zettle.sdk.LogConfig;
import com.zettle.sdk.ZettleSDK;
import com.zettle.sdk.ZettleSDKLifecycle;
import com.zettle.sdk.feature.cardreader.ui.CardReaderFeature;
import com.zettle.sdk.feature.qrc.paypal.PayPalQrcFeature;
import com.zettle.sdk.feature.qrc.venmo.VenmoQrcFeature;

public class MainApplication extends MultiDexApplication {

    private boolean started = false;
    private boolean isDevMode = false;

    public boolean isStarted() {
        return started;
    }

    public boolean isDevMode() {
        return isDevMode;
    }

    public void initZettleSDK(boolean devMode) {
        if (started) return;
        started = true;
        isDevMode = devMode;

        String clientId = getString(R.string.client_id);
        String scheme = getString(R.string.redirect_url_scheme);
        String host = getString(R.string.redirect_url_host);
        String redirectUrl = scheme + "://" + host;

        AuthConfig auth = new AuthConfig();
        auth.setClientId(clientId);
        auth.setRedirectUrl(redirectUrl);

        LogConfig logging = new LogConfig();
        logging.setAllowWhileRoaming(false);

        Configuration config = Configuration.build(getApplicationContext(), auth, logging);
        config.setDevMode(devMode);
        config.addFeature(CardReaderFeature.Configuration);
        config.addFeature(PayPalQrcFeature.Configuration);
        config.addFeature(VenmoQrcFeature.Configuration);

        ZettleSDK.configure(config);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new ZettleSDKLifecycle());
    }

}
