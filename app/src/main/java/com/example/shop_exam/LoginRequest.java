package com.example.shop_exam;

// This class is no longer needed because we will send fields directly
// in the ApiService interface for FormUrlEncoded requests.
// For JSON requests, we will use RegisterRequest.
public class LoginRequest {
    String username;
    String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
