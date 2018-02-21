package sjsu.research.hera_beta_10;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    BLEHandler mBLEHandler;
    HERA mHera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHera = new HERA();
        mBLEHandler = new BLEHandler(this, mHera);
        mBLEHandler.startServer();
        mBLEHandler.startAdvertise();
        mBLEHandler.startScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEHandler.stopAdvertise();
        mBLEHandler.stopScan();

    }
}
