package com.example.bluetoothframework.bluetoothframework;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Set;

//Created by sumayyah
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //BLUETOOTH TEST
        int REQUEST_ENABLE_BT = 2;

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            View LinearLayout = findViewById(R.id.container);

            final ArrayList<String> devicesList = new ArrayList<String>();

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices(); //Preloaded paired devices in memory

        /*Check if device supports Bluetooth*/
            if(bluetoothAdapter==null) Console.log("Bluetooth is not supported in device."); //TODO: Show alert
            else Console.log("Bluetooth is supported in device.");

        /*Check if Bluetooth is enabled. If not, present that option to the user*/
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Console.log("Bluetooth is not enabled, request sent");
            }else Console.log("Bluetooth is enabled");

        /*If there are paired devices*/
            if (pairedDevices.size() > 0) {
                Console.log("Paired devices found");
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    devicesList.add(device.getName() + "\n" + device.getAddress());
                    Console.log("Device "+device.getName() + "\n" + device.getAddress());
                }
            }else Console.log("No paired devices");

        /*Find new Bluetooth devices*/

        /*TODO: This does not work when re-registering a previously paired device. Double-check with new BT device*/
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    Console.log("Starting broadcastReceiver");
                    String action = intent.getAction();
                    // When discovery finds a device
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        Console.log("Device found");
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // Add the name and address to an array adapter to show in a ListView
                        devicesList.add(device.getName() + "\n" + device.getAddress());
                        Console.log("Device "+device.getName() + "\n" + device.getAddress());
                    }else Console.log("No devices found");

                }
            };
            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceiver, filter);

        /*Make your own device discoverable*/
//        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);
            //Finish with onActivityResult (this will also turn Bluetooth on)

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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
