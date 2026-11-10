package ca.iot.innovators.smart.farm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import ca.iot.innovators.smart.farm.R;

public class Help extends Fragment {

    private MaterialButton btnContactSupport;
    private MaterialCardView cardDashboard;
    private MaterialCardView cardAlerts;

    public Help() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.activity_help, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        btnContactSupport = view.findViewById(R.id.btnContactSupport);
        cardDashboard = view.findViewById(R.id.cardDashboard);
        cardAlerts = view.findViewById(R.id.cardAlerts);

        // Dashboard help
        cardDashboard.setOnClickListener(v -> {
            Toast.makeText(
                    requireContext(),
                    R.string.the_dashboard_displays_real_time_farm_sensor_information,
                    Toast.LENGTH_SHORT
            ).show();

        });

        // Alert help
        cardAlerts.setOnClickListener(v -> {

            Toast.makeText(
                    requireContext(),
                    R.string.alert_help,
                    Toast.LENGTH_SHORT
            ).show();

        });

        // Contact support
        btnContactSupport.setOnClickListener(v -> {

            Intent emailIntent = new Intent(
                    Intent.ACTION_SENDTO
            );

            emailIntent.setData(
                    Uri.parse(getString(R.string.mailto_smartfarm_gmail_com))
            );

            emailIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.smartfarm_support)
            );

            try {

                startActivity(emailIntent);

            } catch (Exception e) {

                Toast.makeText(
                        requireContext(),
                        R.string.no_email_application_found,
                        Toast.LENGTH_SHORT
                ).show();

            }

        });
    }
}