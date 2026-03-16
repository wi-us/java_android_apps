package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран оформления заказа
 */
public class CheckoutActivity extends AppCompatActivity {

    private static final int REQUEST_MAP = 701;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private AutoCompleteTextView addressInput;
    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText apartmentInput;
    private TextInputEditText commentInput;
    private TextView totalText;
    private Button submitButton;
    private Button mapButton;

    private ApiService apiService;
    private ArrayAdapter<AddressSuggestion> addressAdapter;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Координаты выбранного адреса
    private Double selectedLat = null;
    private Double selectedLon = null;

    // Сумма заказа
    private double orderTotal = 0.0;

    // Счётчик для отслеживания загрузки
    private int loadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        findViews();
        setupToolbar();
        apiService = ApiClient.getClient(this).create(ApiService.class);
        setupAddressAutocomplete();
        loadData();

        // Обновление свайпом
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData();
            }
        });

        // Кнопка "Выбрать на карте"
        mapButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                openMap();
            }
        });

        // Кнопка "Оформить заказ"
        submitButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                submitOrder();
            }
        });
    }

    // Поиск элементов интерфейса
    private void findViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.checkout_swipe_refresh);
        addressInput = findViewById(R.id.address_autocomplete);
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        apartmentInput = findViewById(R.id.apartment_input);
        commentInput = findViewById(R.id.comment_input);
        totalText = findViewById(R.id.total_text);
        submitButton = findViewById(R.id.submit_order_button);
        mapButton = findViewById(R.id.pick_on_map_button);
    }

    // Настройка toolbar
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Оплата");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                finish();
            }
        });
    }

    // Настройка автодополнения адреса
    private void setupAddressAutocomplete() {
        addressAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        addressInput.setAdapter(addressAdapter);
        addressInput.setThreshold(2);

        // Слушатель изменения текста
        addressInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                selectedLat = null;
                selectedLon = null;
                String text = (s != null) ? s.toString() : "";
                scheduleAddressSearch(text);
            }
        });

        // Выбор подсказки из списка
        addressInput.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof AddressSuggestion) {
                AddressSuggestion suggestion = (AddressSuggestion) item;
                selectedLat = suggestion.getLat();
                selectedLon = suggestion.getLon();

                if (suggestion.getText() != null) {
                    addressInput.setText(suggestion.getText());
                    addressInput.setSelection(addressInput.getText().length());
                }

                getAddressCoordinates(suggestion.getText());
            }
        });
    }

    // Загрузка начальных данных
    private void loadData() {
        swipeRefresh.setRefreshing(true);
        loadCount = 0;
        loadProfile();
        loadCartTotal();
    }

    // Проверка завершения загрузки
    private void checkLoadComplete() {
        loadCount++;
        if (loadCount >= 2) {
            swipeRefresh.setRefreshing(false);
            loadCount = 0;
        }
    }

    // Загрузка профиля пользователя
    private void loadProfile() {
        apiService.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();

                    // Имя пользователя
                    String name = "";
                    if (user.first_name != null) {
                        name = user.first_name.trim();
                    }
                    if (user.last_name != null && !user.last_name.trim().isEmpty()) {
                        if (!name.isEmpty()) name += " ";
                        name += user.last_name.trim();
                    }
                    if (name.isEmpty() && user.login != null) {
                        name = user.login;
                    }
                    nameInput.setText(name);

                    // Телефон
                    if (user.phone_number != null && !user.phone_number.trim().isEmpty()) {
                        phoneInput.setText(user.phone_number.trim());
                    }
                }
                checkLoadComplete();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                checkLoadComplete();
            }
        });
    }

    // Загрузка суммы корзины
    private void loadCartTotal() {
        apiService.getCart().enqueue(new Callback<ServerCartResponse>() {
            @Override
            public void onResponse(Call<ServerCartResponse> call, Response<ServerCartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderTotal = response.body().total;
                    totalText.setText(String.format(Locale.getDefault(),
                            "Сумма к оплате  %.0f ₽", orderTotal));
                    submitButton.setEnabled(orderTotal > 0);
                } else {
                    submitButton.setEnabled(false);
                }
                checkLoadComplete();
            }

            @Override
            public void onFailure(Call<ServerCartResponse> call, Throwable t) {
                submitButton.setEnabled(false);
                checkLoadComplete();
            }
        });
    }

    // Задержка перед поиском адреса
    private void scheduleAddressSearch(String query) {
        if (debounceRunnable != null) {
            handler.removeCallbacks(debounceRunnable);
        }
        debounceRunnable = new Runnable() {
            @Override
            public void run() {
                searchAddress(query);
            }
        };
        handler.postDelayed(debounceRunnable, 350);
    }

    // Поиск адресов
    private void searchAddress(String query) {
        if (query == null || query.length() < 2) {
            addressAdapter.clear();
            addressAdapter.notifyDataSetChanged();
            addressInput.dismissDropDown();
            return;
        }

        apiService.addressSuggest(query).enqueue(new Callback<List<AddressSuggestion>>() {
            @Override
            public void onResponse(Call<List<AddressSuggestion>> call,
                                   Response<List<AddressSuggestion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AddressSuggestion> suggestions = response.body();
                    addressAdapter.clear();
                    addressAdapter.addAll(suggestions);
                    addressAdapter.notifyDataSetChanged();

                    if (!suggestions.isEmpty()) {
                        addressInput.post(new Runnable() {
                            @Override
                            public void run() {
                                if (addressInput.isShown()) {
                                    addressInput.showDropDown();
                                }
                            }
                        });
                    } else {
                        addressInput.dismissDropDown();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AddressSuggestion>> call, Throwable t) {
            }
        });
    }

    // Получение координат адреса
    private void getAddressCoordinates(String address) {
        if (address == null || address.trim().isEmpty()) return;

        apiService.addressDetails(address).enqueue(new Callback<AddressDetails>() {
            @Override
            public void onResponse(Call<AddressDetails> call, Response<AddressDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AddressDetails details = response.body();
                    selectedLat = details.getLat();
                    selectedLon = details.getLon();
                }
            }

            @Override
            public void onFailure(Call<AddressDetails> call, Throwable t) {
            }
        });
    }

    // Открытие карты для выбора адреса
    private void openMap() {
        double lat = (selectedLat != null) ? selectedLat : 55.7558;
        double lon = (selectedLon != null) ? selectedLon : 37.6173;

        Intent intent = new Intent(this, MapPickerActivity.class);
        intent.putExtra(MapPickerActivity.EXTRA_LAT, lat);
        intent.putExtra(MapPickerActivity.EXTRA_LON, lon);
        startActivityForResult(intent, REQUEST_MAP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MAP && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra(MapPickerActivity.RESULT_LAT, 55.7558);
            selectedLon = data.getDoubleExtra(MapPickerActivity.RESULT_LON, 37.6173);
            String address = data.getStringExtra(MapPickerActivity.RESULT_TEXT);

            if (address != null && !address.trim().isEmpty()) {
                addressInput.setText(address);
                addressInput.setSelection(addressInput.getText().length());
            }

        }
    }

    // Оформление заказа
    private void submitOrder() {
        String address = getText(addressInput);
        String name = getText(nameInput);
        String phone = getText(phoneInput);
        String apartment = getText(apartmentInput);
        String comment = getText(commentInput);

        if (address.isEmpty()) {
            return;
        }
        if (name.isEmpty()) {
            return;
        }
        if (orderTotal <= 0) {
            return;
        }

        // Блокируем кнопку
        submitButton.setEnabled(false);
        submitButton.setText("Оформляем...");

        // Создание запроса
        CheckoutRequest request = new CheckoutRequest(
                address,
                apartment.isEmpty() ? null : apartment,
                name,
                phone.isEmpty() ? null : phone,
                comment.isEmpty() ? null : comment
        );

        // Отправка его на сервер
        apiService.checkout(request).enqueue(new Callback<CheckoutResponse>() {
            @Override
            public void onResponse(Call<CheckoutResponse> call, Response<CheckoutResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CheckoutResponse result = response.body();
                    
                    // очистка локальной корзины после успешного заказа
                    LocalCartStore.clear(CheckoutActivity.this);
                    
                    // отправка Push уведомление
                    NotificationHelper.showOrderNotification(
                            CheckoutActivity.this,
                            result.getOrderId(),
                            result.getTotal()
                    );
                    
                    // Успех - переход на экран подтверждения
                    Intent intent = new Intent(CheckoutActivity.this, OrderSuccessActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    submitButton.setEnabled(true);
                    submitButton.setText("Оформить заказ");
                }
            }

            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                submitButton.setEnabled(true);
                submitButton.setText("Оформить заказ");
            }
        });
    }

    // Вспомогательный метод для получения текста из поля
    private String getText(android.widget.EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}
