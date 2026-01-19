package com.example.shop_exam;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран регистрации нового пользователя
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout emailInput;
    private TextInputLayout loginInput;
    private TextInputLayout passwordInput;
    private TextInputLayout confirmPasswordInput;
    private Button registerButton;
    private Button backButton;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        // API
        apiService = ApiClient.getClient(this).create(ApiService.class);

        emailInput = findViewById(R.id.email_input_layout);
        loginInput = findViewById(R.id.login_input_layout);
        passwordInput = findViewById(R.id.password_input_layout);
        confirmPasswordInput = findViewById(R.id.confirm_password_input_layout);
        registerButton = findViewById(R.id.button_continue_registration);
        backButton = findViewById(R.id.button_back_to_login);

        // Кнопка регистрации
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        // Кнопка назад
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // Регистрация пользователя
    private void register() {
        // Очистка ошибок
        clearErrors();

        // Получаем данные из полей
        String email = getTextFromInput(emailInput);
        String login = getTextFromInput(loginInput);
        String password = getTextFromInput(passwordInput);
        String confirmPassword = getTextFromInput(confirmPasswordInput);

        // Валидация
        boolean isValid = true;

        // Проверка email
        if (email.isEmpty()) {
            emailInput.setError("Введите email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Некорректный email");
            isValid = false;
        }

        // Проверка логина
        if (login.isEmpty()) {
            loginInput.setError("Введите логин");
            isValid = false;
        } else if (login.length() < 4) {
            loginInput.setError("Минимум 4 символа");
            isValid = false;
        } else if (!login.matches("^[a-zA-Z0-9._-]+$")) {
            loginInput.setError("Только латинские буквы, цифры, . _ -");
            isValid = false;
        }

        // Проверка пароля
        if (password.isEmpty()) {
            passwordInput.setError("Введите пароль");
            isValid = false;
        } else if (password.length() < 6) {
            passwordInput.setError("Минимум 6 символов");
            isValid = false;
        }

        // Проверка подтверждения пароля
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Пароли не совпадают");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Создаём запрос
        RegisterRequest request = new RegisterRequest(email, password, login);

        // Отправляем на сервер
        apiService.registerUser(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful()) {
                    finish();
                } else {
                    loginInput.setError("Логин уже занят");
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
            }
        });
    }

    // Сброс ошибок
    private void clearErrors() {
        emailInput.setError(null);
        loginInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);
    }

    // Вспомогательный метод для получения текста из TextInputLayout
    private String getTextFromInput(TextInputLayout input) {
        if (input != null && input.getEditText() != null) {
            return input.getEditText().getText().toString().trim();
        }
        return "";
    }
}
