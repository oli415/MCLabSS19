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

        // define preferences which are currently not changeable in the program;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //public String feature_filename = "features_JoggingWalkingSittingStanding_wholeset_allFeatures.csv";
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //see https://stackoverflow.com/questions/3570690/whats-the-best-way-to-do-application-settings-in-android

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

    /**
     * opens the application to predict your current activity
     * @param view
     */
    public void openActivityMonitoringView(View view){
        startActivity(new Intent(this, ActivityMonitoring.class));
    }

    /**
     * opens the application to list the nearby wifi access points and their rssi
     * @param view
     */
    public void openWifiScannerView(View view){
        startActivity(new Intent(this, WifiActivity.class));
    }

    /**
     * opens the particle filter indoor localization
     * @param view
     */
    public void openIndoorLocalizationView(View view){
        startActivity(new Intent(this, IndoorLocalization.class));
    }

    /**
     * opens settings view where several parameters for the indoor localization can be adapted
     * @param view
     */
    public void openSettingsView(View view) {
        startActivity(new Intent(this, Settings.class));
    }


    public static Context getAppContext() {
        return MainActivity.context;
    }


}



