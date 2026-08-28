package com.example.minijobhunt.fragments;

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

import com.example.minijobhunt.adapter.FreelancerJobAdapter;
import com.example.minijobhunt.controller.ProposalController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.databinding.FragmentFreelancerJobsBinding;
import com.example.minijobhunt.utils.Constants;

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

public class FreelancerJobsFragment extends Fragment {

    private FragmentFreelancerJobsBinding binding;
    private TaskController controller;
    private List<JSONObject> jobList = new ArrayList<>();
    private FreelancerJobAdapter adapter;
    private Map<Integer, String> appliedJobsCache = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFreelancerJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        controller = new TaskController();
        setupRecyclerView();
        loadAllJobs();
    }

    private void setupRecyclerView() {
        adapter = new FreelancerJobAdapter(jobList, requireActivity(), appliedJobsCache);
        binding.recyclerViewJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewJobs.setAdapter(adapter);
    }

    private void loadAllJobs() {
        if (!isAdded() || binding == null) return;

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", -1);

        appliedJobsCache.clear();

        // First fetch proposals to see what we've applied for
        new ProposalController().getProposals(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject prop = data.getJSONObject(i);
                            if (prop.optInt("user_id") == userId) {
                                appliedJobsCache.put(prop.optInt("task_id"), prop.optString("status", "pending"));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Then load jobs
                fetchTasks(token);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                fetchTasks(token);
            }
        });
    }

    private void fetchTasks(String token) {
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
                            int jobId = job.optInt("id", -1);
                            
                            // Only show jobs where the user has applied
                            if (appliedJobsCache.containsKey(jobId)) {
                                String proposalStatus = appliedJobsCache.get(jobId);
                                
                                // Problem 3 logic:
                                // 1. If proposal is accepted, don't show here (it moves to In Progress)
                                // 2. If proposal is rejected, don't show here
                                if (proposalStatus != null && 
                                    !proposalStatus.equalsIgnoreCase("accepted") &&
                                    !proposalStatus.equalsIgnoreCase("rejected")) {
                                    jobList.add(job);
                                }
                            }
                        }
                        
                        adapter = new FreelancerJobAdapter(jobList, requireActivity(), appliedJobsCache);
                        binding.recyclerViewJobs.setAdapter(adapter);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllJobs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
