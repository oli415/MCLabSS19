package com.example.activitymonitoring;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity
    extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // TextViews to display current sensor values
    private TextView mSensorAccelerometerTextView;

    private RadioGroup radioActivityGroup;
    private RadioButton radioActivityButton;
    private Button btnRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometerTextView = findViewById(R.id.label_acceleromter);

        // assign sensor instances
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Get the error message from string resources.
        String sensor_error = getResources().getString(R.string.error_no_sensor);

        // If mAccelerometer is null, the sensor
        // is not available in the device.  Set the text to the error message
        if (mAccelerometer == null) {
            mSensorAccelerometerTextView.setText(sensor_error);
        }

        addListenerOnButton();
    }

    public void addListenerOnButton() {

        radioActivityGroup = findViewById(R.id.radioActivity);
        btnRecord = findViewById(R.id.btnRecord);

        btnRecord.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // get selected radio button from radioGroup
                int selectedId = radioActivityGroup.getCheckedRadioButtonId();

                // find the radiobutton by returned id
                radioActivityButton = findViewById(selectedId);

                Toast.makeText(MainActivity.this,
                        radioActivityButton.getText(), Toast.LENGTH_SHORT).show();
            }
        });
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


            default:
                // do nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void AppendDataToFile(double x, double y, double z){

    }
}