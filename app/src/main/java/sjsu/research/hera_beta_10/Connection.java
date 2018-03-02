package sjsu.research.hera_beta_10;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.util.Log;

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
    private String _address;
    private ByteArrayOutputStream _cache;
    private int _clientMTU;
    private int _serverMTU;
    private int _Datasize;
    private Map<String, List<Double>> _myHERAMatrix;
    private Map<String, List<Double>> _neighborHERAMatrix;
    private byte[] _messageByteArray;
    private int _totalSegmentCount;
    private Queue<String> _toSendQueue;
    private static String TAG = "Connection";
    private int connectionOverhead = 3;
    private String neighborAndroidID;
    public Connection(BluetoothGatt gatt) {
        _transmitterGatt = gatt;
        _device = gatt.getDevice();
        _address = gatt.getDevice().getAddress();
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
        _clientMTU = 20;
    }
    public Connection(BluetoothDevice device) {
        _device = device;
        _address = device.getAddress();
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
        _serverMTU = 20;
    }

    public void setNeighborAndroidID() {
        neighborAndroidID = new String(_cache.toByteArray());
    }
    public String getNeighborAndroidID() {
        return neighborAndroidID;
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
            byte[] cacheByteArr = _cache.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(cacheByteArr);
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

    public void setClientMTU(int mtu) {
        this._clientMTU = 300;
        this._Datasize = _clientMTU - connectionOverhead;
    }

    public void setServerMTU(int mtu) {
        this._serverMTU = mtu;
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

    public void setAndroidID (String androidID){
        _messageByteArray = androidID.getBytes();
        _totalSegmentCount = +_messageByteArray.length / _Datasize + (_messageByteArray.length % _Datasize == 0 ? 0 : 1);
    }
    public void setMyHERAMatrix (Map<String, List<Double>> map)  {
        try {
        _myHERAMatrix = map;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        _messageByteArray = bos.toByteArray();
        Log.d(TAG, "Successfully converted Map to Byte Array, size : " + _messageByteArray.length);
        oos.close();
        _totalSegmentCount = _messageByteArray.length / _Datasize + (_messageByteArray.length % _Datasize == 0 ? 0 : 1);
        } catch (IOException e) {
            e.fillInStackTrace();
        }
//        System.out.println("The Flattened Map is: " + ConnectionSystem.bytesToHex(_messageByteArray));
    }
    public void setMessage(byte[] messageArray) {
        _messageByteArray = messageArray;
        _totalSegmentCount = +_messageByteArray.length / _Datasize + (_messageByteArray.length % _Datasize == 0 ? 0 : 1);
    }
    public Map<String, List<Double>> getMyHERAMatrix() {
        Log.d(TAG, "getMyHERAMatrix: " + _myHERAMatrix);
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

    public void setAddres(String addres) {
        _address = addres;
    }
    public String getAddress() {
        return _address;
    }
    public int getOverHeadSize() {
        return connectionOverhead;
    }
}
