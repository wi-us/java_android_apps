package com.example.shop_exam;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Класс для хранения токена авторизации. Использует SharedPreferences для сохранения данных
 */
public class TokenStore {

    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "access_token";

    // Сохранение токена
    public static void saveToken(Context context, String token) {
        if (context == null) return;

        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    // Получение токена
    public static String getToken(Context context) {
        if (context == null) return null;

        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    // Удаление токена (выход из аккаунта)
    public static void clearToken(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }
}
