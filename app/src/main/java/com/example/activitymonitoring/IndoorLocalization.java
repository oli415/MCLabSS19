package com.example.activitymonitoring;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import static com.example.activitymonitoring.MotionEstimation.Activity.JOGGING;
import static com.example.activitymonitoring.MotionEstimation.Activity.SITTING;
import static com.example.activitymonitoring.MotionEstimation.Activity.STANDING;
import static com.example.activitymonitoring.MotionEstimation.Activity.WALKING;

//https://www.techrepublic.com/article/pro-tip-create-your-own-magnetic-compass-using-androids-internal-sensors/
public class IndoorLocalization extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float[] mCurrentDegreeBuffer = new float[10];
    private int mCurrentDegreeIndex = 0;
    private float mAverageDegree = 0;
    private float directionOffset = -30;

    private TextView mDirectionTextView;
    private TextView mPredictionTextView;
    //---
    private Button btnDrawRooms;
    private Button btnClearImage;
    private Button btnStart;
    private Button btnStop;
    //---
    private ImageView imageViewFloorplan;

    private static Context appContext;

    MotionEstimation motionEstimation;
    //private boolean motion_prediction_enabled = false;
    private int prediction_event_update_delay; //milliseconds
    private Handler prediction_event_update_handler;

    Floor floor;
    FloorMap floorMap;
    ParticleFilter particleFilter;

    boolean running = false;

    public void openMainView(View view){
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_localization);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //read some configuration values
        appContext = MainActivity.getAppContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        prediction_event_update_delay = preferences.getInt("predict_intervall_ms", 0);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mDirectionTextView = findViewById(R.id.textDirection);
        mPredictionTextView = findViewById(R.id.textPrediction);

        // If msensors is null, the sensor is not available in the device.
        String sensor_error = getResources().getString(R.string.error_no_sensor);
        if (mAccelerometer == null) {
            Log.e("Sensor", sensor_error);
            while(true);
        }
        if (mMagnetometer == null) {
            Log.e("Sensor", sensor_error);
            while(true);
        }

        imageViewFloorplan = findViewById(R.id.imageViewFloorplan);
        floor = new Floor();
        floorMap = new FloorMap(imageViewFloorplan);
        //floorMap.clearImage(imageViewFloorplan);

        particleFilter = new ParticleFilter();

        motionEstimation = new MotionEstimation();

        // cyclical event to update the proposed activity; can be enabled and disabled with "prediction_enabled"
        prediction_event_update_handler = new Handler();
        prediction_event_update_handler.postDelayed(new Runnable(){
            public void run(){
                //if(motion_prediction_enabled) {
                //String currentActivity = motionEstimation.estimate();
                MotionEstimation.Activity currentActivity = motionEstimation.estimate();
                mPredictionTextView.setText(String.format("Based on the accelerometer\n data it is likely that you are:\n %s", currentActivity.name()));

                if (running){
                    if (currentActivity == SITTING || currentActivity == WALKING ||currentActivity == JOGGING){
                        particleFilter.moveParticles(mAverageDegree);
                        particleFilter.substitudeInvalidMoves();
                        particleFilter.normalizeWeights();
                        particleFilter.resampleParticles();
                        particleFilter.updateCurrentPosition();

                        floorMap.clearImage();
                        floorMap.drawParticles(particleFilter.getParticles(), particleFilter.currentPosition);
                    }
                    if (currentActivity == STANDING){

                    }
                }

                prediction_event_update_handler.postDelayed(this, prediction_event_update_delay);
            }
        }, prediction_event_update_delay);


        btnDrawRooms = findViewById(R.id.btnDrawRooms);
        btnDrawRooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floorMap.drawRooms(floor.rooms);
            }
        });

        btnClearImage= findViewById(R.id.btnClearImage);
        btnClearImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floorMap.clearImage();
            }
        });

        btnStart= findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floorMap.drawParticles(particleFilter.getParticles(), particleFilter.getParticles()[0].getCurrentPosition());
                running = true;
            }
        });
        btnStop= findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                running = false;
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                //for motion estimation:
                motionEstimation.addAccelerationValues(event.values[0], event.values[1], event.values[2]);
                //for orientation:
                System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
                mLastAccelerometerSet = true; //TODO are they reset anywhere?
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
                mLastMagnetometerSet = true;
            default:
                    //do nothing
        }

        //TODO consider offloading this to orientation class?
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            mCurrentDegreeBuffer[mCurrentDegreeIndex] = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            mCurrentDegreeIndex = (mCurrentDegreeIndex + 1) % 10;
            if (mCurrentDegreeIndex == 0){
                mAverageDegree = 0;
                for (int i = 0; i < 10; i++){
                    mAverageDegree = mAverageDegree + mCurrentDegreeBuffer[i];
                }
                mAverageDegree = mAverageDegree / 10;
                mAverageDegree = (mAverageDegree + directionOffset + 360) % 360;
                mDirectionTextView.setText(String.format("Direction: %f", mAverageDegree));
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
