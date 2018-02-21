package sjsu.research.hera_beta_10;
/**
 * Created by Steven on 2/7/2018.
 * Version 1.0
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Handler class used interface with Bluetooth Low Energy
 */
public class BLEHandler {
    private Context sContext;
    //API
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    Map<BluetoothDevice, Integer> connectionStatus = new HashMap<>();
    ParcelUuid mServiceUUIDParcel = ParcelUuid.fromString("00001830-0000-1000-8000-00805F9B34FB");
    ParcelUuid mServiceDataUUIDParcel = ParcelUuid.fromString("00009208-0000-1000-8000-00805F9B34FB");
    UUID mServiceUUID = UUID.fromString("00001830-0000-1000-8000-00805F9B34FB");
    UUID mCharUUID = UUID.fromString("00003000-0000-1000-8000-00805f9b34fb");
    ParcelUuid BeanServiceUUID = ParcelUuid.fromString("A495FF10-C5B1-4B44-B512-1370F02D74DE");
    ParcelUuid BeanServiceMask = ParcelUuid.fromString("11111100-0000-0000-0000-000000000000");

    //Scanner
    BluetoothLeScanner mBluetoothLeScanner;

    //Advertiser
    BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    //Gatt
    BluetoothGattServer mBluetoothGattServer;
    int _mtu = 400;
    Boolean connecting = false;

    ConnectionSystem mConnectionSystem;
    HERA myHera;
    /**
     * BLEHandler Constructor
     */
    public BLEHandler(Context systemContext, HERA hera) {
        myHera = hera;
        sContext = systemContext;
        mBluetoothManager = (BluetoothManager) sContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mConnectionSystem = new ConnectionSystem();
        System.out.println("BLE Handler Initiated");

    }

    /**
     * Bluetooth Low Energy Server
     */
    public void startServer(){
        mBluetoothGattServer = mBluetoothManager.openGattServer(sContext, mBluetoothGattServerCallback);
        BluetoothGattService mBluetoothGattService = new BluetoothGattService(mServiceUUID, 0);
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = new BluetoothGattCharacteristic(mCharUUID,BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE);
        mBluetoothGattService.addCharacteristic(mBluetoothGattCharacteristic);
        mBluetoothGattServer.addService(mBluetoothGattService);
    }

    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public synchronized void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Connection curConnection;
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                curConnection = new Connection(device);
                System.out.println("New server side connection formed with " + curConnection.getDevice().getAddress().toString());
                mConnectionSystem.putConnection(curConnection);
                System.out.println("Connection formed with " + device.getAddress().toString());
            }
            if (status == BluetoothGatt.STATE_DISCONNECTED) {
                mConnectionSystem.removeConnection(mConnectionSystem.getConnection(device));
            }
        }

        @Override
        public synchronized void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            System.out.println("Character write request received: " + ConnectionSystem.bytesToHex(value));
            Connection curConnection = mConnectionSystem.getConnection(device);
            if (characteristic.getUuid().equals(mCharUUID)) {
                curConnection.writeToCache(Arrays.copyOfRange(value, 2, value.length));
                System.out.println("Current cache contains: " + ConnectionSystem.bytesToHex(mConnectionSystem.getConnection(device).getCache().toByteArray()));
                if (value[1] == 0) {
                    curConnection.setNeighborHERAMatrix();
                    curConnection.resetCache();
                }
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Connection curConnection = mConnectionSystem.getConnection(device);
//            curConnection.setServerMTU(mtu);
            System.out.println("Server MTU with " + device.getAddress().toString() + " changed to " + mtu);
        }
    };

    /**
     * Prepares beacon's advertise data field
     * @param str Advertise Service Data;
     * @precondition str length + device name cannot exceed 15 bytes
     * @return AdvertiseData class
     */
    private AdvertiseData prepareAdvertiseData(String str) {
        AdvertiseData.Builder mAdvertiseDataBuilder = new AdvertiseData.Builder();
        mAdvertiseDataBuilder.addServiceData(mServiceDataUUIDParcel,str.getBytes());
        mAdvertiseDataBuilder.setIncludeDeviceName(true);
        mAdvertiseDataBuilder.setIncludeTxPowerLevel(true);
        mAdvertiseDataBuilder.addServiceUuid(mServiceUUIDParcel);
        return mAdvertiseDataBuilder.build();
    }

    /**
     * Prepares beacon's advertise setting
     * @return
     */
    private AdvertiseSettings prepareAdvertiseSettings() {
        AdvertiseSettings.Builder mAdvertiseSettingsBuilder= new AdvertiseSettings.Builder();
        mAdvertiseSettingsBuilder.setTimeout(0);
        mAdvertiseSettingsBuilder.setTxPowerLevel(3);
        mAdvertiseSettingsBuilder.setConnectable(true);
        return mAdvertiseSettingsBuilder.build();
    }

    /**
     * AdvertiseCallBack (Do nothing for now)
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }
    };

    /**
     * Starts BLE Beacon Advertisement
     * @Precondition The advertiser isn't be already broadcasting
     * @Postcondition Creates Async thread that broadcasts BLE Beacons
     */
    public void startAdvertise() {
        mBluetoothAdapter.cancelDiscovery();
        AdvertiseData mAdvertiseData = prepareAdvertiseData("PlzWork");
        AdvertiseSettings mAdvertiseSettings = prepareAdvertiseSettings();
        mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiseCallback);
        System.out.println("Beacon advertisement started");
    }

    /**
     * Stops BLE Beacon Advertisement
     * @Precondition The advertiser is already broadcasting
     * @Postcondition Ends advertiser thread
     */
    public void stopAdvertise() {
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        System.out.println("Beacon advertisement ended");
    }

    private List<ScanFilter> prepareScanFilterList(){
        ScanFilter.Builder mScanFilterBuilder = new ScanFilter.Builder();
        List<ScanFilter> filterList = new ArrayList<>();
        mScanFilterBuilder.setServiceUuid(mServiceUUIDParcel);
        ScanFilter.Builder mScanFilterBuilder2 = new ScanFilter.Builder();
        mScanFilterBuilder2.setServiceUuid(BeanServiceUUID, BeanServiceMask);
        filterList.add(mScanFilterBuilder2.build());
        filterList.add(mScanFilterBuilder.build());
        return filterList;
    }

    private ScanSettings prepareScanSetting(){
        ScanSettings.Builder mScanSettingsBuilder = new ScanSettings.Builder();
        mScanSettingsBuilder.setScanMode(1);
        return mScanSettingsBuilder.build();
    }
    public void startScan() {
        List<ScanFilter> mFilterList = prepareScanFilterList();
        ScanSettings mScanSettings = prepareScanSetting();
        mBluetoothLeScanner.startScan(mFilterList, mScanSettings, mScanCallback);
    }
    public void stopScan() {
        mBluetoothLeScanner.stopScan(mScanCallback);
    }
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final String address = result.getDevice().getAddress();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            System.out.println("Beacon received from: " + result.getDevice().getName());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    if ((!connectionStatus.containsKey(device) || connectionStatus.get(device) == 0) && !connecting)
                        EstablishConnection(device);
                }
            };
            mainHandler.post(myRunnable);
        }
    };
    private synchronized void EstablishConnection(BluetoothDevice device){
        connecting = true;
        connectionStatus.put(device, 1);
        System.out.println("Connecting to " + device.getAddress() + "\nCurrent GATT connection : " + mBluetoothManager.getConnectedDevices(7).size());
        device.connectGatt(sContext, false, mGattCallback, 2);
    }
    private void DisableConnection(BluetoothGatt gatt){
        gatt.close();
        System.out.println("Device " + gatt.getDevice().getAddress() + " disconnected.");
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            connecting = false;
            if(newState == BluetoothGatt.STATE_CONNECTED){
                mConnectionSystem.putConnection(new Connection(gatt));
                Connection curConnection = mConnectionSystem.getConnection(gatt);
                try {
                    curConnection.setConnectionHERAMatrix(myHera.getReachabilityMatrix());
                } catch(IOException e) {
                    e.fillInStackTrace();
                }
                connectionStatus.put(gatt.getDevice(), 2);
                gatt.discoverServices();
            }
            else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                mConnectionSystem.removeConnection(mConnectionSystem.getConnection(gatt));
                connectionStatus.put(gatt.getDevice(), 0);
                gatt.close();
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            int scratchNumber = Integer.valueOf(characteristic.getUuid().toString().substring(7,8));
            System.out.println("Scratch " + scratchNumber+ " read");
            System.out.println(ConnectionSystem.bytesToHex(characteristic.getValue()));
            if (scratchNumber < 5)
                gatt.readCharacteristic(gatt.getService(UUID.fromString("A495FF20-C5B1-4B44-B512-1370F02D74DE")).getCharacteristic(UUID.fromString("A495FF2" + String.valueOf(scratchNumber + 1)+ "-C5B1-4B44-B512-1370F02D74DE")));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            System.out.println("Service discovered");
            System.out.println(gatt.getServices());
            if (mConnectionSystem.isBean(gatt)) {
                gatt.readCharacteristic(gatt.getService(UUID.fromString("A495FF20-C5B1-4B44-B512-1370F02D74DE")).getCharacteristic(UUID.fromString("A495FF21-C5B1-4B44-B512-1370F02D74DE")));
            }
            else {
                gatt.requestMtu(_mtu);
                System.out.println("Requesting mtu change of " + _mtu);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mConnectionSystem.getConnection(gatt).setClientMTU(mtu);
                System.out.println("MTU is changed to " + mtu);
                BluetoothGattCharacteristic toSendValue = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
                toSendValue.setValue(mConnectionSystem.getToSendFragment(gatt, 0));
                System.out.println(toSendValue);
                gatt.writeCharacteristic(toSendValue);
                System.out.println("Initial segment sent");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int prevSegCount = characteristic.getValue()[0];
            int isLast = characteristic.getValue()[1];
            System.out.println("prevSegCount = " + prevSegCount);
            System.out.println("isLast = " + isLast);
            if(isLast == 0) {
                System.out.println("All " + (prevSegCount + 1) + " segments have been transmitted");
                DisableConnection(gatt);
                return;
            }
            BluetoothGattCharacteristic segmentToSend = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
            segmentToSend.setValue(mConnectionSystem.getToSendFragment(gatt, prevSegCount + 1));
            gatt.writeCharacteristic(segmentToSend);
            System.out.println("Fragment " + (prevSegCount + 1) + " sent");
        }


    };
}