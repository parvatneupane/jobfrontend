package com.example.minijobhunt.fragments;

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.minijobhunt.controller.FreeLancerProfileController;
import com.example.minijobhunt.controller.ProfileController;
import com.example.minijobhunt.databinding.FragmentFreeLancerProfileEditBinding;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreeLancerProfileEditFragment
        extends Fragment {

    private FragmentFreeLancerProfileEditBinding binding;
    private FreeLancerProfileController controller;
    private ProfileController globalProfileController;
    
    private List<JSONObject> allCategories = new ArrayList<>();
    private List<Integer> selectedCategoryIds = new ArrayList<>();



    @Override
    public View onCreateView(

            LayoutInflater inflater,

            ViewGroup container,

            Bundle savedInstanceState

    ) {

        binding =

                FragmentFreeLancerProfileEditBinding
                        .inflate(

                                inflater,

                                container,

                                false

                        );



        return binding.getRoot();

    }



    @Override
    public void onViewCreated(

            @NonNull View view,

            @Nullable Bundle savedInstanceState

    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );



        controller =
                new FreeLancerProfileController(
                        this
                );
        
        globalProfileController = new ProfileController();

        setupAvailability();
        loadAllCategories();



        // load existing profile
        controller.loadProfile();



        binding.btnSaveProfile
                .setOnClickListener(

                        v -> saveProfile()

                );
        
        binding.txtSelectedCategories.setOnClickListener(v -> showCategorySelector());

    }

    private void loadAllCategories() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        globalProfileController.getCategories(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        allCategories.clear();
                        for (int i = 0; i < data.length(); i++) {
                            allCategories.add(data.getJSONObject(i));
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

    private void showCategorySelector() {
        if (allCategories.isEmpty()) {
            Toast.makeText(getContext(), "Loading categories...", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[allCategories.size()];
        boolean[] checked = new boolean[allCategories.size()];

        for (int i = 0; i < allCategories.size(); i++) {
            names[i] = allCategories.get(i).optString("name");
            checked[i] = selectedCategoryIds.contains(allCategories.get(i).optInt("id"));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Categories")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    int id = allCategories.get(which).optInt("id");
                    if (isChecked) {
                        if (!selectedCategoryIds.contains(id)) selectedCategoryIds.add(id);
                    } else {
                        selectedCategoryIds.remove(Integer.valueOf(id));
                    }
                })
                .setPositiveButton("Done", (dialog, which) -> updateSelectedCategoriesText())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSelectedCategoriesText() {
        if (selectedCategoryIds.isEmpty()) {
            binding.txtSelectedCategories.setText("Select Categories");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allCategories.size(); i++) {
            if (selectedCategoryIds.contains(allCategories.get(i).optInt("id"))) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(allCategories.get(i).optString("name"));
            }
        }
        binding.txtSelectedCategories.setText(sb.toString());
    }



    private void setupAvailability() {

        String[] items = {

                "available",

                "busy",

                "unavailable"

        };



        ArrayAdapter<String> adapter =

                new ArrayAdapter<>(

                        requireContext(),

                        android.R.layout
                                .simple_spinner_dropdown_item,

                        items

                );



        binding.spAvailability
                .setAdapter(
                        adapter
                );

    }



    private void saveProfile() {

        controller.saveProfile(

                binding.etTitle
                        .getText()
                        .toString(),

                binding.etBio
                        .getText()
                        .toString(),

                binding.etExperience
                        .getText()
                        .toString(),

                binding.etSkills
                        .getText()
                        .toString(),

                binding.etLocation
                        .getText()
                        .toString(),

                binding.spAvailability
                        .getSelectedItem()
                        .toString(),

                binding.etPortfolio
                        .getText()
                        .toString(),
                
                selectedCategoryIds

        );

    }



    public void fillData(
            JSONObject p
    ) {

        try {

            binding.etTitle.setText(
                    p.optString(
                            "title"
                    )
            );



            binding.etBio.setText(
                    p.optString(
                            "bio"
                    )
            );



            binding.etExperience.setText(

                    String.valueOf(

                            p.optInt(
                                    "experience_years"
                            )

                    )

            );



            binding.etSkills.setText(
                    p.optString(
                            "skills"
                    )
            );



            binding.etLocation.setText(
                    p.optString(
                            "location"
                    )
            );



            binding.etPortfolio.setText(
                    p.optString(
                            "portfolio_url"
                    )
            );

            // Handle categories
            JSONArray categories = p.optJSONArray("categories");
            if (categories != null) {
                selectedCategoryIds.clear();
                for (int i = 0; i < categories.length(); i++) {
                    selectedCategoryIds.add(categories.getJSONObject(i).optInt("id"));
                }
                updateSelectedCategoriesText();
            }



            String availability =

                    p.optString(
                            "availability",
                            "available"
                    );



            ArrayAdapter adapter =

                    (ArrayAdapter)

                            binding
                                    .spAvailability
                                    .getAdapter();



            if (
                    adapter != null
            ) {

                int pos =

                        adapter.getPosition(
                                availability
                        );



                binding.spAvailability
                        .setSelection(
                                pos
                        );

            }

        }

        catch (
                Exception e
        ) {

            e.printStackTrace();

        }

    }



    public void profileSaved() {

        Toast.makeText(

                getContext(),

                "Profile Saved",

                Toast.LENGTH_SHORT

        ).show();



        requireActivity()

                .getSupportFragmentManager()

                .popBackStack();

    }



    public void showError(
            String msg
    ) {

        Toast.makeText(

                getContext(),

                msg,

                Toast.LENGTH_LONG

        ).show();

    }



    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;

    }

}