package sjsu.research.hera_beta_10;

import android.os.Bundle;

import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    BLEHandler mBLEHandler;
    HERA mHera;
    String TAG = "MainActivity";
    static String android_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        mHera = new HERA();
        mBLEHandler = new BLEHandler(this, mHera);
        mBLEHandler.startServer();
        mBLEHandler.startAdvertise();
        mBLEHandler.startScan();

        Log.d(TAG, "Android ID: " + android_id);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEHandler.stopAdvertise();
        mBLEHandler.stopScan();

    }
}
