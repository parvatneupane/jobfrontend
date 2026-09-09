package com.example.minijobhunt.views;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.minijobhunt.R;
import com.example.minijobhunt.fragments.ClientChatFragment;
import com.example.minijobhunt.fragments.ClientDashboardFragment;
import com.example.minijobhunt.fragments.ClientProfileFragment;
import com.example.minijobhunt.fragments.ClientSearchFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import com.example.minijobhunt.fragments.ClientTaskPostingFragment;
import com.example.minijobhunt.NotificationFragment;
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


import androidx.fragment.app.FragmentManager;
import com.example.minijobhunt.fragments.ChatMessageFragment;
import com.example.minijobhunt.fragments.FreeLancerPortfolioFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TextView notificationBadge;
    private int notificationCount = 0;
    private View brandingLayout;
    private TextView toolbarTitle;
    private Toolbar toolbar;
    private boolean showBranding = true;

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadNotificationCount();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        brandingLayout = findViewById(R.id.branding_layout);
        toolbarTitle = findViewById(R.id.toolbar_title);

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                updateToolbarVisibility(f);
            }
        }, true);

        bottomNav = findViewById(R.id.bottomNav);

        requestNotificationPermission();
        deviceToken();
        loadNotificationCount();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final MenuItem menuItem = menu.findItem(R.id.action_notifications);
        View actionView = menuItem.getActionView();
        notificationBadge = actionView.findViewById(R.id.count_badge);

        setupBadge();

        actionView.setOnClickListener(v -> onOptionsItemSelected(menuItem));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        if (notificationItem != null) {
            notificationItem.setVisible(showBranding);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void updateToolbarVisibility(Fragment fragment) {
        showBranding = isMainFragment(fragment);
        
        // Handle full-screen fragments that have their own headers
        if (fragment instanceof ChatMessageFragment || 
            fragment instanceof FreeLancerPortfolioFragment ||
            fragment instanceof NotificationFragment) {
            toolbar.setVisibility(View.GONE);
        } else {
            toolbar.setVisibility(View.VISIBLE);
            if (brandingLayout != null) {
                brandingLayout.setVisibility(showBranding ? View.VISIBLE : View.GONE);
            }
            if (toolbarTitle != null) {
                toolbarTitle.setVisibility(showBranding ? View.GONE : View.VISIBLE);
                toolbarTitle.setText(getFragmentTitle(fragment));
            }
        }
        
        invalidateOptionsMenu();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(!showBranding);
        }
    }

    private String getFragmentTitle(Fragment fragment) {
        if (fragment instanceof NotificationFragment) return "Notifications";
        return "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private boolean isMainFragment(Fragment fragment) {
        return fragment instanceof ClientDashboardFragment ||
                fragment instanceof ClientSearchFragment ||
                fragment instanceof ClientChatFragment ||
                fragment instanceof ClientProfileFragment ||
                fragment instanceof ClientTaskPostingFragment;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotificationCount();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, new IntentFilter("com.example.minijobhunt.UPDATE_NOTIFICATION_COUNT"), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, new IntentFilter("com.example.minijobhunt.UPDATE_NOTIFICATION_COUNT"));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(notificationReceiver);
    }

    private void loadNotificationCount() {
        SharedPreferences sp = getSharedPreferences(Constants.cache, MODE_PRIVATE);
        notificationCount = sp.getInt("unread_notifications", 0);
        setupBadge();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_notifications) {
            // Clear unread count when opening notifications
            SharedPreferences sp = getSharedPreferences(Constants.cache, MODE_PRIVATE);
            sp.edit().putInt("unread_notifications", 0).apply();
            notificationCount = 0;
            setupBadge();
            loadFragment(new NotificationFragment());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBadge() {
        if (notificationBadge != null) {
            if (notificationCount == 0) {
                notificationBadge.setVisibility(View.GONE);
            } else {
                notificationBadge.setText(String.valueOf(Math.min(notificationCount, 99)));
                notificationBadge.setVisibility(View.VISIBLE);
            }
        }
    }

    // Call this method when a new notification is received (e.g., via BroadcastReceiver)
    public void updateNotificationCount(int count) {
        notificationCount = count;
        runOnUiThread(this::setupBadge);
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