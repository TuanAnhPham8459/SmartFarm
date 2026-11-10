package ca.iot.innovators.smart.farm.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * Utility class to monitor internet connectivity status in real-time.
 */
public class NetworkMonitor {

    public interface NetworkCallback {
        void onConnectivityChanged(boolean isConnected);
    }

    private final ConnectivityManager connectivityManager;
    private final NetworkCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isCurrentlyConnected = false;

    private final ConnectivityManager.NetworkCallback internalCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            updateStatus(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            updateStatus(false);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            updateStatus(hasInternet);
        }
    };

    public NetworkMonitor(Context context, NetworkCallback callback) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.callback = callback;
        this.isCurrentlyConnected = checkInitialStatus();
    }

    public void start() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, internalCallback);
    }

    public void stop() {
        connectivityManager.unregisterNetworkCallback(internalCallback);
    }

    public boolean isConnected() {
        return isCurrentlyConnected;
    }

    private boolean checkInitialStatus() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void updateStatus(boolean isConnected) {
        if (this.isCurrentlyConnected != isConnected) {
            this.isCurrentlyConnected = isConnected;
            handler.post(() -> callback.onConnectivityChanged(isConnected));
        }
    }
}
