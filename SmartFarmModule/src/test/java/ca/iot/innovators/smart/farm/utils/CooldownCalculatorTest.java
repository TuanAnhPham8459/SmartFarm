package ca.iot.innovators.smart.farm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JUnit unit tests for CooldownCalculator.
 * Deliverable 5 - Requirement 18a/18b: JUnit test class #2.
 */
public class CooldownCalculatorTest {

    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;
    private static final long TWENTY_FOUR_HOURS_MS = 24 * ONE_HOUR_MS;

    @Test
    public void getRemainingMillis_returnsFullCooldown_whenJustSubmitted() {
        long now = 1_000_000L;
        long lastSubmit = now;
        long remaining = CooldownCalculator.getRemainingMillis(lastSubmit, now, TWENTY_FOUR_HOURS_MS);
        assertEquals(TWENTY_FOUR_HOURS_MS, remaining);
    }

    @Test
    public void getRemainingMillis_returnsZero_whenCooldownFullyElapsed() {
        long lastSubmit = 0L;
        long now = TWENTY_FOUR_HOURS_MS + ONE_HOUR_MS;
        long remaining = CooldownCalculator.getRemainingMillis(lastSubmit, now, TWENTY_FOUR_HOURS_MS);
        assertEquals(0L, remaining);
    }

    @Test
    public void getRemainingMillis_neverReturnsNegative() {
        long lastSubmit = 0L;
        long now = TWENTY_FOUR_HOURS_MS * 10;
        long remaining = CooldownCalculator.getRemainingMillis(lastSubmit, now, TWENTY_FOUR_HOURS_MS);
        assertTrue(remaining >= 0);
    }

    @Test
    public void getRemainingMillis_returnsPartialTime_whenHalfwayThrough() {
        long lastSubmit = 0L;
        long now = TWENTY_FOUR_HOURS_MS / 2;
        long remaining = CooldownCalculator.getRemainingMillis(lastSubmit, now, TWENTY_FOUR_HOURS_MS);
        assertEquals(TWENTY_FOUR_HOURS_MS / 2, remaining);
    }

    @Test
    public void isCooldownActive_returnsTrue_immediatelyAfterSubmit() {
        long now = 500_000L;
        assertTrue(CooldownCalculator.isCooldownActive(now, now, TWENTY_FOUR_HOURS_MS));
    }

    @Test
    public void isCooldownActive_returnsFalse_afterFullDayPasses() {
        long lastSubmit = 0L;
        long now = TWENTY_FOUR_HOURS_MS + 1;
        assertFalse(CooldownCalculator.isCooldownActive(lastSubmit, now, TWENTY_FOUR_HOURS_MS));
    }

    @Test
    public void isCooldownActive_activeAndExpired_areNotEqual() {
        long lastSubmit = 0L;
        boolean activeState = CooldownCalculator.isCooldownActive(lastSubmit, 1000L, TWENTY_FOUR_HOURS_MS);
        boolean expiredState = CooldownCalculator.isCooldownActive(lastSubmit, TWENTY_FOUR_HOURS_MS + 1, TWENTY_FOUR_HOURS_MS);
        assertNotEquals(activeState, expiredState);
    }

    @Test
    public void toHours_convertsMillisCorrectly() {
        long twoHours = 2 * ONE_HOUR_MS;
        assertEquals(2L, CooldownCalculator.toHours(twoHours));
    }

    @Test
    public void toMinutesRemainder_convertsMillisCorrectly() {
        long ninetyMinutes = 90 * 60 * 1000L;
        assertEquals(30L, CooldownCalculator.toMinutesRemainder(ninetyMinutes));
    }

    @Test
    public void formatRemaining_producesNonNullReadableString() {
        String formatted = CooldownCalculator.formatRemaining(TWENTY_FOUR_HOURS_MS);
        assertNotNull(formatted);
        assertTrue(formatted.contains("h"));
        assertTrue(formatted.contains("m"));
    }

    @Test
    public void formatRemaining_zeroMillis_showsZeroHoursZeroMinutes() {
        String formatted = CooldownCalculator.formatRemaining(0L);
        assertEquals("0h 0m", formatted);
    }
}