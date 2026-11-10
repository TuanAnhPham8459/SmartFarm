/** CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/

package ca.iot.innovators.smart.farm.login;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.FirebaseUser;

import ca.iot.innovators.smart.farm.auth.FirebaseAuthManager;
import ca.iot.innovators.smart.farm.utils.InputChecker;
import ca.iot.innovators.smart.farm.R;

public class LoginController implements FirebaseAuthManager.AuthCallback {
    private static final String TAG = "LoginController";
    private final FirebaseAuthManager authManager;
    private final LoginView view;
    private final InputChecker inputChecker;
    private final LoginPreferences loginPreferences;
    private final CredentialManager credentialManager;
    private Context context;

    public interface LoginView {
        void showLoading(boolean isLoading);

        void showEmailError(String err);

        void showPasswordError(String err);

        void clearEmailError();

        void clearPasswordError();

        void onLoginSuccess();

        void onLoginFailure(String errorMessage);

        void navigateToMain();
//        void showGoogleSignInPicker();
//        void showLoginScreen();
    }

    public LoginController(LoginView view, Context context) {
        this.view = view;
        this.context = context;
        this.authManager = new FirebaseAuthManager(this);
        this.inputChecker = new InputChecker(context);
        this.loginPreferences = LoginPreferences.getInstance(context);
        this.credentialManager = CredentialManager.create(context);
    }

    public void onStart() {
        if (loginPreferences.isRememberMe() && authManager.isUserLoggedIn()) {
            view.navigateToMain();
        }
    }

    public void handleLogin(String email, String password, boolean rememberMe) {
        view.clearEmailError();
        view.clearPasswordError();

        loginPreferences.setRememberMe(rememberMe);

        InputChecker.ValidationResult result =
                inputChecker.validateLogin(email, password);
        if (!result.isValid()) {
            if (result.getEmailError() != null) {
                view.showEmailError(result.getEmailError());
            }
            if (result.getPasswordError() != null) {
                view.showPasswordError(result.getPasswordError());
            }
            return;
        }

        view.showLoading(true);
        authManager.signInWithEmailAndPassword(email, password);
    }

    public void handleGoogleSignIn(boolean rememberMe) {
        loginPreferences.setRememberMe(rememberMe);
        view.showLoading(true);
        performGoogleSignIn();
    }

    private void performGoogleSignIn() {
        String nonce = java.util.UUID.randomUUID().toString();

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setNonce(nonce)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Log.d(TAG, "Launching Google Sign-In picker...");
        credentialManager.getCredentialAsync(
                context,
                request,
                null,
                context.getMainExecutor(),
                new CredentialManagerCallback<>() {

                    @Override
                    public void onResult(@NonNull GetCredentialResponse response) {
                        Log.d(TAG, "Credential result received");
                        handleCredentialResponse(response);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Credential error received: " + e.getMessage());
                        handleCredentialError(e);
                    }
                }
        );
    }



    private void handleCredentialResponse(GetCredentialResponse response) {
        Log.d(TAG, "Handling credential response...");

        if (!(response.getCredential() instanceof CustomCredential)) {
            Log.e(TAG, "Unexpected credential class: " + response.getCredential().getClass().getName());
            view.showLoading(false);
            view.onLoginFailure("Invalid credential.");
            return;
        }


        CustomCredential credential = (CustomCredential) response.getCredential();
        Log.d(TAG, "Credential type: " + credential.getType());

        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            Log.e(TAG, "Unsupported credential type: " + credential.getType());
            view.showLoading(false);
            view.onLoginFailure("Unsupported credential.");
            return;
        }

        try {
            GoogleIdTokenCredential googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.getData());

            String idToken = googleIdTokenCredential.getIdToken();
            Log.d(TAG, "Google ID Token extracted successfully");

            authManager.signInWithGoogle(idToken);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing GoogleIdTokenCredential", e);
            view.showLoading(false);
            view.onLoginFailure("Unable to read Google account.");
        }
    }

    private void handleCredentialError(GetCredentialException e) {
        view.showLoading(false);

        // 1. Handle User Cancellation (Silent for production, but adding feedback for debugging)
        if (e instanceof androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Sign-In");
            view.onLoginFailure("Sign-in cancelled."); // Feedback for debugging
            return;
        }

        String errorMessage;

        // 2. Specific Exception Handling
        if (e instanceof androidx.credentials.exceptions.GetCredentialProviderConfigurationException) {
            errorMessage = "Authentication configuration error. Please contact support.";
            Log.e(TAG, "Credential Config Error: Check SHA-1/Client ID", e);
        } else if (e instanceof androidx.credentials.exceptions.NoCredentialException) {
            errorMessage = "No Google accounts found or sign-in not configured for this device.";
        } else {
            errorMessage = e.getMessage() != null ? e.getMessage() : "Google Sign-In failed.";
        }

        view.onLoginFailure(errorMessage);

        // 3. Use class name instead of restricted getType()
        Log.e(TAG, "Credential Error: " + e.getClass().getSimpleName(), e);
    }

    @Override
    public void onSuccess(FirebaseUser user) {
        view.showLoading(false);
        view.onLoginSuccess();
    }

    @Override
    public void onFailure(String errorMessage) {
        view.showLoading(false);
        view.onLoginFailure(errorMessage);
    }

    public boolean isRememberMe() {
        return loginPreferences.isRememberMe();
    }

    public void signOut() {
        loginPreferences.clear();
        authManager.signOut();
    }
}
