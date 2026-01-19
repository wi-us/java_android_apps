package com.example.shop_exam;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран с детальной информацией о товаре
 */
public class GameDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "extra_game_id";

    private ViewPager2 imageSlider;
    private TabLayout sliderIndicator;
    private TextView titleText;
    private TextView descriptionText;
    private Button cartButton;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private ApiService apiService;
    private int productId = -1;

    // Для автоматического пролистывания слайдера
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean autoSlideEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        findViews();
        setupToolbar();
        apiService = ApiClient.getClient(this).create(ApiService.class);
        productId = getIntent().getIntExtra(EXTRA_GAME_ID, -1);
        if (productId == -1) {
            finish();
            return;
        }

        loadProductDetails();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    loadProductDetails();
                }
            });
        }
    }
    private void findViews() {
        toolbar = findViewById(R.id.toolbar);
        imageSlider = findViewById(R.id.image_slider);
        sliderIndicator = findViewById(R.id.slider_indicator);
        swipeRefresh = findViewById(R.id.detail_swipe_refresh);
        titleText = findViewById(R.id.game_title_detail);
        descriptionText = findViewById(R.id.game_description);
        cartButton = findViewById(R.id.add_to_cart_button);
    }

    // Настройка toolbar
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                onBackPressed();
            }
        });
    }

    // Загрузка данных товара с сервера
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

    // Отображение данных товара
    @SuppressLint("DefaultLocale")
    private void showProductDetails(GameDetail product) {
        // Проверяем возрастное ограничение
        int age = 0;
        if (product.getGame() != null && product.getGame().getAgeRating() != null) {
            age = product.getGame().getAgeRating().getMinAge();
        }

        boolean isAdult = (age >= 18);
        boolean answered = AgeGateStore.isAnswered(this);
        boolean allowed = AgeGateStore.isAdultAllowed(this);

        // Если контент 18+ и пользователь не подтвердил возраст
        if (isAdult && (!answered || !allowed)) {
            showAgeDialog(product);
            return;
        }

        // Заголовок в toolbar
        if (getSupportActionBar() != null && product.getGame() != null) {
            getSupportActionBar().setTitle(product.getGame().getTitle());
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // Название товара
        if (product.getGame() != null) {
            titleText.setText(product.getGame().getTitle());
        } else {
            titleText.setText("Название не загружено");
        }

        // Описание товара
        descriptionText.setText(formatDescription(product.getDescription()));

        // Проверерка наличия
        boolean inStock = false;
        if (product.getStatus() != null && product.getStatus().getName() != null) {
            inStock = product.getStatus().getName().equalsIgnoreCase("В наличии");
        }

        // Кнопка корзины
        setupCartButton(product, inStock);
        // Слайдер изображений
        setupImageSlider(product);
    }

    // Диалог подтверждения возраста
    private void showAgeDialog(GameDetail product) {
        new AlertDialog.Builder(this)
                .setTitle("Контент 18+")
                .setMessage("Вам уже есть 18 лет?")
                .setPositiveButton("Да", (dialog, which) -> {
                    AgeGateStore.setAdultAllowed(this, true);
                    showProductDetails(product);
                })
                .setNegativeButton("Нет", (dialog, which) -> {
                    AgeGateStore.setAdultAllowed(this, false);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    // Настройка кнопки добавления в корзину
    @SuppressLint("DefaultLocale")
    private void setupCartButton(GameDetail product, boolean inStock) {
        if (inStock) {
            cartButton.setEnabled(true);
            cartButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_secondary)));

            double price = product.getPrice();
            Double discount = product.getDiscountPrice();

            // Если есть скидка
            if (discount != null && discount > 0 && discount < price) {
                String oldPrice = String.format("%.0f", price);
                String newPrice = String.format("%.0f", discount);
                String html = "В корзину за <small><strike>" + oldPrice +
                        "</strike></small> <b>" + newPrice + " ₽</b>";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cartButton.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
                } else {
                    cartButton.setText(Html.fromHtml(html));
                }
            } else {
                cartButton.setText(String.format("В корзину за %.0f ₽", price));
            }

            // Обработчик нажатия
            cartButton.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    addToCart(product);
                }
            });
        } else {
            // Товара нет в наличии
            cartButton.setText("Нет в наличии");
            cartButton.setEnabled(false);
            cartButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_disabled)));
        }
    }

    // Добавление товара в корзину
    private void addToCart(GameDetail product) {
        LocalCartStore.addItem(this, product.getId(), 1);
        
        // Если авторизован - синхронизация с сервером
        String token = TokenStore.getToken(this);
        if (token != null && !token.isEmpty()) {
            CartAddRequest request = new CartAddRequest(product.getId(), 1);
            apiService.addToCart(request).enqueue(new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                    // Синхронизация завершена
                }

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {
                    // Ошибка синхронизации - данные останутся локально
                }
            });
        }
    }

    // Настройка слайдера изображений
    private void setupImageSlider(GameDetail product) {
        List<String> images = new ArrayList<>();

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            // Сортировка изображения - обложка первая
            List<GameDetail.ImageInfo> imgList = new ArrayList<>(product.getImages());
            Collections.sort(imgList, new Comparator<GameDetail.ImageInfo>() {
                @Override
                public int compare(GameDetail.ImageInfo a, GameDetail.ImageInfo b) {
                    if (a.isCover() != b.isCover()) {
                        return a.isCover() ? -1 : 1;
                    }
                    return Integer.compare(a.getSortOrder(), b.getSortOrder());
                }
            });

            for (GameDetail.ImageInfo img : imgList) {
                images.add(img.getUrl());
            }
        } else {
            images.add(""); // Пустое изображение как заглушка
        }

        // Адаптер для слайдера
        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, images);
        imageSlider.setAdapter(sliderAdapter);

        // Индикатор точками
        try {
            new TabLayoutMediator(sliderIndicator, imageSlider,
                    (tab, position) -> {}).attach();
        } catch (Exception e) {
        }

        // Автопролистывание
        setupAutoSlide(images.size());
    }

    // Настройка автоматического пролистывания
    private void setupAutoSlide(int count) {
        autoSlideEnabled = (count > 1);
        if (!autoSlideEnabled) return;

        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (!autoSlideEnabled) return;
                int next = imageSlider.getCurrentItem() + 1;
                if (next >= count) next = 0;
                imageSlider.setCurrentItem(next, true);
                sliderHandler.postDelayed(this, 3500);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 3500);

        // Сбрасываем таймер при ручном пролистывании
        imageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (sliderRunnable != null) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3500);
                }
            }
        });
    }

    // Форматирование описания товара
    private CharSequence formatDescription(String text) {
        if (TextUtils.isEmpty(text)) {
            return "Описание отсутствует.";
        }

        text = text.replace("\r\n", "\n").replace("\r", "\n").trim();
        String[] lines = text.split("\n");

        // Слова которые считаются заголовками
        Set<String> headings = new HashSet<>();
        headings.add("комплектация");
        headings.add("правила");
        headings.add("описание");
        headings.add("характеристики");

        SpannableStringBuilder result = new SpannableStringBuilder();
        boolean lastEmpty = false;

        for (String line : lines) {
            String s = line.trim();
            if (s.isEmpty()) {
                if (!lastEmpty) {
                    result.append("\n\n");
                }
                lastEmpty = true;
                continue;
            }
            lastEmpty = false;

            boolean isHeading = s.endsWith(":") || headings.contains(s.toLowerCase());
            boolean isBullet = s.startsWith("- ") || s.startsWith("• ") || s.startsWith("— ");

            int start = result.length();
            if (isBullet) {
                result.append("• ").append(s.substring(2).trim());
            } else {
                result.append(s);
            }
            int end = result.length();

            if (isHeading) {
                result.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            result.append("\n");
        }

        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (autoSlideEnabled && sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
            sliderHandler.postDelayed(sliderRunnable, 3500);
        }
    }
}







