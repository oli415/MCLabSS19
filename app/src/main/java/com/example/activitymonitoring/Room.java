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
}
