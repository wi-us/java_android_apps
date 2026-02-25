package com.example.a4_2_food;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.annotation.NonNull;

import java.util.Locale;

// Сохраняем язык в настройках и подставляем его для экранов.
public class LocaleHelper {

    private static final String PREFS_NAME = "42food_prefs";
    private static final String KEY_LANG = "lang";

    public static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "ru");
    }

    @NonNull
    public static Context wrap(@NonNull Context context) {
        String lang = getLanguage(context);
        Locale locale = "en".equals(lang) ? Locale.ENGLISH : ("es".equals(lang) ? new Locale("es") : new Locale("ru"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.setLocale(locale);
            config.setLocales(new LocaleList(locale));
            return context.createConfigurationContext(config);
        } else {
            Resources res = context.getResources();
            Configuration config = new Configuration(res.getConfiguration());
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }
}
