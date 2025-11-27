package com.example.lab7;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_IMAGE = "extra_image";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ImageView imageView = findViewById(R.id.detailImage);
        TextView titleView = findViewById(R.id.detailTitle);
        TextView descriptionView = findViewById(R.id.detailDescription);

        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_TITLE);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);
        @DrawableRes int imageRes = intent.getIntExtra(EXTRA_IMAGE, 0);

        titleView.setText(title != null ? title : getString(R.string.detail_default_title));
        descriptionView.setText(description != null ? description : getString(R.string.detail_default_description));
        if (imageRes != 0) {
            imageView.setImageResource(imageRes);
        } else {
            imageView.setImageResource(R.drawable.ic_placeholder_object);
        }
    }
}

