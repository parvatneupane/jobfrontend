package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class ReviewController {

    public Call<ResponseBody> getReviews(String token) {
        return App.api.getReviews(token, null);
    }

    public Call<ResponseBody> submitReview(String token, Map<String, Object> body) {
        return App.api.submitReview(token, body);
    }

    public Call<ResponseBody> getReview(String token, int id) {
        return App.api.getReview(token, id);
    }

    public Call<ResponseBody> updateReview(String token, int id, Map<String, Object> body) {
        return App.api.updateReview(token, id, body);
    }

    public Call<ResponseBody> deleteReview(String token, int id) {
        return App.api.deleteReview(token, id);
    }

    public Call<ResponseBody> getFreelancerReviews(String token, int freelancerId) {
        return App.api.getFreelancerReviews(token, freelancerId);
    }
}
