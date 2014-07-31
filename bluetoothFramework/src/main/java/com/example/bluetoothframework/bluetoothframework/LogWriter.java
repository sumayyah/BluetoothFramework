package com.example.bluetoothframework.bluetoothframework;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;

import android.annotation.SuppressLint;

/**
 * Created by sumayyah on 4/26/14.
 */
public class LogWriter {

    public static void writeData(final String data) {


        // ++++ Fire off a thread to write info to file

        Thread w_thread = new Thread() {
            public void run() {
//                File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/CarData/files");
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/storage/emulated/0/Android/carData");
                file.mkdirs();


                String dataline = (timestamp() + ", " + data + "\n");

                String dataString = (System.currentTimeMillis()+"\n"+ data + "\n");

                File myfile = new File(file + "/" + "Log" + getDate() + ".txt");
//                File myfile = new File("carData.txt");
                if(myfile.exists() == true)
                {
                    try {
                        FileWriter write = new FileWriter(myfile, true);
                        write.append(dataline);
                        //read_ct++;
                        write.close();
                    }catch (Exception e){

                    }

                }else{ //make a new file since we apparently need one
                    try {
                        FileWriter write = new FileWriter(myfile, true);
                        //	write.append(header);
                        write.append(dataline);
                        //read_ct++;
                        write.close();
                    }catch (Exception e){

                    }

                }
            }
        };
        w_thread.start();
    }

    public static long timestamp(){
        long timestamp = System.currentTimeMillis();
        return timestamp;
    }

    @SuppressLint("SimpleDateFormat")
    public static String sDate(){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        java.util.Date date= new java.util.Date();
        String sDate = sdf.format(date.getTime());
        return sDate;
    }

    @SuppressLint("SimpleDateFormat")
    public static String getDate(){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        java.util.Date date= new java.util.Date();
        String sDate = sdf.format(date.getTime());
        return sDate;
    }

}
