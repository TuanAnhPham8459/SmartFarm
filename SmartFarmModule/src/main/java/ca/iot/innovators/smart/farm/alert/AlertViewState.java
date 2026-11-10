package ca.iot.innovators.smart.farm.alert;

public class AlertViewState {

    private boolean notificationsEnabled;
    private boolean criticalOnlyEnabled;

    public AlertViewState(
            boolean notificationsEnabled,
            boolean criticalOnlyEnabled) {

        this.notificationsEnabled = notificationsEnabled;

        // Critical Only cannot be enabled if notifications are disabled
        this.criticalOnlyEnabled =
                notificationsEnabled && criticalOnlyEnabled;
    }

    public void setNotificationsEnabled(boolean enabled) {

        notificationsEnabled = enabled;

        // When notifications are disabled,
        // Critical Only must also be disabled.
        if (!enabled) {
            criticalOnlyEnabled = false;
        }
    }

    public void setCriticalOnlyEnabled(boolean enabled) {

        // Critical Only can only be enabled
        // when notifications are enabled.
        if (notificationsEnabled) {
            criticalOnlyEnabled = enabled;
        } else {
            criticalOnlyEnabled = false;
        }
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isCriticalOnlyEnabled() {
        return criticalOnlyEnabled;
    }

    public boolean isCriticalOnlyAvailable() {
        return notificationsEnabled;
    }
}