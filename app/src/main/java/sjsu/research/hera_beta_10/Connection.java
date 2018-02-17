package sjsu.research.hera_beta_10;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by Steven on 2/7/2018.
 */

public class Connection {
    private BluetoothGatt _transmitterGatt;
    private BluetoothDevice _device;
    private ByteArrayOutputStream _cache;
    private int _mtu;
    private int _Datasize;
    private Map<String, List<Double>> _connectionHERAMatrix;
    private byte[] _matrixByteArray;
    private int _totalSegmentCount;
    public Connection(BluetoothGatt gatt) {
//        _connectionHERAMatrix = connectionHERAMatrix;
        _transmitterGatt = gatt;
        _device = gatt.getDevice();
        _cache = new ByteArrayOutputStream();
        _mtu = 20;
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

    public void setMTU(int mtu) {
        _mtu = 300;
        _Datasize = _mtu - 2;
        _totalSegmentCount = _matrixByteArray.length / _Datasize + (_matrixByteArray.length % _Datasize == 0 ? 0 : 1);
    }

    public int getMTU() {
        return _mtu;
    }

    public int getDatasize() {
        return _Datasize;
    }

    public void setConnectionHERAMatrix (Map<String, List<Double>> map) throws IOException {
        _connectionHERAMatrix = map;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        _Datasize = _mtu - 2;
        _matrixByteArray = bos.toByteArray();
        System.out.println("Successfully converted Map to Byte Array, size : " + _matrixByteArray.length);
        oos.close();
        _totalSegmentCount = _matrixByteArray.length / _Datasize + (_matrixByteArray.length % _Datasize == 0 ? 0 : 1);
        System.out.println("The Flattened Map is: " + ConnectionSystem.bytesToHex(_matrixByteArray));
    }

    public int getTotalSegmentCount() {
        return _totalSegmentCount;
    }

    public byte[] getMatrixByteArray() {
        return _matrixByteArray;
    }
}
