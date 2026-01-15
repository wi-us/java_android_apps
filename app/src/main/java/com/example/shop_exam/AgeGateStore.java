package com.example.shop_exam;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Хранилище ответа для возрастного фильтра (18+).
 * - answered: показывали ли уже модальное окно
 * - adultAllowed: разрешён ли контент 18+
 */
public class AgeGateStore {
    private static final String PREFS = "age_gate_prefs";
    private static final String KEY_ANSWERED = "answered";
    private static final String KEY_ADULT_ALLOWED = "adult_allowed";

    public static boolean isAnswered(Context context) {
        if (context == null) return false;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_ANSWERED, false);
    }

    public static boolean isAdultAllowed(Context context) {
        if (context == null) return false;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_ADULT_ALLOWED, false);
    }

    public static void setAdultAllowed(Context context, boolean allowed) {
        if (context == null) return;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putBoolean(KEY_ANSWERED, true)
                .putBoolean(KEY_ADULT_ALLOWED, allowed)
                .apply();
    }

    /**
     * Сбрасывает решение 18+ (используется, чтобы спрашивать заново при каждом запуске приложения).
     */
    public static void resetForNewLaunch(Context context) {
        if (context == null) return;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putBoolean(KEY_ANSWERED, false)
                .putBoolean(KEY_ADULT_ALLOWED, false)
                .apply();
    }
}


