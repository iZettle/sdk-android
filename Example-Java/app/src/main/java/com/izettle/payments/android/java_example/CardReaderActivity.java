package com.izettle.payments.android.java_example;

import static com.izettle.payments.android.java_example.Utils.parseLong;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

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

import java.util.UUID;

public class CardReaderActivity extends AppCompatActivity {

    private Button chargeButton;
    private Button refundButton;
    private Button settingsButton;
    private EditText amountEditText;
    private CheckBox tippingCheckBox;
    private CheckBox installmentsCheckBox;
    private CheckBox loginCheckBox;
    private MutableLiveData<String> lastPaymentTraceId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_reader);
        chargeButton = findViewById(R.id.charge_btn);
        refundButton = findViewById(R.id.refund_btn);
        settingsButton = findViewById(R.id.settings_btn);
        amountEditText = findViewById(R.id.amount_input);
        tippingCheckBox = findViewById(R.id.tipping_check_box);
        loginCheckBox = findViewById(R.id.login_check_box);
        installmentsCheckBox = findViewById(R.id.installments_check_box);
        lastPaymentTraceId = new MutableLiveData<>();

        lastPaymentTraceId.observe(this, value -> {
            refundButton.setEnabled(value != null);
        });

        chargeButton.setOnClickListener( v -> { onChargeClicked(); });
        refundButton.setOnClickListener( v -> { onRefundClicked(); });
        settingsButton.setOnClickListener( v -> { onSettingsClicked(); });
    }

    private final ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            CardPaymentResult parsed = result.getData().getParcelableExtra(CardPaymentActivity.RESULT_EXTRA_PAYLOAD);
            if(parsed instanceof CardPaymentResult.Completed) {
                showToast("Payment completed");
                CardPaymentResult.Completed casted = (CardPaymentResult.Completed) parsed;
                lastPaymentTraceId.setValue(casted.getPayload().getReference().getId());
            }
            else if(parsed instanceof CardPaymentResult.Failed) {
                showToast("Payment failed "+ ((CardPaymentResult.Failed) parsed).getReason().toString());
            }
            else if(parsed instanceof CardPaymentResult.Canceled) {
                showToast("Payment canceled");
            }
        }
    });

    private final ActivityResultLauncher<Intent> refundLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            RefundResult parsed = result.getData().getParcelableExtra(RefundsActivity.RESULT_EXTRA_PAYLOAD);
            if(parsed instanceof RefundResult.Completed) {
                showToast("Refund completed");
            }
            else if(parsed instanceof RefundResult.Failed) {
                showToast("Refund failed "+ ((RefundResult.Failed) parsed).getReason().toString());
            }
            else if(parsed instanceof RefundResult.Canceled) {
                showToast("Refund canceled");
            }
        }
    });

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void onChargeClicked() {
        String amountEditTextContent = amountEditText.getText().toString();
        if (amountEditTextContent.isEmpty()) {
            return;
        }
        String internalTraceId = UUID.randomUUID().toString();
        long amount = parseLong(amountEditText.getText());
        boolean enableTipping = tippingCheckBox.isChecked();
        boolean enableInstallments = installmentsCheckBox.isChecked();
        boolean enableLogin = loginCheckBox.isChecked();
        TransactionReference reference = new TransactionReference.Builder(internalTraceId)
            .put("PAYMENT_EXTRA_INFO", "Started from home screen")
            .build();

        Intent intent = new CardPaymentActivity.IntentBuilder(this)
            .amount(amount)
            .reference(reference)
            .enableInstalments(enableInstallments)
            .enableTipping(enableTipping)
            .enableLogin(enableLogin)
            .build();

        paymentLauncher.launch(intent);
    }

    private void onSettingsClicked() {
        startActivity(CardReadersActivity.newIntent(this));
    }

    private void onRefundClicked() {
        String id = lastPaymentTraceId.getValue();
        if(id != null) {
            IZettleSDK.Instance.getRefundsManager().retrieveCardPayment(id, new RefundCallback());
        }
    }

    private class RefundCallback implements RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {

        @Override
        public void onFailure(RetrieveCardPaymentFailureReason reason) {
            showToast("Refund failed");
        }

        @Override
        public void onSuccess(CardPaymentPayload payload) {
            TransactionReference reference = new TransactionReference.Builder(payload.getReferenceId())
                .put("REFUND_EXTRA_INFO", "Started from home screen")
                .build();
            Intent intent = new RefundsActivity.IntentBuilder(CardReaderActivity.this)
                .cardPayment(payload)
                .receiptNumber("#123456")
                .taxAmount(payload.getAmount() / 10)
                .reference(reference)
                .build();

            refundLauncher.launch(intent);
        }
    }
}