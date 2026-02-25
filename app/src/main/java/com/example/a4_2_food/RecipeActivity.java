package com.example.a4_2_food;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipe_id";

    private String lang;
    private Recipe recipe;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, 1);
        lang = LocaleHelper.getLanguage(this);
        setLanguageButtonIcon((ImageButton) findViewById(R.id.btn_language), lang);

        RecipeDbHelper dbHelper = new RecipeDbHelper(this);
        recipe = dbHelper.getRecipeById(recipeId);

        if (recipe != null) {
            ((TextView) findViewById(R.id.recipe_title)).setText(recipe.getName(lang));
            fillIngredientsContainer();
            fillStepsContainer();
            loadRecipeImage(R.id.recipe_photo_dish, recipe.getDishImageName());
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_language).setOnClickListener(v -> showLanguageDialog());
    }

    private void fillIngredientsContainer() {
        LinearLayout container = findViewById(R.id.ingredients_container);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String line : recipe.getIngredientsList(lang)) {
            if (isIngredientSectionHeader(line)) {
                int colon = line.indexOf(':');
                String headerTitle = line.substring(0, colon + 1).trim();
                String rest = colon + 1 < line.length() ? line.substring(colon + 1).trim() : "";
                View headerItem = inflater.inflate(R.layout.item_ingredient_header, container, false);
                ((TextView) headerItem.findViewById(R.id.ingredient_header_text)).setText(headerTitle);
                container.addView(headerItem);
                List<String> parts = Arrays.asList(rest.split(",\\s+"));
                for (String part : parts) {
                    String t = part.trim();
                    if (t.isEmpty()) continue;
                    View item = inflater.inflate(R.layout.item_ingredient, container, false);
                    ((TextView) item.findViewById(R.id.ingredient_text)).setText(t);
                    container.addView(item);
                }
            } else {
                View item = inflater.inflate(R.layout.item_ingredient, container, false);
                ((TextView) item.findViewById(R.id.ingredient_text)).setText(line);
                container.addView(item);
            }
        }
    }

    private boolean isIngredientSectionHeader(String line) {
        if (line == null || line.isEmpty()) return false;
        int colon = line.indexOf(':');
        if (colon <= 0 || colon > 25) return false;
        String prefix = line.substring(0, colon).trim();
        return prefix.matches("^[А-ЯЁA-Z\\s]+$");
    }

    private void fillStepsContainer() {
        LinearLayout container = findViewById(R.id.steps_container);
        LayoutInflater inflater = LayoutInflater.from(this);
        final long minecraftCookbookId = 3L;
        if (recipe.cookbookId == minecraftCookbookId) {
            String stepDrawableName = recipe.getStepImageName(2);
            int resId = getResources().getIdentifier(stepDrawableName, "drawable", getPackageName());
            if (resId != 0) {
                View item = inflater.inflate(R.layout.item_recipe_step_image, container, false);
                ((ImageView) item.findViewById(R.id.step_image)).setImageResource(resId);
                container.addView(item);
                return;
            }
        }
        int stepIndex = 1;
        for (String stepText : recipe.getStepsList(lang)) {
            View item = inflater.inflate(R.layout.item_recipe_step, container, false);
            ((TextView) item.findViewById(R.id.step_number)).setText(String.valueOf(stepIndex));
            ((TextView) item.findViewById(R.id.step_text)).setText(stepText);
            container.addView(item);
            stepIndex++;
        }
    }

    private void loadRecipeImage(int imageViewId, String drawableName) {
        if (drawableName == null || drawableName.isEmpty()) return;
        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            ((ImageView) findViewById(imageViewId)).setImageResource(resId);
        }
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
