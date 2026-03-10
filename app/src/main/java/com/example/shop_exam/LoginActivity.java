package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
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

    private static final String TAG = "LoginActivity";
    
    private TextInputLayout loginInput;
    private TextInputLayout passwordInput;
    private Button loginButton;
    private Button registerButton;
    private Button loginButton_GOOGLE;
    private ApiService apiService;
    private boolean returnToCart = false;
    
    // Google авторизация
    private GoogleAuthHelper googleAuthHelper;
    private ActivityResultLauncher<Intent> googleLauncher;

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

        // Инициализация Google launcher'а (ПЕРЕД инициализацией GoogleAuthHelper!)
        Log.d(TAG, "🔧 Инициализация Google Launcher...");
        googleLauncher = GoogleAuthHelper.createLauncher(this, new GoogleAuthHelper.GoogleAuthCallback() {
            @Override
            public void onSuccess(String idToken, String email, String displayName) {
                Log.d(TAG, "✅ [CALLBACK] Google авторизация успешна!");
                Log.d(TAG, "   Email: " + email);
                Log.d(TAG, "   Display Name: " + displayName);
                Log.d(TAG, "   ID Token: " + (idToken != null ? "присутствует (длина: " + idToken.length() + ")" : "NULL"));
                onGoogleAuthSuccess(idToken, email, displayName);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ [CALLBACK] Ошибка Google авторизации: " + error);
                passwordInput.setError("Ошибка Google: " + error);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "🚫 [CALLBACK] Google авторизация отменена пользователем");
                passwordInput.setError("Авторизация отменена");
            }
        });
        Log.d(TAG, "✅ Google Launcher создан");
        
        // Инициализация Google авторизации
        String clientId = "782186171474-2pb8t3j9k0t8dp3i90e70qviie2lutlp.apps.googleusercontent.com";
        Log.d(TAG, "🔧 Инициализация GoogleAuthHelper...");
        Log.d(TAG, "   Client ID: " + clientId);
        Log.d(TAG, "   Package Name: " + getPackageName());
        
        googleAuthHelper = new GoogleAuthHelper(
            this,
            clientId,
            new GoogleAuthHelper.GoogleAuthCallback() {
                @Override
                public void onSuccess(String idToken, String email, String displayName) {
                    // Не используется, обработка в launcher
                }

                @Override
                public void onError(String error) {
                    // Не используется, обработка в launcher
                }

                @Override
                public void onCancel() {
                    // Не используется, обработка в launcher
                }
            }
        );
        Log.d(TAG, "✅ GoogleAuthHelper инициализирован");

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
        loginButton_GOOGLE.setOnClickListener(v -> {
            Log.d(TAG, "");
            Log.d(TAG, "========================================");
            Log.d(TAG, "🔵 НАЖАТА КНОПКА GOOGLE АВТОРИЗАЦИИ");
            Log.d(TAG, "========================================");
            startGoogleAuth();
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

    /**
     * Запускает Google авторизацию
     */
    private void startGoogleAuth() {
        try {
            Log.d(TAG, "▶️ Вызываем googleAuthHelper.startAuth()...");
            Log.d(TAG, "   GoogleAuthHelper: " + (googleAuthHelper != null ? "OK" : "NULL"));
            Log.d(TAG, "   GoogleLauncher: " + (googleLauncher != null ? "OK" : "NULL"));
            
            googleAuthHelper.startAuth(googleLauncher);
            
            Log.d(TAG, "✅ startAuth() вызван успешно, ожидаем результат...");
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION при запуске Google авторизации!");
            Log.e(TAG, "   Тип: " + e.getClass().getName());
            Log.e(TAG, "   Сообщение: " + e.getMessage());
            e.printStackTrace();
            passwordInput.setError("Ошибка Google авторизации");
        }
    }

    /**
     * Обработка успешной Google авторизации
     */
    private void onGoogleAuthSuccess(String idToken, String email, String displayName) {
        Log.d(TAG, "");
        Log.d(TAG, "========================================");
        Log.d(TAG, "🎉 GOOGLE АВТОРИЗАЦИЯ УСПЕШНА!");
        Log.d(TAG, "========================================");
        Log.d(TAG, "📧 Email: " + email);
        Log.d(TAG, "👤 Display Name: " + displayName);
        Log.d(TAG, "🔑 ID Token: " + (idToken != null ? "присутствует (длина: " + idToken.length() + ")" : "NULL"));
        
        Log.d(TAG, "💾 Сохраняем токен...");
        TokenStore.saveToken(LoginActivity.this, idToken);
        Log.d(TAG, "✅ Токен сохранён");
        
        Log.d(TAG, "🛒 Синхронизируем корзину...");
        syncLocalCartToServer();
        
        // Переходим на нужный экран
        Intent intent;
        if (returnToCart) {
            Log.d(TAG, "📱 Переход в CartActivity");
            intent = new Intent(LoginActivity.this, CartActivity.class);
        } else {
            Log.d(TAG, "📱 Переход в MainActivity");
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }
        startActivity(intent);
        Log.d(TAG, "🏁 Завершаем LoginActivity");
        finish();
    }
    
}

