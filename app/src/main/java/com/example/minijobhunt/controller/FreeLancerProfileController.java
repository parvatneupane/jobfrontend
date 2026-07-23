package com.example.minijobhunt.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.minijobhunt.fragments.FreeLancerProfileEditFragment;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreeLancerProfileController {

    private FreeLancerProfileEditFragment view;

    public int profileId = 0;

    public FreeLancerProfileController(
            FreeLancerProfileEditFragment view
    ) {

        this.view = view;

    }


    public void loadProfile() {

        SharedPreferences pref =
                view.requireActivity()
                        .getSharedPreferences(
                                Constants.cache,
                                Context.MODE_PRIVATE
                        );

        try {

            JSONObject profile = new JSONObject();

            profile.put(
                    "title",
                    pref.getString("profile_title", "")
            );

            profile.put(
                    "bio",
                    pref.getString("profile_bio", "")
            );

            profile.put(
                    "experience_years",
                    pref.getInt("profile_experience", 0)
            );

            profile.put(
                    "hourly_rate",
                    pref.getString("profile_rate", "")
            );

            profile.put(
                    "skills",
                    pref.getString("profile_skills", "")
            );

            profile.put(
                    "location",
                    pref.getString("profile_location", "")
            );

            profile.put(
                    "availability",
                    pref.getString(
                            "profile_availability",
                            "available"
                    )
            );

            profile.put(
                    "portfolio_url",
                    pref.getString(
                            "profile_portfolio",
                            ""
                    )
            );

            profileId =
                    pref.getInt(
                            "profile_id",
                            0
                    );

            view.fillData(profile);

        }

        catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void saveProfile(

            String title,

            String bio,

            String experience,

            String rate,

            String skills,

            String location,

            String availability,

            String portfolio,
            
            List<Integer> categories

    ) {

        SharedPreferences pref =
                view.requireActivity()
                        .getSharedPreferences(
                                Constants.cache,
                                Context.MODE_PRIVATE
                        );

        String token =
                pref.getString(
                        "token",
                        ""
                );

        Map<String, Object> body =
                new HashMap<>();

        body.put(
                "title",
                title
        );

        body.put(
                "bio",
                bio
        );

        body.put(
                "experience_years",
                experience.isEmpty()
                        ? 0
                        : Integer.parseInt(experience)
        );

        body.put(
                "hourly_rate",
                rate.isEmpty()
                        ? 0
                        : Double.parseDouble(rate)
        );

        body.put(
                "skills",
                skills
        );

        body.put(
                "location",
                location
        );

        body.put(
                "availability",
                availability
        );

        body.put(
                "status",
                "active"
        );

        body.put(
                "portfolio_url",
                portfolio
        );
        
        if (categories != null && !categories.isEmpty()) {
            body.put("categories", categories);
        }

        Call<ResponseBody> call;

        if (profileId == 0) {

            call =
                    App.api.createProfile(
                            "Bearer " + token,
                            body
                    );

        } else {

            call =
                    App.api.updateProfile(
                            "Bearer " + token,
                            profileId,
                            body
                    );

        }

        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    Call<ResponseBody> call,
                    Response<ResponseBody> response
            ) {

                if (response.isSuccessful()) {

                    SharedPreferences.Editor editor =
                            pref.edit();

                    try {
                        if (response.body() != null) {
                            String json = response.body().string();
                            JSONObject root = new JSONObject(json);
                            JSONObject data = root.optJSONObject("data");
                            if (data != null) {
                                int newId = data.optInt("id", 0);
                                if (newId != 0) {
                                    profileId = newId;
                                    editor.putInt("profile_id", newId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    editor.putString(
                            "profile_title",
                            title
                    );

                    editor.putString(
                            "profile_bio",
                            bio
                    );

                    editor.putString(
                            "profile_skills",
                            skills
                    );

                    editor.putString(
                            "profile_location",
                            location
                    );

                    editor.putString(
                            "profile_portfolio",
                            portfolio
                    );

                    editor.putString(
                            "profile_availability",
                            availability
                    );

                    editor.putInt(
                            "profile_experience",
                            experience.isEmpty()
                                    ? 0
                                    : Integer.parseInt(experience)
                    );

                    editor.putString(
                            "profile_rate",
                            rate
                    );

                    editor.apply();

                    view.profileSaved();

                } else {

                    view.showError(
                            "Unable To Save"
                    );

                }

            }

            @Override
            public void onFailure(
                    Call<ResponseBody> call,
                    Throwable t
            ) {

                view.showError(
                        "Server Error"
                );

            }

        });

    }

}