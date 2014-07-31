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

import org.w3c.dom.Text;

import java.sql.BatchUpdateException;
import java.util.Arrays;
import java.util.Set;

//import android.support.v7.app.ActionBarActivity;

//Created by sumayyah
public class MainActivity extends Activity implements View.OnClickListener{
    
    private final String classID = "MainActivity";

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
    private Button scanButton;
    private Button fuelButton;
    private Button fuelSysButton;
    private Button speedButton;
    private Button rpmButton;
    private Button mafButton;
    private Button timeButton;
    private Button clearButton;
    private Button coolantButton;
    private Button checkISOButton;

    private TextView sendingMessage;
    private TextView response;
    private TextView connectStatus;
    private EditText userInput;
    private TextView hexVal;
    private TextView intVal;
    private TextView value;
    private TextView metricVal;

    private String command="";
    private int initFuel;
    private float tankCapacity;
    private final double treesPerGallon = 0.228;

    private final String PREFS_NAME = "MyPreferences";

    boolean fuel = false;
    private String buttonClicked="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getDataButton = (Button)(findViewById(R.id.getDataButton));
        scanButton = (Button)(findViewById(R.id.scanButton));
//        getContDataButton = (Button)(findViewById(R.id.contDataButton));
        fuelButton = (Button)(findViewById(R.id.fuelButton));
        fuelSysButton = (Button)(findViewById(R.id.fuelSysButton));
        speedButton = (Button)(findViewById(R.id.speedButton));
        rpmButton= (Button)(findViewById(R.id.rpmButton));
        mafButton = (Button)(findViewById(R.id.mafButton));
//        timeButton = (Button)(findViewById(R.id.timeButton));
        clearButton = (Button)(findViewById(R.id.clearButton));
        coolantButton = (Button)(findViewById(R.id.coolantButton));
        checkISOButton = (Button)(findViewById(R.id.checkISOButton));


        sendingMessage = (TextView)(findViewById(R.id.sendingMessage));
        response = (TextView)(findViewById(R.id.response));
        connectStatus = (TextView)(findViewById(R.id.connectStatus));
        userInput = (EditText)(findViewById(R.id.userInput));
        hexVal = (TextView)(findViewById(R.id.hexVal));
        intVal = (TextView)(findViewById(R.id.intVal));
        value = (TextView)(findViewById(R.id.value));
        metricVal = (TextView)(findViewById(R.id.metricVal));

        fuelButton.setOnClickListener(this);
        fuelSysButton.setOnClickListener(this);
        speedButton.setOnClickListener(this);
        rpmButton.setOnClickListener(this);
        mafButton.setOnClickListener(this);
//        timeButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        coolantButton.setOnClickListener(this);
        checkISOButton.setOnClickListener(this);

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

        /*Send initial blank message to recieve OK*/ //TODO: trigger this

        sendMessage(""+"\r");

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                command = userInput.getText().toString();
                userInput.setText("");
                sendMessage(command + "\r");
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
            LogWriter.writeData("\n"+"Command: "+message);

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

                    if(command.equals("012f")){
                        Console.log(classID+" asking for fuel");
                        fuel = true;
                    }
                    else fuel=false;
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
        Console.log("Reading message!");
        byte[] readBuffer = (byte[]) msg.obj;
        String bufferString = new String(readBuffer, 0, msg.arg1);

        LogWriter.writeData("\n"+"Response: "+bufferString.trim()+" ");

        /*If we get 4 bytes of data returned*/
        if(bufferString!="" && bufferString.matches("\\s*[0-9A-Fa-f]{2} [0-9A-Fa-f]{2} [0-9A-Fa-f]{2} [0-9A-Fa-f]{2}\\s*\r?\n?")){
            Console.log("Buffer String matches 8 digit pattern: "+bufferString);

            bufferString.trim();
            String[] bytes = bufferString.split(" ");

            if((bytes[0]!=null) && (bytes[1]!=null) && (bytes[2]!=null) && (bytes[3]!=null)){
                String firstPart = bytes[2];
                String secondPart = bytes[3];
                String finalString = firstPart+secondPart;
                Console.log("No null pieces! They're "+firstPart+" and "+secondPart+" makes "+finalString);

                hexVal.setText("0x"+finalString);
                hexToInt(finalString);

                response.append("\n"+"Command: "+command+"\n"+" Response: "+bufferString);

            } else Console.log("NUll pieces in first regex check :(");
        }
        /*If we get 3 bytes of data returned*/
        else if (bufferString!="" && bufferString.matches("\\s*[0-9A-Fa-f]{2} [0-9A-Fa-f]{2} [0-9A-Fa-f]{2}\\s*\r\n?")){
            Console.log("Buffer String matches 4 digit pattern: "+bufferString);

            bufferString.trim();
            String[] bytes = bufferString.split(" ");

            if((bytes[0]!=null) && (bytes[1]!=null) && (bytes[2]!=null)){

                String value2 = bytes[2];
                Console.log("No null pieces! It's "+value2);

                hexVal.setText("0x"+value2);
                hexToInt(value2);

                response.append("\n"+"Command: "+command+"\n"+" Response: "+bufferString);

            } else Console.log("NUll pieces in second regex check :(");
        }
        else {
            Console.log("Buffer string doesn't match regex, it's "+bufferString);
            response.append("\n"+"Command: "+command+"\n"+" Response: "+bufferString);
        }


    }

    private void hexToInt(String hexString){

        int value = Integer.parseInt(hexString, 16);
        Console.log("Converted! "+hexString+ " to "+value);
        Console.log("hex to int checking button type, its "+buttonClicked);

        intVal.setText(value+"");
        displayMetric(buttonClicked, value);

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


    private void displayMetric(String buttonClicked, float intValue){

        if(buttonClicked.equals("fuelButton")){
            Console.log("Type "+buttonClicked);
            float fuelVal = (intValue/255);
            value.setText(fuelVal+"% of tank");
            float treesUsed = (float) (fuelVal*treesPerGallon);

            metricVal.setText(treesUsed+ " tree seedlings grown for 10 years");
            Console.log("Int is "+intValue+" converted to "+fuelVal+" percent of tank, "+treesUsed+" treel killed");

        }else if(buttonClicked.equals("fuelSysButton")){
            Console.log("Type "+buttonClicked);
            value.setText("");
            metricVal.setText("");

        }else if(buttonClicked.equals("speedButton")){
            Console.log("Type "+buttonClicked);

            float mph = (float) (intValue/1.6);

            value.setText(intValue+"km/h "+mph+" mph");
            metricVal.setText("");

        }else if(buttonClicked.equals("rpmButton")){
            Console.log("Type "+buttonClicked);
            value.setText("");
            metricVal.setText("");

        }else if(buttonClicked.equals("timeButton")) {
            Console.log("Type " + buttonClicked);
            value.setText("");
            metricVal.setText("");

        }
        else if(buttonClicked.equals("mafButton")){
            Console.log("Type "+buttonClicked);
            value.setText("");
            metricVal.setText("");

        }else if(buttonClicked.equals("coolantButton")){
            Console.log("Type "+buttonClicked);

            float celsius = intValue-40;
            float fahrenheit = (((celsius*9)/5)+9);

            value.setText(celsius+" C "+fahrenheit+" F");
            metricVal.setText("");

        }else if(buttonClicked.equals("checkISOButton")){

            Console.log("Type "+buttonClicked);
        }else{
            Console.log("Wrong button clicked type!!");
            value.setText("");
            metricVal.setText("");
        }


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


    @Override
    public void onClick(View v) {

        buttonClicked = (String) v.getTag();

        switch (v.getId()){
            case R.id.fuelButton:
                userInput.setText("012F");
                userInput.setSelection(userInput.getText().length());
                break;
            case R.id.fuelSysButton:
                userInput.setText("015E");
                userInput.setSelection(userInput.getText().length());
                break;
            case R.id.speedButton:
                userInput.setText("010D");
                userInput.setSelection(userInput.getText().length());
                break;
            case R.id.rpmButton:
                userInput.setText("010C");
                userInput.setSelection(userInput.getText().length());
                break;
            case R.id.mafButton:
                userInput.setText("0110");
                userInput.setSelection(userInput.getText().length());
                break;
//            case R.id.timeButton:
//                userInput.setText("011F");
//                userInput.setSelection(userInput.getText().length());
//                break;
            case R.id.coolantButton:
                userInput.setText("0105");
                userInput.setSelection(userInput.getText().length());
                break;
            case R.id.checkISOButton:
                userInput.setText("ATDP");
                userInput.setSelection(userInput.getText().length());
            case R.id.clearButton:
                response.setText("");
                hexVal.setText("");
                intVal.setText("");
                value.setText("");
                metricVal.setText("");
                break;
            default:
                Console.log("Some other button clicked!");
                break;
        }
    }
}
