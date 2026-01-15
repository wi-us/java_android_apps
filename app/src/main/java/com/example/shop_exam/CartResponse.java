package com.example.shop_exam;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CartResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("total")
    private double total;

    @SerializedName("items")
    private List<CartServerItem> items;

    public int getId() { return id; }
    public double getTotal() { return total; }
    public List<CartServerItem> getItems() { return items; }

    public static class CartServerItem {
        @SerializedName("id")
        private int id;
        @SerializedName("quantity")
        private int quantity;
        @SerializedName("game_variant")
        private GameVariantForList gameVariant;
        @SerializedName("line_total")
        private double lineTotal;

        public int getId() { return id; }
        public int getQuantity() { return quantity; }
        public GameVariantForList getGameVariant() { return gameVariant; }
        public double getLineTotal() { return lineTotal; }
    }
}




