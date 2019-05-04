package com.example.activitymonitoring;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class ActivityMonitoring
        extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // TextViews to display current sensor values
    private TextView mSensorAccelerometerTextView;
    private TextView mRecordStatusTextView;
    private TextView mLogTextView;
    private TextView mPredictionTextView;

    private RadioGroup radioActivityGroup;
    private RadioButton radioActivityButton;
    private Button btnRecord, btnStop;
    private ToggleButton btnPredict;
    String selectedActivity = "N/A";

    private String filename = "AM_data.txt";
    private String filepath = "ActivityMonitoring";
    File myExternalFile;
    String myData = "";

    private Handler event_update_handler;
    private int event_update_delay; //milliseconds
    private int event_update_delay_default; //milliseconds
    private boolean prediction_enabled = false;
    private String activityLabels[];

    private Knn knn;

    //To provide context to static methods
    private static Context context;
    private static ActivityMonitoring ActivityMonitoring;
    private SharedPreferences sharedPreferences;

    public static final int DEFAULT_HEIGHT = 5;

    // ring buffer of size window-length x 4 (x,y,z,n)
    // n = squared absolute value: direction independent n as sum of squares (of x,y,z)
    private double[][] accelerationRingBuffer;
    // index of the oldest inserted acceleration values
    private int accelerationRingBufferIndex;

    public void openMainView(View view){
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        // assign this instance, that it can be used in static method
        ActivityMonitoring.context = getApplicationContext();
        ActivityMonitoring.ActivityMonitoring = this;

        // define preferences; does not suite perfectly here, but so it can be easily modified
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //int storedPreference = preferences.getInt("storedInt", 0);
        //public String feature_filename = "features_JoggingWalkingSittingStanding_wholeset_allFeatures.csv";
        SharedPreferences.Editor editor = sharedPreferences.edit();
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

        // assign graphical objects
        mSensorAccelerometerTextView = findViewById(R.id.label_acceleromter);
        mRecordStatusTextView = (TextView) findViewById(R.id.textRecordStatus);
        mLogTextView = findViewById(R.id.textLog);
        mPredictionTextView = findViewById(R.id.textPrediction);

        // assign sensor instances
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Get the error message from string resources.
        String sensor_error = getResources().getString(R.string.error_no_sensor);

        // If mAccelerometer is null, the sensor
        // is not available in the device.  Set the text to the error message
        if (mAccelerometer == null) {
            mSensorAccelerometerTextView.setText(sensor_error);
        }

        // initialized the radio group and the button(handlers)
        radioActivityGroup = findViewById(R.id.radioActivity);
        RadioButton radioButtonSitting = findViewById(R.id.radioSitting);
        radioButtonSitting.setChecked(true);

        btnRecord = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get selected radio button from radioGroup
                int selectedId = radioActivityGroup.getCheckedRadioButtonId();

                // find the radiobutton by returned id
                radioActivityButton = findViewById(selectedId);

                selectedActivity = radioActivityButton.getText().toString();
                mRecordStatusTextView.setText("recording: " + selectedActivity);
            }
        });

        btnStop = findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedActivity = "N/A";

                mRecordStatusTextView.setText( getResources().getString(R.string.label_record_status_disabled ));
            }
        });

        btnPredict = (ToggleButton)findViewById(R.id.btnPredict);
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPredict.isChecked()){
                    event_update_delay = event_update_delay_default;
                    prediction_enabled = true;
                    mPredictionTextView.setText(String.format("+ %d", event_update_delay));

                } else{
                    prediction_enabled = false;
                    //event_update_delay = Integer.MAX_VALUE;
                    mPredictionTextView.setText(getResources().getString(R.string.label_prediction_status_disabled));
                }
            }
        });

        // setup the logging
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            btnRecord.setEnabled(false);
        } else {
            //Internal storage\Android\data\com.example.activitymonitoring\files\ActivityMonitoring\AM_data.txt
            myExternalFile = new File(getExternalFilesDir(filepath), filename);
        }


        // cyclical event to update the proposed activity; can be enabled and disabled with "prediction_enabled"
        event_update_delay_default = sharedPreferences.getInt("predict_intervall_ms", 0);
        event_update_handler = new Handler();
        event_update_handler.postDelayed(new Runnable(){
            public void run(){
                //do something
                //setLogText(String.format("event: i=%d, pred: %b", tempCounter++, prediction_enabled));
                if(prediction_enabled) {
                    int k = sharedPreferences.getInt("knn_neighbor_count", 0);
                    int windowLen = sharedPreferences.getInt("window_length", 0);
                    TestRecord testEntry = new TestRecord(accelerationRingBuffer, windowLen);
                    int activity_id = knn.execute(k, testEntry);
                    mPredictionTextView.setText(String.format("Based on the accelerometer\n data it is likely that you are:\n %s", activityLabels[activity_id]));
                }
                event_update_handler.postDelayed(this, event_update_delay);
                //event_update_handler.postDelayed(this, 1000);
            }
        }, event_update_delay);

        //allocate acceleration Ring buffer
        int window_length = sharedPreferences.getInt("window_length", 0);
        accelerationRingBuffer = new double[window_length][4];

        String[] label = {"-", "Jogging", "Sitting", "Standing", "Walking"};
        activityLabels = label;

        // instantiate Knn Singelton, and store reference for simpler access
        knn = Knn.getInstance();

    }


    public static Context getAppContext() {
        return ActivityMonitoring.context;
    }

    public static Context getActivityMonitoring() {
        return ActivityMonitoring.ActivityMonitoring;
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        double x, y, z;
        int window_length = sharedPreferences.getInt("window_length", 0);

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                x = sensorEvent.values[0];
                y = sensorEvent.values[1];
                z = sensorEvent.values[2];
                accelerationRingBuffer[accelerationRingBufferIndex][0] = x;
                accelerationRingBuffer[accelerationRingBufferIndex][1] = y;
                accelerationRingBuffer[accelerationRingBufferIndex][2] = z;
                accelerationRingBuffer[accelerationRingBufferIndex][3] =
                        Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2);
                accelerationRingBufferIndex = (accelerationRingBufferIndex + 1) % window_length;
                mSensorAccelerometerTextView.setText(getResources().getString(
                        R.string.label_accelerometer, x, y, z));

                if (!selectedActivity.equals("N/A")) {
                    AppendDataToFile(x, y, z);
                }

            default:
                // do nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void AppendDataToFile(double x, double y, double z) {
        try {
            FileOutputStream fos = new FileOutputStream(myExternalFile, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            Long tsLong = System.currentTimeMillis() * 1000;
            String ts = tsLong.toString();
            String separator = System.getProperty("line.separator");
            osw.write("0," + selectedActivity + "," + ts + "," + x + "," + y + "," + z);
            osw.append(separator);
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLogText(String text){
        mLogTextView.setText(text);
    }
}