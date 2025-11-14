package com.example.lab4;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class midnight extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_midnight);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.student), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btn = findViewById(R.id.nightButton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(midnight.this)
                        .setTitle("БУ! Испугался?")
                        .setMessage("Ты спишь?")
                        .setPositiveButton("Да", (dialog, which) ->
                        {
                            closeContextMenu();
                            finishAffinity();
                            System.exit(0);
                        })
                        .setNegativeButton("Нет", (dialog, which) ->
                        {
                            closeContextMenu();
                            finish();
                        })
                        .show();
            }
        });

    }
}