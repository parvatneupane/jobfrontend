package com.example.minijobhunt.controller;

import android.util.Log;

import com.example.minijobhunt.model.RegisterResponse;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.views.ClientFreeLancerRegisterActivity;

import java.util.HashMap;
import java.util.Map;

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

        String name =
                view.getName();

        String email =
                view.getEmail();

        String password =
                view.getPassword();

        String confirm =
                view.getPasswordConformation();

        String role =
                view.getRole();

        if (name.isEmpty()) {

            view.showError(
                    "Name is required"
            );

            return;

        }

        if (email.isEmpty()) {

            view.showError(
                    "Email required"
            );

            return;

        }

        if (password.isEmpty()) {

            view.showError(
                    "Password required"
            );

            return;

        }

        if (!password.equals(confirm)) {

            view.showError(
                    "Password mismatch"
            );

            return;

        }

        registration(
                name,
                email,
                password,
                confirm,
                role
        );

    }

    public void registration(
            String name,
            String email,
            String password,
            String confirm,
            String role
    ) {

        Map<String, String> body =
                new HashMap<>();

        body.put(
                "name",
                name
        );

        body.put(
                "email",
                email
        );

        body.put(
                "password",
                password
        );

        body.put(
                "password_confirmation",
                confirm
        );

        body.put(
                "role",
                role
        );

        App.api
                .register(body)
                .enqueue(
                        new Callback<RegisterResponse>() {

                            @Override
                            public void onResponse(
                                    Call<RegisterResponse> call,
                                    Response<RegisterResponse> response
                            ) {

                                if (
                                        response.isSuccessful()
                                                &&
                                                response.body() != null
                                ) {

                                    RegisterResponse resp =
                                            response.body();

                                    if (
                                            resp.isSuccess()
                                    ) {

                                        view.showSuccess(
                                                resp.getMessage()
                                        );

                                        view.showLogin();

                                    }

                                    else {

                                        view.showError(
                                                resp.getMessage()
                                        );

                                    }

                                }

                                else {

                                    view.showError(
                                            "Registration failed"
                                    );

                                }

                            }

                            @Override
                            public void onFailure(
                                    Call<RegisterResponse> call,
                                    Throwable t
                            ) {

                                view.showError(
                                        t.getMessage()
                                );

                                Log.e(
                                        "REGISTER",
                                        t.toString()
                                );

                            }

                        });

    }

}