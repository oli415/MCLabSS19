package com.example.activitymonitoring;

//FileManager
// * ReadFile: read training files and test files
// * OutputFile: output predicted labels into a file

import android.content.res.AssetManager;
import android.util.Log;
import android.content.Context;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.Scanner;

public class FileManager {
    //private MainActivity mainActivity;

    private static final String COMMA_DELIMITER = ",";

    public FileManager(MainActivity activity) {
        //mainActivity = activity;
    }

    public static int readFileNumberOfLines(File file) {
        int linenumber = 0;

        try{
    		if(file.exists()){

    		    FileReader fr = new FileReader(file);
    		    LineNumberReader lnr = new LineNumberReader(fr);

    	        while (lnr.readLine() != null){
    	        	linenumber++;
    	        }
    	        //System.out.println("Total number of lines : " + linenumber);
    	        lnr.close();
    	        fr.close();
    		} else{
    			 Log.e("FileManager", "File does not exists!");
    		}

    	}catch(IOException e){
    		e.printStackTrace();
    	}

        return linenumber;
    }

        public static int readStreamNumberOfLines(InputStream in) {
        int linenumber = 0;

        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

    	    while (reader.readLine() != null){
    	        	linenumber++;
    	    }
    	    reader.close();

    	} catch(IOException e){
    		e.printStackTrace();
    	}

        return linenumber;
    }


    //read training files
    //todo improve: can't handle empty line at the end
    public static TrainRecord[] readTrainFile(String fileName) throws IOException{
        Context appContext = MainActivity.getAppContext();
        AssetManager assetManager =  appContext.getAssets();

        int numOfSamples;
        TrainRecord[] records;

        try {
            String[] files = assetManager.list("");
            if (!Arrays.asList(files).contains(fileName)) {
//                activityMonitoring.setLogText("Error: feature file does not exist");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStream input = assetManager.open(fileName);
        numOfSamples = readStreamNumberOfLines(input) - 1; //1 line header
        input.close();
        //activityMonitoring.setLogText(String.format("num lines %d", numOfSamples));


        //File file = new File(fileName);
        //numOfSamples = readFileNumberOfLines(file);
        int numOfAttributes = 12; //TODO not static
        //Scanner scanner = new Scanner(file).useLocale(Locale.US);
        try {
            input = assetManager.open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(input));

            //read title line
            String TitleLine = br.readLine();

            //transform data from file into TrainRecord objects
            records = new TrainRecord[numOfSamples];
            int index = 0;
            String line = "";
            while ((line = br.readLine()) != null) {
                double[] attributes = new double[numOfAttributes];
                String[] row = line.split(COMMA_DELIMITER);
                int classLabel = -1;

                if(row.length >= numOfAttributes + 3) {   //todo real value
                    //row[0] is index
                    classLabel = (int) Float.parseFloat(row[1]);
                    //row[2] is person id
                    for (int feature_index = 0; feature_index < numOfAttributes; feature_index++) {
                        attributes[feature_index] = Double.parseDouble(row[3 + feature_index]);
                    }
                }
                else{
                    Log.e("FileManager", "TraininsSet row has to less elements");
                }

                records[index] = new TrainRecord(attributes, classLabel);
                index++;
            }
            input.close();
        } catch (Exception e){
            e.printStackTrace();
            return new TrainRecord[0];
        }

        //activityMonitoring.setLogText(String.format("records: %f, %f", records[0].attributes[0], records[0].attributes[11]));
        return records;
    }


    // TODO not supported and not used, as we feed live data
    public static TestRecord[] readTestFile(String fileName) throws IOException{
        File file = new File(fileName);
        //Scanner scanner = new Scanner(file).useLocale(Locale.US);
        Scanner scanner = new Scanner(file);

        //read file
        int NumOfSamples = scanner.nextInt();
        int NumOfAttributes = scanner.nextInt();
        int LabelOrNot = scanner.nextInt();
        scanner.nextLine();

        assert LabelOrNot == 1 : "No classLabel";

        TestRecord[] records = new TestRecord[NumOfSamples];
        int index = 0;
        while(scanner.hasNext()){
            double[] attributes = new double[NumOfAttributes];
            int classLabel = -1;

            //read a whole line for a TestRecord
            for(int i = 0; i < NumOfAttributes; i ++){
                attributes[i] = scanner.nextDouble();
            }

            //read the true lable of a TestRecord which is later used for validation
            classLabel = (int) scanner.nextDouble();
            assert classLabel != -1 : "Reading class label is wrong!";

            records[index] = new TestRecord(attributes, classLabel);
            index ++;
        }

        return records;
    }


    //TODO not supported yet
    public static String outputFile(TestRecord[] testRecords, String trainFilePath) throws IOException{
        //construct the predication file name
        StringBuilder predictName = new StringBuilder();
        for(int i = 15; i < trainFilePath.length(); i ++){
            if(trainFilePath.charAt(i) != '_')
                predictName.append(trainFilePath.charAt(i));
            else
                break;
        }
        String predictPath = "classification\\"+predictName.toString()+"_prediction.txt";

        //ouput the prediction labels
        File file = new File(predictPath);
        if(!file.exists())
            file.createNewFile();

        FileWriter fw = new FileWriter(file);
        BufferedWriter bw = new BufferedWriter(fw);

        for(int i =0; i < testRecords.length; i ++){
            TestRecord tr = testRecords[i];
            bw.write(Integer.toString(tr.predictedLabel));
            bw.newLine();
        }

        bw.close();
        fw.close();

        return predictPath;
    }
}
