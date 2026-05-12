package com.elevatorsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modèle représentant un immeuble avec ses étages et ses ascenseurs.
 */
public class Building {

    private final String name;
    private final int minFloor;
    private final int maxFloor;
    private final int totalFloors;
    private final List<Elevator> elevators;

    /**
     * Crée un immeuble.
     *
     * @param name          nom de l'immeuble
     * @param minFloor      étage minimum (ex: 0 pour RDC)
     * @param maxFloor      étage maximum
     * @param elevatorCount nombre d'ascenseurs
     */
    public Building(String name, int minFloor, int maxFloor, int elevatorCount) {
        this.name = name;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.totalFloors = maxFloor - minFloor + 1;

        this.elevators = new ArrayList<>();
        for (int i = 0; i < elevatorCount; i++) {
            elevators.add(new Elevator(i, minFloor, maxFloor));
        }
    }

    /**
     * Retourne le nom d'affichage d'un étage.
     *
     * @param floor le numéro d'étage
     * @return le label (ex: "RDC", "1er", "2ème", etc.)
     */
    public static String getFloorLabel(int floor) {
        if (floor == 0) return "RDC";
        if (floor == 1) return "1er";
        return floor + "ème";
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public int getMinFloor() {
        return minFloor;
    }

    public int getMaxFloor() {
        return maxFloor;
    }

    public int getTotalFloors() {
        return totalFloors;
    }

    public List<Elevator> getElevators() {
        return Collections.unmodifiableList(elevators);
    }

    public Elevator getElevator(int index) {
        return elevators.get(index);
    }

    @Override
    public String toString() {
        return String.format("Immeuble '%s' | Étages %d à %d | %d ascenseurs",
                name, minFloor, maxFloor, elevators.size());
    }
}
