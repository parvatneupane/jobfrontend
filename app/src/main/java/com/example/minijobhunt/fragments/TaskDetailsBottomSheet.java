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
                txtSkills.setText(task.optString("required_skills", "N/A"));
                txtDescription.setText(task.optString("description", "No description provided."));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnClose.setOnClickListener(v -> dismiss());
    }
}
