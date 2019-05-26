package com.example.activitymonitoring;

public class Room {
    private Position bottomLeftCorner;
    private Position topRightCorner;
    private int id;

    public Room(Position bottomLeftCorner, Position topRightCorner, int id){
        this.bottomLeftCorner = new Position(bottomLeftCorner);
        this.topRightCorner = new Position(topRightCorner);
        this.id = id;
    }

    public Position getBottomLeftCorner() {
        return bottomLeftCorner;
    }

    public Position getTopRightCorner() {
        return topRightCorner;
    }

    /*
    public RectF getRect() {
        //left, top, right, bottom
        RectF r = new RectF((float)bottomLeftCorner.getX(), (float)topRightCorner.getY(), (float)topRightCorner.getX(), (float)bottomLeftCorner.getY());
        return r;
    }
    */
}
