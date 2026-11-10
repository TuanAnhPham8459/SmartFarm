package ca.iot.innovators.smart.farm.firestore;

import android.util.Log;

import com.google.firebase.firestore.ListenerRegistration;

/**
 * Manager class for handling real-time device controls.
 */
public class DeviceControlManager extends BaseFireStoreManager {
    private static final String COLLECTION_CONTROLS = "device_controls";
    private static final String TAG = "DeviceControlManager";

    public DeviceControlManager() {
        super(COLLECTION_CONTROLS);
    }

    /**
     * Listens for real-time updates to controls for a specific device.
     */
    public ListenerRegistration listenToControls(String macAddress, FireStoreCallbackWthData<DeviceControlModel> callback) {
        return db.collection(collectionName)
                .document(macAddress)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        if (callback != null) callback.onFailure(e.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        DeviceControlModel controls = snapshot.toObject(DeviceControlModel.class);
                        if (callback != null) callback.onSuccess(controls);
                    } else {
                        // Create default controls if document doesn't exist
                        DeviceControlModel defaultControls = new DeviceControlModel(false, false, false, true);
                        updateAllControls(macAddress, defaultControls);
                        if (callback != null) callback.onSuccess(defaultControls);
                    }
                });
    }

    /**
     * Updates a single control field in Firestore.
     */
    public void updateControlField(String macAddress, String field, boolean value) {
        db.collection(collectionName)
                .document(macAddress)
                .update(field, value)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating field: " + field, e));
    }

    /**
     * Updates all control fields at once.
     */
    public void updateAllControls(String macAddress, DeviceControlModel controls) {
        db.collection(collectionName)
                .document(macAddress)
                .set(controls)
                .addOnFailureListener(e -> Log.e(TAG, "Error setting controls", e));
    }
}
