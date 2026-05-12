package com.elevatorsim.config;

public record SimulationConfig(
    String buildingName,
    int minFloor,
    int maxFloor,
    int elevatorCount,
    long floorTravelTimeMs,
    long doorOpenTimeMs
) {
    public static SimulationConfig createDefault() {
        return new SimulationConfig("Tour Horizon", 0, 9, 2, 1000, 2000);
    }
}
