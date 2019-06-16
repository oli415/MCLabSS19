package com.example.activitymonitoring;

/**
 * a position in the two dimensional space
 */
public class Position {
    private double x;
    private double y;

    public Position(){
        x = 0.0;
        y = 0.0;
    }

    public Position(double x, double y){
        this.x = x;
        this.y = y;
    }

    public Position(Position position){
        this.x = position.x;
        this.y = position.y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }
}
