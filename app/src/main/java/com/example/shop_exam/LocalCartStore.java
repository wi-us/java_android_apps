package com.example.shop_exam;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Локальное хранилище корзины
 */
public class LocalCartStore {

    private static final String PREFS_NAME = "local_cart";
    private static final String KEY_ITEMS = "cart_items";

    // Структура: variant_id (String) -> quantity
    private static Map<String, Integer> getCartMapInternal(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ITEMS, null);
        
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> map = gson.fromJson(json, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // Конвертация в Map<Integer, Integer> для внешнего использования
    private static Map<Integer, Integer> getCartMap(Context context) {
        Map<String, Integer> stringMap = getCartMapInternal(context);
        Map<Integer, Integer> intMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : stringMap.entrySet()) {
            try {
                int key = Integer.parseInt(entry.getKey());
                intMap.put(key, entry.getValue());
            } catch (NumberFormatException e) {
            }
        }
        return intMap;
    }

    private static void saveCartMap(Context context, Map<Integer, Integer> map) {
        // Конвертируем в Map<String, Integer> для корректной сериализации
        Map<String, Integer> stringMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(stringMap);
        prefs.edit().putString(KEY_ITEMS, json).apply();
    }

    // Добавить товар в корзину
    public static void addItem(Context context, int variantId, int quantity) {
        Map<Integer, Integer> map = getCartMap(context);
        int current = map.containsKey(variantId) ? map.get(variantId) : 0;
        map.put(variantId, current + quantity);
        saveCartMap(context, map);
    }

    // Установить количество товара
    public static void setQuantity(Context context, int variantId, int quantity) {
        Map<Integer, Integer> map = getCartMap(context);
        if (quantity <= 0) {
            map.remove(variantId);
        } else {
            map.put(variantId, quantity);
        }
        saveCartMap(context, map);
    }

    // Удалить товар из корзины
    public static void removeItem(Context context, int variantId) {
        Map<Integer, Integer> map = getCartMap(context);
        map.remove(variantId);
        saveCartMap(context, map);
    }

    // Получить все товары в корзине
    public static Map<Integer, Integer> getItems(Context context) {
        return getCartMap(context);
    }

    // Получить количество конкретного товара
    public static int getQuantity(Context context, int variantId) {
        Map<Integer, Integer> map = getCartMap(context);
        return map.containsKey(variantId) ? map.get(variantId) : 0;
    }

    // Получить общее количество товаров
    public static int getTotalCount(Context context) {
        Map<Integer, Integer> map = getCartMap(context);
        int total = 0;
        for (int qty : map.values()) {
            total += qty;
        }
        return total;
    }

    // Очистить корзину
    public static void clear(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_ITEMS).apply();
    }

    // Проверить, пуста ли корзина
    public static boolean isEmpty(Context context) {
        return getCartMap(context).isEmpty();
    }
}

