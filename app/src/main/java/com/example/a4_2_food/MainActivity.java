package com.example.a4_2_food;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecipeDbHelper dbHelper;
    private String lang;
    private List<Cookbook> books;
    private LinearLayout recipesContainer;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lang = LocaleHelper.getLanguage(this);
        dbHelper = new RecipeDbHelper(this);
        setLanguageButtonIcon((ImageButton) findViewById(R.id.btn_language), lang);

        books = dbHelper.getAllCookbooks();
        recipesContainer = findViewById(R.id.recipes_container);

        List<String> cuisineLabels = new ArrayList<>();
        cuisineLabels.add(getString(R.string.filter_all));
        for (Cookbook b : books) cuisineLabels.add(b.getName(lang));
        Spinner spinnerCuisine = findViewById(R.id.spinner_cuisine);
        spinnerCuisine.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cuisineLabels));
        spinnerCuisine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] mealLabels = {
                getString(R.string.filter_all),
                getString(R.string.meal_breakfast),
                getString(R.string.meal_lunch),
                getString(R.string.meal_dinner)
        };
        Spinner spinnerMeal = findViewById(R.id.spinner_meal_type);
        spinnerMeal.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mealLabels));
        spinnerMeal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        applyFilters();

        View filterToggleRow = findViewById(R.id.filter_toggle_row);
        View filterContainer = findViewById(R.id.filter_container);
        filterToggleRow.setOnClickListener(v -> {
            boolean visible = filterContainer.getVisibility() == View.VISIBLE;
            filterContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        });

        findViewById(R.id.btn_language).setOnClickListener(v -> showLanguageDialog());
    }

    private void applyFilters() {
        Spinner spinnerCuisine = findViewById(R.id.spinner_cuisine);
        Spinner spinnerMeal = findViewById(R.id.spinner_meal_type);
        int cuisinePos = spinnerCuisine.getSelectedItemPosition();
        int mealPos = spinnerMeal.getSelectedItemPosition();

        Long cookbookIdFilter = (cuisinePos <= 0 || books.isEmpty()) ? null : books.get(cuisinePos - 1).id;
        String mealTypeFilter = null;
        if (mealPos == 1) mealTypeFilter = "breakfast";
        else if (mealPos == 2) mealTypeFilter = "lunch";
        else if (mealPos == 3) mealTypeFilter = "dinner";

        List<Recipe> recipes = dbHelper.getRecipesFiltered(cookbookIdFilter, mealTypeFilter);
        fillRecipeList(recipes);
    }

    private void fillRecipeList(List<Recipe> recipes) {
        recipesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Recipe r : recipes) {
            View card = inflater.inflate(R.layout.item_recipe_card, recipesContainer, false);
            ((TextView) card.findViewById(R.id.recipe_card_name)).setText(r.getName(lang));
            setRecipeThumb((ImageView) card.findViewById(R.id.recipe_card_bg), r.getDishImageName());
            long recipeId = r.id;
            card.setOnClickListener(v -> openRecipe(recipeId));
            recipesContainer.addView(card);
        }
    }

    private void setRecipeThumb(ImageView imageView, String drawableName) {
        if (drawableName != null && !drawableName.isEmpty()) {
            int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
            if (resId != 0) imageView.setImageResource(resId);
        }
    }

    private void openRecipe(long recipeId) {
        Intent i = new Intent(this, RecipeActivity.class);
        i.putExtra(RecipeActivity.EXTRA_RECIPE_ID, recipeId);
        startActivity(i);
    }

    private void showLanguageDialog() {
        String[] items = {
                getString(R.string.lang_russian),
                getString(R.string.lang_english),
                getString(R.string.lang_spanish)
        };
        String[] langCodes = {"ru", "en", "es"};
        int[] flagResIds = {R.drawable.flag_ru, R.drawable.flag_usa, R.drawable.flag_esp};

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_language, null);
        LinearLayout container = dialogView.findViewById(R.id.language_dialog_container);

        for (int i = 0; i < items.length; i++) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_language_option, container, false);
            ((ImageView) row.findViewById(R.id.language_option_icon)).setImageResource(flagResIds[i]);
            ((TextView) row.findViewById(R.id.language_option_text)).setText(items[i]);
            int which = i;
            row.setOnClickListener(v -> {
                String chosen = langCodes[which];
                if (chosen.equals(lang)) return;
                LocaleHelper.saveLanguage(this, chosen);
                recreate();
            });
            container.addView(row);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.language_dialog_title)
                .setView(dialogView)
                .show();
    }

    private void setLanguageButtonIcon(ImageButton btn, String lang) {
        int resId = "en".equals(lang) ? R.drawable.flag_usa : ("es".equals(lang) ? R.drawable.flag_esp : R.drawable.flag_ru);
        btn.setImageResource(resId);
    }
}
