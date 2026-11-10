package ca.iot.innovators.smart.farm.firestore;

import android.util.Log;

import ca.iot.innovators.smart.farm.profile.UserModel;

public class UserManager extends BaseFireStoreManager {
    private static final String COLLECTION_USERS = "users";
    private static final String TAG = "UserManager";

    public UserManager() {
        super(COLLECTION_USERS);
    }

    public void saveUser(UserModel user, FireStoreCallback callback) {
        if (user == null) {
            if (callback != null) callback.onFailure("User data is null");
            return;
        }

        db.collection(collectionName)
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved successfully: " + user.getUserId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });

    }

    public void getUser(String uid, FireStoreCallbackWthData<UserModel> callback) {
        db.collection(collectionName)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (callback != null) callback.onSuccess(user);
                    } else {
                        if (callback != null) callback.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void updateUserFullName(String uid, String fullName, FireStoreCallback callback) {
        db.collection(collectionName)
            .document(uid)
            .update("fullName", fullName)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e.getMessage());
            });
    }

    public void updateUserPhone(String uid, String phone, FireStoreCallback callback) {
        db.collection(collectionName)
            .document(uid)
            .update("phone", phone)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e.getMessage());
            });
    }

    public void deleteUser(String uid, FireStoreCallback callback) {
        db.collection(collectionName)
            .document(uid)
            .delete()
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e.getMessage());
            });
    }
}
