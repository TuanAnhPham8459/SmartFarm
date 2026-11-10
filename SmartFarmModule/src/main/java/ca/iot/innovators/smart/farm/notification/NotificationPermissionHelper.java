/**
 * CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/
package ca.iot.innovators.smart.farm.notification;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import ca.iot.innovators.smart.farm.R;

public class NotificationPermissionHelper {

    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private static final String NOTIFICATION_ENABLED = "notifications_enabled";
    private static final String CRITICAL_ONLY_ENABLED = "critical_only_enabled";

    private final Context context;
    private final Fragment fragment;
    private final PermissionCallback callback;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private SharedPreferences prefs;

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
        void onPermissionError(String message);
    }

    public NotificationPermissionHelper(Fragment fragment, PermissionCallback callback) {
        this.context = fragment.requireContext();
        this.fragment = fragment;
        this.callback = callback;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void initializePermissionLauncher() {
        notificationPermissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        setNotificationEnabled(true);
                        if (callback != null) {
                            callback.onPermissionGranted();
                        }
                        Toast.makeText(context, R.string.enable_notif, Toast.LENGTH_SHORT).show();
                    } else {
                        setNotificationEnabled(false);
                        if (callback != null) {
                            callback.onPermissionDenied();
                        }
                        Toast.makeText(context, R.string.deny_notif_perm, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public boolean hasNotificationPermission() {
        if (requiresNotificationPermission()) {
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public boolean requiresNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    public void requestNotificationPermission() {
        if (hasNotificationPermission()) {
            return;
        }

        boolean requestedBefore = prefs.getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false);
        if (!requestedBefore) {
            prefs.edit().putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply();

            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            Toast.makeText(context, R.string.manual_permission, Toast.LENGTH_LONG).show();
            openAppSettings();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent);
    }

    public void syncNotificationPreference(SwitchPreferenceCompat notificationPref) {
        if (notificationPref != null) {
            boolean prefEnabled = prefs.getBoolean(NOTIFICATION_ENABLED, false);

            if (requiresNotificationPermission()) {
                if (prefEnabled && !hasNotificationPermission()) {
                    prefs.edit().putBoolean(NOTIFICATION_ENABLED, false).apply();
                    notificationPref.setChecked(false);
                }
            }
        }
    }

    public boolean shouldEnableNotification(boolean enabled) {
        if (enabled && requiresNotificationPermission()) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission();
                return false;
            }
        }
        return true;
    }

    public boolean handleImportNotificationPermission(boolean notifEnabled) {
        boolean needPermission = false;

        if (notifEnabled && requiresNotificationPermission()) {
            if (hasNotificationPermission()) {
                prefs.edit().putBoolean(NOTIFICATION_ENABLED, true).apply();
            } else {
                prefs.edit().putBoolean(NOTIFICATION_ENABLED, false).apply();
                needPermission = true;
            }
        } else {
            prefs.edit().putBoolean(NOTIFICATION_ENABLED, notifEnabled).apply();
        }

        return needPermission;
    }

    public void setNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(NOTIFICATION_ENABLED, enabled).apply();
    }

    public boolean getNotificationEnabled() {
        return prefs.getBoolean(NOTIFICATION_ENABLED, false);
    }

    public void setCriticalOnlyEnabled(boolean enabled) {
        prefs.edit().putBoolean(CRITICAL_ONLY_ENABLED, enabled).apply();
    }

    public boolean getCriticalOnlyEnabled() {
        return prefs.getBoolean(CRITICAL_ONLY_ENABLED, false);
    }

    public void handleEnableNotification() {
        // Check if permission is needed (API 33+)
        if (requiresNotificationPermission()) {
            if (hasNotificationPermission()) {
                // Permission already granted
                setNotificationEnabled(true);
                callback.onPermissionGranted();
                Toast.makeText(context, R.string.enable_notif, Toast.LENGTH_SHORT).show();
            } else {
                // Request permission - callback will handle result
                requestNotificationPermission();
            }
        } else {
            // API 32 and below - no permission needed, enable directly
            setNotificationEnabled(true);
            callback.onPermissionGranted();
            Toast.makeText(context, R.string.enable_notif, Toast.LENGTH_SHORT).show();
        }
    }

}