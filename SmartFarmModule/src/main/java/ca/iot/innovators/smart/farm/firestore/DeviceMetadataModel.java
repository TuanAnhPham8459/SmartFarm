package ca.iot.innovators.smart.farm.firestore;

/**
 * Device metadata stored in sensor_data collection.
 * This is the index document for each device.
 * Matches the Python script's metadata structure.
 */
public class DeviceMetadataModel {
    private String macAddress;
    private String deviceName;
    private String ownerId;
    private long lastUpdated;
    private long totalReadings;
    private long createdAt;

    public DeviceMetadataModel() {}

    public DeviceMetadataModel(String macAddress, String deviceName, String ownerId) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.ownerId = ownerId;
        this.lastUpdated = 0;
        this.totalReadings = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public long getTotalReadings() { return totalReadings; }
    public void setTotalReadings(long totalReadings) { this.totalReadings = totalReadings; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}