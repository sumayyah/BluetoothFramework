package com.example.bluetoothframework.bluetoothframework;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
/**
 * Created by sumayyah on 4/2/14.
 */
public class Console {
    public static void log(String message){
        Log.d("Log", message);
    }
    public static void logAndShow(Context context, String message){
        Console.log(message);
        Toast.makeText(context, message, Toast.LENGTH_SHORT);
    }
}
