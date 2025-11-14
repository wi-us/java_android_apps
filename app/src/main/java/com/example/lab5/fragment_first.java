package com.example.lab5;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class fragment_first extends Fragment {

    private TextInputEditText fioEditText;
    private TextInputEditText emailEditText;
    private Button sendButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        
        // Инициализация элементов интерфейса
        fioEditText = view.findViewById(R.id.fioEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        sendButton = view.findViewById(R.id.sendButton);
        
        // Обработчик нажатия на кнопку "Отправить"
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleFormSubmission();
            }
        });
        
        return view;
    }
    
    private void handleFormSubmission() {
        String fio = fioEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        
        // Проверка заполненности полей
        if (TextUtils.isEmpty(fio)) {
            fioEditText.setError("Поле ФИО не может быть пустым");
            return;
        }
        
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Поле Email не может быть пустым");
            return;
        }
        
        // Простая проверка формата email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Введите корректный email адрес");
            return;
        }
        
        // Очистка полей
        fioEditText.setText("");
        emailEditText.setText("");
        
        // Показ всплывающего окна с информацией о заявке
        String message = "Заявка отправлена!\n" +
                "ФИО: " + fio + "\n" +
                "Email: " + email + "\n";

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
}