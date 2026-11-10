package ca.iot.innovators.smart.farm.firestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model class for farm alerts.
 */
public class AlertModel {
    private String title;
    private String message;
    private String severity; // e.g., "Warning", "Critical", "Normal"
    private long timestamp;

    // Required no-argument constructor for Firestore
    public AlertModel() {}

    public AlertModel(String title, String message, String severity) {
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.timestamp = System.currentTimeMillis();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        return "Detected: " + sdf.format(new Date(timestamp));
    }
}
