package com.example.shop_exam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private static final String TAG = "GameAdapter";

    private final Context context;
    private final List<GameVariantForList> variantList;
    private final OnCartChangedListener onCartChangedListener;

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    public GameAdapter(Context context, List<GameVariantForList> variantList) {
        this.context = context;
        this.variantList = variantList;
        this.onCartChangedListener = null;
    }

    public GameAdapter(Context context, List<GameVariantForList> variantList, OnCartChangedListener listener) {
        this.context = context;
        this.variantList = variantList;
        this.onCartChangedListener = listener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item_game, parent, false);
        return new GameViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameVariantForList variant = variantList.get(position);
        GameVariantForList.GameInfo game = variant.getGame();
        boolean isInStock = variant.getStatus() != null
                && variant.getStatus().getName() != null
                && variant.getStatus().getName().equalsIgnoreCase("В наличии");

        int minAge = 0;
        try {
            if (game != null && game.getAgeRating() != null) {
                minAge = game.getAgeRating().getMinAge();
            }
        } catch (Throwable ignored) {}
        boolean isAdult = minAge >= 18;
        boolean answered = AgeGateStore.isAnswered(context);
        boolean adultAllowed = AgeGateStore.isAdultAllowed(context);
        // Если пользователь ещё не отвечал ИЛИ ответил "нет" — цензурим 18+ в списке
        boolean shouldCensor = isAdult && (!answered || !adultAllowed);

        if (variant.getImageLink() != null && !variant.getImageLink().isEmpty()) {
            String imageUrl = ApiClient.resolveUrl(variant.getImageLink());
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(holder.imageView, new Callback() {
                @Override public void onSuccess() {}
                @Override public void onError(Exception e) {
                    Log.e(TAG, "Failed to load image: " + imageUrl, e);
                }
            });
        } else {
            holder.imageView.setImageResource(R.drawable.ic_image_placeholder);
        }

        // Blur 18+ карточек до подтверждения возраста
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                holder.imageView.setRenderEffect(shouldCensor
                        ? RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
                        : null);
            } catch (Throwable ignored) {}
        } else {
            // fallback для старых Android: лёгкое затемнение (реальный blur недоступен без доп. либ)
            holder.imageView.setAlpha(shouldCensor ? 0.55f : 1.0f);
        }
        if (holder.ageBadge != null) {
            holder.ageBadge.setVisibility(shouldCensor ? View.VISIBLE : View.GONE);
        }

        if (game != null) {
            holder.titleTextView.setText(game.getTitle());

            if (game.getMinPlayers() > 0 && game.getMaxPlayers() > 0) {
                holder.playersTextView.setText(game.getMinPlayers() + "-" + game.getMaxPlayers());
            } else {
                holder.playersTextView.setText("");
            }

            if (game.getAgeRating() != null) {
                holder.ageTextView.setText(game.getAgeRating().getMinAge() + "+");
            } else {
                holder.ageTextView.setText("");
            }
        }

        double price = variant.getPrice();
        Double discountPrice = variant.getDiscountPrice();
        String currency = "₽"; // Валюта теперь жестко задана

        if (discountPrice != null && discountPrice > 0 && discountPrice < price) {
            String oldPriceFormatted = String.format("%.0f", price);
            String newPriceFormatted = String.format("%.0f", discountPrice);
            String priceHtml = "<small><strike>" + oldPriceFormatted + "</strike></small> <b>" + newPriceFormatted + " " + currency + "</b>";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.priceButton.setText(Html.fromHtml(priceHtml, Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.priceButton.setText(Html.fromHtml(priceHtml));
            }

        } else {
            String priceText = String.format("%.0f %s", price, currency);
            holder.priceButton.setText(priceText);
        }

        holder.itemView.setOnClickListener(v -> openDetailActivity(variant));

        // Если 18+ и доступ не подтверждён — вместо открытия/добавления показываем предупреждение
        if (shouldCensor) {
            View.OnClickListener gateClick = vv -> showAgeGateDialogAndMaybeOpen(variant);
            holder.itemView.setOnClickListener(gateClick);
            holder.priceButton.setOnClickListener(gateClick);
            holder.priceButton.setEnabled(true);
            holder.priceButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_primary))
            );
            holder.priceButton.setText("18+");
            return;
        }

        if (isInStock) {
            holder.priceButton.setEnabled(true);
            holder.priceButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_secondary))
            );
            holder.priceButton.setOnClickListener(v -> {
                ApiService api = ApiClient.getClient(context).create(ApiService.class);
                api.addToCart(new CartAddRequest(variant.getId(), 1)).enqueue(new retrofit2.Callback<CartResponse>() {
                    @Override
                    public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Добавлено в корзину", Toast.LENGTH_SHORT).show();
                            if (onCartChangedListener != null) onCartChangedListener.onCartChanged();
                        } else {
                            Toast.makeText(context, "Ошибка корзины: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CartResponse> call, Throwable t) {
                        Toast.makeText(context, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else {
            holder.priceButton.setEnabled(false);
            holder.priceButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_disabled))
            );
            holder.priceButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return variantList.size();
    }

    public static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView playersTextView;
        TextView ageTextView;
        Button priceButton;
        TextView ageBadge;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.game_image);
            ageBadge = itemView.findViewById(R.id.age_blur_badge);
            titleTextView = itemView.findViewById(R.id.game_title);
            playersTextView = itemView.findViewById(R.id.game_players);
            ageTextView = itemView.findViewById(R.id.game_age);
            priceButton = itemView.findViewById(R.id.game_price_button);
        }
    }

    private void openDetailActivity(GameVariantForList variant) {
        if (variant == null) return;
        Intent intent = new Intent(context, GameDetailActivity.class);
        intent.putExtra(GameDetailActivity.EXTRA_GAME_ID, variant.getId());
        context.startActivity(intent);
    }

    private void showAgeGateDialogAndMaybeOpen(GameVariantForList variant) {
        new AlertDialog.Builder(context)
                .setTitle("Контент 18+")
                .setMessage("Вам уже есть 18 лет?")
                .setPositiveButton("Да", (d, w) -> {
                    AgeGateStore.setAdultAllowed(context, true);
                    notifyDataSetChanged();
                    openDetailActivity(variant);
                })
                .setNegativeButton("Нет", (d, w) -> {
                    AgeGateStore.setAdultAllowed(context, false);
                    notifyDataSetChanged();
                })
                .setCancelable(false)
                .show();
    }
}
