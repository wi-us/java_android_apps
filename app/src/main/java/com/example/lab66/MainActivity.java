package com.example.lab66;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private ArrayAdapter<String> productsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseHelper = new DatabaseHelper(this);
        ListView listView = findViewById(R.id.productsList);
        productsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(productsAdapter);

        Button foodButton = findViewById(R.id.btnFood);
        Button clothesButton = findViewById(R.id.btnClothes);
        Button toysButton = findViewById(R.id.btnToys);

        foodButton.setOnClickListener(v -> showProducts("Еда"));
        clothesButton.setOnClickListener(v -> showProducts("Одежда"));
        toysButton.setOnClickListener(v -> showProducts("Игрушки"));

        showProducts("Еда");
    }

    private void showProducts(String categoryName) {
        List<Product> products = databaseHelper.getProductsByCategory(categoryName);
        List<String> formatted = new ArrayList<>();
        for (Product product : products) {
            String item = product.getName() + "\n" +
                    product.getDescription() + "\n" +
                    String.format(Locale.getDefault(), "Цена: %.2f ₽", product.getPrice());
            formatted.add(item);
        }
        productsAdapter.clear();
        productsAdapter.addAll(formatted);
        productsAdapter.notifyDataSetChanged();
    }
}