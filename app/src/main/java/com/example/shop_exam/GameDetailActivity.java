package com.example.shop_exam;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран с детальной информацией о товаре
 */
public class GameDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "extra_game_id";

    // Вкладки
    private static final int TAB_DESCRIPTION  = 0;
    private static final int TAB_RULES        = 1;
    private static final int TAB_COMPONENTS   = 2;
    private static final int TAB_SPECS        = 3;

    private ViewPager2 imageSlider;
    private TabLayout sliderIndicator;
    private TextView titleText;
    private TextView tabContentHtml;
    private LinearLayout specsTable;
    private MaterialButton tabDescription, tabRules, tabComponents, tabSpecs;
    private Button cartButton;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private ApiService apiService;
    private int productId = -1;
    private GameDetail currentProduct;
    private int activeTab = TAB_DESCRIPTION;

    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean autoSlideEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        findViews();
        setupToolbar();
        setupTabButtons();

        apiService = ApiClient.getClient(this).create(ApiService.class);
        productId = getIntent().getIntExtra(EXTRA_GAME_ID, -1);
        if (productId == -1) { finish(); return; }

        loadProductDetails();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadProductDetails);
        }
    }

    private void findViews() {
        toolbar        = findViewById(R.id.toolbar);
        imageSlider    = findViewById(R.id.image_slider);
        sliderIndicator = findViewById(R.id.slider_indicator);
        swipeRefresh   = findViewById(R.id.detail_swipe_refresh);
        titleText      = findViewById(R.id.game_title_detail);
        tabContentHtml = findViewById(R.id.tab_content_html);
        specsTable     = findViewById(R.id.tab_specs_table);
        cartButton     = findViewById(R.id.add_to_cart_button);
        tabDescription = findViewById(R.id.tab_description);
        tabRules       = findViewById(R.id.tab_rules);
        tabComponents  = findViewById(R.id.tab_components);
        tabSpecs       = findViewById(R.id.tab_specs);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupTabButtons() {
        tabDescription.setOnClickListener(v -> selectTab(TAB_DESCRIPTION));
        tabRules.setOnClickListener(v -> selectTab(TAB_RULES));
        tabComponents.setOnClickListener(v -> selectTab(TAB_COMPONENTS));
        tabSpecs.setOnClickListener(v -> selectTab(TAB_SPECS));

        // Начальное состояние
        updateTabVisuals(TAB_DESCRIPTION);
    }

    private void selectTab(int tab) {
        activeTab = tab;
        updateTabVisuals(tab);
        if (currentProduct != null) showTabContent(currentProduct, tab);
    }

    private void updateTabVisuals(int tab) {
        int colorAccent    = ContextCompat.getColor(this, R.color.color_secondary);
        int colorDefault   = ContextCompat.getColor(this, R.color.color_text);

        MaterialButton[] all = {tabDescription, tabRules, tabComponents, tabSpecs};
        int[] ids = {TAB_DESCRIPTION, TAB_RULES, TAB_COMPONENTS, TAB_SPECS};
        for (int i = 0; i < all.length; i++) {
            boolean active = (ids[i] == tab);
            all[i].setStrokeColor(ColorStateList.valueOf(active ? colorAccent : Color.LTGRAY));
            all[i].setTextColor(active ? colorAccent : colorDefault);
        }
    }

    private void showTabContent(GameDetail product, int tab) {
        if (tab == TAB_SPECS) {
            tabContentHtml.setVisibility(View.GONE);
            specsTable.setVisibility(View.VISIBLE);
            buildSpecsTable(product);
        } else {
            specsTable.setVisibility(View.GONE);
            tabContentHtml.setVisibility(View.VISIBLE);
            String html = "";
            switch (tab) {
                case TAB_DESCRIPTION: html = product.getDescriptionHtml(); break;
                case TAB_RULES:       html = product.getRulesHtml();        break;
                case TAB_COMPONENTS:  html = product.getComponentsHtml();   break;
            }
            if (html == null || html.isEmpty()) html = "<p>Информация отсутствует.</p>";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tabContentHtml.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
            } else {
                tabContentHtml.setText(Html.fromHtml(html));
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void buildSpecsTable(GameDetail product) {
        specsTable.removeAllViews();

        // Outer border (1dp)
        specsTable.setBackgroundColor(0xFFE0E0E0);
        specsTable.setPadding(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));

        GameDetail.GameInfo game = product.getGame();
        int ageMin = (game != null && game.getAgeRating() != null) ? game.getAgeRating().getMinAge() : 0;

        // ── Коробка ─────────────────────────────────────────────────────────
        addSectionHeader("Коробка", true);
        addRow("Ширина, мм",   product.getBoxWidthMm()  != null ? String.valueOf(product.getBoxWidthMm())  : "—", true);
        addRow("Высота, мм",   product.getBoxHeightMm() != null ? String.valueOf(product.getBoxHeightMm()) : "—", true);
        addRow("Толщина, мм",  product.getBoxDepthMm()  != null ? String.valueOf(product.getBoxDepthMm())  : "—", true);
        addRow("Вес, г",       product.getWeightGrams() != null ? String.valueOf(product.getWeightGrams())  : "—", false);

        // ── Игроки ───────────────────────────────────────────────────────────
        addSectionHeader("Игроки", false);
        addRow("Минимум, чел", game != null && game.getMinPlayers() != null ? String.valueOf(game.getMinPlayers()) : "—", true);
        addRow("Максимум, чел",game != null && game.getMaxPlayers() != null ? String.valueOf(game.getMaxPlayers()) : "—", true);
        addRow("Возраст, лет", ageMin > 0 ? ageMin + "+" : "—", false);

        // ── Игра ─────────────────────────────────────────────────────────────
        addSectionHeader("Игра", false);
        addRow("Сложность",           product.getComplexity() != null ? product.getComplexity() : "—", true);
        String playtime = (game != null && game.getPlaytimeAvg() != null)
                ? String.valueOf(game.getPlaytimeAvg())
                : "—";
        addRow("Средняя длительность, мин", playtime, true);
        List<String> genres = product.getGenres();
        String genresStr = (genres != null && !genres.isEmpty())
                ? String.join(", ", genres)
                : "—";
        addRow("Жанры", genresStr, false);
    }

    private void addSectionHeader(String title, boolean topSection) {
        int dp10 = dpToPx(10);
        int dp12 = dpToPx(12);
        int borderColor = 0xFFE0E0E0;

        if (!topSection) {
            // Divider before non-first sections
            View spacer = new View(this);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            spacer.setBackgroundColor(borderColor);
            specsTable.addView(spacer);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xFFF0F0F0);
        row.setPadding(dp12, dp10, dp12, dp10);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(13f);
        tv.setTextColor(ContextCompat.getColor(this, R.color.color_primary));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        row.addView(tv);
        specsTable.addView(row);
    }

    private void addRow(String label, String value, boolean withDivider) {
        int dp10 = dpToPx(10);
        int dp12 = dpToPx(12);
        int borderColor = 0xFFE0E0E0;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(dp12, dp10, dp12, dp10);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(0xFF757575);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(lpLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(13f);
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setTextColor(ContextCompat.getColor(this, R.color.color_text));
        tvValue.setGravity(Gravity.END);
        LinearLayout.LayoutParams lpValue = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvValue.setLayoutParams(lpValue);

        row.addView(tvLabel);
        row.addView(tvValue);
        specsTable.addView(row);

        if (withDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
            lp.setMarginStart(dp12);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(borderColor);
            specsTable.addView(divider);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void loadProductDetails() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        apiService.getGameDetails(productId).enqueue(new Callback<GameDetail>() {
            @Override
            public void onResponse(Call<GameDetail> call, Response<GameDetail> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    showProductDetails(response.body());
                }
            }
            @Override
            public void onFailure(Call<GameDetail> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private void showProductDetails(GameDetail product) {
        int age = 0;
        if (product.getGame() != null && product.getGame().getAgeRating() != null) {
            age = product.getGame().getAgeRating().getMinAge();
        }
        if (age >= 18 && (!AgeGateStore.isAnswered(this) || !AgeGateStore.isAdultAllowed(this))) {
            showAgeDialog(product);
            return;
        }

        currentProduct = product;

        if (getSupportActionBar() != null && product.getGame() != null) {
            getSupportActionBar().setTitle(product.getGame().getTitle());
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        if (product.getGame() != null) {
            titleText.setText(product.getGame().getTitle());
        }

        // Показываем начальную вкладку «Описание»
        selectTab(TAB_DESCRIPTION);

        boolean inStock = product.getStatus() != null
                && "В наличии".equalsIgnoreCase(product.getStatus().getName());
        setupCartButton(product, inStock);
        setupImageSlider(product);
    }

    private void showAgeDialog(GameDetail product) {
        new AlertDialog.Builder(this)
                .setTitle("Контент 18+")
                .setMessage("Вам уже есть 18 лет?")
                .setPositiveButton("Да", (d, w) -> {
                    AgeGateStore.setAdultAllowed(this, true);
                    showProductDetails(product);
                })
                .setNegativeButton("Нет", (d, w) -> {
                    AgeGateStore.setAdultAllowed(this, false);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @SuppressLint("DefaultLocale")
    private void setupCartButton(GameDetail product, boolean inStock) {
        if (inStock) {
            cartButton.setEnabled(true);
            cartButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_secondary)));
            double price = product.getPrice();
            Double discount = product.getDiscountPrice();
            if (discount != null && discount > 0 && discount < price) {
                String html = "В корзину за <small><strike>" + String.format("%.0f", price) +
                        "</strike></small> <b>" + String.format("%.0f", discount) + " ₽</b>";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cartButton.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
                } else {
                    cartButton.setText(Html.fromHtml(html));
                }
            } else {
                cartButton.setText(String.format("В корзину за %.0f ₽", price));
            }
            cartButton.setOnClickListener(v -> addToCart(product));
        } else {
            cartButton.setText("Нет в наличии");
            cartButton.setEnabled(false);
            cartButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_disabled)));
        }
    }

    private void addToCart(GameDetail product) {
        String token = TokenStore.getToken(this);
        boolean loggedIn = token != null && !token.isEmpty();

        if (loggedIn) {
            // Авторизован — только сервер
            apiService.addToCart(new CartAddRequest(product.getId(), 1))
                    .enqueue(new Callback<CartResponse>() {
                        @Override public void onResponse(Call<CartResponse> c, Response<CartResponse> r) {}
                        @Override public void onFailure(Call<CartResponse> c, Throwable t) {}
                    });
        } else {
            // Не авторизован — только локально
            LocalCartStore.addItem(this, product.getId(), 1);
        }
    }

    private void setupImageSlider(GameDetail product) {
        List<String> images = new ArrayList<>();
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            List<GameDetail.ImageInfo> imgList = new ArrayList<>(product.getImages());
            Collections.sort(imgList, (a, b) -> {
                if (a.isCover() != b.isCover()) return a.isCover() ? -1 : 1;
                return Integer.compare(a.getSortOrder(), b.getSortOrder());
            });
            for (GameDetail.ImageInfo img : imgList) images.add(img.getUrl());
        } else {
            images.add("");
        }
        imageSlider.setAdapter(new ImageSliderAdapter(this, images));
        try {
            new TabLayoutMediator(sliderIndicator, imageSlider, (tab, pos) -> {}).attach();
        } catch (Exception ignored) {}
        setupAutoSlide(images.size());
    }

    private void setupAutoSlide(int count) {
        autoSlideEnabled = (count > 1);
        if (!autoSlideEnabled) return;
        if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable);
        sliderRunnable = new Runnable() {
            @Override public void run() {
                if (!autoSlideEnabled) return;
                int next = (imageSlider.getCurrentItem() + 1) % count;
                imageSlider.setCurrentItem(next, true);
                sliderHandler.postDelayed(this, 3500);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 3500);
        imageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                if (sliderRunnable != null) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3500);
                }
            }
        });
    }

    @Override protected void onPause() { super.onPause(); if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable); }
    @Override protected void onResume() {
        super.onResume();
        if (autoSlideEnabled && sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
            sliderHandler.postDelayed(sliderRunnable, 3500);
        }
    }
}
