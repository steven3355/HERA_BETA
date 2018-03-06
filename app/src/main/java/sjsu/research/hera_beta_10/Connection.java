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
    private byte[] toSendPacket;
    private int _totalSegmentCount;
    private Queue<String> _toSendQueue;
    private static String TAG = "Connection";
    private int _connectionOverhead = 3;
    private String neighborAndroidID;

    public Connection(BluetoothGatt gatt) {
        _transmitterGatt = gatt;
        _device = gatt.getDevice();
        _address = gatt.getDevice().getAddress();
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
        _clientMTU = 20;
        _Datasize = _clientMTU - _connectionOverhead;
    }
    public Connection(BluetoothDevice device) {
        _device = device;
        _address = device.getAddress();
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
        _serverMTU = 20;
        _Datasize = _serverMTU - _connectionOverhead;
    }

    public int getOverHeadSize() {
        return _connectionOverhead;
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

    public void buildNeighborHERAMatrix() {
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

    public byte[] getCacheByteArray() {
        return _cache.toByteArray();
    }

    public void buildMessage(MessageSystem mMessageSystem) {
        mMessageSystem.putMessage(getCacheByteArray());
    }

    public void setClientMTU(int mtu) {
        this._clientMTU = 300;
        this._Datasize = _clientMTU - _connectionOverhead;
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

    public void setMyAndroidID (String androidID){
        toSendPacket = androidID.getBytes();
        _totalSegmentCount = +toSendPacket.length / _Datasize + (toSendPacket.length % _Datasize == 0 ? 0 : 1);
    }
    public void setMyHERAMatrix (Map<String, List<Double>> map)  {
        try {
        _myHERAMatrix = map;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        toSendPacket = bos.toByteArray();
        Log.d(TAG, "Successfully converted Map to Byte Array, size : " + toSendPacket.length);
        oos.close();
        _totalSegmentCount = toSendPacket.length / _Datasize + (toSendPacket.length % _Datasize == 0 ? 0 : 1);
        } catch (IOException e) {
            e.fillInStackTrace();
        }
//        System.out.println("The Flattened Map is: " + ConnectionSystem.bytesToHex(toSendPacket));
    }
    public void setCurrentToSendPacket(byte[] data) {
        toSendPacket = data;
        _totalSegmentCount = +toSendPacket.length / _Datasize + (toSendPacket.length % _Datasize == 0 ? 0 : 1);
    }
    public Map<String, List<Double>> getMyHERAMatrix() {
        Log.d(TAG, "getMyHERAMatrix: " + _myHERAMatrix);
        return _myHERAMatrix;
    }

    public int getTotalSegmentCount() {
        return _totalSegmentCount;
    }

    public byte[] getToSendPacket() {
        return toSendPacket;
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

}
