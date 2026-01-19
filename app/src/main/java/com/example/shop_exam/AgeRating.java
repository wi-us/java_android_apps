package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;

public class AgeRating {
    @SerializedName("min_age")
    private int minAge;

    public int getMinAge() {
        return minAge;
    }
}

