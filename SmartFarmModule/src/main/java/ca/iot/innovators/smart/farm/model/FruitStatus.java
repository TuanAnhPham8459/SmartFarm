package ca.iot.innovators.smart.farm.model;

import androidx.annotation.StringRes;

import ca.iot.innovators.smart.farm.R;

/**
 * Ripeness state of a tomato, decided from the OPT4048 color reading.
 * Only the constants below exist; the constructor is private.
 */
public class FruitStatus {

    // Severity strings must match what AlertAdapter and AlertView compare against.
    private static final String SEVERITY_NORMAL = "Normal";
    private static final String SEVERITY_WARNING = "Warning";
    private static final String SEVERITY_CRITICAL = "Critical";

    public static final FruitStatus UNKNOWN = new FruitStatus(
            "UNKNOWN", R.string.fruit_unknown, 0, 0, SEVERITY_NORMAL, false);

    public static final FruitStatus UNRIPE = new FruitStatus(
            "UNRIPE", R.string.fruit_unripe, 0, 0, SEVERITY_NORMAL, false);

    public static final FruitStatus RIPENING = new FruitStatus(
            "RIPENING", R.string.fruit_ripening,
            R.string.alert_ripening_title, R.string.alert_ripening_msg, SEVERITY_NORMAL, true);

    public static final FruitStatus RIPE = new FruitStatus(
            "RIPE", R.string.fruit_ripe,
            R.string.alert_ripe_title, R.string.alert_ripe_msg, SEVERITY_WARNING, true);

    public static final FruitStatus OVERRIPE = new FruitStatus(
            "OVERRIPE", R.string.fruit_overripe,
            R.string.alert_overripe_title, R.string.alert_overripe_msg, SEVERITY_CRITICAL, true);

    private final String key;
    @StringRes private final int labelRes;
    @StringRes private final int alertTitleRes;
    @StringRes private final int alertMessageRes;
    private final String severity;
    private final boolean alertable;

    private FruitStatus(String key, int labelRes, int alertTitleRes, int alertMessageRes,
                        String severity, boolean alertable) {
        this.key = key;
        this.labelRes = labelRes;
        this.alertTitleRes = alertTitleRes;
        this.alertMessageRes = alertMessageRes;
        this.severity = severity;
        this.alertable = alertable;
    }

    /** Stable text id, used for SharedPreferences. */
    public String getKey() { return key; }

    @StringRes public int getLabelRes() { return labelRes; }
    @StringRes public int getAlertTitleRes() { return alertTitleRes; }
    @StringRes public int getAlertMessageRes() { return alertMessageRes; }
    public String getSeverity() { return severity; }

    /** True if this status should create an alert (ripening, ripe, overripe). */
    public boolean needsAlert() { return alertable; }

    @Override
    public String toString() { return key; }

    // ---------- Calibration constants (tune with your own tomatoes) ----------
    private static final double WHITE_X = 0.3127;
    private static final double WHITE_Y = 0.3290;
    private static final double MIN_COLOR_LUX = 1.0;
    private static final double MIN_CHROMA_DISTANCE = 0.03;
    private static final double UNRIPE_MIN_ANGLE = 70.0;
    private static final double RIPENING_MIN_ANGLE = 20.0;
    private static final double RED_MIN_ANGLE = -30.0;
    private static final double UNRIPE_MAX_ANGLE = 170.0;
    private static final double DARK_RED_MAX_LUX = 40.0;

    public static FruitStatus fromCie(double x, double y, double lux) {
        if (x <= 0 || y <= 0 || lux < MIN_COLOR_LUX) return UNKNOWN;

        double dx = x - WHITE_X;
        double dy = y - WHITE_Y;
        if (Math.hypot(dx, dy) < MIN_CHROMA_DISTANCE) return UNKNOWN;

        double angle = Math.toDegrees(Math.atan2(dy, dx));

        if (angle >= UNRIPE_MIN_ANGLE && angle <= UNRIPE_MAX_ANGLE) return UNRIPE;
        if (angle >= RIPENING_MIN_ANGLE && angle < UNRIPE_MIN_ANGLE) return RIPENING;
        if (angle >= RED_MIN_ANGLE && angle < RIPENING_MIN_ANGLE) {
            return lux < DARK_RED_MAX_LUX ? OVERRIPE : RIPE;
        }
        return UNKNOWN;
    }
}