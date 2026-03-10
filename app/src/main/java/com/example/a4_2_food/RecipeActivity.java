package com.example.a4_2_food;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RecipeActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipe_id";
    private static final String TAG = "RecipeActivity";

    private String lang;
    private Recipe recipe;
    private TextToSpeech tts;
    private VideoView videoPlayer;
    private Button btnVoice;
    private boolean isSpeaking = false;
    
    // Плеер для управления озвучкой
    Button playButton, pauseButton, stopButton;

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
            setupMediaForRecipe(); // Видео только для рецепта с кроликом
            setupAudioPlayerForAll(); // Плеер озвучки для всех рецептов
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_language).setOnClickListener(v -> showLanguageDialog());
    }

    private boolean isRecipeWithVideo() {
        if (recipe == null) return false;
        String nameRu = recipe.getName("ru");
        String nameEn = recipe.getName("en");
        return (nameRu != null && nameRu.contains("кролик"))
                || (nameEn != null && nameEn.toLowerCase().contains("rabbit"));
    }

    private void setupMediaForRecipe() {
        if (recipe == null || !isRecipeWithVideo()) return;

        View container = findViewById(R.id.recipe_video_container);
        videoPlayer = findViewById(R.id.recipe_video);
        container.setVisibility(View.VISIBLE);
        videoPlayer.setVisibility(View.VISIBLE);

        Uri myVideoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.recipe_video);
        videoPlayer.setVideoURI(myVideoUri);
        MediaController mediaController = new MediaController(this);
        videoPlayer.setMediaController(mediaController);
        mediaController.setMediaPlayer(videoPlayer);

    }

    
    private void startSpeech() {
        if (recipe == null || tts == null) return;
        String toSpeak = recipe.getName(lang) + ". ";
        for (String step : recipe.getStepsList(lang)) {
            toSpeak += step + ". ";
        }
        
        android.os.Bundle params = new android.os.Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "recipe_speech");
        
        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, params, "recipe_speech");
    }
    
    private void stopSpeech() {
        if (tts != null) {
            tts.stop();
            isSpeaking = false;
            updatePlayerButtons();
        }
    }
    

    // Инициализация TTS для плеера
    private void setupTTSForPlayer() {
        // Скрываем старую кнопку "Озвучить" - теперь используем плеер
        btnVoice = findViewById(R.id.btn_play_voice);
        btnVoice.setVisibility(View.GONE);
        
        // Инициализация TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            
            // Выбираем язык
            Locale locale = "en".equals(lang) ? Locale.ENGLISH : 
                           ("es".equals(lang) ? new Locale("es") : new Locale("ru"));
            
            if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(locale);
            }
            
            // Отслеживание состояния речи
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    runOnUiThread(() -> {
                        isSpeaking = true;
                        updatePlayerButtons();
                    });
                }

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        isSpeaking = false;
                        updatePlayerButtons();
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> {
                        isSpeaking = false;
                        updatePlayerButtons();
                    });
                }
            });
        });
    }

    // Плеер для озвучки страницы для всех рецептов
    private void setupAudioPlayerForAll() {
        View audioContainer = findViewById(R.id.audio_player_container);
        audioContainer.setVisibility(View.VISIBLE);

        // Находим кнопки плеера
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);

        // Инициализация TTS для озвучки
        setupTTSForPlayer();

        // Начальное состояние кнопок - только Play активна
        updatePlayerButtons();
    }


    // Методы управления озвучкой страницы через плеер
    public void play(View view){
        startSpeech();
        updatePlayerButtons();
    }

    public void pause(View view){
        stopSpeech(); // TTS не поддерживает паузу, только остановку
        updatePlayerButtons();
    }

    public void stop(View view){
        stopSpeech();
        updatePlayerButtons();
    }

    // Обновление состояния кнопок плеера
    private void updatePlayerButtons() {
        if (playButton == null || pauseButton == null || stopButton == null) return;
        
        if (isSpeaking) {
            playButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
        } else {
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (videoPlayer != null) {
            videoPlayer.stopPlayback();
        }
        // Дополнительная остановка речи при закрытии
        if (isSpeaking) {
            stopSpeech();
        }
        isSpeaking = false;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoPlayer != null) {
            videoPlayer.pause();
        }
        // Останавливаем речь при уходе с экрана
        if (isSpeaking) {
            stopSpeech();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // VideoView готов к воспроизведению
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

    private boolean isIngredientSectionHeader(String line) 
    {
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
        ImageView imageView = findViewById(imageViewId);
        if (drawableName == null || drawableName.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder_recipe);
            return;
        }
        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        imageView.setImageResource(resId != 0 ? resId : R.drawable.placeholder_recipe);
    }

    private void showLanguageDialog() 
    {
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
