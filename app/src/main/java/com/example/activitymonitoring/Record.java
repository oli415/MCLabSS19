package com.example.activitymonitoring;

//Basic Record class
public class Record {
    double[] attributes;
    int classLabel;

    Record(double[] attributes, int classLabel){
        this.attributes = attributes;
        this.classLabel = classLabel;
    }
}