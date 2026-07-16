package com.example.minijobhunt.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.minijobhunt.controller.ProposalController;
import com.example.minijobhunt.databinding.FragmentFreelancerApplyJobProposalBinding;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreeLancerApplyJobProposalFragment extends Fragment {

    private FragmentFreelancerApplyJobProposalBinding binding;
    private ProposalController controller;
    private int jobId = -1;
    private int proposalId = -1;
    private boolean isUpdate = false;
    private Uri achievementUri;

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            achievementUri = result.getData().getData();
                            binding.txtFileName.setText("File selected: " + achievementUri.getLastPathSegment());
                            
                            String type = requireContext().getContentResolver().getType(achievementUri);
                            if (type != null && type.startsWith("image/")) {
                                binding.imgPreview.setImageURI(achievementUri);
                                binding.imgPreview.setVisibility(View.VISIBLE);
                            } else {
                                binding.imgPreview.setVisibility(View.GONE);
                            }
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFreelancerApplyJobProposalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        controller = new ProposalController();

        setupJobData();

        binding.btnChooseFile.setOnClickListener(v -> openFilePicker());
        binding.btnSubmitProposal.setOnClickListener(v -> submitProposal());
        binding.btnDeleteProposal.setOnClickListener(v -> deleteProposal());
        
        checkExistingProposal();
    }

    private void deleteProposal() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Proposal")
                .setMessage("Are you sure you want to delete this proposal?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
                    String token = "Bearer " + pref.getString("token", "");
                    controller.deleteProposal(token, proposalId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Proposal deleted", Toast.LENGTH_SHORT).show();
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) { }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setupJobData() {
        if (getArguments() != null && getArguments().containsKey("job_data")) {
            try {
                String jobJson = getArguments().getString("job_data");
                JSONObject job = new JSONObject(jobJson);

                jobId = job.getInt("id");
                binding.txtJobTitle.setText(job.getString("title"));
                
                if (job.optJSONObject("category") != null) {
                    binding.txtCategory.setText(job.getJSONObject("category").getString("name"));
                } else {
                    binding.txtCategory.setText("General");
                }
                
                binding.txtBudget.setText("Budget : Rs. " + job.getString("budget"));
                binding.txtDeadline.setText("Deadline : " + job.getString("deadline"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkExistingProposal() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", -1);

        controller.getProposals(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject prop = data.getJSONObject(i);
                            if (prop.getInt("task_id") == jobId && prop.getInt("user_id") == userId) {
                                proposalId = prop.getInt("id");
                                isUpdate = true;
                                binding.btnSubmitProposal.setText("Update Proposal");
                                binding.btnDeleteProposal.setVisibility(View.VISIBLE);
                                binding.etDescription.setText(prop.getString("description"));
                                binding.etTime.setText(prop.getString("takes_time"));
                                
                                String achievement = prop.optString("achievement", "");
                                if (!achievement.isEmpty() && !achievement.equals("null")) {
                                    binding.txtFileName.setText("Previous file: " + achievement.substring(achievement.lastIndexOf("/") + 1));
                                    String imageUrl = Constants.URL + "storage/" + achievement;
                                    if (achievement.toLowerCase().endsWith(".jpg") || achievement.toLowerCase().endsWith(".jpeg") || achievement.toLowerCase().endsWith(".png")) {
                                        Glide.with(requireContext()).load(imageUrl).into(binding.imgPreview);
                                        binding.imgPreview.setVisibility(View.VISIBLE);
                                    }
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*", "application/msword", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePicker.launch(intent);
    }

    private void submitProposal() {
        String description = binding.etDescription.getText().toString().trim();
        String time = binding.etTime.getText().toString().trim();

        if (description.isEmpty() || time.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill description and time", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", -1);

        RequestBody rbTaskId = createPartFromString(String.valueOf(jobId));
        RequestBody rbUserId = createPartFromString(String.valueOf(userId));
        RequestBody rbDescription = createPartFromString(description);
        RequestBody rbTime = createPartFromString(time);

        MultipartBody.Part achievementPart = null;
        if (achievementUri != null) {
            File file = UtilsFunctions.uriToFile(requireContext(), achievementUri);
            if (file != null) {
                String mimeType = requireContext().getContentResolver().getType(achievementUri);
                if (mimeType == null) mimeType = "application/octet-stream";
                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
                achievementPart = MultipartBody.Part.createFormData("achievement", file.getName(), requestFile);
            }
        }

        Call<ResponseBody> call;
        if (isUpdate) {
            RequestBody rbMethod = createPartFromString("PUT");
            RequestBody rbStatus = createPartFromString("pending");
            call = controller.updateProposal(token, proposalId, rbMethod, rbDescription, rbTime, rbStatus, achievementPart);
        } else {
            call = controller.submitProposal(token, rbTaskId, rbUserId, rbDescription, rbTime, achievementPart);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String msg = isUpdate ? "Proposal updated successfully" : "Proposal submitted successfully";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
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

    private RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(MultipartBody.FORM, descriptionString);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
