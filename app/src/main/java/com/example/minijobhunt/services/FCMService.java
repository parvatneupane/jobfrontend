package com.example.minijobhunt.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.minijobhunt.R;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.views.MainActivity;
import com.example.minijobhunt.views.MainActivity2;
import com.example.minijobhunt.views.SplashScreenActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "mini_job_hunt_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Message Notification Title: " + title);
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            if (title == null || title.isEmpty()) {
                title = remoteMessage.getData().get("title");
            }
            if (body == null || body.isEmpty()) {
                // Check multiple possible keys for body
                body = remoteMessage.getData().get("body");
                if (body == null || body.isEmpty()) {
                    body = remoteMessage.getData().get("message");
                }
                if (body == null || body.isEmpty()) {
                    body = remoteMessage.getData().get("content");
                }
            }
        }

        if (title != null && !title.isEmpty()) {
            showNotification(title, body);

            // Increment unread count in SharedPreferences
            SharedPreferences sp = getSharedPreferences(Constants.cache, MODE_PRIVATE);
            int count = sp.getInt("unread_notifications", 0);
            sp.edit().putInt("unread_notifications", count + 1).apply();
            
            // Send broadcast to update UI if activity is running
            Intent broadcast = new Intent("com.example.minijobhunt.UPDATE_NOTIFICATION_COUNT");
            sendBroadcast(broadcast);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save token to SharedPreferences
        SharedPreferences sp = getSharedPreferences(Constants.cache, MODE_PRIVATE);
        sp.edit().putString("fcm_token", token).apply();
        
        // Try to send to server if user is logged in
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        SharedPreferences sp = getSharedPreferences(Constants.cache, MODE_PRIVATE);
        String authToken = sp.getString("token", "");

        if (authToken.isEmpty()) return;

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("fcm_token", token);

        com.example.minijobhunt.utils.App.api.saveFcmToken("Bearer " + authToken, body)
                .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Token saved to server successfully");
                } else {
                    Log.e(TAG, "Failed to save token to server: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                Log.e(TAG, "Error saving token to server", t);
            }
        });
    }

    private void showNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "MiniJobHunt Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for job updates and messages");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Determine which activity to open based on user role
        SharedPreferences sp = getSharedPreferences(Constants.cache, MODE_PRIVATE);
        String role = sp.getString("role", "");

        Intent intent;
        if ("client".equalsIgnoreCase(role)) {
            intent = new Intent(this, MainActivity.class);
        } else if ("freelancer".equalsIgnoreCase(role)) {
            intent = new Intent(this, MainActivity2.class);
        } else {
            intent = new Intent(this, SplashScreenActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
