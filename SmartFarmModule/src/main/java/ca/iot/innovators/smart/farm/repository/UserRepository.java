// UserRepository.java
package ca.iot.innovators.smart.farm.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
// REMOVE this import - it's from crashlytics buildtools
// import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.gson.Gson;
// ADD this import - it's the standard Gson TypeToken
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import ca.iot.innovators.smart.farm.profile.UserModel;
import ca.iot.innovators.smart.farm.firestore.BaseFireStoreManager;
import ca.iot.innovators.smart.farm.firestore.UserManager;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String PREF_NAME = "user_profile_cache";
    private static final String KEY_USER_DATA = "user_data";
    private static final String KEY_USER_ID = "user_id";

    private final FirebaseAuth firebaseAuth;
    private final UserManager userManager;
    private final FirebaseStorage firebaseStorage;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    private final MutableLiveData<UserModel> userLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private final MutableLiveData<String> successMessageLiveData;
    private final MutableLiveData<Boolean> isFromCacheLiveData;

    public UserRepository(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userManager = new UserManager();
        this.firebaseStorage = FirebaseStorage.getInstance();
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();

        this.userLiveData = new MutableLiveData<>();
        this.errorLiveData = new MutableLiveData<>();
        this.isLoadingLiveData = new MutableLiveData<>();
        this.successMessageLiveData = new MutableLiveData<>();
        this.isFromCacheLiveData = new MutableLiveData<>(false);
    }

    public LiveData<UserModel> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getSuccessMessageLiveData() {
        return successMessageLiveData;
    }

    public LiveData<Boolean> getIsFromCacheLiveData() {
        return isFromCacheLiveData;
    }

    public void loadUserData() {
        isLoadingLiveData.setValue(true);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            // Try to load from cache even if user is null
            UserModel cachedUser = loadFromCache();
            if (cachedUser != null) {
                userLiveData.setValue(cachedUser);
                isFromCacheLiveData.setValue(true);
                errorLiveData.setValue("Using cached profile (offline mode)");
            } else {
                errorLiveData.setValue("User not authenticated");
            }
            isLoadingLiveData.setValue(false);
            return;
        }

        // Load cached user data - make it effectively final
        final UserModel cachedUser = loadFromCache();
        final String currentUserId = currentUser.getUid();

        // If cached user exists and matches current user, show it immediately
        if (cachedUser != null && cachedUser.getUserId().equals(currentUserId)) {
            userLiveData.setValue(cachedUser);
            isFromCacheLiveData.setValue(true);
        }

        // Create base user from Auth
        UserModel user = new UserModel();
        user.setUserId(currentUserId);
        user.setEmail(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        user.setFullName(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");

        if (currentUser.getPhotoUrl() != null) {
            user.setProfileImageUrl(currentUser.getPhotoUrl().toString());
        }

        // Merge with cached data if available
        if (cachedUser != null && cachedUser.getUserId().equals(currentUserId)) {
            // Keep cached values as fallback
            if (user.getFullName().isEmpty()) {
                user.setFullName(cachedUser.getFullName());
            }
            if (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty()) {
                user.setProfileImageUrl(cachedUser.getProfileImageUrl());
            }
            user.setPhone(cachedUser.getPhone() != null ? cachedUser.getPhone() : "");
            user.setFarmName(cachedUser.getFarmName() != null ? cachedUser.getFarmName() : "");
            user.setFarmLocation(cachedUser.getFarmLocation() != null ? cachedUser.getFarmLocation() : "");
        }

        // Make user effectively final for the callback
        final UserModel finalUser = user;
        final UserModel finalCachedUser = cachedUser;
        final String finalCurrentUserId = currentUserId;

        // Get additional user data from Firestore
        userManager.getUser(currentUserId, new BaseFireStoreManager.FireStoreCallbackWthData<UserModel>() {
            @Override
            public void onSuccess(UserModel firestoreUser) {
                if (firestoreUser != null) {
                    // Merge Firestore data (Firestore takes precedence)
                    finalUser.setPhone(firestoreUser.getPhone() != null ? firestoreUser.getPhone() : "");
                    finalUser.setFarmName(firestoreUser.getFarmName() != null ? firestoreUser.getFarmName() : "");
                    finalUser.setFarmLocation(firestoreUser.getFarmLocation() != null ? firestoreUser.getFarmLocation() : "");

                    if (firestoreUser.getProfileImageUrl() != null && !firestoreUser.getProfileImageUrl().isEmpty()) {
                        finalUser.setProfileImageUrl(firestoreUser.getProfileImageUrl());
                    }
                }

                // Save to cache
                saveToCache(finalUser);
                userLiveData.setValue(finalUser);
                isFromCacheLiveData.setValue(false);
                isLoadingLiveData.setValue(false);
            }

            @Override
            public void onFailure(String error) {
                // If we already have cached data, use it
                if (finalCachedUser != null && finalCachedUser.getUserId().equals(finalCurrentUserId)) {
                    // Merge any missing data from cached user
                    if (finalUser.getPhone() == null || finalUser.getPhone().isEmpty()) {
                        finalUser.setPhone(finalCachedUser.getPhone());
                    }
                    if (finalUser.getFarmName() == null || finalUser.getFarmName().isEmpty()) {
                        finalUser.setFarmName(finalCachedUser.getFarmName());
                    }
                    if (finalUser.getFarmLocation() == null || finalUser.getFarmLocation().isEmpty()) {
                        finalUser.setFarmLocation(finalCachedUser.getFarmLocation());
                    }

                    userLiveData.setValue(finalUser);
                    isFromCacheLiveData.setValue(true);
                    errorLiveData.setValue("Using cached profile (offline mode): " + error);
                } else {
                    // Still set the basic user data from Auth
                    userLiveData.setValue(finalUser);
                    isFromCacheLiveData.setValue(false);
                    errorLiveData.setValue("Could not load all user data: " + error);
                }
                isLoadingLiveData.setValue(false);
            }
        });
    }

    private void saveToCache(UserModel user) {
        if (user == null) return;

        try {
            String userId = user.getUserId();
            if (userId == null || userId.isEmpty()) return;

            sharedPreferences.edit()
                    .putString(KEY_USER_ID, userId)
                    .putString(KEY_USER_DATA, gson.toJson(user))
                    .apply();

            Log.d(TAG, "User data saved to cache: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save user to cache: " + e.getMessage());
        }
    }

    private UserModel loadFromCache() {
        try {
            String json = sharedPreferences.getString(KEY_USER_DATA, null);
            if (json == null) return null;

            // Now using standard Gson TypeToken
            Type type = new TypeToken<UserModel>() {}.getType();
            UserModel user = gson.fromJson(json, type);

            if (user != null && user.getUserId() != null && !user.getUserId().isEmpty()) {
                Log.d(TAG, "User data loaded from cache: " + user.getUserId());
                return user;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load user from cache: " + e.getMessage());
            return null;
        }
    }

    public void saveUserProfile(UserModel user) {
        isLoadingLiveData.setValue(true);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            errorLiveData.setValue("User not authenticated");
            isLoadingLiveData.setValue(false);
            return;
        }

        // Save to cache immediately for offline use
        saveToCache(user);

        // Update display name in Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(user.getFullName())
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Save complete user data to Firestore
                    user.setUserId(currentUser.getUid());
                    user.setEmail(currentUser.getEmail());

                    userManager.saveUser(user, new BaseFireStoreManager.FireStoreCallback() {
                        @Override
                        public void onSuccess() {
                            // Update cache with latest data
                            saveToCache(user);
                            userLiveData.setValue(user);
                            successMessageLiveData.setValue("Profile updated successfully!");
                            isLoadingLiveData.setValue(false);
                        }

                        @Override
                        public void onFailure(String error) {
                            // Even if Firestore fails, cache is already updated
                            successMessageLiveData.setValue("Profile updated locally (offline mode)");
                            isLoadingLiveData.setValue(false);
                            errorLiveData.setValue("Could not sync to cloud: " + error);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Even if Auth update fails, cache is already updated
                    successMessageLiveData.setValue("Profile updated locally (offline mode)");
                    isLoadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to update display name: " + e.getMessage());
                });
    }

    public void uploadProfileImage(Uri imageUri) {
        isLoadingLiveData.setValue(true);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            errorLiveData.setValue("User not authenticated");
            isLoadingLiveData.setValue(false);
            return;
        }

        // Create a local copy with the new image URL for immediate use
        String localImageUrl = imageUri.toString();
        UserModel currentUserData = userLiveData.getValue();
        if (currentUserData != null) {
            currentUserData.setProfileImageUrl(localImageUrl);
            saveToCache(currentUserData);
            userLiveData.setValue(currentUserData);
        }

        StorageReference storageRef = firebaseStorage.getReference()
                .child("profile_images")
                .child(currentUser.getUid() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateUserProfilePhoto(uri);
                    }).addOnFailureListener(e -> {
                        // Even if download URL fails, local image is cached
                        successMessageLiveData.setValue("Profile photo updated locally (offline mode)");
                        isLoadingLiveData.setValue(false);
                        errorLiveData.setValue("Failed to get download URL: " + e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    // Even if upload fails, local image is cached
                    successMessageLiveData.setValue("Profile photo updated locally (offline mode)");
                    isLoadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to upload image: " + e.getMessage());
                });
    }

    private void updateUserProfilePhoto(Uri photoUri) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            errorLiveData.setValue("User not authenticated");
            isLoadingLiveData.setValue(false);
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUri)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Update user model with new photo URL
                    UserModel currentUserData = userLiveData.getValue();
                    if (currentUserData != null) {
                        currentUserData.setProfileImageUrl(photoUri.toString());
                        saveToCache(currentUserData);
                        userLiveData.setValue(currentUserData);
                    }
                    successMessageLiveData.setValue("Profile photo updated!");
                    isLoadingLiveData.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorLiveData.setValue("Failed to update profile photo: " + e.getMessage());
                    isLoadingLiveData.setValue(false);
                });
    }

    public void changePassword(String currentPassword, String newPassword) {
        isLoadingLiveData.setValue(true);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            errorLiveData.setValue("User not authenticated");
            isLoadingLiveData.setValue(false);
            return;
        }

        if (currentUser.getEmail() == null) {
            errorLiveData.setValue("No email associated with this account");
            isLoadingLiveData.setValue(false);
            return;
        }

        // Re-authenticate user before changing password
        firebaseAuth.signInWithEmailAndPassword(currentUser.getEmail(), currentPassword)
                .addOnSuccessListener(authResult -> {
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> {
                                successMessageLiveData.setValue("Password changed successfully!");
                                isLoadingLiveData.setValue(false);
                            })
                            .addOnFailureListener(e -> {
                                errorLiveData.setValue("Failed to change password: " + e.getMessage());
                                isLoadingLiveData.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    errorLiveData.setValue("Current password is incorrect");
                    isLoadingLiveData.setValue(false);
                });
    }

    public void deleteAccount(String password) {
        isLoadingLiveData.setValue(true);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            errorLiveData.setValue("User not authenticated");
            isLoadingLiveData.setValue(false);
            return;
        }

        String email = currentUser.getEmail();
        String uid = currentUser.getUid();

        // Step 1: Re-authenticate if password is provided
        if (password != null && !password.isEmpty() && email != null) {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> proceedWithDeletion(currentUser, uid))
                    .addOnFailureListener(e -> {
                        errorLiveData.setValue("Incorrect password. Verification failed.");
                        isLoadingLiveData.setValue(false);
                    });
        } else {
            // For Google users or cases where password isn't provided (already handled in UI)
            proceedWithDeletion(currentUser, uid);
        }
    }

    private void proceedWithDeletion(FirebaseUser currentUser, String uid) {
        // Step 2: Delete Firestore Document
        userManager.deleteUser(uid, new BaseFireStoreManager.FireStoreCallback() {
            @Override
            public void onSuccess() {
                // Step 3: Delete Auth Account
                currentUser.delete()
                        .addOnSuccessListener(aVoid -> {
                            clearCache();
                            successMessageLiveData.setValue("Account permanently deleted.");
                            isLoadingLiveData.setValue(false);
                        })
                        .addOnFailureListener(e -> {
                            errorLiveData.setValue("Failed to delete account: " + e.getMessage());
                            isLoadingLiveData.setValue(false);
                        });
            }

            @Override
            public void onFailure(String error) {
                errorLiveData.setValue("Failed to remove data: " + error);
                isLoadingLiveData.setValue(false);
            }
        });
    }

    public void clearMessages() {
        successMessageLiveData.setValue(null);
        errorLiveData.setValue(null);
        isFromCacheLiveData.setValue(false);
    }

    public void clearCache() {
        sharedPreferences.edit().clear().apply();
        Log.d(TAG, "User cache cleared");
    }
}