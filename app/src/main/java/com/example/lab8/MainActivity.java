package com.example.lab8;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private TextInputLayout mathLayout;
    private TextInputLayout russianLayout;
    private TextInputLayout societyLayout;
    private TextInputLayout itLayout;
    private TextInputLayout chemistryLayout;
    private TextInputLayout physicsLayout;
    private TextInputLayout foreignLangLayout;
    private TextInputLayout geographyLayout;

    private TextInputEditText mathInput;
    private TextInputEditText russianInput;
    private TextInputEditText societyInput;
    private TextInputEditText itInput;
    private TextInputEditText chemistryInput;
    private TextInputEditText physicsInput;
    private TextInputEditText foreignLangInput;
    private TextInputEditText geographyInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupSubmit();
    }

    private void bindViews() {
        mathLayout = findViewById(R.id.math_input_layout);
        russianLayout = findViewById(R.id.russian_input_layout);
        societyLayout = findViewById(R.id.society_input_layout);
        itLayout = findViewById(R.id.it_input_layout);
        chemistryLayout = findViewById(R.id.chemistry_input_layout);
        physicsLayout = findViewById(R.id.physics_input_layout);
        foreignLangLayout = findViewById(R.id.flanguage_input_layout);
        geographyLayout = findViewById(R.id.geography_input_layout);

        mathInput = findViewById(R.id.math_input);
        russianInput = findViewById(R.id.russian_input);
        societyInput = findViewById(R.id.society_input);
        itInput = findViewById(R.id.it_input);
        chemistryInput = findViewById(R.id.chemistry_input);
        physicsInput = findViewById(R.id.physics_input);
        foreignLangInput = findViewById(R.id.flanguage_input);
        geographyInput = findViewById(R.id.geography_input);
    }

    private void setupSubmit() {
        MaterialButton btn = findViewById(R.id.button_next);
        btn.setOnClickListener(v -> {
            clearErrors();

            Integer math = readRequired(mathLayout, mathInput);
            Integer rus = readRequired(russianLayout, russianInput);
            if (math == null || rus == null) {
                return;
            }

            Integer inform = readOptional(itLayout, itInput);
            Integer social = readOptional(societyLayout, societyInput);
            Integer chemistry = readOptional(chemistryLayout, chemistryInput);
            Integer physics = readOptional(physicsLayout, physicsInput);
            Integer eng = readOptional(foreignLangLayout, foreignLangInput);
            Integer geo = readOptional(geographyLayout, geographyInput);

            if (inform == null || social == null || chemistry == null || physics == null || eng == null || geo == null) {
                return;
            }

            if (math == 0 && rus == 0 && inform == 0 && social == 0 && chemistry == 0 && physics == 0 && eng == 0 && geo == 0) {
                mathLayout.setError("Введите значение");
                russianLayout.setError("Введите значение");
                Toast.makeText(this, "Заполните хотя бы одно поле", Toast.LENGTH_SHORT).show();
                return;
            }

            if (math == 0) {
                mathLayout.setError("Значение не может быть 0");
                Toast.makeText(this, "Введите баллы по математике", Toast.LENGTH_SHORT).show();
                return;
            }
            if (rus == 0) {
                russianLayout.setError("Значение не может быть 0");
                Toast.makeText(this, "Введите баллы по русскому языку", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, ResultsActivity.class);
            intent.putExtra(ResultsActivity.EXTRA_MATH, math);
            intent.putExtra(ResultsActivity.EXTRA_RUS, rus);
            intent.putExtra(ResultsActivity.EXTRA_INFORM, inform);
            intent.putExtra(ResultsActivity.EXTRA_SOCIAL, social);
            intent.putExtra(ResultsActivity.EXTRA_CHEMISTRY, chemistry);
            intent.putExtra(ResultsActivity.EXTRA_PHYSICS, physics);
            intent.putExtra(ResultsActivity.EXTRA_ENG, eng);
            intent.putExtra(ResultsActivity.EXTRA_GEO, geo);
            startActivity(intent);
        });
    }

    private void clearErrors() {
        mathLayout.setError(null);
        russianLayout.setError(null);
        societyLayout.setError(null);
        itLayout.setError(null);
        chemistryLayout.setError(null);
        physicsLayout.setError(null);
        foreignLangLayout.setError(null);
        geographyLayout.setError(null);
    }

    private Integer readRequired(TextInputLayout layout, TextInputEditText input) {
        String s = String.valueOf(input.getText()).trim();
        if (s.isEmpty()) {
            layout.setError("Обязательное поле");
            return null;
        }

        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            layout.setError("Введите число");
            return null;
        }

        if (v < 0 || v > 100) {
            layout.setError("Введите 0..100");
            return null;
        }
        return v;
    }

    private Integer readOptional(TextInputLayout layout, TextInputEditText input) {
        String s = String.valueOf(input.getText()).trim();
        if (s.isEmpty()) return 0;

        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            layout.setError("Введите число 0..100");
            return null;
        }

        if (v < 0 || v > 100) {
            layout.setError("Введите 0..100");
            return null;
        }
        return v;
    }
}