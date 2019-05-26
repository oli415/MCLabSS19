package com.example.activitymonitoring;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;

public class FloorMap {
    Context appContext;

    Bitmap rawFloorMapFactory;
    Canvas liveFloorMapCanvas;

    int floorMapHeightPixel;
    int floorMapWidthPixel;
    double floorMapHeightMeter;
    double floorMapWidthMeter;
    float scaleX;
    float scaleY;

    Bitmap liveFloorMap;

    ImageView imageView;

    public FloorMap(ImageView imageViewFloorMap) {
        appContext = MainActivity.getAppContext();

        Resources res = appContext.getResources();
        rawFloorMapFactory = BitmapFactory.decodeResource(res, R.drawable.floorplan);

        liveFloorMap = rawFloorMapFactory.copy(Bitmap.Config.ARGB_8888, true);
        liveFloorMapCanvas = new Canvas(liveFloorMap);
        floorMapHeightPixel = liveFloorMapCanvas.getHeight();
        floorMapWidthPixel = liveFloorMapCanvas.getWidth();

        this.imageView = imageViewFloorMap;

        https://online.tugraz.at/tug_online/ris.ris?pOrgNr=2337&pQuellGeogrBTypNr=5&pZielGeogrBTypNr=5&pZielGeogrBerNr=3010001&pRaumNr=4883&pActionFlag=A&pShowEinzelraum=J
        floorMapHeightMeter = 58.56;  //TODO more accurate only fast approximation
        floorMapWidthMeter = 14.14;
        scaleX = floorMapWidthPixel / (float)floorMapWidthMeter;
        scaleY = floorMapHeightPixel / (float)floorMapHeightMeter;

        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth((float)5.0);
        //liveFloorMapCanvas.drawLine((float)5.0, (float)5.0, (float)10.0, (float)100.0, paint);  //TODO only test
        liveFloorMapCanvas.drawLine((float)5.0, (float)5.0, (float)floorMapWidthPixel, (float)floorMapHeightPixel, paint);  //TODO only test

        imageView.setImageBitmap(liveFloorMap);

        Log.i("FloorMap", String.format("canvas height: %d", floorMapHeightPixel));
        Log.i("FloorMap", String.format("canvas width: %d", floorMapWidthPixel));
    }

    private float xToMapPixel(float x_m) {
        return  x_m * scaleX;
        //return  (float) floorMapHeightPixel - x_m * scaleX;
    }

    private float yToMapPixel(float y_m) {
       return  (float)floorMapHeightPixel -  y_m * scaleY;
       //return  y_m * scaleY;
    }



    private void drawRoom(Room room, Paint paint) {

        RectF r = new RectF(xToMapPixel((float)room.getBottomLeftCorner().getX()),
                yToMapPixel((float)room.getTopRightCorner().getY()),
                xToMapPixel((float)room.getTopRightCorner().getX()),
                yToMapPixel((float)room.getBottomLeftCorner().getY()));
        //RectF r = room.getRect();
        liveFloorMapCanvas.drawRect(r, paint);
        imageView.setImageBitmap(liveFloorMap);
    }

    public void drawRooms(ArrayList<Room> rooms) {
        Paint paint = new Paint();
        //paint.setColor(Color.GREEN);
        //TODO transparent: https://stackoverflow.com/questions/30169507/android-how-to-set-color-value-to-transparent
        paint.setColor(Color.parseColor("#5500FFFF"));
        paint.setStyle(Paint.Style.STROKE); //don't fill
        paint.setStrokeWidth((float)5.0);

        for(Room room : rooms) {
           drawRoom(room, paint);
        }
        imageView.setImageBitmap(liveFloorMap);
    }

    public void clearImage() {
        liveFloorMap = rawFloorMapFactory.copy(Bitmap.Config.ARGB_8888, true);
        liveFloorMapCanvas = new Canvas(liveFloorMap);
        imageView.setImageBitmap(liveFloorMap);
    }


    public void drawPositionEstimation(){

    }


}
