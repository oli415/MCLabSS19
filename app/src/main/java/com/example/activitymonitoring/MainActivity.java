package com.example.activitymonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openActivityMonitoringView(View view){
        startActivity(new Intent(this, ActivityMonitoring.class));
    }

    public void openWifiScannerView(View view){
        startActivity(new Intent(this, WifiActivity.class));
    }

    public void openIndoorLocalizationView(View view){
        //Intent intent = new Intent(this, ActivityMonitoring.class);
    }

}



