package ca.iot.innovators.smart.farm.alert;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class AlertViewStateTest {

    private AlertViewState alertState;

    @Before
    public void setUp() {
        alertState = new AlertViewState(false, false);
    }

    @Test
    public void notifications_shouldInitiallyBeDisabled() {

        assertFalse(alertState.isNotificationsEnabled());
    }

    @Test
    public void criticalOnly_shouldInitiallyBeDisabled() {

        assertFalse(alertState.isCriticalOnlyEnabled());
    }

    @Test
    public void criticalOnly_shouldInitiallyBeUnavailable() {

        assertFalse(alertState.isCriticalOnlyAvailable());
    }

    @Test
    public void enablingNotifications_shouldEnableCriticalOnlyAvailability() {

        alertState.setNotificationsEnabled(true);

        assertTrue(alertState.isNotificationsEnabled());
        assertTrue(alertState.isCriticalOnlyAvailable());
    }

    @Test
    public void enablingNotifications_shouldNotAutomaticallyEnableCriticalOnly() {

        alertState.setNotificationsEnabled(true);

        assertFalse(alertState.isCriticalOnlyEnabled());
    }

    @Test
    public void enablingCriticalOnly_shouldWorkWhenNotificationsAreEnabled() {

        alertState.setNotificationsEnabled(true);

        alertState.setCriticalOnlyEnabled(true);

        assertTrue(alertState.isCriticalOnlyEnabled());
    }

    @Test
    public void disablingCriticalOnly_shouldWork() {

        alertState.setNotificationsEnabled(true);
        alertState.setCriticalOnlyEnabled(true);

        alertState.setCriticalOnlyEnabled(false);

        assertFalse(alertState.isCriticalOnlyEnabled());
    }

    @Test
    public void criticalOnly_shouldNotEnableWhenNotificationsAreDisabled() {

        alertState.setNotificationsEnabled(false);

        alertState.setCriticalOnlyEnabled(true);

        assertFalse(alertState.isCriticalOnlyEnabled());
    }

    @Test
    public void disablingNotifications_shouldDisableCriticalOnly() {

        alertState.setNotificationsEnabled(true);
        alertState.setCriticalOnlyEnabled(true);

        alertState.setNotificationsEnabled(false);

        assertFalse(alertState.isNotificationsEnabled());
        assertFalse(alertState.isCriticalOnlyEnabled());
    }

    @Test
    public void disablingNotifications_shouldMakeCriticalOnlyUnavailable() {

        alertState.setNotificationsEnabled(true);

        alertState.setNotificationsEnabled(false);

        assertFalse(alertState.isCriticalOnlyAvailable());
    }

    @Test
    public void reEnablingNotifications_shouldKeepCriticalOnlyDisabled() {

        alertState.setNotificationsEnabled(true);
        alertState.setCriticalOnlyEnabled(true);

        alertState.setNotificationsEnabled(false);
        alertState.setNotificationsEnabled(true);

        assertTrue(alertState.isNotificationsEnabled());
        assertFalse(alertState.isCriticalOnlyEnabled());
    }

    @Test
    public void initialEnabledState_shouldBePreserved() {

        alertState = new AlertViewState(true, true);

        assertTrue(alertState.isNotificationsEnabled());
        assertTrue(alertState.isCriticalOnlyEnabled());
        assertTrue(alertState.isCriticalOnlyAvailable());
    }

    @Test
    public void criticalOnlyInitialState_shouldBeFalseWhenNotificationsAreDisabled() {

        alertState = new AlertViewState(false, true);

        assertFalse(alertState.isNotificationsEnabled());
        assertFalse(alertState.isCriticalOnlyEnabled());
        assertFalse(alertState.isCriticalOnlyAvailable());
    }
}

