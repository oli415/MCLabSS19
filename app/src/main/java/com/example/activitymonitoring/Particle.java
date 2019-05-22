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
        this.currentPosition = currentPosition;
    }

    public void setLastPosition(Position lastPosition) {
        this.lastPosition = lastPosition;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
