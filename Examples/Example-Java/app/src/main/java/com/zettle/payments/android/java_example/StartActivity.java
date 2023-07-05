package com.zettle.payments.android.java_example;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class StartActivity extends AppCompatActivity {

    private SwitchCompat devMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        MainApplication app = (MainApplication) getApplication();
        if(app.isStarted()) {
            progress();
            return;
        }

        Button startButton = findViewById(R.id.start_btn);
        devMode = findViewById(R.id.dev_mode_switch);

        startButton.setOnClickListener(v -> {
            app.initZettleSDK(devMode.isChecked());
            progress();
        });
    }

    private void progress() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
