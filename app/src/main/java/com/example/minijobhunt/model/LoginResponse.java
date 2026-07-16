package com.example.minijobhunt.model;

public class LoginResponse {

    private boolean success;

    private String message;

    private String token;

    private Register user;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public Register getUser() {
        return user;
    }

}