package ca.iot.innovators.smart.farm.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.assertEquals;

import android.os.Looper;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import ca.iot.innovators.smart.farm.R;

/**
 * Robolectric tests for the Feedback fragment's initial/default view state.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 31)
public class FeedbackRobolectricTest {

    @Before
    public void setUp() {
        if (FirebaseApp.getApps(RuntimeEnvironment.getApplication()).isEmpty()) {
            FirebaseApp.initializeApp(
                    RuntimeEnvironment.getApplication(),
                    new FirebaseOptions.Builder()
                            .setApplicationId("1:000000000000:android:0000000000000000")
                            .setApiKey("fake-api-key")
                            .setProjectId("fake-project-id")
                            .build()
            );
        }
    }

    @Test
    public void submitButton_isEnabled_byDefaultOnLaunch() {
        FragmentScenario<Feedback> scenario = FragmentScenario.launchInContainer(
                Feedback.class,
                null,
                R.style.Theme_SmartFarm,
                (FragmentFactory) null
        );

        shadowOf(Looper.getMainLooper()).idle();

        scenario.onFragment(fragment -> {
            assertNotNull(fragment.getView());
            MaterialButton btnSubmit = fragment.getView().findViewById(R.id.btn_submit_feedback);
            assertNotNull(btnSubmit);
            assertTrue(btnSubmit.isEnabled());
        });
    }

    @Test
    public void inputFieldsAndOverlay_haveCorrectDefaultVisibility() {
        FragmentScenario<Feedback> scenario = FragmentScenario.launchInContainer(
                Feedback.class,
                null,
                R.style.Theme_SmartFarm,
                (FragmentFactory) null
        );

        shadowOf(Looper.getMainLooper()).idle();

        scenario.onFragment(fragment -> {
            View view = fragment.getView();
            assertNotNull(view);

            TextInputLayout tilName = view.findViewById(R.id.til_feedback_name);
            TextInputLayout tilEmail = view.findViewById(R.id.til_feedback_email);
            TextInputLayout tilPhone = view.findViewById(R.id.til_feedback_phone);
            TextInputLayout tilMessage = view.findViewById(R.id.til_feedback_message);
            FrameLayout overlaySubmitting = view.findViewById(R.id.overlay_submitting);

            assertNotNull(tilName);
            assertNotNull(tilEmail);
            assertNotNull(tilPhone);
            assertNotNull(tilMessage);
            assertNotNull(overlaySubmitting);

            assertEquals(View.VISIBLE, tilName.getVisibility());
            assertEquals(View.VISIBLE, tilEmail.getVisibility());
            assertEquals(View.VISIBLE, tilPhone.getVisibility());
            assertEquals(View.VISIBLE, tilMessage.getVisibility());

            assertEquals(View.GONE, overlaySubmitting.getVisibility());
        });
    }

    @Test
    public void categoryAndRatingErrors_areHidden_byDefaultOnLaunch() {
        FragmentScenario<Feedback> scenario = FragmentScenario.launchInContainer(
                Feedback.class,
                null,
                R.style.Theme_SmartFarm,
                (FragmentFactory) null
        );

        shadowOf(Looper.getMainLooper()).idle();

        scenario.onFragment(fragment -> {
            View view = fragment.getView();
            assertNotNull(view);

            TextView categoryError = view.findViewById(R.id.tv_category_error);
            TextView ratingError = view.findViewById(R.id.tv_rating_error);

            assertNotNull(categoryError);
            assertNotNull(ratingError);

            assertEquals(View.GONE, categoryError.getVisibility());
            assertEquals(View.GONE, ratingError.getVisibility());
        });
    }

    @Test
    public void ratingBar_hasHalfStarStepSize() {
        FragmentScenario<Feedback> scenario = FragmentScenario.launchInContainer(
                Feedback.class,
                null,
                R.style.Theme_SmartFarm,
                (FragmentFactory) null
        );

        shadowOf(Looper.getMainLooper()).idle();

        scenario.onFragment(fragment -> {
            View view = fragment.getView();
            assertNotNull(view);

            RatingBar ratingFeedback = view.findViewById(R.id.rating_feedback);
            assertNotNull(ratingFeedback);

            assertEquals(0.5f, ratingFeedback.getStepSize(), 0.0f);
        });
    }
}