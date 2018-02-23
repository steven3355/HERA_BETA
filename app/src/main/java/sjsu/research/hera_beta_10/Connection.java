package sjsu.research.hera_beta_10;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Steven on 2/7/2018.
 */

public class Connection {
    private BluetoothGatt _transmitterGatt;
    private BluetoothDevice _device;
    private ByteArrayOutputStream _cache;
    private int _clientMTU;
    private int _serverMTU;
    private int _Datasize;
    private Map<String, List<Double>> _myHERAMatrix;
    private Map<String, List<Double>> _neighborHERAMatrix;
    private byte[] _messageByteArray;
    private int _totalSegmentCount;
    private Queue<String> _toSendQueue;

    public Connection(BluetoothGatt gatt) {
        _transmitterGatt = gatt;
        _device = gatt.getDevice();
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
        _clientMTU = 20;
    }
    public Connection(BluetoothDevice device) {
        _device = device;
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
        _serverMTU = 20;
    }

    public void writeToCache(byte[] input) {
        try {
            _cache.write(input);
        } catch (IOException e) {
            e.fillInStackTrace();
        }
    }
    public void setNeighborHERAMatrix() {
        ObjectInputStream input = null;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(_cache.toByteArray());
            input = new ObjectInputStream(inputStream);
            _neighborHERAMatrix = (Map<String, List<Double>>) input.readObject();
            System.out.println(_neighborHERAMatrix.toString());
        } catch (Exception e) {
            System.out.println("Reconstruct map exception" + e.fillInStackTrace());
        }
    }
    public Map<String, List<Double>> getNeighborHERAMatrix() {
        return _neighborHERAMatrix;
    }

    public void resetCache() {
        _cache = new ByteArrayOutputStream();
    }

    public void setDevice(BluetoothDevice device) {
        _device = device;
    }

    public void setGatt(BluetoothGatt gatt) {
        _transmitterGatt = gatt;
    }

    public BluetoothGatt getGatt() {
        return _transmitterGatt;
    }

    public BluetoothDevice getDevice() {
        return _device;
    }

    public ByteArrayOutputStream getCache() {
        return _cache;
    }

    public Map<String, List<Double>> buildCache() {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(_cache.toByteArray());
            ObjectInputStream input = new ObjectInputStream(inputStream);
            Map<String, List<Double>> neighborReachabilityMatrix = (Map<String, List<Double>>) input.readObject();
            System.out.println("Map reconstructed successfully");
            return neighborReachabilityMatrix;
        } catch (Exception e) {
            System.out.println("Reconstruct map exception" + e.fillInStackTrace());
            return null;
        }
    }

    public void setClientMTU(int mtu) {
        _clientMTU = 300;
        _Datasize = _clientMTU - 2;
        _totalSegmentCount = _messageByteArray.length / _Datasize + (_messageByteArray.length % _Datasize == 0 ? 0 : 1);
    }

    public void setServerMTU(int mtu) {
        _serverMTU = mtu;
    }

    public int getServerMTU() {
        return _serverMTU;
    }
    public int getClientMTU() {
        return _clientMTU;
    }

    public int getDatasize() {
        return _Datasize;
    }

    public void setMyHERAMatrix (Map<String, List<Double>> map) throws IOException {
        _myHERAMatrix = map;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        _Datasize = _clientMTU - 2;
        _messageByteArray = bos.toByteArray();
        System.out.println("Successfully converted Map to Byte Array, size : " + _messageByteArray.length);
        oos.close();
        _totalSegmentCount = +_messageByteArray.length / _Datasize + (_messageByteArray.length % _Datasize == 0 ? 0 : 1);
        System.out.println("The Flattened Map is: " + ConnectionSystem.bytesToHex(_messageByteArray));
    }
    public void setMessage(byte[] messageArray) {
        _messageByteArray = messageArray;
        _totalSegmentCount = +_messageByteArray.length / _Datasize + (_messageByteArray.length % _Datasize == 0 ? 0 : 1);
    }
    public Map<String, List<Double>> getMyHERAMatrix() {
        return _myHERAMatrix;
    }

    public int getTotalSegmentCount() {
        return _totalSegmentCount;
    }

    public byte[] getMessageByteArray() {
        return _messageByteArray;
    }

    public Queue<String> getToSendQueue() {
        return _toSendQueue;
    }

    public String getOneToSendDestination() {
        return _toSendQueue.poll();

    }

    public boolean isToSendEmpty() {
        return _toSendQueue.isEmpty();
    }

    public void pushToSendQueue(String dest) {
        _toSendQueue.add(dest);
    }
}
