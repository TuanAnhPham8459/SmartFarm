// AccountFragment.java - Fixed save button state logic
package ca.iot.innovators.smart.farm.profile;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.utils.NetworkUtils;

public class AccountFragment extends Fragment {

    // Views
    private ImageView imgProfile;
    private TextInputEditText etName, etEmail, etPhone, etFarmName, etFarmLocation;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePhoto, btnEdit, btnCancel, btnSaveChanges, btnDeleteAccount;
    private TextInputLayout tilName, tilPhone, tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private LinearLayout layoutSecurity;

    private AccountViewModel viewModel;
    private boolean isEditMode = false;
    private boolean isOffline = false;
    private boolean suppressNetworkToast = false;

    // Debounce handler for validation
    private final Handler validationHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingValidation;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupObservers();
        setupListeners();
        setupImagePicker();

        viewModel.loadUserData();
        checkNetworkStatus();
    }

    private void initViews(View view) {
        imgProfile = view.findViewById(R.id.imgProfile);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etFarmName = view.findViewById(R.id.etFarmName);
        etFarmLocation = view.findViewById(R.id.etLocation);
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        layoutSecurity = view.findViewById(R.id.layoutSecurity);

        tilName = view.findViewById(R.id.tilName);
        tilPhone = view.findViewById(R.id.tilPhone);
        tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword);
        tilNewPassword = view.findViewById(R.id.tilNewPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);

        setEditMode(false);
    }

    private void initViewModel() {
        Application application = requireActivity().getApplication();
        AccountViewModelFactory factory = new AccountViewModelFactory(application);
        viewModel = new ViewModelProvider(this, factory).get(AccountViewModel.class);
    }

    private void checkNetworkStatus() {
        boolean wasOffline = isOffline;
        isOffline = !NetworkUtils.isNetworkAvailable(requireContext());
        viewModel.setOnlineStatus(!isOffline);

        updateEditButtonState();

        if (!suppressNetworkToast && isOffline != wasOffline) {
            String message = isOffline ? "You are offline. Edit mode is disabled." : "Back online. Edit mode enabled.";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        suppressNetworkToast = false;
    }

    private void updateEditButtonState() {
        if (!isEditMode) {
            btnEdit.setEnabled(!isOffline);
            btnEdit.setAlpha(isOffline ? 0.5f : 1.0f);
        }
    }

    private void setupObservers() {
        // User data observer - only updates UI, no validation
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) updateUI(user);
        });

        // Error observer
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && !error.contains("offline") && !error.contains("Using cached")) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Success observer
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();

                if (message.contains("Password") || message.contains("Profile") || message.contains("photo")) {
                    viewModel.loadUserData();
                    if (isEditMode) {
                        requireView().postDelayed(this::toggleEditMode, 300);
                    }
                }
            }
        });

        // Loading state observer
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            btnSaveChanges.setEnabled(!loading && isEditMode && !isOffline && canSave());
            btnEdit.setEnabled(!loading && !isOffline && !isEditMode);
            btnCancel.setEnabled(!loading);
            btnChangePhoto.setEnabled(!loading && isEditMode);
            btnSaveChanges.setText(loading ? "Saving..." : "Save Changes");
        });

        // Validation error observers - only update when in edit mode
        viewModel.getFullNameError().observe(getViewLifecycleOwner(), error -> {
            if (isEditMode) {
                tilName.setError(error);
                updateSaveButtonState();
            }
        });
        viewModel.getPhoneError().observe(getViewLifecycleOwner(), error -> {
            if (isEditMode) {
                tilPhone.setError(error);
                updateSaveButtonState();
            }
        });
        viewModel.getCurrentPasswordError().observe(getViewLifecycleOwner(), error -> {
            if (isEditMode) {
                tilCurrentPassword.setError(error);
                updateSaveButtonState();
            }
        });
        viewModel.getPasswordError().observe(getViewLifecycleOwner(), error -> {
            if (isEditMode) {
                tilNewPassword.setError(error);
                updateSaveButtonState();
            }
        });
        viewModel.getConfirmPasswordError().observe(getViewLifecycleOwner(), error -> {
            if (isEditMode) {
                tilConfirmPassword.setError(error);
                updateSaveButtonState();
            }
        });

        // Combined observer for save button state
        viewModel.getHasAnyChanges().observe(getViewLifecycleOwner(), hasChanges -> {
            if (isEditMode) updateSaveButtonState();
        });

        // Observe form validity
        viewModel.getIsProfileFormValid().observe(getViewLifecycleOwner(), isValid -> {
            if (isEditMode) updateSaveButtonState();
        });
        viewModel.getIsPasswordFormValid().observe(getViewLifecycleOwner(), isValid -> {
            if (isEditMode) updateSaveButtonState();
        });

        // Observe account deletion
        viewModel.getAccountDeleted().observe(getViewLifecycleOwner(), deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                navigateToLogin();
            }
        });
    }

    private void updateUI(UserModel user) {
        etName.setText(user.getFullName() != null ? user.getFullName() : "");
        etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        etPhone.setText(user.getPhone() != null ? user.getPhone() : "");
        etFarmName.setText(user.getFarmName() != null ? user.getFarmName() : "");
        etFarmLocation.setText(user.getFarmLocation() != null ? user.getFarmLocation() : "");

        if (user.getProfileImageUrl() != null) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.account)
                    .error(R.drawable.account)
                    .circleCrop()
                    .into(imgProfile);
        }
    }

    private void setupListeners() {
        setupDebouncedFieldListeners();
        setupButtonListeners();
    }

    private void setupDebouncedFieldListeners() {
        // Name field with debounced validation
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isEditMode) {
                viewModel.markFullNameTouched();
                viewModel.setFullName(etName.getText().toString().trim());
            }
        });
        addDebouncedTextWatcher(etName, viewModel::setFullName);

        // Phone field with debounced validation
        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isEditMode) {
                viewModel.markPhoneTouched();
                viewModel.setPhone(etPhone.getText().toString().trim());
            }
        });
        addDebouncedTextWatcher(etPhone, viewModel::setPhone);

        // Farm name - less frequent validation needed
        addDebouncedTextWatcher(etFarmName, viewModel::setFarmName);

        // Farm location - less frequent validation needed
        addDebouncedTextWatcher(etFarmLocation, viewModel::setFarmLocation);

        // Password fields with debounced validation
        setupPasswordFieldListeners();
    }

    private void setupPasswordFieldListeners() {
        // Current password
        etCurrentPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isEditMode) {
                viewModel.markCurrentPasswordTouched();
                viewModel.setCurrentPassword(etCurrentPassword.getText().toString().trim());
            }
        });
        addDebouncedTextWatcher(etCurrentPassword, viewModel::setCurrentPassword);

        // New password
        etNewPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isEditMode) {
                viewModel.markNewPasswordTouched();
                viewModel.setNewPassword(etNewPassword.getText().toString().trim());
            }
        });
        addDebouncedTextWatcher(etNewPassword, viewModel::setNewPassword);

        // Confirm password
        etConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isEditMode) {
                viewModel.markConfirmPasswordTouched();
                viewModel.setConfirmPassword(etConfirmPassword.getText().toString().trim());
            }
        });
        addDebouncedTextWatcher(etConfirmPassword, viewModel::setConfirmPassword);
    }

    private void addDebouncedTextWatcher(TextInputEditText editText, java.util.function.Consumer<String> setter) {
        editText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isEditMode) return;

                // Cancel pending validation
                if (pendingValidation != null) {
                    validationHandler.removeCallbacks(pendingValidation);
                }

                // Schedule validation with debounce delay (300ms)
                pendingValidation = () -> {
                    String value = s != null ? s.toString().trim() : "";
                    setter.accept(value);
                    pendingValidation = null;
                };
                validationHandler.postDelayed(pendingValidation, 300);
            }
        });
    }

    private void setupButtonListeners() {
        btnChangePhoto.setOnClickListener(v -> {
            if (isEditMode) openImagePicker();
        });

        btnEdit.setOnClickListener(v -> {
            if (!isEditMode) {
                if (isOffline) {
                    Toast.makeText(getContext(), "Cannot edit while offline. Connect to internet.", Toast.LENGTH_LONG).show();
                    return;
                }
                toggleEditMode();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (isEditMode) {
                // Cancel pending validation
                if (pendingValidation != null) {
                    validationHandler.removeCallbacks(pendingValidation);
                    pendingValidation = null;
                }

                viewModel.resetToOriginalValues();
                viewModel.clearMessages();
                viewModel.clearPasswordFields();
                clearPasswordFields();
                toggleEditMode();
                Toast.makeText(getContext(), "Changes cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveChanges.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(getContext(), "No internet connection.", Toast.LENGTH_LONG).show();
                return;
            }

            // Cancel pending validation
            if (pendingValidation != null) {
                validationHandler.removeCallbacks(pendingValidation);
                pendingValidation = null;
            }

            updateViewModelWithFormValues();

            if (viewModel.hasProfileChanges()) {
                viewModel.saveUserData();
            } else if (viewModel.hasPasswordChanges()) {
                viewModel.changePassword();
            } else {
                Toast.makeText(getContext(), "No changes to save", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showDeleteAccountDialog() {
        if (isOffline) {
            Toast.makeText(getContext(), "Cannot delete account while offline.", Toast.LENGTH_LONG).show();
            return;
        }

        Boolean isGoogle = viewModel.getIsGoogleUser().getValue();
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Account")
               .setMessage("This action is permanent and will delete all your data including sensor history and device links. Are you sure?");

        if (Boolean.TRUE.equals(isGoogle)) {
            builder.setPositiveButton("Delete Permanently", (dialog, which) -> viewModel.deleteAccount(null));
        } else {
            // Add password verification for email users
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 20, 60, 0);

            TextInputLayout tilPwd = new TextInputLayout(requireContext());
            tilPwd.setHint("Confirm with Password");
            tilPwd.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

            TextInputEditText etPwd = new TextInputEditText(requireContext());
            etPwd.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            tilPwd.addView(etPwd);
            layout.addView(tilPwd);
            builder.setView(layout);

            builder.setPositiveButton("Verify & Delete", (dialog, which) -> {
                String pwd = etPwd.getText().toString().trim();
                if (pwd.isEmpty()) {
                    Toast.makeText(getContext(), "Password is required to delete account.", Toast.LENGTH_SHORT).show();
                } else {
                    viewModel.deleteAccount(pwd);
                }
            });
        }

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireActivity(), ca.iot.innovators.smart.farm.login.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void updateViewModelWithFormValues() {
        if (viewModel.hasProfileChanges()) {
            viewModel.setFullName(etName.getText().toString().trim());
            viewModel.setPhone(etPhone.getText().toString().trim());
            viewModel.setFarmName(etFarmName.getText().toString().trim());
            viewModel.setFarmLocation(etFarmLocation.getText().toString().trim());
        }

        if (viewModel.hasPasswordChanges()) {
            viewModel.setCurrentPassword(etCurrentPassword.getText().toString().trim());
            viewModel.setNewPassword(etNewPassword.getText().toString().trim());
            viewModel.setConfirmPassword(etConfirmPassword.getText().toString().trim());
        }
    }

    private void clearPasswordFields() {
        etCurrentPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");
        viewModel.setCurrentPassword("");
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        setEditMode(isEditMode);

        if (isEditMode) {
            enterEditMode();
        } else {
            exitEditMode();
        }
    }

    private void enterEditMode() {
        btnEdit.setVisibility(View.GONE);
        btnCancel.setVisibility(View.VISIBLE);
        btnSaveChanges.setVisibility(View.VISIBLE);
        btnChangePhoto.setVisibility(View.VISIBLE);

        Boolean isGoogle = viewModel.getIsGoogleUser().getValue();
        layoutSecurity.setVisibility(Boolean.TRUE.equals(isGoogle) ? View.GONE : View.VISIBLE);

        viewModel.clearPasswordFields();
        viewModel.resetPasswordTouchState();
        clearPasswordFields();
        btnSaveChanges.setEnabled(false);

        if (isOffline) {
            Toast.makeText(getContext(), "Cannot edit while offline.", Toast.LENGTH_SHORT).show();
            // Revert edit mode
            isEditMode = false;
            setEditMode(false);
            btnEdit.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.GONE);
            btnSaveChanges.setVisibility(View.GONE);
            btnChangePhoto.setVisibility(View.GONE);
            layoutSecurity.setVisibility(View.GONE);
        }
    }

    private void exitEditMode() {
        btnEdit.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.GONE);
        btnSaveChanges.setVisibility(View.GONE);
        btnChangePhoto.setVisibility(View.GONE);
        layoutSecurity.setVisibility(View.GONE);

        viewModel.loadUserData();
        viewModel.clearMessages();
        viewModel.clearPasswordFields();
        clearPasswordFields();
        btnSaveChanges.setEnabled(false);

        suppressNetworkToast = true;
        checkNetworkStatus();
    }

    private void setEditMode(boolean enabled) {
        etName.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etFarmName.setEnabled(enabled);
        etFarmLocation.setEnabled(enabled);
        etEmail.setEnabled(false);

        btnChangePhoto.setVisibility(enabled ? View.VISIBLE : View.GONE);
        btnChangePhoto.setEnabled(enabled);

        if (!enabled) {
            clearErrorStates();
        }
    }

    private void clearErrorStates() {
        tilName.setError(null);
        tilPhone.setError(null);
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private boolean canSave() {
        if (!isEditMode || isOffline) return false;

        Boolean hasChanges = viewModel.getHasAnyChanges().getValue();
        if (!Boolean.TRUE.equals(hasChanges)) return false;

        // Check profile validity if there are profile changes
        if (viewModel.hasProfileChanges()) {
            Boolean isProfileValid = viewModel.getIsProfileFormValid().getValue();
            if (!Boolean.TRUE.equals(isProfileValid)) return false;
        }

        // Check password validity if there are password changes
        if (viewModel.hasPasswordChanges()) {
            Boolean isPasswordValid = viewModel.getIsPasswordFormValid().getValue();
            return Boolean.TRUE.equals(isPasswordValid);
        }

        return true;
    }

    private void updateSaveButtonState() {
        if (!isEditMode) {
            btnSaveChanges.setEnabled(false);
            return;
        }

        Boolean isLoading = viewModel.getIsLoading().getValue();
        boolean notLoading = !Boolean.TRUE.equals(isLoading);

        btnSaveChanges.setEnabled(canSave() && notLoading);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).circleCrop().into(imgProfile);
                            viewModel.uploadProfileImage(selectedImageUri);
                        }
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        suppressNetworkToast = true;
        checkNetworkStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up pending validation
        if (pendingValidation != null) {
            validationHandler.removeCallbacks(pendingValidation);
            pendingValidation = null;
        }
    }

    // Helper TextWatcher
    private static abstract class SimpleTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
}