package com.example.shop_exam;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenStore {
    private static final String PREFS = "auth_prefs";
    private static final String KEY_TOKEN = "access_token";

    public static void saveToken(Context context, String token) {
        if (context == null) return;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        if (context == null) return null;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, null);
    }
}