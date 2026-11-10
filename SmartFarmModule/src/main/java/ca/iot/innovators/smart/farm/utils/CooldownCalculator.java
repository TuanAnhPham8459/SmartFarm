package ca.iot.innovators.smart.farm.utils;

/**
 * Pure, Android-independent logic for computing feedback submission cooldown
 * state. Extracted from Feedback.java so it can be unit tested with plain
 * JUnit (no Robolectric/instrumentation needed).
 */
public class CooldownCalculator {

    public static final long DEFAULT_COOLDOWN_MS = 24 * 60 * 60 * 1000L;

    private CooldownCalculator() {
        // Utility class, no instances.
    }

    /**
     * @return milliseconds remaining until the cooldown expires, or 0 if it
     * has already expired.
     */
    public static long getRemainingMillis(long lastSubmitMillis, long nowMillis, long cooldownMs) {
        long elapsed = nowMillis - lastSubmitMillis;
        long remaining = cooldownMs - elapsed;
        return remaining > 0 ? remaining : 0;
    }

    public static boolean isCooldownActive(long lastSubmitMillis, long nowMillis, long cooldownMs) {
        return getRemainingMillis(lastSubmitMillis, nowMillis, cooldownMs) > 0;
    }

    public static long toHours(long millis) {
        return (millis / (60 * 1000)) / 60;
    }

    public static long toMinutesRemainder(long millis) {
        return (millis / (60 * 1000)) % 60;
    }

    public static String formatRemaining(long millis) {
        return toHours(millis) + "h " + toMinutesRemainder(millis) + "m";
    }
}