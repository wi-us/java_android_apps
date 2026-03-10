package com.example.shop_exam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.view.inputmethod.InputMethodManager;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Главный экран приложения с каталогом товаров
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private ApiService apiService;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton cartButton;
    private FloatingActionButton profileButton;
    private LinearLayout errorLayout;
    private Button retryButton;
    private EditText searchEditText;
    private Button searchButton;
    private View emptySearchLayout;
    private ImageButton filterButton;

    // Состояние фильтров (null = не задано)
    private Integer filterMinPlayers;
    private Integer filterMaxPlayers;
    private Integer filterMinAge;
    private Double filterPriceMin;
    private Double filterPriceMax;
    private Boolean filterInStock;

    private static final long SEARCH_DELAY_MS = 1000;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    /** Текущий запрос каталога; отменяется при новом поиске. */
    private Call<List<GameVariantForList>> productsCall;

    // Запрос нескольких разрешений сразу
    private final ActivityResultLauncher<String[]> permissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Запрос всех необходимых разрешений
        requestAllPermissions();

        recyclerView = findViewById(R.id.games_recycler_view);
        swipeRefresh = findViewById(R.id.main_swipe_refresh);
        cartButton = findViewById(R.id.cart_fab);
        profileButton = findViewById(R.id.profile_fab);
        errorLayout = findViewById(R.id.error_layout);
        retryButton = findViewById(R.id.retry_button);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        emptySearchLayout = findViewById(R.id.empty_search_layout);
        filterButton = findViewById(R.id.filter_button);
        View searchBar = findViewById(R.id.search_bar);

        searchRunnable = () -> loadProducts();

        // Тап по панели поиска — фокус в поле и показ клавиатуры
        if (searchBar != null && searchEditText != null) {
            searchBar.setOnClickListener(v -> {
                searchEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }

        // Поиск: по кнопке «Найти»
        if (searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performSearch();
                }
            });
        }
        // Поиск: по клавише «Поиск» на клавиатуре
        if (searchEditText != null) {
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            });
            // Явный показ клавиатуры при фокусе (обходит проблему "not passing ime action check")
            searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
            // Поиск через 2 секунды после последнего введённого символа
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    searchHandler.removeCallbacks(searchRunnable);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                }
            });
        }

        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }

        // Сетка товаров
        int columns = getResources().getInteger(R.integer.grid_column_count);
        recyclerView.setLayoutManager(new GridLayoutManager(this, columns));

        // Кнопка корзины
        if (cartButton != null) {
            cartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, CartActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Кнопка профиля
        if (profileButton != null) {
            profileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String token = TokenStore.getToken(MainActivity.this);
                    if (token != null && !token.isEmpty()) {
                        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                }
            });
        }

        // Кнопка повторной загрузки
        if (retryButton != null) {
            retryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadProducts();
                }
            });
        }

        apiService = ApiClient.getClient(this).create(ApiService.class);
        loadProducts();

        // Обновление свайпом вниз
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadProducts();
            }
        });
    }

    // Запрос всех необходимых разрешений при запуске
    private void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Разрешение на геолокацию
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Разрешение на уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Если есть разрешения для запроса - запрашиваем
        if (!permissionsToRequest.isEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    // Показать экран ошибки
    private void showError() {
        recyclerView.setVisibility(View.GONE);
        emptySearchLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
    }

    // Скрыть экран ошибки
    private void hideError() {
        errorLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptySearchLayout.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        emptySearchLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    /** Выполняет поиск по текущему тексту в строке поиска и обновляет список. */
    private void performSearch() {
        loadProducts();
    }

    /** Возвращает текущий поисковый запрос (или null, если пусто). */
    private String getSearchQuery() {
        if (searchEditText == null) return null;
        String s = searchEditText.getText() != null ? searchEditText.getText().toString().trim() : "";
        return s.isEmpty() ? null : s;
    }

    private void showFilterDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filters, null);
        EditText minPlayersView = view.findViewById(R.id.filter_min_players);
        EditText maxPlayersView = view.findViewById(R.id.filter_max_players);
        Spinner ageSpinner = view.findViewById(R.id.filter_age_spinner);
        EditText priceMinView = view.findViewById(R.id.filter_price_min);
        EditText priceMaxView = view.findViewById(R.id.filter_price_max);
        CheckBox inStockView = view.findViewById(R.id.filter_in_stock);
        Button resetBtn = view.findViewById(R.id.filter_reset_btn);
        Button applyBtn = view.findViewById(R.id.filter_apply_btn);

        String[] ageOptions = { getString(R.string.filter_any), "6+", "12+", "18+" };
        ageSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ageOptions));
        if (filterMinAge != null) {
            int pos = filterMinAge == 6 ? 1 : filterMinAge == 12 ? 2 : filterMinAge == 18 ? 3 : 0;
            ageSpinner.setSelection(pos);
        }
        if (filterMinPlayers != null) minPlayersView.setText(String.valueOf(filterMinPlayers));
        if (filterMaxPlayers != null) maxPlayersView.setText(String.valueOf(filterMaxPlayers));
        if (filterPriceMin != null) priceMinView.setText(String.valueOf(filterPriceMin.intValue()));
        if (filterPriceMax != null) priceMaxView.setText(String.valueOf(filterPriceMax.intValue()));
        inStockView.setChecked(Boolean.TRUE.equals(filterInStock));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.filters_button)
                .setView(view)
                .create();

        resetBtn.setOnClickListener(v2 -> {
            filterMinPlayers = null;
            filterMaxPlayers = null;
            filterMinAge = null;
            filterPriceMin = null;
            filterPriceMax = null;
            filterInStock = null;
            minPlayersView.setText("");
            maxPlayersView.setText("");
            ageSpinner.setSelection(0);
            priceMinView.setText("");
            priceMaxView.setText("");
            inStockView.setChecked(false);
            loadProducts();
            dialog.dismiss();
        });

        applyBtn.setOnClickListener(v2 -> {
            filterMinPlayers = parsePositiveInt(minPlayersView.getText());
            filterMaxPlayers = parsePositiveInt(maxPlayersView.getText());
            int agePos = ageSpinner.getSelectedItemPosition();
            filterMinAge = agePos == 0 ? null : (agePos == 1 ? 6 : agePos == 2 ? 12 : 18);
            filterPriceMin = parsePositiveDouble(priceMinView.getText());
            filterPriceMax = parsePositiveDouble(priceMaxView.getText());
            filterInStock = inStockView.isChecked() ? Boolean.TRUE : null;
            loadProducts();
            dialog.dismiss();
        });

        dialog.show();
    }

    private static Integer parsePositiveInt(CharSequence s) {
        if (s == null || s.toString().trim().isEmpty()) return null;
        try {
            int v = Integer.parseInt(s.toString().trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parsePositiveDouble(CharSequence s) {
        if (s == null || s.toString().trim().isEmpty()) return null;
        try {
            double v = Double.parseDouble(s.toString().trim().replace(',', '.'));
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Загрузка списка товаров с сервера
    private void loadProducts() {
        if (productsCall != null) {
            productsCall.cancel();
            productsCall = null;
        }
        swipeRefresh.setRefreshing(true);
        hideError();
        hideEmptyState();

        final String queryAtRequest = getSearchQuery();
        productsCall = apiService.getGameVariants(
                queryAtRequest,
                filterMinPlayers,
                filterMaxPlayers,
                filterMinAge,
                filterPriceMin,
                filterPriceMax,
                filterInStock
        );
        productsCall.enqueue(new Callback<List<GameVariantForList>>() {
            @Override
            public void onResponse(Call<List<GameVariantForList>> call,
                                   Response<List<GameVariantForList>> response) {
                if (productsCall == call) productsCall = null;
                swipeRefresh.setRefreshing(false);
                if (call.isCanceled()) return;
                if (!Objects.equals(queryAtRequest, getSearchQuery())) return;
                if (response.isSuccessful() && response.body() != null) {
                    hideError();
                    showProducts(response.body());
                } else {
                    showError();
                }
            }

            @Override
            public void onFailure(Call<List<GameVariantForList>> call, Throwable t) {
                if (productsCall == call) productsCall = null;
                swipeRefresh.setRefreshing(false);
                if (call.isCanceled()) return;
                if (!Objects.equals(queryAtRequest, getSearchQuery())) return;
                showError();
            }
        });
    }

    // Отображение списка товаров
    private void showProducts(List<GameVariantForList> products) {
        if (products.isEmpty()) {
            showEmptyState();
            return;
        }
        hideEmptyState();
        // Сортировка - товары в наличии показываем первыми
        Collections.sort(products, new Comparator<GameVariantForList>() {
            @Override
            public int compare(GameVariantForList a, GameVariantForList b) {
                boolean aInStock = a.getStatus() != null &&
                        "В наличии".equalsIgnoreCase(a.getStatus().getName());
                boolean bInStock = b.getStatus() != null &&
                        "В наличии".equalsIgnoreCase(b.getStatus().getName());

                if (aInStock == bInStock) return 0;
                return aInStock ? -1 : 1;
            }
        });

        adapter = new GameAdapter(this, products);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (productsCall != null) {
            productsCall.cancel();
            productsCall = null;
        }
    }
}
