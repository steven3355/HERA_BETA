package sjsu.research.hera_beta_10;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Steven on 2/9/2018.
 */

public class ConnectionSystem {
    private Map<BluetoothDevice, Connection> deviceConnectionMap;
    private Map<BluetoothGatt, Connection> gattConnectionMap;

    public ConnectionSystem() {
        deviceConnectionMap = new HashMap<>();
        gattConnectionMap = new HashMap<>();
    }
    public byte[] getToSendFragment(BluetoothGatt gatt, int fragmentSeq) {
        Connection curConnection = getConnection(gatt);
        int curConnectionDataSize = curConnection.getDatasize();
        int curConnectionSegmentCount = curConnection.getTotalSegmentCount();
        System.out.println("ConnectionDataSize is " + curConnectionDataSize);
        System.out.println("ConnectionSegmentCount is " + curConnectionSegmentCount);
        List<Byte> temp = new ArrayList<>();
        temp.add((byte) fragmentSeq);
        temp.add((byte) (fragmentSeq == curConnectionSegmentCount - 1 ? 0 : 1));
        for (int i = 0; i < curConnectionDataSize; i++) {
            if (fragmentSeq*curConnectionDataSize + i >= curConnection.getMatrixByteArray().length)
                break;
            temp.add(curConnection.getMatrixByteArray()[fragmentSeq*curConnectionDataSize + i]);
        }
        byte[] toSend = new byte[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            toSend[i] = temp.get(i);
        }
        System.out.println("Data prepared, sequence: " + fragmentSeq + " Length: " + toSend.length);
        return toSend;
    }

    public Connection getConnection(BluetoothGatt gatt) {
        return gattConnectionMap.get(gatt);
    }
    public Connection getConnection(BluetoothDevice device) {
        return deviceConnectionMap.get(device);
    }
    public void putConnection(Connection connection) {
        deviceConnectionMap.put(connection.getDevice(), connection);
        gattConnectionMap.put(connection.getGatt(), connection);
    }
    public void removeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        if (deviceConnectionMap.containsKey(connection.getDevice()))
            deviceConnectionMap.remove(connection.getDevice());
        if (gattConnectionMap.containsKey(connection.getGatt()))
            gattConnectionMap.remove(connection.getGatt());
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
