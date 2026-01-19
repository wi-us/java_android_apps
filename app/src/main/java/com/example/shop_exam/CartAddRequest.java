package com.example.shop_exam;

public class CartAddRequest {
    public final int game_variant_id;
    public final int quantity;

    public CartAddRequest(int gameVariantId, int quantity) {
        this.game_variant_id = gameVariantId;
        this.quantity = quantity;
    }
}