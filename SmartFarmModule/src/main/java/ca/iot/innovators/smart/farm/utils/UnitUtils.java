package ca.iot.innovators.smart.farm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

/**
 * Utility class for unit conversion and preference handling.
 */
public class UnitUtils {

    public static boolean isFahrenheit(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unit = prefs.getString("units", "celsius");
        return "fahrenheit".equals(unit);
    }

    public static double convertTemperature(double celsius, boolean toFahrenheit) {
        if (toFahrenheit) {
            return (celsius * 1.8) + 32;
        }
        return celsius;
    }

    public static String getTemperatureUnit(boolean isFahrenheit) {
        return isFahrenheit ? "°F" : "°C";
    }
}
