package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class PaymentController {

    public Call<ResponseBody> getPayments(String token) {
        return App.api.getPayments(token);
    }

    public Call<ResponseBody> storePayment(String token, Map<String, Object> body) {
        return App.api.storePayment(token, body);
    }

    public Call<ResponseBody> getPayment(String token, int id) {
        return App.api.getPayment(token, id);
    }

    public Call<ResponseBody> updatePayment(String token, int id, Map<String, Object> body) {
        return App.api.updatePayment(token, id, body);
    }

    public Call<ResponseBody> deletePayment(String token, int id) {
        return App.api.deletePayment(token, id);
    }

    public Call<ResponseBody> releasePayment(String token, int id) {
        return App.api.releasePayment(token, id);
    }

    public Call<ResponseBody> refundPayment(String token, int id) {
        return App.api.refundPayment(token, id);
    }
}
