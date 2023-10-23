package com.zettle.payments.android.java_example;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.EditText;

import androidx.lifecycle.MutableLiveData;

import android.os.Bundle;

import static com.zettle.payments.android.java_example.Utils.parseLong;

import android.app.Activity;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;

import com.google.android.material.snackbar.Snackbar;
import com.zettle.sdk.feature.manualcardentry.ui.ManualCardEntryAction;
import com.zettle.sdk.feature.manualcardentry.ui.payments.ManualCardEntryPaymentResult;
import com.zettle.sdk.features.ActionUtils;
import com.zettle.sdk.features.Transaction;
import com.zettle.sdk.ui.ZettleResult;
import com.zettle.sdk.ui.ZettleResultKt;

import java.util.Objects;
import java.util.UUID;

public class ManualCardEntryActivity extends AppCompatActivity {

    private Button chargeButton;
    private Button settingsButton;
    private EditText amountEditText;
    private Button refundButton;
    private Button retrieveButton;
    private EditText refundAmountEditText;
    private MutableLiveData<String> lastPaymentTraceId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_card_entry);
        chargeButton = findViewById(R.id.charge_btn);
        settingsButton = findViewById(R.id.settings_btn);
        amountEditText = findViewById(R.id.amount_input);
        refundButton = findViewById(R.id.refund_btn);
        retrieveButton = findViewById(R.id.retrieve_btn);
        refundAmountEditText = findViewById(R.id.refund_amount_input);
        lastPaymentTraceId = new MutableLiveData<>();

        chargeButton.setOnClickListener(v -> onChargeClicked());
        settingsButton.setOnClickListener(v -> onSettingsClicked());
        refundButton.setOnClickListener(v -> onRefundLastPayment());
        retrieveButton.setOnClickListener(v -> onRetrieveLastPayment());
    }

    private final ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            ZettleResult parsed = ZettleResultKt.zettleResult(result.getData());
            if (parsed instanceof ZettleResult.Completed) {
                showSnackBar("Payment completed");
                ManualCardEntryPaymentResult.Completed casted = (ManualCardEntryPaymentResult.Completed) parsed;
                lastPaymentTraceId.setValue(Objects.requireNonNull(casted.getPayload().getReferenceId()));
                refundAmountEditText.setText(new SpannableStringBuilder()
                        .append(String.valueOf(casted.getPayload().getAmount())));
            } else if (parsed instanceof ZettleResult.Failed) {
                showSnackBar("Payment failed " + ((ZettleResult.Failed) parsed).getReason());
            } else if (parsed instanceof ZettleResult.Cancelled) {
                showSnackBar("Payment canceled");
            }
        }
    });

    private final ActivityResultLauncher<Intent> refundLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            ZettleResult parsed = ZettleResultKt.zettleResult(result.getData());
            if (parsed instanceof ZettleResult.Completed) {
                showSnackBar("Refund completed");
            } else if (parsed instanceof ZettleResult.Failed) {
                showSnackBar("Refund failed " + ((ZettleResult.Failed) parsed).getReason());
            } else if (parsed instanceof ZettleResult.Cancelled) {
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

        String uuid = UUID.randomUUID().toString();
        Intent intent;
        ManualCardEntryAction.Payment payment = new ManualCardEntryAction.Payment(amount, uuid);
        intent = ActionUtils.charge(payment, this);
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
        if (internalTraceId == null) {
            internalTraceId = "";
        }
        if (amount == null) {
            amount = ActionUtils.FULL_REFUND;
        }

        String uuid = UUID.randomUUID().toString();
        Intent intent;
        ManualCardEntryAction.Refund refund = new ManualCardEntryAction.Refund(amount, internalTraceId, uuid);
        intent = ActionUtils.refund(refund, this);
        refundLauncher.launch(intent);
    }

    private void onRetrieveLastPayment() {
        String internalTraceId = lastPaymentTraceId.getValue();
        boolean isDevMode = ((MainApplication) getApplication()).isDevMode();

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken");
            return;
        }
        if (internalTraceId == null) {
            internalTraceId = "";
        }

        Transaction transaction = new ManualCardEntryAction.Transaction(internalTraceId);

        ActionUtils.retrieve(transaction, result -> {
            if (result instanceof ZettleResult.Completed) {
                showSnackBar("Retrieve completed");
            } else if (result instanceof ZettleResult.Failed) {
                showSnackBar("Retrieve failed " + ((ZettleResult.Failed) result).getReason());
            } else if (result instanceof ZettleResult.Cancelled) {
                showSnackBar("Retrieve canceled");
            }
            return null;
        });
    }

    private void onSettingsClicked() {
        Intent intent;
        ManualCardEntryAction.Activation action = ManualCardEntryAction.Activation.INSTANCE;
        intent = ActionUtils.show(action, this);
        startActivity(intent);
    }
}

