package com.example.shop_exam;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Класс для хранения ответа на вопрос о возрасте (18+)
 */
public class AgeGateStore {

    private static final String PREFS_NAME = "age_gate_prefs";
    private static final String KEY_ANSWERED = "answered";
    private static final String KEY_ALLOWED = "adult_allowed";

    // Проверка ответа на вопрос о возрасте
    public static boolean isAnswered(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ANSWERED, false);
    }

    // Проверка возраста
    public static boolean isAdultAllowed(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ALLOWED, false);
    }

    // Сохранение ответа
    public static void setAdultAllowed(Context context, boolean allowed) {
        if (context == null) return;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_ANSWERED, true)
                .putBoolean(KEY_ALLOWED, allowed)
                .apply();
    }

    // Сброс ответа
    public static void resetForNewLaunch(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_ANSWERED, false)
                .putBoolean(KEY_ALLOWED, false)
                .apply();
    }
}
