package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;

public class CheckoutResponse {
    @SerializedName("order_id")
    int orderId;

    @SerializedName("total")
    double total;

    public int getOrderId() {
        return orderId;
    }

    public double getTotal() {
        return total;
    }
}




