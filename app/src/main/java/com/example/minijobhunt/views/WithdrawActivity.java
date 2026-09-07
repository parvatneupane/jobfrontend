package com.example.minijobhunt.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.minijobhunt.R;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WithdrawActivity extends AppCompatActivity {

    private EditText etMobileNumber, etAmount;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private TextView txtBalance;
    private String balanceStr = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etMobileNumber = findViewById(R.id.etMobileNumber);
        etAmount = findViewById(R.id.etAmount);
        btnSubmit = findViewById(R.id.btnSubmitWithdrawal);
        progressBar = findViewById(R.id.progressBar);
        txtBalance = findViewById(R.id.txtBalance);

        loadBalanceFromCache();

        btnSubmit.setOnClickListener(v -> attemptWithdrawal());
    }

    private void loadBalanceFromCache() {
        SharedPreferences pref = getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        balanceStr = pref.getString("profile_earnings", "0");
        txtBalance.setText("Available Balance: Rs. " + balanceStr);
    }

    private void attemptWithdrawal() {
        String mobile = etMobileNumber.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (mobile.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        double currentBalance = Double.parseDouble(balanceStr);

        if (amount > currentBalance) {
            Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount < 10) {
            Toast.makeText(this, "Minimum withdrawal amount is Rs. 10", Toast.LENGTH_SHORT).show();
            return;
        }

        submitWithdrawalRequest(mobile, amount);
    }

    private void submitWithdrawalRequest(String mobile, double amount) {
        btnSubmit.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences pref = getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        String name = pref.getString("name", "Freelancer");

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("account_number", mobile);
        body.put("account_name", name);

        App.api.withdrawEarnings(token, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setVisibility(View.VISIBLE);

                if (response.isSuccessful()) {
                    Toast.makeText(WithdrawActivity.this, "Withdrawal request submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject error = new JSONObject(errorBody);
                        Toast.makeText(WithdrawActivity.this, error.optString("message", "Request failed"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(WithdrawActivity.this, "Submission failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setVisibility(View.VISIBLE);
                Toast.makeText(WithdrawActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}