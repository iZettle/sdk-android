package com.izettle.payments.android.java_sample;

import android.text.Editable;

public class Utils {

    public static Long parseLong(Editable editable) {
        Long amount = null;
        try {
            amount = Long.parseLong(editable.toString());
        }
        catch (NumberFormatException ignore) {}
        return amount;
    }
}
