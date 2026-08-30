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
import com.example.minijobhunt.databinding.FragmentFreelancerDashboardBinding;
import com.example.minijobhunt.controller.VerificationController;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreelancerDashboardFragment extends Fragment {

    private FragmentFreelancerDashboardBinding binding;
    private VerificationController verificationController;
    private boolean isUserVerified = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFreelancerDashboardBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState);
        verificationController = new VerificationController();

        loadDashboard();
        checkVerificationStatus();
        loadActiveJobsCount();
        loadProposalsCount();

        binding.btnBrowseJobs.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FreelancerJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnApplications.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FreelancerJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnEarnings.setOnClickListener(v -> {
            String url = "https://esewa.com.np";
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            startActivity(intent);
        });
    }

    private void loadActiveJobsCount() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        App.api.getContracts(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        int activeCount = 0;
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject contract = data.getJSONObject(i);
                            if (contract.optInt("freelancer_id") == userId && 
                                "active".equalsIgnoreCase(contract.optString("status"))) {
                                activeCount++;
                            }
                        }
                        binding.txtActiveJobs.setText(String.valueOf(activeCount));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void loadProposalsCount() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        App.api.getProposals(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        int proposalCount = 0;
                        for (int i = 0; i < data.length(); i++) {
                            if (data.getJSONObject(i).optInt("user_id") == userId) {
                                proposalCount++;
                            }
                        }
                        binding.txtProposals.setText(String.valueOf(proposalCount));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
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
                        String status = data.optString("status", "").toLowerCase();
                        isUserVerified = status.equals("approved");
                        binding.imgVerifiedBadge.setVisibility(isUserVerified ? View.VISIBLE : View.GONE);
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

        SharedPreferences pref =
                requireActivity().getSharedPreferences(
                        Constants.cache,
                        Context.MODE_PRIVATE
                );

        // ------------------------
        // Load cached data first
        // ------------------------

        String name =
                pref.getString("name", "Freelancer");

        String title =
                pref.getString("profile_title", "No Title");

        String availability =
                pref.getString(
                        "profile_availability",
                        "Available"
                );

        float rating =
                pref.getFloat(
                        "profile_rating",
                        0f
                );

        binding.txtUserName.setText(name + " 👋");
        binding.txtTitle.setText(title);
        binding.txtAvailability.setText(availability);
        binding.txtRating.setText("⭐ " + rating);

        String cachedEarnings = pref.getString("profile_earnings", "0");
        binding.txtEarnings.setText("Rs. " + cachedEarnings);

        if (rating == 0) {

            binding.txtRatingText.setText(
                    "No ratings yet"
            );

        } else {

            binding.txtRatingText.setText(
                    "Excellent performance"
            );

        }

        // Sample values

        binding.txtEarnings.setText("Rs. 0");
        binding.txtGrowth.setText("+0% this month");
        binding.txtActiveJobs.setText("0");
        binding.txtProposals.setText("0");

        // ------------------------
        // Refresh from API
        // ------------------------

        String token =
                pref.getString(
                        "token",
                        ""
                );

        App.api
                .getMyProfile("Bearer " + token)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            Call<ResponseBody> call,
                            Response<ResponseBody> response
                    ) {

                        try {

                            if (!response.isSuccessful()
                                    || response.body() == null) {

                                return;

                            }

                            String json =
                                    response.body().string();

                            JSONObject root =
                                    new JSONObject(json);

                            JSONObject profile =
                                    root.getJSONObject("data");

                            JSONObject user =
                                    profile.getJSONObject("user");

                            String name =
                                    user.optString(
                                            "name",
                                            "Freelancer"
                                    );

                            String title =
                                    profile.optString(
                                            "title",
                                            "No Title"
                                    );

                            String availability =
                                    profile.optString(
                                            "availability",
                                            "Available"
                                    );

                            float rating =
                                    (float) profile.optDouble(
                                            "rating",
                                            0
                                    );

                            String earnings = profile.optString("earned_money", "0");

                            // Update UI

                            binding.txtUserName.setText(
                                    name + " 👋"
                            );

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

                            binding.txtTitle.setText(
                                    title
                            );

                            binding.txtAvailability.setText(
                                    availability
                            );

                            binding.txtRating.setText(
                                    "⭐ " + rating
                            );

                            binding.txtEarnings.setText("Rs. " + earnings);

                            if (rating == 0) {

                                binding.txtRatingText.setText(
                                        "No ratings yet"
                                );

                            } else {

                                binding.txtRatingText.setText(
                                        "Excellent performance"
                                );

                            }

                            // Save cache

                            SharedPreferences.Editor editor =
                                    pref.edit();

                            editor.putString(
                                    "name",
                                    name
                            );

                            editor.putString(
                                    "profile_title",
                                    title
                            );

                            editor.putString(
                                    "profile_availability",
                                    availability
                            );

                            editor.putFloat(
                                    "profile_rating",
                                    rating
                            );

                            editor.putString("profile_earnings", earnings);

                            editor.apply();

                        } catch (Exception e) {

                            e.printStackTrace();

                        }

                    }

                    @Override
                    public void onFailure(
                            Call<ResponseBody> call,
                            Throwable t
                    ) {

                        t.printStackTrace();

                    }

                });

    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;

    }

}