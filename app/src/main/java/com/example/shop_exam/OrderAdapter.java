package com.example.shop_exam;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Адаптер для списка заказов
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private List<OrderResponse> orders;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(OrderResponse order);
    }

    public OrderAdapter(List<OrderResponse> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderResponse order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber;
        TextView orderStatus;
        TextView orderDate;
        TextView orderTotal;

        ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.order_number);
            orderStatus = itemView.findViewById(R.id.order_status);
            orderDate = itemView.findViewById(R.id.order_date);
            orderTotal = itemView.findViewById(R.id.order_total);
        }

        void bind(OrderResponse order) {
            orderNumber.setText("Заказ №" + order.getId());

            if (order.getStatus() != null) {
                orderStatus.setText(order.getStatus().getName());
                orderStatus.setVisibility(View.VISIBLE);
            } else {
                orderStatus.setVisibility(View.GONE);
            }

            // Форматирование даты
            String dateText = formatDate(order.getCreatedAt());
            orderDate.setText(dateText);

            // Сумма заказа
            orderTotal.setText(String.format(Locale.getDefault(), "%.0f ₽", order.getTotalPrice()));

            // Обработка клика
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onOrderClick(order);
                    }
                }
            });
        }

        private String formatDate(String isoDate) {
            if (isoDate == null || isoDate.isEmpty()) {
                return "";
            }
            try {
                // Парсинг ISO формата
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = isoFormat.parse(isoDate);
                
                // Форматирование для отображения
                SimpleDateFormat displayFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
                return displayFormat.format(date);
            } catch (Exception e) {
                return isoDate;
            }
        }
    }
}







