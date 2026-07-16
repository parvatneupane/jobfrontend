package com.example.minijobhunt.fragments;
import com.bumptech.glide.Glide;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import org.json.JSONObject;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.minijobhunt.controller.VerificationController;
import com.example.minijobhunt.databinding.FragmentVerificationBinding;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationFragment extends Fragment {

    private FragmentVerificationBinding binding;

    private Uri frontUri, backUri, panUri;
    private int verificationId = 0;
    private boolean isUpdate = false;

    private static final int FRONT = 1;
    private static final int BACK = 2;
    private static final int PAN = 3;

    private int currentSelection = 0;

    private VerificationController controller;

    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {

                            Uri uri = result.getData().getData();

                            if (currentSelection == FRONT) {
                                frontUri = uri;
                                binding.imgFront.setImageURI(uri);
                                binding.imgFront.setVisibility(View.VISIBLE);

                            } else if (currentSelection == BACK) {
                                backUri = uri;
                                binding.imgBack.setImageURI(uri);
                                binding.imgBack.setVisibility(View.VISIBLE);

                            } else if (currentSelection == PAN) {
                                panUri = uri;
                                binding.imgPan.setImageURI(uri);
                                binding.imgPan.setVisibility(View.VISIBLE);
                            }
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentVerificationBinding.inflate(inflater, container, false);

        controller = new VerificationController();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        binding.btnCitizenshipFront.setOnClickListener(v -> {
            currentSelection = FRONT;
            openGallery();
        });

        binding.btnCitizenshipBack.setOnClickListener(v -> {
            currentSelection = BACK;
            openGallery();
        });

        binding.btnPan.setOnClickListener(v -> {
            currentSelection = PAN;
            openGallery();
        });

        binding.btnSubmitVerification.setOnClickListener(v -> validate());
        loadVerification();
    }


    private void loadVerification() {

        SharedPreferences sp = requireContext()
                .getSharedPreferences(Constants.cache,0);

        String token = "Bearer " + sp.getString("token","");
        int id = sp.getInt("userid",0);

        controller.getVerification(token,id)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {

                        if(response.isSuccessful()){

                            try{

                                String json=response.body().string();

                                JSONObject object=new JSONObject(json);

                                JSONObject data=object.getJSONObject("data");

                                verificationId = data.getInt("id");
                                isUpdate = true;

                                binding.btnSubmitVerification.setText("Update Verification");

                                binding.etFullName.setText(
                                        data.getString("full_name")
                                );

                                String front=data.getString("citizenship_front");
                                String back=data.getString("citizenship_back");
                                String pan=data.getString("pan_card");

                                Glide.with(requireContext())
                                        .load(front)
                                        .into(binding.imgFront);

                                Glide.with(requireContext())
                                        .load(back)
                                        .into(binding.imgBack);

                                Glide.with(requireContext())
                                        .load(pan)
                                        .into(binding.imgPan);

                                binding.imgFront.setVisibility(View.VISIBLE);
                                binding.imgBack.setVisibility(View.VISIBLE);
                                binding.imgPan.setVisibility(View.VISIBLE);

                            }catch(Exception e){
                                e.printStackTrace();
                            }

                        }

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }

                });


    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePicker.launch(intent);
    }

    private void validate() {

        String name = binding.etFullName.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etFullName.setError("Enter full name");
            return;
        }

        if(!isUpdate){

            if(frontUri==null || backUri==null){
                Toast.makeText(requireContext(),
                        "Select required images",
                        Toast.LENGTH_SHORT).show();
                return;
            }

        }

        if(isUpdate){
            update(name);
        }else{
            upload(name);
        }
    }
    private void update(String name){

        SharedPreferences sp = requireContext()
                .getSharedPreferences(Constants.cache,0);

        String token = "Bearer " + sp.getString("token","");

        controller.updateVerification(
                token,
                verificationId,
                toRequest("PUT"),
                toRequest(name),
                filePart(frontUri,"citizenship_front"),
                filePart(backUri,"citizenship_back"),
                filePart(panUri,"pan_card")
        ).enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Updated Successfully", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();

                    try {

                        String json = response.body().string();

                        JSONObject object = new JSONObject(json);
                        JSONObject data = object.getJSONObject("data");

                        // Existing verification found
                        isUpdate = true;
                        verificationId = data.getInt("id");

                        binding.btnSubmitVerification.setText("Update Verification");

                        binding.etFullName.setText(data.getString("full_name"));

                        Glide.with(requireContext())
                                .load(data.getString("citizenship_front"))
                                .into(binding.imgFront);

                        Glide.with(requireContext())
                                .load(data.getString("citizenship_back"))
                                .into(binding.imgBack);

                        Glide.with(requireContext())
                                .load(data.getString("pan_card"))
                                .into(binding.imgPan);

                        binding.imgFront.setVisibility(View.VISIBLE);
                        binding.imgBack.setVisibility(View.VISIBLE);
                        binding.imgPan.setVisibility(View.VISIBLE);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                else if (response.code() == 404) {

                    // No verification exists yet
                    isUpdate = false;
                    verificationId = 0;

                    binding.btnSubmitVerification.setText("Submit Verification");

                }
                else {

                    Toast.makeText(requireContext(),
                            "Error : " + response.code(),
                            Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call,
                                  Throwable t) {

                Toast.makeText(requireContext(),
                        t.getMessage(),
                        Toast.LENGTH_SHORT).show();

            }

        });

    }

    private void upload(String name) {

        SharedPreferences sp = requireContext().getSharedPreferences(com.example.minijobhunt.utils.Constants.cache, 0);
        String token = "Bearer " + sp.getString("token", "");
        int id = sp.getInt("userid", 0);

        if (id == 0) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = String.valueOf(id);

        Call<ResponseBody> call = controller.submitVerification(
                token,
                toRequest(userId),
                toRequest(name),
                filePart(frontUri, "citizenship_front"),
                filePart(backUri, "citizenship_back"),
                filePart(panUri, "pan_card")
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            "Submitted Successfully",
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                    resetForm();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        android.util.Log.e("VERIFY_ERROR", errorBody);
                        Toast.makeText(requireContext(),
                                "Failed (422): " + errorBody,
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(),
                                "Failed: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(),
                        t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RequestBody toRequest(String value) {
        return RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                value
        );
    }

    private MultipartBody.Part filePart(Uri uri, String name) {

        File file = UtilsFunctions.uriToFile(requireContext(), uri);
        if (file == null || !file.exists()) return null;

        RequestBody requestFile = RequestBody.create(

                okhttp3.MediaType.parse("image/jpeg"),
                file
        );

        return MultipartBody.Part.createFormData(name, file.getName(), requestFile);
    }

    private void resetForm() {

        binding.etFullName.setText("");

        frontUri = null;
        backUri = null;
        panUri = null;

        binding.imgFront.setVisibility(View.GONE);
        binding.imgBack.setVisibility(View.GONE);
        binding.imgPan.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}