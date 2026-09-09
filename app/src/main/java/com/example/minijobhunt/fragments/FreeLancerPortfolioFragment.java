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

import com.example.minijobhunt.databinding.FragmentFreeLancerPortfolioBinding;
import com.example.minijobhunt.controller.ProfileController;
import com.example.minijobhunt.controller.ReviewController;
import com.example.minijobhunt.adapter.ReviewAdapter;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;

import androidx.recyclerview.widget.LinearLayoutManager;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreeLancerPortfolioFragment extends Fragment {

    private FragmentFreeLancerPortfolioBinding binding;
    private ProfileController profileController;
    private ReviewController reviewController;
    private int targetProfileId = -1;
    private int targetUserId = -1;
    private int targetUserIdForReviews = -1;
    private String targetUserName = "";

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFreeLancerPortfolioBinding.inflate(
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
        profileController = new ProfileController();
        reviewController = new ReviewController();

        if (getArguments() != null) {
            targetProfileId = getArguments().getInt("profile_id", -1);
            targetUserId = getArguments().getInt("user_id", -1);
            targetUserName = getArguments().getString("user_name", "");
            if (!targetUserName.isEmpty()) {
                binding.txtName.setText(targetUserName);
                binding.txtAvatar.setText(String.valueOf(targetUserName.charAt(0)));
            }
            
            // Show back button if we are viewing someone else's profile
            binding.btnBack.setVisibility(View.VISIBLE);
            binding.btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        binding.rvReviews.setLayoutManager(new LinearLayoutManager(getContext()));

        loadProfile();
        loadReviews();
    }

    private void checkVerificationStatus(int userId) {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        
        App.api.getVerification(token, userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                JSONObject root = new JSONObject(response.body().string());
                                JSONObject data = root.getJSONObject("data");
                                String status = data.optString("status", "").toLowerCase();

                                if (status.equals("approved")) {
                                    binding.imgVerifiedBadge.setVisibility(View.VISIBLE);
                                } else {
                                    binding.imgVerifiedBadge.setVisibility(View.GONE);
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

    private void loadProfile() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        Call<ResponseBody> call;
        if (targetUserId != -1) {
            call = profileController.getFreelancerProfileByUserId(token, targetUserId);
        } else if (targetProfileId != -1) {
            call = profileController.getFreelancerProfile(token, targetProfileId);
        } else {
            call = profileController.getMyProfile(token);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(
                    Call<ResponseBody> call,
                    Response<ResponseBody> response
            ) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        return;
                    }

                    JSONObject root = new JSONObject(response.body().string());
                    Object data = root.opt("data");
                    JSONObject profile = null;

                    if (data instanceof JSONArray) {
                        JSONArray array = (JSONArray) data;
                        if (array.length() > 0) profile = array.getJSONObject(0);
                    } else if (data instanceof JSONObject) {
                        profile = (JSONObject) data;
                    }

                    if (profile == null) {
                        return;
                    }

                    JSONObject user = profile.optJSONObject("user");

                    //----------------------------------
                    // User Information
                    //----------------------------------

                    if (user != null) {
                        String name = user.optString("name", "");
                        binding.txtName.setText(name);
                        if (!name.isEmpty()) {
                            binding.txtAvatar.setText(String.valueOf(name.charAt(0)));
                        }
                        
                        targetUserIdForReviews = user.optInt("id", -1);
                        checkVerificationStatus(targetUserIdForReviews);
                        
                        // Reload reviews if we just found the user ID
                        loadReviews();
                    } else if (!targetUserName.isEmpty()) {
                        binding.txtName.setText(targetUserName);
                        binding.txtAvatar.setText(String.valueOf(targetUserName.charAt(0)));
                    }

                    //----------------------------------
                    // Profile Information
                    //----------------------------------

                    binding.txtTitle.setText(profile.optString("title", "No Title"));
                    binding.txtStatus.setText(profile.optString("availability", "Available"));
                    binding.txtBio.setText(profile.optString("bio", "No bio added."));
                    binding.txtExperience.setText("Experience : " + profile.optInt("experience_years") + " Years");
                    binding.txtLocation.setText("Location : " + profile.optString("location", "-"));
                    binding.txtAvailability.setText("Availability : " + profile.optString("availability", "-"));
                    
                    // Display Skills
                    binding.txtSkills.setText(profile.optString("skills", "-"));

                    // Display Categories in Header
                    JSONArray categories = profile.optJSONArray("categories");
                    if (categories != null && categories.length() > 0) {
                        StringBuilder catBuilder = new StringBuilder();
                        for (int i = 0; i < categories.length(); i++) {
                            JSONObject cat = categories.getJSONObject(i);
                            catBuilder.append(cat.optString("name"));
                            if (i < categories.length() - 1) catBuilder.append(", ");
                        }
                        binding.txtCategories.setText(catBuilder.toString());
                        binding.txtCategories.setVisibility(View.VISIBLE);
                        binding.ratingDivider.setVisibility(View.VISIBLE);
                    } else {
                        binding.txtCategories.setVisibility(View.GONE);
                        binding.ratingDivider.setVisibility(View.GONE);
                    }
                    
                    binding.txtPortfolio.setText(profile.optString("portfolio_url", "-"));
                    String rating = profile.optString("rating", "0");
                    binding.txtRating.setText(rating);
                    binding.txtRatingSmall.setText(rating);
                    binding.txtCompletedJobs.setText(profile.optString("completed_jobs", "0"));

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

    private void loadReviews() {
        if (targetUserIdForReviews == -1) {
            if (targetUserId != -1) {
                targetUserIdForReviews = targetUserId;
            } else if (targetProfileId == -1) {
                // Try to get from pref if it's my own profile
                SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
                targetUserIdForReviews = pref.getInt("userid", 0);
            }
        }

        if (targetUserIdForReviews <= 0) return;

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        reviewController.getFreelancerReviews(token, targetUserIdForReviews)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                JSONObject root = new JSONObject(response.body().string());
                                JSONArray data = root.getJSONArray("data");

                                if (data.length() > 0) {
                                    binding.rvReviews.setVisibility(View.VISIBLE);
                                    binding.txtNoReviews.setVisibility(View.GONE);
                                    ReviewAdapter adapter = new ReviewAdapter(data);
                                    binding.rvReviews.setAdapter(adapter);

                                    if (data.length() > 1) {
                                        binding.btnShowAllReviews.setVisibility(View.VISIBLE);
                                        binding.btnShowAllReviews.setOnClickListener(v -> {
                                            adapter.setShowAll(true);
                                            binding.btnShowAllReviews.setVisibility(View.GONE);
                                        });
                                    } else {
                                        binding.btnShowAllReviews.setVisibility(View.GONE);
                                    }
                                } else {
                                    binding.rvReviews.setVisibility(View.GONE);
                                    binding.txtNoReviews.setVisibility(View.VISIBLE);
                                    binding.btnShowAllReviews.setVisibility(View.GONE);
                                }
                            } else {
                                android.util.Log.e("Portfolio", "Error: " + response.code());
                            }
                        } catch (Exception e) {
                            android.util.Log.e("Portfolio", "Parsing error", e);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        android.util.Log.e("Portfolio", "Network failure", t);
                    }
                });
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;

    }
}