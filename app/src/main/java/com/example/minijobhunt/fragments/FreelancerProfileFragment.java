package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.minijobhunt.R;
import com.example.minijobhunt.databinding.FragmentFreelancerProfileBinding;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.views.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreelancerProfileFragment extends Fragment {

    private FragmentFreelancerProfileBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFreelancerProfileBinding.inflate(
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

        loadProfile();
        checkVerificationStatus();

        binding.btnEditProfile.setOnClickListener(v -> {

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new FreeLancerProfileEditFragment()
                    )
                    .addToBackStack(null)
                    .commit();

        });

        // Open Portfolio Fragment
        binding.btnPortfolio.setOnClickListener(v -> {

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new FreeLancerPortfolioFragment()
                    )
                    .addToBackStack(null)
                    .commit();

        });

        // Open Verification Fragment
        binding.btnVerify.setOnClickListener(v -> {

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new VerificationFragment()
                    )
                    .addToBackStack(null)
                    .commit();

        });

        binding.btnLogout.setOnClickListener(v -> logout());

        binding.btnPaymentHistory.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.minijobhunt.views.PaymentHistoryActivity.class);
            startActivity(intent);
        });

        binding.btnActiveInProgressJobs.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FreelancerInProgressJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnActiveCompletedJobs.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FreeLancerCompletedJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void logout() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = pref.getString("token", "");

        App.api.logout("Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                performLocalLogout();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                performLocalLogout();
            }
        });
    }

    private void performLocalLogout() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        pref.edit().clear().apply();

        android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void loadProfile() {

        SharedPreferences pref =
                requireActivity().getSharedPreferences(
                        Constants.cache,
                        Context.MODE_PRIVATE
                );

        //--------------------------------
        // Show cached data immediately
        //--------------------------------

        String name =
                pref.getString("name", "");

        String title =
                pref.getString("profile_title", "");

        String availability =
                pref.getString("profile_availability", "");

        String earnings =
                pref.getString("profile_earnings", "0");

        if (!name.isEmpty()) {

            binding.txtUsername.setText(name);

            binding.txtAvatar.setText(
                    String.valueOf(name.charAt(0))
            );

        }

        if (!title.isEmpty()) {

            binding.txtTitle.setText(title);

        }

        if (!availability.isEmpty()) {

            binding.txtAvailability.setText(availability);

        }

        binding.txtEarnings.setText("Rs. " + earnings);

        //--------------------------------
        // Refresh from server
        //--------------------------------

        String token =
                pref.getString("token", "");

        App.api.getMyProfile(
                        "Bearer " + token
                )
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

                            JSONObject root =
                                    new JSONObject(
                                            response.body().string()
                                    );

                            JSONObject profile =
                                    root.getJSONObject("data");

                            JSONObject user =
                                    profile.getJSONObject("user");

                            String name =
                                    user.optString("name");

                            String email =
                                    user.optString("email");

                            String title =
                                    profile.optString(
                                            "title",
                                            ""
                                    );

                            String availability =
                                    profile.optString(
                                            "availability",
                                            ""
                                    );

                            String earnings =
                                    profile.optString(
                                            "earned_money",
                                            "0"
                                    );

                            //--------------------------------
                            // Update UI
                            //--------------------------------

                            binding.txtUsername.setText(name);

                            binding.txtTitle.setText(title);

                            binding.txtAvailability.setText(
                                    availability
                            );

                            binding.txtEarnings.setText("Rs. " + earnings);

                            if (!name.isEmpty()) {

                                binding.txtAvatar.setText(
                                        String.valueOf(
                                                name.charAt(0)
                                        )
                                );

                            }

                            //--------------------------------
                            // Save latest data
                            //--------------------------------

                            SharedPreferences.Editor editor =
                                    pref.edit();

                            editor.putString(
                                    "name",
                                    name
                            );

                            editor.putString(
                                    "email",
                                    email
                            );

                            editor.putInt(
                                    "profile_id",
                                    profile.optInt("id")
                            );

                            editor.putString(
                                    "profile_title",
                                    title
                            );

                            editor.putString(
                                    "profile_bio",
                                    profile.optString("bio")
                            );

                            editor.putString(
                                    "profile_skills",
                                    profile.optString("skills")
                            );

                            editor.putString(
                                    "profile_location",
                                    profile.optString("location")
                            );

                            editor.putString(
                                    "profile_portfolio",
                                    profile.optString("portfolio_url")
                            );

                            editor.putString(
                                    "profile_availability",
                                    availability
                            );

                            editor.putInt(
                                    "profile_experience",
                                    profile.optInt("experience_years")
                            );

                            editor.putString(
                                    "profile_earnings",
                                    earnings
                            );

                            JSONArray categories = profile.optJSONArray("categories");
                            if (categories != null) {
                                editor.putString("profile_categories", categories.toString());
                            }

                            editor.apply();

                        }

                        catch (Exception e) {

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

        loadProfile();
        checkVerificationStatus();
    }

    private void checkVerificationStatus() {

        SharedPreferences pref =
                requireActivity().getSharedPreferences(
                        Constants.cache,
                        Context.MODE_PRIVATE
                );

        String token = pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        App.api.getVerification("Bearer " + token, userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                JSONObject root = new JSONObject(response.body().string());
                                JSONObject data = root.getJSONObject("data");
                                String status = data.optString("status", "pending").toLowerCase();
                                String date = data.optString("updated_at", "");

                                binding.txtVerificationStatus.setText("Status • " + status.substring(0, 1).toUpperCase() + status.substring(1));

                                if (status.equals("approved")) {
                                    // Set color to Green
                                    int green = ContextCompat.getColor(requireContext(), R.color.success);
                                    binding.txtVerificationStatus.setTextColor(green);
                                    binding.txtVerificationDate.setTextColor(green);
                                    
                                    binding.imgVerifiedBadge.setVisibility(View.VISIBLE);

                                    if (!date.isEmpty()) {
                                        binding.txtVerificationDate.setText("Verified on: " + date.split("T")[0]);
                                        binding.txtVerificationDate.setVisibility(View.VISIBLE);
                                    }

                                    binding.btnVerify.setText("Verified");
                                    binding.btnVerify.setEnabled(false);
                                    binding.btnVerify.setAlpha(0.6f);
                                } else if (status.equals("pending")) {
                                    binding.txtVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
                                    binding.txtVerificationDate.setVisibility(View.GONE);
                                    binding.imgVerifiedBadge.setVisibility(View.GONE);
                                    
                                    binding.btnVerify.setText("Pending");
                                    binding.btnVerify.setEnabled(false);
                                    binding.btnVerify.setAlpha(0.6f);
                                } else if (status.equals("rejected")) {
                                    binding.txtVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
                                    binding.txtVerificationDate.setVisibility(View.GONE);
                                    binding.imgVerifiedBadge.setVisibility(View.GONE);

                                    binding.btnVerify.setText("Re-verify");
                                    binding.btnVerify.setEnabled(true);
                                    binding.btnVerify.setAlpha(1.0f);
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

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;

    }

}