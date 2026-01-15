package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout emailInput, loginInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private Button backButton;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Привязка View
        emailInput = findViewById(R.id.email_input_layout);
        loginInput = findViewById(R.id.login_input_layout);
        passwordInput = findViewById(R.id.password_input_layout);
        confirmPasswordInput = findViewById(R.id.confirm_password_input_layout);
        registerButton = findViewById(R.id.button_continue_registration);
        backButton = findViewById(R.id.button_back_to_login);

        registerButton.setOnClickListener(v -> {
            registerUser();
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String email = emailInput.getEditText().getText().toString().trim();
        String login = loginInput.getEditText().getText().toString().trim();
        String password = passwordInput.getEditText().getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getEditText().getText().toString().trim();

        // Валидация
        if (email.isEmpty() || login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создание и отправка запроса
        RegisterRequest request = new RegisterRequest(email, password, login);
        apiService.registerUser(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Регистрация успешна! Теперь вы можете войти.", Toast.LENGTH_LONG).show();
                    // Возвращаемся на экран входа после успешной регистрации
                    finish();
                } else {
                    String errorMsg = "Ошибка регистрации.";
                    try {
                        if (response.errorBody() != null) {
                            // Пытаемся получить более детальную ошибку от сервера
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
