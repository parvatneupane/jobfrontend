package com.example.minijobhunt.model;

public class VerificationRequest {

    private String user_id;
    private String full_name;

    public VerificationRequest(String user_id, String full_name) {
        this.user_id = user_id;
        this.full_name = full_name;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getFull_name() {
        return full_name;
    }
}