package com.example.shop_exam;

public class CheckoutRequest {
    final String address;
    final String apartment;
    final String name;
    final String phone;
    final String comment;

    public CheckoutRequest(String address, String apartment, String name, String phone, String comment) {
        this.address = address;
        this.apartment = apartment;
        this.name = name;
        this.phone = phone;
        this.comment = comment;
    }
}


