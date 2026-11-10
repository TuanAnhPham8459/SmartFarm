/**
 * CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/

package ca.iot.innovators.smart.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.iot.innovators.smart.farm.alert.AlertView;
import ca.iot.innovators.smart.farm.firestore.AlertModel;
import ca.iot.innovators.smart.farm.firestore.BaseFireStoreManager;
import ca.iot.innovators.smart.farm.firestore.DeviceModel;
import ca.iot.innovators.smart.farm.firestore.FireStoreService;
import ca.iot.innovators.smart.farm.firestore.SensorDataModel;
import ca.iot.innovators.smart.farm.history.AnalyticGraph;
import ca.iot.innovators.smart.farm.login.LoginActivity;
import ca.iot.innovators.smart.farm.notification.NotificationHelper;
import ca.iot.innovators.smart.farm.profile.AccountFragment;
import ca.iot.innovators.smart.farm.ui.ControlPanel;
import ca.iot.innovators.smart.farm.ui.Feedback;
import ca.iot.innovators.smart.farm.ui.Help;
import ca.iot.innovators.smart.farm.ui.Home;
import ca.iot.innovators.smart.farm.ui.Settings;
import ca.iot.innovators.smart.farm.utils.NetworkMonitor;
import ca.iot.innovators.smart.farm.utils.ScreenTrackingManager;
import ca.iot.innovators.smart.farm.utils.UnitUtils;


public class MainActivity extends BaseActivity {
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private Fragment homeFragment, dashboardFragment, settingsFragment, alertFragment;
    private Fragment activeFragment;

    private NotificationHelper notificationHelper;
    private ListenerRegistration globalSensorListener;

    // FAB Speed Dial
    private FloatingActionButton fabMain, fabAdd, fabSwitch;
    private LinearLayout layoutFabMenu;
    private View fabScrim;
    private boolean isFabOpen = false;

    // Connectivity Monitor
    private NetworkMonitor networkMonitor;
    private Snackbar offlineSnackbar;

    // Dragging variables
    private float dX, dY;
    private float initialX, initialY;
    private static final int CLICK_ACTION_THRESHOLD = 200;
    private long startClickTime;
    private ScreenTrackingManager trackingManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyOrientationSetting();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.hoa_view);
        Toolbar toolbar = findViewById(R.id.hoa_toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);  // NEW: Initialize bottom nav

        setSupportActionBar(toolbar);
        trackingManager = ScreenTrackingManager.getInstance();

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Handle navigation icon click (Hamburger vs Back)
        toolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                onBackPressed();
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Initialize Fragments
        homeFragment = new Home();
        dashboardFragment = new AnalyticGraph();
        settingsFragment = new ControlPanel();
        alertFragment = new AlertView();
        activeFragment = homeFragment;

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, alertFragment, "alert").hide(alertFragment)
                .add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.fragment_container, dashboardFragment, "dashboard").hide(dashboardFragment)
                .add(R.id.fragment_container, homeFragment, "home")
                .commit();

        // Navigation Drawer Listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.hoa_home) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_HOME);
                switchFragment(homeFragment);
                bottomNavigationView.setSelectedItemId(R.id.bottom_home);
            } else if (itemId == R.id.hoa_settings) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_CONTROL_PANEL);
                switchFragment(settingsFragment);
                bottomNavigationView.setSelectedItemId(R.id.bottom_settings);
            } else if (itemId == R.id.hoa_frag2) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_DASHBOARD);
                switchFragment(dashboardFragment);
                bottomNavigationView.setSelectedItemId(R.id.bottom_dashboard);
            } else if (itemId == R.id.hoa_frag3) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_SETTINGS);
                navigateToSubPage(new Settings(), "settings_sub");
            } else if (itemId == R.id.action_logout) {
                showLogoutConfirmation();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Bottom Navigation Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.bottom_home) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_HOME);
                switchFragment(homeFragment);
                navigationView.setCheckedItem(R.id.hoa_home);
                return true;
            } else if (itemId == R.id.bottom_dashboard) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_DASHBOARD);
                switchFragment(dashboardFragment);
                navigationView.setCheckedItem(R.id.hoa_frag2);
                return true;
            } else if (itemId == R.id.bottom_settings) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_SETTINGS);
                switchFragment(settingsFragment);
                navigationView.setCheckedItem(R.id.hoa_settings);
                return true;
            } else if (itemId == R.id.bottom_alert) {
                trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_ALERT);
                switchFragment(alertFragment);
                navigationView.setCheckedItem(R.id.bottom_alert);
                return true;
            }

            return false;
        });

        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.hoa_home);
            bottomNavigationView.setSelectedItemId(R.id.bottom_home);  // Set default selection
        }

        // Load the last used device from storage before falling back to a default,
        // so the persisted choice isn't clobbered by the default-device lookup below.
        FireStoreService.getInstance().loadPersistedDevice(this);

        initializeDefaultDevice();
        setupFabSpeedDial();
        setupNetworkMonitor();

        getSupportFragmentManager().addOnBackStackChangedListener(this::syncSubPageUI);

        notificationHelper = new NotificationHelper(this);
        notificationHelper.createNotificationChannel();

        startGlobalMonitoring();
    }

    private void setupNetworkMonitor() {
        networkMonitor = new NetworkMonitor(this, isConnected -> {
            if (!isConnected) {
                showOfflineBanner();
            } else {
                hideOfflineBanner();
            }
        });
        networkMonitor.start();
    }

    private void showOfflineBanner() {
        if (offlineSnackbar == null) {
            offlineSnackbar = Snackbar.make(findViewById(android.R.id.content),
                    R.string.you_are_offline_showing_cached_data, Snackbar.LENGTH_INDEFINITE);
            offlineSnackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.gray));
            offlineSnackbar.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        if (!offlineSnackbar.isShown()) {
            offlineSnackbar.show();
        }
    }

    private void hideOfflineBanner() {
        if (offlineSnackbar != null && offlineSnackbar.isShown()) {
            offlineSnackbar.dismiss();
            Snackbar.make(findViewById(android.R.id.content), R.string.back_online, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.status_ok))
                    .show();
        }
    }

    private void switchFragment(Fragment fragment) {
        if (fragment != activeFragment) {
            getSupportFragmentManager().beginTransaction().hide(activeFragment).show(fragment).commit();
            activeFragment = fragment;
            updateDashboardTitle();
        }
    }

    private void navigateToSubPage(Fragment fragment, String tag) {
        Fragment currentVisible = getVisibleFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, tag)
                .hide(currentVisible)
                .addToBackStack(tag)
                .commit();
    }

    private Fragment getVisibleFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            if (f != null && f.isVisible()) {
                return f;
            }
        }
        return activeFragment;
    }

    private void syncSubPageUI() {
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        boolean isSubPage = backStackCount > 0;

        // Toggle visibility of BottomNav and FAB
        bottomNavigationView.setVisibility(isSubPage ? View.GONE : View.VISIBLE);
        fabMain.setVisibility(isSubPage ? View.GONE : View.VISIBLE);
        if (isFabOpen) toggleFabMenu();

        // Toggle Drawer Lock Mode
        drawerLayout.setDrawerLockMode(isSubPage ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);

        // Toggle Back Arrow vs Hamburger
        if (getSupportActionBar() != null) {
            toggle.setDrawerIndicatorEnabled(!isSubPage);
            getSupportActionBar().setDisplayHomeAsUpEnabled(isSubPage);
            toggle.syncState();
        }

        // Update Title
        if (isSubPage) {
            String tag = getSupportFragmentManager().getBackStackEntryAt(backStackCount - 1).getName();
            updateTitleFromTag(tag);
        } else {
            updateDashboardTitle();
        }
    }

    private void updateTitleFromTag(String tag) {
        if ("account".equals(tag)) {
            setTitle(R.string.account);
        } else if ("settings_sub".equals(tag)) {
            setTitle(R.string.settings);
        } else if ("feedback".equals(tag)) {
            setTitle(R.string.feedback);
        } else if ("help".equals(tag)) {
            setTitle(R.string.help);
        }
    }

    private void updateDashboardTitle() {
        if (activeFragment instanceof Home) {
            setTitle(R.string.home);
        } else if (activeFragment instanceof AnalyticGraph) {
            setTitle(R.string.analytic_graph);
        } else if (activeFragment instanceof ControlPanel) {
            setTitle(R.string.control_panel);
        } else if (activeFragment instanceof AlertView) {
            setTitle(R.string.alerts);
        }
    }

    private void initializeDefaultDevice() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (FireStoreService.getInstance().getSelectedDeviceId() == null) {
            FireStoreService.getInstance().getDeviceManager().getUserDevices(user.getUid(), new BaseFireStoreManager.FireStoreCallbackWthData<List<DeviceModel>>() {
                @Override
                public void onSuccess(List<DeviceModel> devices) {
                    // Re-check: the persisted device may have loaded while this async call was in flight.
                    if (FireStoreService.getInstance().getSelectedDeviceId() != null) return;
                    if (!devices.isEmpty()) {
                        FireStoreService.getInstance().setSelectedDeviceId(devices.get(0).getMacAddress());
                        refreshAllDataForNewDevice();
                        startGlobalMonitoring();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(getString(R.string.mainactivity), getString(R.string.error_fetching_initial_device) + errorMessage);
                }
            });
        }
    }

    private void setupFabSpeedDial() {
        fabMain = findViewById(R.id.fabMain);
        fabAdd = findViewById(R.id.fabAdd);
        fabSwitch = findViewById(R.id.fabSwitch);
        layoutFabMenu = findViewById(R.id.layoutFabMenu);
        fabScrim = findViewById(R.id.fabScrim);

        fabMain.setOnClickListener(v -> toggleFabMenu());
        fabAdd.setOnClickListener(v -> {
            toggleFabMenu();
            showAddDeviceDialog();
        });
        fabSwitch.setOnClickListener(v -> {
            toggleFabMenu();
            showSwitchDeviceDialog();
        });

        if (fabScrim != null) {
            fabScrim.setOnClickListener(v -> {
                if (isFabOpen) toggleFabMenu();
            });
        }

        setupDraggableFab();
    }

    private void setupDraggableFab() {
        fabMain.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        initialX = view.getX();
                        initialY = view.getY();
                        startClickTime = System.currentTimeMillis();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Boundaries check
                        View parent = (View) view.getParent();
                        float maxX = parent.getWidth() - view.getWidth();
                        float maxY = parent.getHeight() - view.getHeight();

                        if (newX < 0) newX = 0;
                        if (newX > maxX) newX = maxX;
                        if (newY < 0) newY = 0;
                        if (newY > maxY) newY = maxY;

                        view.setX(newX);
                        view.setY(newY);

                        // Update menu position to follow the FAB
                        if (layoutFabMenu != null && layoutFabMenu.getVisibility() == View.VISIBLE) {
                            updateMenuPosition(newX, newY);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = System.currentTimeMillis() - startClickTime;
                        float movedDistance = (float) Math.sqrt(Math.pow(view.getX() - initialX, 2) + Math.pow(view.getY() - initialY, 2));

                        // Distinguish between click and drag
                        if (clickDuration < CLICK_ACTION_THRESHOLD && movedDistance < 10) {
                            view.performClick();
                        }
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });
    }

    private void toggleFabMenu() {
        if (!isFabOpen) {
            layoutFabMenu.animate().cancel(); // Prevent animation conflicts

            // Update position before showing to ensure it pops up from the right place
            updateMenuPosition(fabMain.getX(), fabMain.getY());

            // Show and animate Scrim
            if (fabScrim != null) {
                fabScrim.animate().cancel();
                fabScrim.setVisibility(View.VISIBLE);
                fabScrim.animate().alpha(1f).setDuration(300).start();
            }

            layoutFabMenu.setVisibility(View.VISIBLE);
            layoutFabMenu.setAlpha(0f);
            layoutFabMenu.setScaleX(0f);
            layoutFabMenu.setScaleY(0f);

            // Pivot from the bottom-right corner of the menu
            layoutFabMenu.post(() -> {
                layoutFabMenu.setPivotX(layoutFabMenu.getWidth());
                layoutFabMenu.setPivotY(layoutFabMenu.getHeight());

                layoutFabMenu.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            });

            fabMain.animate().rotation(135f).setDuration(300).start();
            isFabOpen = true;
        } else {
            layoutFabMenu.animate().cancel();
            if (fabScrim != null) fabScrim.animate().cancel();

            // Hide and animate Scrim
            if (fabScrim != null) {
                fabScrim.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    fabScrim.setVisibility(View.GONE);
                }).start();
            }

            layoutFabMenu.animate()
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        layoutFabMenu.setVisibility(View.GONE);
                    })
                    .start();

            fabMain.animate().rotation(0f).setDuration(200).start();
            isFabOpen = false;
        }
    }

    private void updateMenuPosition(float fabX, float fabY) {
        if (layoutFabMenu == null || fabMain == null) return;

        // If width/height are not yet measured (0), use approximate defaults to prevent "jump"
        float menuWidth = layoutFabMenu.getWidth() > 0 ? layoutFabMenu.getWidth() : 400; // Estimated 150dp
        float menuHeight = layoutFabMenu.getHeight() > 0 ? layoutFabMenu.getHeight() : 350; // Estimated 130dp

        float density = getResources().getDisplayMetrics().density;
        
        // Position menu above and slightly to the left of the FAB
        float menuX = fabX + fabMain.getWidth() - menuWidth;
        float menuY = fabY - menuHeight - (8 * density); // 8dp gap

        layoutFabMenu.setX(menuX);
        layoutFabMenu.setY(menuY);
    }

    private void showAddDeviceDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_new_device);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        final EditText inputMac = new EditText(this);
        inputMac.setHint(R.string.mac_address);
        layout.addView(inputMac);

        final EditText inputName = new EditText(this);
        inputName.setHint(R.string.device_name);
        layout.addView(inputName);

        builder.setView(layout);

        builder.setPositiveButton(R.string.add, (dialog, which) -> {
            String mac = inputMac.getText().toString().trim().toUpperCase();
            String name = inputName.getText().toString().trim();

            if (mac.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, R.string.please_fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            DeviceModel newDevice = new DeviceModel(mac, name, user.getUid());
            FireStoreService.getInstance().getDeviceManager().addDevice(newDevice, new BaseFireStoreManager.FireStoreCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, R.string.device_added_successfully, Toast.LENGTH_SHORT).show();
                    // Set as selected and persist it
                    FireStoreService.getInstance().updateAndPersistDevice(MainActivity.this, mac);
                    refreshAllDataForNewDevice();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, getString(R.string.error) + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void showSwitchDeviceDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FireStoreService.getInstance().getDeviceManager().getUserDevices(user.getUid(), new BaseFireStoreManager.FireStoreCallbackWthData<List<DeviceModel>>() {
            @Override
            public void onSuccess(List<DeviceModel> devices) {
                if (devices.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.no_devices_found_add_one_first, Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] names = new String[devices.size()];
                for (int i = 0; i < devices.size(); i++) {
                    names[i] = devices.get(i).getDeviceName() + " (" + devices.get(i).getMacAddress() + ")";
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.select_device)
                        .setItems(names, (dialog, which) -> {
                            DeviceModel selected = devices.get(which);
                            FireStoreService.getInstance().updateAndPersistDevice(MainActivity.this, selected.getMacAddress());
                            Toast.makeText(MainActivity.this, getString(R.string.switched_to) + selected.getDeviceName(), Toast.LENGTH_SHORT).show();
                            
                            // 1. Immediately switch to Home screen
                            switchFragment(homeFragment);
                            bottomNavigationView.setSelectedItemId(R.id.bottom_home);
                            navigationView.setCheckedItem(R.id.hoa_home);

                            // 2. Trigger data refresh on all screens
                            refreshAllDataForNewDevice();
                            
                            // 3. Restart global background monitoring
                            startGlobalMonitoring();
                        })
                        .show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, getString(R.string.error_fetching_devices) + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshAllDataForNewDevice() {
        if (homeFragment instanceof Home) ((Home) homeFragment).refreshForNewDevice();
        if (dashboardFragment instanceof AnalyticGraph) ((AnalyticGraph) dashboardFragment).refreshForNewDevice();
        if (settingsFragment instanceof ControlPanel) ((ControlPanel) settingsFragment).refreshForNewDevice();
        if (alertFragment instanceof AlertView) ((AlertView) alertFragment).refreshForNewDevice();
    }

    private void startGlobalMonitoring() {
        String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
        if (deviceId == null || deviceId.isEmpty()) return;

        if (globalSensorListener != null) {
            globalSensorListener.remove();
        }

        globalSensorListener = FireStoreService.getInstance().getSensorDataManager()
                .listenToSensorData(deviceId, new BaseFireStoreManager.FireStoreCallbackWthData<SensorDataModel>() {
                    @Override
                    public void onSuccess(SensorDataModel data) {
                        if (data != null) {
                            checkForAlertsGlobal(deviceId, data);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("MainActivity", "Global monitoring error: " + errorMessage);
                    }
                });
    }

    private void checkForAlertsGlobal(String deviceId, SensorDataModel data) {
        List<AlertModel> newAlerts = new ArrayList<>();
        boolean useFahrenheit = UnitUtils.isFahrenheit(this);
        String unit = UnitUtils.getTemperatureUnit(useFahrenheit);

        if (data.getTemperature() > 35) {
            double displayTemp = UnitUtils.convertTemperature(data.getTemperature(), useFahrenheit);
            newAlerts.add(new AlertModel("High Temperature",
                    String.format(Locale.getDefault(), "Temperature reached %.1f%s. Too hot!", displayTemp, unit),
                    "Critical"));
        } else if (data.getTemperature() < 18) {
            double displayTemp = UnitUtils.convertTemperature(data.getTemperature(), useFahrenheit);
            newAlerts.add(new AlertModel("Low Temperature",
                    String.format(Locale.getDefault(), "Temperature dropped to %.1f%s. Too cold!", displayTemp, unit),
                    "Warning"));
        }

        if (data.getHumidity() > 80) {
            newAlerts.add(new AlertModel("High Humidity",
                    String.format(Locale.getDefault(), "Humidity is at %.1f%%. Risk of mold.", data.getHumidity()),
                    "Warning"));
        }

        for (AlertModel alert : newAlerts) {
            // Save to Firestore
            FireStoreService.getInstance().getAlertManager().saveAlert(deviceId, alert, null);
            
            // Show System Notification
            notificationHelper.showNotification(alert.getTitle(), alert.getMessage(), alert.getSeverity());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkMonitor != null) {
            networkMonitor.stop();
        }
        if (globalSensorListener != null) {
            globalSensorListener.remove();
        }
    }

    // Override original to close drawer layout if opened
    @Override
    protected void showExitConfirmationDialog() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.showExitConfirmationDialog();
        }
    }

    // Menu on the toolbar + it's items

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        MenuItem accountItem = menu.findItem(R.id.action_account);
        View actionView = accountItem.getActionView();

        if (actionView != null) {
            actionView.setOnClickListener(v -> onOptionsItemSelected(accountItem));
            
            // Programmatically set colors for dark mode support if XML attributes fail
            TextView tv = actionView.findViewById(R.id.accTxt);
            ImageView iv = actionView.findViewById(R.id.accImg);
            int color = ContextCompat.getColor(this, isDarkMode() ? R.color.white : R.color.text_primary);
            if (tv != null) tv.setTextColor(color);
            if (iv != null) iv.setColorFilter(color);
        }
        return true;
    }

    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_account) {
            trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_ACCOUNT);
            navigateToSubPage(new AccountFragment(), "account");
            return true;
        } else if (id == R.id.action_settings) {
            trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_SETTINGS);
            navigateToSubPage(new Settings(), "settings_sub");
            return true;
        } else if (id == R.id.action_feedback) {
            trackingManager.trackScreenClick(ScreenTrackingManager.SCREEN_FEEDBACK);
            navigateToSubPage(new Feedback(), "feedback");
            return true;
        } else if (id == R.id.action_help) {
            trackingManager.trackScreenClick(getString(R.string.help));
            navigateToSubPage(new Help(), "help");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showLogoutConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {

                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // Clear local login/session information
                    SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(this);

                    prefs.edit()
                            .clear()
                            .apply();

                    // Open Login screen
                    Intent intent = new Intent(
                            MainActivity.this,
                            LoginActivity.class
                    );

                    // Prevent returning to MainActivity with Back button
                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);

                    finish();
                })
                .show();
    }

    private void applyOrientationSetting() {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);

        boolean lockPortrait = prefs.getBoolean(getString(R.string.portrait_lock), false);

        if (lockPortrait) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }
}