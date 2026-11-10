/** CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/
package ca.iot.innovators.smart.farm;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import ca.iot.innovators.smart.farm.login.LoginActivity;

public class SplashScreen extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen_activity);

        ImageView splashIcon = findViewById(R.id.splash_icon);

        Animation splashAnim = AnimationUtils.loadAnimation(this, R.anim.splash_animation);
        splashIcon.startAnimation(splashAnim);

        handler.postDelayed(() -> {
            startActivity(new Intent(SplashScreen.this, LoginActivity.class));
            finish();
        }, 3000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
