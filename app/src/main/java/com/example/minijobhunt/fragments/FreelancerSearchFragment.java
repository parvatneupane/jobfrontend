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

import com.example.minijobhunt.R;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

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

    private static final int MAP_PICKER_REQUEST_CODE = 2003;
    private FragmentFreelancerSearchBinding binding;
    private TaskController taskController;
    private List<JSONObject> allTasks = new ArrayList<>();
    private List<JSONObject> filteredJobs = new ArrayList<>();
    private FreelancerJobAdapter adapter;
    private Map<Integer, String> appliedJobsCache = new HashMap<>();
    private String selectedCategory = "All";
    private String selectedLocation = "";
    private double filterLat = 0, filterLng = 0;
    private float maxDistanceKm = 100f; // Default "Any"
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFreelancerSearchBinding.inflate(inflater, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskController = new TaskController();
        
        binding.rvJobs.setLayoutManager(new GridLayoutManager(requireContext(), 1));
        adapter = new FreelancerJobAdapter(filteredJobs, requireActivity(), appliedJobsCache);
        binding.rvJobs.setAdapter(adapter);
        
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
            applyFilters();
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etLocationFilter.setOnClickListener(v -> startMapPicker());
        binding.btnClearLocation.setOnClickListener(v -> clearLocationFilter());

        binding.btnNearbyMe.setOnClickListener(v -> {
            if (filterLat != 0) {
                clearLocationFilter();
            } else {
                useLiveLocation();
            }
        });

        // Removed setupDistanceFilter() as it was removed from layout
    }

    private void clearLocationFilter() {
        selectedLocation = "";
        filterLat = 0;
        filterLng = 0;
        maxDistanceKm = 100f;
        binding.etLocationFilter.setText("");
        binding.btnClearLocation.setVisibility(View.GONE);
        binding.btnNearbyMe.setIconResource(R.drawable.ic_home_outlined);
        
        loadAllJobs(binding.etSearch.getText().toString());
    }

    private void useLiveLocation() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        Toast.makeText(requireContext(), "Getting live location...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                filterLat = location.getLatitude();
                filterLng = location.getLongitude();
                selectedLocation = "Nearby Me";
                binding.etLocationFilter.setText(selectedLocation);
                binding.btnClearLocation.setVisibility(View.VISIBLE);
                binding.btnNearbyMe.setIconResource(R.drawable.ic_close);
                
                maxDistanceKm = 10f; // 10km radius as requested
                
                applyFilters();
                loadAllJobs(binding.etSearch.getText().toString());
            } else {
                Toast.makeText(requireContext(), "Unable to get GPS. Make sure location is ON.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMapPicker() {
        android.content.Intent intent = new android.content.Intent(requireContext(), com.example.minijobhunt.views.MapPickerActivity.class);
        intent.putExtra("show_radius", true);
        
        // Pass all available jobs to show on map
        JSONArray array = new JSONArray();
        for (JSONObject job : allTasks) {
            array.put(job);
        }
        intent.putExtra("jobs_data", array.toString());
        
        startActivityForResult(intent, MAP_PICKER_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        if (requestCode == MAP_PICKER_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                selectedLocation = data.getStringExtra("address");
                filterLat = data.getDoubleExtra("latitude", 0);
                filterLng = data.getDoubleExtra("longitude", 0);
                maxDistanceKm = data.getFloatExtra("radius", 100f);
                
                binding.etLocationFilter.setText(selectedLocation);
                binding.btnClearLocation.setVisibility(View.VISIBLE);
                binding.btnNearbyMe.setIconResource(R.drawable.ic_close);
                
                loadAllJobs(binding.etSearch.getText().toString());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                fetchTasks(token);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                fetchTasks(token);
            }
        });
    }

    private void fetchTasks(String token) {
        Double lat = (filterLat != 0) ? filterLat : null;
        Double lng = (filterLng != 0) ? filterLng : null;
        Float radius = (maxDistanceKm < 100f) ? maxDistanceKm : null;

        taskController.getTasks(token, lat, lng, radius).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!isAdded() || binding == null) return;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        allTasks.clear();
                        for (int i = 0; i < data.length(); i++) {
                            allTasks.add(data.getJSONObject(i));
                        }
                        applyFilters();
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

    private void applyFilters() {
        if (binding == null) return;
        
        filteredJobs.clear();
        String query = binding.etSearch.getText().toString().toLowerCase().trim();

        for (JSONObject job : allTasks) {
            String title = job.optString("title", "").toLowerCase();
            String jobStatus = job.optString("status", "open");
            int jobId = job.optInt("id", -1);
            
            String category = "";
            JSONObject catObj = job.optJSONObject("category");
            if (catObj != null) {
                category = catObj.optString("name", "");
            }
            
            double jobLat = job.optDouble("latitude", 0);
            double jobLng = job.optDouble("longitude", 0);

            // Handle string coordinates from API
            if (jobLat == 0 && job.has("latitude")) {
                jobLat = Double.parseDouble(job.optString("latitude", "0"));
            }
            if (jobLng == 0 && job.has("longitude")) {
                jobLng = Double.parseDouble(job.optString("longitude", "0"));
            }
            
            boolean matchesCategory = selectedCategory.equals("All") || 
                                     category.equalsIgnoreCase(selectedCategory);
            
            boolean matchesSearch = query.isEmpty() || 
                                   title.contains(query) || 
                                   category.toLowerCase().contains(query);
            
            boolean matchesLocation = true;
            float distanceKm = -1;
            
            if (jobLat != 0 && jobLng != 0 && filterLat != 0 && filterLng != 0) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(filterLat, filterLng, jobLat, jobLng, results);
                distanceKm = results[0] / 1000;
            }

            if (maxDistanceKm < 100f) {
                if (distanceKm != -1) {
                    matchesLocation = distanceKm <= maxDistanceKm;
                } else {
                    matchesLocation = false;
                }
            }

            if (matchesCategory && matchesSearch && matchesLocation && jobStatus.equalsIgnoreCase("open") && !appliedJobsCache.containsKey(jobId)) {
                try {
                    job.put("calculated_distance", distanceKm);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                filteredJobs.add(job);
            }
        }
        
        adapter.notifyDataSetChanged();

        if (filteredJobs.isEmpty()) {
            binding.txtEmpty.setVisibility(View.VISIBLE);
            binding.rvJobs.setVisibility(View.GONE);
        } else {
            binding.txtEmpty.setVisibility(View.GONE);
            binding.rvJobs.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadAllJobs(binding.etSearch.getText().toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
