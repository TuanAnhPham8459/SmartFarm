package ca.iot.innovators.smart.farm.utils;

import static org.junit.Assert.*;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

import ca.iot.innovators.smart.farm.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 31)
public class InputCheckerTest {

    private InputChecker inputChecker;
    private Context context;

    private static final String REQUIRED_MSG = "Required";
    private static final String INVALID_EMAIL_MSG = "Invalid email";
    private static final String INVALID_PHONE_MSG = "Invalid phone";

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        inputChecker = new InputChecker(context);
    }

    @Test
    public void testValidateLogin_ValidInput() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "Password123!");

        assertTrue(result.isValid());
        assertNotNull(result.getError());
    }

    @Test
    public void testValidateLogin_EmptyEmail_Invalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "", "Password123!");

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_EMAIL));
        assertNotNull(result.getEmailError());
        assertEquals(context.getString(R.string.email_required), result.getEmailError());
    }

    @Test
    public void testValidateLogin_InvalidEmailFormat_Invalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "random-string", "Password123!");

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_EMAIL));
        assertNotNull(result.getEmailError());
        assertEquals(context.getString(R.string.email_invalid), result.getEmailError());
    }

    @Test
    public void testValidateLogin_EmptyPassword_Invalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", ""
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PASSWORD));
        assertEquals(context.getString(R.string.password_required), result.getPasswordError());
    }

    @Test
    public void testValidateLogin_ShortPassword_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "Pass1!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PASSWORD));
        assertNotNull(result.getPasswordError());
        assertEquals(context.getString(R.string.password_too_short), result.getPasswordError());
    }

    @Test
    public void testValidateLogin_PasswordWithoutUppercase_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "password123!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PASSWORD));
        assertNotNull(result.getPasswordError());
        assertEquals(context.getString(R.string.password_format_invalid), result.getPasswordError());
    }

    @Test
    public void testValidateLogin_PasswordWithoutSpecialChar_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "Password123"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PASSWORD));
        assertEquals(context.getString(R.string.password_format_invalid), result.getPasswordError());
    }

    @Test
    public void testValidateLogin_PasswordWithoutDigit_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "Password!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PASSWORD));
        assertEquals(context.getString(R.string.password_format_invalid), result.getPasswordError());
    }

    @Test
    public void testValidateSignup_AllValidFields_ReturnsValid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "1234567890", "Password123!"
        );

        assertTrue(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().isEmpty());
    }

    @Test
    public void testValidateSignup_EmptyFullName_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "", "1234567890", "Password123!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_FULL_NAME));
        assertNotNull(result.getFullNameError());
        assertEquals(context.getString(R.string.full_name_required), result.getFullNameError());
    }

    @Test
    public void testValidateSignup_ShortFullName_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "Jo", "1234567890", "Password123!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_FULL_NAME));
        assertEquals(context.getString(R.string.full_name_length), result.getFullNameError());
    }

    @Test
    public void testValidateSignup_EmptyPhone_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "", "Password123!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PHONE));
        assertEquals(context.getString(R.string.phone_number_required), result.getPhoneError());
    }

    @Test
    public void testValidateSignup_InvalidPhoneFormat_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "12345", "Password123!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PHONE));
        assertEquals(context.getString(R.string.invalid_phone_number), result.getPhoneError());
    }

    @Test
    public void testValidateSignup_MismatchedPasswords_ReturnsInvalid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "1234567890", "Password456!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_CONFIRM_PASSWORD));
        assertEquals(context.getString(R.string.cpassword_not_match), result.getConfirmPasswordError());
    }

    @Test
    public void testValidateSignup_MultipleErrors_ReturnsAllErrors() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "invalid", "pass",
                "", "12345", "pass"
        );

        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().size() > 1);
        assertTrue(result.hasError(InputChecker.FIELD_EMAIL));
        assertTrue(result.hasError(InputChecker.FIELD_PASSWORD));
        assertTrue(result.hasError(InputChecker.FIELD_FULL_NAME));
        assertTrue(result.hasError(InputChecker.FIELD_PHONE));
    }

    @Test
    public void testValidatePhone_WithPlus1Format_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "+11234567890", "Password123!"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidatePhone_WithSpaces_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "123 456 7890", "Password123!"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidatePhone_WithHyphens_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "123-456-7890", "Password123!"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidationResult_DefaultConstructor_EmptyErrors() {
        InputChecker.ValidationResult result = new InputChecker.ValidationResult(true);

        assertNotNull(result.getError());
        assertTrue(result.getError().isEmpty());
        assertTrue(result.isValid());
    }

    @Test
    public void testValidationResult_WithErrors_ContainsErrors() {
        Map<String, String> errors = new java.util.HashMap<>();
        errors.put(InputChecker.FIELD_EMAIL, context.getString(R.string.email_invalid));
        InputChecker.ValidationResult result = new InputChecker.ValidationResult(false, errors);

        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().containsKey(InputChecker.FIELD_EMAIL));
        assertEquals(context.getString(R.string.email_invalid), result.getEmailError());
    }

    @Test
    public void testValidateLogin_EmailWithSpecialChars_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test.user+label@example.co.uk", "Password123!"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidateLogin_ValidComplexPassword_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "MyStr0ng#P@ssw0rd!"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidateSignup_PhoneWithParentheses_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "(123)456-7890", "Password123!"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidateSignup_PhoneWithPlus1AndSpaces_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateSignup(
                "test@example.com", "Password123!",
                "John Doe", "+1 234 567 8900", "Password123!"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidateLogin_PasswordWithAllRequirements_Valid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "Aa1!56789"
        );

        assertTrue(result.isValid());
    }

    @Test
    public void testValidateLogin_PasswordWithWhitespace_Invalid() {
        InputChecker.ValidationResult result = inputChecker.validateLogin(
                "test@example.com", "Pass word123!"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasError(InputChecker.FIELD_PASSWORD));
    }

    @Test
    public void testContactForm_email_returnsNoError_forValidEmail() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertNull(result.getEmailError());
    }

    @Test
    public void testContactForm_email_returnsError_forMissingDomain() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(INVALID_EMAIL_MSG, result.getEmailError());
    }

    @Test
    public void testContactForm_email_returnsError_forNoAtSymbol() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaabbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(INVALID_EMAIL_MSG, result.getEmailError());
    }

    @Test
    public void testContactForm_email_returnsError_forNumericGarbage() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "@2222", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(INVALID_EMAIL_MSG, result.getEmailError());
    }

    @Test
    public void testContactForm_email_returnsRequiredError_forEmptyString() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(REQUIRED_MSG, result.getEmailError());
    }

    @Test
    public void testContactForm_email_returnsRequiredError_forNull() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", null, "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(REQUIRED_MSG, result.getEmailError());
    }

    @Test
    public void testContactForm_email_validAndInvalid_areNotEqualResults() {
        boolean validResult = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG).isValid();
        boolean invalidResult = inputChecker.validateContactForm(
                "John Doe", "aaa@", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG).isValid();

        assertNotEquals(validResult, invalidResult);
    }

    @Test
    public void testContactForm_phone_returnsNoError_forValidPhoneNumber() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertNull(result.getPhoneError());
    }

    @Test
    public void testContactForm_phone_returnsError_forTooShortNumber() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", "123", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(INVALID_PHONE_MSG, result.getPhoneError());
    }

    @Test
    public void testContactForm_phone_returnsRequiredError_forEmptyString() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", "", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(REQUIRED_MSG, result.getPhoneError());
    }

    @Test
    public void testContactForm_phone_returnsRequiredError_forNull() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", null, "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(REQUIRED_MSG, result.getPhoneError());
    }

    @Test
    public void testContactForm_name_returnsNoError_forNonBlankString() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertNull(result.getNameError());
    }

    @Test
    public void testContactForm_name_returnsRequiredError_forWhitespaceOnly() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "   ", "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(REQUIRED_MSG, result.getNameError());
    }

    @Test
    public void testContactForm_name_returnsRequiredError_forNull() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                null, "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertEquals(REQUIRED_MSG, result.getNameError());
    }

    @Test
    public void testContactForm_returnsValid_whenAllFieldsCorrect() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    public void testContactForm_returnsInvalid_whenEmailMalformed() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "John Doe", "aaa@", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals(INVALID_EMAIL_MSG, result.getEmailError());
    }

    @Test
    public void testContactForm_setsNameError_whenNameEmpty() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "", "aaa@bbb.com", "4165551234", "Great app!",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertNotNull(result.getNameError());
        assertFalse(result.isValid());
    }

    @Test
    public void testContactForm_setsNoErrors_onFullyValidInput() {
        InputChecker.ValidationResult result = inputChecker.validateContactForm(
                "Jane Doe", "jane@example.com", "6475551234", "Loved the app",
                REQUIRED_MSG, INVALID_EMAIL_MSG, INVALID_PHONE_MSG);

        assertNull(result.getNameError());
        assertNull(result.getEmailError());
        assertNull(result.getPhoneError());
        assertNull(result.getMessageError());
    }
}