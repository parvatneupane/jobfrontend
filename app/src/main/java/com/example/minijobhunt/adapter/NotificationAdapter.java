package com.example.minijobhunt.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minijobhunt.R;

import org.json.JSONObject;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<JSONObject> notifications;

    public NotificationAdapter(List<JSONObject> notifications) {
        this.notifications = notifications;
    }

    public void removeItem(int position) {
        notifications.remove(position);
        notifyItemRemoved(position);
    }

    public JSONObject getItem(int position) {
        return notifications.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject notification = notifications.get(position);
            holder.txtTitle.setText(notification.optString("title"));
            holder.txtMessage.setText(notification.optString("message"));
            
            String createdAt = notification.optString("created_at");
            if (createdAt.contains("T")) {
                createdAt = createdAt.split("T")[0];
            }
            holder.txtTime.setText(createdAt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtMessage, txtTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}