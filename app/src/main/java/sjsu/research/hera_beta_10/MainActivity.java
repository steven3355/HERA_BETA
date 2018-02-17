package sjsu.research.hera_beta_10;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    BLEHandler mBLEHandler;
    HERA mHera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHera = new HERA();
        mBLEHandler = new BLEHandler(this, mHera);
        TextView BeaconsReceived = (TextView) findViewById(R.id.BeaconsReceived);
        Button BroadcastButton = (Button) findViewById(R.id.BroadcastButton);
        BroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBLEHandler.startAdvertise();
                mBLEHandler.startScan();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEHandler.stopAdvertise();
        mBLEHandler.stopScan();

    }
}
