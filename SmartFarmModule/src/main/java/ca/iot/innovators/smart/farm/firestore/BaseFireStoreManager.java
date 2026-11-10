package ca.iot.innovators.smart.farm.firestore;

import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseFireStoreManager {
    protected final FirebaseFirestore db;
    protected final String collectionName;

    public BaseFireStoreManager(String collectionName) {
        this.db = FirebaseFirestore.getInstance();
        this.collectionName = collectionName;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public interface FireStoreCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface FireStoreCallbackWthData<T> {
        void onSuccess(T data);
        void onFailure(String errorMessage);
    }

    public interface FireStoreCallbackWithMetadata<T> {
        void onSuccess(T data, boolean isFromCache);
        void onFailure(String errorMessage);
    }
}