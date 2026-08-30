package com.example.minijobhunt.views;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minijobhunt.R;
import com.example.minijobhunt.utils.Constants;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkLoginStatus();
        }, SPLASH_DELAY);
    }

    private void checkLoginStatus() {
        SharedPreferences pref = getSharedPreferences(Constants.cache, MODE_PRIVATE);
        String token = pref.getString("token", "");
        long lastTime = pref.getLong("last_termination_time", 0);
        long currentTime = System.currentTimeMillis();

        // 2 minutes = 120,000 milliseconds
        boolean isWithinTwoMinutes = (currentTime - lastTime) <= 120000;

        if (!token.isEmpty() && isWithinTwoMinutes) {
            String role = pref.getString("role", "");
            Intent intent;
            if ("client".equalsIgnoreCase(role)) {
                intent = new Intent(this, MainActivity.class);
            } else if ("freelancer".equalsIgnoreCase(role)) {
                intent = new Intent(this, MainActivity2.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
        } else {
            // Clear token if more than 2 minutes passed or no token exists
            if (!isWithinTwoMinutes) {
                pref.edit().remove("token").apply();
            }
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
