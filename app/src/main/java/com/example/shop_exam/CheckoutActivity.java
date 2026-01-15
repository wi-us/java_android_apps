package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    private ApiService apiService;
    private SwipeRefreshLayout swipeRefresh;
    private Toolbar toolbar;

    private AutoCompleteTextView addressInput;
    private com.google.android.material.textfield.TextInputEditText nameInput;
    private com.google.android.material.textfield.TextInputEditText phoneInput;
    private com.google.android.material.textfield.TextInputEditText apartmentInput;
    private com.google.android.material.textfield.TextInputEditText commentInput;
    private TextView totalText;
    private Button submitButton;
    private Button pickOnMapButton;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable addressDebounceRunnable;
    private ArrayAdapter<AddressSuggestion> addressAdapter;
    private String selectedPlaceId;
    private Double selectedLat = null;
    private Double selectedLon = null;
    private static final int REQ_PICK_MAP = 701;

    private double currentTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.checkout_swipe_refresh);
        addressInput = findViewById(R.id.address_autocomplete);
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        apartmentInput = findViewById(R.id.apartment_input);
        commentInput = findViewById(R.id.comment_input);
        totalText = findViewById(R.id.total_text);
        submitButton = findViewById(R.id.submit_order_button);
        pickOnMapButton = findViewById(R.id.pick_on_map_button);

        initToolbar();

        addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        addressInput.setAdapter(addressAdapter);
        // Внутри NestedScrollView иногда dropdown не привязывается корректно — задаём якорь явно.
        try {
            addressInput.setDropDownAnchor(R.id.address_autocomplete);
        } catch (Throwable t) {
            // ignore
        }
        // меньше порог => подсказки появляются раньше (и стабильнее для русской IME-композиции)
        addressInput.setThreshold(2);

        addressInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                // afterTextChanged лучше работает с русской раскладкой (IME иногда не "коммитит" текст до пробела)
                selectedPlaceId = null; // оставим как флаг "выбрано из подсказок" (не используем как id)
                selectedLat = null;
                selectedLon = null;
                scheduleAddressSuggest(s == null ? "" : s.toString());
            }
        });

        addressInput.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof AddressSuggestion) {
                AddressSuggestion s = (AddressSuggestion) item;
                selectedPlaceId = s.getId();
                selectedLat = s.getLat();
                selectedLon = s.getLon();
                // У 2ГИС suggest уже отдаёт готовую строку (обычно без "Россия,")
                if (s.getText() != null) {
                    try {
                        addressInput.setText(s.getText(), false);
                    } catch (Throwable t) {
                        addressInput.setText(s.getText());
                    }
                    addressInput.setSelection(addressInput.getText().length());
                }
                // На всякий случай уточняем координаты через geocoder по выбранной строке
                resolveSelectedAddress(s.getText());
            }
        });

        swipeRefresh.setOnRefreshListener(this::loadInitialData);

        submitButton.setOnClickListener(v -> submitOrder());
        pickOnMapButton.setOnClickListener(v -> openMapPicker());

        loadInitialData();
    }

    private void initToolbar() {
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Оплата");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void openMapPicker() {
        double lat = selectedLat != null ? selectedLat : 55.7558;
        double lon = selectedLon != null ? selectedLon : 37.6173;
        Intent intent = new Intent(CheckoutActivity.this, MapPickerActivity.class);
        intent.putExtra(MapPickerActivity.EXTRA_LAT, lat);
        intent.putExtra(MapPickerActivity.EXTRA_LON, lon);
        startActivityForResult(intent, REQ_PICK_MAP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_MAP && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra(MapPickerActivity.RESULT_LAT, selectedLat != null ? selectedLat : 55.7558);
            selectedLon = data.getDoubleExtra(MapPickerActivity.RESULT_LON, selectedLon != null ? selectedLon : 37.6173);
            String addr = data.getStringExtra(MapPickerActivity.RESULT_TEXT);
            if (addr != null && !addr.trim().isEmpty()) {
                try {
                    addressInput.setText(addr, false);
                } catch (Throwable t) {
                    addressInput.setText(addr);
                }
                addressInput.setSelection(addressInput.getText().length());
            }
            Toast.makeText(this, "Точка выбрана на карте", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadInitialData() {
        swipeRefresh.setRefreshing(true);
        loadMe();
        loadCartTotal();
    }

    private void loadMe() {
        apiService.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User u = response.body();
                    String defaultName = "";
                    if (u.first_name != null) defaultName += u.first_name.trim();
                    if (u.last_name != null && !u.last_name.trim().isEmpty()) {
                        if (!defaultName.isEmpty()) defaultName += " ";
                        defaultName += u.last_name.trim();
                    }
                    if (defaultName.isEmpty() && u.login != null) defaultName = u.login;

                    nameInput.setText(defaultName);

                    if (u.phone_number != null && !u.phone_number.trim().isEmpty()) {
                        phoneInput.setText(u.phone_number.trim());
                    }
                } else {
                    Toast.makeText(CheckoutActivity.this, "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show();
                }
                stopRefreshingIfDone();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                stopRefreshingIfDone();
            }
        });
    }

    private void loadCartTotal() {
        apiService.getCart().enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentTotal = response.body().getTotal();
                    totalText.setText(String.format(Locale.getDefault(), "Сумма к оплате  %.0f ₽", currentTotal));
                    submitButton.setEnabled(currentTotal > 0.0);
                } else {
                    Toast.makeText(CheckoutActivity.this, "Не удалось загрузить корзину", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(false);
                }
                stopRefreshingIfDone();
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                submitButton.setEnabled(false);
                stopRefreshingIfDone();
            }
        });
    }

    private int refreshDoneCount = 0;
    private void stopRefreshingIfDone() {
        refreshDoneCount++;
        if (refreshDoneCount >= 2) {
            swipeRefresh.setRefreshing(false);
            refreshDoneCount = 0;
        }
    }

    private void scheduleAddressSuggest(String query) {
        if (addressDebounceRunnable != null) handler.removeCallbacks(addressDebounceRunnable);
        addressDebounceRunnable = () -> fetchAddressSuggest(query);
        handler.postDelayed(addressDebounceRunnable, 350);
    }

    private void fetchAddressSuggest(String query) {
        // Важно: не делаем .trim() — для IME/русского ввода и пробелов в адресе так стабильнее.
        String q = query == null ? "" : query;
        if (q.length() < 2) {
            addressAdapter.clear();
            addressAdapter.notifyDataSetChanged();
            addressInput.dismissDropDown();
            return;
        }

        apiService.addressSuggest(q).enqueue(new Callback<List<AddressSuggestion>>() {
            @Override
            public void onResponse(Call<List<AddressSuggestion>> call, Response<List<AddressSuggestion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AddressSuggestion> items = response.body();
                    addressAdapter.clear();
                    addressAdapter.addAll(items);
                    addressAdapter.notifyDataSetChanged();
                    if (!items.isEmpty()) {
                        // showDropDown() иногда игнорируется если вызвать "сразу" — делаем через post()
                        addressInput.post(() -> {
                            if (addressInput.isShown()) addressInput.showDropDown();
                        });
                    } else {
                        addressInput.dismissDropDown();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AddressSuggestion>> call, Throwable t) {
                // тихо игнорируем, чтобы не спамить тостами при наборе
            }
        });
    }

    private void resolveSelectedAddress(String addressText) {
        if (addressText == null || addressText.trim().isEmpty()) return;
        apiService.addressDetails(addressText).enqueue(new Callback<AddressDetails>() {
            @Override
            public void onResponse(Call<AddressDetails> call, Response<AddressDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AddressDetails d = response.body();
                    selectedLat = d.getLat();
                    selectedLon = d.getLon();
                }
            }

            @Override
            public void onFailure(Call<AddressDetails> call, Throwable t) {
                // не спамим тостами
            }
        });
    }

    private void submitOrder() {
        String address = addressInput.getText() == null ? "" : addressInput.getText().toString().trim();
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        String phone = phoneInput.getText() == null ? "" : phoneInput.getText().toString().trim();
        String apartment = apartmentInput.getText() == null ? "" : apartmentInput.getText().toString().trim();
        String comment = commentInput.getText() == null ? "" : commentInput.getText().toString().trim();

        if (address.isEmpty()) {
            Toast.makeText(this, "Введите адрес", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentTotal <= 0.0) {
            Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Оформляем...");

        CheckoutRequest req = new CheckoutRequest(
                address,
                apartment.isEmpty() ? null : apartment,
                name,
                phone.isEmpty() ? null : phone,
                comment.isEmpty() ? null : comment
        );

        apiService.checkout(req).enqueue(new Callback<CheckoutResponse>() {
            @Override
            public void onResponse(Call<CheckoutResponse> call, Response<CheckoutResponse> response) {
                if (response.isSuccessful()) {
                    Intent i = new Intent(CheckoutActivity.this, OrderSuccessActivity.class);
                    startActivity(i);
                    finish();
                } else {
                    submitButton.setEnabled(true);
                    submitButton.setText("Оформить заказ");
                    Toast.makeText(CheckoutActivity.this, "Ошибка оформления: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                submitButton.setEnabled(true);
                submitButton.setText("Оформить заказ");
                Toast.makeText(CheckoutActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}


