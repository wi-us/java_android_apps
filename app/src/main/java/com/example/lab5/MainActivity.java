package com.example.lab5;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.lab5.fragment_first;
import com.example.lab5.fragment_second;
import com.example.lab5.fragment_third;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        
        // Отключаем ripple эффект и индикатор активности
        bottomNavigationView.setItemRippleColor(null);
        bottomNavigationView.setItemActiveIndicatorEnabled(false);
        
        Fragment firstFragment = new fragment_first();
        Fragment secondFragment = new fragment_second();
        Fragment thirdFragment = new fragment_third();

        // Используем OnItemSelectedListener для корректного управления состоянием
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.profile) {
                    setCurrentFragment(firstFragment);
                } else if (itemId == R.id.home) {
                    setCurrentFragment(secondFragment);
                } else if (itemId == R.id.settings) {
                    setCurrentFragment(thirdFragment);
                }
                
                return true; // Возвращаем true, чтобы элемент остался выбранным
            }
        });
        
        // Устанавливаем первый фрагмент по умолчанию
        bottomNavigationView.setSelectedItemId(R.id.profile);
        setCurrentFragment(firstFragment);
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}