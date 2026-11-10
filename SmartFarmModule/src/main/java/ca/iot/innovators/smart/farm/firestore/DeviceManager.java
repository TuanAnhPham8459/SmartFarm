package ca.iot.innovators.smart.farm.firestore;

import android.util.Log;
import java.util.List;

/**
 * Manager class for handling device registration and ownership.
 */
public class DeviceManager extends BaseFireStoreManager {
    private static final String COLLECTION_DEVICES = "devices";
    private static final String TAG = "DeviceManager";

    public DeviceManager() {
        super(COLLECTION_DEVICES);
    }

    /**
     * Registers a new device to a user.
     */
    public void addDevice(DeviceModel device, FireStoreCallback callback) {
        db.collection(collectionName)
                .document(device.getMacAddress())
                .set(device)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Device added: " + device.getMacAddress());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding device", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Gets all devices registered to a specific user.
     */
    public void getUserDevices(String ownerId, FireStoreCallbackWthData<List<DeviceModel>> callback) {
        db.collection(collectionName)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DeviceModel> devices = queryDocumentSnapshots.toObjects(DeviceModel.class);
                    if (callback != null) callback.onSuccess(devices);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user devices", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}
