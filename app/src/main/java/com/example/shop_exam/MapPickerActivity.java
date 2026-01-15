package com.example.shop_exam;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
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
import com.google.android.gms.tasks.CancellationTokenSource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";
    public static final String RESULT_LAT = "result_lat";
    public static final String RESULT_LON = "result_lon";
    public static final String RESULT_TEXT = "result_text";

    private WebView webView;
    private ApiService apiService;
    private volatile boolean finishingWithPick = false;

    private static final int REQ_LOCATION = 901;
    private FusedLocationProviderClient fusedClient;
    private String mapKey;
    private double fallbackLat;
    private double fallbackLon;

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        fallbackLat = getIntent().getDoubleExtra(EXTRA_LAT, 55.7558);
        fallbackLon = getIntent().getDoubleExtra(EXTRA_LON, 37.6173);

        mapKey = getString(R.string.dgis_map_key);
        apiService = ApiClient.getClient(this).create(ApiService.class);
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        webView = findViewById(R.id.map_webview);
        webView.setWebViewClient(new WebViewClient());

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new Bridge(), "Android");

        // Центрируем карту на текущем городе покупателя (по геолокации), если разрешение дано.
        ensureLocationAndLoad();
    }

    private void ensureLocationAndLoad() {
        if (hasLocationPermission()) {
            loadByDeviceLocationOrFallback();
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ_LOCATION
        );
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void loadByDeviceLocationOrFallback() {
        if (fusedClient == null) {
            loadMap(fallbackLat, fallbackLon);
            return;
        }

        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                loadMap(loc.getLatitude(), loc.getLongitude());
                return;
            }
            // Если lastLocation недоступна — попробуем запросить текущую.
            try {
                CancellationTokenSource cts = new CancellationTokenSource();
                fusedClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.getToken()
                ).addOnSuccessListener(l2 -> {
                    if (l2 != null) {
                        loadMap(l2.getLatitude(), l2.getLongitude());
                    } else {
                        loadMap(fallbackLat, fallbackLon);
                    }
                }).addOnFailureListener(e -> loadMap(fallbackLat, fallbackLon));
            } catch (Throwable t) {
                loadMap(fallbackLat, fallbackLon);
            }
        }).addOnFailureListener(e -> loadMap(fallbackLat, fallbackLon));
    }

    private void loadMap(double lat, double lon) {
        String url = "file:///android_asset/dgis_map.html"
                + "?key=" + encode(mapKey)
                + "&lat=" + lat
                + "&lon=" + lon
                + "&zoom=14";
        webView.loadUrl(url);
    }

    private String encode(String s) {
        if (s == null) return "";
        return s.replace(" ", "%20");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (hasLocationPermission()) {
                loadByDeviceLocationOrFallback();
            } else {
                // Разрешение не дали — оставляем fallback (что пришло из Checkout или Москва)
                loadMap(fallbackLat, fallbackLon);
            }
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void onPointChanged(String latStr, String lonStr) {
            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                fetchAndShowAddress(lat, lon, false);
            } catch (Exception e) {
                // ignore
            }
        }

        @JavascriptInterface
        public void onPicked(String latStr, String lonStr) {
            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                fetchAndShowAddress(lat, lon, true);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void fetchAndShowAddress(double lat, double lon, boolean finishAfter) {
        if (apiService == null) return;
        finishingWithPick = finishAfter;
        apiService.addressReverse(lat, lon).enqueue(new Callback<AddressDetails>() {
            @Override
            public void onResponse(Call<AddressDetails> call, Response<AddressDetails> response) {
                String text = null;
                if (response.isSuccessful() && response.body() != null) {
                    text = response.body().getText();
                }
                final String addr = (text == null) ? "" : text;

                // показать адрес на странице карты
                runOnUiThread(() -> {
                    try {
                        webView.evaluateJavascript("window.setSelectedAddress && window.setSelectedAddress(" + jsStr(addr) + ");", null);
                    } catch (Exception ignored) { }
                });

                if (finishingWithPick) {
                    Intent data = new Intent();
                    data.putExtra(RESULT_LAT, lat);
                    data.putExtra(RESULT_LON, lon);
                    data.putExtra(RESULT_TEXT, addr);
                    setResult(RESULT_OK, data);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<AddressDetails> call, Throwable t) {
                if (finishingWithPick) {
                    Intent data = new Intent();
                    data.putExtra(RESULT_LAT, lat);
                    data.putExtra(RESULT_LON, lon);
                    data.putExtra(RESULT_TEXT, "");
                    setResult(RESULT_OK, data);
                    finish();
                } else {
                    Toast.makeText(MapPickerActivity.this, "Не удалось определить адрес", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String jsStr(String s) {
        if (s == null) return "\"\"";
        // very small JSON-string escape
        String out = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        return "\"" + out + "\"";
    }
}


