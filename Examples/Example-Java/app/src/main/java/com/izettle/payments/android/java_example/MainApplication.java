package com.izettle.payments.android.java_example;

import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.izettle.payments.android.sdk.IZettleSDK;
import com.izettle.payments.android.ui.SdkLifecycle;

public class MainApplication extends MultiDexApplication {

    private boolean started = false;
    private boolean isDevMode = false;

    public boolean isStarted() {
        return started;
    }

    public boolean isDevMode() {
        return isDevMode;
    }

    public void initIZettleSDK(boolean devMode) {
        if (started) return;
        started = true;
        isDevMode = devMode;

        String clientId = getString(R.string.client_id);
        String scheme = getString(R.string.redirect_url_scheme);
        String host = getString(R.string.redirect_url_host);
        String redirectUrl = scheme + "://" + host;
        IZettleSDK.Instance.init(this, clientId, redirectUrl, isDevMode);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new SdkLifecycle(IZettleSDK.Instance));
    }

}
