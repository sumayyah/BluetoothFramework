package com.example.bluetoothframework.bluetoothframework;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Set;

//import android.support.v7.app.ActionBarActivity;

//Created by sumayyah
public class MainActivity extends Activity{
    
    private final String classID = "MainActivity";


    // Name of the connected device
    private String mConnectedDeviceName = null;
    //Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
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
    private Button scanButton;
    private TextView fuelUsedData;
    private TextView metricData;
    private TextView sendingMessage;
    private TextView response;
    private TextView connectStatus;
    private EditText userInput;

    private String command="";
    private int initFuel;
    private float tankCapacity;
    private final double treesPerGallon = 0.228;

    private final String PREFS_NAME = "MyPreferences";


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getDataButton = (Button)(findViewById(R.id.getDataButton));
        scanButton = (Button)(findViewById(R.id.scanButton));
//        fuelUsedData = (TextView)(findViewById(R.id.fuelUsedData));
//        metricData = (TextView)(findViewById(R.id.metricData));
        sendingMessage = (TextView)(findViewById(R.id.sendingMessage));
        response = (TextView)(findViewById(R.id.response));
        connectStatus = (TextView)(findViewById(R.id.connectStatus));
        userInput = (EditText)(findViewById(R.id.userInput));

        mBluetoothAdapter = mBluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); //Preloaded paired devices in memory

        /*Check if device supports Bluetooth*/
        if(mBluetoothAdapter==null) Console.log(classID+"Bluetooth is not supported in device."); //TODO: Show alert
        else Console.log(classID+"Bluetooth is supported in device.");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if(settings.getBoolean("my_first_time", true)){
            Console.log(classID+"FIRST TIME!!");
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
            Console.log(classID+"Bluetooth is not enabled, request sent");
        }else {
            Console.log(classID+"Bluetooth is enabled");
            if(mChatService == null) setupChat();
        }

        scanButton.setOnClickListener(new View.OnClickListener(){
            Intent serverIntent = null;

            @Override
            public void onClick(View v) {
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(MainActivity.this, Devices.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });

    }

    private void setupChat(){

        mChatService = new BluetoothChatService(this, mHandler);
        // Initialize the array adapter for the conversation thread


        /*Initialize buffer for outgoing messages*/
        mOutStringBuffer = new StringBuffer("");

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                command = userInput.getText().toString();
                userInput.setText("");
                sendMessage(command + "\r"); //get Fuel Percentage
                sendingMessage.setText("\n" + "Sent " + command);
            }
        });

    }

    public void sendMessage(String message){

        /*Make sure we are connected*/
        if(mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            connectStatus.setText("Not connected!");
            Console.log(classID+"Not connected!");
            return;
        }

        if(message.length() > 0){

            byte[] toSend = message.getBytes();
            mChatService.write(toSend);
            Console.log(classID+"Written "+message);

            /*Reset output buffer*/
            mOutStringBuffer.setLength(0);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){

        Console.log(classID+"on Activity result");

        switch (requestCode){
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectStatus.setText("Connecting...");
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectStatus.setText("Connecting...");
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
                    Console.log(classID+"BT not enabled");
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address and info
        String address = data.getExtras().getString(Devices.EXTRA_DEVICE_ADDRESS);
        String info = data.getExtras().getString(Devices.EXTRA_DEVICE_INFO);

        connectStatus.setText("Connected to: "+info+" "+address);
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
                            Console.log(classID + "Connected, calling onConnect");
                            connectStatus.setText("Connected to " + mConnectedDeviceName);
                            onConnect();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            connectStatus.setText("Connecting...");
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                    }
                    break;
                case MESSAGE_WRITE:
                    Console.log(classID+"Write command given");
                    break;
                case MESSAGE_READ:
                    Console.log(classID+" calling read message");
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
        String byteStr="";
        byte[] tempByte;
        String hexString="";

        byte[] readBuffer = (byte[]) msg.obj;
        String bufferString = new String(readBuffer, 0, msg.arg1);

        Console.log(classID+"\n"+"Raw buffer is: "+bufferString);

        if(command.equals("012f")){

            tempByte = Arrays.copyOfRange(readBuffer, 6, readBuffer.length-1);
            byteStr = new String(tempByte);
//            hexString = bufferString.substring(bufferString.length()-4,bufferString.length()-1);
//            for(int i=0;i<bufferString.length();i++){
//                Console.log("BUffer string at "+i+" is "+bufferString.charAt(i));
//            }
            if(bufferString.length()>6){
                hexString += bufferString.substring(6,8);
            }
        }

        else {
            tempByte = Arrays.copyOfRange(readBuffer, 6, readBuffer.length-1);
            byteStr = new String(tempByte);
//            for(int i=0;i<bufferString.length();i++){
//                Console.log("BUffer string at "+i+" is "+bufferString.charAt(i));

//            hexString = bufferString.substring(bufferString.length()-4,bufferString.length()-1);
            if(bufferString.length()>6){
                hexString += bufferString.substring(6,11);
            }
            hexString.trim();

        }
        response.append("\n"+"Command: "+command+"\n"+" Response: "+bufferString+"\n"+ "Bytes: "+byteStr+"\n"+ "Hex String: "+hexString);

        hexToInt(hexString);
//        int currentFuelPercentage = Integer.parseInt(readString);
//        Console.log(classID+"Fuel in int is "+currentFuelPercentage);
//
//        int percentFuelUsed = initFuel - currentFuelPercentage;
//        float fuelUsed = tankCapacity*(percentFuelUsed/100); //Find exact gallons used at this moment
//
//        fuelUsedData.setText("Used up "+fuelUsed+" gallons since last check");

//        String fuelUsed = readString;
//        displayMetric(fuelUsed);

    }

    private void hexToInt(String hexString){

        if(hexString.length()==5){
//            String temp = hexString.substring(1,2);
//            String temp2 = hexString.substring(4,5);
            String[] temp = hexString.split(" ");
            Console.log("temps are "+temp[0]+temp[1]);
            return;
        }
        if(hexString.length()>0) {
            Console.log("hexString "+hexString+" length "+hexString.length());
            int value = Integer.parseInt(hexString, 16);
            Console.log("Int "+value);
        }
    }

    private void onConnect(){

        /*Initialize PID codes*/
        Console.log(classID+"Sending initial message ATEO");
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
//                serverIntent = new Intent(this, Devices.class);
//                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
