package com.example.shop_exam;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран деталей заказа
 */
public class OrderDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView orderNumber;
    private TextView orderStatus;
    private TextView orderDate;
    private TextView orderAddress;
    private TextView orderCommentLabel;
    private TextView orderComment;
    private TextView orderTotal;
    private RecyclerView itemsRecycler;

    private ApiService apiService;
    private int orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // ID заказа
        orderId = getIntent().getIntExtra("order_id", -1);
        if (orderId == -1) {
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.order_swipe_refresh);
        orderNumber = findViewById(R.id.order_number);
        orderStatus = findViewById(R.id.order_status);
        orderDate = findViewById(R.id.order_date);
        orderAddress = findViewById(R.id.order_address);
        orderCommentLabel = findViewById(R.id.order_comment_label);
        orderComment = findViewById(R.id.order_comment);
        orderTotal = findViewById(R.id.order_total);
        itemsRecycler = findViewById(R.id.items_recycler);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Заказ №" + orderId);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Настройка списка товаров
        itemsRecycler.setLayoutManager(new LinearLayoutManager(this));

        // API
        apiService = ApiClient.getClient(this).create(ApiService.class);
        loadOrder();

        // Обновление свайпом
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadOrder();
            }
        });
    }

    private void loadOrder() {
        swipeRefresh.setRefreshing(true);

        apiService.getOrderDetail(orderId).enqueue(new Callback<OrderDetailResponse>() {
            @Override
            public void onResponse(Call<OrderDetailResponse> call, Response<OrderDetailResponse> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    showOrder(response.body());
                } else {
                    finish();
                }
            }

            @Override
            public void onFailure(Call<OrderDetailResponse> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                finish();
            }
        });
    }

    private void showOrder(OrderDetailResponse order) {
        // Номер заказа
        orderNumber.setText("Заказ №" + order.getId());

        // Статус
        if (order.getStatus() != null) {
            orderStatus.setText(order.getStatus().getName());
            orderStatus.setVisibility(View.VISIBLE);
        } else {
            orderStatus.setVisibility(View.GONE);
        }

        // Дата
        orderDate.setText(formatDate(order.getCreatedAt()));

        // Адрес
        if (order.getAddress() != null && !order.getAddress().isEmpty()) {
            orderAddress.setText(order.getAddress());
        } else {
            orderAddress.setText("Не указан");
        }

        // Комментарий
        if (order.getComment() != null && !order.getComment().isEmpty()) {
            orderCommentLabel.setVisibility(View.VISIBLE);
            orderComment.setVisibility(View.VISIBLE);
            orderComment.setText(order.getComment());
        } else {
            orderCommentLabel.setVisibility(View.GONE);
            orderComment.setVisibility(View.GONE);
        }

        // Итого
        orderTotal.setText(String.format(Locale.getDefault(), "%.0f ₽", order.getTotalPrice()));

        // Список товаров
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(this, order.getItems());
            itemsRecycler.setAdapter(adapter);
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFormat.parse(isoDate);
            SimpleDateFormat displayFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
            return displayFormat.format(date);
        } catch (Exception e) {
            return isoDate;
        }
    }
}







