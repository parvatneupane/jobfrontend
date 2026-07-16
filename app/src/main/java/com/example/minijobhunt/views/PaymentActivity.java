package com.example.minijobhunt.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.minijobhunt.R;
import com.f1soft.esewapaymentsdk.EsewaConfiguration;
import com.f1soft.esewapaymentsdk.EsewaPayment;
import com.f1soft.esewapaymentsdk.ui.screens.EsewaPaymentActivity;

import java.util.HashMap;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PAYMENT = 1001;
    private static final String TAG = "PaymentActivity";

    private EsewaConfiguration esewaConfiguration;

    // Standard Test Credentials for eSewa SDK
    private final String CLIENT_ID =
            "JB0BBQ4aD0UqIThFJwAKBgAXEUkEGQUBBAwdOgABHD4DChwUAB0R";

    private final String SECRET_KEY =
            "BhwIWQQADhIYSxILExMcAgFXFhcOBwAKBgAXEQ==";

    private final String CALLBACK_URL = "https://yourcallbackurl.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        esewaConfiguration = new EsewaConfiguration(
                CLIENT_ID,
                SECRET_KEY,
                EsewaConfiguration.ENVIRONMENT_TEST
        );

        Intent intent = getIntent();

        String rawAmount = intent.getStringExtra("amount");
        String productName = intent.getStringExtra("productName");
        String productId = intent.getStringExtra("productId");

        if (rawAmount == null || rawAmount.isEmpty()
                || productName == null || productName.isEmpty()
                || productId == null || productId.isEmpty()) {

            Toast.makeText(this, "Invalid payment details", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Format amount: use what is passed, but ensure it's a valid string
        String finalAmount = rawAmount;
        try {
            double amt = Double.parseDouble(rawAmount);
            if (amt == (long) amt) {
                finalAmount = String.valueOf((long) amt);
            } else {
                finalAmount = String.valueOf(amt);
            }
        } catch (Exception e) {
            Log.e(TAG, "Amount parsing error", e);
        }

        Log.d(TAG, "Requesting eSewa Payment:");
        Log.d(TAG, "Amount : " + finalAmount);
        Log.d(TAG, "Product Name : " + productName);
        Log.d(TAG, "Product Id : " + productId);

        EsewaPayment payment = new EsewaPayment(
                finalAmount,
                productName,
                productId,
                CALLBACK_URL,
                new HashMap<>()
        );

        Intent paymentIntent = new Intent(this, EsewaPaymentActivity.class);
        paymentIntent.putExtra(EsewaConfiguration.ESEWA_CONFIGURATION, esewaConfiguration);
        paymentIntent.putExtra(EsewaPayment.ESEWA_PAYMENT, payment);

        startActivityForResult(paymentIntent, REQUEST_CODE_PAYMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PAYMENT) {

            if (resultCode == Activity.RESULT_OK) {
                Intent result = new Intent();
                result.putExtra("payment_success", true);
                if (data != null) {
                    // This often contains the transaction ID or message from eSewa
                    result.putExtra(
                            "payment_message",
                            data.getStringExtra(EsewaPayment.EXTRA_RESULT_MESSAGE)
                    );
                }
                setResult(Activity.RESULT_OK, result);
            } else {
                Intent result = new Intent();
                result.putExtra("payment_success", false);
                setResult(Activity.RESULT_CANCELED, result);
            }
            finish();
        }
    }
}
