package com.example.shop_exam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

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
        errorLayout.setVisibility(View.VISIBLE);
    }

    // Скрыть экран ошибки
    private void hideError() {
        errorLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    // Загрузка списка товаров с сервера
    private void loadProducts() {
        swipeRefresh.setRefreshing(true);
        hideError();

        apiService.getGameVariants().enqueue(new Callback<List<GameVariantForList>>() {
            @Override
            public void onResponse(Call<List<GameVariantForList>> call,
                                   Response<List<GameVariantForList>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    hideError();
                    showProducts(response.body());
                } else {
                    showError();
                }
            }

            @Override
            public void onFailure(Call<List<GameVariantForList>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showError();
            }
        });
    }

    // Отображение списка товаров
    private void showProducts(List<GameVariantForList> products) {
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
}
