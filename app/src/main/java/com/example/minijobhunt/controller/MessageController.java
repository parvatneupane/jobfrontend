package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class MessageController {

    public Call<ResponseBody> sendMessage(String token, Map<String, Object> body) {
        return App.api.sendMessage(token, body);
    }
}
