package com.example.activitymonitoring;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;


public class MainActivity extends AppCompatActivity {
    //To provide context to static methods
    private static Context context;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // assign this instance, that it can be used in static method
        MainActivity.context = getApplicationContext();

        // define preferences; does not suite perfectly here, but so it can be easily modified
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //int storedPreference = preferences.getInt("storedInt", 0);
        //public String feature_filename = "features_JoggingWalkingSittingStanding_wholeset_allFeatures.csv";
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //see https://stackoverflow.com/questions/3570690/whats-the-best-way-to-do-application-settings-in-android
        //editor.putString("feature_filename",  "features_JoggingWalkingSittingStanding_wholeset_allFeatures.csv" );

        //see https://stackoverflow.com/questions/3570690/whats-the-best-way-to-do-application-settings-in-android
        //editor.putString("feature_filename",  "features_JoggingWalkingSittingStanding_wholeset_allFeatures.csv" );
        editor.putString("feature_filename",  "features.csv" );
        editor.putInt("knn_metric",  2 );  // the algorithm used to find the nearest neighbors
        editor.putInt("window_length", 20);  //defines the number of acceleration values merged into one window
        editor.putInt("knn_neighbor_count", 5); //defines how many neighbors are considered
        editor.putInt("feature_count", 12); //defines how many attribute features are used per entry
        editor.putInt("predict_intervall_ms", 1000); //the cycle time of the prediction calculation in ms
        // .remove()   .clear()
        editor.commit();




    }

    public void openActivityMonitoringView(View view){
        startActivity(new Intent(this, ActivityMonitoring.class));
    }

    public void openWifiScannerView(View view){
        startActivity(new Intent(this, WifiActivity.class));
    }

    public void openIndoorLocalizationView(View view){
        startActivity(new Intent(this, IndoorLocalization.class));
    }


    public static Context getAppContext() {
        return MainActivity.context;
    }


}



