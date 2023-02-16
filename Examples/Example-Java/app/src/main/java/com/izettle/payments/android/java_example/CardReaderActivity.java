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
import com.izettle.payments.android.payment.TippingStyle;
import com.izettle.payments.android.payment.TransactionReference;
import com.izettle.payments.android.payment.refunds.CardPaymentPayload;
import com.izettle.payments.android.payment.refunds.RefundsManager;
import com.izettle.payments.android.payment.refunds.RetrieveCardPaymentFailureReason;
import com.izettle.payments.android.sdk.IZettleSDK;
import com.izettle.payments.android.ui.payment.CardPaymentActivity;
import com.izettle.payments.android.ui.payment.CardPaymentResult;
import com.izettle.payments.android.ui.readers.CardReadersActivity;
import com.izettle.payments.android.ui.refunds.RefundResult;
import com.izettle.payments.android.ui.refunds.RefundsActivity;

import java.util.Objects;
import java.util.UUID;

public class CardReaderActivity extends AppCompatActivity {

    private Button chargeButton;
    private Button refundButton;
    private EditText refundAmountEditText;
    private Button settingsButton;
    private EditText amountEditText;
    private Button tippingStyleButton;
    private CheckBox installmentsCheckBox;
    private CheckBox loginCheckBox;
    private MutableLiveData<String> lastPaymentTraceId;
    private TippingStyle tippingStyle = TippingStyle.None;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_reader);
        chargeButton = findViewById(R.id.charge_btn);
        refundButton = findViewById(R.id.refund_btn);
        settingsButton = findViewById(R.id.settings_btn);
        amountEditText = findViewById(R.id.amount_input);
        refundAmountEditText = findViewById(R.id.refund_amount_input);
        tippingStyleButton = findViewById(R.id.tipping_style_btn);
        loginCheckBox = findViewById(R.id.login_check_box);
        installmentsCheckBox = findViewById(R.id.installments_check_box);
        lastPaymentTraceId = new MutableLiveData<>();

        lastPaymentTraceId.observe(this, value -> refundButton.setEnabled(value != null));

        chargeButton.setOnClickListener(v -> onChargeClicked());
        refundButton.setOnClickListener(v -> onRefundClicked());
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
            CardPaymentResult parsed = result.getData().getParcelableExtra(CardPaymentActivity.RESULT_EXTRA_PAYLOAD);
            if (parsed instanceof CardPaymentResult.Completed) {
                showSnackBar("Payment completed");
                CardPaymentResult.Completed casted = (CardPaymentResult.Completed) parsed;
                lastPaymentTraceId.setValue(Objects.requireNonNull(casted.getPayload().getReference()).getId());
                refundAmountEditText.setText(new SpannableStringBuilder()
                        .append(String.valueOf(casted.getPayload().getAmount())));
            } else if (parsed instanceof CardPaymentResult.Failed) {
                showSnackBar("Payment failed " + ((CardPaymentResult.Failed) parsed).getReason());
            } else if (parsed instanceof CardPaymentResult.Canceled) {
                showSnackBar("Payment canceled");
            }
        }
    });

    private final ActivityResultLauncher<Intent> refundLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            RefundResult parsed = result.getData().getParcelableExtra(RefundsActivity.RESULT_EXTRA_PAYLOAD);
            if (parsed instanceof RefundResult.Completed) {
                showSnackBar("Refund completed");
            } else if (parsed instanceof RefundResult.Failed) {
                showSnackBar("Refund failed " + ((RefundResult.Failed) parsed).getReason());
            } else if (parsed instanceof RefundResult.Canceled) {
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
        long amount = parseLong(amountEditText.getText());
        TippingStyle tippingStyle = this.tippingStyle;
        boolean enableInstallments = installmentsCheckBox.isChecked();
        boolean enableLogin = loginCheckBox.isChecked();
        TransactionReference reference = new TransactionReference.Builder(internalTraceId)
                .put("PAYMENT_EXTRA_INFO", "Started from home screen")
                .build();

        Intent intent = new CardPaymentActivity.IntentBuilder(this)
                .amount(amount)
                .reference(reference)
                .enableInstalments(enableInstallments)
                .enableTipping(tippingStyle)
                .enableLogin(enableLogin)
                .build();

        paymentLauncher.launch(intent);
    }

    private void onSettingsClicked() {
        startActivity(CardReadersActivity.newIntent(this));
    }

    private void onRefundClicked() {
        String internalTraceId = lastPaymentTraceId.getValue();
        boolean isDevMode = ((MainApplication) getApplication()).isDevMode();

        if (internalTraceId == null && !isDevMode) {
            showSnackBar("No payment taken");
            return;
        }

        Long amount = parseLong(refundAmountEditText.getText());
        IZettleSDK.Instance.getRefundsManager().retrieveCardPayment(
                (internalTraceId != null ? internalTraceId : ""),
                new RefundCallback(amount)
        );
    }

    private void onTippingStyleClicked() {
        TippingStyleBottomSheet.newInstance().show(getSupportFragmentManager(), TippingStyleBottomSheet.class.getSimpleName());
    }

    private void setTippingStyleTitle() {
        String tippingStyleTitle = "Tipping Style - " + tippingStyle.name();
        tippingStyleButton.setText(tippingStyleTitle);
    }

    private class RefundCallback implements RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {

        private final Long amount;

        public RefundCallback(Long amount) {
            this.amount = amount != null ? amount : 0L;
        }

        @Override
        public void onFailure(RetrieveCardPaymentFailureReason reason) {
            showSnackBar("Refund failed");
        }

        @Override
        public void onSuccess(CardPaymentPayload payload) {
            TransactionReference reference = new TransactionReference.Builder(UUID.randomUUID().toString())
                    .put("REFUND_EXTRA_INFO", "Started from home screen")
                    .build();
            Intent intent = new RefundsActivity.IntentBuilder(CardReaderActivity.this)
                    .cardPayment(payload)
                    .receiptNumber("#123456")
                    .taxAmount(amount)
                    .refundAmount(amount)
                    .reference(reference)
                    .build();

            refundLauncher.launch(intent);
        }
    }
}