/**
 * CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/

package ca.iot.innovators.smart.farm.utils;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import ca.iot.innovators.smart.farm.R;

public class InputChecker {

    private final Context context;

    // Constants for mapping errors
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_CONFIRM_PASSWORD = "confirmPassword";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_MESSAGE = "message";
    private static final Pattern CONTACT_EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern CONTACT_PHONE_PATTERN = Pattern.compile(
            "^[0-9+()\\-\\s]{7,15}$"
    );

    public InputChecker(Context context) {
        this.context = context;
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final Map<String, String> errors;

        public ValidationResult(boolean isValid) {
            this.isValid = isValid;
            this.errors = new HashMap<>();
        }

        public ValidationResult(boolean isValid, Map<String, String> errors) {
            this.isValid = isValid;
            this.errors = errors != null ? errors : new HashMap<>();
        }

        public boolean isValid() {
            return isValid;
        }

        public String getError(String field) {
            return errors.get(field);
        }

        public Map<String, String> getError() {
            return errors;
        }

        public boolean hasError(String field) {
            return errors.containsKey(field) && errors.get(field) != null;
        }

        public String getEmailError() { return getError(FIELD_EMAIL); }
        public String getPasswordError() { return getError(FIELD_PASSWORD); }
        public String getFullNameError() { return getError(FIELD_FULL_NAME); }
        public String getPhoneError() { return getError(FIELD_PHONE); }
        public String getConfirmPasswordError() { return getError(FIELD_CONFIRM_PASSWORD); }
        public String getNameError() { return getError(FIELD_NAME); }
        public String getMessageError() { return getError(FIELD_MESSAGE); }
    }

    private String validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return context.getString(R.string.email_required);
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return context.getString(R.string.email_invalid);
        }
        return null;
    }

    public String validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return context.getString(R.string.password_required);
        }
        if (password.length() < 9) {
            return context.getString(R.string.password_too_short);
        }

        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])(?!.*\\s).{9,}$";
        if (!password.matches(passwordRegex)) {
            return context.getString(R.string.password_format_invalid);
        }
        return null;
    }

    public String validateFullName(String fullName) {
        if (TextUtils.isEmpty(fullName)) {
            return context.getString(R.string.full_name_required);
        } else if (fullName.length() < 3) {
            return context.getString(R.string.full_name_length);
        }
        return null;
    }

    public String validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return context.getString(R.string.phone_number_required);
        }

        // Remove spaces, hyphens, parentheses for validation
        String cleanPhone = phone.replaceAll("[\\s\\-()]", "");

        // Check for +1 format: +1XXXXXXXXXX (11 digits after +)
        if (cleanPhone.matches("^\\+1\\d{10}$")) {
            return null;
        }

        // Check for simple 10-digit format: XXXXXXXXXX (10 digits)
        if (cleanPhone.matches("^\\d{10}$")) {
            return null;
        }

        return context.getString(R.string.invalid_phone_number);
    }

    public String validateConfirmPassword(String password, String confirmPassword) {
        if(TextUtils.isEmpty(confirmPassword)) {
            return context.getString(R.string.password_required);
        } else if (!password.equals(confirmPassword)) {
            return context.getString(R.string.cpassword_not_match);
        }
        return null;
    }
    private static boolean isEmpty(String value) {
        return TextUtils.isEmpty(value) || value.trim().isEmpty();
    }

    private static boolean isContactEmailValid(String email) {
        return !isEmpty(email) && CONTACT_EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private static boolean isContactPhoneValid(String phone) {
        return !isEmpty(phone) && CONTACT_PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public ValidationResult validateLogin(String email, String password) {
        Map<String, String> errors = new HashMap<>();

        String emailError = validateEmail(email);
        if (emailError != null) {
            errors.put(FIELD_EMAIL, emailError);
        }

        String passwordError = validatePassword(password);
        if (passwordError != null) {
            errors.put(FIELD_PASSWORD, passwordError);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public ValidationResult validateSignup(String email, String password, String fullName,
                                           String phone, String confirmPassword) {
        Map<String, String> errors = new HashMap<>();

        String emailError = validateEmail(email);
        if (emailError != null) {
            errors.put(FIELD_EMAIL, emailError);
        }

        String passwordError = validatePassword(password);
        if (passwordError != null) {
            errors.put(FIELD_PASSWORD, passwordError);
        }

        String fullNameError = validateFullName(fullName);
        if (fullNameError != null) {
            errors.put(FIELD_FULL_NAME, fullNameError);
        }

        String phoneError = validatePhone(phone);
        if (phoneError != null) {
            errors.put(FIELD_PHONE, phoneError);
        }

        String confirmPasswordError = validateConfirmPassword(password, confirmPassword);
        if (confirmPasswordError != null) {
            errors.put(FIELD_CONFIRM_PASSWORD, confirmPasswordError);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public ValidationResult validateProfile(String fullName, String phone) {
        Map<String, String> errors = new HashMap<>();

        String fullNameError = validateFullName(fullName);
        if (fullNameError != null) {
            errors.put(FIELD_FULL_NAME, fullNameError);
        }

        // Phone is optional, but validate if provided
        if (phone != null && !phone.isEmpty()) {
            String phoneError = validatePhone(phone);
            if (phoneError != null) {
                errors.put(FIELD_PHONE, phoneError);
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public ValidationResult validatePasswordChange(String currentPassword,
                                                   String newPassword,
                                                   String confirmPassword) {
        Map<String, String> errors = new HashMap<>();

        // Validate current password
        String currentPasswordError = validatePassword(currentPassword);
        if (currentPasswordError != null) {
            errors.put("currentPassword", currentPasswordError);
        }

        // Validate new password
        String newPasswordError = validatePassword(newPassword);
        if (newPasswordError != null) {
            errors.put("newPassword", newPasswordError);
        }

        // Validate confirm password
        String confirmPasswordError = validateConfirmPassword(newPassword, confirmPassword);
        if (confirmPasswordError != null) {
            errors.put("confirmPassword", confirmPasswordError);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public ValidationResult validateAccountForm(String fullName, String phone,
                                                String currentPassword,
                                                String newPassword,
                                                String confirmPassword,
                                                boolean isGoogleUser) {
        Map<String, String> errors = new HashMap<>();

        // Validate profile
        ValidationResult profileResult = validateProfile(fullName, phone);
        errors.putAll(profileResult.getError());

        // Validate password only if not Google user
        if (!isGoogleUser) {
            // Check if any password fields are filled
            boolean hasPasswordContent = (currentPassword != null && !currentPassword.isEmpty()) ||
                    (newPassword != null && !newPassword.isEmpty()) ||
                    (confirmPassword != null && !confirmPassword.isEmpty());

            if (hasPasswordContent) {
                ValidationResult passwordResult = validatePasswordChange(
                        currentPassword, newPassword, confirmPassword
                );
                errors.putAll(passwordResult.getError());
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public ValidationResult validateContactForm(String name, String email, String phone, String message,
                                                String requiredFieldMsg, String invalidEmailMsg, String invalidPhoneMsg) {
        Map<String, String> errors = new HashMap<>();

        if (isEmpty(name)) {
            errors.put(FIELD_NAME, requiredFieldMsg);
        }

        if (isEmpty(email)) {
            errors.put(FIELD_EMAIL, requiredFieldMsg);
        } else if (!isContactEmailValid(email)) {
            errors.put(FIELD_EMAIL, invalidEmailMsg);
        }

        if (isEmpty(phone)) {
            errors.put(FIELD_PHONE, requiredFieldMsg);
        } else if (!isContactPhoneValid(phone)) {
            errors.put(FIELD_PHONE, invalidPhoneMsg);
        }

        if (isEmpty(message)) {
            errors.put(FIELD_MESSAGE, requiredFieldMsg);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}