/** CENG-322-0CA
 * Chau Vi Cuong Nguyen N01634611
 * Hoang Tran N01613506
 * Truong An Nguyen N01639800
 * Tuan Anh Pham N01238459
 **/
package ca.iot.innovators.smart.farm.ui;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.alert.AlertAdapter;
import ca.iot.innovators.smart.farm.firestore.AlertModel;
import ca.iot.innovators.smart.farm.firestore.BaseFireStoreManager;
import ca.iot.innovators.smart.farm.firestore.FireStoreService;
import ca.iot.innovators.smart.farm.firestore.SensorDataModel;
import ca.iot.innovators.smart.farm.model.FruitStatus;
import ca.iot.innovators.smart.farm.utils.UnitUtils;

public class Home extends Fragment {

    private static final String TAG = "HomeFragment";

    private static final int STABLE_READINGS_REQUIRED = 3;
    private static final String PREF_LAST_FRUIT_STATUS = "last_fruit_status_";

    private TextView tvTemperatureValue;
    private TextView tvHumidityValue;
    private TextView tvLightValue;
    private TextView tvFruitStatusValue;
    private ImageView btnRefresh;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvRecentAlerts;
    private AlertAdapter alertAdapter;
    private final LinkedList<AlertModel> recentAlerts = new LinkedList<>();

    // Alerts from Firestore, and the local "Fruit detected" alert shown on top (RFID card present).
    private final List<AlertModel> firestoreAlerts = new ArrayList<>();
    private AlertModel fruitDetectedAlert = null;
    private String lastRfidUid = "";

    private ListenerRegistration sensorListener;
    private ListenerRegistration deviceListener;
    private ListenerRegistration alertListener;

    private ImageView ivConnectionIcon, ivSignalIcon;
    private TextView tvDeviceName, tvOnlineStatus, tvLastUpdated;
    private long lastReceivedTimestamp = 0;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private Runnable statusRunnable;

    private FruitStatus pendingStatus = FruitStatus.UNKNOWN;
    private int pendingCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        startListeningToSensors();
        startListeningToAlerts();
    }

    private void initializeViews(View view) {
        tvTemperatureValue = view.findViewById(R.id.tvTemperatureValue);
        tvHumidityValue = view.findViewById(R.id.tvHumidityValue);
        tvLightValue = view.findViewById(R.id.tvLightValue);
        tvFruitStatusValue = view.findViewById(R.id.tvFruitStatusValue);

        ivConnectionIcon = view.findViewById(R.id.ivConnectionIcon);
        ivSignalIcon = view.findViewById(R.id.ivSignalIcon);
        tvDeviceName = view.findViewById(R.id.tvDeviceName);
        tvOnlineStatus = view.findViewById(R.id.tvOnlineStatus);
        tvLastUpdated = view.findViewById(R.id.tvSub);

        rvRecentAlerts = view.findViewById(R.id.rvRecentAlerts);
        rvRecentAlerts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        alertAdapter = new AlertAdapter(requireContext(), recentAlerts);
        rvRecentAlerts.setAdapter(alertAdapter);
        rvRecentAlerts.setNestedScrollingEnabled(false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Dashboard Updated", Toast.LENGTH_SHORT).show();
            }, 1000);
        });

        btnRefresh = view.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            swipeRefreshLayout.setRefreshing(true);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Refreshing data...", Toast.LENGTH_SHORT).show();
            }, 1000);
        });

        startStatusChecker();
        listenToDeviceMetadata();
    }

    private void startStatusChecker() {
        statusRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (lastReceivedTimestamp > 0 && currentTime - lastReceivedTimestamp > 60000) {
                    updateConnectionUI(false);
                }
                updateLastUpdatedTime();
                statusHandler.postDelayed(this, 30000);
            }
        };
        statusHandler.post(statusRunnable);
    }

    private void updateLastUpdatedTime() {
        if (lastReceivedTimestamp > 0 && tvLastUpdated != null) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    lastReceivedTimestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            tvLastUpdated.setText("Last updated: " + relativeTime);
        }
    }

    private void listenToDeviceMetadata() {
        String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
        if (deviceId == null) return;

        deviceListener = FireStoreService.getInstance().getDeviceManager().getDb()
                .collection("devices")
                .document(deviceId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    String name = snapshot.getString("deviceName");
                    if (name != null && tvDeviceName != null) {
                        tvDeviceName.setText(name);
                    }
                });
    }

    private void updateConnectionUI(boolean isOnline) {
        if (tvOnlineStatus == null || ivConnectionIcon == null || ivSignalIcon == null) return;

        if (isOnline) {
            tvOnlineStatus.setText(R.string.online);
            tvOnlineStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_ok));
            ivSignalIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_ok));
            ivConnectionIcon.setAlpha(1.0f);
        } else {
            tvOnlineStatus.setText(R.string.offline);
            tvOnlineStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            ivSignalIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            ivConnectionIcon.setAlpha(0.5f);
        }
    }

    private void startListeningToSensors() {
        String deviceId = FireStoreService.getInstance().getSelectedDeviceId();

        if (deviceId == null || deviceId.isEmpty()) {
            tvTemperatureValue.setText("--");
            tvHumidityValue.setText("--");
            tvLightValue.setText("--");
            tvFruitStatusValue.setText(R.string.fruit_unknown);
            return;
        }

        if (sensorListener != null) {
            sensorListener.remove();
        }

        sensorListener = FireStoreService.getInstance().getSensorDataManager()
                .listenToSensorData(deviceId, new BaseFireStoreManager.FireStoreCallbackWthData<SensorDataModel>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(SensorDataModel data) {
                        if (!isAdded() || getContext() == null) return;

                        if (data != null) {
                            lastReceivedTimestamp = System.currentTimeMillis();
                            updateConnectionUI(true);
                            updateLastUpdatedTime();

                            boolean useFahrenheit = UnitUtils.isFahrenheit(requireContext());
                            double temp = UnitUtils.convertTemperature(data.getTemperature(), useFahrenheit);
                            String unit = UnitUtils.getTemperatureUnit(useFahrenheit);

                            tvTemperatureValue.setText(String.format(Locale.getDefault(), "%.1f%s", temp, unit));
                            tvHumidityValue.setText(String.format(Locale.getDefault(), "%.1f%%", data.getHumidity()));
                            tvLightValue.setText(String.format(Locale.getDefault(), "%.0f lx", data.getLight()));

                            FruitStatus fruitStatus = FruitStatus.fromCie(
                                    data.getCieX(), data.getCieY(), data.getColorLux());
                            updateFruitStatusUI(fruitStatus);
                            handleFruitAlert(deviceId, fruitStatus);

                            updateFruitDetectedAlert(data);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error fetching sensor data: " + errorMessage);
                    }
                });
    }

    private void updateFruitStatusUI(FruitStatus status) {
        tvFruitStatusValue.setText(status.getLabelRes());

        int colorRes;
        if (status == FruitStatus.UNRIPE) {
            colorRes = R.color.status_ok;
        } else if (status == FruitStatus.RIPENING) {
            colorRes = R.color.status_warning;
        } else if (status == FruitStatus.RIPE || status == FruitStatus.OVERRIPE) {
            colorRes = R.color.status_error;
        } else {
            colorRes = R.color.text_secondary;
        }
        tvFruitStatusValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    /**
     * Saves an alert only after the same status is seen STABLE_READINGS_REQUIRED times in a row,
     * and only if it differs from the last status already alerted for this device.
     */
    private void handleFruitAlert(String deviceId, FruitStatus status) {
        if (status == pendingStatus) {
            pendingCount++;
        } else {
            pendingStatus = status;
            pendingCount = 1;
        }
        if (pendingCount != STABLE_READINGS_REQUIRED || status == FruitStatus.UNKNOWN) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String key = PREF_LAST_FRUIT_STATUS + deviceId;
        if (status.getKey().equals(prefs.getString(key, ""))) return;
        prefs.edit().putString(key, status.getKey()).apply();

        if (!status.needsAlert()) return;

        AlertModel alert = new AlertModel(
                getString(status.getAlertTitleRes()),
                getString(status.getAlertMessageRes()),
                status.getSeverity());

        FireStoreService.getInstance().getAlertManager().saveAlert(deviceId, alert,
                new BaseFireStoreManager.FireStoreCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Fruit alert saved: " + status);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to save fruit alert: " + errorMessage);
                    }
                });
    }

    /**
     * Shows a green "Fruit detected / Tomato" alert on top of Recent Alerts
     * while the RFID card is present. It is local only and is not saved to Firestore.
     */
    private void updateFruitDetectedAlert(SensorDataModel data) {
        String uid = data.getRfidUid() != null ? data.getRfidUid() : "";

        if (data.isRfidTagPresent()) {
            if (fruitDetectedAlert == null || !uid.equals(lastRfidUid)) {
                AlertModel alert = new AlertModel(
                        getString(R.string.fruit_detected_title),
                        getString(R.string.fruit_detected_msg),
                        "Normal");
                if (data.getTimestamp() > 0) {
                    alert.setTimestamp(data.getTimestamp());
                }
                fruitDetectedAlert = alert;
                lastRfidUid = uid;
            }
        } else {
            fruitDetectedAlert = null;
            lastRfidUid = "";
        }
        refreshRecentAlerts();
    }

    /** Rebuilds the Recent Alerts list: local fruit-detected alert first, then top 5 from Firestore. */
    private void refreshRecentAlerts() {
        if (alertAdapter == null || rvRecentAlerts == null) return;

        recentAlerts.clear();
        if (fruitDetectedAlert != null) {
            recentAlerts.add(fruitDetectedAlert);
        }
        int count = Math.min(firestoreAlerts.size(), 5);
        for (int i = 0; i < count; i++) {
            recentAlerts.add(firestoreAlerts.get(i));
        }
        alertAdapter.notifyDataSetChanged();
        rvRecentAlerts.requestLayout();
    }

    /** Called by MainActivity when the selected device changes. */
    public void refreshForNewDevice() {
        if (getView() == null || tvTemperatureValue == null) return; // view not created yet

        // Clear old device state
        if (deviceListener != null) {
            deviceListener.remove();
            deviceListener = null;
        }
        firestoreAlerts.clear();
        fruitDetectedAlert = null;
        lastRfidUid = "";
        pendingStatus = FruitStatus.UNKNOWN;
        pendingCount = 0;
        lastReceivedTimestamp = 0;

        tvTemperatureValue.setText("--");
        tvHumidityValue.setText("--");
        tvLightValue.setText("--");
        tvFruitStatusValue.setText(R.string.fruit_unknown);
        tvFruitStatusValue.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary));
        refreshRecentAlerts();

        // Start listening to the newly selected device
        startListeningToSensors();
        listenToDeviceMetadata();
        startListeningToAlerts();
    }

    private void startListeningToAlerts() {
        String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
        if (deviceId == null) return;

        if (alertListener != null) alertListener.remove();

        alertListener = FireStoreService.getInstance().getAlertManager()
                .listenToAlerts(deviceId, new BaseFireStoreManager.FireStoreCallbackWthData<List<AlertModel>>() {
                    @Override
                    public void onSuccess(List<AlertModel> alerts) {
                        if (!isAdded() || getContext() == null) return;

                        rvRecentAlerts.post(() -> {
                            firestoreAlerts.clear();
                            firestoreAlerts.addAll(alerts);
                            refreshRecentAlerts();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error fetching alerts: " + errorMessage);
                    }
                });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            startListeningToSensors();
            listenToDeviceMetadata();
            startListeningToAlerts();
        } else {
            if (sensorListener != null) sensorListener.remove();
            if (deviceListener != null) deviceListener.remove();
            if (alertListener != null) alertListener.remove();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sensorListener != null) {
            sensorListener.remove();
        }
        if (deviceListener != null) {
            deviceListener.remove();
        }
        if (alertListener != null) {
            alertListener.remove();
        }
        if (statusRunnable != null) {
            statusHandler.removeCallbacks(statusRunnable);
        }
    }
}