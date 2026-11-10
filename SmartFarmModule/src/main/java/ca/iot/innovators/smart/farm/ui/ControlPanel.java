package ca.iot.innovators.smart.farm.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.content.Context;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.firestore.ListenerRegistration;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.firestore.BaseFireStoreManager;
import ca.iot.innovators.smart.farm.firestore.DeviceControlModel;
import ca.iot.innovators.smart.farm.firestore.FireStoreService;


public class ControlPanel extends Fragment {

    private static final String TAG = "ControlPanel";
    private MaterialSwitch switchPump;
    private MaterialSwitch switchFan;
    private MaterialSwitch switchLight;
    private MaterialSwitch switchAutoMode;

    private TextView pumpStatus;
    private TextView fanStatus;
    private TextView lightStatus;

    private ListenerRegistration controlListener;

    public void refreshForNewDevice() {
        if (!isAdded()) return;
        
        // Reset UI labels
        if (pumpStatus != null) pumpStatus.setText("--");
        if (fanStatus != null) fanStatus.setText("--");
        if (lightStatus != null) lightStatus.setText("--");
        
        // Restart listener
        startListeningToControls();
    }


    public ControlPanel(){

        super(R.layout.control_panel);

    }




    @Override
    public void onViewCreated(
            @NonNull android.view.View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);


        initializeViews(view);
        setupSwitches();
        startListeningToControls();
    }

    private void startListeningToControls() {
        String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
        if (deviceId == null) {
            Log.d(TAG, "No device selected for controls");
            setControlsEnabled(false);
            return;
        }

        setControlsEnabled(true);
        controlListener = FireStoreService.getInstance().getDeviceControlManager()
                .listenToControls(deviceId, new BaseFireStoreManager.FireStoreCallbackWthData<DeviceControlModel>() {
                    @Override
                    public void onSuccess(DeviceControlModel data) {
                        if (data != null) {
                            updateUIFromModel(data);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error listening to controls: " + errorMessage);
                    }
                });
    }

    private void updateUIFromModel(DeviceControlModel data) {
        switchPump.setChecked(data.isPump());
        switchFan.setChecked(data.isFan());
        switchLight.setChecked(data.isLight());
        switchAutoMode.setChecked(data.isAutoMode());

        updateStatusLabels(data.isPump(), data.isFan(), data.isLight());
    }

    private void updateStatusLabels(boolean pump, boolean fan, boolean light) {
        // Pump
        if (pump) {
            pumpStatus.setText("ON");
            pumpStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        } else {
            pumpStatus.setText("OFF");
            pumpStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        }

        // Fan
        fanStatus.setText(fan ? "ON" : "OFF");
        fanStatus.setTextColor(fan ? ContextCompat.getColor(requireContext(), R.color.green) : ContextCompat.getColor(requireContext(), android.R.color.darker_gray));

        // Light
        lightStatus.setText(light ? "ON" : "OFF");
        lightStatus.setTextColor(light ? ContextCompat.getColor(requireContext(), R.color.status_ok) : ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
    }

    private void setControlsEnabled(boolean enabled) {
        switchPump.setEnabled(enabled);
        switchFan.setEnabled(enabled);
        switchLight.setEnabled(enabled);
        switchAutoMode.setEnabled(enabled);
    }







    private void initializeViews(android.view.View view){


        switchPump =
                view.findViewById(R.id.switchPump);


        switchFan =
                view.findViewById(R.id.switchFan);


        switchLight =
                view.findViewById(R.id.switchLight);


        switchAutoMode =
                view.findViewById(R.id.switchAutoMode);



        pumpStatus =
                view.findViewById(R.id.tvPumpStatus);


        fanStatus =
                view.findViewById(R.id.tvFanStatus);


        lightStatus =
                view.findViewById(R.id.tvLightStatus);


    }







    @SuppressLint("SetTextI18n")
    private void setupSwitches(){

        switchPump.setOnClickListener(v -> {
            String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
            boolean isChecked = switchPump.isChecked();
            if (deviceId != null) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(requireContext(), "Offline: Command queued", Toast.LENGTH_SHORT).show();
                }
                FireStoreService.getInstance().getDeviceControlManager()
                        .updateControlField(deviceId, "pump", isChecked);
            }

            if (isChecked) {
                pumpStatus.setText("ON");
                pumpStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
                Toast.makeText(requireContext(), "Irrigation Pump ON", Toast.LENGTH_SHORT).show();
            }
            else{
                pumpStatus.setText("OFF");
                pumpStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                Toast.makeText(requireContext(), "Irrigation Pump OFF", Toast.LENGTH_SHORT).show();
            }
        });

        switchFan.setOnClickListener(v -> {
            String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
            boolean isChecked = switchFan.isChecked();
            if (deviceId != null) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(requireContext(), "Offline: Command queued", Toast.LENGTH_SHORT).show();
                }
                FireStoreService.getInstance().getDeviceControlManager()
                        .updateControlField(deviceId, "fan", isChecked);
            }

            fanStatus.setText(isChecked ? "ON" : "OFF");
            fanStatus.setTextColor(isChecked ? ContextCompat.getColor(requireContext(), R.color.green) : ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            Toast.makeText(requireContext(), isChecked ? "Cooling Fan ON" : "Cooling Fan OFF", Toast.LENGTH_SHORT).show();
        });

        switchLight.setOnClickListener(v -> {
            String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
            boolean isChecked = switchLight.isChecked();
            if (deviceId != null) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(requireContext(), "Offline: Command queued", Toast.LENGTH_SHORT).show();
                }
                FireStoreService.getInstance().getDeviceControlManager()
                        .updateControlField(deviceId, "light", isChecked);
            }

            lightStatus.setText(isChecked ? "ON" : "OFF");
            lightStatus.setTextColor(isChecked ? ContextCompat.getColor(requireContext(), R.color.status_ok) : ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            Toast.makeText(requireContext(), isChecked ? "Grow Light ON" : "Grow Light OFF", Toast.LENGTH_SHORT).show();
        });

        switchAutoMode.setOnClickListener(v -> {
            String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
            boolean isChecked = switchAutoMode.isChecked();
            if (deviceId != null) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(requireContext(), "Offline: Command queued", Toast.LENGTH_SHORT).show();
                }
                FireStoreService.getInstance().getDeviceControlManager()
                        .updateControlField(deviceId, "autoMode", isChecked);
            }

            Toast.makeText(requireContext(), isChecked ? "Auto Mode Enabled" : "Manual Mode Enabled", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Re-sync with Firestore when fragment becomes visible
            startListeningToControls();
        } else {
            // Stop listening when hidden to save resources
            if (controlListener != null) {
                controlListener.remove();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (controlListener != null) {
            controlListener.remove();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
}
