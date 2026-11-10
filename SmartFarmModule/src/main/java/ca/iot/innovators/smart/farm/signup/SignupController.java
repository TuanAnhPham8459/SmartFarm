/** CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/
package ca.iot.innovators.smart.farm.signup;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

import ca.iot.innovators.smart.farm.auth.FirebaseAuthManager;
import ca.iot.innovators.smart.farm.utils.InputChecker;
import ca.iot.innovators.smart.farm.profile.UserModel;
import ca.iot.innovators.smart.farm.firestore.BaseFireStoreManager;
import ca.iot.innovators.smart.farm.firestore.FireStoreService;

public class SignupController implements FirebaseAuthManager.AuthCallback {
    private final SignupView view;
    private final InputChecker inputChecker;
    private final FirebaseAuthManager authManager;
    private final FireStoreService fireStoreService;
    private String signupFullName;
    private String signupPhone;

    public interface SignupView {
        void showLoading(boolean isLoading);
        void showFullNameError(String error);
        void showPhoneError(String err);
        void showEmailError(String err);
        void showPasswordError(String err);
        void showConfirmPasswordError(String err);
        void clearAllErrors();
        void onSignupSuccess();
        void onSignupFailure(String errorMessage);
        void navigateToLogin();
        void navigateToMain();
    }

    public SignupController(SignupView view, Context context) {
        this.view = view;
        this.inputChecker = new InputChecker(context);
        this.authManager = new FirebaseAuthManager(this);
        this.fireStoreService = FireStoreService.getInstance();
    }

    public void handleSignup(String email, String password, String fullName,
                             String phone, String confirmPassword, boolean termsAccepted) {
        view.clearAllErrors();

        if(!termsAccepted) {
            view.onSignupFailure("Please accept the Terms and Conditions");
            return;
        }

        InputChecker.ValidationResult result =
                inputChecker.validateSignup(email, password, fullName, phone, confirmPassword);

        if (!result.isValid()) {
            displayValidationErrors(result);
            return;
        }

        this.signupFullName = fullName;
        this.signupPhone = phone;

        view.showLoading(true);
        authManager.signUp(email, password);
    }

    private void displayValidationErrors(InputChecker.ValidationResult result) {
        Map<String, String> errors = result.getError();

        for (Map.Entry<String, String> entry : errors.entrySet()) {
            String field = entry.getKey();
            String error = entry.getValue();

            switch (field) {
                case InputChecker.FIELD_EMAIL:
                    view.showEmailError(error);
                    break;
                case InputChecker.FIELD_PASSWORD:
                    view.showPasswordError(error);
                    break;
                case InputChecker.FIELD_FULL_NAME:
                    view.showFullNameError(error);
                    break;
                case InputChecker.FIELD_PHONE:
                    view.showPhoneError(error);
                    break;
                case InputChecker.FIELD_CONFIRM_PASSWORD:
                    view.showConfirmPasswordError(error);
                    break;
                default:
                    view.onSignupFailure(error);
                    break;
            }
        }
    }

    @Override
    public void onSuccess(FirebaseUser user) {
        view.showLoading(false);

        if (user != null) {
            UserModel userModel = new UserModel(
                    user.getUid(),
                    user.getEmail(),
                    signupFullName,
                    signupPhone,
                    "", ""
            );
            view.showLoading(true);

            fireStoreService.getUserManager().saveUser(userModel, new BaseFireStoreManager.FireStoreCallback() {
                @Override
                public void onSuccess() {
                    view.showLoading(false);
                    view.onSignupSuccess();
                }

                @Override
                public void onFailure(String errorMessage) {
                    view.showLoading(false);

                    user.delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.e("SignupController", "Auth user deleted due to Firestore failure");
                                view.onSignupFailure("Failed to create account. Please check your internet connection and try again.");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("SignupController", "CRITICAL: Auth user exists but FireStore save failed and deletion failed");
                                view.onSignupFailure("Account partially created. Please contact support with this error: " + errorMessage);
                            });
                }
            });
        } else {
            view.onSignupFailure("User authentication failed");
        }
    }

    @Override
    public void onFailure(String errorMessage) {
        view.showLoading(false);
        view.onSignupFailure(errorMessage);
    }

    public void signOut() {
        authManager.signOut();
    }
}
