package com.example.minijobhunt.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.databinding.FragmentClientTaskPostingBinding;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientTaskPostingFragment extends Fragment {

    private static final int MAP_PICKER_REQUEST_CODE = 2001;
    private FragmentClientTaskPostingBinding binding;
    private TaskController controller;
    private List<String> categoryNames = new ArrayList<>();
    private List<Integer> categoryIds = new ArrayList<>();
    private int selectedCategoryId = -1;
    
    private int jobId = -1;
    private boolean isEdit = false;
    private double selectedLat = 0, selectedLng = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClientTaskPostingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkVerification();
        controller = new TaskController();
        loadCategories();

        binding.etDeadline.setOnClickListener(v -> showDatePicker());
        binding.btnPostJob.setOnClickListener(v -> submitJob());

        binding.actCategory.setOnItemClickListener((parent, view1, position, id) -> {
            selectedCategoryId = categoryIds.get(position);
        });

        binding.etLocation.setOnClickListener(v -> startMapPicker());
        
        checkEditMode();
    }

    private void startMapPicker() {
        android.content.Intent intent = new android.content.Intent(requireContext(), com.example.minijobhunt.views.MapPickerActivity.class);
        startActivityForResult(intent, MAP_PICKER_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        if (requestCode == MAP_PICKER_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                String address = data.getStringExtra("address");
                selectedLat = data.getDoubleExtra("latitude", 0);
                selectedLng = data.getDoubleExtra("longitude", 0);
                binding.etLocation.setText(address);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkEditMode() {
        if (getArguments() != null && getArguments().containsKey("job_data")) {
            try {
                isEdit = true;
                String jobJson = getArguments().getString("job_data");
                JSONObject job = new JSONObject(jobJson);
                
                jobId = job.getInt("id");
                binding.etTitle.setText(job.getString("title"));
                binding.etDescription.setText(job.getString("description"));
                binding.etSkills.setText(job.optString("required_skills", ""));
                binding.etExperience.setText(job.optString("min_experience", ""));
                binding.etBudget.setText(job.getString("budget"));
                binding.etDeadline.setText(job.getString("deadline"));
                binding.etLocation.setText(job.optString("location", ""));
                selectedLat = job.optDouble("latitude", 0);
                selectedLng = job.optDouble("longitude", 0);
                
                if (job.optJSONObject("category") != null) {
                    selectedCategoryId = job.getJSONObject("category").getInt("id");
                    binding.actCategory.setText(job.getJSONObject("category").getString("name"), false);
                }

                binding.btnPostJob.setText("Update Job");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkVerification() {
        if (isEdit) return;

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        new com.example.minijobhunt.controller.VerificationController().getVerification(token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String status = "";
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject data = root.getJSONObject("data");
                        status = data.optString("status", "").toLowerCase();
                    }
                    
                    if (status.equals("approved")) {
                        // All good
                    } else if (status.equals("pending")) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Wait for your verification approval", Toast.LENGTH_LONG).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    } else {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "You must be verified to post a job", Toast.LENGTH_LONG).show();
                            requireActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, new VerificationFragment())
                                    .commit();
                        }
                    }
                } catch (Exception e) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Please complete your verification first", Toast.LENGTH_LONG).show();
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new VerificationFragment())
                                .commit();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void loadCategories() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        String cachedCategories = pref.getString("task_categories_json", "");
        if (!cachedCategories.isEmpty()) {
            parseAndSetCategories(cachedCategories);
        }

        controller.getCategories(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        pref.edit().putString("task_categories_json", json).apply();
                        parseAndSetCategories(json);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (categoryNames.isEmpty()) {
                    Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void parseAndSetCategories(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray data = root.getJSONArray("data");

            categoryNames.clear();
            categoryIds.clear();

            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.getJSONObject(i);
                categoryNames.add(obj.getString("name"));
                categoryIds.add(obj.getInt("id"));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categoryNames
            );
            binding.actCategory.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = year1 + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                    binding.etDeadline.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void submitJob() {
        String title = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String skills = binding.etSkills.getText().toString().trim();
        String experience = binding.etExperience.getText().toString().trim();
        String budgetStr = binding.etBudget.getText().toString().trim();
        String deadline = binding.etDeadline.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategoryId == -1 || budgetStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("category_id", selectedCategoryId);
        body.put("title", title);
        body.put("description", description);
        body.put("required_skills", skills);
        body.put("min_experience", experience);
        body.put("budget", budgetStr);
        body.put("deadline", deadline);
        body.put("location", location);
        body.put("latitude", selectedLat);
        body.put("longitude", selectedLng);
        body.put("status", "open");

        Call<ResponseBody> call;
        if (isEdit) {
            call = controller.updateTask(token, jobId, body);
        } else {
            call = controller.createTask(token, body);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String msg = isEdit ? "Job Updated Successfully" : "Job Posted Successfully";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

                    requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new ClientMyJobsFragment())
                            .commit();
                } else {
                    Toast.makeText(requireContext(), "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
