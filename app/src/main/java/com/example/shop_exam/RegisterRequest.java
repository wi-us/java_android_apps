package com.example.shop_exam;

public class RegisterRequest {
    // Fields must match the UserCreate schema in schemas.py
    final String email;
    final String password;
    final String login;

    public RegisterRequest(String email, String password, String login) {
        this.email = email;
        this.password = password;
        this.login = login;
    }
}