package com.example.shop_exam;

// Локальная модель корзины (без сервера): достаточно для института.
public class CartItem {

    private int gameVariantId;
    private String title;
    private String imageUrl;
    private double price;
    private int quantity;

    public CartItem(int gameVariantId, String title, String imageUrl, double price, int quantity) {
        this.gameVariantId = gameVariantId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
    }

    public int getGameVariantId() { return gameVariantId; }
    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public double getPrice() { return price; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
}

