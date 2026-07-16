package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class ChatController {

    public Call<ResponseBody> createChat(String token, Map<String, Object> body) {
        return App.api.createChat(token, body);
    }

    public Call<ResponseBody> getChats(String token) {
        return App.api.getChats(token);
    }

    public Call<ResponseBody> getChatMessages(String token, int chatId) {
        return App.api.getChatMessages(token, chatId);
    }

    public Call<ResponseBody> sendMessage(String token, Map<String, Object> body) {
        return App.api.sendMessage(token, body);
    }
}
