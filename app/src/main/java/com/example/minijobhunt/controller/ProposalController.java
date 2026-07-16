package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class ProposalController {

    public Call<ResponseBody> submitProposal(
            String token,
            RequestBody taskId,
            RequestBody userId,
            RequestBody description,
            RequestBody takesTime,
            MultipartBody.Part achievement
    ) {
        return App.api.submitProposal(
                token,
                taskId,
                userId,
                description,
                takesTime,
                achievement
        );
    }

    public Call<ResponseBody> getProposals(String token) {
        return App.api.getProposals(token);
    }

    public Call<ResponseBody> getProposal(String token, int id) {
        return App.api.getProposal(token, id);
    }

    public Call<ResponseBody> updateProposal(
            String token,
            int id,
            RequestBody method,
            RequestBody description,
            RequestBody takesTime,
            RequestBody status,
            MultipartBody.Part achievement
    ) {
        return App.api.updateProposal(
                token,
                id,
                method,
                description,
                takesTime,
                status,
                achievement
        );
    }

    public Call<ResponseBody> deleteProposal(String token, int id) {
        return App.api.deleteProposal(token, id);
    }
}
