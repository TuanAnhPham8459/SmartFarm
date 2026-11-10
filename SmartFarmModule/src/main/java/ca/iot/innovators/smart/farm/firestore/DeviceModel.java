package ca.iot.innovators.smart.farm.firestore;

/**
 * Model class for farm devices (e.g., ESP32, Raspberry Pi).
 */
public class DeviceModel {
    private String macAddress; // Unique identifier
    private String deviceName;
    private String ownerId;    // Firebase User ID

    // Required no-argument constructor for Firestore serialization
    public DeviceModel() {}

    public DeviceModel(String macAddress, String deviceName, String ownerId) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.ownerId = ownerId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        return deviceName != null ? deviceName : macAddress;
    }
}
