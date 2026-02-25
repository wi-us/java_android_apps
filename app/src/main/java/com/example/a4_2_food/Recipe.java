package com.example.a4_2_food;

import java.util.ArrayList;
import java.util.List;

// Рецепт: названия и тексты на трёх языках. Шаги и ингредиенты через "||".
public class Recipe {

    public static final String STEP_DELIMITER = "||";
    public long id;
    public long cookbookId;
    public String nameRu, nameEn, nameEs;
    public String ingredientsRu, ingredientsEn, ingredientsEs;
    public String stepsRu, stepsEn, stepsEs;
    public String imageDish;
    public String ingredientImages;
    public String mealType; // breakfast, lunch, dinner

    public Recipe(long id, long cookbookId,
                  String nameRu, String nameEn, String nameEs,
                  String ingredientsRu, String ingredientsEn, String ingredientsEs,
                  String stepsRu, String stepsEn, String stepsEs,
                  String imageDish, String ingredientImages, String mealType) {
        this.id = id;
        this.cookbookId = cookbookId;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.nameEs = nameEs;
        this.ingredientsRu = ingredientsRu;
        this.ingredientsEn = ingredientsEn;
        this.ingredientsEs = ingredientsEs;
        this.stepsRu = stepsRu;
        this.stepsEn = stepsEn;
        this.stepsEs = stepsEs;
        this.imageDish = imageDish;
        this.ingredientImages = ingredientImages;
        this.mealType = mealType != null ? mealType : "lunch";
    }

    public String getName(String lang) {
        if ("en".equals(lang)) return nameEn;
        if ("es".equals(lang)) return nameEs;
        return nameRu;
    }

    public String getIngredients(String lang) {
        if ("en".equals(lang)) return ingredientsEn;
        if ("es".equals(lang)) return ingredientsEs;
        return ingredientsRu;
    }

    public List<String> getIngredientsList(String lang) {
        String full = getIngredients(lang);
        if (full == null || full.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : full.split("\\|\\|")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    public List<String> getIngredientImageList() {
        if (ingredientImages == null || ingredientImages.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : ingredientImages.split("\\|\\|")) {
            String t = s.trim();
            list.add(t.isEmpty() ? null : t);
        }
        return list;
    }

    public String getSteps(String lang) {
        if ("en".equals(lang)) return stepsEn;
        if ("es".equals(lang)) return stepsEs;
        return stepsRu;
    }

    public List<String> getStepsList(String lang) {
        String full = getSteps(lang);
        if (full == null || full.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : full.split("\\|\\|")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    public String getDishImageName() {
        if (imageDish != null && !imageDish.isEmpty()) return imageDish;
        return String.format("img_%03d%03d1", cookbookId, id);
    }

    public String getStepImageName(int index) {
        return String.format("img_%03d%03d%d", cookbookId, id, index);
    }
}
