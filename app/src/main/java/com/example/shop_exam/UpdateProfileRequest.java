package com.example.shop_exam;

/**
 * Запрос на обновление профиля
 */
public class UpdateProfileRequest {
    public String email;
    public String first_name;
    public String last_name;
    public String phone_number;

    public UpdateProfileRequest(String email, String firstName, String lastName, String phoneNumber) {
        this.email = email;
        this.first_name = firstName;
        this.last_name = lastName;
        this.phone_number = phoneNumber;
    }
}







