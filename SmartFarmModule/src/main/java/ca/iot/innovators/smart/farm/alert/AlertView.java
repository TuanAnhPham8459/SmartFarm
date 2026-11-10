package ca.iot.innovators.smart.farm.alert;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.firestore.AlertModel;
import ca.iot.innovators.smart.farm.firestore.BaseFireStoreManager;
import ca.iot.innovators.smart.farm.firestore.FireStoreService;
import ca.iot.innovators.smart.farm.notification.NotificationPermissionHelper;

public class AlertView extends Fragment {
    private static final String TAG = "AlertView";
    private CompoundButton switchNotifications;
    private CompoundButton switchCriticalOnly;
    private TextView tvAlertSummary, tvSystemStatus;
    private MaterialCardView cardSystemStatus;
    private RecyclerView rvAlerts;
    private AlertAdapter alertAdapter;
    private final List<AlertModel> allAlerts = new ArrayList<>();
    private final List<AlertModel> displayedAlerts = new ArrayList<>();
    private ListenerRegistration alertListener;
    private NotificationPermissionHelper permissionHelper;

    public AlertView() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.alert, container, false);

        initializeViews(view);
        setupRecyclerView();

        permissionHelper = new NotificationPermissionHelper(this, new NotificationPermissionHelper.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                switchNotifications.setChecked(true);
                switchCriticalOnly.setEnabled(true);
            }

            @Override
            public void onPermissionDenied() {
                switchNotifications.setChecked(false);
                switchCriticalOnly.setEnabled(false);
            }

            @Override
            public void onPermissionError(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        permissionHelper.initializePermissionLauncher();
        boolean savedState = permissionHelper.getNotificationEnabled();

        switchNotifications.setChecked(savedState);
        switchCriticalOnly.setEnabled(savedState);
        switchCriticalOnly.setChecked(permissionHelper.getCriticalOnlyEnabled());

        setupListeners();
        startListeningForAlerts();

        return view;
    }

    private void initializeViews(View view) {
        switchNotifications = view.findViewById(R.id.switch_notifications);
        switchCriticalOnly = view.findViewById(R.id.switch_critical_only);
        tvAlertSummary = view.findViewById(R.id.tv_alert_summary);
        tvSystemStatus = view.findViewById(R.id.tv_system_status);
        cardSystemStatus = view.findViewById(R.id.card_system_status);
        rvAlerts = view.findViewById(R.id.rv_active_alerts);
    }

    private void setupRecyclerView() {
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        alertAdapter = new AlertAdapter(requireContext(), displayedAlerts);
        rvAlerts.setAdapter(alertAdapter);
    }

    private void setupListeners() {
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                permissionHelper.handleEnableNotification();
            } else {
                showToast(R.string.disable_notifications);
                switchCriticalOnly.setEnabled(false);
                permissionHelper.setNotificationEnabled(false);
            }
        });

        switchCriticalOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showSnackbar(R.string.critical_alerts_only);
            } else {
                showSnackbar(R.string.all_alerts);
            }
            permissionHelper.setCriticalOnlyEnabled(isChecked);
            filterAlerts();
        });

        if (!switchNotifications.isChecked()) {
            switchCriticalOnly.setEnabled(false);
        }
    }

    private void startListeningForAlerts() {
        String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
        if (deviceId == null) return;

        alertListener = FireStoreService.getInstance().getAlertManager()
                .listenToAlerts(deviceId, new BaseFireStoreManager.FireStoreCallbackWthData<List<AlertModel>>() {
                    @Override
                    public void onSuccess(List<AlertModel> alerts) {
                        if (!isAdded() || getContext() == null) return;

                        allAlerts.clear();
                        allAlerts.addAll(alerts);
                        filterAlerts();
                        updateSummary();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error fetching alerts: " + errorMessage);
                    }
                });
    }

    private void filterAlerts() {
        displayedAlerts.clear();
        boolean criticalOnly = switchCriticalOnly.isChecked();

        for (AlertModel alert : allAlerts) {
            if (!criticalOnly || "Critical".equalsIgnoreCase(alert.getSeverity())) {
                displayedAlerts.add(alert);
            }
        }
        alertAdapter.notifyDataSetChanged();

        // Show/hide system status card based on whether we have alerts
        if (displayedAlerts.isEmpty()) {
            cardSystemStatus.setVisibility(View.VISIBLE);
        } else {
            cardSystemStatus.setVisibility(View.GONE);
        }
    }

    private void updateSummary() {
        int count = allAlerts.size();
        if (count == 0) {
            tvAlertSummary.setText(R.string.fruit_no_alerts);
        } else {
            tvAlertSummary.setText(getString(R.string.fruit_alert_summary, count));
        }
    }

    public void refreshForNewDevice() {
        if (getView() == null || alertAdapter == null) return; // view not created yet

        if (alertListener != null) {
            alertListener.remove();
            alertListener = null;
        }
        allAlerts.clear();
        displayedAlerts.clear();
        alertAdapter.notifyDataSetChanged();
        updateSummary();
        filterAlerts();

        startListeningForAlerts();
    }

    private void showSnackbar(int messageResId) {
        String message = getString(messageResId);
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
    }

    private void showToast(int messageResId) {
        Toast.makeText(getContext(), messageResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            startListeningForAlerts();
        } else {
            if (alertListener != null) {
                alertListener.remove();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertListener != null) {
            alertListener.remove();
        }
    }
}