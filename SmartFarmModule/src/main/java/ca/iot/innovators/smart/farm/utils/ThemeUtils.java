package ca.iot.innovators.smart.farm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * Utility class to manage app themes.
 */
public class ThemeUtils {

    public static void applyTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString("theme", "light");
        applyThemeValue(theme);
    }

    public static void applyTheme(Context context, String themeValue) {
        applyThemeValue(themeValue);
    }

    private static void applyThemeValue(String theme) {
        switch (theme) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
