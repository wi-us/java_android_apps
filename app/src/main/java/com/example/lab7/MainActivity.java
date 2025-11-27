package com.example.lab7;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> scanLauncher;

    private static final Map<String, QrInfo> SUPPORTED_CODES;

    static {
        Map<String, QrInfo> codes = new HashMap<>();
        codes.put("дом", new QrInfo(R.string.house_title, R.string.house_description, R.drawable.ic_house));
        codes.put("университет", new QrInfo(R.string.campus_title, R.string.campus_description, R.drawable.ic_campus));
        codes.put("студент", new QrInfo(R.string.student_title, R.string.student_description, R.drawable.ic_robot));
        SUPPORTED_CODES = Collections.unmodifiableMap(codes);
    }

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

        Button scanButton = findViewById(R.id.scanButton);
        setupLaunchers();
        scanButton.setOnClickListener(v -> tryLaunchScanner());
    }

    private void setupLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchScanner();
                    } else {
                        Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        scanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                    if (scanResult != null) {
                        handleScanResult(scanResult.getContents());
                    }
                }
        );
    }

    private void tryLaunchScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setOrientationLocked(true);
        integrator.setPrompt(getString(R.string.scan_prompt));
        integrator.setBeepEnabled(true);
        Intent scanIntent = integrator.createScanIntent();
        scanLauncher.launch(scanIntent);
    }

    private void handleScanResult(String contents) {
        if (contents == null) {
            Toast.makeText(this, R.string.detail_default_description, Toast.LENGTH_SHORT).show();
            return;
        }
        String normalized = contents.trim().toLowerCase(Locale.getDefault());
        QrInfo info = SUPPORTED_CODES.get(normalized);
        if (info == null) {
            Toast.makeText(this, R.string.unknown_qr_message, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent detailIntent = new Intent(this, DetailActivity.class);
        detailIntent.putExtra(DetailActivity.EXTRA_TITLE, getString(info.titleRes));
        detailIntent.putExtra(DetailActivity.EXTRA_DESCRIPTION, getString(info.descriptionRes));
        detailIntent.putExtra(DetailActivity.EXTRA_IMAGE, info.imageRes);
        startActivity(detailIntent);
    }

    private static class QrInfo {
        @StringRes
        final int titleRes;
        @StringRes
        final int descriptionRes;
        @DrawableRes
        final int imageRes;

        QrInfo(@StringRes int titleRes, @StringRes int descriptionRes, @DrawableRes int imageRes) {
            this.titleRes = titleRes;
            this.descriptionRes = descriptionRes;
            this.imageRes = imageRes;
        }
    }
}