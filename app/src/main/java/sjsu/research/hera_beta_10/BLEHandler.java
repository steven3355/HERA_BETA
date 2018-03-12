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
import android.util.Log;

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
    private String TAG = "BLEHandler";
    private Context sContext;
    //API
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    Map<BluetoothDevice, Integer> connectionStatus = new HashMap<>();
    ParcelUuid mServiceUUIDParcel = ParcelUuid.fromString("00001830-0000-1000-8000-00805F9B34FB");
    ParcelUuid mServiceDataUUIDParcel = ParcelUuid.fromString("00009208-0000-1000-8000-00805F9B34FB");
    static UUID mServiceUUID = UUID.fromString("00001830-0000-1000-8000-00805F9B34FB");
    UUID mCharUUID = UUID.fromString("00003000-0000-1000-8000-00805f9b34fb");
    UUID mAndroidIDCharUUID = UUID.fromString("00003001-0000-1000-8000-00805f9b34fb");
    UUID BeanScratchServiceUUID = UUID.fromString("A495FF20-C5B1-4B44-B512-1370F02D74DE");
    UUID BeanScratchFirstCharUUID = UUID.fromString("A495FF21-C5B1-4B44-B512-1370F02D74DE");
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
    MessageSystem mMessageSystem;

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
        Log.d(TAG, "Bluetooth Low Energy Handler constructed.");
        mMessageSystem = new MessageSystem();
    }

    /**
     * Bluetooth Low Energy Server
     */
    public void startServer(){
        mBluetoothGattServer = mBluetoothManager.openGattServer(sContext, mBluetoothGattServerCallback);
        BluetoothGattService mBluetoothGattService = new BluetoothGattService(mServiceUUID, 0);
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = new BluetoothGattCharacteristic(mCharUUID,BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattCharacteristic mAndroidIDCharacteristic = new BluetoothGattCharacteristic(mAndroidIDCharUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mBluetoothGattService.addCharacteristic(mAndroidIDCharacteristic);
        mBluetoothGattService.addCharacteristic(mBluetoothGattCharacteristic);
        mBluetoothGattServer.addService(mBluetoothGattService);
    }

    /**
     * Prepares one message to send for curConnection using the message system message queue
     * @param curConnection
     */
    private void prepareToSendMessage(Connection curConnection) {
        String dest = curConnection.getOneToSendDestination();
        Log.d(TAG, "Preparing to send message for: " + dest);
        curConnection.setCurrentToSendPacket(mMessageSystem.getMessage(dest).getByte());
    }

    /**
     * Starts the send message process
     * @param curConnection
     */
    private void sendMessage(Connection curConnection) {
        String TAG = "toSend";
        BluetoothGatt gatt = curConnection.getGatt();
        if (gatt != null) {
            Log.d(TAG, "client gatt found, sending message");
            prepareToSendMessage(curConnection);
            BluetoothGattCharacteristic toSend = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
            toSend.setValue(mConnectionSystem.getToSendFragment(gatt, 0, ConnectionSystem.DATA_TYPE_MESSAGE));
            gatt.writeCharacteristic(toSend);
        }
        else {
            Log.d(TAG, "Client gatt not found");
            final String address = curConnection.getDevice().getAddress();
            Handler mainHandler = new Handler(Looper.getMainLooper());
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
    }

    /**
     * GattServer's callback function
     */
    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        String TAG = "BluetoothGattServerCallBack";
        @Override
        public synchronized void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

//            Connection curConnection;
            if (newState == BluetoothGatt.STATE_CONNECTED) {
//                curConnection = mConnectionSystem.updateConnection(device);
//                curConnection.setMyHERAMatrix(myHera.getReachabilityMatrix());
//                mConnectionSystem.putConnection(curConnection);
//                Log.d(TAG, "New server side connection formed with " + curConnection.getDevice().getAddress().toString());
            }
            if (status == BluetoothGatt.STATE_DISCONNECTED) {
//                mConnectionSystem.removeConnection(mConnectionSystem.getConnection(device));
                Log.d(TAG, device.getAddress().toString() + " disconnected");
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            if (characteristic.getUuid() == mAndroidIDCharUUID) {
                Log.d(TAG, "Android ID read request received, sending android ID" + MainActivity.android_id);
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, MainActivity.android_id.getBytes());
            }
        }

        @Override
        public synchronized void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            if (characteristic.getUuid().equals(mAndroidIDCharUUID)) {
                String neighborAndroidID = new String(value);
                mConnectionSystem.updateConnection(neighborAndroidID, device);
                mConnectionSystem.getConnection(neighborAndroidID).setMyHERAMatrix(myHera.getReachabilityMatrix());
                Log.d(TAG, neighborAndroidID);
            }
            int dataSequence = value[1];
            int isLast = value[2];
            int dataType = value[0];
            Log.d(TAG, "Character write request sequence: " + dataSequence + " isLast: " + isLast + " dataType: " + dataType) ;
            if (characteristic.getUuid().equals(mCharUUID)) {
                Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(device));
                curConnection.writeToCache(Arrays.copyOfRange(value, curConnection.getOverHeadSize(), value.length));
                if (isLast == 0) {
                    if (dataType == ConnectionSystem.DATA_TYPE_MATRIX) {
                        curConnection.buildNeighborHERAMatrix();
                        curConnection.resetCache();
                        String neighborAndroidID = curConnection.getNeighborAndroidID();
                        myHera.updateDirectHop(neighborAndroidID);
                        myHera.updateTransitiveHops(neighborAndroidID, curConnection.getNeighborHERAMatrix());
                        mMessageSystem.buildToSendMessageQueue(curConnection);
                        if (curConnection.isToSendQueueEmpty()) {
                            sendMessage(curConnection);
                            Log.d(TAG, "sendMessage Function called");
                        }
                        Log.d(TAG, "neighbor HERA matrix received");
                    } else if (dataType == ConnectionSystem.DATA_TYPE_MESSAGE) {
                        curConnection.buildMessage(mMessageSystem);
                        curConnection.resetCache();
                    }
                }
            }
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
//            Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(device));
//            curConnection.setServerMTU(mtu);
            Log.d(TAG, "Server MTU with " + device.getAddress().toString() + " changed to " + mtu);
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
        String TAG = "AdvertiseCallBack";
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
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Adapter isn't enabled");
            return;
        }
        mBluetoothAdapter.cancelDiscovery();
        AdvertiseData mAdvertiseData = prepareAdvertiseData("PlzWork");
        AdvertiseSettings mAdvertiseSettings = prepareAdvertiseSettings();
        mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiseCallback);
        Log.d(TAG, "Beacon advertisement started");
    }

    /**
     * Stops BLE Beacon Advertisement
     * @Precondition The advertiser is already broadcasting
     * @Postcondition Ends advertiser thread
     */
    public void stopAdvertise() {
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        Log.d(TAG, "Beacon advertisement ended");
    }

    private List<ScanFilter> prepareScanFilterList(){
        ScanFilter.Builder mScanFilterBuilder = new ScanFilter.Builder();
        List<ScanFilter> filterList = new ArrayList<>();
        mScanFilterBuilder.setServiceUuid(mServiceUUIDParcel);
        ScanFilter.Builder mScanFilterBuilder2 = new ScanFilter.Builder();
        mScanFilterBuilder2.setServiceUuid(BeanServiceUUID, BeanServiceMask);
        filterList.add(mScanFilterBuilder2.build());
        filterList.add(mScanFilterBuilder.build());
        Log.d(TAG, "Scan filter prepared");
        return filterList;
    }

    private ScanSettings prepareScanSetting(){
        ScanSettings.Builder mScanSettingsBuilder = new ScanSettings.Builder();
        mScanSettingsBuilder.setScanMode(1);
        Log.d(TAG, "Scan Setting prepared");
        return mScanSettingsBuilder.build();
    }
    public void startScan() {
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Adapter isn't enabled");
            return;
        }
        List<ScanFilter> mFilterList = prepareScanFilterList();
        ScanSettings mScanSettings = prepareScanSetting();
        mBluetoothLeScanner.startScan(mFilterList, mScanSettings, mScanCallback);
        Log.d(TAG, "Scan started");
    }
    public void stopScan() {
        mBluetoothLeScanner.stopScan(mScanCallback);
    }
    private ScanCallback mScanCallback = new ScanCallback() {
        String TAG = "ScanCallBack";
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final String address = result.getDevice().getAddress();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Log.d(TAG,"Beacon received from: " + result.getDevice().getName());
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
        Log.d(TAG, "Connecting to " + device.getAddress());
        device.connectGatt(sContext, false, mGattCallback, 2);
    }
    private void DisableConnection(BluetoothGatt gatt){
        gatt.close();
        Log.d(TAG, "Device " + gatt.getDevice().getAddress() + " disconnected.");
    }

    private void sendAndroidID(BluetoothGatt gatt) {
        BluetoothGattCharacteristic toSendValue = gatt.getService(mServiceUUID).getCharacteristic(mAndroidIDCharUUID);
        toSendValue.setValue(MainActivity.android_id.getBytes());
        Log.d(TAG, "Sending Android ID: " + new String(toSendValue.getValue()));
        gatt.writeCharacteristic(toSendValue);
    }

    private void sendHERAMatrix(BluetoothGatt gatt) {
        Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt.getDevice()));
        curConnection.setMyHERAMatrix(myHera.getReachabilityMatrix());
        BluetoothGattCharacteristic toSendValue = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
        toSendValue.setValue(mConnectionSystem.getToSendFragment(gatt,0, ConnectionSystem.DATA_TYPE_MATRIX));
        gatt.writeCharacteristic(toSendValue);
    }

    private void getNeighborAndroidID(BluetoothGatt gatt) {
        gatt.readCharacteristic(gatt.getService(mServiceUUID).getCharacteristic(mAndroidIDCharUUID));
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        String TAG = "BluetoothGattCallBack";
        @Override
        public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            connecting = false;
            if(newState == BluetoothGatt.STATE_CONNECTED){
//                mConnectionSystem.createConnection(gatt);
//                Log.d(TAG, "Connection formed with: " + mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt.getDevice())).getAddress());
                connectionStatus.put(gatt.getDevice(), 2);
                gatt.discoverServices();
            }
            else if(newState == BluetoothGatt.STATE_DISCONNECTED){
//                mConnectionSystem.removeConnection(mConnectionSystem.getConnection(gatt));
                connectionStatus.put(gatt.getDevice(), 0);
                gatt.close();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG,"Service discovered");
            if (mConnectionSystem.isBean(gatt)) {
                gatt.readCharacteristic(gatt.getService(BeanScratchServiceUUID).getCharacteristic(BeanScratchFirstCharUUID));
            }
            else if (mConnectionSystem.isHERANode(gatt)) {
                getNeighborAndroidID(gatt);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "MTU changed with " + gatt.getDevice() + " to " + mtu);
                mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt.getDevice())).setClientMTU(mtu);
                sendAndroidID(gatt);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            String TAG = "CharacteristicRead";
            if (mConnectionSystem.isBean(gatt)) {
                int scratchNumber = Integer.valueOf(characteristic.getUuid().toString().substring(7, 8));
                System.out.println("Scratch " + scratchNumber + " read");
                System.out.println(ConnectionSystem.bytesToHex(characteristic.getValue()));
                if (scratchNumber < 5)
                    gatt.readCharacteristic(gatt.getService(BeanScratchServiceUUID).getCharacteristic(UUID.fromString("A495FF2" + String.valueOf(scratchNumber + 1) + "-C5B1-4B44-B512-1370F02D74DE")));
            }
            else if (mConnectionSystem.isHERANode(gatt)) {
                String neighborAndroidID = characteristic.getStringValue(0);
                Log.d(TAG, "neighbor Android ID: " + neighborAndroidID);
                mConnectionSystem.updateConnection(neighborAndroidID, gatt);
                gatt.requestMtu(_mtu);
                Log.d(TAG, "Requesting mtu change of " + _mtu);
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            String TAG = "CharacteristicWrite";
            int dataType = characteristic.getValue()[0];
            int prevSegCount = characteristic.getValue()[1];
            int isLast = characteristic.getValue()[2];

            if (characteristic.getUuid().equals(mAndroidIDCharUUID)) {
                Log.d(TAG, "Android ID sent, sending HERA Matrix");
                sendHERAMatrix(gatt);
                return;
            }
            Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt.getDevice()));
            if(isLast == 0) {
                Log.d(TAG, "All " + (prevSegCount + 1) + " segments have been transmitted");
                if (dataType == ConnectionSystem.DATA_TYPE_MATRIX) {
                    if (!curConnection.isToSendQueueEmpty()) {
                        Log.d(TAG, "toSend is not empty, sending message");
                        sendMessage(curConnection);
                    }
                }
                if (dataType == ConnectionSystem.DATA_TYPE_MESSAGE) {
                    Log.d(TAG, "All messages have been transmitted, the connection will now terminate");
                    gatt.disconnect();
                }
            }
            else {
                BluetoothGattCharacteristic segmentToSend = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
                segmentToSend.setValue(mConnectionSystem.getToSendFragment(gatt, prevSegCount + 1, ConnectionSystem.DATA_TYPE_MATRIX));
                gatt.writeCharacteristic(segmentToSend);
                Log.d(TAG, "Fragment " + (prevSegCount + 1) + " sent");
            }
        }
    };
}