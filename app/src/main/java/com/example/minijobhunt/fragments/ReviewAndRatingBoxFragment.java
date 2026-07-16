package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ReviewController;
import com.example.minijobhunt.databinding.FragmentReviewAndRatingBoxBinding;
import com.example.minijobhunt.utils.Constants;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewAndRatingBoxFragment extends Fragment {

    private FragmentReviewAndRatingBoxBinding binding;
    private ReviewController reviewController;
    private int contractId, taskId, freelancerId, clientId;
    private String freelancerName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReviewAndRatingBoxBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reviewController = new ReviewController();

        if (getArguments() != null) {
            contractId = getArguments().getInt("contract_id", -1);
            taskId = getArguments().getInt("task_id", -1);
            freelancerId = getArguments().getInt("freelancer_id", -1);
            clientId = getArguments().getInt("client_id", -1);
            freelancerName = getArguments().getString("freelancer_name", "Freelancer");

            binding.txtFreelancerName.setText(freelancerName);
        }

        binding.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (rating <= 1) binding.txtRatingStatus.setText("Poor 😞");
            else if (rating <= 2) binding.txtRatingStatus.setText("Fair 😐");
            else if (rating <= 3) binding.txtRatingStatus.setText("Good 🙂");
            else if (rating <= 4) binding.txtRatingStatus.setText("Very Good 😊");
            else binding.txtRatingStatus.setText("Excellent! 🤩");
        });

        binding.btnSkip.setOnClickListener(v -> closeFragment());

        binding.btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        float rating = binding.ratingBar.getRating();
        String review = binding.edtReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (review.isEmpty()) {
            Toast.makeText(requireContext(), "Please write a review", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        Map<String, Object> body = new HashMap<>();
        body.put("contract_id", contractId);
        body.put("task_id", taskId);
        body.put("client_id", clientId);
        body.put("freelancer_id", freelancerId);
        body.put("rating", (int) rating);
        body.put("review", review);
        body.put("recommended", binding.chkRecommend.isChecked());

        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setText("Submitting...");

        reviewController.submitReview(token, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    closeFragment();
                } else {
                    binding.btnSubmit.setEnabled(true);
                    binding.btnSubmit.setText("Submit");
                    Toast.makeText(requireContext(), "Failed to submit review", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                binding.btnSubmit.setEnabled(true);
                binding.btnSubmit.setText("Submit");
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closeFragment() {
        if (isAdded()) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
