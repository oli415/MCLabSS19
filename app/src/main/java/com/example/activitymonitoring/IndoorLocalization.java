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
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

//https://www.techrepublic.com/article/pro-tip-create-your-own-magnetic-compass-using-androids-internal-sensors/
public class IndoorLocalization extends AppCompatActivity implements SensorEventListener {
//Sensors
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private final int degreeBufferSize = 100;
    private float[] mCurrentDegreeBuffer = new float[degreeBufferSize];
    private int mCurrentDegreeIndex = 0;
    private float mAverageDegree = 0;
    private float directionOffset = -90;
    private int stepFrequency = 1000; //in ms todo

//Gui-Stuff
    private TextView mDirectionTextView;
    private TextView mPredictionTextView;
    //---
    private Button btnDrawRooms;
    private Button btnClearImage;
    private Button btnStart;
    private Button btnStop;
    private Button btnReset;
    //---
    private ImageView imageViewFloorplan;
    private ImageView compassNeedleImageView;
    //----
    private SeekBar directionSeekBar;

    private static Context appContext;


    MotionEstimation motionEstimation;
    //private boolean motion_prediction_enabled = false;
    private int motion_prediction_event_update_delay; //milliseconds
//    private Handler prediction_event_update_handler;
    private Timer motionEstimationTimer;
    private MotionEstimationThread motionEstimationThread;
//    private MotionEstimation.Activity currentActivity;
    private Handler stepExecutionHandler;
    private StepExecutionThread stepExecutionThread;

    Floor floor;
    FloorMap floorMap;
    ParticleFilter particleFilter;

    boolean executeLocalization = false;
    boolean isInMotion = false;

    public void openMainView(View view){
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_localization);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //read some configuration values
        appContext = MainActivity.getAppContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        motion_prediction_event_update_delay = preferences.getInt("predict_intervall_ms", 0);

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
        Log.i("Sensor", String.format("accelerometerSensorDelay: %d us", mAccelerometer.getMinDelay()));
        Log.i("Sensor", String.format("erometerSensorDelay %d us", mAccelerometer.getMinDelay()));
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        //mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);

        imageViewFloorplan = findViewById(R.id.imageViewFloorplan);
        floor = new Floor();
        floorMap = new FloorMap(imageViewFloorplan);
        //floorMap.clearImage(imageViewFloorplan);

        compassNeedleImageView = findViewById(R.id.imageViewCompassNeedle);

        directionSeekBar = (SeekBar) findViewById(R.id.directionSeekBar);
        //directionSeekBar.setMin(0);
        directionSeekBar.setMax(360);

        particleFilter = new ParticleFilter();

        motionEstimation = new MotionEstimation();

        //the motion is estimated in fixed intervals TODO probably better to use handler as before
        motionEstimationThread = new MotionEstimationThread();
        motionEstimationTimer = new Timer("motionExtimationTimer");
        motionEstimationTimer.scheduleAtFixedRate(motionEstimationThread, motion_prediction_event_update_delay, motion_prediction_event_update_delay); //TODO should we run it all the time or only on start button press

        //steps are simulated at fixed frequency, when to device is in motion
        stepExecutionHandler = new Handler();
        stepExecutionThread = new StepExecutionThread();

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
                executeLocalization = true;
            }
        });
        btnStop= findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeLocalization = false;
            }
        });

        btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                executeLocalization = false;
                particleFilter.initializeParticles();
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
                //TODO forward not all acceleration values as we do sample now at 10th speed
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
            mCurrentDegreeIndex = (mCurrentDegreeIndex + 1) % degreeBufferSize;
            if (mCurrentDegreeIndex == 0){
                mAverageDegree = 0;
                for (int i = 0; i < degreeBufferSize; i++){
                    mAverageDegree = mAverageDegree + mCurrentDegreeBuffer[i];
                    if(i<4) {
                        Log.i("sensor", String.format("[%d]= %f", i, mCurrentDegreeBuffer[i]));
                    }
                }
                mAverageDegree = mAverageDegree / degreeBufferSize;
                mAverageDegree = directionSeekBar.getProgress(); //TODO seekBar overwrites measured value
                //mAverageDegree = (mAverageDegree + directionOffset) % 360; //TODO add offset
                mDirectionTextView.setText(String.format("Direction: %f", mAverageDegree));

                compassNeedleImageView.setRotation(mAverageDegree);
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean isActivityMotion(MotionEstimation.Activity activity) {
        boolean isMotion = false;
        switch (activity) {
            case UNDEFINED:
                break;
            case JOGGING:
                isMotion = true;
                break;
            case SITTING:
                isMotion = false;
                break;
            case STANDING:
                isMotion = false;
                break;
            case WALKING:
                isMotion = true;
                break;
        }
        return isMotion;
    }


    private class MotionEstimationThread extends TimerTask {
        @Override
        public void run() {
            MotionEstimation.Activity currentActivity = motionEstimation.estimate();
            //mPredictionTextView.setText(String.format("Based on the accelerometer\n data it is likely that you are:\n %s", currentActivity.name()));
            mPredictionTextView.setText(String.format("you are:\n %s", currentActivity.name()));

            if (!executeLocalization) {
                return;
            }

            if(isInMotion == false && isActivityMotion(currentActivity) == true) {
                isInMotion = true;
                stepExecutionHandler.postDelayed(stepExecutionThread, stepFrequency);
                //stepTimer.scheduleAtFixedRate(motionEstimationThread, stepFrequency, stepFrequency); //TODO should we run it all the time or only on start button press

            } else if (isInMotion == true && isActivityMotion(currentActivity) == false){
                isInMotion = false;
                //step timer is stopped in stepExecutionThread
                //TODO end walking
                }
            }
        }


    private class StepExecutionThread implements Runnable {
        @Override
        public void run() {
            if (isInMotion){
                particleFilter.moveParticles(mAverageDegree);
                particleFilter.substitudeInvalidMoves();
                particleFilter.normalizeWeights();
                particleFilter.resampleParticles();
                particleFilter.updateCurrentPosition();

                floorMap.clearImage();
                floorMap.drawParticles(particleFilter.getParticles(), particleFilter.currentPosition);

                stepExecutionHandler.postDelayed(this, stepFrequency);
                //stepExecutionHandler.removeCallbacks(stepExecutionThread);
            } else {
                //don't postDelayed
                //TODO display something
            }
        }
    };


}
