package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class ProfileController {

    public Call<ResponseBody> getMyProfile(String token) {
        return App.api.getMyProfile(token);
    }

    public Call<ResponseBody> getFreelancerProfile(String token, int profileId) {
        return App.api.getFreelancerProfile(token, profileId);
    }

    public Call<ResponseBody> getFreelancers(String token, String search) {
        return App.api.getFreelancers(token, search);
    }

    public Call<ResponseBody> getCategories(String token) {
        return App.api.getCategories(token);
    }
}
