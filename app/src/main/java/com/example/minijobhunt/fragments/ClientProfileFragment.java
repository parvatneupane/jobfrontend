package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ProposalController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.databinding.FragmentClientProfileBinding;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.views.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientProfileFragment extends Fragment {

    private FragmentClientProfileBinding binding;
    private TaskController taskController;
    private ProposalController proposalController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentClientProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskController = new TaskController();
        proposalController = new ProposalController();

        loadProfile();
        checkVerificationStatus();
        loadStats();

        binding.btnLogout.setOnClickListener(v -> logout());

        binding.btnVerify.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new VerificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnJobs.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ClientMyJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnInProgressClientJobs.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ClientInProgressJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnCompletedClientJobs.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ClientCompletedJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadStats() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        // Fetch Tasks
        taskController.getTasks(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        int postedCount = 0;
                        List<Integer> myJobIds = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject job = data.getJSONObject(i);
                            if (job.optInt("user_id") == currentUserId) {
                                postedCount++;
                                myJobIds.add(job.getInt("id"));
                            }
                        }

                        binding.txtJobsPostedCount.setText(String.valueOf(postedCount));

                        // Fetch Proposals to count applicants for my jobs
                        fetchApplicantStats(token, myJobIds);
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

    private void fetchApplicantStats(String token, List<Integer> myJobIds) {
        proposalController.getProposals(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        int totalApplicants = 0;
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject prop = data.getJSONObject(i);
                            if (myJobIds.contains(prop.getInt("task_id"))) {
                                totalApplicants++;
                            }
                        }
                        binding.txtTotalApplicantsCount.setText(String.valueOf(totalApplicants));
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

    private void loadProfile() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);

        // Show cached data
        String name = pref.getString("name", "");
        if (!name.isEmpty()) {
            binding.txtUsername.setText(name);
            binding.txtAvatar.setText(String.valueOf(name.charAt(0)));
        }

        // Refresh from server
        String token = pref.getString("token", "");
        App.api.getMyProfile("Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject profile = root.getJSONObject("data");
                        JSONObject user = profile.getJSONObject("user");

                        String name = user.optString("name");
                        binding.txtUsername.setText(name);
                        if (!name.isEmpty()) {
                            binding.txtAvatar.setText(String.valueOf(name.charAt(0)));
                        }

                        // Save latest
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("name", name);
                        editor.apply();
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

    private void checkVerificationStatus() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        App.api.getVerification("Bearer " + token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject data = root.getJSONObject("data");
                        String status = data.optString("status", "").toLowerCase();
                        String date = data.optString("updated_at", "");

                        binding.txtVerificationStatus.setText("Status • " + status.substring(0, 1).toUpperCase() + status.substring(1));

                        if (status.equals("approved")) {
                            int green = ContextCompat.getColor(requireContext(), R.color.success);
                            binding.txtVerificationStatus.setTextColor(green);
                            binding.imgVerifiedBadge.setVisibility(View.VISIBLE);
                            
                            if (!date.isEmpty()) {
                                binding.txtVerificationDate.setText("Verified on: " + date.split("T")[0]);
                                binding.txtVerificationDate.setVisibility(View.VISIBLE);
                                binding.txtVerificationDate.setTextColor(green);
                            }

                            binding.btnVerify.setText("Verified");
                            binding.btnVerify.setEnabled(false);
                            binding.btnVerify.setAlpha(0.6f);
                        } else if (status.equals("pending")) {
                            binding.txtVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
                            binding.btnVerify.setText("Pending");
                            binding.btnVerify.setEnabled(false);
                            binding.btnVerify.setAlpha(0.6f);
                        } else if (status.equals("rejected")) {
                            binding.txtVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
                            binding.btnVerify.setText("Re-verify");
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

    private void logout() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = pref.getString("token", "");

        App.api.logout("Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                performLocalLogout();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                performLocalLogout();
            }
        });
    }

    private void performLocalLogout() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        pref.edit().clear().apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        checkVerificationStatus();
        loadStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
