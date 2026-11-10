/**
 * CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/
package ca.iot.innovators.smart.farm.login;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginPreferences {
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private static LoginPreferences instance;

    private LoginPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized LoginPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new LoginPreferences(context.getApplicationContext());
        }
        return instance;
    }

    public void setRememberMe(boolean rememberMe) {
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    public boolean isRememberMe() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }
}
