// AccountViewModel.java - Performance Optimized
package ca.iot.innovators.smart.farm.profile;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import ca.iot.innovators.smart.farm.utils.InputChecker;
import ca.iot.innovators.smart.farm.repository.UserRepository;

public class AccountViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final InputChecker inputChecker;
    private final FirebaseAuth firebaseAuth;

    // Form fields
    private final MutableLiveData<String> fullName = new MutableLiveData<>("");
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final MutableLiveData<String> phone = new MutableLiveData<>("");
    private final MutableLiveData<String> farmName = new MutableLiveData<>("");
    private final MutableLiveData<String> farmLocation = new MutableLiveData<>("");
    private final MutableLiveData<String> profileImageUrl = new MutableLiveData<>("");

    // Original values
    private String originalFullName = "";
    private String originalPhone = "";
    private String originalFarmName = "";
    private String originalFarmLocation = "";

    // Password fields
    private final MutableLiveData<String> currentPassword = new MutableLiveData<>("");
    private final MutableLiveData<String> newPassword = new MutableLiveData<>("");
    private final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    // Validation errors
    private final MutableLiveData<String> fullNameError = new MutableLiveData<>();
    private final MutableLiveData<String> phoneError = new MutableLiveData<>();
    private final MutableLiveData<String> currentPasswordError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<String> confirmPasswordError = new MutableLiveData<>();

    // UI state - reduced number of LiveData to minimize observer callbacks
    private final MutableLiveData<Boolean> isProfileFormValid = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isPasswordFormValid = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasProfileChanges = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasPasswordChanges = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasAnyChanges = new MutableLiveData<>(false);

    // Touch state
    private boolean isFullNameTouched = false;
    private boolean isPhoneTouched = false;
    private boolean isCurrentPasswordTouched = false;
    private boolean isNewPasswordTouched = false;
    private boolean isConfirmPasswordTouched = false;

    private final MutableLiveData<Boolean> isGoogleUser = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isOnline = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> accountDeleted = new MutableLiveData<>(false);

    // Cache for validation results to avoid redundant computation
    private InputChecker.ValidationResult cachedProfileResult;
    private InputChecker.ValidationResult cachedPasswordResult;
    private String cachedFullName = "";
    private String cachedPhone = "";
    private String cachedCurrentPwd = "";
    private String cachedNewPwd = "";
    private String cachedConfirmPwd = "";

    public AccountViewModel(Application application) {
        super(application);
        this.userRepository = new UserRepository(application.getApplicationContext());
        this.inputChecker = new InputChecker(application.getApplicationContext());
        this.firebaseAuth = FirebaseAuth.getInstance();

        userRepository.getUserLiveData().observeForever(user -> {
            if (user != null) updateFormFields(user);
        });

        userRepository.getIsFromCacheLiveData().observeForever(isFromCache -> {
            if (Boolean.TRUE.equals(isFromCache)) isOnline.setValue(false);
        });

        userRepository.getSuccessMessageLiveData().observeForever(message -> {
            if (message != null && message.contains("deleted")) {
                accountDeleted.setValue(true);
            }
        });

        checkProviderType();
    }

    private void checkProviderType() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && currentUser.getProviderData() != null) {
            for (com.google.firebase.auth.UserInfo profile : currentUser.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                    isGoogleUser.setValue(true);
                    return;
                }
            }
        }
        isGoogleUser.setValue(false);
    }

    private void updateFormFields(UserModel user) {
        originalFullName = user.getFullName() != null ? user.getFullName() : "";
        originalPhone = user.getPhone() != null ? user.getPhone() : "";
        originalFarmName = user.getFarmName() != null ? user.getFarmName() : "";
        originalFarmLocation = user.getFarmLocation() != null ? user.getFarmLocation() : "";

        fullName.setValue(originalFullName);
        email.setValue(user.getEmail() != null ? user.getEmail() : "");
        phone.setValue(originalPhone);
        farmName.setValue(originalFarmName);
        farmLocation.setValue(originalFarmLocation);
        profileImageUrl.setValue(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "");

        hasProfileChanges.setValue(false);
        updateHasAnyChanges();
    }

    // Getters
    public LiveData<UserModel> getUser() { return userRepository.getUserLiveData(); }
    public LiveData<Boolean> getIsGoogleUser() { return isGoogleUser; }
    public LiveData<String> getError() { return userRepository.getErrorLiveData(); }
    public LiveData<Boolean> getIsLoading() { return userRepository.getIsLoadingLiveData(); }
    public LiveData<String> getSuccessMessage() { return userRepository.getSuccessMessageLiveData(); }

    public LiveData<String> getFullName() { return fullName; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<String> getPhone() { return phone; }
    public LiveData<String> getFarmName() { return farmName; }
    public LiveData<String> getFarmLocation() { return farmLocation; }
    public LiveData<String> getProfileImageUrl() { return profileImageUrl; }
    public LiveData<String> getCurrentPassword() { return currentPassword; }
    public LiveData<String> getNewPassword() { return newPassword; }
    public LiveData<String> getConfirmPassword() { return confirmPassword; }

    public LiveData<String> getFullNameError() { return fullNameError; }
    public LiveData<String> getPhoneError() { return phoneError; }
    public LiveData<String> getCurrentPasswordError() { return currentPasswordError; }
    public LiveData<String> getPasswordError() { return passwordError; }
    public LiveData<String> getConfirmPasswordError() { return confirmPasswordError; }

    public LiveData<Boolean> getIsProfileFormValid() { return isProfileFormValid; }
    public LiveData<Boolean> getIsPasswordFormValid() { return isPasswordFormValid; }
    public LiveData<Boolean> getHasProfileChanges() { return hasProfileChanges; }
    public LiveData<Boolean> getHasPasswordChanges() { return hasPasswordChanges; }
    public LiveData<Boolean> getHasAnyChanges() { return hasAnyChanges; }
    public LiveData<Boolean> getAccountDeleted() { return accountDeleted; }

    // Setters with validation - optimized to avoid redundant computation
    public void setFullName(String value) {
        if (value.equals(getString(fullName))) return; // No change
        fullName.setValue(value);
        isFullNameTouched = true;
        validateProfileForm();
        checkProfileChanges();
        updateHasAnyChanges();
    }

    public void setPhone(String value) {
        if (value.equals(getString(phone))) return; // No change
        phone.setValue(value);
        isPhoneTouched = true;
        validateProfileForm();
        checkProfileChanges();
        updateHasAnyChanges();
    }

    public void setFarmName(String value) {
        String current = getString(farmName);
        if (value.equals(current)) return;
        farmName.setValue(value);
        checkProfileChanges();
        updateHasAnyChanges();
    }

    public void setFarmLocation(String value) {
        String current = getString(farmLocation);
        if (value.equals(current)) return;
        farmLocation.setValue(value);
        checkProfileChanges();
        updateHasAnyChanges();
    }

    public void setCurrentPassword(String value) {
        if (value.equals(getString(currentPassword))) return;
        currentPassword.setValue(value);
        if (value != null && !value.isEmpty()) isCurrentPasswordTouched = true;
        validatePasswordForm();
        checkPasswordChanges();
        updateHasAnyChanges();
    }

    public void setNewPassword(String value) {
        if (value.equals(getString(newPassword))) return;
        newPassword.setValue(value);
        if (value != null && !value.isEmpty()) isNewPasswordTouched = true;
        validatePasswordForm();
        checkPasswordChanges();
        updateHasAnyChanges();
    }

    public void setConfirmPassword(String value) {
        if (value.equals(getString(confirmPassword))) return;
        confirmPassword.setValue(value);
        if (value != null && !value.isEmpty()) isConfirmPasswordTouched = true;
        validatePasswordForm();
        checkPasswordChanges();
        updateHasAnyChanges();
    }

    private void validateProfileForm() {
        String name = getString(fullName);
        String phoneVal = getString(phone);

        // Use cached result if values haven't changed
        if (cachedProfileResult != null &&
                name.equals(cachedFullName) &&
                phoneVal.equals(cachedPhone)) {
            return; // No change, skip validation
        }

        cachedFullName = name;
        cachedPhone = phoneVal;
        cachedProfileResult = inputChecker.validateProfile(name, phoneVal);

        fullNameError.setValue(isFullNameTouched ? cachedProfileResult.getFullNameError() : null);
        phoneError.setValue(isPhoneTouched ? cachedProfileResult.getPhoneError() : null);

        // Form valid if no errors on touched fields
        boolean isNameValid = !isFullNameTouched || cachedProfileResult.getFullNameError() == null;
        boolean isPhoneValid = !isPhoneTouched || cachedProfileResult.getPhoneError() == null;
        isProfileFormValid.setValue(isNameValid && isPhoneValid);
    }

    private void validatePasswordForm() {
        if (Boolean.TRUE.equals(isGoogleUser.getValue())) {
            isPasswordFormValid.setValue(true);
            clearPasswordErrors();
            return;
        }

        String currentPwd = getString(currentPassword);
        String newPwd = getString(newPassword);
        String confirmPwd = getString(confirmPassword);

        boolean hasCurrent = !currentPwd.isEmpty();
        boolean hasNew = !newPwd.isEmpty();
        boolean hasConfirm = !confirmPwd.isEmpty();
        boolean hasContent = hasCurrent || hasNew || hasConfirm;

        if (!hasContent && !isAnyPasswordTouched()) {
            clearPasswordErrors();
            isPasswordFormValid.setValue(true);
            return;
        }

        // Use cached result if values haven't changed
        if (cachedPasswordResult != null &&
                currentPwd.equals(cachedCurrentPwd) &&
                newPwd.equals(cachedNewPwd) &&
                confirmPwd.equals(cachedConfirmPwd)) {
            return;
        }

        cachedCurrentPwd = currentPwd;
        cachedNewPwd = newPwd;
        cachedConfirmPwd = confirmPwd;
        cachedPasswordResult = inputChecker.validatePasswordChange(currentPwd, newPwd, confirmPwd);

        // This ensures the current password error appears when new password is filled
        String currentError = cachedPasswordResult.getError("currentPassword");
        String newError = cachedPasswordResult.getError("newPassword");
        String confirmError = cachedPasswordResult.getError("confirmPassword");

        // Show current password error if there's an error AND (field is touched OR has content)
        // OR if the error is about current password being required when new password is filled
        if (currentError != null && (isCurrentPasswordTouched || hasCurrent || hasNew || hasConfirm)) {
            currentPasswordError.setValue(currentError);
        } else {
            currentPasswordError.setValue(null);
        }

        // Show new password error if there's an error AND (field is touched OR has content)
        if (newError != null && (isNewPasswordTouched || hasNew)) {
            passwordError.setValue(newError);
        } else {
            passwordError.setValue(null);
        }

        // Show confirm password error if there's an error AND (field is touched OR has content)
        if (confirmError != null && (isConfirmPasswordTouched || hasConfirm)) {
            confirmPasswordError.setValue(confirmError);
        } else {
            confirmPasswordError.setValue(null);
        }

        // Determine if form is valid
        boolean isValid = true;

        // Check current password - if new password is filled, current password is required
        if (hasNew || hasConfirm) {
            isValid = isValid && (currentError == null);
        } else if (isCurrentPasswordTouched || hasCurrent) {
            isValid = isValid && (currentError == null);
        }

        // Check new password
        if (isNewPasswordTouched || hasNew) {
            isValid = isValid && (newError == null);
        }

        // Check confirm password
        if (isConfirmPasswordTouched || hasConfirm) {
            isValid = isValid && (confirmError == null);
        }

        isPasswordFormValid.setValue(isValid);
    }

    private boolean isAnyPasswordTouched() {
        return isCurrentPasswordTouched || isNewPasswordTouched || isConfirmPasswordTouched;
    }

    private void clearPasswordErrors() {
        currentPasswordError.setValue(null);
        passwordError.setValue(null);
        confirmPasswordError.setValue(null);
    }

    private void checkProfileChanges() {
        boolean hasChanges = !getString(fullName).equals(originalFullName) ||
                !getString(phone).equals(originalPhone) ||
                !getString(farmName).equals(originalFarmName) ||
                !getString(farmLocation).equals(originalFarmLocation);
        hasProfileChanges.setValue(hasChanges);
    }

    private void checkPasswordChanges() {
        if (Boolean.TRUE.equals(isGoogleUser.getValue())) {
            hasPasswordChanges.setValue(false);
            return;
        }
        boolean hasChanges = !getString(currentPassword).isEmpty() ||
                !getString(newPassword).isEmpty() ||
                !getString(confirmPassword).isEmpty();
        hasPasswordChanges.setValue(hasChanges);
    }

    private void updateHasAnyChanges() {
        hasAnyChanges.setValue(Boolean.TRUE.equals(hasProfileChanges.getValue()) ||
                Boolean.TRUE.equals(hasPasswordChanges.getValue()));
    }

    private String getString(MutableLiveData<String> liveData) {
        return liveData.getValue() != null ? liveData.getValue() : "";
    }

    public boolean hasProfileChanges() { return Boolean.TRUE.equals(hasProfileChanges.getValue()); }
    public boolean hasPasswordChanges() { return Boolean.TRUE.equals(hasPasswordChanges.getValue()); }
    public boolean hasAnyChanges() { return Boolean.TRUE.equals(hasAnyChanges.getValue()); }

    public void loadUserData() {
        userRepository.loadUserData();
        checkProviderType();
    }

    public void saveUserData() {
        validateProfileForm();
        if (!Boolean.TRUE.equals(isProfileFormValid.getValue())) {
            return;
        }

        UserModel user = new UserModel();
        user.setFullName(fullName.getValue());
        user.setPhone(phone.getValue());
        user.setFarmName(farmName.getValue());
        user.setFarmLocation(farmLocation.getValue());
        user.setProfileImageUrl(profileImageUrl.getValue());
        userRepository.saveUserProfile(user);
    }

    public void uploadProfileImage(Uri imageUri) {
        if (imageUri != null) userRepository.uploadProfileImage(imageUri);
    }

    public void changePassword() {
        if (Boolean.TRUE.equals(isGoogleUser.getValue())) return;

        validatePasswordForm();
        if (!Boolean.TRUE.equals(isPasswordFormValid.getValue())) return;

        String currentPwd = currentPassword.getValue();
        String newPwd = newPassword.getValue();
        if (currentPwd != null && !currentPwd.isEmpty() && newPwd != null && !newPwd.isEmpty()) {
            userRepository.changePassword(currentPwd, newPwd);
        }
    }

    public void deleteAccount(String password) {
        userRepository.deleteAccount(password);
    }

    public void setOnlineStatus(boolean isOnline) { this.isOnline.setValue(isOnline); }

    public void clearMessages() {
        userRepository.clearMessages();
        fullNameError.setValue(null);
        phoneError.setValue(null);
        clearPasswordErrors();
        // Clear cache
        cachedProfileResult = null;
        cachedPasswordResult = null;
    }

    public void clearPasswordFields() {
        currentPassword.setValue("");
        newPassword.setValue("");
        confirmPassword.setValue("");
        isCurrentPasswordTouched = false;
        isNewPasswordTouched = false;
        isConfirmPasswordTouched = false;
        clearPasswordErrors();
        isPasswordFormValid.setValue(false);
        hasPasswordChanges.setValue(false);
        updateHasAnyChanges();
        // Clear password cache
        cachedPasswordResult = null;
        cachedCurrentPwd = "";
        cachedNewPwd = "";
        cachedConfirmPwd = "";
    }

    public void resetPasswordTouchState() {
        isCurrentPasswordTouched = false;
        isNewPasswordTouched = false;
        isConfirmPasswordTouched = false;
        clearPasswordErrors();
        isPasswordFormValid.setValue(true);
        // Clear password cache
        cachedPasswordResult = null;
    }

    public void resetToOriginalValues() {
        fullName.setValue(originalFullName);
        phone.setValue(originalPhone);
        farmName.setValue(originalFarmName);
        farmLocation.setValue(originalFarmLocation);

        isFullNameTouched = false;
        isPhoneTouched = false;
        resetPasswordTouchState();

        hasProfileChanges.setValue(false);
        hasPasswordChanges.setValue(false);
        hasAnyChanges.setValue(false);
        isProfileFormValid.setValue(true);

        // Clear caches
        cachedProfileResult = null;
        cachedPasswordResult = null;
        cachedFullName = "";
        cachedPhone = "";
    }

    // Touch methods
    public void markFullNameTouched() {
        isFullNameTouched = true;
        validateProfileForm();
    }
    public void markPhoneTouched() {
        isPhoneTouched = true;
        validateProfileForm();
    }
    public void markCurrentPasswordTouched() {
        isCurrentPasswordTouched = true;
        validatePasswordForm();
    }
    public void markNewPasswordTouched() {
        isNewPasswordTouched = true;
        validatePasswordForm();
    }
    public void markConfirmPasswordTouched() {
        isConfirmPasswordTouched = true;
        validatePasswordForm();
    }
}