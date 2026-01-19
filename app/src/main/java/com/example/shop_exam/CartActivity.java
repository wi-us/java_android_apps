package com.example.shop_exam;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран корзины покупок
 */
public class CartActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private Button checkoutButton;
    private SwipeRefreshLayout swipeRefresh;
    private android.widget.TextView emptyCartText;
    private ApiService apiService;

    private List<CartItem> cartItems = new ArrayList<>();

    // Для отрисовки фона при свайпе
    private Paint swipePaint;
    private Drawable deleteIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // поиск элементов интерфейса
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.cart_recycler_view);
        checkoutButton = findViewById(R.id.pay_button);
        swipeRefresh = findViewById(R.id.cart_swipe_refresh);
        emptyCartText = findViewById(R.id.empty_cart_text);

        // Инициализируем API
        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Настраиваем цвет и иконку для свайпа
        swipePaint = new Paint();
        swipePaint.setColor(ContextCompat.getColor(this, R.color.color_delete_bg));
        deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_close);

        // Настройка
        setupToolbar();
        setupRecyclerView();
        loadCart();

        // Обновление свайпом
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadCart();
            }
        });

        // Кнопка оформления заказа (с проверкой авторизации)
        checkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkoutButton.isEnabled()) {
                    if (isUserLoggedIn()) {
                        syncCartAndCheckout();
                    } else {
                        // Переход на авторизацию, если вход не выполнен
                        Intent intent = new Intent(CartActivity.this, LoginActivity.class);
                        intent.putExtra("return_to_cart", true);
                        startActivity(intent);
                    }
                }
            }
        });
    }

    // Проверка авторизации
    private boolean isUserLoggedIn() {
        String token = TokenStore.getToken(this);
        return token != null && !token.isEmpty();
    }

    // Синхронизация локальной корзины с сервером
    private void syncCartAndCheckout() {
        Map<Integer, Integer> localItems = LocalCartStore.getItems(this);
        
        if (localItems.isEmpty()) {
            // Если корзина пуста - просто переходим
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
            return;
        }

        // Показываем прогресс
        checkoutButton.setEnabled(false);
        checkoutButton.setText("Загрузка...");

        // Добавляем каждый товар на сервер
        syncCount = localItems.size();
        syncedCount = 0;
        syncSuccess = true;

        for (Map.Entry<Integer, Integer> entry : localItems.entrySet()) {
            int variantId = entry.getKey();
            int quantity = entry.getValue();
            addToServerCart(variantId, quantity);
        }
    }

    private int syncCount = 0;
    private int syncedCount = 0;
    private boolean syncSuccess = true;

    // Добавление товара в серверную корзину
    private void addToServerCart(int variantId, int quantity) {
        CartAddRequest request = new CartAddRequest(variantId, quantity);
        
        apiService.addToCart(request).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (!response.isSuccessful()) {
                    syncSuccess = false;
                }
                checkSyncComplete();
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                syncSuccess = false;
                checkSyncComplete();
            }
        });
    }

    // Проверка завершения синхронизации
    private void checkSyncComplete() {
        syncedCount++;
        if (syncedCount >= syncCount) {
            if (syncSuccess) {
                // Переходим к оформлению (корзина очистится после успешного заказа)
                Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
                startActivity(intent);
            }
            
            // Восстанавливаем кнопку
            recalculateTotal();
        }
    }

    // Настройка toolbar
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Корзина");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // Настройка RecyclerView
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(this, cartItems);
        recyclerView.setAdapter(adapter);

        // Слушатель изменения количества
        adapter.setListener(new CartAdapter.OnQuantityChangeListener() {
            @Override
            public void onQuantityChange(int position, int newQuantity) {
                updateItemQuantity(position, newQuantity);
            }
        });

        // Свайп для удаления
        ItemTouchHelper swipeHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                        int position = vh.getAdapterPosition();
                        deleteItem(position);
                    }

                    @Override
                    public void onChildDraw(Canvas c, RecyclerView rv, RecyclerView.ViewHolder vh,
                                            float dX, float dY, int actionState, boolean isActive) {
                        // Рисуем красный фон при свайпе
                        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                            View item = vh.itemView;
                            drawSwipeBackground(c, item, dX);
                        }
                        super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive);
                    }
                });
        swipeHelper.attachToRecyclerView(recyclerView);
    }

    // Отрисовка фона при свайпе
    private void drawSwipeBackground(Canvas canvas, View item, float dX) {
        float height = item.getBottom() - item.getTop();
        float width = height / 3f;

        // Прямоугольник фона
        RectF background;
        if (dX > 0) {
            background = new RectF(item.getLeft(), item.getTop(),
                    item.getLeft() + dX, item.getBottom());
        } else {
            background = new RectF(item.getRight() + dX, item.getTop(),
                    item.getRight(), item.getBottom());
        }

        // Прозрачность зависит от расстояния свайпа
        int alpha = (int) (Math.min(1f, Math.abs(dX) / item.getWidth()) * 255);
        swipePaint.setAlpha(alpha);
        canvas.drawRoundRect(background, 16f, 16f, swipePaint);

        // Иконка удаления
        if (deleteIcon != null) {
            int iconSize = (int) (height / 3f);
            int iconTop = item.getTop() + (int) ((height - iconSize) / 2f);
            int iconBottom = iconTop + iconSize;
            int iconLeft, iconRight;

            if (dX > 0) {
                iconLeft = item.getLeft() + (int) (width / 2f);
                iconRight = iconLeft + iconSize;
            } else {
                iconRight = item.getRight() - (int) (width / 2f);
                iconLeft = iconRight - iconSize;
            }

            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            deleteIcon.setAlpha(alpha);
            deleteIcon.draw(canvas);
        }
    }

    // Загрузка корзины из локального хранилища
    private void loadCart() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        
        swipeRefresh.setRefreshing(true);
        cartItems.clear();

        Map<Integer, Integer> localItems = LocalCartStore.getItems(this);
        
        if (localItems.isEmpty()) {
            swipeRefresh.setRefreshing(false);
            adapter.notifyDataSetChanged();
            updateCheckoutButton(0);
            isLoading = false;
            return;
        }

        // Загрузка информации о товарах с сервера
        loadingCount = localItems.size();
        loadedCount = 0;
        
        for (Map.Entry<Integer, Integer> entry : localItems.entrySet()) {
            int variantId = entry.getKey();
            int quantity = entry.getValue();
            loadVariantInfo(variantId, quantity);
        }
    }

    private int loadingCount = 0;
    private int loadedCount = 0;
    private boolean isLoading = false;

    // Загрузка информации о товаре
    private void loadVariantInfo(int variantId, int quantity) {
        apiService.getGameDetails(variantId).enqueue(new Callback<GameDetail>() {
            @Override
            public void onResponse(Call<GameDetail> call, Response<GameDetail> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GameDetail detail = response.body();
                    String title = "";
                    if (detail.getGame() != null) {
                        title = detail.getGame().getTitle();
                    }
                    String image = detail.getImageLink();
                    double price = detail.getActualPrice();
                    
                    cartItems.add(new CartItem(variantId, title, image, price, quantity));
                }
                checkLoadComplete();
            }

            @Override
            public void onFailure(Call<GameDetail> call, Throwable t) {
                // Если не удалось загрузить - удаляем из локальной корзины
                LocalCartStore.removeItem(CartActivity.this, variantId);
                checkLoadComplete();
            }
        });
    }

    // Проверка завершения загрузки всех товаров
    private void checkLoadComplete() {
        loadedCount++;
        if (loadedCount >= loadingCount) {
            swipeRefresh.setRefreshing(false);
            isLoading = false;
            adapter.notifyDataSetChanged();
            
            // Пересчёт итоговой суммы
            double total = 0;
            for (CartItem item : cartItems) {
                total += item.getPrice() * item.getQuantity();
            }
            updateCheckoutButton(total);
        }
    }

    // Обновление кнопки оформления заказа
    private void updateCheckoutButton(double total) {
        if (total > 0) {
            checkoutButton.setEnabled(true);
            checkoutButton.setText(String.format(Locale.getDefault(), "Оплатить %.0f ₽", total));
            emptyCartText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            checkoutButton.setEnabled(false);
            checkoutButton.setText("Оплатить");
            emptyCartText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    // Изменение количества товара
    private void updateItemQuantity(int position, int newQuantity) {
        if (position < 0 || position >= cartItems.size()) return;

        CartItem item = cartItems.get(position);
        int variantId = item.getGameVariantId();

        if (newQuantity <= 0) {
            // Удаление товара
            LocalCartStore.removeItem(this, variantId);
            cartItems.remove(position);
            adapter.notifyItemRemoved(position);
        } else {
            // Обновление количества
            LocalCartStore.setQuantity(this, variantId, newQuantity);
            item.setQuantity(newQuantity);
            adapter.notifyItemChanged(position);
        }

        // Синхронизация с сервером если авторизован
        syncQuantityToServer(variantId, newQuantity);

        recalculateTotal();
    }

    // Удаление товара из корзины
    private void deleteItem(int position) {
        if (position < 0 || position >= cartItems.size()) return;

        int variantId = cartItems.get(position).getGameVariantId();

        // Удаление из локального хранилища
        LocalCartStore.removeItem(this, variantId);

        // Удаление из списка
        cartItems.remove(position);
        adapter.notifyItemRemoved(position);

        // Синхронизация с сервером если авторизован (quantity=0 удаляет товар)
        syncQuantityToServer(variantId, 0);

        recalculateTotal();
    }

    // Синхронизация количества товара с сервером
    private void syncQuantityToServer(int variantId, int quantity) {
        if (!isUserLoggedIn()) {
            return;
        }
        
        CartSetQuantityRequest request = new CartSetQuantityRequest(quantity);
        apiService.setCartItemQuantity(variantId, request).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                // Синхронизация завершена
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                // Ошибка синхронизации
            }
        });
    }

    // Пересчёт итоговой суммы
    private void recalculateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }
        updateCheckoutButton(total);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }
}
