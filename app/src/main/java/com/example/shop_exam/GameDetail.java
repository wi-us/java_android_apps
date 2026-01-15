package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GameDetail {

    @SerializedName("id")
    private int id;
    @SerializedName("description")
    private String description;
    @SerializedName("price")
    private double price;
    @SerializedName("discount_price")
    private Double discountPrice;
    
    @SerializedName("game")
    private GameInfo game;

    @SerializedName("stock")
    private StockInfo stock;

    @SerializedName("status")
    private Status status;

    @SerializedName("images")
    private List<ImageInfo> images;

    // Геттеры
    public int getId() { return id; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public Double getDiscountPrice() { return discountPrice; }
    public GameInfo getGame() { return game; }
    public StockInfo getStock() { return stock; }
    public Status getStatus() { return status; }
    public List<ImageInfo> getImages() { return images; }
    
    public double getActualPrice() {
        return (discountPrice != null && discountPrice > 0) ? discountPrice : price;
    }

    public static class GameInfo {
        @SerializedName("title")
        private String title;

        @SerializedName("age_rating")
        private AgeRating ageRating;
        
        public String getTitle() { return title; }
        public AgeRating getAgeRating() { return ageRating; }
    }

    public static class StockInfo {
        @SerializedName("quantity")
        private int quantity;
        
        public int getQuantity() { return quantity; }
    }

    public static class ImageInfo {
        @SerializedName("url")
        private String url;
        @SerializedName("is_cover")
        private boolean isCover;
        @SerializedName("sort_order")
        private int sortOrder;
        
        public String getUrl() { return url; }
        public boolean isCover() { return isCover; }
        public int getSortOrder() { return sortOrder; }
    }
}
