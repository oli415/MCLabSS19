package com.example.activitymonitoring;

import java.util.ArrayList;

public class Floor {
    public ArrayList<Room> rooms;
    public ArrayList<Line> walls;

    public Floor(){
        //add rooms
        rooms = new ArrayList<Room>();
        rooms.add(new Room(new Position(8.98,0.11), new Position(14.37, 5.61), 1));
        rooms.add(new Room(new Position(8.98,5.61), new Position(14.37, 10.1), 2));
        rooms.add(new Room(new Position(8.98,10.1), new Position(10.21, 16.73), 3));
        rooms.add(new Room(new Position(8.98,16.73), new Position(14.7, 18.97), 4));
        rooms.add(new Room(new Position(8.98,18.97), new Position(10.21, 23.24), 5));
        rooms.add(new Room(new Position(8.98,23.24), new Position(10.21, 27.17), 6));
        rooms.add(new Room(new Position(4.49,16.73), new Position(8.98, 18.97), 7));
        rooms.add(new Room(new Position(8.98,27.17), new Position(10.21, 31.43), 8));
        rooms.add(new Room(new Position(8.98,31.43), new Position(10.21, 36.26), 9));
        rooms.add(new Room(new Position(8.98,36.26), new Position(10.21, 40.52), 10));

        //add walls
        walls = new ArrayList<Line>();
        
    }
}
