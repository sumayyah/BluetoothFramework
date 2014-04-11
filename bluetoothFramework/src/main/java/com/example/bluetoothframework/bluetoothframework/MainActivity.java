package com.example.bluetoothframework.bluetoothframework;

import android.app.Activity;
//import android.support.v7.app.ActionBarActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Set;

//Created by sumayyah
public class MainActivity extends Activity{


    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private static StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    static BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private static BluetoothChatService mChatService = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";


    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private Button getDataButton;
    private TextView fuelUsedData;
    private TextView metricData;

    private int initFuel;
    private float tankCapacity;
    private final double treesPerGallon = 0.228;

    private final String PREFS_NAME = "MyPreferences";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //BLUETOOTH TEST
        int REQUEST_ENABLE_BT = 2;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getDataButton = (Button)(findViewById(R.id.getDataButton));
        fuelUsedData = (TextView)(findViewById(R.id.fuelUsedData));
        metricData = (TextView)(findViewById(R.id.metricData));

        mBluetoothAdapter = mBluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); //Preloaded paired devices in memory

        /*Check if device supports Bluetooth*/
        if(mBluetoothAdapter==null) Console.log("Bluetooth is not supported in device."); //TODO: Show alert
        else Console.log("Bluetooth is supported in device.");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if(settings.getBoolean("my_first_time", true)){
            Console.log("FIRST TIME!!");
            collectUserData(settings);
            settings.edit().putBoolean("my_first_time", false).commit();
        }else{
            tankCapacity = settings.getFloat("tank_capacity", 14);
        }



    }

    @Override
    public void onStart() {
        super.onStart();

        initFuel = 100;

        /*Check if Bluetooth is enabled. If not, present that option to the user*/
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Console.log("Bluetooth is not enabled, request sent");
        }else {
            Console.log("Bluetooth is enabled");
            if(mChatService == null) setupChat();
        }

    }

    private void setupChat(){

        mChatService = new BluetoothChatService(this, mHandler);

        /*Initialize buffer for outgoing messages*/
        mOutStringBuffer = new StringBuffer("");

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("01 2F"); //get Fuel Percentage
            }
        });
    }

    public void sendMessage(String message){

        /*Make sure we are connected*/
        if(mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            Console.log("Not connected!");
            return;
        }

        if(message.length() > 0){

            byte[] toSend = message.getBytes();
            mChatService.write(toSend);
            Console.log("Written "+message);

            /*Reset output buffer*/
            mOutStringBuffer.setLength(0);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){

        Console.log("on Activity result");

        switch (requestCode){
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Console.log("BT not enabled");
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(Devices.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    private final Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg){

            switch (msg.what){
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1){

                        case BluetoothChatService.STATE_CONNECTED:
                            onConnect();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                    }
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_READ:
                    readMessage(msg);
                    break;
                case MESSAGE_DEVICE_NAME:

                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Console.log(TOAST);
                    break;
            }
        }
    };

    private void readMessage(Message msg){
        Console.log("Reading message");
        byte[] readBuffer = (byte[]) msg.obj;

        String readString = new String(readBuffer, 0, msg.arg1);
        Console.log("Message is: "+readString);

        int currentFuelPercentage = Integer.parseInt(readString);
        Console.log("Fuel in int is "+currentFuelPercentage);

        int percentFuelUsed = initFuel - currentFuelPercentage;
        float fuelUsed = tankCapacity*(percentFuelUsed/100); //Find exact gallons used at this moment

        fuelUsedData.setText("Used up "+fuelUsed+" gallons since last check");

        displayMetric(fuelUsed);

    }

    private void onConnect(){

        /*Initialize PID codes*/
        sendMessage("ATE0");
    }

    private void collectUserData(SharedPreferences settings){

        //TODO: Get user's input
        String make = "Honda";
        String model = "Accord";
        float tank_capacity =14;

        settings.edit().putString("car_make", make).commit();
        settings.edit().putString("car_model", model).commit();
        settings.edit().putFloat("tank_capacity", tank_capacity).commit();
    }


    private void displayMetric(float fuelUsed){

        double treesUsed = fuelUsed*treesPerGallon;

        metricData.setText(treesUsed+ " tree seedlings grown per 10 years");

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        Intent serverIntent = null;

        switch (item.getItemId()) {

            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, Devices.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
