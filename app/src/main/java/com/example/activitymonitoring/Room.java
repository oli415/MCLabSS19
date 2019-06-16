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

    public float getArea() {
        float a = (float) ((topRightCorner.getX() -  bottomLeftCorner.getX()) * (topRightCorner.getY() - bottomLeftCorner.getY()));
        if(a<0 ) {
            return -a;
        } else {
            return a;
        }
    }
}
