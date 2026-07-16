package com.example.minijobhunt.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minijobhunt.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private JSONArray reviews;
    private boolean showAll = false;

    public ReviewAdapter(JSONArray reviews) {
        this.reviews = reviews;
    }

    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject review = reviews.getJSONObject(position);
            JSONObject client = review.optJSONObject("client");
            
            if (client != null) {
                String name = client.optString("name", "Unknown");
                holder.txtClientName.setText(name);
                if (!name.isEmpty()) {
                    holder.txtClientAvatar.setText(String.valueOf(name.charAt(0)));
                }
            }

            holder.ratingBar.setRating((float) review.optDouble("rating", 0));
            holder.txtReview.setText(review.optString("review", ""));
            
            // Format date if available, otherwise hide or set default
            String date = review.optString("created_at", "");
            if (date.length() >= 10) {
                holder.txtDate.setText(date.substring(0, 10));
            } else {
                holder.txtDate.setText("");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        if (reviews == null) return 0;
        if (showAll || reviews.length() <= 1) {
            return reviews.length();
        } else {
            return 1;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtClientAvatar, txtClientName, txtDate, txtReview;
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtClientAvatar = itemView.findViewById(R.id.txtClientAvatar);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtReview = itemView.findViewById(R.id.txtReview);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
