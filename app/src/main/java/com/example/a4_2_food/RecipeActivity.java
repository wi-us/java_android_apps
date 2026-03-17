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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RecipeActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipe_id";

    private String lang;
    private Recipe recipe;
    private TextToSpeech tts;
    private VideoView videoPlayer;
    private boolean isSpeaking = false;
    private boolean userRequestedStop = false;
    private int speechChunkIndex = 0;
    private String[] speechChunks = new String[0];
    Button voiceButton;

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
            ((TextView) findViewById(R.id.recipe_title_scroll)).setText(recipe.getName(lang));
            fillIngredientsContainer();
            fillStepsContainer();
            loadRecipeImage(R.id.recipe_photo_dish_scroll, recipe.getDishImageName());
            setupMediaForRecipe();
            setupAudioPlayerForAll();
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_language).setOnClickListener(v -> showLanguageDialog());
        FloatingActionButton fabTimerSettings = findViewById(R.id.fab_timer_settings);
        fabTimerSettings.setOnClickListener(v -> showTimerSettingsDialog());
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

        View container = findViewById(R.id.recipe_video_container_scroll);
        videoPlayer = findViewById(R.id.recipe_video_scroll);
        container.setVisibility(View.VISIBLE);
        videoPlayer.setVisibility(View.VISIBLE);

        Uri myVideoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.recipe_video);
        videoPlayer.setVideoURI(myVideoUri);
        MediaController mediaController = new MediaController(this);
        videoPlayer.setMediaController(mediaController);
        mediaController.setMediaPlayer(videoPlayer);
        mediaController.setAnchorView(container);

    }

    
    private void startSpeech() {
        if (recipe == null || tts == null) return;
        
        String fullText = recipe.getName(lang) + ". ";
        for (String step : recipe.getStepsList(lang)) {
            fullText += step + ". ";
        }
        
        speechChunks = fullText.trim().isEmpty() ? new String[]{fullText} : fullText.split("(?<=[.!?])\\s+");
        if (speechChunks.length == 0) speechChunks = new String[]{fullText};
        speechChunkIndex = 0;
        
        speakNextChunk();
    }
    
    private void speakNextChunk() {
        if (tts == null || speechChunkIndex >= speechChunks.length) {
            isSpeaking = false;
            speechChunkIndex = 0;
            if (voiceButton != null) voiceButton.setText(R.string.play_voice);
            return;
        }
        
        String chunk = speechChunks[speechChunkIndex];
        String utteranceId = "chunk_" + speechChunkIndex;
        
        android.os.Bundle params = new android.os.Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        
        tts.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        isSpeaking = true;
        if (voiceButton != null) voiceButton.setText(R.string.stop_voice);
    }
    
    private void stopSpeech() {
        if (tts != null) {
            userRequestedStop = true;
            tts.stop();
            isSpeaking = false;
            speechChunkIndex = 0;
            if (voiceButton != null) voiceButton.setText(R.string.play_voice);
        }
    }
    

    private void setupTTSForSimpleButton(Button voiceButton) {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            
            Locale locale = "en".equals(lang) ? Locale.ENGLISH : 
                           ("es".equals(lang) ? new Locale("es") : new Locale("ru"));
            
            if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(locale);
            }
            
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    runOnUiThread(() -> {
                        isSpeaking = true;
                        voiceButton.setText(R.string.stop_voice);
                    });
                }
                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        if (userRequestedStop) {
                            userRequestedStop = false;
                            return;
                        }
                        speechChunkIndex++;
                        speakNextChunk();
                    });
                }
                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> {
                        isSpeaking = false;
                        speechChunkIndex = 0;
                        voiceButton.setText(R.string.play_voice);
                    });
                }
            });
        });
        
        voiceButton.setOnClickListener(v -> {
            if (isSpeaking) {
                stopSpeech();
                voiceButton.setText(R.string.play_voice);
            } else {
                startSpeech();
            }
        });
    }

    private void setupAudioPlayerForAll() {
        if (recipe == null) return;
        
        voiceButton = findViewById(R.id.btn_play_voice_scroll);
        voiceButton.setVisibility(View.VISIBLE);
        setupTTSForSimpleButton(voiceButton);
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
        if (isSpeaking) {
            stopSpeech();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            imageView.setVisibility(View.GONE);
            return;
        }
        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(resId);
        } else {
            imageView.setVisibility(View.GONE);
        }
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

    private void showTimerSettingsDialog() {
        String recipeName = recipe != null ? recipe.getName("ru").toLowerCase() : "";
        boolean hasMarinary = recipeName.contains("шашлык") || recipeName.contains("кролик");
        
        TimerSettingsDialog dialog = TimerSettingsDialog.newInstance(hasMarinary);
        dialog.show(getSupportFragmentManager(), "timer_settings");
    }
}
