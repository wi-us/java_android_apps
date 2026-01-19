package com.example.shop_exam;

import java.util.List;

/**
 * Детальная модель заказа с позициями
 */
public class OrderDetailResponse {
    public int id;
    public double total_price;
    public String created_at;
    public Status status;
    public String address;
    public String comment;
    public List<OrderItemResponse> items;

    public int getId() {
        return id;
    }

    public double getTotalPrice() {
        return total_price;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public Status getStatus() {
        return status;
    }

    public String getAddress() {
        return address;
    }

    public String getComment() {
        return comment;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    /**
     * Позиция заказа
     */
    public static class OrderItemResponse {
        public int id;
        public int quantity;
        public double price_per_item;
        public double subtotal;
        public GameVariantForList game_variant;

        public int getId() {
            return id;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPricePerItem() {
            return price_per_item;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public GameVariantForList getGameVariant() {
            return game_variant;
        }
    }
}







