/**
 * CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/
package ca.iot.innovators.smart.farm.auth;

import android.util.Log;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private final FirebaseAuth mAuth;
    private final AuthCallback callback;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onFailure(String errorMessage);
    }

    public FirebaseAuthManager(AuthCallback callback) {
        this.mAuth = FirebaseAuth.getInstance();
        this.callback = callback;
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Sign in existing user
    public void signInWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "signIn:success");
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.w(TAG, "signIn:failure", task.getException());
                        if (callback != null) {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    public void signInWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    if (callback != null) {
                        callback.onSuccess(result.getUser());
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void signUp(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "signUp:success");
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.w(TAG, "signUp:failure", task.getException());
                        if (callback != null) {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    public void signOut() {
        mAuth.signOut();
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }
}