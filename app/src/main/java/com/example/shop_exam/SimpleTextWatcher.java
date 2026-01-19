package com.example.shop_exam;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Упрощённый слушатель изменения текста. Позволяет переопределить только нужные методы.
 */
public class SimpleTextWatcher implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Пустая реализация
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Пустая реализация
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Пустая реализация
    }
}
