package com.lightwave.scannsend;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.Thread.sleep;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity {

    private static final String logTag = MainActivity.class.getSimpleName();
    private Button scanBtn;
    private ListView deviceList;
    private ListView newAddedDeviceList;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean mScanning;
    private Handler handler;
    private static final long scanPeriod =  10000;
    private LeDeviceListAdapter leDeviceListAdapter;
    private ArrayList<BluetoothDevice> mLeDevices;
    Context context;
    private int toastDuration = Toast.LENGTH_SHORT;
    private Toast toast;
    private String deviceName = null;
    private String deviceAddress = null;
    private String bleDeviceName = null;
    ArrayList<String> bleDeviceListArray = new ArrayList<String>();
    List<BluetoothGattService> gattServiceList;
    List<BluetoothGattCharacteristic> characteristicList;
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





    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = findViewById(R.id.scan_button);
        deviceList = findViewById(R.id.device_list_view);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

    }

    public void checkBluetoothEnable()
    {
        context = getApplicationContext();
       if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
       {
           Log.d(logTag, "Turning Bluetooth ON");
           Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
           startActivityForResult(enableBtIntent, 1);
           toast = Toast.makeText(context, "Turning Bluetooth ON", toastDuration);
           toast.show();
       }
       else
           {
            Log.d(logTag, "Bluetooth already ON");
            toast = Toast.makeText(context,"Bluetooth already ON",toastDuration);
            toast.show();
           }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startLeScan()
    {
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        handler = new Handler();
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
            bleDeviceListArray.add(bleDeviceName);
            final ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, bleDeviceListArray);
            deviceList.setAdapter(adapter);

            if(bleDeviceName.equals("SH-HC-08"))
            {
                bluetoothLeScanner.stopScan(leScanCallback);
                bluetoothGatt = result.getDevice().connectGatt(context,false,gattCallBack);
            }
            //leDeviceListAdapter.addDevice(result.getDevice());
            //leDeviceListAdapter.notifyDataSetChanged();
        }
    };

    private final BluetoothGattCallback gattCallBack= new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
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

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                Log.d(logTag, "Gatt services:" +gatt.getServices());
                gattServiceList = gatt.getServices();
            }
            else
            {
                Log.d(logTag, "onServicesDiscovered received: " + status);
            }

            if(gattServiceList.size() > 0)
            {
                for(BluetoothGattService gattServices : gattServiceList)
                {
                    UUID serviceUUID = gattServices.getUuid();
                    String serviceString = serviceUUID.toString();
                    Log.d(logTag,"serviceUUID: " +serviceUUID);
                    Log.d(logTag, "Characterisitcs: " +gattServices.getCharacteristics());
                }

            }
            else
            {
                Log.d(logTag, "Gatt service list is empty!!");
            }

        }
    };



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startScanDevice(View view) throws InterruptedException {

        checkBluetoothEnable();
        startLeScan();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {

        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
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