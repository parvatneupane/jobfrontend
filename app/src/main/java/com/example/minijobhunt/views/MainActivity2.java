package com.example.minijobhunt.views;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.minijobhunt.R;
import com.example.minijobhunt.fragments.FreelancerChatFragment;
import com.example.minijobhunt.fragments.FreelancerDashboardFragment;
import com.example.minijobhunt.fragments.FreelancerJobsFragment;
import com.example.minijobhunt.fragments.FreelancerProfileFragment;
import com.example.minijobhunt.fragments.FreelancerSearchFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

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


public class MainActivity2 extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main2);

        bottomNav = findViewById(R.id.bottomNav);

        requestNotificationPermission();
        deviceToken();
        syncProfileData();

        // Default Fragment
        if (savedInstanceState == null) {
            loadFragment(new FreelancerDashboardFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new FreelancerDashboardFragment();

            } else if (id == R.id.nav_search) {
                selectedFragment = new FreelancerSearchFragment();

            } else if (id == R.id.nav_jobs) {
                selectedFragment = new FreelancerJobsFragment();

            } else if (id == R.id.nav_chat) {
                selectedFragment = new FreelancerChatFragment();

            } else if (id == R.id.nav_profile) {
                selectedFragment = new FreelancerProfileFragment();
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

    private void syncProfileData() {
        android.content.SharedPreferences sp = getSharedPreferences(Constants.cache, 0);
        String token = sp.getString("token", "");
        if (token.isEmpty()) return;

        App.api.getMyProfile("Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject profile = root.getJSONObject("data");
                        
                        android.content.SharedPreferences.Editor editor = sp.edit();
                        editor.putInt("profile_id", profile.optInt("id"));
                        editor.putString("profile_title", profile.optString("title"));
                        editor.putString("profile_bio", profile.optString("bio"));
                        editor.putString("profile_skills", profile.optString("skills"));
                        editor.putString("profile_location", profile.optString("location"));
                        editor.putString("profile_portfolio", profile.optString("portfolio_url"));
                        editor.putString("profile_availability", profile.optString("availability"));
                        editor.putInt("profile_experience", profile.optInt("experience_years"));
                        editor.putString("profile_rate", profile.optString("hourly_rate"));
                        editor.apply();
                        
                        Log.d("FCM", "Profile synced successfully. ID: " + profile.optInt("id"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("FCM", "Profile sync failed", t);
            }
        });
    }
}