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
import android.view.KeyEvent;
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
//    private final int degreeBufferSize = 100;
//    private float[] mCurrentDegreeBuffer = new float[degreeBufferSize];
//    private int mCurrentDegreeIndex = 0;
//    private float mAverageDegree = 0;
static int accCount=0, magCount=0; //TODO remove
    private float degreeExponentialMovingAverage = 0;
    private float degreeExponentialMovingAverageCorrected = 0;
    private float degreeCurrent = 0;
    private float directionOffset = -90;  //taken from preferences
    private int stepPeriode = 1000; //in ms //taken from preferences

//Gui-Stuff
    private TextView mDirectionTextView;
    private TextView mPredictionTextView;
    private TextView mPositionTextView;
    private TextView mRoomTextView;
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
    private boolean manualDirectionEnabled;
    boolean lowVarianceResamplingEnabled;
    boolean compassDirectionIsTrueDirection;

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
    private Position currentPosition;
    private int currentRoomId;

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
        stepPeriode = preferences.getInt("step_period", 0);
        directionOffset = preferences.getInt("direction_offset", 0);

        double stepLengthMeter = (double)preferences.getInt("step_length", 0) / 1000.0d;
        int directionUncertainty = preferences.getInt("direction_uncertainty", 0);
        int lengthUncertainty = preferences.getInt("length_uncertainty", 0);



        manualDirectionEnabled = preferences.getBoolean("manual_direction_enabled", false);
        lowVarianceResamplingEnabled = preferences.getBoolean("low_variance_resampling_enabled", false);
        compassDirectionIsTrueDirection = preferences.getBoolean("compass_is_true_direction", true);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mDirectionTextView = findViewById(R.id.textDirection);
        mPredictionTextView = findViewById(R.id.textPrediction);
        mPositionTextView = findViewById(R.id.textRoom);
        mRoomTextView = findViewById(R.id.textRoom);


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
        directionSeekBar.setMax(360);
        if(manualDirectionEnabled == false) {
            directionSeekBar.setVisibility(View.INVISIBLE);
        }

        particleFilter = new ParticleFilter(stepLengthMeter, lengthUncertainty, directionUncertainty);

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
                floorMap.drawRooms(floor.rooms, 0);
            }
        });
        btnDrawRooms.setVisibility(View.INVISIBLE);

        btnClearImage= findViewById(R.id.btnClearImage);
        btnClearImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floorMap.clearImage();
            }
        });
        btnClearImage.setVisibility(View.INVISIBLE);

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
                floorMap.clearImage();
            }

        });

   }

    //@Override
    //public void onBackPressed() {
    //    Log.i("indoor", "####################backPressed\n");
    //    executeLocalization = false;
    //    super.onBackPressed();
    //}

    @Override
   public boolean onKeyDown(int keyCode, KeyEvent event)  {
            if (keyCode == KeyEvent.KEYCODE_BACK ) {
                // do something on back.
                executeLocalization = false;
                Log.i("indoor", "backbutton\n");
                return super.onKeyDown(keyCode, event);
                //return true;
            }

            return super.onKeyDown(keyCode, event);
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
                accCount = accCount +1;
                //if(accCount == 10) {
                    motionEstimation.addAccelerationValues(event.values[0], event.values[1], event.values[2]);
                //    accCount = 0;
                //}
                //for orientation:
                System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
                mLastAccelerometerSet = true; //TODO are they reset anywhere?
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
                mLastMagnetometerSet = true;

                SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
                SensorManager.getOrientation(mR, mOrientation);
                float azimuthInRadians = mOrientation[0];
                degreeCurrent = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

                if((degreeCurrent - degreeExponentialMovingAverage) > 180.0f) {
                    degreeExponentialMovingAverage += 360;
                } else if(( degreeCurrent - degreeExponentialMovingAverage) < -180.0f){
                    degreeCurrent += 360;
                }
                float a = 0.960789f;
                magCount++;
                degreeExponentialMovingAverage = a*degreeExponentialMovingAverage + (1-a)* degreeCurrent;
                degreeExponentialMovingAverage = degreeExponentialMovingAverage % 360;

                degreeExponentialMovingAverageCorrected = (degreeExponentialMovingAverage  + directionOffset) % 360;

                //overwrite direction in manual mode
                if(manualDirectionEnabled) {
                    degreeExponentialMovingAverageCorrected = directionSeekBar.getProgress();
                }

                float needle_direction;
                if(compassDirectionIsTrueDirection) {
                     needle_direction = degreeExponentialMovingAverageCorrected;
                } else { //display north
                    needle_direction = -degreeExponentialMovingAverageCorrected;  //todo for manual mode value with correction not that useful, but here it is in general not that useful
                }
                compassNeedleImageView.setRotation(needle_direction);

                if(magCount == 10)  {
                    mDirectionTextView.setText(String.format("Direction: %f", degreeExponentialMovingAverageCorrected));
                   magCount = 0;
                }
            default:
                    //do nothing
        }

        // sampling in GAMING_MODE: all T=20ms
        //                                                Tau=1.0   ->  a =               =0.980198
        //exponential moving average: a = e^(-T/ Tau)=>   Tau= 0.5  ->  a = e^(-0.02/0.5) =0.960789
        //                                                Tau=0.2   ->  a =               =0.904837
        //                                                Tau=0.1   ->  a =               =0.818731


        //TODO consider offloading this to orientation class?
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
//            Log.i("sensor", String.format("acc: %d      magnet: %d", accCount, magCount));

            /*
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            Log.i("sensor", String.format("acc: x:%f, y:%f, z:%f ------magneto: x:%f, y:%f, z:%f",
                    mLastAccelerometer[0], mLastAccelerometer[1], mLastAccelerometer[2],
                    mLastMagnetometer[0], mLastMagnetometer[1], mLastMagnetometer[2] ));
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
                if(manualDirectionEnabled) {
                    mAverageDegree = directionSeekBar.getProgress(); //TODO seekBar overwrites measured value
                } else {
                    mAverageDegree = mAverageDegree / degreeBufferSize;
                }
                //mAverageDegree = (mAverageDegree + directionOffset) % 360; //TODO add offset
                mDirectionTextView.setText(String.format("Direction: %f", mAverageDegree));

                compassNeedleImageView.setRotation(mAverageDegree);

            }
  */    }
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
                isMotion = true;
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
            mPredictionTextView.setText(String.format("you are: %s", currentActivity.name()));


            if(executeLocalization && isInMotion == false && isActivityMotion(currentActivity) == true) {
                isInMotion = true;
                stepExecutionHandler.postDelayed(stepExecutionThread, stepPeriode);
                //stepTimer.scheduleAtFixedRate(motionEstimationThread, stepFrequency, stepFrequency); //TODO should we run it all the time or only on start button press

            } else if ( isInMotion == true &&( !executeLocalization || isActivityMotion(currentActivity) == false)){
                isInMotion = false;
                stepExecutionHandler.removeCallbacks(stepExecutionThread);
                Log.i("indoorLocalization", "stopping");
                //step timer is stopped in stepExecutionThread
                //TODO end walking
                }
            }
        }


    private class StepExecutionThread implements Runnable {
        @Override
        public void run() {
            if (isInMotion){
                particleFilter.moveParticles(degreeExponentialMovingAverageCorrected);
                particleFilter.removeInvalidMoves();
                particleFilter.normalizeWeights();

                //particleFilter.resampleParticles();
                if(lowVarianceResamplingEnabled) {
                    particleFilter.lowVarianceSampler();
                    //particleFilter.resampleParticles();
                } else {
                    particleFilter.randomSampler();
                }

                particleFilter.updateCurrentPosition();
                currentPosition = particleFilter.getCurrentPosition();
                currentRoomId = floor.getRoomIdForPosition(currentPosition);

                mPositionTextView.setText(String.format("Position: x:%f,  y:%f", currentPosition.getX(), currentPosition.getY()));
                mRoomTextView.setText(String.format("Room: %d", currentRoomId));
                Log.i("indoorLocalization", String.format("executed one step periode: %d",stepPeriode ));

                floorMap.clearImage();
                floorMap.drawRooms(floor.rooms, currentRoomId);
                floorMap.drawParticles(particleFilter.getParticles(), particleFilter.getCurrentPosition());

                stepExecutionHandler.postDelayed(this, stepPeriode);
                //stepExecutionHandler.removeCallbacks(stepExecutionThread);
            } else {
                //don't postDelayed
                //TODO display something
            }
        }
    };


}
