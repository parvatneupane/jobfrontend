package com.example.minijobhunt.utils;

import android.app.Application;
import android.content.SharedPreferences;
import com.example.minijobhunt.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import com.google.android.libraries.places.api.Places;

public class App extends Application {

    public static RestApi api;

    @Override
    public void onCreate() {
        super.onCreate();
        initRetrofit();
        initPlaces();
        updateTerminationTime();
    }

    private void updateTerminationTime() {
        SharedPreferences pref = getSharedPreferences(Constants.cache, MODE_PRIVATE);
        pref.edit().putLong("last_termination_time", System.currentTimeMillis()).apply();
    }

    @Override
    public void onTerminate() {
        updateTerminationTime();
        super.onTerminate();
    }

    private void initPlaces() {
        if (!Places.isInitialized()) {
            // Note: Replace with your actual API Key
            Places.initialize(getApplicationContext(), "AIzaSyDP_MwmKtuyd2DqBDobHW2TEM1R_XFr3Qc");
        }
    }

    private void initRetrofit() {

        Gson gson = new GsonBuilder().setLenient().create();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request request = chain.request().newBuilder()
                            .addHeader("Accept", "application/json")
                            .build();
                    return chain.proceed(request);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(RestApi.class);
    }
}