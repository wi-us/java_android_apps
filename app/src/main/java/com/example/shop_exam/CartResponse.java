package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;

public class CartResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("total")
    private double total;

    public double getTotal() { return total; }
}




