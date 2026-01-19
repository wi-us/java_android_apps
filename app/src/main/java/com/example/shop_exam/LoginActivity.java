package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран авторизации пользователя
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout loginInput;
    private TextInputLayout passwordInput;
    private Button loginButton;
    private Button registerButton;
    private ApiService apiService;
    private boolean returnToCart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Нужно ли вернуться к корзине после авторизации
        returnToCart = getIntent().getBooleanExtra("return_to_cart", false);
        apiService = ApiClient.getClient(this).create(ApiService.class);
        loginInput = findViewById(R.id.login_input_layout);
        passwordInput = findViewById(R.id.password_input_layout);
        loginButton = findViewById(R.id.button_login);
        registerButton = findViewById(R.id.button_registration);

        // Кнопка входа
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        // Кнопка перехода к регистрации
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // Авторизация пользователя
    private void login() {
        // Сброс ошибок
        loginInput.setError(null);
        passwordInput.setError(null);

        // Получение данных из полей
        String username = "";
        String password = "";

        if (loginInput.getEditText() != null) {
            username = loginInput.getEditText().getText().toString().trim();
        }
        if (passwordInput.getEditText() != null) {
            password = passwordInput.getEditText().getText().toString().trim();
        }

        // Валидация
        boolean isValid = true;

        if (username.isEmpty()) {
            loginInput.setError("Введите логин");
            isValid = false;
        } else if (username.length() < 4) {
            loginInput.setError("Минимум 4 символа");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Введите пароль");
            isValid = false;
        } else if (password.length() < 6) {
            passwordInput.setError("Минимум 6 символов");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Запрос на сервер
        apiService.loginUser(username, password).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Сохранение токена
                    String token = response.body().access_token;
                    TokenStore.saveToken(LoginActivity.this, token);

                    // Синхронизация локальной корзины с сервером
                    syncLocalCartToServer();

                    // Переход на нужный экран
                    if (returnToCart) {
                        Intent intent = new Intent(LoginActivity.this, CartActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    finish();
                } else {
                    passwordInput.setError("Неверный логин или пароль");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                passwordInput.setError("Ошибка сети");
            }
        });
    }

    // Синхронизация локальной корзины с сервером после авторизации
    private void syncLocalCartToServer() {
        Map<Integer, Integer> localItems = LocalCartStore.getItems(this);
        
        if (localItems.isEmpty()) {
            return;
        }

        // Отправка каждого товара на сервер
        for (Map.Entry<Integer, Integer> entry : localItems.entrySet()) {
            int variantId = entry.getKey();
            int quantity = entry.getValue();
            
            CartAddRequest request = new CartAddRequest(variantId, quantity);
            apiService.addToCart(request).enqueue(new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                }

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {
                }
            });
        }
    }
}
