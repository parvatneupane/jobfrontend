package com.example.minijobhunt.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ProposalController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.controller.VerificationController;
import com.example.minijobhunt.databinding.FragmentClientMyJobsBinding;
import com.example.minijobhunt.databinding.ItemClientJobBinding;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientMyJobsFragment extends Fragment {

    private FragmentClientMyJobsBinding binding;
    private TaskController controller;
    private VerificationController verificationController;
    private ProposalController proposalController;
    private List<JSONObject> jobList = new ArrayList<>();
    private Map<Integer, Integer> proposalCounts = new HashMap<>();
    private JobAdapter adapter;
    private boolean isClientVerified = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClientMyJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        controller = new TaskController();
        verificationController = new VerificationController();
        proposalController = new ProposalController();

        setupRecyclerView();
        loadMyJobs();
        checkVerificationStatus();
    }

    private void checkVerificationStatus() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        verificationController.getVerification(token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject data = root.getJSONObject("data");
                        String status = data.optString("status", "").toLowerCase();
                        isClientVerified = status.equals("approved");
                        if (isClientVerified && adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new JobAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadMyJobs() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        // Fetch Proposals first to get counts
        proposalController.getProposals(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        proposalCounts.clear();
                        for (int i = 0; i < data.length(); i++) {
                            int taskId = data.getJSONObject(i).getInt("task_id");
                            proposalCounts.put(taskId, proposalCounts.getOrDefault(taskId, 0) + 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Now fetch tasks
                fetchTasks(token, currentUserId);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                fetchTasks(token, currentUserId);
            }
        });
    }

    private void fetchTasks(String token, int currentUserId) {
        controller.getTasks(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        jobList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject job = data.getJSONObject(i);
                            // Filter by logged-in user ID
                            if (job.optInt("user_id") == currentUserId) {
                                jobList.add(job);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteJob(int jobId) {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        controller.deleteTask(token, jobId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Job Deleted", Toast.LENGTH_SHORT).show();
                    loadMyJobs(); // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class JobAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<JobAdapter.JobViewHolder> {

        @NonNull
        @Override
        public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemClientJobBinding itemBinding = ItemClientJobBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new JobViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
            JSONObject job = jobList.get(position);
            try {
                int jobId = job.getInt("id");

                // Bind Client Info (You)
                if (job.has("user")) {
                    JSONObject user = job.getJSONObject("user");
                    holder.itemBinding.txtClientName.setText(user.optString("name", "You"));
                    
                    // Show verified badge if user is verified
                    boolean isVerified = isClientVerified || 
                                       "approved".equalsIgnoreCase(user.optString("verification_status", "")) ||
                                       "approved".equalsIgnoreCase(user.optString("status", "")) ||
                                       user.optBoolean("is_verified", false) ||
                                       user.optInt("is_verified", 0) == 1;

                    if (!isVerified && user.has("verification")) {
                        JSONObject verification = user.optJSONObject("verification");
                        if (verification != null) {
                            isVerified = "approved".equalsIgnoreCase(verification.optString("status", ""));
                        }
                    }

                    holder.itemBinding.imgVerifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);

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
                    holder.itemBinding.txtClientName.setText("You");
                    holder.itemBinding.imgClientProfile.setImageResource(R.drawable.ic_profile);
                }

                holder.itemBinding.txtTitle.setText(job.getString("title"));
                
                String postedAt = job.optString("created_at", "");
                holder.itemBinding.txtPostedAt.setText(UtilsFunctions.getTimeAgo(postedAt));

                String status = job.optString("status", "open");
                holder.itemBinding.txtStatus.setText(status.toUpperCase());

                boolean isInProgress = status.equalsIgnoreCase("in_progress");

                if (isInProgress) {
                    holder.itemBinding.btnEdit.setEnabled(false);
                    holder.itemBinding.btnDelete.setEnabled(false);
                    holder.itemBinding.btnEdit.setAlpha(0.5f);
                    holder.itemBinding.btnDelete.setAlpha(0.5f);
                } else {
                    holder.itemBinding.btnEdit.setEnabled(true);
                    holder.itemBinding.btnDelete.setEnabled(true);
                    holder.itemBinding.btnEdit.setAlpha(1.0f);
                    holder.itemBinding.btnDelete.setAlpha(1.0f);
                }

                holder.itemBinding.txtCategory.setText(job.optJSONObject("category") != null ? 
                        job.getJSONObject("category").getString("name") : "General");
                holder.itemBinding.txtBudget.setText("Rs. " + job.getString("budget"));
                holder.itemBinding.txtDeadline.setText("Due: " + job.getString("deadline"));

                int applicantCount = proposalCounts.getOrDefault(jobId, 0);
                holder.itemBinding.btnApplicants.setText("Applicants (" + applicantCount + ")");

                holder.itemBinding.btnApplicants.setOnClickListener(v -> {
                    ClientAllProposalsFragment fragment = new ClientAllProposalsFragment();
                    Bundle args = new Bundle();
                    args.putInt("job_id", jobId);

                    try {
                        args.putString("job_title", job.getString("title"));
                        args.putString("job_budget", job.getString("budget"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fragment.setArguments(args);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });

                holder.itemBinding.btnEdit.setOnClickListener(v -> {
                    ClientTaskPostingFragment fragment = new ClientTaskPostingFragment();
                    Bundle args = new Bundle();
                    args.putString("job_data", job.toString());
                    fragment.setArguments(args);
                    
                    requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });

                holder.itemBinding.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Job")
                            .setMessage("Are you sure you want to delete this job?")
                            .setPositiveButton("Yes", (dialog, which) -> deleteJob(jobId))
                            .setNegativeButton("No", null)
                            .show();
                });

                holder.itemView.setOnClickListener(v -> {
                    TaskDetailsBottomSheet sheet = TaskDetailsBottomSheet.newInstance(job.toString());
                    sheet.show(getChildFragmentManager(), "TaskDetails");
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return jobList.size();
        }

        class JobViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemClientJobBinding itemBinding;
            JobViewHolder(ItemClientJobBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
