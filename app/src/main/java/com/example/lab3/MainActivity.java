package com.example.lab3;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {

    Button button1;
    Button button2;
    Button button3;
    Button button4;

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

        button1 = findViewById(R.id.button_1);
        button2 = findViewById(R.id.button_2);
        button3 = findViewById(R.id.button_3);
        button4 = findViewById(R.id.button_4);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "кнопка номер 1 нажата", Toast.LENGTH_SHORT).show();

            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "кнопка номер 2 нажата", Toast.LENGTH_LONG).show();
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Сквозняк")
                        .setMessage("Закрыть окно?")
                        .setIcon(R.mipmap.test_icon)
                        .setPositiveButton("Да", (dialog, which) ->
                        {
                            button1.setTextColor(Color.RED);
                            button2.setTextColor(Color.RED);
                            button3.setTextColor(Color.RED);
                            button4.setTextColor(Color.RED);
                            closeContextMenu();
                        })
                        .setNegativeButton("Отмена", (dialog, which) ->
                        {
                            Toast.makeText(MainActivity.this, "Закрыл окно", Toast.LENGTH_SHORT).show();
                            closeContextMenu();
                        })
                        .show();
            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = {"Овца", "Корова", "Волк", "Лось", "Ленивец"};
                final boolean[] selectedItems = {false, false, false, false, false};
                final boolean[] correctAnswers = {true, true, false, true, false};

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Внезапный тест! Какие животные являются травоядными?")
                        .setMultiChoiceItems(items, selectedItems, (dialog, which, isChecked) ->
                        {
                            selectedItems[which] = isChecked;
                        })
                        .setPositiveButton("Готово", (dialog, which) ->
                        {
                            boolean isCorrect = true;
                            for (int i = 0; i < correctAnswers.length; i++)
                            {
                                if (selectedItems[i] != correctAnswers[i])
                                {
                                    isCorrect = false;
                                    break;
                                }
                            }
                            if (isCorrect)
                            {
                                Toast.makeText(MainActivity.this, "Все верно!", Toast.LENGTH_SHORT).show();
                            } else
                            {
                                Toast.makeText(MainActivity.this, "Неверно!", Toast.LENGTH_SHORT).show();
                                button1.setVisibility(View.INVISIBLE);
                                button2.setVisibility(View.INVISIBLE);
                                button3.setVisibility(View.INVISIBLE);
                                button4.setVisibility(View.INVISIBLE);
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        });
    }
}