package com.example.shop_exam;

import android.app.Application;

/**
 * Application class: выполняется один раз при старте процесса приложения.
 * Здесь сбрасываем возрастную проверку 18+, чтобы она спрашивалась заново при каждом запуске.
 */
public class ShopExamApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AgeGateStore.resetForNewLaunch(this);
    }
}












