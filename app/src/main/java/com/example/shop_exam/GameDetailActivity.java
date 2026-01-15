package com.example.shop_exam;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.os.Handler;
import android.os.Looper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "extra_game_id";
    private static final String TAG = "GameDetailActivity";

    private ViewPager2 imageSlider;
    private TextView titleTextView, descriptionTextView;
    private Button addToCartButton;
    private Toolbar toolbar;
    private TabLayout sliderIndicator;
    private SwipeRefreshLayout swipeRefresh;
    private ApiService apiService;
    private GameDetail currentGameVariant;
    private int currentVariantId = -1;

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean sliderAutoEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        initViews();
        initToolbar();
        apiService = ApiClient.getClient(this).create(ApiService.class);

        currentVariantId = getIntent().getIntExtra(EXTRA_GAME_ID, -1);
        if (currentVariantId != -1) {
            loadVariantDetails(currentVariantId);
        } else {
            Toast.makeText(this, "Ошибка: ID варианта не найден", Toast.LENGTH_LONG).show();
            finish();
        }

        // обработчик задаём в displayVariantDetails (чтобы учитывать статус)
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        imageSlider = findViewById(R.id.image_slider);
        sliderIndicator = findViewById(R.id.slider_indicator);
        swipeRefresh = findViewById(R.id.detail_swipe_refresh);
        titleTextView = findViewById(R.id.game_title_detail);
        descriptionTextView = findViewById(R.id.game_description);
        addToCartButton = findViewById(R.id.add_to_cart_button);

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                if (currentVariantId != -1) loadVariantDetails(currentVariantId);
            });
        }
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadVariantDetails(int variantId) {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        apiService.getGameDetails(variantId).enqueue(new Callback<GameDetail>() {
            @Override
            public void onResponse(Call<GameDetail> call, Response<GameDetail> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentGameVariant = response.body();
                    displayVariantDetails(currentGameVariant);
                } else {
                    Toast.makeText(GameDetailActivity.this, "Не удалось загрузить детали", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Ошибка загрузки: " + response.code() + " " + response.message());
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<GameDetail> call, Throwable t) {
                Toast.makeText(GameDetailActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Сетевая ошибка", t);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private void displayVariantDetails(GameDetail variant) {
        // Возрастной фильтр: предупреждение/блокировка для 18+.
        int minAge = 0;
        try {
            if (variant != null && variant.getGame() != null && variant.getGame().getAgeRating() != null) {
                minAge = variant.getGame().getAgeRating().getMinAge();
            }
        } catch (Throwable ignored) {}
        boolean isAdultContent = minAge >= 18;
        if (isAdultContent && (!AgeGateStore.isAnswered(this) || !AgeGateStore.isAdultAllowed(this))) {
            new AlertDialog.Builder(this)
                    .setTitle("Контент 18+")
                    .setMessage("Вам уже есть 18 лет?")
                    .setPositiveButton("Да", (d, w) -> {
                        AgeGateStore.setAdultAllowed(this, true);
                        displayVariantDetails(variant);
                    })
                    .setNegativeButton("Нет", (d, w) -> {
                        AgeGateStore.setAdultAllowed(this, false);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
            return;
        }

        if (getSupportActionBar() != null && variant.getGame() != null) {
            getSupportActionBar().setTitle(variant.getGame().getTitle());
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        titleTextView.setText(variant.getGame() != null ? variant.getGame().getTitle() : "Название не загружено");
        descriptionTextView.setText(formatDescription(variant.getDescription()));

        boolean isInStock = variant.getStatus() != null
                && variant.getStatus().getName() != null
                && variant.getStatus().getName().equalsIgnoreCase("В наличии");

        if (isInStock) {
            addToCartButton.setEnabled(true);
            addToCartButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_secondary))
            );
            
            double price = variant.getPrice();
            Double discountPrice = variant.getDiscountPrice();
            String currency = "₽"; // Валюта теперь жестко задана

            if (discountPrice != null && discountPrice > 0 && discountPrice < price) {
                String oldPriceFormatted = String.format("%.0f", price);
                String newPriceFormatted = String.format("%.0f", discountPrice);
                String priceHtml = "В корзину за <small><strike>" + oldPriceFormatted + "</strike></small> <b>" + newPriceFormatted + " " + currency + "</b>";
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addToCartButton.setText(Html.fromHtml(priceHtml, Html.FROM_HTML_MODE_COMPACT));
                } else {
                    addToCartButton.setText(Html.fromHtml(priceHtml));
                }
            } else {
                String priceText = String.format("В корзину за %.0f %s", price, currency);
                addToCartButton.setText(priceText);
            }

        } else {
            addToCartButton.setText("Нет в наличии");
            addToCartButton.setEnabled(false);
            addToCartButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_disabled))
            );
        }

        addToCartButton.setOnClickListener(v -> {
            if (!isInStock) return;
            try {
                apiService.addToCart(new CartAddRequest(variant.getId(), 1)).enqueue(new Callback<CartResponse>() {
                    @Override
                    public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(GameDetailActivity.this, "Добавлено в корзину", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GameDetailActivity.this, "Ошибка корзины: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CartResponse> call, Throwable t) {
                        Toast.makeText(GameDetailActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Add to cart failed", e);
                Toast.makeText(GameDetailActivity.this, "Ошибка корзины: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
        });
        
        List<String> imageUrls = new ArrayList<>();
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            List<GameDetail.ImageInfo> imgs = new ArrayList<>(variant.getImages());
            // cover -> first, then by sort_order if we have it (fallback: keep order)
            Collections.sort(imgs, new Comparator<GameDetail.ImageInfo>() {
                @Override public int compare(GameDetail.ImageInfo a, GameDetail.ImageInfo b) {
                    if (a == null && b == null) return 0;
                    if (a == null) return 1;
                    if (b == null) return -1;
                    if (a.isCover() != b.isCover()) return a.isCover() ? -1 : 1;
                    // if model doesn't have sort_order, it will return 0
                    return Integer.compare(a.getSortOrder(), b.getSortOrder());
                }
            });
            for (GameDetail.ImageInfo image : imgs) {
                imageUrls.add(image != null ? image.getUrl() : "");
            }
        } else {
            imageUrls.add("");
        }
        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, imageUrls);
        imageSlider.setAdapter(sliderAdapter);

        // dots indicator
        try {
            new TabLayoutMediator(sliderIndicator, imageSlider, (tab, position) -> {}).attach();
        } catch (Exception e) {
            Log.w(TAG, "TabLayoutMediator attach failed", e);
        }

        setupAutoSlide(imageUrls.size());
    }

    private void setupAutoSlide(int count) {
        sliderAutoEnabled = count > 1;
        if (!sliderAutoEnabled) return;

        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }

        sliderRunnable = new Runnable() {
            @Override public void run() {
                if (!sliderAutoEnabled) return;
                int next = imageSlider.getCurrentItem() + 1;
                if (next >= count) next = 0;
                imageSlider.setCurrentItem(next, true);
                sliderHandler.postDelayed(this, 3500);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 3500);

        imageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                // reset timer on manual swipe
                if (sliderRunnable != null) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3500);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sliderAutoEnabled && sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
            sliderHandler.postDelayed(sliderRunnable, 3500);
        }
    }

    private CharSequence formatDescription(String raw) {
        if (TextUtils.isEmpty(raw)) return "Описание отсутствует.";

        String text = raw.replace("\r\n", "\n").replace("\r", "\n").trim();
        String[] lines = text.split("\n");

        Set<String> headings = new HashSet<>();
        headings.add("комплектация");
        headings.add("правила");
        headings.add("описание");
        headings.add("характеристики");

        SpannableStringBuilder out = new SpannableStringBuilder();
        boolean lastWasEmpty = false;

        for (String line : lines) {
            String s = line == null ? "" : line.trim();
            if (s.isEmpty()) {
                if (!lastWasEmpty) {
                    out.append("\n\n");
                }
                lastWasEmpty = true;
                continue;
            }
            lastWasEmpty = false;

            boolean isHeading = s.endsWith(":") || headings.contains(s.toLowerCase());
            boolean isBullet = s.startsWith("- ") || s.startsWith("• ") || s.startsWith("— ");

            int start = out.length();
            if (isBullet) {
                String item = s.substring(2).trim();
                out.append("• ").append(item);
            } else {
                out.append(s);
            }
            int end = out.length();

            if (isHeading) {
                out.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.append("\n");
            } else {
                out.append("\n");
            }
        }

        return out.toString().trim();
    }
}
