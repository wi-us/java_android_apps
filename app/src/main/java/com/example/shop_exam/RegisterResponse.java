package com.example.shop_exam;

public class RegisterResponse {
    // В ответе мы ожидаем объект user и токен
    User user;
    String token;

    // Геттеры могут понадобиться для доступа к полям из других классов
    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}