package com.lightwave.scannsend;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    private EditText inputCommandText ;

    private BluetoothAdapter bluetoothAdapter;
    Context context;
    private int toastDuration = Toast.LENGTH_SHORT;
    private Toast toast;
    private String deviceName = null;
    private String deviceAddress = null;
    private String bleDeviceName = null;
    public String commandInput = null;

    private ArrayAdapter<String> adapter = null;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;


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
    public final static String EXTRA_MESSAGE = "com.example.bluetooth.le.EXTRA_MESSAGE";

    /**
     * Description: Below function will initialize all the UI components. Also it will set Bluetooth adapter
     * @param savedInstanceState
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = findViewById(R.id.scan_button);
        deviceList = findViewById(R.id.device_list_view);
        inputCommandText = findViewById(R.id.input_command_text);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    /**
     * Description: This function will check if the bluetooth is ON/OF on the device, if it's OFF, it will make it ON.
     * param: null
     */

    public void checkBluetoothEnable()
    {
        context = getApplicationContext();
        //Logic to check if bluetooth is ON/OFF, if OFF, it will turn ON
       if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
       {
           Log.d(logTag, "Turning Bluetooth ON");
           Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
           startActivityForResult(enableBtIntent, 1);
           toast = Toast.makeText(context, "Turning Bluetooth ON", toastDuration);
           toast.show();
       }
       //If bluetooth is already ON, show user it's already ON
       else
           {
            Log.d(logTag, "Bluetooth already ON");
            toast = Toast.makeText(context,"Bluetooth already ON",toastDuration);
            toast.show();
           }

    }

    /**
     * Description: This function calls the scan method from BleClass, which will start scanning BLE device around and connect, find service and characteristics.
     * Also it will send the command to the BLE device.
     * @param view
     * @throws InterruptedException
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startScanDevice(View view) throws InterruptedException {

        commandInput = inputCommandText.getText().toString();
        Log.d(logTag, "Input command: " +commandInput);
        BleClass.getInstance().startLeScan(commandInput);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

}