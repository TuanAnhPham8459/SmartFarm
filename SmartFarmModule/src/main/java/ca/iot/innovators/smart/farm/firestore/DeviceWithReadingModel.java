package ca.iot.innovators.smart.farm.firestore;

/**
 * Combines a device with its latest reading.
 * Used for displaying devices in the dashboard.
 */
public class DeviceWithReadingModel {
    private DeviceModel device;
    private SensorDataModel latestReading;

    public DeviceWithReadingModel() {}

    public DeviceWithReadingModel(DeviceModel device, SensorDataModel latestReading) {
        this.device = device;
        this.latestReading = latestReading;
    }

    public DeviceModel getDevice() { return device; }
    public void setDevice(DeviceModel device) { this.device = device; }

    public SensorDataModel getLatestReading() { return latestReading; }
    public void setLatestReading(SensorDataModel latestReading) { this.latestReading = latestReading; }

    public boolean hasReading() { return latestReading != null; }

    // Helper methods for easier access
    public String getMacAddress() { return device != null ? device.getMacAddress() : null; }
    public String getDeviceName() { return device != null ? device.getDeviceName() : null; }

    public Double getTemperature() {
        return latestReading != null ? latestReading.getTemperature() : null;
    }
    public Double getHumidity() {
        return latestReading != null ? latestReading.getHumidity() : null;
    }
    public Double getLight() {
        return latestReading != null ? latestReading.getLight() : null;
    }
    public Long getTimestamp() {
        return latestReading != null ? latestReading.getTimestamp() : null;
    }
}