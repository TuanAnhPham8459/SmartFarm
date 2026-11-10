package ca.iot.innovators.smart.farm.firestore;

/**
 * Model class for device control settings (Pump, Fan, Light, Auto Mode).
 */
public class DeviceControlModel {
    private boolean pump;
    private boolean fan;
    private boolean light;
    private boolean autoMode;

    // Required no-argument constructor for Firestore
    public DeviceControlModel() {}

    public DeviceControlModel(boolean pump, boolean fan, boolean light, boolean autoMode) {
        this.pump = pump;
        this.fan = fan;
        this.light = light;
        this.autoMode = autoMode;
    }

    public boolean isPump() { return pump; }
    public void setPump(boolean pump) { this.pump = pump; }

    public boolean isFan() { return fan; }
    public void setFan(boolean fan) { this.fan = fan; }

    public boolean isLight() { return light; }
    public void setLight(boolean light) { this.light = light; }

    public boolean isAutoMode() { return autoMode; }
    public void setAutoMode(boolean autoMode) { this.autoMode = autoMode; }
}
