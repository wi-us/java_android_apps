package com.example.shop_exam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран выбора адреса на карте 2ГИС
 */
public class MapPickerActivity extends AppCompatActivity {

    // Ключи для передачи данных через Intent
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";
    public static final String RESULT_LAT = "result_lat";
    public static final String RESULT_LON = "result_lon";
    public static final String RESULT_TEXT = "result_text";

    private static final int PERMISSION_REQUEST = 901;

    private WebView webView;
    private ApiService apiService;
    private FusedLocationProviderClient locationClient;

    private String mapApiKey;
    private double defaultLat;
    private double defaultLon;
    private boolean isPicking = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        // Получаем координаты из Intent (если есть)
        defaultLat = getIntent().getDoubleExtra(EXTRA_LAT, 55.7558);
        defaultLon = getIntent().getDoubleExtra(EXTRA_LON, 37.6173);

        // API-ключ
        mapApiKey = getString(R.string.dgis_map_key);

        // Инициализация сервисов
        apiService = ApiClient.getClient(this).create(ApiService.class);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Настраиваем WebView
        webView = findViewById(R.id.map_webview);
        setupWebView();

        // Запрос разрешения на геолокацию и загрузка карты
        requestLocationAndLoadMap();
    }

    // Настройка WebView для отображения карты
    private void setupWebView() {
        webView.setWebViewClient(new WebViewClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Добавляем JavaScript-интерфейс для взаимодействия с картой
        webView.addJavascriptInterface(new MapBridge(), "Android");
    }

    // Запрос разрешения на геолокацию
    private void requestLocationAndLoadMap() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasPermission) {
            // Разрешение есть - получаем координаты
            getCurrentLocation();
        } else {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено
                getCurrentLocation();
            } else {
                // Разрешение не получено - используем координаты по умолчанию
                loadMap(defaultLat, defaultLon);
            }
        }
    }

    // Получение текущего местоположения
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        // Сначала пробуем получить последнее известное местоположение
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                loadMap(location.getLatitude(), location.getLongitude());
            } else {
                // Если последнее местоположение недоступно - запрашиваем текущее
                requestCurrentLocation();
            }
        }).addOnFailureListener(e -> {
            loadMap(defaultLat, defaultLon);
        });
    }

    // Запрос текущего местоположения
    @SuppressLint("MissingPermission")
    private void requestCurrentLocation() {
        CancellationTokenSource cts = new CancellationTokenSource();

        locationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        loadMap(location.getLatitude(), location.getLongitude());
                    } else {
                        loadMap(defaultLat, defaultLon);
                    }
                })
                .addOnFailureListener(e -> {
                    loadMap(defaultLat, defaultLon);
                });
    }

    // Загрузка карты с указанными координатами
    private void loadMap(double lat, double lon) {
        String url = "file:///android_asset/dgis_map.html"
                + "?key=" + mapApiKey
                + "&lat=" + lat
                + "&lon=" + lon
                + "&zoom=14";
        webView.loadUrl(url);
    }

    /**
     * JavaScript-интерфейс для взаимодействия с картой
     */
    private class MapBridge {

        // Вызывается при перемещении маркера на карте
        @JavascriptInterface
        public void onPointChanged(String latStr, String lonStr) {
            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                getAddressByCoordinates(lat, lon, false);
            } catch (NumberFormatException e) {
                // Игнорируем ошибку парсинга
            }
        }

        // Вызывается при нажатии кнопки "Выбрать"
        @JavascriptInterface
        public void onPicked(String latStr, String lonStr) {
            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                getAddressByCoordinates(lat, lon, true);
            } catch (NumberFormatException e) {
                // Игнорируем ошибку парсинга
            }
        }
    }

    // Получение адреса по координатам (обратное геокодирование)
    private void getAddressByCoordinates(double lat, double lon, boolean shouldFinish) {
        isPicking = shouldFinish;

        apiService.addressReverse(lat, lon).enqueue(new Callback<AddressDetails>() {
            @Override
            public void onResponse(Call<AddressDetails> call, Response<AddressDetails> response) {
                String address = "";
                if (response.isSuccessful() && response.body() != null) {
                    address = response.body().getText();
                    if (address == null) address = "";
                }

                // Показываем адрес на карте
                final String finalAddress = address;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAddressOnMap(finalAddress);
                    }
                });

                // Если нужно завершить - возвращаем результат
                if (isPicking) {
                    returnResult(lat, lon, finalAddress);
                }
            }

            @Override
            public void onFailure(Call<AddressDetails> call, Throwable t) {
                if (isPicking) {
                    returnResult(lat, lon, "");
                }
            }
        });
    }

    // Показ адреса на карте через JavaScript
    private void showAddressOnMap(String address) {
        String jsAddress = escapeForJs(address);
        String script = "if(window.setSelectedAddress) window.setSelectedAddress(\"" + jsAddress + "\");";
        webView.evaluateJavascript(script, null);
    }

    // Экранирование строки для JavaScript
    private String escapeForJs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    // Возврат результата в вызывающую Activity
    private void returnResult(double lat, double lon, String address) {
        Intent data = new Intent();
        data.putExtra(RESULT_LAT, lat);
        data.putExtra(RESULT_LON, lon);
        data.putExtra(RESULT_TEXT, address);
        setResult(RESULT_OK, data);
        finish();
    }
}
