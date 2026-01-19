package com.example.shop_exam;

import android.app.Application;

/**
 * Главный класс приложения. Выполняется один раз при запуске
 */
public class ShopExamApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // сброс проверки возарста при каждом запуске
        AgeGateStore.resetForNewLaunch(this);
    }
}
