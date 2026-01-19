package com.example.shop_exam;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран редактирования профиля
 */
public class EditProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputEditText loginInput;
    private TextInputEditText emailInput;
    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText phoneInput;
    private Button saveButton;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        toolbar = findViewById(R.id.toolbar);
        loginInput = findViewById(R.id.login_input);
        emailInput = findViewById(R.id.email_input);
        firstNameInput = findViewById(R.id.first_name_input);
        lastNameInput = findViewById(R.id.last_name_input);
        phoneInput = findViewById(R.id.phone_input);
        saveButton = findViewById(R.id.save_button);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Редактирование профиля");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        apiService = ApiClient.getClient(this).create(ApiService.class);
        loadProfile();

        // Кнопка сохранения
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
    }

    private void loadProfile() {
        apiService.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    
                    if (user.login != null) {
                        loginInput.setText(user.login);
                    }
                    if (user.email != null) {
                        emailInput.setText(user.email);
                    }
                    if (user.first_name != null) {
                        firstNameInput.setText(user.first_name);
                    }
                    if (user.last_name != null) {
                        lastNameInput.setText(user.last_name);
                    }
                    if (user.phone_number != null) {
                        phoneInput.setText(user.phone_number);
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
            }
        });
    }

    private void saveProfile() {
        String email = getText(emailInput);
        String firstName = getText(firstNameInput);
        String lastName = getText(lastNameInput);
        String phone = getText(phoneInput);

        if (email.isEmpty()) {
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Сохранение...");

        UpdateProfileRequest request = new UpdateProfileRequest(email, firstName, lastName, phone);

        apiService.updateProfile(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                saveButton.setEnabled(true);
                saveButton.setText("Сохранить");

                if (response.isSuccessful()) {
                    finish();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                saveButton.setEnabled(true);
                saveButton.setText("Сохранить");
            }
        });
    }

    private String getText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}







