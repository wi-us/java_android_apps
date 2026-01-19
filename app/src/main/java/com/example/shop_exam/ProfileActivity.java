package com.example.shop_exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран профиля пользователя
 */
public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView loginText;
    private TextView emailText;
    private TextView phoneText;
    private Button logoutButton;
    private com.google.android.material.floatingactionbutton.FloatingActionButton editButton;
    private RecyclerView ordersRecycler;
    private TextView noOrdersText;

    private ApiService apiService;
    private int loadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.profile_swipe_refresh);
        loginText = findViewById(R.id.profile_login);
        emailText = findViewById(R.id.profile_email);
        phoneText = findViewById(R.id.profile_phone);
        logoutButton = findViewById(R.id.logout_button);
        editButton = findViewById(R.id.edit_profile_button);
        ordersRecycler = findViewById(R.id.orders_recycler);
        noOrdersText = findViewById(R.id.no_orders_text);

        // toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Профиль");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Список заказов
        ordersRecycler.setLayoutManager(new LinearLayoutManager(this));

        // API
        apiService = ApiClient.getClient(this).create(ApiService.class);

        loadData();

        // свайпо
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData();
            }
        });

        // Кнопка выхода
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        // Кнопка редактирования профиля
        if (editButton != null) {
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    private void loadData() {
        swipeRefresh.setRefreshing(true);
        loadCount = 0;
        loadProfile();
        loadOrders();
    }

    private void checkLoadComplete() {
        loadCount++;
        if (loadCount >= 2) {
            swipeRefresh.setRefreshing(false);
            loadCount = 0;
        }
    }

    private void loadProfile() {
        apiService.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    showProfile(user);
                } else if (response.code() == 401 || response.code() == 403) {
                    // Токен невалидный = выход
                    logout();
                    return;
                }
                checkLoadComplete();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                checkLoadComplete();
            }
        });
    }

    private void showProfile(User user) {
        // Логин
        if (user.login != null && !user.login.isEmpty()) {
            loginText.setText(user.login);
        }

        // Email
        if (user.email != null && !user.email.isEmpty()) {
            emailText.setText(user.email);
            emailText.setVisibility(View.VISIBLE);
        } else {
            emailText.setVisibility(View.GONE);
        }

        // Телефон
        if (user.phone_number != null && !user.phone_number.isEmpty()) {
            phoneText.setText(user.phone_number);
            phoneText.setVisibility(View.VISIBLE);
        } else {
            phoneText.setVisibility(View.GONE);
        }
    }

    private void loadOrders() {
        apiService.getMyOrders().enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showOrders(response.body());
                } else {
                    showNoOrders();
                }
                checkLoadComplete();
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                showNoOrders();
                checkLoadComplete();
            }
        });
    }

    private void showOrders(List<OrderResponse> orders) {
        if (orders.isEmpty()) {
            showNoOrders();
            return;
        }

        noOrdersText.setVisibility(View.GONE);
        ordersRecycler.setVisibility(View.VISIBLE);

        OrderAdapter adapter = new OrderAdapter(orders, new OrderAdapter.OnOrderClickListener() {
            @Override
            public void onOrderClick(OrderResponse order) {
                openOrderDetail(order.getId());
            }
        });
        ordersRecycler.setAdapter(adapter);
    }

    private void showNoOrders() {
        noOrdersText.setVisibility(View.VISIBLE);
        ordersRecycler.setVisibility(View.GONE);
    }

    private void openOrderDetail(int orderId) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("order_id", orderId);
        startActivity(intent);
    }

    private void logout() {
        TokenStore.clearToken(this);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

