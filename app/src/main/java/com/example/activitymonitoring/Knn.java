package com.example.activitymonitoring;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

// class knn is defined as Singleton
public class Knn {

    private static Knn instance = null;

    private Context appContext;
    private MainActivity mainActivity;

    private TrainRecord[] trainingSet;
    private double feature_mean_values[];
    private double feature_std_values[];
    private TestRecord currentTestingEntry;

    //int metricType;
    private Metric metric;
    private int featureCount;

    private String featureFilename = "";


    private Knn() {
        // get the application context that we can access content like the Shared Preferences
        appContext = MainActivity.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        featureFilename = preferences.getString("feature_filename" , featureFilename);
        int metricType = preferences.getInt("knn_metric", 2);
        featureCount = preferences.getInt("feature_count", 0);

        mainActivity = (MainActivity) MainActivity.getMainActivity();
        TextView logTextView = mainActivity.findViewById(R.id.textLog);
        logTextView.setText("loaded feature filename: %s".format( featureFilename));


        // metricType should be within [0,2];  //TODO now only type 2 is supported: return probably not approbriate
        if(metricType > 2 || metricType <0){
            System.out.println("metricType is not within the range [0,2]. Please try again later");
            return;
        }


        try {
            //read trainingSet and testingSet
            trainingSet =  FileManager.readTrainFile(mainActivity, featureFilename);

            //TODO support additional metrics
            //determine the type of metric according to metricType
//            if(metricType == 0)
//                metric = new CosineSimilarity();
//            else if(metricType == 1)
//                metric = new L1Distance();
//            else if (metricType == 2)
            if (metricType == 2)
                metric = new EuclideanDistance();
            else{
                System.out.println("The entered metric_type is wrong!");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        normalizeTrainSet();
    }

    // instantiate and get Knn Singelton
    public static Knn getInstance() {
        if (instance == null) {
            instance = new Knn();
        }
        return instance;
    }

    //execute knn for given k and testEntry(the given testEntry is not normalized yet)
    // takes 30 to 60 ms for 43071 entry feature-set with 12 features each (wisdom)
    public int execute(int K, TestRecord testEntry){
        //get the current time
        //final long startTime = System.currentTimeMillis();

        // make sure the input arguments are legal
        if(K <= 0){
            Log.e("KNN", "K should be larger than 0!");
            return -1;
        }
        currentTestingEntry = testEntry;
        normalizeCurrentTestingEntry();

        TrainRecord[] neighbors = findKNearestNeighbors(trainingSet, currentTestingEntry, K, metric);
        int classLabel = classify(neighbors);
        currentTestingEntry.predictedLabel = classLabel; //assign the predicted lable to TestRecord

        //final long endTime = System.currentTimeMillis();
        //System.out.println("Total excution time: "+(endTime - startTime) / (double)1000 +" seconds.");
        return classLabel;
    }

    // normalizes the train-set and sets the mean and std value member of the class to be able to also normalize the live data
    void normalizeTrainSet(){
        feature_mean_values = new double[featureCount];
        feature_std_values = new double[featureCount];

        // mean value
        int train_index = 0, feature_index = 0;
        for (train_index = 0; train_index < trainingSet.length; train_index++) {
            for (feature_index = 0; feature_index < featureCount; feature_index++) {
                feature_mean_values[feature_index] += trainingSet[train_index].attributes[feature_index];
            }
        }
        for (feature_index = 0; feature_index < featureCount; feature_index++) {
            feature_mean_values[feature_index] /= trainingSet.length;
        }

        // std-deviation is sqrt(  sum( (X - mean)^2 )  / (n-1)  )
        for (train_index = 0; train_index < trainingSet.length; train_index++) {
            for (feature_index = 0; feature_index < featureCount; feature_index++) {
                feature_std_values[feature_index] += Math.pow( trainingSet[train_index].attributes[feature_index] - feature_mean_values[feature_index], 2 );
            }
        }
        for (feature_index = 0; feature_index < featureCount; feature_index++) {
            feature_std_values[feature_index] = Math.sqrt( feature_std_values[feature_index] / (trainingSet.length - 1));
        }

        // normalize
        for (train_index = 0; train_index < trainingSet.length; train_index++) {
            for (feature_index = 0; feature_index < featureCount; feature_index++) {
                trainingSet[train_index].attributes[feature_index] = (trainingSet[train_index].attributes[feature_index] - feature_mean_values[feature_index]) / feature_std_values[feature_index];
            }
        }
    }

    //normalizes the currentTesingEntry member, given the Trainset is already read in and normalized
    void normalizeCurrentTestingEntry() {
        for (int feature_index = 0; feature_index < featureCount; feature_index++) {
            currentTestingEntry.attributes[feature_index] = (currentTestingEntry.attributes[feature_index] - feature_mean_values[feature_index]) / feature_std_values[feature_index];
        }
    }

    // Find K nearest neighbors of testRecord within trainingSet
    TrainRecord[] findKNearestNeighbors(TrainRecord[] trainingSet, TestRecord testRecord,int K, Metric metric){
        int NumOfTrainingSet = trainingSet.length;
        assert K <= NumOfTrainingSet : "K is lager than the length of trainingSet!";

        //Update KNN: take the case when testRecord has multiple neighbors with the same distance into consideration
        //Solution: Update the size of container holding the neighbors
        TrainRecord[] neighbors = new TrainRecord[K];

        //initialization, put the first K trainRecords into the above arrayList
        int index;
        for(index = 0; index < K; index++){
            trainingSet[index].distance = metric.getDistance(trainingSet[index], testRecord);
            neighbors[index] = trainingSet[index];
        }

        //go through the remaining records in the trainingSet to find K nearest neighbors
        for(index = K; index < NumOfTrainingSet; index ++){
            trainingSet[index].distance = metric.getDistance(trainingSet[index], testRecord);

            //get the index of the neighbor with the largest distance to testRecord
            int maxIndex = 0;
            for(int i = 1; i < K; i ++){
                if(neighbors[i].distance > neighbors[maxIndex].distance)
                    maxIndex = i;
            }

            //add the current trainingSet[index] into neighbors if applicable
            if(neighbors[maxIndex].distance > trainingSet[index].distance)
                neighbors[maxIndex] = trainingSet[index];
        }

        return neighbors;
    }

    // Get the class label by using neighbors
    int classify(TrainRecord[] neighbors){
        //construct a HashMap to store <classLabel, weight>
        HashMap<Integer, Double> map = new HashMap<Integer, Double>();
        int num = neighbors.length;

        for(int index = 0;index < num; index ++){
            TrainRecord temp = neighbors[index];
            int key = temp.classLabel;

            //if this classLabel does not exist in the HashMap, put <key, 1/(temp.distance)> into the HashMap
            if(!map.containsKey(key))
                map.put(key, 1 / temp.distance);

                //else, update the HashMap by adding the weight associating with that key
            else{
                double value = map.get(key);
                value += 1 / temp.distance;
                map.put(key, value);
            }
        }

        //Find the most likely label
        double maxSimilarity = 0;
        int returnLabel = -1;
        Set<Integer> labelSet = map.keySet();
        Iterator<Integer> it = labelSet.iterator();

        //go through the HashMap by using keys
        //and find the key with the highest weights
        while(it.hasNext()){
            int label = it.next();
            double value = map.get(label);
            if(value > maxSimilarity){
                maxSimilarity = value;
                returnLabel = label;
            }
        }

        return returnLabel;
    }

    String extractGroupName(String filePath){
        StringBuilder groupName = new StringBuilder();
        for(int i = 15; i < filePath.length(); i ++){
            if(filePath.charAt(i) != '_')
                groupName.append(filePath.charAt(i));
            else
                break;
        }

        return groupName.toString();
    }
}

