package com.example.minijobhunt.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.VerificationController;
import com.example.minijobhunt.databinding.ItemFreelancerJobBinding;
import com.example.minijobhunt.fragments.FreeLancerApplyJobProposalFragment;
import com.example.minijobhunt.fragments.FreeLancerProfileEditFragment;
import com.example.minijobhunt.fragments.TaskDetailsBottomSheet;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreelancerJobAdapter extends RecyclerView.Adapter<FreelancerJobAdapter.JobViewHolder> {

    private List<JSONObject> jobList;
    private FragmentActivity activity;
    private VerificationController verificationController;
    private Map<Integer, Boolean> userVerificationCache = new HashMap<>();
    private Map<Integer, String> appliedJobsCache;

    public FreelancerJobAdapter(List<JSONObject> jobList, FragmentActivity activity, Map<Integer, String> appliedJobsCache) {
        this.jobList = jobList;
        this.activity = activity;
        this.appliedJobsCache = appliedJobsCache;
        this.verificationController = new VerificationController();
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFreelancerJobBinding itemBinding = ItemFreelancerJobBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        JSONObject job = jobList.get(position);
        try {
            // Bind Client Info
            if (job.has("user")) {
                JSONObject user = job.getJSONObject("user");
                int userId = user.getInt("id");
                holder.itemBinding.txtClientName.setText(user.optString("name", "Unknown Client"));

                // Show verified badge if user is verified
                if (userVerificationCache.containsKey(userId)) {
                    holder.itemBinding.imgVerifiedBadge.setVisibility(userVerificationCache.get(userId) ? View.VISIBLE : View.GONE);
                } else {
                    boolean isVerified = "approved".equalsIgnoreCase(user.optString("verification_status", "")) ||
                            "approved".equalsIgnoreCase(user.optString("status", "")) ||
                            user.optBoolean("is_verified", false) ||
                            user.optInt("is_verified", 0) == 1;

                    if (!isVerified && user.has("verification")) {
                        JSONObject verification = user.optJSONObject("verification");
                        if (verification != null) {
                            isVerified = "approved".equalsIgnoreCase(verification.optString("status", ""));
                        }
                    }

                    if (isVerified) {
                        userVerificationCache.put(userId, true);
                        holder.itemBinding.imgVerifiedBadge.setVisibility(View.VISIBLE);
                    } else {
                        holder.itemBinding.imgVerifiedBadge.setVisibility(View.GONE);
                        fetchVerificationStatus(userId);
                    }
                }

                String photo = user.optString("profile_photo", "");
                if (!photo.isEmpty()) {
                    String imageUrl = Constants.BASE_URL.replace("/api/", "") + (photo.startsWith("http") ? "" : "/storage/") + photo;
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .into(holder.itemBinding.imgClientProfile);
                } else {
                    holder.itemBinding.imgClientProfile.setImageResource(R.drawable.ic_profile);
                }
            } else {
                holder.itemBinding.txtClientName.setText("Client");
                holder.itemBinding.imgClientProfile.setImageResource(R.drawable.ic_profile);
            }

            holder.itemBinding.txtJobTitle.setText(job.getString("title"));

            String postedAt = job.optString("created_at", "");
            holder.itemBinding.txtPostedAt.setText("Posted " + UtilsFunctions.getTimeAgo(postedAt));

            String category = "General";
            if (job.optJSONObject("category") != null) {
                category = job.getJSONObject("category").getString("name");
            }

            String deadline = job.optString("deadline", "N/A");
            holder.itemBinding.txtJobDetails.setText(category + " • Due: " + UtilsFunctions.formatDate(deadline));
            holder.itemBinding.txtJobBudget.setText("Budget: Rs. " + job.getString("budget"));

            String status = job.optString("status", "open");
            int jobIdValue = job.optInt("id", -1);
            boolean hasApplied = appliedJobsCache.containsKey(jobIdValue);

            if (status.equalsIgnoreCase("in_progress")) {
                holder.itemBinding.btnApply.setText("In Progress");
                holder.itemBinding.btnApply.setEnabled(false);
                holder.itemBinding.btnApply.setAlpha(0.6f);
                holder.itemBinding.btnApply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(activity, R.color.outline)));
            } else if (status.equalsIgnoreCase("under_review")) {
                holder.itemBinding.btnApply.setText("Under Review");
                holder.itemBinding.btnApply.setEnabled(false);
                holder.itemBinding.btnApply.setAlpha(0.6f);
                holder.itemBinding.btnApply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(activity, R.color.warning)));
            } else if (status.equalsIgnoreCase("completed")) {
                holder.itemBinding.btnApply.setText("Completed");
                holder.itemBinding.btnApply.setEnabled(false);
                holder.itemBinding.btnApply.setAlpha(0.6f);
                holder.itemBinding.btnApply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(activity, R.color.success)));
            } else {
                holder.itemBinding.btnApply.setEnabled(true);
                holder.itemBinding.btnApply.setAlpha(1.0f);
                if (hasApplied) {
                    holder.itemBinding.btnApply.setText("Edit Proposal");
                    holder.itemBinding.btnApply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(activity, R.color.warning)));
                } else {
                    holder.itemBinding.btnApply.setText("Apply");
                    holder.itemBinding.btnApply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(activity, R.color.primary)));
                }
            }

            holder.itemBinding.btnApply.setOnClickListener(v -> {
                if (status.equalsIgnoreCase("under_review")) {
                    Toast.makeText(activity, "Job is currently under conflict review", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences pref = activity.getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
                int profileId = pref.getInt("profile_id", 0);

                if (profileId == 0) {
                    Toast.makeText(activity, "Please complete your profile/portfolio first", Toast.LENGTH_LONG).show();
                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new FreeLancerProfileEditFragment())
                            .addToBackStack(null)
                            .commit();
                    return;
                }

                try {
                    FreeLancerApplyJobProposalFragment fragment = new FreeLancerApplyJobProposalFragment();
                    Bundle args = new Bundle();
                    args.putString("job_data", job.toString());
                    fragment.setArguments(args);

                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            holder.itemView.setOnClickListener(v -> {
                TaskDetailsBottomSheet sheet = TaskDetailsBottomSheet.newInstance(job.toString());
                sheet.show(activity.getSupportFragmentManager(), "TaskDetails");
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchVerificationStatus(int userId) {
        if (userVerificationCache.containsKey(userId)) return;

        SharedPreferences pref = activity.getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
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
                            String status = data.optString("status", "").toLowerCase();
                            isVerified = status.equals("approved");
                        }
                    }
                    userVerificationCache.put(userId, isVerified);
                    if (isVerified) {
                        notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    userVerificationCache.put(userId, false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                userVerificationCache.put(userId, false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    public static class JobViewHolder extends RecyclerView.ViewHolder {
        public ItemFreelancerJobBinding itemBinding;
        public JobViewHolder(ItemFreelancerJobBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }
    }
}
