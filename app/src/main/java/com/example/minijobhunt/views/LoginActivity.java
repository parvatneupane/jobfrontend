package com.example.minijobhunt.views;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.LoginController;
import com.example.minijobhunt.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    public ActivityLoginBinding binding;

    private LoginController controller;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        EdgeToEdge.enable(this);

        binding =
                ActivityLoginBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );


        controller =
                new LoginController(
                        this
                );


        binding.txtBack
                .setOnClickListener(v -> {

                    finish();

                });



        binding.txtRegister
                .setOnClickListener(v -> {

                    startActivity(

                            new Intent(
                                    LoginActivity.this,
                                    ClientOrFreeLancerActvity.class
                            )

                    );

                });



        binding.btnLogin
                .setOnClickListener(v -> {

                    controller.validation();

                });



    /*
    binding.txtForgot.setOnClickListener(v -> {

        startActivity(
                new Intent(
                        LoginClient.this,
                        ForgetPassword.class
                )
        );

    });
    */

    }




    public void loginSuccessful() {

        sharedPreferences =
                getSharedPreferences(
                        "shared_preference_cache",
                        MODE_PRIVATE
                );

        Log.i(
                "token",
                sharedPreferences.getString(
                        "token",
                        ""
                )
        );

        String role =
                sharedPreferences.getString(
                        "role",
                        ""
                );

        Intent intent;

        if ("client".equalsIgnoreCase(role)) {

            intent =
                    new Intent(
                            this,
                            MainActivity.class
                    );

        } else if ("freelancer".equalsIgnoreCase(role)) {

            intent =
                    new Intent(
                            this,
                            MainActivity2.class
                    );

        } else {

            Toast.makeText(
                    this,
                    "Unknown user role",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        startActivity(intent);

        finish();
    }



    public void showError(
            String msg
    ){

        Toast.makeText(
                this,
                msg,
                Toast.LENGTH_SHORT
        ).show();

    }
}