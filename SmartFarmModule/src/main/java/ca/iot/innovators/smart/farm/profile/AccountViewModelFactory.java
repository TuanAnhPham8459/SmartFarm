// AccountViewModelFactory.java
package ca.iot.innovators.smart.farm.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class AccountViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public AccountViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AccountViewModel.class)) {
            return (T) new AccountViewModel(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}