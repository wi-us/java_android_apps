package com.example.shop_exam;

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
 * Адаптер для списка товаров в заказе
 */
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

    private Context context;
    private List<OrderDetailResponse.OrderItemResponse> items;

    public OrderItemAdapter(Context context, List<OrderDetailResponse.OrderItemResponse> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderDetailResponse.OrderItemResponse item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productQuantity;
        TextView productPrice;

        ViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productQuantity = itemView.findViewById(R.id.product_quantity);
            productPrice = itemView.findViewById(R.id.product_price);
        }

        void bind(OrderDetailResponse.OrderItemResponse item) {
            // Название товара
            GameVariantForList variant = item.getGameVariant();
            if (variant != null) {
                String name = "";
                if (variant.getGame() != null && variant.getGame().getTitle() != null) {
                    name = variant.getGame().getTitle();
                }
                if (variant.getEditionName() != null && !variant.getEditionName().isEmpty()) {
                    if (!name.isEmpty()) {
                        name += " — " + variant.getEditionName();
                    } else {
                        name = variant.getEditionName();
                    }
                }
                productName.setText(name);

                // Изображение
                String imageUrl = variant.getImageLink();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get()
                            .load(imageUrl)
                            .fit()
                            .centerCrop()
                            .into(productImage);
                }
            }

            // Количество
            productQuantity.setText(item.getQuantity() + " шт. × " +
                    String.format(Locale.getDefault(), "%.0f ₽", item.getPricePerItem()));

            // Сумма
            productPrice.setText(String.format(Locale.getDefault(), "%.0f ₽", item.getSubtotal()));
        }
    }
}







