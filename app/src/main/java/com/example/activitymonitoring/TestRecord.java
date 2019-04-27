package com.example.activitymonitoring;

// compared with Record, add another attribute - predictLabel
// which is used to store the predicted label for the current testRecord.

public class TestRecord extends Record{
    int predictedLabel;

    TestRecord(double[] attributes, int classLabel) {
        super(attributes, classLabel);
    }


    // Calculates the features from accelerationRingBuffer and normalizes them
    // todo we need to export the normalization coefficients in the python training script
    TestRecord(double[][] accelertions, int windowLen) {
        super(new double[2], 0);  //useless, but has to be first to comply java standard

        //currently the features are ["x_max", "x_min", "x_mean", "y_max", "y_min", "y_mean", "z_max", "z_min", "z_mean", "n_max", "n_min", "n_mean"]
        double[] attributes;
        int classLabel = 0;

        double x_min = Double.POSITIVE_INFINITY, y_min = Double.POSITIVE_INFINITY, z_min = Double.POSITIVE_INFINITY, n_min = Double.POSITIVE_INFINITY;
        double x_max = Double.NEGATIVE_INFINITY, y_max = Double.NEGATIVE_INFINITY, z_max = Double.NEGATIVE_INFINITY, n_max = Double.NEGATIVE_INFINITY;
        double x_mean = 0, y_mean = 0, z_mean = 0, n_mean = 0;
        for (int i = 0; i < windowLen; i++) {
            //min values
            if(accelertions[i][0] < x_min)
                x_min = accelertions[i][0];
            if(accelertions[i][1] < y_min)
                y_min = accelertions[i][1];
            if(accelertions[i][2] < z_min)
                z_min = accelertions[i][2];
            if(accelertions[i][3] < n_min)
                n_min = accelertions[i][3];

            //max values
            if(accelertions[i][0] > x_max)
                x_max = accelertions[i][0];
            if(accelertions[i][1] > y_max)
                y_max = accelertions[i][1];
            if(accelertions[i][2] > z_max)
                z_max = accelertions[i][2];
            if(accelertions[i][3] > n_max)
                n_max = accelertions[i][3];

            //mean values
            x_mean += accelertions[i][0];
            y_mean += accelertions[i][1];
            z_mean += accelertions[i][2];
            n_mean += accelertions[i][3];
        }
        x_mean = x_mean / windowLen;
        y_mean = y_mean / windowLen;
        z_mean = z_mean / windowLen;
        n_mean = n_mean / windowLen;

        attributes = new double[] {x_max, x_min, x_mean, y_max, y_min, y_mean, z_max, z_min, z_mean, n_max, n_min, n_mean};

       //todo normalize attributes

        super.attributes = attributes;
    }
    //public void CalculateFromAccelerationArray(int K){
}
