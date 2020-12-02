package com.lightwave.scannsend;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleClass {

    private static final String logTag = MainActivity.class.getSimpleName();
    private ListView deviceList;
    private ListView newAddedDeviceList;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean mScanning;
    private Handler handler;
    private static final long scanPeriod =  10000;
    private ArrayList<BluetoothDevice> mLeDevices;
    Context context;
    private int toastDuration = Toast.LENGTH_SHORT;
    private Toast toast;
    private String deviceName = null;
    private String deviceAddress = null;
    private String bleDeviceName = null;
    private String receiveInputCommand = null;
    ArrayList<String> bleDeviceListArray = new ArrayList<String>();
    List<BluetoothGattService> gattServiceList;
    List<BluetoothGattCharacteristic> characteristicList;
    UUID serviceUUID = null;
    UUID charUUID = null;
    private ArrayAdapter<String> adapter = null;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_MESSAGE_SERVICE = "com.example.ScanNSend.MESSAGE_SERVICE";
    public final static String EXTRA_MESSAGE_CHAR = "com.example.ScanNSend.MESSAGE_CHAR";
    private static BleClass ourInstance = new BleClass();


    static BleClass getInstance() {
        if(ourInstance == null)
        {
            ourInstance = new BleClass();
        }
        return ourInstance;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BleClass() {


    }


    /**
    * Description: This function starts BLE scan, if device is already scanning, it will turn it OFF
    */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startLeScan(String command)
    {
        //Logic to check if scanning is already ON, it its ON, it will turn it OFF and start scanning again.
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        handler = new Handler();
        receiveInputCommand = command;
        if(!mScanning)
        {
            Log.d(logTag, "Inside If condition startLeScan");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);

                }
            }, scanPeriod);
            mScanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        }
        else
        {
            Log.d(logTag, "Inside Else condition startLeScan");
            mScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    /**
     * Description: Below function will return the result once scanning is done and BLE device is found.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback leScanCallback= new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(logTag, "Scanned device name: " +result.getDevice().getName() +"      " +"Address: " +result.getDevice().getAddress());
            bleDeviceName = result.getDevice().getName();
            if(bleDeviceName == null)
            {
                bleDeviceName = "NULL";
            }
            //Add newly found BLE devices into an array and display it to the user.
            bleDeviceListArray.add(bleDeviceName);

            //Logic to check if the required BLE device is found, if found stop scanning and return the scan results
            if(bleDeviceName.equals("SH-HC-08"))
            {
                bluetoothLeScanner.stopScan(leScanCallback);
                bluetoothGatt = result.getDevice().connectGatt(context,false,gattCallBack);

            }

        }
    };

    /**
     * The below function will connect to the GATT server once BLE device is found
     */
    private final BluetoothGattCallback gattCallBack= new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            //Check if device and BLE peripheral are paired and connected
            if(newState == BluetoothProfile.STATE_CONNECTED)
            {
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                Log.d(logTag, "Connected to GATT server.");
                Log.d(logTag, "Attempting to start service discovery:" +bluetoothGatt.discoverServices());
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = STATE_DISCONNECTED;
                Log.d(logTag, "Disconnected from GATT server.");
            }
        }

        /**
         * Description: Below function will start service discovery as soon as device and BLE peripheral are connected as GATT server-client.
         * BLE peripheral is server and device is client.
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //UUID serviceUUID = null;
            //UUID charUUID = null;
            //If client is connected to GATT server, get all the services and add it to array list.
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                Log.d(logTag, "Gatt services:" +gatt.getServices());
                gattServiceList = gatt.getServices();
            }
            else
            {
                Log.d(logTag, "onServicesDiscovered received: " + status);
            }

            //Identify the characteristics of the services
            if(gattServiceList.size() > 0)
            {
                for(BluetoothGattService gattServices : gattServiceList)
                {
                    serviceUUID = gattServices.getUuid();
                    String serviceString = serviceUUID.toString();
                    Log.d(logTag,"serviceUUID: " +serviceUUID);
                    characteristicList = gattServices.getCharacteristics();
                    Log.d(logTag, "Characterisitcs: " +gattServices.getCharacteristics());
                }

                for(BluetoothGattCharacteristic gattCharList : characteristicList)
                {
                    charUUID = gattCharList.getUuid();
                    Log.d(logTag, "Characteristic UUID: " +gattCharList.getUuid().toString());
                }
                BluetoothGattCharacteristic c = gatt.getService(serviceUUID).getCharacteristic(charUUID);
                c.setValue(receiveInputCommand);
                gatt.writeCharacteristic(c);

            }
            else
            {
                Log.d(logTag, "Gatt service list is empty!!");
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

        }
    };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {

        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
        }
        public void addDevice(BluetoothDevice device) {
            Log.d(logTag, "addDevice: " +device);
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        public void clear() {
            mLeDevices.clear();
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }



    }

}
