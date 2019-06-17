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
    double scaleX;
    double scaleY;

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

        //https://online.tugraz.at/tug_online/ris.ris?pOrgNr=2337&pQuellGeogrBTypNr=5&pZielGeogrBTypNr=5&pZielGeogrBerNr=3010001&pRaumNr=4883&pActionFlag=A&pShowEinzelraum=J
        floorMapHeightMeter = 48.83;  //TODO more accurate only fast approximation
        floorMapWidthMeter = 15.04;
        scaleX = floorMapWidthPixel / (float)floorMapWidthMeter;
        scaleY = floorMapHeightPixel / (float)floorMapHeightMeter;

        /*Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth((float)5.0);
        liveFloorMapCanvas.drawLine((float)5.0, (float)5.0, (float)10.0, (float)100.0, paint);  //TODO only test
        liveFloorMapCanvas.drawLine((float)5.0, (float)5.0, (float)floorMapWidthPixel, (float)floorMapHeightPixel, paint);  //TODO only test
        */

        imageView.setImageBitmap(liveFloorMap);

        Log.i("FloorMap", String.format("canvas height: %d", floorMapHeightPixel));
        Log.i("FloorMap", String.format("canvas width: %d", floorMapWidthPixel));
    }

    private int xToMapPixel(double x_m) {
        return  (int)(x_m * scaleX);
        //return  (float) floorMapHeightPixel - x_m * scaleX;
    }

    private int yToMapPixel(double y_m) {
       return  floorMapHeightPixel -  (int)(y_m * scaleY);
       //return  y_m * scaleY;
    }



    private void drawRoom(Room room, Paint paint) {

        RectF r = new RectF(xToMapPixel(room.getBottomLeftCorner().getX()),
                yToMapPixel(room.getTopRightCorner().getY()),
                xToMapPixel(room.getTopRightCorner().getX()),
                yToMapPixel(room.getBottomLeftCorner().getY()));
        //RectF r = room.getRect();
        liveFloorMapCanvas.drawRect(r, paint);
        imageView.setImageBitmap(liveFloorMap);
    }

    public void drawRooms(ArrayList<Room> rooms, int highlightRoomId) {
        Paint paint = new Paint();
        //paint.setColor(Color.GREEN);
        //TODO transparent: https://stackoverflow.com/questions/30169507/android-how-to-set-color-value-to-transparent
        paint.setColor(Color.parseColor("#5500FFFF"));
        paint.setStyle(Paint.Style.STROKE); //don't fill
        paint.setStrokeWidth(5.0f);

        for(Room room : rooms) {
            if(room.getId() == highlightRoomId) {
                paint.setColor(Color.parseColor("#55FF2200"));
                paint.setStrokeWidth(15.0f);
                drawRoom(room, paint);
                paint.setColor(Color.parseColor("#5500FFFF"));
                paint.setStrokeWidth(5.0f);
                Log.i("floormap", String.format("highligth room", highlightRoomId));
            }
            drawRoom(room, paint);
        }
        imageView.setImageBitmap(liveFloorMap);
    }

    private void drawParticle(Particle particle, Paint paint) {
        liveFloorMapCanvas.drawCircle(
                xToMapPixel(particle.getCurrentPosition().getX()),
                yToMapPixel(particle.getCurrentPosition().getY()),
                3,
                paint);
        imageView.setImageBitmap(liveFloorMap);
    }

    public void drawParticles(Particle[] particles, Position currentPosition) {
        Paint paint = new Paint();
        //paint.setColor(Color.GREEN);
        // transparent: https://stackoverflow.com/questions/30169507/android-how-to-set-color-value-to-transparent
        //              Adding 00 in the beginning will make it 100% transparent and adding FF will make it 100% solid.
        paint.setColor(Color.parseColor("#3300DD00")); //green
        //paint.setStyle(Paint.Style.STROKE); //don't fill
        //paint.setStrokeWidth((float)5.0);

        for(Particle particle : particles) {
            drawParticle(particle, paint);
        }
        imageView.setImageBitmap(liveFloorMap);

        // draw highest weighted particle in red (position estimate)
        Paint mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);

        liveFloorMapCanvas.drawCircle(xToMapPixel(currentPosition.getX()), yToMapPixel(currentPosition.getY()), 5, mPaint);

        imageView.setAdjustViewBounds(true);
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
