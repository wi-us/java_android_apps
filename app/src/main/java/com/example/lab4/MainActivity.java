package com.example.lab4;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {
    // Unique channel ID for notifications
    public static final String CHANNEL_ID = "i.apps.notifications";

    // Unique identifier for the notification
    public static final int NOTIFICATION_ID = 1234;

    // Description for the notification channel
    public static final String DESCRIPTION = "Test notification";

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

        ImageButton morning = findViewById(R.id.sunriseButton);
        ImageButton midday = findViewById(R.id.dayButton);
        ImageButton evening = findViewById(R.id.sunsetButton);
        ImageButton midnight = findViewById(R.id.nightButton);

        // Create a notification channel (required for Android 8.0 and higher)
        createNotificationChannel();

        morning.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, com.example.lab4.morning.class);
                startActivity(intent);
            }
        });

        midday.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, com.example.lab4.midday.class);
                startActivity(intent);

                // Request runtime permission for notifications on Android 13 and higher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                                {Manifest.permission.POST_NOTIFICATIONS}, 101);
                        return;
                    }
                }

                // Trigger the notification
                sendNotification("Товарищ!", "День скоро завершится, успей закончить свои дела!");
            }
        });

        evening.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, com.example.lab4.evening.class);
                startActivity(intent);

                // Request runtime permission for notifications on Android 13 and higher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                                {Manifest.permission.POST_NOTIFICATIONS}, 101);
                        return;
                    }
                }

                // Trigger the notification
                sendNotification("СПАТЬ!", "Споки ноки!");
            }
        });

        midnight.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, com.example.lab4.midnight.class);
                startActivity(intent);
            }
        });
    }
    /**
     * Create a notification channel for devices running Android 8.0 or higher.
     * A channel groups notifications with similar behavior.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    DESCRIPTION,
                    NotificationManager.IMPORTANCE_HIGH
            );

            // Turn on notification light
            notificationChannel.enableLights(true);

            notificationChannel.setLightColor(Color.GREEN);

            // Allow vibration for notifications
            notificationChannel.enableVibration(true);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    /**
     * Build and send a notification with a custom layout and action.
     */
    private void sendNotification(String title, String text) {

        // Intent that triggers when the notification is tapped
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.geeksforgeeks.org/"));

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 6, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Converting resource file to icon
        Icon icon = Icon.createWithResource(this, R.drawable.night);

        // Converting resource file to bitmap image
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.horizon);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.sunset) // Notification icon
                .setLargeIcon(bitmap)
                .setContentTitle(title) // Title displayed in the notification
                .setContentText(text) // Text displayed in the notification
                .setContentIntent(pendingIntent) // Pending intent triggered when tapped
                .setAutoCancel(true) // Dismiss notification when tapped
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Notification priority for better visibility
                .addAction(0, "Let's Contribute", pendingIntent);

        // Display the notification
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }
}