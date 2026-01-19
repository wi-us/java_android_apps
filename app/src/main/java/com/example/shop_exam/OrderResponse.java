package com.example.shop_exam;

import java.util.List;

/**
 * Модель заказа
 */
public class OrderResponse {
    public int id;
    public double total_price;
    public String created_at;
    public Status status;
    public String address;
    public String comment;

    public int getId() {
        return id;
    }

    public double getTotalPrice() {
        return total_price;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public Status getStatus() {
        return status;
    }

    public String getAddress() {
        return address;
    }

    public String getComment() {
        return comment;
    }
}







