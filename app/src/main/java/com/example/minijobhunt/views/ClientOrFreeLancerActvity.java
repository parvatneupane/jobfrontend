package com.example.minijobhunt.views;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minijobhunt.databinding.ActivityClientOrFreeLancerActvityBinding;

public class ClientOrFreeLancerActvity extends AppCompatActivity {

    private ActivityClientOrFreeLancerActvityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding =
                ActivityClientOrFreeLancerActvityBinding
                        .inflate(
                                getLayoutInflater()
                        );

        setContentView(
                binding.getRoot()
        );

        binding.FreeLancerButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            ClientFreeLancerRegisterActivity.class
                    );

            intent.putExtra(
                    "ROLE",
                    "freelancer"
            );

            startActivity(
                    intent
            );

        });

        binding.ClientButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            ClientFreeLancerRegisterActivity.class
                    );

            intent.putExtra(
                    "ROLE",
                    "client"
            );

            startActivity(
                    intent
            );

        });

    }

}