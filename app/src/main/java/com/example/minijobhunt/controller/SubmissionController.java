package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class SubmissionController {

    public Call<ResponseBody> submitWork(
            String token,
            RequestBody contractId,
            RequestBody freelancerId,
            RequestBody message,
            java.util.List<okhttp3.MultipartBody.Part> attachments
    ) {
        return App.api.submitWork(token, contractId, freelancerId, message, attachments);
    }

    public Call<ResponseBody> getSubmissions(String token) {
        return App.api.getSubmissions(token);
    }

    public Call<ResponseBody> getSubmission(String token, int id) {
        return App.api.getSubmission(token, id);
    }

    public Call<ResponseBody> updateSubmission(String token, int id, Map<String, Object> body) {
        return App.api.updateSubmission(token, id, body);
    }

    public Call<ResponseBody> deleteSubmission(String token, int id) {
        return App.api.deleteSubmission(token, id);
    }
}
