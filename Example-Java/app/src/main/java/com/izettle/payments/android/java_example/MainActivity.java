package com.izettle.payments.android.java_example;

import static com.izettle.android.commons.ext.state.StateExtKt.toLiveData;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.izettle.payments.android.sdk.IZettleSDK;
import com.izettle.payments.android.sdk.User;

public class MainActivity extends AppCompatActivity {

    private TextView loginStateText;
    private Button loginButton;
    private Button logoutButton;
    private Button openCardReaderButton;
    private Button openPayPalQrcButton;

    @Override
    public void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginStateText = findViewById(R.id.login_state);
        loginButton = findViewById(R.id.login_btn);
        logoutButton = findViewById(R.id.logout_btn);
        openCardReaderButton = findViewById(R.id.open_card_reader_btn);
        openPayPalQrcButton = findViewById(R.id.open_paypal_btn);

        toLiveData(IZettleSDK.Instance.getUser().getState()).observe(this, state -> {
            onAuthStateChanged(state instanceof User.AuthState.LoggedIn);
        });

        loginButton.setOnClickListener( v -> {
            IZettleSDK.Instance.getUser().login(this, Color.WHITE);
        });

        logoutButton.setOnClickListener( v -> {
            IZettleSDK.Instance.getUser().logout();
        });

        openCardReaderButton.setOnClickListener( v -> {
            Intent intent = new Intent(this, CardReaderActivity.class);
            startActivity(intent);
        });

        openPayPalQrcButton.setOnClickListener( v -> {
            Intent intent = new Intent(this, PayPalQrcActivity.class);
            startActivity(intent);
        });
    }

    @SuppressLint("SetTextI18n")
    private void onAuthStateChanged(boolean isLoggedIn) {
        String state = "Unauthenticated";
        if(isLoggedIn) {
            state = "Authenticated";
        }
        loginStateText.setText("State: "+ state);
        loginButton.setEnabled(!isLoggedIn);
        logoutButton.setEnabled(isLoggedIn);
    }
}