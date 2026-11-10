package ca.iot.innovators.smart.farm.firestore;

/**
 * Model class for sensor data fetched from Firestore.
 * Matches the data structure from the Python simulator.
 */
public class SensorDataModel {
    private double temperature;
    private double humidity;
    private double light;
    private long timestamp;
    private String macAddress;
    private String deviceName; // Device name from Python script
    private int sequence;      // Sequence number from Python script

    // Fruit color (OPT4048 CIE chromaticity + brightness)
    private double cieX;
    private double cieY;
    private double colorLux;

    // RFID card / fruit detection
    private boolean rfidTagPresent;
    private String rfidUid;

    // Required no-argument constructor for Firestore serialization
    public SensorDataModel() {}

    public SensorDataModel(double temperature, double humidity, double light) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.light = light;
        this.timestamp = System.currentTimeMillis();
    }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getLight() { return light; }
    public void setLight(double light) { this.light = light; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public double getCieX() { return cieX; }
    public void setCieX(double cieX) { this.cieX = cieX; }

    public double getCieY() { return cieY; }
    public void setCieY(double cieY) { this.cieY = cieY; }

    public double getColorLux() { return colorLux; }
    public void setColorLux(double colorLux) { this.colorLux = colorLux; }

    public boolean isRfidTagPresent() { return rfidTagPresent; }
    public void setRfidTagPresent(boolean rfidTagPresent) { this.rfidTagPresent = rfidTagPresent; }

    public String getRfidUid() { return rfidUid; }
    public void setRfidUid(String rfidUid) { this.rfidUid = rfidUid; }
}