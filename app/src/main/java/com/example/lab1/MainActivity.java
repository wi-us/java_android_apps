package com.example.lab1;

import android.opengl.Visibility;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    Button button, button2;
    ImageButton ib;
    TextView tv;
    TextView tv2;
    ImageView imageView;

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

        button = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);
        ib = findViewById(R.id.imageButton);
        tv =  findViewById(R.id.textView);
        tv2 =  findViewById(R.id.textView2);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tv.getVisibility() == View.VISIBLE)
                {
                    tv.setVisibility(View.INVISIBLE);
                    tv2.setVisibility(View.INVISIBLE);
                }
                else
                {
                    tv.setVisibility(View.VISIBLE);
                    tv2.setVisibility(View.VISIBLE);
                }

            }

        });

        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageView.getVisibility() == View.VISIBLE)
                {
                    imageView.setVisibility(View.INVISIBLE);
                }
                else
                {
                    imageView.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}