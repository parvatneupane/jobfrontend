package com.example.minijobhunt.views;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.minijobhunt.R;
import com.example.minijobhunt.fragments.ClientChatFragment;
import com.example.minijobhunt.fragments.ClientDashboardFragment;
import com.example.minijobhunt.fragments.ClientProfileFragment;
import com.example.minijobhunt.fragments.ClientSearchFragment;
import com.example.minijobhunt.fragments.ClientTaskPostingFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import android.util.Log;
import androidx.annotation.NonNull;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        requestNotificationPermission();
        deviceToken();

        // Default Fragment
        if (savedInstanceState == null) {
            loadFragment(new ClientDashboardFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new ClientDashboardFragment();

            } else if (id == R.id.nav_search) {
                selectedFragment = new ClientSearchFragment();

            } else if (id == R.id.nav_profile) {
                selectedFragment = new ClientProfileFragment();
            }
            else if (id == R.id.nav_chat) {
                selectedFragment = new ClientChatFragment();
            }
            else if (id == R.id.nav_postjob) {
                selectedFragment = new ClientTaskPostingFragment();
            }


            return loadFragment(selectedFragment);

        });
    }

    private boolean loadFragment(Fragment fragment) {

        if (fragment == null)
            return false;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        return true;
    }

    private void deviceToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        String deviceToken = task.getResult();
                        Log.w("FCM", "Device Token: " + deviceToken);
                        sendTokenToServer(deviceToken);
                    }
                });
    }

    private void sendTokenToServer(String token) {
        android.content.SharedPreferences sp = getSharedPreferences(Constants.cache, 0);
        String authToken = sp.getString("token", "");

        if (authToken.isEmpty()) return;

        Map<String, String> body = new HashMap<>();
        body.put("fcm_token", token);

        App.api.saveFcmToken("Bearer " + authToken, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("FCM", "Token saved to server");
                } else {
                    Log.e("FCM", "Failed to save token: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("FCM", "Error saving token", t);
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}