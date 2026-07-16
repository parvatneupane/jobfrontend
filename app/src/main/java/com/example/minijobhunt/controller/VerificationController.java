package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class VerificationController {

    // ===========================
    // CREATE VERIFICATION (POST)
    // ===========================
    public Call<ResponseBody> submitVerification(
            String token,
            RequestBody userId,
            RequestBody fullName,
            MultipartBody.Part front,
            MultipartBody.Part back,
            MultipartBody.Part pan
    ) {

        return App.api.submitVerification(
                token,
                userId,
                fullName,
                front,
                back,
                pan
        );
    }

    // ===========================
    // GET VERIFICATION BY USER ID
    // ===========================
    public Call<ResponseBody> getVerification(
            String token,
            int userId
    ) {

        return App.api.getVerification(
                token,
                userId
        );
    }

    // ===========================
    // UPDATE VERIFICATION (PUT via POST)
    // ===========================
    public Call<ResponseBody> updateVerification(
            String token,
            int verificationId,
            RequestBody method,
            RequestBody fullName,
            MultipartBody.Part front,
            MultipartBody.Part back,
            MultipartBody.Part pan
    ) {

        return App.api.updateVerification(
                token,
                verificationId,
                method,
                fullName,
                front,
                back,
                pan
        );
    }

}