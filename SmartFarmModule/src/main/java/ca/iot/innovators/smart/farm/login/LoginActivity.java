/**
 * CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/

package ca.iot.innovators.smart.farm.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ca.iot.innovators.smart.farm.*;
import ca.iot.innovators.smart.farm.signup.SignupActivity;

public class LoginActivity extends BaseActivity
        implements LoginController.LoginView {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin, btnSignup, btnGoogleSignIn;
    private CheckBox cbRememberMe;
    private LoginController loginController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginController = new LoginController(this, this);
        loginController.onStart();

        initializeViews();
        setupClickListeners();

    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnSignup = findViewById(R.id.btn_signup);
        btnGoogleSignIn = findViewById(R.id.btn_signin_google);
        cbRememberMe = findViewById(R.id.cb_remember_me);

        cbRememberMe.setChecked(loginController.isRememberMe());
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean rememberMe = cbRememberMe.isChecked();

            loginController.handleLogin(email, password, rememberMe);
        });

        btnSignup.setOnClickListener(v -> navigateToSignup());

        btnGoogleSignIn.setOnClickListener(v -> {
            boolean rememberMe = cbRememberMe.isChecked();
            loginController.handleGoogleSignIn(rememberMe);
        });
    }

    private void navigateToSignup() {
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
        finish();
    }

    // LoginView Interface Methods
    @Override
    public void showEmailError(String err) {
        tilEmail.setError(err);
    }

    @Override
    public void showPasswordError(String err) {
        tilPassword.setError(err);
    }

    @Override
    public void clearEmailError() {
        tilEmail.setError(null);
    }

    @Override
    public void clearPasswordError() {
        tilPassword.setError(null);
    }

    @Override
    public void showLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnGoogleSignIn.setEnabled(!isLoading);
        btnSignup.setEnabled(!isLoading);
    }

    @Override
    public void onLoginSuccess() {
        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    @Override
    public void onLoginFailure(String errorMessage) {
        Toast.makeText(this, "Login Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


}