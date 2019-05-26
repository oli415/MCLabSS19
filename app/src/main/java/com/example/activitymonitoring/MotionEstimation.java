package com.example.activitymonitoring;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;

public class MotionEstimation {

    private String filename = "AM_data.txt";
    private String filepath = "ActivityMonitoring";
    File myExternalFile;
    String myData = "";

    private String activityLabels[];

    private int accelerationWindowLength;
    private int knnNeighborCount;

    private Context appContext;

    // ring buffer of size window-length x 4 (x,y,z,n)
    // n = squared absolute value: direction independent n as sum of squares (of x,y,z)
    private double[][] accelerationRingBuffer;
    // index of the oldest inserted acceleration values
    private int accelerationRingBufferIndex;


    private Knn knn;

    //public MotionEstimation(String trainFileName, int knnMetric, int accelerationWindowLength, int knnNeighborCount, int featureCount, int predictIntervallms) {
    public MotionEstimation() {

        appContext = MainActivity.getAppContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        accelerationWindowLength = preferences.getInt("window_length", 0);
        knnNeighborCount = preferences.getInt("knn_neighbor_count", 0);

        //allocate acceleration Ring buffer
        int window_length = preferences.getInt("window_length", 0);
        accelerationRingBuffer = new double[window_length][4];

        String[] label = {"-", "Jogging", "Sitting", "Standing", "Walking"};
        activityLabels = label;

        // instantiate Knn Singelton, and store reference for simpler access
        knn = Knn.getInstance();
    }

    /**
     * Returns the estimated Activity
     * depends on acceleration values, updated at the same periode as those values in the reference file
     *
     * @return the string identifying the activity
     */
    public String estimate() {
        TestRecord testEntry = new TestRecord(accelerationRingBuffer, accelerationWindowLength);
        int activity_id = knn.execute(knnNeighborCount, testEntry);

        return activityLabels[activity_id];
    }

    public void addAccelerationValues(float x, float y, float z)
    {
        accelerationRingBuffer[accelerationRingBufferIndex][0] = x;
        accelerationRingBuffer[accelerationRingBufferIndex][1] = y;
        accelerationRingBuffer[accelerationRingBufferIndex][2] = z;
        accelerationRingBuffer[accelerationRingBufferIndex][3] =
            Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2);
        accelerationRingBufferIndex = (accelerationRingBufferIndex + 1) % accelerationWindowLength;

    }
}
