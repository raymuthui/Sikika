package com.example.sikika;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay for a few seconds and then transition to the main activity
        new Handler().postDelayed(() -> {
            // Start the main activity
            Intent intent = new Intent(SplashActivity.this, SignInActivity.class);
            startActivity(intent);

            // Finish the splash activity
            finish();
        }, 3000); // Delay for 3 seconds (adjust as needed)
    }
}
