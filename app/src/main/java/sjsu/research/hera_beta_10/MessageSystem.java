package sjsu.research.hera_beta_10;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Steven on 2/7/2018.
 */

public class MessageSystem {
    private Map<String, Message> messageMap;

    MessageSystem() {
        messageMap = new HashMap<>();
    }
    Message getMessage(BluetoothDevice device) {
        return messageMap.get(device.getAddress().toString());
    }
    void putMessage(byte[] input) {

    }
}
