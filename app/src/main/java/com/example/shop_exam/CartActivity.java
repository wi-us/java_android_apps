package com.example.shop_exam;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.graphics.drawable.Drawable;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private Button payButton;
    private List<CartItem> cartItems = new ArrayList<>();
    private ApiService apiService;
    private SwipeRefreshLayout swipeRefresh;
    private Paint swipePaint;
    private Drawable swipeDeleteIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        toolbar = findViewById(R.id.toolbar);
        cartRecyclerView = findViewById(R.id.cart_recycler_view);
        payButton = findViewById(R.id.pay_button);
        swipeRefresh = findViewById(R.id.cart_swipe_refresh);
        apiService = ApiClient.getClient(this).create(ApiService.class);
        swipePaint = new Paint();
        swipePaint.setColor(ContextCompat.getColor(this, R.color.color_delete_bg));
        swipeDeleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_close);

        initToolbar();
        setupRecyclerView();
        loadCartItems();

        swipeRefresh.setOnRefreshListener(this::loadCartItems);

        payButton.setOnClickListener(v -> {
            if (!payButton.isEnabled()) return;
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });
    }

    private void initToolbar() {
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Корзина");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, cartItems);
        cartRecyclerView.setAdapter(cartAdapter);

        // Свайп влево/вправо по товару => удалить из корзины
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    float height = itemView.getBottom() - itemView.getTop();
                    float width = height / 3f;

                    // background
                    RectF background;
                    if (dX > 0) {
                        background = new RectF(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                    } else {
                        background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    }
                    // fade-in effect based on swipe distance
                    int alpha = (int) (Math.min(1f, Math.abs(dX) / itemView.getWidth()) * 255);
                    swipePaint.setAlpha(alpha);
                    c.drawRoundRect(background, 16f, 16f, swipePaint);

                    // icon
                    if (swipeDeleteIcon != null) {
                        int iconSize = (int) (height / 3f);
                        int iconTop = itemView.getTop() + (int) ((height - iconSize) / 2f);
                        int iconBottom = iconTop + iconSize;
                        int iconLeft;
                        int iconRight;
                        if (dX > 0) {
                            iconLeft = itemView.getLeft() + (int) (width / 2f);
                            iconRight = iconLeft + iconSize;
                        } else {
                            iconRight = itemView.getRight() - (int) (width / 2f);
                            iconLeft = iconRight - iconSize;
                        }
                        swipeDeleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        swipeDeleteIcon.setAlpha(alpha);
                        swipeDeleteIcon.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos < 0 || pos >= cartItems.size()) return;
                int variantId = cartItems.get(pos).getGameVariantId();
                // optimistic UI: remove row
                cartItems.remove(pos);
                cartAdapter.notifyItemRemoved(pos);
                applyCartResponse(null); // just refresh totals/text from current list

                apiService.setCartItemQuantity(variantId, new CartSetQuantityRequest(0)).enqueue(new Callback<CartResponse>() {
                    @Override
                    public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            applyCartResponse(response.body());
                        } else {
                            Toast.makeText(CartActivity.this, "Ошибка удаления: " + response.code(), Toast.LENGTH_LONG).show();
                            loadCartItems();
                        }
                    }

                    @Override
                    public void onFailure(Call<CartResponse> call, Throwable t) {
                        Toast.makeText(CartActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        loadCartItems();
                    }
                });
            }
        });
        helper.attachToRecyclerView(cartRecyclerView);

        // Устанавливаем слушателя для кнопок +/- в адаптере
        cartAdapter.setListener((position, newQuantity) -> {
            if (position < 0 || position >= cartItems.size()) return;
            int variantId = cartItems.get(position).getGameVariantId();
            apiService.setCartItemQuantity(variantId, new CartSetQuantityRequest(newQuantity))
                    .enqueue(new Callback<CartResponse>() {
                        @Override
                        public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                applyCartResponse(response.body());
                            } else {
                                Toast.makeText(CartActivity.this, "Ошибка корзины: " + response.code(), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<CartResponse> call, Throwable t) {
                            Toast.makeText(CartActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void loadCartItems() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        apiService.getCart().enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    applyCartResponse(response.body());
                } else {
                    Toast.makeText(CartActivity.this, "Не удалось загрузить корзину", Toast.LENGTH_SHORT).show();
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void applyCartResponse(CartResponse cart) {
        if (cart != null) {
            cartItems.clear();
            if (cart.getItems() != null) {
                for (CartResponse.CartServerItem it : cart.getItems()) {
                    if (it == null || it.getGameVariant() == null || it.getGameVariant().getGame() == null) continue;
                    GameVariantForList v = it.getGameVariant();
                    int variantId = v.getId();
                    String title = v.getGame().getTitle();
                    String imageUrl = v.getImageLink();
                    double price = v.getActualPrice();
                    int qty = it.getQuantity();
                    cartItems.add(new CartItem(variantId, title, imageUrl, price, qty));
                }
            }
            cartAdapter.notifyDataSetChanged();
        }

        double total = 0.0;
        if (cart != null) {
            total = cart.getTotal();
        } else {
            for (CartItem item : cartItems) {
                if (item == null) continue;
                total += item.getPrice() * item.getQuantity();
            }
        }
        if (total <= 0.0) {
            payButton.setText("Оплатить");
            payButton.setEnabled(false);
            Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
        } else {
            payButton.setEnabled(true);
            payButton.setText(String.format(Locale.getDefault(), "Оплатить %.0f ₽", total));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems();
    }
}

