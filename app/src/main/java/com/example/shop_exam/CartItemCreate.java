package com.example.shop_exam;

// Модель для создания элемента в корзине.
// Соответствует схеме CartItemCreate на сервере.
public class CartItemCreate {
    final int cart_id;
    final int game_variant_id;
    final int quantity;
    final double price_snapshot;

    public CartItemCreate(int cart_id, int game_variant_id, int quantity, double price_snapshot) {
        this.cart_id = cart_id;
        this.game_variant_id = game_variant_id;
        this.quantity = quantity;
        this.price_snapshot = price_snapshot;
    }
}

