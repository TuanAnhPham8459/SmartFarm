package ca.iot.innovators.smart.farm.firestore;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class FireStoreService {
    private static FireStoreService instance;
    private final UserManager userManager;
    private final SensorDataManager sensorDataManager;
    private final DeviceManager deviceManager;
    private final DeviceControlManager deviceControlManager;
    private final AlertManager alertManager;
    private String selectedDeviceId; // MAC Address

    private static final String PREF_LAST_DEVICE = "last_selected_device_mac";

    private FireStoreService() {
        this.userManager = new UserManager();
        this.sensorDataManager = new SensorDataManager();
        this.deviceManager = new DeviceManager();
        this.deviceControlManager = new DeviceControlManager();
        this.alertManager = new AlertManager();
    }

    public static synchronized  FireStoreService getInstance() {
        if (instance == null) {
            instance = new FireStoreService();
        }
        return instance;
    }

    public UserManager getUserManager() { return userManager; }

    public SensorDataManager getSensorDataManager() { return sensorDataManager; }

    public DeviceManager getDeviceManager() { return deviceManager; }

    public DeviceControlManager getDeviceControlManager() { return deviceControlManager; }

    public AlertManager getAlertManager() { return alertManager; }

    public String getSelectedDeviceId() {
        return selectedDeviceId;
    }

    public void setSelectedDeviceId(String selectedDeviceId) {
        this.selectedDeviceId = selectedDeviceId;
    }

    /**
     * Loads the last selected device ID from persistent storage.
     */
    public void loadPersistedDevice(Context context) {
        if (selectedDeviceId == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            selectedDeviceId = prefs.getString(PREF_LAST_DEVICE, null);
        }
    }

    /**
     * Updates the selected device ID and persists it.
     */
    public void updateAndPersistDevice(Context context, String deviceId) {
        this.selectedDeviceId = deviceId;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_LAST_DEVICE, deviceId).apply();
    }
}
