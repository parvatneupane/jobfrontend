package com.example.minijobhunt.controller;

import android.util.Log;

import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.views.ClientFreeLancerRegisterActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationController {

    private ClientFreeLancerRegisterActivity view;

    public RegistrationController(
            ClientFreeLancerRegisterActivity view
    ) {
        this.view = view;
    }

    public void validation() {
        String name = view.getName();
        String email = view.getEmail();
        String password = view.getPassword();
        String confirm = view.getPasswordConformation();
        String role = view.getRole();

        if (name.isEmpty()) {
            view.showError("Name is required");
            return;
        }
        if (email.isEmpty()) {
            view.showError("Email required");
            return;
        }
        if (password.isEmpty()) {
            view.showError("Password required");
            return;
        }
        if (!password.equals(confirm)) {
            view.showError("Password mismatch");
            return;
        }

        registration(name, email, password, confirm, role);
    }

    public void registration(
            String name,
            String email,
            String password,
            String confirm,
            String role
    ) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("password_confirmation", confirm);
        body.put("role", role);

        App.api.register(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseString = "";
                    if (response.isSuccessful() && response.body() != null) {
                        responseString = response.body().string();
                    } else if (response.errorBody() != null) {
                        responseString = response.errorBody().string();
                    }

                    Log.d("REGISTER", "Raw: " + responseString);

                    String normalized = responseString.trim().toLowerCase();
                    
                    // If server returns "ok" or "success" even in errorBody, treat as success
                    if (response.isSuccessful() || normalized.equals("ok") || normalized.equals("success")) {
                        String message = "Registration successful";
                        try {
                            if (normalized.startsWith("{")) {
                                JSONObject json = new JSONObject(responseString);
                                message = json.optString("message", message);
                                if (json.has("success") && !json.optBoolean("success")) {
                                    view.showError(message);
                                    return;
                                }
                            }
                        } catch (Exception e) { }
                        
                        view.showSuccess(message);
                        view.showLogin();
                    } else {
                        String errorMsg = "Registration failed";
                        try {
                            if (normalized.startsWith("{")) {
                                JSONObject json = new JSONObject(responseString);
                                if (json.has("errors")) {
                                    JSONObject errors = json.getJSONObject("errors");
                                    if (errors.has("email")) {
                                        errorMsg = errors.getJSONArray("email").getString(0);
                                    } else {
                                        errorMsg = json.optString("message", errorMsg);
                                    }
                                } else {
                                    errorMsg = json.optString("message", errorMsg);
                                }
                            } else if (!normalized.isEmpty()) {
                                errorMsg = responseString;
                            }
                        } catch (Exception e) {
                            errorMsg = responseString;
                        }
                        view.showError(errorMsg);
                    }
                } catch (Exception e) {
                    Log.e("REGISTER", "Error", e);
                    view.showSuccess("Registration completed");
                    view.showLogin();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("REGISTER", "Failure", t);
                view.showError("Connection error: " + t.getMessage());
            }
        });
    }
}
