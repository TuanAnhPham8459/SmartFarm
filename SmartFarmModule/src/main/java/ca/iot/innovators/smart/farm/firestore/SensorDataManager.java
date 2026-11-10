package ca.iot.innovators.smart.farm.firestore;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for handling sensor data in Firestore.
 * Supports both subcollections and nested map fields for bme688 (temperature, humidity) and veml7700 (lux).
 */
public class SensorDataManager extends BaseFireStoreManager {
    private static final String COLLECTION_SENSORS = "sensor_data";
    private static final String TAG = "SensorDataManager";

    public SensorDataManager() {
        super(COLLECTION_SENSORS);
    }

    private Task<SensorDataModel> fetchSensorDataTask(DocumentSnapshot readingDoc) {
        long timestamp = 0;
        try {
            timestamp = Long.parseLong(readingDoc.getId());
        } catch (NumberFormatException e) {
            Long ts = readingDoc.getLong("timestamp");
            if (ts != null) timestamp = ts;
        }
        final long finalTimestamp = timestamp;

        Task<QuerySnapshot> bmeTask = readingDoc.getReference().collection("bme688").limit(1).get();
        Task<QuerySnapshot> vemlTask = readingDoc.getReference().collection("veml7700").limit(1).get();

        return Tasks.whenAllComplete(bmeTask, vemlTask)
                .continueWith(task -> {
                    double temp = 0.0;
                    double hum = 0.0;
                    double lux = 0.0;

                    // 1. Check bme688 subcollection
                    if (bmeTask.isSuccessful() && bmeTask.getResult() != null) {
                        if (!bmeTask.getResult().isEmpty()) {
                            DocumentSnapshot bmeDoc = bmeTask.getResult().getDocuments().get(0);
                            Double t = bmeDoc.getDouble("temperature");
                            Double h = bmeDoc.getDouble("humidity");
                            if (t != null) temp = t;
                            if (h != null) hum = h;
                        }
                    } else if (bmeTask.getException() != null) {
                        Log.w(TAG, "bme688 subcollection query failed: " + bmeTask.getException().getMessage());
                    }

                    // 2. Check veml7700 subcollection
                    if (vemlTask.isSuccessful() && vemlTask.getResult() != null) {
                        if (!vemlTask.getResult().isEmpty()) {
                            DocumentSnapshot vemlDoc = vemlTask.getResult().getDocuments().get(0);
                            Double l = vemlDoc.getDouble("lux");
                            if (l != null) lux = l;
                        }
                    } else if (vemlTask.getException() != null) {
                        Log.w(TAG, "veml7700 subcollection query failed: " + vemlTask.getException().getMessage());
                    }

                    // 3. Fallback: check bme688 and veml7700 as map fields on readingDoc if subcollections were empty/missing
                    if (temp == 0.0 && hum == 0.0) {
                        Object bmeObj = readingDoc.get("bme688");
                        if (bmeObj instanceof java.util.Map) {
                            java.util.Map<?, ?> map = (java.util.Map<?, ?>) bmeObj;
                            Object t = map.get("temperature");
                            Object h = map.get("humidity");
                            if (t instanceof Number) temp = ((Number) t).doubleValue();
                            if (h instanceof Number) hum = ((Number) h).doubleValue();
                        }
                    }

                    if (lux == 0.0) {
                        Object vemlObj = readingDoc.get("veml7700");
                        if (vemlObj instanceof java.util.Map) {
                            java.util.Map<?, ?> map = (java.util.Map<?, ?>) vemlObj;
                            Object l = map.get("lux");
                            if (l instanceof Number) lux = ((Number) l).doubleValue();
                        }
                    }

                    // 4. Root level fallbacks
                    if (temp == 0.0) {
                        Double rt = readingDoc.getDouble("temperature");
                        if (rt != null) temp = rt;
                    }
                    if (hum == 0.0) {
                        Double rh = readingDoc.getDouble("humidity");
                        if (rh != null) hum = rh;
                    }
                    if (lux == 0.0) {
                        Double rl = readingDoc.getDouble("light");
                        if (rl != null) lux = rl;
                    }

                    SensorDataModel model = new SensorDataModel();
                    model.setTemperature(temp);
                    model.setHumidity(hum);
                    model.setLight(lux);
                    model.setTimestamp(finalTimestamp);
                    String mac = readingDoc.getReference().getParent().getParent() != null ?
                            readingDoc.getReference().getParent().getParent().getId() : null;
                    model.setMacAddress(mac);
                    return model;
                });
    }

    // ============================================================
    // LISTEN TO LATEST SENSOR DATA
    // ============================================================

    public ListenerRegistration listenToSensorData(String deviceId,
                                                   FireStoreCallbackWthData<SensorDataModel> callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            if (callback != null) {
                callback.onFailure("Device ID is null or empty");
            }
            return null;
        }

        return db.collection(collectionName)
                .document(deviceId)
                .collection("readings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for device: " + deviceId, e);
                        if (callback != null) {
                            callback.onFailure(e.getMessage());
                        }
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        fetchSensorDataTask(doc)
                                .addOnSuccessListener(model -> {
                                    if (callback != null) callback.onSuccess(model);
                                })
                                .addOnFailureListener(ex -> {
                                    if (callback != null) callback.onFailure(ex.getMessage());
                                });
                    } else {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }
                });
    }

    // ============================================================
    // GET LATEST READING
    // ============================================================

    public void getLatestReading(String macAddress, FireStoreCallbackWthData<SensorDataModel> callback) {
        db.collection(collectionName)
                .document(macAddress)
                .collection("readings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        fetchSensorDataTask(doc)
                                .addOnSuccessListener(model -> {
                                    if (callback != null) callback.onSuccess(model);
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting latest reading", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // ============================================================
    // GET HISTORY
    // ============================================================

    public void getHistory(String macAddress, int limit, FireStoreCallbackWthData<List<SensorDataModel>> callback) {
        db.collection(collectionName)
                .document(macAddress)
                .collection("readings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task<SensorDataModel>> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        tasks.add(fetchSensorDataTask(doc));
                    }

                    if (tasks.isEmpty()) {
                        if (callback != null) callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(list -> {
                                List<SensorDataModel> history = new ArrayList<>();
                                for (Object obj : list) {
                                    if (obj instanceof SensorDataModel) {
                                        history.add((SensorDataModel) obj);
                                    }
                                }
                                if (callback != null) callback.onSuccess(history);
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting history", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // ============================================================
    // GET HISTORY BY TIME RANGE
    // ============================================================

    public void getHistoryByTimeRange(String macAddress, long startTime, long endTime,
                                      FireStoreCallbackWthData<List<SensorDataModel>> callback) {
        db.collection(collectionName)
                .document(macAddress)
                .collection("readings")
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task<SensorDataModel>> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        tasks.add(fetchSensorDataTask(doc));
                    }

                    if (tasks.isEmpty()) {
                        if (callback != null) callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(list -> {
                                List<SensorDataModel> history = new ArrayList<>();
                                for (Object obj : list) {
                                    if (obj instanceof SensorDataModel) {
                                        history.add((SensorDataModel) obj);
                                    }
                                }
                                if (callback != null) callback.onSuccess(history);
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting history by time range", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public ListenerRegistration listenToHistory(String macAddress, long startTime, long endTime,
                                                FireStoreCallbackWithMetadata<List<SensorDataModel>> callback) {
        Query query = db.collection(collectionName)
                .document(macAddress)
                .collection("readings")
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        return query.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                if (callback != null) callback.onFailure(e.getMessage());
                return;
            }

            if (queryDocumentSnapshots != null) {
                boolean isFromCache = queryDocumentSnapshots.getMetadata().isFromCache();
                List<Task<SensorDataModel>> tasks = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    tasks.add(fetchSensorDataTask(doc));
                }

                if (tasks.isEmpty()) {
                    if (callback != null) callback.onSuccess(new ArrayList<>(), isFromCache);
                    return;
                }

                Tasks.whenAllSuccess(tasks)
                        .addOnSuccessListener(list -> {
                            List<SensorDataModel> history = new ArrayList<>();
                            for (Object obj : list) {
                                if (obj instanceof SensorDataModel) {
                                    history.add((SensorDataModel) obj);
                                }
                            }
                            if (callback != null) callback.onSuccess(history, isFromCache);
                        })
                        .addOnFailureListener(ex -> {
                            if (callback != null) callback.onFailure(ex.getMessage());
                        });
            }
        });
    }

    // ============================================================
    // DEVICE METADATA
    // ============================================================

    public void getDeviceMetadata(String macAddress, FireStoreCallbackWthData<DeviceMetadataModel> callback) {
        db.collection(collectionName)
                .document(macAddress)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        DeviceMetadataModel metadata = documentSnapshot.toObject(DeviceMetadataModel.class);
                        if (callback != null) callback.onSuccess(metadata);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting device metadata", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public ListenerRegistration listenToDeviceMetadata(String macAddress,
                                                       FireStoreCallbackWthData<DeviceMetadataModel> callback) {
        return db.collection(collectionName)
                .document(macAddress)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        if (callback != null) callback.onFailure(e.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        DeviceMetadataModel metadata = documentSnapshot.toObject(DeviceMetadataModel.class);
                        if (callback != null) callback.onSuccess(metadata);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                });
    }
}