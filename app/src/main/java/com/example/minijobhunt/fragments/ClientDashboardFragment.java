package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ProposalController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.controller.VerificationController;
import com.example.minijobhunt.utils.UtilsFunctions;
import com.bumptech.glide.Glide;
import com.example.minijobhunt.databinding.FragmentClientDashboardBinding;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientDashboardFragment extends Fragment {

    private FragmentClientDashboardBinding binding;
    private TaskController taskController;
    private ProposalController proposalController;
    private VerificationController verificationController;
    private String verificationStatus = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentClientDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskController = new TaskController();
        proposalController = new ProposalController();
        verificationController = new VerificationController();
        
        loadDashboard();
        loadOpenJobs();
        checkVerificationStatus();

        binding.btnPostJob.setOnClickListener(v -> {
            if ("approved".equalsIgnoreCase(verificationStatus)) {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ClientTaskPostingFragment())
                        .addToBackStack(null)
                        .commit();
            } else if ("pending".equalsIgnoreCase(verificationStatus)) {
                Toast.makeText(requireContext(), "Wait for your verification approval", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Please verify your account to post a job", Toast.LENGTH_LONG).show();
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new VerificationFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
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
                        verificationStatus = data.optString("status", "").toLowerCase();
                        boolean isVerified = verificationStatus.equals("approved");
                        
                        binding.imgVerifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
                        binding.imgRecentVerifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
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

    private void loadDashboard() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);

        // Load cached name
        String name = pref.getString("name", "Client");
        binding.txtUserName.setText(name + " 👋");

        // Refresh from API
        String token = pref.getString("token", "");
        App.api.getMyProfile("Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject profile = root.getJSONObject("data");
                        JSONObject user = profile.getJSONObject("user");

                        String name = user.optString("name", "Client");
                        binding.txtUserName.setText(name + " 👋");

                        // Show verified badge if user is verified
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
                        
                        binding.imgVerifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);

                        // Save cache
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("name", name);
                        editor.apply();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void loadOpenJobs() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        taskController.getTasks(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        int openCount = 0;
                        JSONObject recentJob = null;
                        List<Integer> myJobIds = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject job = data.getJSONObject(i);
                            
                            // Filter by logged-in user ID
                            if (job.optInt("user_id") == currentUserId) {
                                myJobIds.add(job.getInt("id"));
                                
                                if (job.getString("status").equalsIgnoreCase("open")) {
                                    openCount++;
                                }

                                if (recentJob == null) { // First one belonging to user is most recent
                                    recentJob = job;
                                }
                            }
                        }

                        binding.txtOpenJobsCount.setText(String.valueOf(openCount));

                        // Fetch total applicants for these jobs
                        fetchApplicantStats(token, myJobIds, recentJob);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void fetchApplicantStats(String token, List<Integer> myJobIds, JSONObject recentJob) {
        proposalController.getProposals(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        int totalApplicants = 0;
                        int recentJobApplicants = 0;
                        int recentJobId = recentJob != null ? recentJob.getInt("id") : -1;

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject prop = data.getJSONObject(i);
                            int taskId = prop.getInt("task_id");
                            if (myJobIds.contains(taskId)) {
                                totalApplicants++;
                                if (taskId == recentJobId) {
                                    recentJobApplicants++;
                                }
                            }
                        }

                        binding.txtApplicationsCount.setText(String.valueOf(totalApplicants));

                        if (recentJob != null) {
                            binding.txtRecentJobTitle.setText(recentJob.getString("title"));
                            binding.txtRecentJobPostedAt.setText(UtilsFunctions.getTimeAgo(recentJob.optString("created_at", "")));
                            binding.txtRecentJobApplicants.setText(recentJobApplicants + " applicants");

                            if (recentJob.has("user")) {
                                JSONObject user = recentJob.getJSONObject("user");
                                binding.txtRecentClientName.setText(user.optString("name", "You"));
                                
                                // Show verified badge if user is verified
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

                                binding.imgRecentVerifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);

                                String photo = user.optString("profile_photo", "");
                                if (!photo.isEmpty()) {
                                    String imageUrl = Constants.BASE_URL.replace("/api/", "") + (photo.startsWith("http") ? "" : "/storage/") + photo;
                                    Glide.with(requireContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.ic_profile)
                                            .into(binding.imgRecentClientProfile);
                                }
                            }

                            binding.cardRecentJob.setOnClickListener(v -> {
                                ClientMyJobsFragment fragment = new ClientMyJobsFragment();
                                Bundle args = new Bundle();
                                try {
                                    args.putInt("job_id", recentJob.getInt("id"));
                                    args.putString("job_title", recentJob.getString("title"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                fragment.setArguments(args);
                                
                                requireActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, fragment)
                                        .addToBackStack(null)
                                        .commit();
                            });
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

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
        loadOpenJobs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
