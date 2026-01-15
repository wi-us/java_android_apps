package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputLayout loginInputLayout;
    private TextInputLayout passwordInputLayout;
    private Button loginButton;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        // 2. Привязка View-элементов
        loginInputLayout = findViewById(R.id.login_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        loginButton = findViewById(R.id.button_login);

        // 3. Установка обработчика нажатия на кнопку входа
        loginButton.setOnClickListener(v -> loginUser());

        // 4. Установка обработчика нажатия на кнопку регистрации
        Button registerNavButton = findViewById(R.id.button_registration);
        registerNavButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String username = loginInputLayout.getEditText().getText().toString().trim();
        String password = passwordInputLayout.getEditText().getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите логин и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Attempting to login with username: " + username);

        apiService.loginUser(username, password).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Login successful. Token: " + response.body().access_token);
                    Toast.makeText(LoginActivity.this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show();
                    
                    TokenStore.saveToken(LoginActivity.this, response.body().access_token);

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String error = "Ошибка входа. Проверьте данные.";
                    try {
                        if (response.errorBody() != null) {
                            error += " Код: " + response.code() + " Тело: " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body", e);
                    }
                    Log.e(TAG, "Login failed: " + error);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "Network request failed", t);
                Toast.makeText(LoginActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
