package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView gamesRecyclerView;
    private GameAdapter gameAdapter;
    private ApiService apiService;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton cartFab;
    private BadgeDrawable cartBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "MainActivity has started successfully!");

        gamesRecyclerView = findViewById(R.id.games_recycler_view);
        swipeRefresh = findViewById(R.id.main_swipe_refresh);
        cartFab = findViewById(R.id.cart_fab);
        int spanCount = getResources().getInteger(R.integer.grid_column_count);
        gamesRecyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        
        if (cartFab != null) cartFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);
        });

        initApiService();
        initCartBadge();

        // Каталог грузим сразу. Предупреждение 18+ показываем только при клике на 18+ товар.
        fetchGameVariants();

        swipeRefresh.setOnRefreshListener(this::fetchGameVariants);
    }

    private void initApiService() {
        apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void initCartBadge() {
        if (cartFab == null) return;
        try {
            cartBadge = BadgeDrawable.create(this);
            cartBadge.setVisible(false);
            cartBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.color_secondary));
            cartBadge.setBadgeTextColor(ContextCompat.getColor(this, R.color.white));
            BadgeUtils.attachBadgeDrawable(cartBadge, cartFab);
        } catch (Throwable t) {
            // ignore (badge is optional)
        }
    }

    // НОВЫЙ МЕТОД для загрузки ВАРИАНТОВ
    private void fetchGameVariants() {
        Log.d(TAG, "Fetching game variants from server...");
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        apiService.getGameVariants().enqueue(new Callback<List<GameVariantForList>>() {
            @Override
            public void onResponse(Call<List<GameVariantForList>> call, Response<List<GameVariantForList>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Variants fetched successfully. Count: " + response.body().size());
                    displayVariants(response.body());
                } else {
                    Log.e(TAG, "Failed to fetch variants. Code: " + response.code());
                    Toast.makeText(MainActivity.this, "Не удалось загрузить товары", Toast.LENGTH_SHORT).show();
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<List<GameVariantForList>> call, Throwable t) {
                Log.e(TAG, "Network request failed for variants.", t);
                Toast.makeText(MainActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    // НОВЫЙ МЕТОД для отображения ВАРИАНТОВ
    private void displayVariants(List<GameVariantForList> variantList) {
        // Сортировка: "Нет в наличии" уводим вниз списка
        Collections.sort(variantList, new Comparator<GameVariantForList>() {
            @Override public int compare(GameVariantForList a, GameVariantForList b) {
                boolean aIn = a != null && a.getStatus() != null && "В наличии".equalsIgnoreCase(a.getStatus().getName());
                boolean bIn = b != null && b.getStatus() != null && "В наличии".equalsIgnoreCase(b.getStatus().getName());
                if (aIn == bIn) return 0;
                return aIn ? -1 : 1;
            }
        });
        gameAdapter = new GameAdapter(MainActivity.this, variantList, this::refreshCartBadge);
        gamesRecyclerView.setAdapter(gameAdapter);
    }

    private void refreshCartBadge() {
        if (cartBadge == null || apiService == null) return;
        apiService.getCart().enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                int count = 0;
                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                    for (CartResponse.CartServerItem it : response.body().getItems()) {
                        if (it == null) continue;
                        count += Math.max(0, it.getQuantity());
                    }
                }
                final int finalCount = count;
                runOnUiThread(() -> {
                    try {
                        if (finalCount > 0) {
                            cartBadge.setVisible(true);
                            cartBadge.setNumber(finalCount);
                        } else {
                            cartBadge.clearNumber();
                            cartBadge.setVisible(false);
                        }
                    } catch (Throwable ignored) {}
                });
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                // ignore
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCartBadge();
    }
}
