package com.example.minijobhunt.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minijobhunt.controller.RegistrationController;
import com.example.minijobhunt.databinding.ActivityClientFreeLancerRegisterBinding;

public class ClientFreeLancerRegisterActivity
        extends AppCompatActivity {

    ActivityClientFreeLancerRegisterBinding binding;

    RegistrationController controller;

    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding =
                ActivityClientFreeLancerRegisterBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        role =
                getIntent()
                        .getStringExtra(
                                "ROLE"
                        );

        if (role == null) {
            role = "client";
        }

        controller =
                new RegistrationController(
                        this
                );

        binding.txtBack.setOnClickListener(v -> {
            finish();
        });

        binding.btnRegister.setOnClickListener(v -> {
            controller.validation();
        });

    }

    public String getRole() {
        return role;
    }

    public String getName() {

        return binding.etName
                .getText()
                .toString()
                .trim();

    }

    public String getEmail() {

        return binding.etEmail
                .getText()
                .toString()
                .trim();

    }

    public String getPassword() {

        return binding.etPassword
                .getText()
                .toString()
                .trim();

    }

    public String getPasswordConformation() {

        return binding.etConfirmPassword
                .getText()
                .toString()
                .trim();

    }

    public void showError(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();

    }

    public void showSuccess(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();

    }

    public void showLogin() {

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        startActivity(
                intent
        );

        finish();

    }

}