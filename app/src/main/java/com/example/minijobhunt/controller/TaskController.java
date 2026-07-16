package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class TaskController {

    public Call<ResponseBody> getCategories(String token) {
        return App.api.getCategories(token);
    }

    public Call<ResponseBody> getTasks(String token) {
        return App.api.getTasks(token);
    }

    public Call<ResponseBody> createTask(String token, Map<String, Object> body) {
        return App.api.createTask(token, body);
    }

    public Call<ResponseBody> getTask(String token, int id) {
        return App.api.getTask(token, id);
    }

    public Call<ResponseBody> updateTask(String token, int id, Map<String, Object> body) {
        return App.api.updateTask(token, id, body);
    }

    public Call<ResponseBody> deleteTask(String token, int id) {
        return App.api.deleteTask(token, id);
    }
}
