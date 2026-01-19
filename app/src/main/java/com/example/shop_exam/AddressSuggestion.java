package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;

public class AddressSuggestion {
    @SerializedName("id")
    private String id;

    @SerializedName("text")
    private String text;

    @SerializedName("lat")
    private Double lat;

    @SerializedName("lon")
    private Double lon;

    public String getText() { return text; }
    public Double getLat() { return lat; }
    public Double getLon() { return lon; }

    @Override
    public String toString() {
        return text == null ? "" : text;
    }
}


