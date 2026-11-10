package ca.iot.innovators.smart.farm.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import ca.iot.innovators.smart.farm.MainActivity;
import ca.iot.innovators.smart.farm.R;

public class NotificationHelper {
    public static final String CHANNEL_ID = "smart_farm_alerts";
    private static final String NOTIFICATION_ENABLED = "notifications_enabled";
    private static final String CRITICAL_ONLY_ENABLED = "critical_only_enabled";

    private final Context context;
    private final SharedPreferences prefs;

    public NotificationHelper(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Farm Alerts";
            String description = "Notifications for critical farm conditions";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void showNotification(String title, String message, String severity) {
        // Check user preferences
        boolean enabled = prefs.getBoolean(NOTIFICATION_ENABLED, false);
        boolean criticalOnly = prefs.getBoolean(CRITICAL_ONLY_ENABLED, false);

        if (!enabled) return;
        if (criticalOnly && !"Critical".equalsIgnoreCase(severity)) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alerts)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            // Use timestamp as unique ID to prevent overwriting
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            // Permission not granted or revoked
        }
    }
}
