package com.example.minijobhunt.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.minijobhunt.R;
import com.example.minijobhunt.utils.UtilsFunctions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONObject;

public class TaskDetailsBottomSheet extends BottomSheetDialogFragment {

    private JSONObject task;

    public static TaskDetailsBottomSheet newInstance(String taskJson) {
        TaskDetailsBottomSheet fragment = new TaskDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString("task_data", taskJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_task_details_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtTitle = view.findViewById(R.id.txtTitle);
        TextView txtCategory = view.findViewById(R.id.txtCategory);
        TextView txtBudget = view.findViewById(R.id.txtBudget);
        TextView txtDeadline = view.findViewById(R.id.txtDeadline);
        TextView txtExperience = view.findViewById(R.id.txtExperience);
        TextView txtLocation = view.findViewById(R.id.txtLocation);
        TextView txtDistance = view.findViewById(R.id.txtDistance);
        TextView txtSkills = view.findViewById(R.id.txtSkills);
        TextView txtDescription = view.findViewById(R.id.txtDescription);
        Button btnClose = view.findViewById(R.id.btnClose);

        if (getArguments() != null) {
            try {
                task = new JSONObject(getArguments().getString("task_data"));
                
                txtTitle.setText(task.optString("title", "No Title"));
                
                if (task.has("category")) {
                    txtCategory.setText(task.getJSONObject("category").optString("name", "General"));
                } else {
                    txtCategory.setText("General");
                }
                
                txtBudget.setText("Rs. " + task.optString("budget", "0"));
                txtDeadline.setText(UtilsFunctions.formatDate(task.optString("deadline", "N/A")));
                txtExperience.setText(task.optString("min_experience", "N/A"));
                txtLocation.setText(task.optString("location", "Location N/A"));

                // Distance calculation
                double jobLat = task.optDouble("latitude", 0);
                double jobLng = task.optDouble("longitude", 0);
                if (jobLat != 0 && jobLng != 0) {
                    android.content.SharedPreferences pref = requireActivity().getSharedPreferences(com.example.minijobhunt.utils.Constants.cache, android.content.Context.MODE_PRIVATE);
                    double userLat = Double.longBitsToDouble(pref.getLong("profile_latitude", Double.doubleToLongBits(0)));
                    double userLng = Double.longBitsToDouble(pref.getLong("profile_longitude", Double.doubleToLongBits(0)));
                    
                    if (userLat != 0 && userLng != 0) {
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(userLat, userLng, jobLat, jobLng, results);
                        float distanceKm = results[0] / 1000;
                        txtDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f km away", distanceKm));
                        txtDistance.setVisibility(View.VISIBLE);
                    } else {
                        txtDistance.setVisibility(View.GONE);
                    }
                } else {
                    txtDistance.setVisibility(View.GONE);
                }

                txtSkills.setText(task.optString("required_skills", "N/A"));
                txtDescription.setText(task.optString("description", "No description provided."));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnClose.setOnClickListener(v -> dismiss());
    }
}
