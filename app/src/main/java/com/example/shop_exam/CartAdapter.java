package com.example.shop_exam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

/**
 * Адаптер для отображения позиций в корзине
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> items;
    private OnQuantityChangeListener listener;

    // Интерфейс для уведомления об изменении количества
    public interface OnQuantityChangeListener {
        void onQuantityChange(int position, int newQuantity);
    }

    public CartAdapter(Context context, List<CartItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setListener(OnQuantityChangeListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);

        // Название товара
        holder.titleText.setText(item.getTitle());

        // Количество
        holder.quantityText.setText(String.valueOf(item.getQuantity()));

        // Стоимость позиции
        double lineTotal = item.getPrice() * item.getQuantity();
        holder.priceText.setText(String.format(Locale.getDefault(), "%.0f ₽", lineTotal));

        // Загрузка изображения
        String imageUrl = item.getImageUrl();
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

        // Кнопка "+"
        holder.plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    listener.onQuantityChange(pos, item.getQuantity() + 1);
                }
            }
        });

        // Кнопка "-"
        holder.minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    listener.onQuantityChange(pos, item.getQuantity() - 1);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder для позиции корзины
    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleText;
        TextView priceText;
        TextView quantityText;
        ImageView plusButton;
        ImageView minusButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_image);
            titleText = itemView.findViewById(R.id.item_title);
            priceText = itemView.findViewById(R.id.item_price);
            quantityText = itemView.findViewById(R.id.item_quantity);
            plusButton = itemView.findViewById(R.id.quantity_plus);
            minusButton = itemView.findViewById(R.id.quantity_minus);
        }
    }
}
