package ca.iot.innovators.smart.farm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ca.iot.innovators.smart.farm.login.LoginActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginUITest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityScenarioRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /*
     * TEST 1
     * Verify that the email field is displayed with correct hint.
     */
    @Test
    public void loginScreen_displaysEmailField() {
        onView(withId(R.id.til_email)).check(matches(isDisplayed()));
        onView(withId(R.id.et_email)).check(matches(isDisplayed()));
        // ✅ FIXED: Use withHint() instead of withText()
        onView(withId(R.id.et_email)).check(matches(withHint(R.string.email_hint)));
    }

    /*
     * TEST 2
     * Verify that the password field is displayed with correct hint.
     */
    @Test
    public void loginScreen_displaysPasswordField() {
        onView(withId(R.id.til_password)).check(matches(isDisplayed()));
        onView(withId(R.id.et_password)).check(matches(isDisplayed()));
        // ✅ FIXED: Use withHint() instead of withText()
        onView(withId(R.id.et_password)).check(matches(withHint(R.string.password_hint)));
    }

    /*
     * TEST 3
     * Verify that the Remember Me checkbox and Login button are displayed.
     */
    @Test
    public void loginScreen_displaysLoginControls() {
        onView(withId(R.id.cb_remember_me)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_login)).check(matches(withText(R.string.login)));
    }

    /*
     * TEST 4
     * Verify that the Google sign-in button is displayed.
     */
    @Test
    public void loginScreen_displaysGoogleSignIn() {
        onView(withId(R.id.btn_signin_google)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_signin_google)).check(matches(withText(R.string.sign_in)));
    }

    /*
     * TEST 5
     * Verify that the Sign Up button is displayed.
     */
    @Test
    public void loginScreen_displaysSignUpSection() {
        onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_signup)).check(matches(withText(R.string.sign_up)));
    }

    /*
     * TEST 7
     * Verify that the OR divider text is displayed.
     */
    @Test
    public void loginScreen_displaysOrDivider() {
        onView(withText("OR")).check(matches(isDisplayed()));
    }

    /*
     * TEST 8
     * Verify the "Do not have an account?" text is displayed.
     */
    @Test
    public void loginScreen_displaysSignUpPrompt() {
        onView(withText(R.string.signup))
                .check(matches(isDisplayed()));
    }

    /*
     * TEST 9
     * Verify user can type email into the email field.
     */
    @Test
    public void emailField_acceptsUserInput() {
        String email = "farmer@gmail.com";
        onView(withId(R.id.et_email))
                .perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.et_email))
                .check(matches(withText(email)));
    }

    /*
     * TEST 10
     * Verify user can type password into the password field.
     */
    @Test
    public void passwordField_acceptsUserInput() {
        String password = "SmartFarm123!";
        onView(withId(R.id.et_password))
                .perform(typeText(password), closeSoftKeyboard());
        onView(withId(R.id.et_password))
                .check(matches(withText(password)));
    }

    /*
     * TEST 11
     * Verify Remember Me checkbox can be checked.
     */
    @Test
    public void rememberMe_canBeChecked() {
        onView(withId(R.id.cb_remember_me))
                .perform(click());
        onView(withId(R.id.cb_remember_me))
                .check(matches(isChecked()));
    }
}