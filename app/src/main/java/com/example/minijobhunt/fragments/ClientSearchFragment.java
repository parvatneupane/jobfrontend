package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.minijobhunt.R;
import com.example.minijobhunt.adapter.FreelancerSearchAdapter;
import com.example.minijobhunt.controller.ProfileController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.databinding.FragmentClientSearchBinding;
import com.example.minijobhunt.utils.Constants;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientSearchFragment extends Fragment {

    private FragmentClientSearchBinding binding;
    private ProfileController profileController;
    private TaskController taskController;
    private FreelancerSearchAdapter adapter;
    private JSONArray allFreelancers = new JSONArray();
    private String selectedCategory = "All";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentClientSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileController = new ProfileController();
        taskController = new TaskController();
        
        binding.rvFreelancers.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadCategories();
        loadFreelancers("");

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadFreelancers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = "All";
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedCategory = chip.getText().toString();
                } else {
                    selectedCategory = "All";
                }
            }
            filterFreelancers();
        });
    }

    private void loadCategories() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        taskController.getCategories(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!isAdded()) return;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject cat = data.getJSONObject(i);
                            addCategoryChip(cat.getString("name"));
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

    private void addCategoryChip(String categoryName) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filter, binding.chipGroupCategories, false);
        chip.setText(categoryName);
        chip.setId(View.generateViewId());
        binding.chipGroupCategories.addView(chip);
    }

    private void loadFreelancers(String query) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        profileController.getFreelancers(token, query).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!isAdded()) return;
                binding.progressBar.setVisibility(View.GONE);
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        allFreelancers = root.getJSONArray("data");
                        filterFreelancers();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (!isAdded()) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Search Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterFreelancers() {
        if (selectedCategory.equals("All")) {
            adapter = new FreelancerSearchAdapter(allFreelancers, requireActivity());
            binding.rvFreelancers.setAdapter(adapter);
            return;
        }

        JSONArray filtered = new JSONArray();
        for (int i = 0; i < allFreelancers.length(); i++) {
            try {
                JSONObject profile = allFreelancers.getJSONObject(i);
                boolean matches = false;
                
                // Check if profile has categories array
                if (profile.has("categories")) {
                    JSONArray cats = profile.getJSONArray("categories");
                    for (int j = 0; j < cats.length(); j++) {
                        if (cats.getJSONObject(j).getString("name").equalsIgnoreCase(selectedCategory)) {
                            matches = true;
                            break;
                        }
                    }
                }
                
                // Fallback: check skills or title if categories not explicitly linked in profile
                if (!matches) {
                    String skills = profile.optString("skills", "").toLowerCase();
                    String title = profile.optString("title", "").toLowerCase();
                    if (skills.contains(selectedCategory.toLowerCase()) || title.contains(selectedCategory.toLowerCase())) {
                        matches = true;
                    }
                }

                if (matches) {
                    filtered.put(profile);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter = new FreelancerSearchAdapter(filtered, requireActivity());
        binding.rvFreelancers.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
