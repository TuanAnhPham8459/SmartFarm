package ca.iot.innovators.smart.farm.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.utils.ThemeUtils;


public class Settings extends PreferenceFragmentCompat {


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(
                R.xml.settings,
                rootKey
        );


        SwitchPreferenceCompat portrait =
                findPreference("portrait_lock");


        if (portrait != null) {

            portrait.setOnPreferenceChangeListener(
                    (preference, newValue) -> {


                        boolean enabled = (Boolean)newValue;


                        if(enabled){

                            requireActivity()
                                    .setRequestedOrientation(
                                            android.content.pm.ActivityInfo
                                                    .SCREEN_ORIENTATION_PORTRAIT);

                        }
                        else{

                            requireActivity()
                                    .setRequestedOrientation(
                                            android.content.pm.ActivityInfo
                                                    .SCREEN_ORIENTATION_UNSPECIFIED);
                        }


                        return true;
                    });
        }


        Preference about =
                findPreference("about");


        if(about != null){

            about.setOnPreferenceClickListener(v -> {

                Toast.makeText(
                        getContext(),
                        "Smart Farm App v1.0.0",
                        Toast.LENGTH_LONG
                ).show();

                return true;
            });
        }

        Preference theme = findPreference("theme");
        if (theme != null) {
            theme.setOnPreferenceChangeListener((preference, newValue) -> {
                ThemeUtils.applyTheme(requireContext(), (String) newValue);
                return true;
            });
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
    }
}
