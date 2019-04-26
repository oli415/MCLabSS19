package com.example.activitymonitoring;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity
        extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // TextViews to display current sensor values
    private TextView mSensorAccelerometerTextView;
    private TextView mRecordStatusTextView;

    private RadioGroup radioActivityGroup;
    private RadioButton radioActivityButton;
    private Button btnRecord, btnStop;
    String selectedActivity = "N/A";

    private String filename = "AM_data.txt";
    private String feature_filename = "features_JoggingWalkingSittingStanding_wholeset_allFeatures.csv";
    private String filepath = "ActivityMonitoring";
    File myExternalFile;
    String myData = "";

    public static final int DEFAULT_HEIGHT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometerTextView = findViewById(R.id.label_acceleromter);
        mRecordStatusTextView = (TextView) findViewById(R.id.textRecordStatus);

        // assign sensor instances
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Get the error message from string resources.
        String sensor_error = getResources().getString(R.string.error_no_sensor);

        // If mAccelerometer is null, the sensor
        // is not available in the device.  Set the text to the error message
        if (mAccelerometer == null) {
            mSensorAccelerometerTextView.setText(sensor_error);
        }

        radioActivityGroup = findViewById(R.id.radioActivity);
        RadioButton radioButtonSitting = (RadioButton)findViewById(R.id.radioSitting);
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

                mRecordStatusTextView.setText("not recording: ");
            }
        });

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            btnRecord.setEnabled(false);
        } else {
            //Internal storage\Android\data\com.example.activitymonitoring\files\ActivityMonitoring\AM_data.txt
            myExternalFile = new File(getExternalFilesDir(filepath), filename);
        }
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

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                x = sensorEvent.values[0];
                y = sensorEvent.values[1];
                z = sensorEvent.values[2];
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
}