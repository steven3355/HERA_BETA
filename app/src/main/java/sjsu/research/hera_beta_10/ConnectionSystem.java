package sjsu.research.hera_beta_10;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Created by Steven on 2/9/2018.
 */

public class ConnectionSystem {
    private Map<String, Connection> addressConnectionMap;
    private Map<String, Connection> androidIDConnectionMap;
    public static final int DATA_TYPE_NAME = 0;
    public static final int DATA_TYPE_MATRIX = 1;
    public static final int DATA_TYPE_MESSAGE = 2;

    public ConnectionSystem() {
        addressConnectionMap = new HashMap<>();
    }

    public byte[] getToSendFragment(BluetoothGatt gatt, int fragmentSeq, int dataType) {
        Connection curConnection = getConnection(gatt);
        int curConnectionDataSize = curConnection.getDatasize();
        int curConnectionSegmentCount = curConnection.getTotalSegmentCount();
        System.out.println("ConnectionDataSize is " + curConnectionDataSize);
        System.out.println("ConnectionSegmentCount is " + curConnectionSegmentCount);
        List<Byte> temp = new ArrayList<>();
        temp.add((byte) fragmentSeq);
        temp.add((byte) (fragmentSeq == curConnectionSegmentCount - 1 ? 0 : 1));
        temp.add((byte) dataType);
        for (int i = 0; i < curConnectionDataSize; i++) {
            if (fragmentSeq*curConnectionDataSize + i >= curConnection.getMessageByteArray().length)
                break;
            temp.add(curConnection.getMessageByteArray()[fragmentSeq*curConnectionDataSize + i]);
        }
        byte[] toSend = new byte[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            toSend[i] = temp.get(i);
        }
        System.out.println("Data prepared, sequence: " + fragmentSeq + " Length: " + toSend.length);
        return toSend;
    }

    public Connection getConnection(BluetoothGatt gatt) {
        if (addressConnectionMap.containsKey(gatt.getDevice().getAddress()))
            return addressConnectionMap.get(gatt.getDevice().getAddress());
        else {
            addressConnectionMap.put(gatt.getDevice().getAddress(), new Connection(gatt));
            return addressConnectionMap.get(gatt.getDevice().getAddress());
        }
    }
    public Connection getConnection(BluetoothDevice device) {
        if (addressConnectionMap.containsKey(device.getAddress()))
            return addressConnectionMap.get(device.getAddress());
        else {
            addressConnectionMap.put(device.getAddress(), new Connection(device));
            return addressConnectionMap.get(device.getAddress());
        }

    }
    public void putConnection(Connection connection) {
        addressConnectionMap.put(connection.getAddress(), connection);
    }
    public Connection createConnection(BluetoothGatt gatt) {
        if (!addressConnectionMap.containsKey(gatt.getDevice().getAddress()) || addressConnectionMap.get(gatt.getDevice().getAddress()) == null) {
            addressConnectionMap.put(gatt.getDevice().getAddress(), new Connection(gatt));
        }
        else {
            addressConnectionMap.get(gatt.getDevice().getAddress()).setGatt(gatt);
        }
        return addressConnectionMap.get(gatt.getDevice().getAddress());
    }
    public Connection createConnection(BluetoothDevice device) {
        if (!addressConnectionMap.containsKey(device.getAddress()) || addressConnectionMap.get(device.getAddress()) == null) {
            addressConnectionMap.put(device.getAddress(), new Connection(device));
        }
        else {
            addressConnectionMap.get(device.getAddress()).setDevice(device);
        }
        return addressConnectionMap.get(device.getAddress());
    }
    public void removeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        if (addressConnectionMap.containsKey(connection.getDevice().getAddress()))
            addressConnectionMap.remove(connection.getDevice().getAddress());
    }
    public boolean isBean(BluetoothGatt gatt) {
        return gatt.getService(UUID.fromString("A495FF20-C5B1-4B44-B512-1370F02D74DE")) != null;
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
