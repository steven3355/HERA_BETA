package sjsu.research.hera_beta_10;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Created by Steven on 2/9/2018.
 */

public class ConnectionSystem {
    private Map<String, Connection> androidIDConnectionMap;
    private Map<BluetoothDevice, String> deviceAndroidIDMap;
    private String TAG = "ConnectionSystem";
    public static final int DATA_TYPE_NAME = 0;
    public static final int DATA_TYPE_MATRIX = 1;
    public static final int DATA_TYPE_MESSAGE = 2;

    public ConnectionSystem() {
        androidIDConnectionMap = new HashMap<>();
        deviceAndroidIDMap = new HashMap<>();
    }

    public byte[] getToSendFragment(BluetoothGatt gatt, int fragmentSeq, int dataType) {
        String neighborAndroidID = getAndroidID(gatt.getDevice());
        Connection curConnection = getConnection(neighborAndroidID);
        int curConnectionDataSize = curConnection.getDatasize();
        int curConnectionSegmentCount = curConnection.getTotalSegmentCount();
        List<Byte> temp = new ArrayList<>();
        temp.add((byte) dataType);
        temp.add((byte) fragmentSeq);
        temp.add((byte) (fragmentSeq == curConnectionSegmentCount - 1 ? 0 : 1));
        for (int i = 0; i < curConnectionDataSize; i++) {
            if (fragmentSeq*curConnectionDataSize + i >= curConnection.getToSendPacket().length)
                break;
            temp.add(curConnection.getToSendPacket()[fragmentSeq*curConnectionDataSize + i]);
        }
        byte[] toSend = new byte[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            toSend[i] = temp.get(i);
        }
        Log.d(TAG, "toSend fragment prepared");
        return toSend;
    }

    public String getAndroidID(BluetoothDevice device) {
        return deviceAndroidIDMap.get(device);
    }
    public Connection getConnection(String androidID) {
        if (androidIDConnectionMap.containsKey(androidID)) {
            return androidIDConnectionMap.get(androidID);
        }
        else {
            return null;
        }
    }


    public void putConnection(String androidID, Connection connection) {
        androidIDConnectionMap.put(androidID, connection);
    }
//    public Connection createConnection(BluetoothGatt gatt) {
//        String TAG = "createConnection";
//        if (!androidIDConnectionMap.containsKey(gatt.getDevice().getAddress()) || androidIDConnectionMap.get(gatt.getDevice().getAddress()) == null) {
//            androidIDConnectionMap.put(gatt.getDevice().getAddress(), new Connection(gatt));
//            Log.d(TAG, "gatt connection not found, entry created");
//        }
//        else {
//            androidIDConnectionMap.get(gatt.getDevice().getAddress()).setGatt(gatt);
//            Log.d(TAG, "gatt connection found, entry updated");
//        }
//        return androidIDConnectionMap.get(gatt.getDevice().getAddress());
//    }
//    public Connection createConnection(BluetoothDevice device) {
//        String TAG = "createConnection";
//        if (!androidIDConnectionMap.containsKey(device.getAddress()) || androidIDConnectionMap.get(device.getAddress()) == null) {
//            androidIDConnectionMap.put(device.getAddress(), new Connection(device));
//            Log.d(TAG, "device connection not found, entry created");
//        }
//        else {
//            androidIDConnectionMap.get(device.getAddress()).setDevice(device);
//            Log.d(TAG, "device connection found, entry updated");
//        }
//        return androidIDConnectionMap.get(device.getAddress());
//    }

    public void updateConnection(String neighborAndroidID, BluetoothGatt gatt) {
        if (!androidIDConnectionMap.containsKey(neighborAndroidID)) {
            androidIDConnectionMap.put(neighborAndroidID, new Connection(neighborAndroidID, gatt));
        }
        else {
            androidIDConnectionMap.get(neighborAndroidID).setGatt(gatt);
        }
        deviceAndroidIDMap.put(gatt.getDevice(), neighborAndroidID);
    }

    public void updateConnection(String neighborAndroidID, BluetoothDevice device) {
        if (!androidIDConnectionMap.containsKey(neighborAndroidID)) {
            androidIDConnectionMap.put(neighborAndroidID, new Connection(neighborAndroidID, device));
        }
        else {
            androidIDConnectionMap.get(neighborAndroidID).setDevice(device);
        }
        deviceAndroidIDMap.put(device, neighborAndroidID);
    }
    public boolean isBean(BluetoothGatt gatt) {
        return gatt.getService(UUID.fromString("A495FF20-C5B1-4B44-B512-1370F02D74DE")) != null;
    }
    public boolean isHERANode(BluetoothGatt gatt) {
        return gatt.getService(BLEHandler.mServiceUUID) != null;
    }
    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
