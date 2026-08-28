package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.example.minijobhunt.adapter.FreelancerJobAdapter;
import com.example.minijobhunt.controller.ProposalController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.databinding.FragmentFreelancerSearchBinding;
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

public class FreelancerSearchFragment extends Fragment {

    private FragmentFreelancerSearchBinding binding;
    private TaskController taskController;
    private List<JSONObject> jobList = new ArrayList<>();
    private FreelancerJobAdapter adapter;
    private Map<Integer, String> appliedJobsCache = new HashMap<>();
    private String selectedCategory = "All";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFreelancerSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskController = new TaskController();
        
        binding.rvJobs.setLayoutManager(new GridLayoutManager(requireContext(), 1));
        
        loadCategories();
        // Removed loadAllJobs here because it's called in onResume

        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = "All";
                binding.chipAll.setChecked(true);
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedCategory = chip.getText().toString();
                } else {
                    selectedCategory = "All";
                }
            }
            loadAllJobs(binding.etSearch.getText().toString());
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadAllJobs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCategories() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        taskController.getCategories(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!isAdded() || binding == null) return;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        // Clear existing chips except the "All" chip
                        int childCount = binding.chipGroupCategories.getChildCount();
                        int allChipId = binding.chipAll.getId();
                        for (int i = childCount - 1; i >= 0; i--) {
                            View child = binding.chipGroupCategories.getChildAt(i);
                            if (child.getId() != allChipId) {
                                binding.chipGroupCategories.removeViewAt(i);
                            }
                        }

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject cat = data.getJSONObject(i);
                            String name = cat.getString("name");
                            
                            Chip chip = new Chip(new ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Widget_Material3_Chip_Filter));
                            chip.setText(name);
                            chip.setCheckable(true);
                            chip.setClickable(true);
                            binding.chipGroupCategories.addView(chip);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void loadAllJobs(String query) {
        if (!isAdded() || binding == null) return;
        
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", -1);

        appliedJobsCache.clear();

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
                fetchTasks(token, query);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                fetchTasks(token, query);
            }
        });
    }

    private void fetchTasks(String token, String query) {
        taskController.getTasks(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!isAdded() || binding == null) return;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        jobList.clear();
                        String q = query.toLowerCase().trim();
                        
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject job = data.getJSONObject(i);
                            String title = job.optString("title", "").toLowerCase();
                            String jobStatus = job.optString("status", "open");
                            int jobId = job.optInt("id", -1);
                            
                            String category = "";
                            if (job.optJSONObject("category") != null) {
                                category = job.getJSONObject("category").optString("name", "");
                            }
                            
                            boolean matchesCategory = selectedCategory.equals("All") || 
                                                     category.equalsIgnoreCase(selectedCategory);
                            
                            boolean matchesSearch = q.isEmpty() || 
                                                   title.contains(q) || 
                                                   category.toLowerCase().contains(q);
                            
                            // Problem 3 Logic:
                            // 1. Job must be 'open' (not given to someone else)
                            // 2. Freelancer must NOT have applied already (keep search clean)
                            if (matchesCategory && matchesSearch && jobStatus.equalsIgnoreCase("open") && !appliedJobsCache.containsKey(jobId)) {
                                jobList.add(job);
                            }
                        }
                        
                        android.util.Log.d("SEARCH", "Jobs loaded: " + jobList.size() + " out of " + data.length());
                        
                        adapter = new FreelancerJobAdapter(jobList, requireActivity(), appliedJobsCache);
                        binding.rvJobs.setAdapter(adapter);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Search Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            String query = "";
            if (binding.etSearch.getText() != null) {
                query = binding.etSearch.getText().toString();
            }
            loadAllJobs(query);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
