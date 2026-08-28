package com.example.minijobhunt.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.views.LoginActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginController {


    private LoginActivity view;

    public LoginController(LoginActivity view) {
        this.view = view;
    }

    public void validation() {

        String email =
                view.binding.etEmail
                        .getText()
                        .toString()
                        .trim();

        String password =
                view.binding.etPassword
                        .getText()
                        .toString()
                        .trim();

        if(email.isEmpty()){

            view.showError(
                    "Email Required"
            );

        }
        else if(password.isEmpty()){

            view.showError(
                    "Password Required"
            );

        }
        else{

            login(
                    email,
                    password
            );

        }

    }


    public void login(
            String email,
            String password
    ){

        Map<String,String> body=
                new HashMap<>();

        body.put(
                "email",
                email
        );

        body.put(
                "password",
                password
        );


        App.api
                .login(body)
                .enqueue(
                        new Callback<ResponseBody>() {

                            @Override
                            public void onResponse(
                                    Call<ResponseBody> call,
                                    Response<ResponseBody> response
                            ) {

                                try {
                                    if (response.isSuccessful() && response.body() != null) {
                                        String raw = response.body().string();
                                        JSONObject json = new JSONObject(raw);

                                        if (json.optBoolean("success", false)) {
                                            String token = json.optString("token");
                                            JSONObject user = json.getJSONObject("user");

                                            saveData(
                                                    token,
                                                    user.getInt("id"),
                                                    user.optString("name"),
                                                    user.optString("email"),
                                                    user.optString("role")
                                            );

                                            view.loginSuccessful();
                                        } else {
                                            view.showError(json.optString("message", "Invalid credentials"));
                                        }
                                    } else {
                                        String errorMsg = "Login failed";
                                        if (response.errorBody() != null) {
                                            String err = response.errorBody().string();
                                            try {
                                                JSONObject errJson = new JSONObject(err);
                                                errorMsg = errJson.optString("message", err);
                                            } catch (Exception e) {
                                                errorMsg = err;
                                            }
                                        }
                                        view.showError(errorMsg);
                                    }
                                } catch (Exception e) {
                                    Log.e("LOGIN", "Error", e);
                                    view.showError("Server response error");
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<ResponseBody> call,
                                    Throwable t) {
                                Log.e("LOGIN", t.getMessage());
                                view.showError("Connection error");
                            }

                        });

    }

    public void logout(String token) {
        App.api.logout("Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // Even if the server fails to logout, we clear local data
                clearData();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                clearData();
            }
        });
    }

    private void clearData() {
        SharedPreferences pref = view.getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        pref.edit().clear().apply();
        view.startActivity(new android.content.Intent(view, LoginActivity.class));
        view.finishAffinity();
    }

    private void saveData(

            String token,

            int id,

            String name,

            String email,

            String role

    ){

        SharedPreferences pref =
                view.getSharedPreferences(
                        Constants.cache,
                        Context.MODE_PRIVATE
                );

        SharedPreferences.Editor editor =
                pref.edit();

        // User
        editor.putString(
                "token",
                token
        );

        editor.putInt(
                "userid",
                id
        );

        editor.putString(
                "name",
                name
        );

        editor.putString(
                "email",
                email
        );

        editor.putString(
                "role",
                role
        );

        // Profile cache (empty until fetched)
        editor.putInt(
                "profile_id",
                0
        );

        editor.putString(
                "profile_title",
                ""
        );

        editor.putString(
                "profile_bio",
                ""
        );

        editor.putString(
                "profile_skills",
                ""
        );

        editor.putString(
                "profile_location",
                ""
        );

        editor.putString(
                "profile_portfolio",
                ""
        );

        editor.putString(
                "profile_availability",
                ""
        );

        editor.putInt(
                "profile_experience",
                0
        );

        editor.apply();

    }


}
