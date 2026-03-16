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
    private View loginButton_GOOGLE;
    private ApiService apiService;
    private boolean returnToCart = false;
    
    // Google авторизация
    private GoogleAuthHelper googleAuthHelper;
    private androidx.activity.result.ActivityResultLauncher<Intent> googleLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Нужно ли вернуться к корзине после авторизации
        returnToCart = getIntent().getBooleanExtra("return_to_cart", false);
        apiService = ApiClient.getClient(this).create(ApiService.class);
        
        // Инициализация элементов интерфейса
        loginInput = findViewById(R.id.login_input_layout);
        passwordInput = findViewById(R.id.password_input_layout);
        loginButton = findViewById(R.id.button_login);
        registerButton = findViewById(R.id.button_registration);
        loginButton_GOOGLE = findViewById(R.id.button_GoogleLogin);

        // Инициализация Google авторизации (Firebase Auth + Google Sign-In)
        googleAuthHelper = new GoogleAuthHelper(this, new GoogleAuthHelper.GoogleAuthCallback() {
            @Override
            public void onSuccess(String idToken, String email, String displayName) {
                onGoogleAuthSuccess(idToken, email, displayName);
            }

            @Override
            public void onError(String error) {
                passwordInput.setError(error);
            }

            @Override
            public void onCancel() {
                passwordInput.setError("Авторизация отменена");
            }
        });

        googleLauncher = googleAuthHelper.createLauncher(this);

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
        
        // Google авторизация кнопка
        loginButton_GOOGLE.setOnClickListener(v -> startGoogleAuth());
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

        final String finalUsername = username;
        final String finalPassword = password;

        // Запрос на сервер
        apiService.loginUser(finalUsername, finalPassword).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().access_token;
                    TokenStore.saveToken(LoginActivity.this, token);

                    syncLocalCartToServer();

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

    // Синхронизация локальной корзины с сервером после авторизации, затем очистка
    private void syncLocalCartToServer() {
        Map<Integer, Integer> localItems = LocalCartStore.getItems(this);

        if (localItems.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Integer> entry : localItems.entrySet()) {
            CartAddRequest request = new CartAddRequest(entry.getKey(), entry.getValue());
            apiService.addToCart(request).enqueue(new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {}

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {}
            });
        }

        // Очищаем локальную корзину — теперь источник правды сервер
        LocalCartStore.clear(this);
    }

    private void startGoogleAuth() {
        googleAuthHelper.startAuth(googleLauncher);
    }

    private void onGoogleAuthSuccess(String idToken, String email, String displayName) {
        GoogleAuthRequest request = new GoogleAuthRequest(email, displayName);
        apiService.googleAuth(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().access_token;
                    TokenStore.saveToken(LoginActivity.this, token);
                    syncLocalCartToServer();

                    Intent intent;
                    if (returnToCart) {
                        intent = new Intent(LoginActivity.this, CartActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    loginInput.setError("Ошибка входа через Google (код " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loginInput.setError("Ошибка соединения с сервером");
            }
        });
    }
    
}

