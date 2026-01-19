package com.example.shop_exam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Адаптер для отображения списка товаров в каталоге
 */
public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private Context context;
    private List<GameVariantForList> gameList;

    public GameAdapter(Context context, List<GameVariantForList> gameList) {
        this.context = context;
        this.gameList = gameList;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создаётся View из макета карточки товара
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item_game, parent, false);
        return new GameViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameVariantForList item = gameList.get(position);
        GameVariantForList.GameInfo game = item.getGame();

        // Проверка наличия товара на складе
        boolean inStock = false;
        if (item.getStatus() != null && item.getStatus().getName() != null) {
            inStock = item.getStatus().getName().equalsIgnoreCase("В наличии");
        }

        // Получение возрастного рейтинга
        int age = 0;
        if (game != null && game.getAgeRating() != null) {
            age = game.getAgeRating().getMinAge();
        }

        // Проверяем нужно ли скрывать контент 18+
        boolean isAdult = (age >= 18);
        boolean answered = AgeGateStore.isAnswered(context);
        boolean allowed = AgeGateStore.isAdultAllowed(context);
        boolean needBlur = isAdult && (!answered || !allowed);

        // Изобажение
        loadImage(holder, item);
        // Блюр, если нужно
        applyBlur(holder, needBlur);

        // Текстовые поля
        if (game != null) {
            holder.titleText.setText(game.getTitle());

            // Количество игроков
            if (game.getMinPlayers() > 0 && game.getMaxPlayers() > 0) {
                holder.playersText.setText(game.getMinPlayers() + "-" + game.getMaxPlayers());
            } else {
                holder.playersText.setText("");
            }

            // Возраст
            if (game.getAgeRating() != null) {
                holder.ageText.setText(game.getAgeRating().getMinAge() + "+");
            } else {
                holder.ageText.setText("");
            }
        }

        // Кнопку с ценой
        setupPriceButton(holder, item, inStock, needBlur);

        // Обработчики нажатий
        setupClickHandlers(holder, item, inStock, needBlur);
    }

    // Загрузка изображения товара
    private void loadImage(GameViewHolder holder, GameVariantForList item) {
        String imageUrl = item.getImageLink();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullUrl = ApiClient.resolveUrl(imageUrl);
            Picasso.get()
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    // Применение эффекта размытия для 18+
    private void applyBlur(GameViewHolder holder, boolean blur) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blur) {
                RenderEffect effect = RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP);
                holder.imageView.setRenderEffect(effect);
            } else {
                holder.imageView.setRenderEffect(null);
            }
        } else {
            // Для старых версий просто полупрозрачным
            holder.imageView.setAlpha(blur ? 0.55f : 1.0f);
        }

        // Значок 18+
        if (holder.ageBadge != null) {
            holder.ageBadge.setVisibility(blur ? View.VISIBLE : View.GONE);
        }
    }

    // Кнопка с ценой
    private void setupPriceButton(GameViewHolder holder, GameVariantForList item,
                                  boolean inStock, boolean needBlur) {
        if (needBlur) {
            holder.priceButton.setText("18+");
            holder.priceButton.setEnabled(true);
            holder.priceButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_primary)));
            return;
        }

        double price = item.getPrice();
        Double discount = item.getDiscountPrice();

        // Если есть скидка
        if (discount != null && discount > 0 && discount < price) {
            String oldPrice = String.format("%.0f", price);
            String newPrice = String.format("%.0f", discount);
            String html = "<small><strike>" + oldPrice + "</strike></small> <b>" + newPrice + " ₽</b>";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.priceButton.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.priceButton.setText(Html.fromHtml(html));
            }
        } else {
            holder.priceButton.setText(String.format("%.0f ₽", price));
        }

        // Состояние кнопки
        if (inStock) {
            holder.priceButton.setEnabled(true);
            holder.priceButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_secondary)));
        } else {
            holder.priceButton.setEnabled(false);
            holder.priceButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_disabled)));
        }
    }

    // Обработчиков нажатий
    private void setupClickHandlers(GameViewHolder holder, GameVariantForList item,
                                    boolean inStock, boolean needBlur) {
        if (needBlur) {
            // Показываем диалог подтверждения возраста
            View.OnClickListener ageClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAgeDialog(item);
                }
            };
            holder.itemView.setOnClickListener(ageClick);
            holder.priceButton.setOnClickListener(ageClick);
            return;
        }

        // Открытие карточки товара
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDetails(item);
            }
        });

        // Добавление в корзину
        if (inStock) {
            holder.priceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToCart(item);
                }
            });
        } else {
            holder.priceButton.setOnClickListener(null);
        }
    }

    // Добавление товара в корзину
    private void addToCart(GameVariantForList item) {
        // Добавление в локальную корзину
        LocalCartStore.addItem(context, item.getId(), 1);
        
        // Если авторизован - синхронизируем с сервером
        String token = TokenStore.getToken(context);
        if (token != null && !token.isEmpty()) {
            ApiService apiService = ApiClient.getClient(context).create(ApiService.class);
            CartAddRequest request = new CartAddRequest(item.getId(), 1);
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

    // Открытие экрана с деталями товара
    private void openDetails(GameVariantForList item) {
        Intent intent = new Intent(context, GameDetailActivity.class);
        intent.putExtra(GameDetailActivity.EXTRA_GAME_ID, item.getId());
        context.startActivity(intent);
    }

    // Диалог подтверждения возраста
    private void showAgeDialog(GameVariantForList item) {
        new AlertDialog.Builder(context)
                .setTitle("Контент 18+")
                .setMessage("Вам уже есть 18 лет?")
                .setPositiveButton("Да", (dialog, which) -> {
                    AgeGateStore.setAdultAllowed(context, true);
                    notifyDataSetChanged();
                    openDetails(item);
                })
                .setNegativeButton("Нет", (dialog, which) -> {
                    AgeGateStore.setAdultAllowed(context, false);
                    notifyDataSetChanged();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    // Хранения ссылок на элементы карточки
    public static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleText;
        TextView playersText;
        TextView ageText;
        Button priceButton;
        TextView ageBadge;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.game_image);
            titleText = itemView.findViewById(R.id.game_title);
            playersText = itemView.findViewById(R.id.game_players);
            ageText = itemView.findViewById(R.id.game_age);
            priceButton = itemView.findViewById(R.id.game_price_button);
            ageBadge = itemView.findViewById(R.id.age_blur_badge);
        }
    }
}
