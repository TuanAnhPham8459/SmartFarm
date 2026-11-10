package ca.iot.innovators.smart.farm.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class FeedbackModel {
    private String comment;
    private String deviceInfo;
    private String email;
    private String name;
    private String phone;
    private float rating;
    private String category;

    @ServerTimestamp
    private Timestamp createdAt;

    public FeedbackModel() {
        // Required empty constructor for Firestore deserialization
    }

    public FeedbackModel(String name, String email, String phone, String category,
                         float rating, String comment, String deviceInfo) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.category = category;
        this.rating = rating;
        this.comment = comment;
        this.deviceInfo = deviceInfo;
    }

    public String getComment() { return comment; }
    public String getDeviceInfo() { return deviceInfo; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public float getRating() { return rating; }
    public String getCategory() { return category; }
    public Timestamp getCreatedAt() { return createdAt; }
}