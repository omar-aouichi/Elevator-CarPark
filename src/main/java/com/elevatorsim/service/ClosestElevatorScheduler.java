package com.elevatorsim.service;

import com.elevatorsim.model.Elevator;
import com.elevatorsim.utils.LogService;
import java.util.List;

public class ClosestElevatorScheduler implements ElevatorScheduler {

    private final LogService logService;

    public ClosestElevatorScheduler() {
        this.logService = LogService.getInstance();
    }

    @Override
    public Elevator selectBestElevator(List<Elevator> elevators, int targetFloor) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            if (e.getCurrentPassengers() >= Elevator.MAX_CAPACITY) {
                logService.log("ALGORITHME",
                        String.format("  %s: complet (%d/6) - ignore", e.getDisplayName(), e.getCurrentPassengers()));
                continue;
            }

            int distance = Math.abs(e.getCurrentFloor() - targetFloor);
            int occupancy = e.getCurrentPassengers();
            int score = distance + occupancy * 2;

            logService.log("ALGORITHME",
                    String.format("  %s: distance=%d, occupants=%d, score=%d",
                            e.getDisplayName(), distance, occupancy, score));

            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }

        if (best != null) {
            logService.log("ALGORITHME",
                    String.format("  -> Choix: %s (score: %d)", best.getDisplayName(), bestScore));
        } else {
            logService.log("ALGORITHME", "  -> Aucun ascenseur disponible (tous complets)");
            best = elevators.get(0);
            int minPassengers = best.getCurrentPassengers();
            for (Elevator e : elevators) {
                if (e.getCurrentPassengers() < minPassengers) {
                    minPassengers = e.getCurrentPassengers();
                    best = e;
                }
            }
        }
        return best;
    }
}
