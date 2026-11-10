/** CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/

package ca.iot.innovators.smart.farm.signup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ca.iot.innovators.smart.farm.*;
import ca.iot.innovators.smart.farm.login.LoginActivity;

public class SignupActivity extends BaseActivity
        implements SignupController.SignupView{
    private TextInputEditText etEmail, etPassword, etFullName, etPhone, etConfirmPassword;
    private TextInputLayout tilEmail, tilPassword, tilFullName, tilPhone, tilConfirmPassword;
    private MaterialButton btnSignup, btnLogin;
    private CheckBox cbTerms;

    private SignupController signupController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupController = new SignupController(this, this);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilFullName = findViewById(R.id.til_fullname);
        tilPhone = findViewById(R.id.til_phone);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etFullName = findViewById(R.id.et_fullname);
        etPhone = findViewById(R.id.et_phone);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnSignup = findViewById(R.id.btn_signup);
        btnLogin = findViewById(R.id.btn_login);
        cbTerms = findViewById(R.id.cb_terms);
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            String fullName = etFullName.getText().toString();
            String phone = etPhone.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();
            boolean termsAccepted = cbTerms.isChecked();

            signupController.handleSignup(email, password, fullName, phone, confirmPassword, termsAccepted);
        });

        btnLogin.setOnClickListener(v -> navigateToLogin());
    }

    @Override
    public void showLoading(boolean isLoading) {
        btnSignup.setEnabled(!isLoading);
    }

    @Override
    public void showFullNameError(String err) {
        tilFullName.setError(err);
    }

    @Override
    public void showPhoneError(String err) {
        tilPhone.setError(err);
    }

    @Override
    public void showEmailError(String err) {
        tilEmail.setError(err);
    }

    @Override
    public void showPasswordError(String err) {
        tilPassword.setError(err);
    }

    @Override
    public void showConfirmPasswordError(String err) {
        tilConfirmPassword.setError(err);
    }

    @Override
    public void clearAllErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilFullName.setError(null);
        tilPhone.setError(null);
        tilConfirmPassword.setError(null);
    }

    @Override
    public void onSignupSuccess() {
        Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    @Override
    public void onSignupFailure(String errorMessage) {
        Toast.makeText(this, getString(R.string.signup_failure) + errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
