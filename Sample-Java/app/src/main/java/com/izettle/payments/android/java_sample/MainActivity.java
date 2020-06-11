package com.izettle.payments.android.java_sample;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.izettle.payments.android.core.StateObserver;
import com.izettle.payments.android.payment.TransactionReference;
import com.izettle.payments.android.payment.refunds.CardPaymentPayload;
import com.izettle.payments.android.payment.refunds.RefundsManager;
import com.izettle.payments.android.payment.refunds.RetrieveCardPaymentFailureReason;
import com.izettle.payments.android.sdk.IZettleSDK;
import com.izettle.payments.android.sdk.User;
import com.izettle.payments.android.ui.payment.CardPaymentActivity;
import com.izettle.payments.android.ui.payment.CardPaymentResult;
import com.izettle.payments.android.ui.readers.CardReadersActivity;
import com.izettle.payments.android.ui.refunds.RefundResult;
import com.izettle.payments.android.ui.refunds.RefundsActivity;

import java.util.UUID;

import static com.izettle.payments.android.architecturecomponents.HelpersKt.toLiveData;

public class MainActivity extends AppCompatActivity {

    private static int REQUEST_CODE_PAYMENT = 1001;
    private static int REQUEST_CODE_REFUND = 1002;

    private Button loginButton;
    private Button logoutButton;
    private Button chargeButton;
    private Button refundButton;
    private Button settingsButton;
    private EditText amountEditText;
    private CheckBox tippingCheckBox;
    private MutableLiveData<String> lastPaymentTraceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.login_btn);
        logoutButton = findViewById(R.id.logout_btn);
        chargeButton = findViewById(R.id.charge_btn);
        refundButton = findViewById(R.id.refund_btn);
        settingsButton = findViewById(R.id.settings_btn);
        amountEditText = findViewById(R.id.amount_input);
        tippingCheckBox = findViewById(R.id.tipping_check_box);
        lastPaymentTraceId = new MutableLiveData<>(null);

        toLiveData(IZettleSDK.Instance.getUser().getState()).observe(this, authState -> {
            onUserAuthStateChanged(authState instanceof User.AuthState.LoggedIn);
        });

        lastPaymentTraceId.observe(this, value -> {
            refundButton.setEnabled(value != null);
        });

        loginButton.setOnClickListener(view -> onLoginClicked());
        logoutButton.setOnClickListener(view -> onLogoutClicked());
        chargeButton.setOnClickListener(view -> onChargeClicked());
        refundButton.setOnClickListener(view -> onRefundClicked());
        settingsButton.setOnClickListener(view -> onSettingsClicked());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PAYMENT && data != null) {
            CardPaymentResult result = data.getParcelableExtra(CardPaymentActivity.RESULT_EXTRA_PAYLOAD);
            if (result instanceof CardPaymentResult.Completed) {
                Toast.makeText(this, "Payment completed", Toast.LENGTH_SHORT).show();
            } else if (result instanceof CardPaymentResult.Canceled) {
                Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
            } else if (result instanceof CardPaymentResult.Failed) {
                Toast.makeText(this, "Payment failed ", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_CODE_REFUND && data != null) {
            RefundResult result = data.getParcelableExtra(RefundsActivity.RESULT_EXTRA_PAYLOAD);
            if (result instanceof RefundResult.Completed) {
                Toast.makeText(this, "Refund completed", Toast.LENGTH_SHORT).show();
            } else if (result instanceof RefundResult.Canceled) {
                Toast.makeText(this, "Refund canceled", Toast.LENGTH_SHORT).show();
            } else if (result instanceof RefundResult.Failed) {
                Toast.makeText(this, "Refund failed ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onUserAuthStateChanged(boolean isLoggedIn) {
        loginButton.setEnabled(!isLoggedIn);
        logoutButton.setEnabled(isLoggedIn);
        chargeButton.setEnabled(isLoggedIn);
        refundButton.setEnabled(isLoggedIn && lastPaymentTraceId.getValue() != null);
        settingsButton.setEnabled(isLoggedIn);
        amountEditText.setEnabled(isLoggedIn);
        tippingCheckBox.setEnabled(isLoggedIn);
    }

    private void onLoginClicked() {
        int toolbarColor = ResourcesCompat.getColor(getResources(), R.color.white, getTheme());
        IZettleSDK.Instance.getUser().login(this, toolbarColor);
    }

    private void onLogoutClicked() {
        IZettleSDK.Instance.getUser().logout();
    }

    private void onChargeClicked() {
        String amountEditTextContent = amountEditText.getText().toString();
        if (amountEditTextContent.equals("")) {
            return;
        }

        String internalTraceId = UUID.randomUUID().toString();
        long amount = Long.parseLong(amountEditTextContent);
        boolean enableTipping = tippingCheckBox.isChecked();

        TransactionReference reference = new TransactionReference.Builder(internalTraceId)
                .put("PAYMENT_EXTRA_INFO", "Started from home screen")
                .build();

        Intent intent = new CardPaymentActivity.IntentBuilder(this)
                .amount(amount)
                .reference(reference)
                .enableTipping(enableTipping)
                .build();

        startActivityForResult(intent, REQUEST_CODE_PAYMENT);
        lastPaymentTraceId.setValue(internalTraceId);
    }

    private void onSettingsClicked() {
        startActivity(CardReadersActivity.newIntent(this));
    }

    private void onRefundClicked() {
        String internalTraceId = lastPaymentTraceId.getValue();
        if (internalTraceId == null) {
            return;
        }

        IZettleSDK.Instance.getRefundsManager().retrieveCardPayment(internalTraceId, new RefundCallback());
    }

    private class RefundCallback implements RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {

        @Override
        public void onFailure(RetrieveCardPaymentFailureReason reason) {
            Toast.makeText(MainActivity.this, "Refund failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSuccess(CardPaymentPayload payload) {
            TransactionReference reference = new TransactionReference.Builder(payload.getReferenceId())
                    .put("REFUND_EXTRA_INFO", "Started from home screen")
                    .build();

            Intent intent = new RefundsActivity.IntentBuilder(MainActivity.this)
                    .cardPayment(payload)
                    .receiptNumber("#123456")
                    .taxAmount(payload.getAmount() / 10)
                    .reference(reference)
                    .build();

            startActivityForResult(intent, REQUEST_CODE_REFUND);
        }
    }
}
