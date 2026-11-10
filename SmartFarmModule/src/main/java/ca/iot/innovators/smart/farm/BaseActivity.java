package ca.iot.innovators.smart.farm;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import ca.iot.innovators.smart.farm.utils.ThemeUtils;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If there are fragments in the back stack, let the fragment manager handle it
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    // Otherwise, show the exit dialog
                    showExitConfirmationDialog();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    protected void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.exit_icon)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> finishAffinity())
                .setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }
}