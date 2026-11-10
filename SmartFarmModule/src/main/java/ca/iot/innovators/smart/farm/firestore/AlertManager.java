package ca.iot.innovators.smart.farm.firestore;

import android.util.Log;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.List;

/**
 * Manager class for handling farm alerts in Firestore.
 */
public class AlertManager extends BaseFireStoreManager {
    private static final String COLLECTION_ALERTS = "alerts";
    private static final String TAG = "AlertManager";

    public AlertManager() {
        super(COLLECTION_ALERTS);
    }

    /**
     * Saves a new alert to Firestore.
     */
    public void saveAlert(String macAddress, AlertModel alert, FireStoreCallback callback) {
        db.collection(COLLECTION_ALERTS)
                .document(macAddress)
                .collection("history")
                .add(alert)
                .addOnSuccessListener(documentReference -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Listens for real-time alerts for a specific device.
     */
    public ListenerRegistration listenToAlerts(String macAddress, FireStoreCallbackWthData<List<AlertModel>> callback) {
        return db.collection(COLLECTION_ALERTS)
                .document(macAddress)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        if (callback != null) callback.onFailure(e.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<AlertModel> alerts = queryDocumentSnapshots.toObjects(AlertModel.class);
                        if (callback != null) callback.onSuccess(alerts);
                    }
                });
    }
}
