package com.zettle.payments.android.java_example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.snackbar.Snackbar;
import com.zettle.sdk.feature.cardreader.payment.TippingStyle;
import com.zettle.sdk.feature.cardreader.payment.TransactionReference;
import com.zettle.sdk.feature.cardreader.ui.CardReaderAction;
import com.zettle.sdk.feature.cardreader.ui.payment.CardPaymentResult;
import com.zettle.sdk.feature.cardreader.ui.readers.CardReadersActivity;
import com.zettle.sdk.features.ActionUtils;
import com.zettle.sdk.features.Transaction;
import com.zettle.sdk.ui.ZettleResult;
import com.zettle.sdk.ui.ZettleResultKt;

import java.util.Objects;
import java.util.UUID;

public class CardReaderActivity extends AppCompatActivity {

    private Button chargeButton;
    private Button refundButton;
    private Button retrieveButton;
    private EditText refundAmountEditText;
    private Button settingsButton;
    private EditText amountEditText;
    private CheckBox installmentsCheckBox;
    private MutableLiveData<String> lastPaymentTraceId;
    private Button tippingStyleButton;
    private TippingStyle tippingStyle = TippingStyle.None;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_reader);
        chargeButton = findViewById(R.id.charge_btn);
        refundButton = findViewById(R.id.refund_btn);
        retrieveButton = findViewById(R.id.retrieve_btn);
        settingsButton = findViewById(R.id.settings_btn);
        amountEditText = findViewById(R.id.amount_input);
        refundAmountEditText = findViewById(R.id.refund_amount_input);
        tippingStyleButton = findViewById(R.id.tipping_style_btn);
        installmentsCheckBox = findViewById(R.id.installments_check_box);
        lastPaymentTraceId = new MutableLiveData<>();

        lastPaymentTraceId.observe(this, value -> {
            refundButton.setEnabled(value != null);
            retrieveButton.setEnabled(value != null);
        });

        chargeButton.setOnClickListener(v -> onChargeClicked());
        refundButton.setOnClickListener(v -> onRefundClicked());
        retrieveButton.setOnClickListener(v -> onRetrieveClicked());
        settingsButton.setOnClickListener(v -> onSettingsClicked());
        tippingStyleButton.setOnClickListener(v -> onTippingStyleClicked());

        setTippingStyleTitle();
        getSupportFragmentManager().setFragmentResultListener(TippingStyleBottomSheet.REQUEST_KEY, this, (requestKey, result) -> {
            TippingStyle newTippingStyle = (TippingStyle) result.getSerializable(TippingStyleBottomSheet.TIPPING_STYLE_KEY);
            tippingStyle = newTippingStyle != null ? newTippingStyle : TippingStyle.None;
            setTippingStyleTitle();
        });
    }

    private final ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            ZettleResult parsed = ZettleResultKt.zettleResult(result.getData());
            if (parsed instanceof ZettleResult.Completed) {
                showSnackBar("Payment completed");
                CardPaymentResult.Completed casted = (CardPaymentResult.Completed) parsed;
                lastPaymentTraceId.setValue(Objects.requireNonNull(casted.getPayload().getReference()).getId());
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
        String amountEditTextContent = amountEditText.getText().toString();
        if (amountEditTextContent.isEmpty()) {
            return;
        }
        String internalTraceId = UUID.randomUUID().toString();
        long amount = Utils.parseLong(amountEditText.getText());
        boolean enableInstallments = installmentsCheckBox.isChecked();

        TransactionReference reference = new TransactionReference.Builder(internalTraceId)
                .put("PAYMENT_EXTRA_INFO", "Started from home screen")
                .build();

        CardReaderAction.Payment payment = new CardReaderAction.Payment(
                reference, amount, tippingStyle, enableInstallments
        );
        Intent intent = ActionUtils.charge(payment, this);
        paymentLauncher.launch(intent);
    }

    private void onSettingsClicked() {
        startActivity(CardReadersActivity.newIntent(this));
    }

    private void onTippingStyleClicked() {
        TippingStyleBottomSheet.newInstance().show(getSupportFragmentManager(), TippingStyleBottomSheet.class.getSimpleName());
    }

    private void setTippingStyleTitle() {
        String tippingStyleTitle = "Tipping Style - " + tippingStyle.name();
        tippingStyleButton.setText(tippingStyleTitle);
    }

    private void onRefundClicked() {
        String internalTraceId = lastPaymentTraceId.getValue();
        boolean isDevMode = ((MainApplication) getApplication()).isDevMode();

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken");
            return;
        }

        Long amount = Utils.parseLong(refundAmountEditText.getText());

        TransactionReference reference = new TransactionReference.Builder(UUID.randomUUID().toString())
                .put("REFUND_EXTRA_INFO", "Started from home screen")
                .build();

        CardReaderAction.Refund refund = new CardReaderAction.Refund(
                reference, amount, internalTraceId, null, null
        );
        refundLauncher.launch(ActionUtils.refund(refund,this));
    }

    private void onRetrieveClicked() {
        String internalTraceId = lastPaymentTraceId.getValue();
        boolean isDevMode = ((MainApplication) getApplication()).isDevMode();

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken");
            return;
        }

        Transaction transaction = new CardReaderAction.Transaction(internalTraceId);
        ActionUtils.retrieve(transaction, result -> {
            if (result instanceof ZettleResult.Completed) {
                showSnackBar("Retrieve payment completed");
            } else if (result instanceof ZettleResult.Failed) {
                showSnackBar("Retrieve failed " + ((ZettleResult.Failed) result).getReason());
            } else if (result instanceof ZettleResult.Cancelled) {
                showSnackBar("Retrieve canceled");
            }
            return null;
        });
    }

}