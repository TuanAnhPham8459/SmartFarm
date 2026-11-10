/**
 * CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/

package ca.iot.innovators.smart.farm.ui;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.model.FeedbackModel;
import ca.iot.innovators.smart.farm.utils.CooldownCalculator;
import ca.iot.innovators.smart.farm.utils.InputChecker;

public class Feedback extends Fragment {

    // Fake delay to simulate a submission taking time, drives the progress bar animation
    private static final long SUBMIT_DELAY_MS = 5000;
    // How long a user must wait between feedback submissions
    private static final long COOLDOWN_MS = CooldownCalculator.DEFAULT_COOLDOWN_MS;
    // Firestore collection name storing each user's last-submit timestamp
    private static final String COOLDOWN_COLLECTION = "feedbackCooldowns";
    private static final String FIELD_LAST_SUBMIT = "lastSubmitMillis";

    private TextInputLayout tilName, tilEmail, tilPhone, tilMessage;
    private TextInputEditText etName, etEmail, etPhone, etMessage;
    private RadioGroup rgCategory;
    private TextView categoryLabel, categoryError, ratingLabel, ratingError, tvCooldown;
    private RatingBar ratingFeedback;
    private MaterialButton btnSubmit;
    private FrameLayout overlaySubmitting;
    private ProgressBar progressFeedback;
    private OnBackPressedCallback backCallback;
    // Guards against showing the "discard changes" dialog right after a successful submit
    private boolean isSubmitting = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer cooldownTimer;
    // Cached so submitFeedback() can check cooldown status without re-querying Firestore
    private long cachedRemainingMs = 0L;
    private InputChecker inputChecker;

    public Feedback() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputChecker = new InputChecker(requireContext());

        // Bind all view references from the inflated layout
        tilName = view.findViewById(R.id.til_feedback_name);
        tilEmail = view.findViewById(R.id.til_feedback_email);
        tilPhone = view.findViewById(R.id.til_feedback_phone);
        tilMessage = view.findViewById(R.id.til_feedback_message);

        etName = view.findViewById(R.id.et_feedback_name);
        etEmail = view.findViewById(R.id.et_feedback_email);
        etPhone = view.findViewById(R.id.et_feedback_phone);
        etMessage = view.findViewById(R.id.et_feedback_message);
        rgCategory = view.findViewById(R.id.rg_feedback_category);
        categoryLabel = view.findViewById(R.id.tv_feedback_category_label);
        categoryError = view.findViewById(R.id.tv_category_error);
        ratingLabel = view.findViewById(R.id.tv_feedback_rating_label);
        ratingError = view.findViewById(R.id.tv_rating_error);
        ratingFeedback = view.findViewById(R.id.rating_feedback);
        btnSubmit = view.findViewById(R.id.btn_submit_feedback);
        overlaySubmitting = view.findViewById(R.id.overlay_submitting);
        progressFeedback = view.findViewById(R.id.progress_feedback);
        tvCooldown = view.findViewById(R.id.tv_feedback_cooldown);

        btnSubmit.setOnClickListener(v -> submitFeedback());

        // Clear field-level errors as soon as the user starts typing again
        clearErrorOnType(etName, tilName);
        clearErrorOnType(etEmail, tilEmail);
        clearErrorOnType(etPhone, tilPhone);
        clearErrorOnType(etMessage, tilMessage);

        // Reset category error styling once any radio option is picked
        rgCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                categoryLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green));
                categoryError.setVisibility(View.GONE);
            }
        });

        // Reset rating error styling once the user picks a star rating
        ratingFeedback.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
            if (rating > 0f) {
                ratingLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green));
                ratingError.setVisibility(View.GONE);
            }
        });

        // Intercept back navigation to warn about unsaved input before leaving
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);

        // Check Firestore for an active submission cooldown as soon as the screen loads
        fetchCooldownFromFirestore();
    }

    // Returns the logged-in user's UID, or null if no one is signed in
    private String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Looks up the user's last submission time to determine if they're still in cooldown
    private void fetchCooldownFromFirestore() {
        String uid = getCurrentUid();
        if (uid == null) {
            setCooldownUiActive(false);
            return;
        }

        // Disable submit while we wait for the Firestore lookup to complete
        btnSubmit.setEnabled(false);

        FirebaseFirestore.getInstance()
                .collection(COOLDOWN_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(this::handleCooldownSnapshot)
                .addOnFailureListener(e -> {
                    // Fail open: if Firestore is unreachable, don't block the user indefinitely
                    cachedRemainingMs = 0L;
                    setCooldownUiActive(false);
                });
    }

    // Calculates remaining cooldown time from the fetched snapshot and updates the UI
    private void handleCooldownSnapshot(DocumentSnapshot snapshot) {
        long lastSubmit = 0L;
        if (snapshot.exists() && snapshot.contains(FIELD_LAST_SUBMIT)) {
            Long value = snapshot.getLong(FIELD_LAST_SUBMIT);
            if (value != null) {
                lastSubmit = value;
            }
        }

        cachedRemainingMs = CooldownCalculator.getRemainingMillis(
                lastSubmit, System.currentTimeMillis(), COOLDOWN_MS);

        if (cachedRemainingMs > 0) {
            startCooldownDisplay(cachedRemainingMs);
        } else {
            setCooldownUiActive(false);
        }
    }

    // Starts a live countdown timer showing the user how long until they can submit again
    private void startCooldownDisplay(long remainingMs) {
        setCooldownUiActive(true);

        if (cooldownTimer != null) {
            cooldownTimer.cancel();
        }

        cooldownTimer = new CountDownTimer(remainingMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                cachedRemainingMs = millisUntilFinished;
                updateCooldownText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                cachedRemainingMs = 0L;
                setCooldownUiActive(false);
            }
        };
        cooldownTimer.start();
    }

    // Formats the remaining cooldown time into "X hours Y minutes" style text
    private void updateCooldownText(long millisRemaining) {
        long hours = CooldownCalculator.toHours(millisRemaining);
        long minutes = CooldownCalculator.toMinutesRemainder(millisRemaining);

        String text = getString(R.string.feedback_cooldown_message, hours, minutes);
        if (tvCooldown != null) {
            tvCooldown.setText(text);
        }
    }

    // Toggles submit button and cooldown message visibility based on cooldown state
    private void setCooldownUiActive(boolean active) {
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!active);
        }
        if (tvCooldown != null) {
            tvCooldown.setVisibility(active ? View.VISIBLE : View.GONE);
        }
        if (active) {
            updateCooldownText(cachedRemainingMs);
        }
    }

    // Attaches a listener that clears a field's error message once the user edits it
    private void clearErrorOnType(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null && !s.toString().trim().isEmpty()) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    // Checks whether the user has entered anything that would be lost if they navigate away
    private boolean hasUnsavedInput() {
        if (isSubmitting) return false;

        boolean nameFilled = etName != null && etName.getText() != null && !etName.getText().toString().trim().isEmpty();
        boolean emailFilled = etEmail != null && etEmail.getText() != null && !etEmail.getText().toString().trim().isEmpty();
        boolean phoneFilled = etPhone != null && etPhone.getText() != null && !etPhone.getText().toString().trim().isEmpty();
        boolean messageFilled = etMessage != null && etMessage.getText() != null && !etMessage.getText().toString().trim().isEmpty();
        boolean categorySelected = rgCategory != null && rgCategory.getCheckedRadioButtonId() != -1;
        boolean ratingSet = ratingFeedback != null && ratingFeedback.getRating() > 0f;

        return nameFilled || emailFilled || phoneFilled || messageFilled || categorySelected || ratingSet;
    }

    // Decides whether to warn the user before leaving, or just leave immediately
    private void handleBackPress() {
        if (hasUnsavedInput()) {
            showDiscardConfirmationDialog();
        } else {
            leaveScreen();
        }
    }

    // Shows a confirmation dialog asking the user to discard or keep editing
    private void showDiscardConfirmationDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.discard_feedback)
                .setMessage(R.string.unsaved_changes)
                .setCancelable(true)
                .setPositiveButton(R.string.discard, (dialog, which) -> leaveScreen())
                .setNegativeButton(R.string.keep_editing, (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Pops this fragment off the back stack, disabling the back callback first to avoid a loop
    private void leaveScreen() {
        if (backCallback != null) {
            backCallback.setEnabled(false);
        }
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    // Runs all field validation via InputChecker and updates error UI for each section
    private boolean validateFields(String name, String email, String phone, String message,
                                   int selectedId, float rating) {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                name, email, phone, message,
                getString(R.string.required_field),
                getString(R.string.invalid_email),
                getString(R.string.invalid_phone));

        tilName.setError(result.getNameError());
        tilEmail.setError(result.getEmailError());
        tilPhone.setError(result.getPhoneError());
        tilMessage.setError(result.getMessageError());

        boolean isValid = result.isValid();

        // Category and rating aren't part of InputChecker's form check, so validate them separately
        if (selectedId == -1) {
            categoryLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.design_error_red));
            categoryError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            categoryLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green));
            categoryError.setVisibility(View.GONE);
        }

        if (rating <= 0f) {
            ratingLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.design_error_red));
            ratingError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            ratingLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green));
            ratingError.setVisibility(View.GONE);
        }

        return isValid;
    }

    // Entry point triggered by the submit button; validates, then kicks off the fake submit flow
    private void submitFeedback() {
        if (cachedRemainingMs > 0) {
            Toast.makeText(getContext(), R.string.feedback_cooldown_active, Toast.LENGTH_SHORT).show();
            return;
        }

        if (etName == null || etEmail == null || etPhone == null || etMessage == null || rgCategory == null) {
            return;
        }

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        int selectedId = rgCategory.getCheckedRadioButtonId();
        float rating = ratingFeedback.getRating();

        if (!validateFields(name, email, phone, message, selectedId, rating)) {
            Toast.makeText(getContext(), R.string.fill_out_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String category = getCategoryLabel(selectedId);
        String deviceInfo = getDeviceInfo();
        FeedbackModel feedback = new FeedbackModel(name, email, phone, category, rating, message, deviceInfo);

        // Clear the form immediately so it looks submitted, even while the fake delay runs
        clearInputs();
        setSubmittingState(true);
        animateProgressAndSubmit(feedback);
    }

    // Resets all input fields back to their empty/default state
    private void clearInputs() {
        if (etName != null) etName.setText("");
        if (etEmail != null) etEmail.setText("");
        if (etPhone != null) etPhone.setText("");
        if (etMessage != null) etMessage.setText("");
        if (rgCategory != null) rgCategory.clearCheck();
        if (ratingFeedback != null) ratingFeedback.setRating(0f);
    }

    // Animates the progress bar over SUBMIT_DELAY_MS, then actually writes to Firestore
    private void animateProgressAndSubmit(FeedbackModel feedback) {
        if (progressFeedback == null) return;

        ObjectAnimator animator = ObjectAnimator.ofInt(progressFeedback, "progress", 0, 100);
        animator.setDuration(SUBMIT_DELAY_MS);
        animator.start();

        handler.postDelayed(() -> sendToFirestore(feedback), SUBMIT_DELAY_MS);
    }

    // Writes the feedback document to Firestore and handles success/failure UI
    private void sendToFirestore(FeedbackModel feedback) {
        if (getContext() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("feedback")
                .add(feedback)
                .addOnSuccessListener(documentReference -> {
                    isSubmitting = true;
                    setSubmittingState(false);
                    recordSubmissionTime();
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    setSubmittingState(false);
                    Toast.makeText(getContext(), R.string.feedback_failed, Toast.LENGTH_SHORT).show();
                });
    }

    // Saves the current timestamp so the cooldown period starts counting from now
    private void recordSubmissionTime() {
        String uid = getCurrentUid();
        if (uid == null) return;

        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_LAST_SUBMIT, now);

        FirebaseFirestore.getInstance()
                .collection(COOLDOWN_COLLECTION)
                .document(uid)
                .set(data);

        cachedRemainingMs = COOLDOWN_MS;
        startCooldownDisplay(COOLDOWN_MS);
    }

    // Shows/hides the loading overlay and disables the submit button while submitting
    private void setSubmittingState(boolean submitting) {
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!submitting);
        }
        if (overlaySubmitting != null) {
            overlaySubmitting.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
        if (progressFeedback != null && submitting) {
            progressFeedback.setProgress(0);
        }
    }

    // Shows a confirmation dialog after a successful submission
    private void showSuccessDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.feedback_submitted_title)
                .setMessage(R.string.feedback_success)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> leaveScreen())
                .show();
    }

    // Maps the selected radio button ID to a human-readable category string for storage
    private String getCategoryLabel(int checkedId) {
        if (checkedId == R.id.rb_feature) return "Feature Request";
        if (checkedId == R.id.rb_ui) return "UI Design Feedback";
        if (checkedId == R.id.rb_general) return "General Feedback";
        return "Unknown";
    }

    // Builds a device info string to include with the feedback for debugging/analytics purposes
    private String getDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any pending callbacks/timers so they don't fire after the view is gone
        handler.removeCallbacksAndMessages(null);
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            cooldownTimer = null;
        }
        // Null out view references to avoid holding onto a destroyed view hierarchy
        tilName = null;
        tilEmail = null;
        tilPhone = null;
        tilMessage = null;
        etName = null;
        etEmail = null;
        etPhone = null;
        etMessage = null;
        rgCategory = null;
        categoryLabel = null;
        categoryError = null;
        ratingLabel = null;
        ratingError = null;
        ratingFeedback = null;
        btnSubmit = null;
        overlaySubmitting = null;
        progressFeedback = null;
        tvCooldown = null;
        backCallback = null;
        inputChecker = null;
    }
}