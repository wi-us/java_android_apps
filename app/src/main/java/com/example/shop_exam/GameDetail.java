package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GameDetail {

    @SerializedName("id")
    private int id;
    @SerializedName("description")
    private String description;
    @SerializedName("description_html")
    private String descriptionHtml;
    @SerializedName("rules_html")
    private String rulesHtml;
    @SerializedName("components_html")
    private String componentsHtml;
    @SerializedName("complexity")
    private String complexity;
    @SerializedName("price")
    private double price;
    @SerializedName("discount_price")
    private Double discountPrice;
    @SerializedName("image_link")
    private String imageLink;
    @SerializedName("weight_grams")
    private Integer weightGrams;
    @SerializedName("dimensions_mm")
    private String dimensionsMm;
    @SerializedName("box_width_mm")
    private Integer boxWidthMm;
    @SerializedName("box_height_mm")
    private Integer boxHeightMm;
    @SerializedName("box_depth_mm")
    private Integer boxDepthMm;
    @SerializedName("genres")
    private List<String> genres;

    @SerializedName("game")
    private GameInfo game;

    @SerializedName("stock")
    private StockInfo stock;

    @SerializedName("status")
    private Status status;

    @SerializedName("images")
    private List<ImageInfo> images;

    public int getId() { return id; }
    public String getDescription() { return description; }
    public String getDescriptionHtml() { return descriptionHtml; }
    public String getRulesHtml() { return rulesHtml; }
    public String getComponentsHtml() { return componentsHtml; }
    public String getComplexity() { return complexity; }
    public double getPrice() { return price; }
    public Double getDiscountPrice() { return discountPrice; }
    public String getImageLink() { return imageLink; }
    public Integer getWeightGrams() { return weightGrams; }
    public String getDimensionsMm() { return dimensionsMm; }
    public Integer getBoxWidthMm() { return boxWidthMm; }
    public Integer getBoxHeightMm() { return boxHeightMm; }
    public Integer getBoxDepthMm() { return boxDepthMm; }
    public List<String> getGenres() { return genres; }
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
        @SerializedName("min_players")
        private Integer minPlayers;
        @SerializedName("max_players")
        private Integer maxPlayers;
        @SerializedName("playtime_avg")
        private Integer playtimeAvg;

        public String getTitle() { return title; }
        public AgeRating getAgeRating() { return ageRating; }
        public Integer getMinPlayers() { return minPlayers; }
        public Integer getMaxPlayers() { return maxPlayers; }
        public Integer getPlaytimeAvg() { return playtimeAvg; }
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
