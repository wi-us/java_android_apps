package com.example.lab5;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class fragment_third extends Fragment
{

    private ImageButton notificationButton;
    private NotificationHelper notificationHelper;
    
    // Константа для запроса разрешения на уведомления
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_third, container, false);
        
        // Инициализация элементов интерфейса
        notificationButton = view.findViewById(R.id.notificationButton);

        // Инициализация NotificationHelper
        notificationHelper = new NotificationHelper(getContext());

        // Обработчик нажатия на кнопку уведомлений
        notificationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendNotification();
            }
        });
        
        return view;
    }
    
    private void sendNotification()
    {
        // Проверка разрешения на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED)
            {
                // Запрос разрешения
                requestPermissions(new String[]{ Manifest.permission.POST_NOTIFICATIONS },
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        notificationHelper.sendStudentNotification("Студент:\tВаравинов Даниил\nГруппа:\tИСиТ-423901", "Информация о студенте");
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Разрешение получено, отправляем уведомление
                sendNotification();
            }
        }
    }
}