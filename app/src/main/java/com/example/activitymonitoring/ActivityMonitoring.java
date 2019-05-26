package com.example.activitymonitoring;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
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

    private String accelerationLogFilename = "AM_data.txt";
    private String accelerationLogFilepath = "ActivityMonitoring";
    File accelerationLogFile;
//    String myData = "";

    private String activityLabels[];

    private Knn knn;

    //To provide context to static methods
    private static Context appContext;
    private static ActivityMonitoring ActivityMonitoring;
    private SharedPreferences sharedPreferences;

    public static final int DEFAULT_HEIGHT = 5;

    MotionEstimation motionEstimation;
    private boolean prediction_enabled = false;
    private int prediction_event_update_delay; //milliseconds
    private Handler prediction_event_update_handler;

    public void openMainView(View view){
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        // assign this instance, that it can be used in static method
        ActivityMonitoring.ActivityMonitoring = this;     //TODO

        motionEstimation = new MotionEstimation();
        appContext = MainActivity.getAppContext();

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        prediction_event_update_delay = preferences.getInt("predict_intervall_ms", 0);

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
                    //motionEstimation.enablePrediction();
                    prediction_enabled = true;
                } else{
                    prediction_enabled = false;
                    //motionEstimation.disablePrediction();
                }
            }
        });

        // setup the logging TODO implement that
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            btnRecord.setEnabled(false);
        } else {
            //Internal storage\Android\data\com.example.activitymonitoring\files\ActivityMonitoring\AM_data.txt
            accelerationLogFile = new File(getExternalFilesDir(accelerationLogFilepath), accelerationLogFilename);
        }

        // cyclical event to update the proposed activity; can be enabled and disabled with "prediction_enabled"
        prediction_event_update_handler = new Handler();
        prediction_event_update_handler.postDelayed(new Runnable(){
            public void run(){
                if(prediction_enabled) {
                    String currentActivity = motionEstimation.estimate();
                    mPredictionTextView.setText(String.format("Based on the accelerometer\n data it is likely that you are:\n %s", currentActivity));
                }
                prediction_event_update_handler.postDelayed(this, prediction_event_update_delay);
            }
        }, prediction_event_update_delay);

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
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                motionEstimation.addAccelerationValues(event.values[0], event.values[1], event.values[2]);

                 if (!selectedActivity.equals("N/A")) {
                    AppendDataToFile(event.values[0], event.values[1], event.values[2]);
                 }
                break;
            default:
                    //do nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void AppendDataToFile(double x, double y, double z) {
        try {
            FileOutputStream fos = new FileOutputStream(accelerationLogFile, true);
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