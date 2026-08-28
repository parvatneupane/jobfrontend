package com.example.minijobhunt.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.minijobhunt.R;
import com.example.minijobhunt.fragments.FreeLancerPortfolioFragment;
import com.example.minijobhunt.controller.VerificationController;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreelancerSearchAdapter extends RecyclerView.Adapter<FreelancerSearchAdapter.ViewHolder> {

    private JSONArray freelancers;
    private FragmentActivity activity;
    private VerificationController verificationController;
    private Map<Integer, Boolean> verificationCache = new HashMap<>();

    public FreelancerSearchAdapter(JSONArray freelancers, FragmentActivity activity) {
        this.freelancers = freelancers;
        this.activity = activity;
        this.verificationController = new VerificationController();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_freelancer_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject profile = freelancers.getJSONObject(position);
            JSONObject user = profile.optJSONObject("user");

            if (user != null) {
                int userId = user.optInt("id", -1);
                holder.txtName.setText(user.optString("name", "Unknown"));
                
                // Verification Check
                if (verificationCache.containsKey(userId)) {
                    holder.imgVerifiedBadge.setVisibility(verificationCache.get(userId) ? View.VISIBLE : View.GONE);
                } else {
                    boolean isVerified = false;
                    if (user.has("verification")) {
                        JSONObject v = user.optJSONObject("verification");
                        if (v != null) isVerified = "approved".equalsIgnoreCase(v.optString("status", ""));
                    } else {
                        isVerified = "approved".equalsIgnoreCase(user.optString("verification_status", "")) ||
                                "approved".equalsIgnoreCase(user.optString("status", "")) ||
                                user.optBoolean("is_verified", false);
                    }

                    if (isVerified) {
                        verificationCache.put(userId, true);
                        holder.imgVerifiedBadge.setVisibility(View.VISIBLE);
                    } else {
                        holder.imgVerifiedBadge.setVisibility(View.GONE);
                        fetchVerificationStatus(userId);
                    }
                }

                String photo = user.optString("profile_photo", "");
                if (!photo.isEmpty()) {
                    String imageUrl = Constants.BASE_URL.replace("/api/", "") + "/storage/" + photo;
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .into(holder.imgProfile);
                } else {
                    holder.imgProfile.setImageResource(R.drawable.ic_profile);
                }
            }

            holder.txtTitle.setText(profile.optString("title", "No Title"));
            
            String rating = profile.optString("rating", "0.0");
            String completedJobs = profile.optString("completed_jobs", "0");
            
            holder.txtDetails.setText("⭐ " + rating + " • " + completedJobs + " Jobs");
            holder.txtSkills.setText("Skills: " + profile.optString("skills", "N/A"));

            holder.btnViewProfile.setOnClickListener(v -> {
                FreeLancerPortfolioFragment fragment = new FreeLancerPortfolioFragment();
                Bundle args = new Bundle();
                try {
                    args.putInt("profile_id", profile.optInt("id", -1));
                    args.putString("user_name", user != null ? user.getString("name") : "");
                } catch (Exception e) {}
                fragment.setArguments(args);

                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return freelancers != null ? freelancers.length() : 0;
    }

    private void fetchVerificationStatus(int userId) {
        if (userId == -1) return;
        
        android.content.SharedPreferences pref = activity.getSharedPreferences(Constants.cache, android.content.Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        verificationController.getVerification(token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    boolean isVerified = false;
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject data = root.optJSONObject("data");
                        if (data != null) {
                            isVerified = "approved".equalsIgnoreCase(data.optString("status", ""));
                        }
                    }
                    verificationCache.put(userId, isVerified);
                    if (isVerified) notifyDataSetChanged();
                } catch (Exception e) {
                    verificationCache.put(userId, false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                verificationCache.put(userId, false);
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile, imgVerifiedBadge;
        TextView txtName, txtTitle, txtDetails, txtSkills;
        Button btnViewProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            imgVerifiedBadge = itemView.findViewById(R.id.imgVerifiedBadge);
            txtName = itemView.findViewById(R.id.txtName);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDetails = itemView.findViewById(R.id.txtDetails);
            txtSkills = itemView.findViewById(R.id.txtSkills);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
        }
    }
}
