package com.example.minijobhunt.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UtilsFunctions {

    public static String getTimeAgo(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) return "";
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(dateString);
            
            long time = date.getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;
            
            if (diff < 60000) return "Just now";
            if (diff < 3600000) return (diff / 60000) + "m ago";
            if (diff < 86400000) return (diff / 3600000) + "h ago";
            if (diff < 604800000) return (diff / 86400000) + "d ago";
            
            SimpleDateFormat outFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return outFormat.format(date);
            
        } catch (Exception e) {
            // Try secondary format (YYYY-MM-DD HH:MM:SS)
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(dateString);
                long diff = System.currentTimeMillis() - date.getTime();
                if (diff < 60000) return "Just now";
                if (diff < 3600000) return (diff / 60000) + "m ago";
                if (diff < 86400000) return (diff / 3600000) + "h ago";
                return (diff / 86400000) + "d ago";
            } catch (Exception e2) {
                return dateString.split(" ")[0]; // Return just the date part as fallback
            }
        }
    }

    public static File uriToFile(Context context, Uri uri) {

        try {

            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            File file = new File(context.getCacheDir(),
                    "upload_" + System.currentTimeMillis() + ".jpg");

            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int len;

            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            outputStream.close();
            inputStream.close();

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}