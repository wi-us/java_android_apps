package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;

public class GameVariantForList {

    @SerializedName("id")
    private int id;
    
    @SerializedName("edition_name")
    private String editionName;
    
    @SerializedName("description")
    private String description;
    @SerializedName("image_link")
    private String imageLink;

    @SerializedName("price")
    private double price;
    @SerializedName("discount_price")
    private Double discountPrice;

    @SerializedName("game")
    private GameInfo game;

    @SerializedName("status")
    private Status status;

    public int getId() { return id; }
    public String getEditionName() { return editionName; }
    public String getDescription() { return description; }
    public String getImageLink() { return imageLink; }
    public double getPrice() { return price; }
    public Double getDiscountPrice() { return discountPrice; }
    public GameInfo getGame() { return game; }
    public Status getStatus() { return status; }

    public double getActualPrice() {
        return (discountPrice != null && discountPrice > 0) ? discountPrice : price;
    }

    public static class GameInfo {
        @SerializedName("id")
        private int id;
        @SerializedName("title")
        private String title;
        @SerializedName("min_players")
        private int minPlayers;
        @SerializedName("max_players")
        private int maxPlayers;
        @SerializedName("age_rating")
        private AgeRating ageRating;

        public String getTitle() { return title; }
        public int getMinPlayers() { return minPlayers; }
        public int getMaxPlayers() { return maxPlayers; }
        public AgeRating getAgeRating() { return ageRating; }
    }
}
