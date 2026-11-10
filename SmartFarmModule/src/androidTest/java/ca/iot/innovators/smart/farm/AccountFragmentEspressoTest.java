// AccountFragmentEspressoTest.java - Fixed all tests
package ca.iot.innovators.smart.farm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import android.view.View;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccountFragmentEspressoTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private IdlingResource idlingResource;

    @Before
    public void setUp() {
        // Navigate to Account Fragment
        onView(withId(R.id.action_account)).perform(click());
        waitFor(500);
    }

    @After
    public void tearDown() {
        if (idlingResource != null) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    private void waitForValidation() {
        waitFor(500); // Wait for debounce (300ms) + extra time
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testInitialViewMode_FieldsDisabled() {
        onView(withId(R.id.etName)).check(matches(not(isEnabled())));
        onView(withId(R.id.etPhone)).check(matches(not(isEnabled())));
        onView(withId(R.id.etFarmName)).check(matches(not(isEnabled())));
        onView(withId(R.id.etLocation)).check(matches(not(isEnabled())));
        onView(withId(R.id.btnChangePhoto)).check(matches(not(isDisplayed())));
        onView(withId(R.id.layoutSecurity)).check(matches(not(isDisplayed())));
        onView(withId(R.id.btnEdit)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmail)).check(matches(not(isEnabled())));
    }

    @Test
    public void testEditButton_EnablesEditMode() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.etName)).check(matches(isEnabled()));
        onView(withId(R.id.etPhone)).check(matches(isEnabled()));
        onView(withId(R.id.etFarmName)).check(matches(isEnabled()));
        onView(withId(R.id.etLocation)).check(matches(isEnabled()));
        onView(withId(R.id.btnChangePhoto)).check(matches(isDisplayed()));
        onView(withId(R.id.layoutSecurity)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCancel)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btnSaveChanges)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.etEmail)).check(matches(not(isEnabled())));
    }

    @Test
    public void testCancelButton_ExitsEditModeWithoutSaving() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.etName)).perform(clearText(), typeText("New Name"), closeSoftKeyboard());
        waitForValidation();

        onView(withId(R.id.btnCancel)).perform(scrollTo(), click());
        waitFor(300);

        onView(withId(R.id.etName)).check(matches(not(isEnabled())));
        onView(withId(R.id.btnChangePhoto)).check(matches(not(isDisplayed())));
        onView(withId(R.id.layoutSecurity)).check(matches(not(isDisplayed())));
        onView(withId(R.id.btnEdit)).check(matches(isDisplayed()));
    }

    @Test
    public void testSaveButton_DisabledWhenNoChanges() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(not(isEnabled())));
    }

    @Test
    public void testSaveButton_EnabledAfterChanges() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.etName)).perform(clearText(), typeText("Changed Name"), closeSoftKeyboard());
        waitForValidation();

        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(isEnabled()));
    }

    @Test
    public void testSaveButton_DisabledWithInvalidData() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Enter invalid data (too short name)
        onView(withId(R.id.etName)).perform(clearText(), typeText("A"), closeSoftKeyboard());
        waitForValidation();

        // Verify error is shown
        onView(withId(R.id.tilName)).check(matches(hasError()));

        // Save button should be disabled
        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(not(isEnabled())));
    }

    @Test
    public void testNameValidation_ShowsErrorOnInvalidInput() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Enter invalid name (too short)
        onView(withId(R.id.etName)).perform(clearText(), typeText("AB"), closeSoftKeyboard());
        waitForValidation();

        onView(withId(R.id.tilName)).check(matches(hasError()));

        // Fix the name
        onView(withId(R.id.etName)).perform(clearText(), typeText("Valid Name"), closeSoftKeyboard());
        waitForValidation();

        onView(withId(R.id.tilName)).check(matches(not(hasError())));
    }

    @Test
    public void testPhoneValidation_ShowsErrorOnInvalidInput() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Enter invalid phone
        onView(withId(R.id.etPhone)).perform(clearText(), typeText("123"), closeSoftKeyboard());
        waitForValidation();

        onView(withId(R.id.tilPhone)).check(matches(hasError()));

        // Enter valid phone
        onView(withId(R.id.etPhone)).perform(clearText(), typeText("+11234567890"), closeSoftKeyboard());
        waitForValidation();

        onView(withId(R.id.tilPhone)).check(matches(not(hasError())));
    }

    @Test
    public void testChangePassword_ValidatesFields() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.etCurrentPassword)).perform(scrollTo(), typeText("old!Password123"), closeSoftKeyboard());
        onView(withId(R.id.etNewPassword)).perform(scrollTo(), typeText("NewP@ssw0rd123"), closeSoftKeyboard());
        onView(withId(R.id.etConfirmPassword)).perform(scrollTo(), typeText("NewP@ssw0rd123"), closeSoftKeyboard());
        waitForValidation();

        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(isEnabled()));
    }

    @Test
    public void testChangePassword_InvalidPasswordShowsError() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.etCurrentPassword)).perform(scrollTo(), typeText("old"), closeSoftKeyboard());
        onView(withId(R.id.etNewPassword)).perform(scrollTo(), typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.etConfirmPassword)).perform(scrollTo(), typeText("123"), closeSoftKeyboard());
        waitForValidation();

        // Verify error is shown
        onView(withId(R.id.tilNewPassword)).perform(scrollTo());
        onView(withId(R.id.tilNewPassword)).check(matches(hasError()));

        // Save button should be disabled
        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(not(isEnabled())));
    }

    @Test
    public void testChangePassword_NonMatchingPasswordsShowsError() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.etCurrentPassword)).perform(scrollTo(), typeText("oldPassword123"), closeSoftKeyboard());
        onView(withId(R.id.etNewPassword)).perform(scrollTo(), typeText("NewP@ssw0rd123"), closeSoftKeyboard());
        onView(withId(R.id.etConfirmPassword)).perform(scrollTo(), typeText("DifferentPass123"), closeSoftKeyboard());
        waitForValidation();

        // Verify error is shown
        onView(withId(R.id.tilConfirmPassword)).perform(scrollTo());
        onView(withId(R.id.tilConfirmPassword)).check(matches(hasError()));

        // Save button should be disabled
        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(not(isEnabled())));
    }

    @Test
    public void testChangePassword_RequiredFieldsValidation() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Scroll to and fill new password
        onView(withId(R.id.etNewPassword)).perform(scrollTo(), typeText("NewP@ssw0rd123"), closeSoftKeyboard());
        waitForValidation();

        // Ensure current password field is visible and check for error
        onView(withId(R.id.etCurrentPassword)).perform(scrollTo());
        onView(withId(R.id.tilCurrentPassword)).check(matches(hasError()));

        // Save button should be disabled
        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(not(isEnabled())));
    }

    @Test
    public void testChangePhoto_ButtonVisibleInEditMode() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        onView(withId(R.id.btnChangePhoto)).check(matches(isDisplayed()));
        onView(withId(R.id.btnChangePhoto)).check(matches(isEnabled()));
    }

    @Test
    public void testFullEditAndSaveFlow() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Make multiple changes
        onView(withId(R.id.etName)).perform(clearText(), typeText("Full Name Test"), closeSoftKeyboard());
        onView(withId(R.id.etPhone)).perform(clearText(), typeText("+19876543210"), closeSoftKeyboard());
        onView(withId(R.id.etFarmName)).perform(clearText(), typeText("Test Farm"), closeSoftKeyboard());
        onView(withId(R.id.etLocation)).perform(clearText(), typeText("123 Test St"), closeSoftKeyboard());
        waitForValidation();

        // Verify save button is enabled
        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(isEnabled()));

        // Click save
        onView(withId(R.id.btnSaveChanges)).perform(click());
        waitFor(500);

        // Exit edit mode
        onView(withId(R.id.btnCancel)).perform(scrollTo(), click());
        waitFor(300);

        // Verify exit edit mode
        onView(withId(R.id.etName)).check(matches(not(isEnabled())));
        onView(withId(R.id.etPhone)).check(matches(not(isEnabled())));
        onView(withId(R.id.etFarmName)).check(matches(not(isEnabled())));
        onView(withId(R.id.etLocation)).check(matches(not(isEnabled())));
        onView(withId(R.id.btnEdit)).check(matches(isDisplayed()));
    }

    @Test
    public void testFullEditWithPasswordAndSaveFlow() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Update profile
        onView(withId(R.id.etName)).perform(clearText(), typeText("Full Name Test"), closeSoftKeyboard());
        onView(withId(R.id.etPhone)).perform(clearText(), typeText("+19876543210"), closeSoftKeyboard());
        waitForValidation();

        // Update password
        onView(withId(R.id.etCurrentPassword)).perform(scrollTo(), typeText("oldP@ssword123"), closeSoftKeyboard());
        onView(withId(R.id.etNewPassword)).perform(scrollTo(), typeText("NewP@ssw0rd123"), closeSoftKeyboard());
        onView(withId(R.id.etConfirmPassword)).perform(scrollTo(), typeText("NewP@ssw0rd123"), closeSoftKeyboard());
        waitForValidation();

        // Verify save button is enabled
        onView(withId(R.id.btnSaveChanges)).perform(scrollTo());
        onView(withId(R.id.btnSaveChanges)).check(matches(isEnabled()));

        // Click save
        onView(withId(R.id.btnSaveChanges)).perform(click());
        waitFor(500);

        // Exit edit mode
        onView(withId(R.id.btnCancel)).perform(scrollTo(), click());
        waitFor(300);

        // Verify exit edit mode
        onView(withId(R.id.etName)).check(matches(not(isEnabled())));
        onView(withId(R.id.etPhone)).check(matches(not(isEnabled())));
        onView(withId(R.id.btnEdit)).check(matches(isDisplayed()));
    }

    @Test
    public void testCancelButton_ClearsValidationErrors() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Enter invalid data
        onView(withId(R.id.etName)).perform(clearText(), typeText("AB"), closeSoftKeyboard());
        waitForValidation();

        // Verify error is shown
        onView(withId(R.id.tilName)).check(matches(hasError()));

        // Click cancel
        onView(withId(R.id.btnCancel)).perform(scrollTo(), click());
        waitFor(300);

        // Verify errors are cleared
        onView(withId(R.id.tilName)).check(matches(not(hasError())));

        // Verify fields are disabled
        onView(withId(R.id.etName)).check(matches(not(isEnabled())));
    }

    @Test
    public void testDebouncedValidation_DoesNotValidateOnEveryKeystroke() {
        onView(withId(R.id.btnEdit)).perform(click());
        waitFor(300);

        // Type quickly - validation should be debounced
        onView(withId(R.id.etName)).perform(clearText());

        // Type characters quickly
        for (char c : "AB".toCharArray()) {
            onView(withId(R.id.etName)).perform(typeText(String.valueOf(c)));
        }

        // Check that error appears after debounce delay
        waitFor(500);
        onView(withId(R.id.tilName)).check(matches(hasError()));

        // Continue typing to fix the name
        for (char c : "Valid Name".toCharArray()) {
            onView(withId(R.id.etName)).perform(typeText(String.valueOf(c)));
        }

        // Wait for debounced validation
        waitFor(500);
        onView(withId(R.id.tilName)).check(matches(not(hasError())));
    }

    // Custom matcher for checking if TextInputLayout has any error
    private static Matcher<View> hasError() {
        return new BoundedMatcher<>(TextInputLayout.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has error");
            }

            @Override
            protected boolean matchesSafely(TextInputLayout textInputLayout) {
                CharSequence error = textInputLayout.getError();
                return error != null && error.length() > 0;
            }
        };
    }
}