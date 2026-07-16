package com.example.minijobhunt.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.minijobhunt.R;

public class NotificationUtils {

    private static final String CHANNEL_ID = "pandit_channel";
    private static final String CHANNEL_NAME = "Pandit Notifications";
    private static final String CHANNEL_DESC = "Notifications for Pandit app";


    public static void showNotification(Context context, String title, String body) {

        // -------------------- CREATE NOTIFICATION CHANNEL --------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // -------------------- BUILD THE NOTIFICATION --------------------
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.header_background) // You can use your app icon here
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // removes notification when clicked

        // -------------------- SHOW THE NOTIFICATION --------------------
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Use unique id for each notification or a static one if only one notification is enough
        int notificationId = (int) System.currentTimeMillis();
       // notificationManager.notify(notificationId, builder.build());
    }
}
