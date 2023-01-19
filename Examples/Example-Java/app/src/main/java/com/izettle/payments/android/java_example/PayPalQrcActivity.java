package com.izettle.payments.android.java_example;

import static com.izettle.payments.android.java_example.Utils.parseLong;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.snackbar.Snackbar;
import com.izettle.android.qrc.paypal.ui.PayPalQrcType;
import com.izettle.android.qrc.paypal.ui.activation.PayPalQrcActivationActivity;
import com.izettle.android.qrc.paypal.ui.payment.PayPalQrcPaymentActivity;
import com.izettle.android.qrc.paypal.ui.payment.PayPalQrcPaymentResult;
import com.izettle.android.qrc.paypal.ui.refund.PayPalQrcRefundActivity;
import com.izettle.android.qrc.paypal.ui.refund.PayPalQrcRefundResult;

import java.util.UUID;

public class PayPalQrcActivity extends AppCompatActivity {

    private Button chargeButton;
    private Button settingsButton;
    private EditText amountEditText;
    private CheckBox venmoCheckBox;
    private Button refundButton;
    private EditText refundAmountEditText;
    private MutableLiveData<String> lastPaymentTraceId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paypal_qrc);
        chargeButton = findViewById(R.id.charge_btn);
        settingsButton = findViewById(R.id.settings_btn);
        amountEditText = findViewById(R.id.amount_input);
        venmoCheckBox = findViewById(R.id.venmo_check_box);
        refundButton = findViewById(R.id.refund_btn);
        refundAmountEditText = findViewById(R.id.refund_amount_input);
        lastPaymentTraceId = new MutableLiveData<>();

        chargeButton.setOnClickListener(v -> onChargeClicked());
        settingsButton.setOnClickListener(v -> onSettingsClicked());
        refundButton.setOnClickListener(v -> onRefundLastPayment());
    }

    private final ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            PayPalQrcPaymentResult parsed = PayPalQrcPaymentActivity.Companion.fromIntent(result.getData());
            if (parsed instanceof PayPalQrcPaymentResult.Completed) {
                showSnackBar("Payment completed");
                PayPalQrcPaymentResult.Completed casted = (PayPalQrcPaymentResult.Completed) parsed;
                lastPaymentTraceId.setValue(casted.getPayment().getReference());
                refundAmountEditText.setText(new SpannableStringBuilder()
                        .append(String.valueOf(casted.getPayment().getAmount())));
            } else if (parsed instanceof PayPalQrcPaymentResult.Failed) {
                showSnackBar("Payment failed " + ((PayPalQrcPaymentResult.Failed) parsed).getReason());
            } else if (parsed instanceof PayPalQrcPaymentResult.Canceled) {
                showSnackBar("Payment canceled");
            }
        }
    });

    private final ActivityResultLauncher<Intent> refundLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            PayPalQrcRefundResult parsed = PayPalQrcRefundActivity.fromIntent(result.getData());
            if (parsed instanceof PayPalQrcRefundResult.Completed) {
                showSnackBar("Refund completed");
            } else if (parsed instanceof PayPalQrcRefundResult.Failed) {
                showSnackBar("Refund failed " + ((PayPalQrcRefundResult.Failed) parsed).getReason());
            } else if (parsed instanceof PayPalQrcRefundResult.Canceled) {
                showSnackBar("Refund canceled");
            }
        }
    });

    private void showSnackBar(String text) {
        ViewGroup viewGroup = findViewById(android.R.id.content);
        Snackbar.make(viewGroup.getChildAt(0), text, Snackbar.LENGTH_LONG).show();
    }

    private void onChargeClicked() {
        Long amount = parseLong(amountEditText.getText());
        if (amount == null) {
            showSnackBar("Invalid amount");
            return;
        }

        PayPalQrcType appearance = PayPalQrcType.PayPal;
        if (venmoCheckBox.isChecked()) {
            appearance = PayPalQrcType.Venmo;
        }

        UUID uuid = UUID.randomUUID();
        Intent intent = new PayPalQrcPaymentActivity.IntentBuilder(this)
                .appearance(appearance)
                .amount(amount)
                .reference(uuid.toString())
                .build();

        paymentLauncher.launch(intent);
    }

    private void onRefundLastPayment() {
        Long amount = parseLong(refundAmountEditText.getText());
        String internalTraceId = lastPaymentTraceId.getValue();
        boolean isDevMode = ((MainApplication) getApplication()).isDevMode();

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken");
            return;
        }

        PayPalQrcRefundActivity.IntentBuilder builder = new PayPalQrcRefundActivity.IntentBuilder(this)
                .paymentReference(internalTraceId != null ? internalTraceId : "")
                .reference(UUID.randomUUID().toString());

        if (amount != null) {
            builder = builder.amount(amount);
        }

        refundLauncher.launch(builder.build());
    }

    private void onSettingsClicked() {
        PayPalQrcType appearance = PayPalQrcType.PayPal;
        if (venmoCheckBox.isChecked()) {
            appearance = PayPalQrcType.Venmo;
        }

        Intent intent = new PayPalQrcActivationActivity.IntentBuilder(this)
                .appearance(appearance)
                .build();

        startActivity(intent);
    }
}