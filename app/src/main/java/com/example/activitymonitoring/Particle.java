package com.example.activitymonitoring;

public class Particle {
    private Position currentPosition;
    private Position lastPosition;
    private double weight;

    public Particle(Position currentPosition, Position lastPosition, double weight) {
        this.currentPosition = new Position(currentPosition);
        this.lastPosition = new Position(lastPosition);
        this.weight = weight;
    }

    public Particle(Particle particle){
        this.currentPosition = new Position(particle.currentPosition);
        this.lastPosition = new Position(particle.lastPosition);
        this.weight = particle.weight;
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public Position getLastPosition() {
        return lastPosition;
    }

    public double getWeight() {
        return weight;
    }

    public void setCurrentPosition(Position currentPosition) {
        this.currentPosition = new Position(currentPosition);
    }

    public void setLastPosition(Position lastPosition) {
        this.lastPosition = new Position(lastPosition);
    }

    /**
     * copies current position to last position and moves the current position by relative x/y value
     * @param x distance in meter the particle currentPosition is moved in to x direction
     * @param y distance in meter the particle currentPosition is moved in to y direction
     */
    public void moveRelative(double x, double y) {
        this.lastPosition = new Position(this.currentPosition);
        this.currentPosition.setX(this.currentPosition.getX() + x);
        this.currentPosition.setY(this.currentPosition.getY() + y);
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
