package com.elevatorsim.model;

public abstract class BuildingComponent {

    protected final int id;
    protected final int minFloor;
    protected final int maxFloor;

    protected BuildingComponent(int id, int minFloor, int maxFloor) {
        this.id = id;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
    }

    public int getId() {
        return id;
    }

    public int getMinFloor() {
        return minFloor;
    }

    public int getMaxFloor() {
        return maxFloor;
    }

    public abstract String getDisplayName();
}
