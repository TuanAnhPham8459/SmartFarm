// ScreenTrackingManager.java - Simplified time-based tracking
package ca.iot.innovators.smart.farm.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ScreenTrackingManager {
    private static final String TAG = "ScreenTrackingManager";
    private static final String COLLECTION_NAME = "screen_tracking";
    private static ScreenTrackingManager instance;
    private final FirebaseFirestore db;

    // Date formatters
    private static final SimpleDateFormat DATE_FORMAT_DAY =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_FORMAT_MONTH =
            new SimpleDateFormat("yyyy-MM", Locale.US);

    // Screen names constants
    public static final String SCREEN_HOME = "home";
    public static final String SCREEN_DASHBOARD = "dashboard";
    public static final String SCREEN_SETTINGS = "settings";
    public static final String SCREEN_ALERT = "alert";
    public static final String SCREEN_ACCOUNT = "account";
    public static final String SCREEN_FEEDBACK = "feedback";
    public static final String SCREEN_CONTROL_PANEL = "control_panel";
    public static final String SCREEN_GRAPH = "graph";

    private ScreenTrackingManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized ScreenTrackingManager getInstance() {
        if (instance == null) {
            instance = new ScreenTrackingManager();
        }
        return instance;
    }

    /**
     * Track a screen click - Stores daily and monthly aggregates
     * Data structure:
     * screen_tracking/
     *   └── {screen_name}/
     *       ├── total: 150
     *       ├── daily/
     *       │   ├── 2026-02-01: 45
     *       │   ├── 2026-02-02: 38
     *       │   └── ...
     *       ├── monthly/
     *       │   ├── 2026-01: 120
     *       │   ├── 2026-02: 95
     *       │   └── ...
     *       └── last_updated: timestamp
     */
    public void trackScreenClick(String screenName) {
        try {
            Date now = new Date();
            String dayKey = DATE_FORMAT_DAY.format(now);
            String monthKey = DATE_FORMAT_MONTH.format(now);

            DocumentReference docRef = db.collection(COLLECTION_NAME)
                    .document(screenName);

            // Build update map with nested fields
            Map<String, Object> updates = new HashMap<>();
            updates.put("total", FieldValue.increment(1));
            updates.put("daily." + dayKey, FieldValue.increment(1));
            updates.put("monthly." + monthKey, FieldValue.increment(1));
            updates.put("last_updated", FieldValue.serverTimestamp());
            updates.put("screen_name", screenName);

            // Merge with existing document
            docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Screen click tracked: " + screenName)
                    )
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to track screen: " + e.getMessage())
                    );

        } catch (Exception e) {
            Log.e(TAG, "Error tracking screen: " + e.getMessage());
        }
    }

    /**
     * Get screen counts for the last N days (simplified)
     */
    public void getScreenCountsLastDays(String screenName, int days, CountsCallback callback) {
        try {
            // Calculate start date
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.add(Calendar.DAY_OF_YEAR, -days);
            Date startDate = calendar.getTime();

            DocumentReference docRef = db.collection(COLLECTION_NAME)
                    .document(screenName);

            docRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> data = documentSnapshot.getData();
                            Map<String, Object> dailyMap = (Map<String, Object>) data.get("daily");

                            if (dailyMap == null) {
                                callback.onSuccess(createEmptyResult(screenName, days));
                                return;
                            }

                            // Calculate total for last N days
                            long totalCount = 0;
                            Map<String, Long> dailyCounts = new HashMap<>();

                            for (Map.Entry<String, Object> entry : dailyMap.entrySet()) {
                                String dateKey = entry.getKey();

                                // Check if date is within range
                                try {
                                    Date entryDate = DATE_FORMAT_DAY.parse(dateKey);
                                    if (entryDate != null && entryDate.after(startDate)) {
                                        Long count = (Long) entry.getValue();
                                        if (count != null) {
                                            totalCount += count;
                                            dailyCounts.put(dateKey, count);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing date: " + dateKey);
                                }
                            }

                            // Prepare result
                            Map<String, Object> result = new HashMap<>();
                            result.put("screen", screenName);
                            result.put("period_days", days);
                            result.put("total_count", totalCount);
                            result.put("daily_counts", dailyCounts);
                            result.put("start_date", DATE_FORMAT_DAY.format(startDate));
                            result.put("end_date", DATE_FORMAT_DAY.format(new Date()));

                            callback.onSuccess(result);

                        } else {
                            callback.onSuccess(createEmptyResult(screenName, days));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get counts: " + e.getMessage());
                        callback.onError(e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error getting screen counts: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Get screen counts for the last 7 days
     */
    public void getScreenCountsLast7Days(String screenName, CountsCallback callback) {
        getScreenCountsLastDays(screenName, 7, callback);
    }

    /**
     * Get screen counts for the last 30 days (last month)
     */
    public void getScreenCountsLastMonth(String screenName, CountsCallback callback) {
        getScreenCountsLastDays(screenName, 30, callback);
    }

    /**
     * Get total counts for all screens in the last 7 days
     */
    public void getAllScreenCountsLast7Days(AllCountsCallback callback) {
        try {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            Date startDate = calendar.getTime();

            db.collection(COLLECTION_NAME)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Map<String, Object> allResults = new HashMap<>();
                        Map<String, Long> screenTotals = new HashMap<>();
                        long grandTotal = 0;

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String screenName = document.getId();
                            Map<String, Object> data = document.getData();
                            Map<String, Object> dailyMap = (Map<String, Object>) data.get("daily");

                            if (dailyMap != null) {
                                long screenTotal = 0;

                                for (Map.Entry<String, Object> entry : dailyMap.entrySet()) {
                                    try {
                                        Date entryDate = DATE_FORMAT_DAY.parse(entry.getKey());
                                        if (entryDate != null && entryDate.after(startDate)) {
                                            Long count = (Long) entry.getValue();
                                            if (count != null) {
                                                screenTotal += count;
                                            }
                                        }
                                    } catch (Exception e) {
                                        // Skip invalid dates
                                    }
                                }

                                if (screenTotal > 0) {
                                    screenTotals.put(screenName, screenTotal);
                                    grandTotal += screenTotal;
                                }
                            }
                        }

                        allResults.put("period", "last_7_days");
                        allResults.put("start_date", DATE_FORMAT_DAY.format(startDate));
                        allResults.put("end_date", DATE_FORMAT_DAY.format(new Date()));
                        allResults.put("screen_totals", screenTotals);
                        allResults.put("grand_total", grandTotal);
                        allResults.put("total_screens_tracked", screenTotals.size());

                        callback.onSuccess(allResults);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get all screen counts: " + e.getMessage());
                        callback.onError(e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error getting all screen counts: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Get detailed counts for a specific month
     */
    public void getScreenCountsMonth(String screenName, int month, int year, CountsCallback callback) {
        try {
            String monthKey = String.format(Locale.US, "%d-%02d", year, month);

            DocumentReference docRef = db.collection(COLLECTION_NAME)
                    .document(screenName);

            docRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> data = documentSnapshot.getData();
                            Map<String, Object> monthlyMap = (Map<String, Object>) data.get("monthly");

                            if (monthlyMap != null && monthlyMap.containsKey(monthKey)) {
                                Long count = (Long) monthlyMap.get(monthKey);
                                Map<String, Object> result = new HashMap<>();
                                result.put("screen", screenName);
                                result.put("month", monthKey);
                                result.put("count", count != null ? count : 0);
                                callback.onSuccess(result);
                            } else {
                                callback.onSuccess(createEmptyMonthResult(screenName, monthKey));
                            }
                        } else {
                            callback.onSuccess(createEmptyMonthResult(screenName, monthKey));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get month count: " + e.getMessage());
                        callback.onError(e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error getting month count: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Get today's count for a screen
     */
    public void getTodayCount(String screenName, CountsCallback callback) {
        try {
            String todayKey = DATE_FORMAT_DAY.format(new Date());

            DocumentReference docRef = db.collection(COLLECTION_NAME)
                    .document(screenName);

            docRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> data = documentSnapshot.getData();
                            Map<String, Object> dailyMap = (Map<String, Object>) data.get("daily");

                            Long todayCount = 0L;
                            if (dailyMap != null && dailyMap.containsKey(todayKey)) {
                                todayCount = (Long) dailyMap.get(todayKey);
                            }

                            Map<String, Object> result = new HashMap<>();
                            result.put("screen", screenName);
                            result.put("date", todayKey);
                            result.put("count", todayCount != null ? todayCount : 0);
                            callback.onSuccess(result);
                        } else {
                            callback.onSuccess(createEmptyResult(screenName, 0));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get today count: " + e.getMessage());
                        callback.onError(e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error getting today count: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> createEmptyResult(String screenName, int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("screen", screenName);
        result.put("period_days", days);
        result.put("total_count", 0);
        result.put("daily_counts", new HashMap<>());
        result.put("start_date", "");
        result.put("end_date", DATE_FORMAT_DAY.format(new Date()));
        return result;
    }

    private Map<String, Object> createEmptyMonthResult(String screenName, String monthKey) {
        Map<String, Object> result = new HashMap<>();
        result.put("screen", screenName);
        result.put("month", monthKey);
        result.put("count", 0);
        return result;
    }

    // ==================== INTERFACES ====================

    public interface CountsCallback {
        void onSuccess(Map<String, Object> counts);
        void onError(String error);
    }

    public interface AllCountsCallback {
        void onSuccess(Map<String, Object> allCounts);
        void onError(String error);
    }
}