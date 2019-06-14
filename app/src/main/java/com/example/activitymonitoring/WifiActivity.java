package com.example.activitymonitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class WifiActivity extends AppCompatActivity {
    WifiManager wifi;
    WifiReceiver wifiReceiver = new WifiReceiver();
    private TextView mLogTextView;
    private int scanningTime = 120; //seconds
    private int scansCurrent = 0;
    private int scansReceivedCounter = 0;
    private int counter = 0;
    String logText;

    public void openMainView(View view){
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLogTextView = findViewById(R.id.textLog);

        // Initialize WifiManager (contains API for managing all aspects of Wi-Fi connectivity)
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // If Wifi is switched off, enable it
        if (wifi.isWifiEnabled() == false)
            wifi.setWifiEnabled(true);

        // Register a receiver where scan results are made available
        registerReceiver (wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        Log.i("Wifi", "Vor startScan!!!");
        // begin scan
        startScan();
        Log.i("Wifi", "after startScan!!!");

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLogText();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        thread.start();
    }

    public void startScan(){
        if (scansCurrent < scanningTime){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    wifi.startScan();   //restricted to 4 scans per 2 minutes since Android 9 (https://developer.android.com/guide/topics/connectivity/wifi-scan)
                    startScan();
                }
            }, 1000);
            //Log.i("Wifi", String.format("in scan %d!!!", scansCurrent));
            scansCurrent++;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver);
    }

    // WifiReceiver class is subclass of WifiActivity
    class WifiReceiver extends BroadcastReceiver {
        // An access point scan has completed and results are sent here
        public void onReceive(Context c, Intent intent) {
            //  Call getScanResults() to obtain the results
            Log.i("Wifi", "onReceive start");
            List<ScanResult> results = wifi.getScanResults();
            scansReceivedCounter++;

            Log.i("Wifi", String.format("Scan # %d received:  %d", scansReceivedCounter, results.size()));
            try {
                StringBuilder builder = new StringBuilder();
                builder.append("WiFi Scan Results:\n");
                for (int n = 0; n < results.size (); n++) {
                    // SSID contains name of AP and level contains RSSI
                    //if (results.get(n).SSID.equals("Phrittenbude")) {
                        builder.append(String.format("SSID = " + results.get(n).SSID + "; RSSI =  " + results.get(n).level + "\n"));
                        Log.i("Wifi", "SSID = " + results.get(n).SSID + "; RSSI =  " + results.get(n).level);
                    //}
                }
                String result = builder.toString();
                //Log.i("Wifi", "Scan #" + scansReceivedCounter + " received");
                logText = result;
            }
            catch (Exception e) {
                Log.e("Wifi", "exception on Receive: " + results.toString());
            }
        }
    } // End of class WifiReceiver

    public void setLogText(){
        mLogTextView.setText(logText);
    }

} // End of class WifiActivity

