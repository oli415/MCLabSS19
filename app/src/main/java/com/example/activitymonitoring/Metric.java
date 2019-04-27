package com.example.activitymonitoring;

// interface for the different distance metrics
public interface Metric {
    double getDistance(Record s, Record e);
}
