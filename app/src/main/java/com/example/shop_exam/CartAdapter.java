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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final Context context;
    private final List<CartItem> cartItemList;
    private OnQuantityChangeListener listener;

    public interface OnQuantityChangeListener {
        void onQuantityChange(int position, int newQuantity);
    }

    public CartAdapter(Context context, List<CartItem> cartItemList) {
        this.context = context;
        this.cartItemList = cartItemList;
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
        CartItem cartItem = cartItemList.get(position);
        holder.titleTextView.setText(cartItem.getTitle());
        holder.quantityTextView.setText(String.valueOf(cartItem.getQuantity()));
        // Стоимость позиции = цена * количество
        double lineTotal = cartItem.getPrice() * cartItem.getQuantity();
        holder.priceTextView.setText(String.format(Locale.getDefault(), "%.0f ₽", lineTotal));

        // Загружаем изображение напрямую из варианта
        if (cartItem.getImageUrl() != null && !cartItem.getImageUrl().isEmpty()) {
            String imageUrl = ApiClient.resolveUrl(cartItem.getImageUrl());
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.addButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityChange(position, cartItem.getQuantity() + 1);
            }
        });

        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityChange(position, cartItem.getQuantity() - 1);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView priceTextView;
        TextView quantityTextView;
        ImageView addButton;
        ImageView removeButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_image);
            titleTextView = itemView.findViewById(R.id.item_title);
            priceTextView = itemView.findViewById(R.id.item_price);
            quantityTextView = itemView.findViewById(R.id.item_quantity);
            addButton = itemView.findViewById(R.id.quantity_plus);
            removeButton = itemView.findViewById(R.id.quantity_minus);
        }
    }
}
