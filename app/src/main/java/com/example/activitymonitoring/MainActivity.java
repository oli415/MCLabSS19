package com.example.activitymonitoring;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

public class MainActivity
    extends AppCompatActivity implements SensorEventListener {


    private SensorManager mSensorManager;


    private Sensor mAccelerometer;

    // TextViews to display current sensor values
    private TextView mTextSensorAccelerometer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mTextSensorAccelerometer = (TextView) findViewById(R.id.label_acceleromter);

        // assign sensor instances
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Get the error message from string resources.
        String sensor_error = getResources().getString(R.string.error_no_sensor);

        // If either mSensorLight or mSensorProximity are null, those sensors
        // are not available in the device.  Set the text to the error message
        if (mTextSensorAccelerometer == null) {
            mTextSensorAccelerometer.setText(sensor_error);
        }

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
        int sensorType;
        sensorType = sensorEvent.sensor.getType();
        double x, y, z;

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                x = sensorEvent.values[0];
                y = sensorEvent.values[1];
                z = sensorEvent.values[2];
                mTextSensorAccelerometer.setText(getResources().getString(
                    R.string.label_accelerometer, x, y, z));

            default:
                // do nothing
}

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}