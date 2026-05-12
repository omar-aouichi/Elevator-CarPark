package com.elevatorsim.service;

import com.elevatorsim.config.SimulationConfig;
import com.elevatorsim.model.Building;
import com.elevatorsim.model.Direction;
import com.elevatorsim.model.Elevator;
import com.elevatorsim.model.Request;
import com.elevatorsim.utils.LogService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ElevatorController {

    private final Building building;
    private final ElevatorScheduler scheduler;
    private final LogService logService;
    private final List<ElevatorThread> elevatorThreads;
    private final SimulationConfig config;
    private ExecutorService executorService;
    private boolean started;
    private Runnable onUIUpdate;

    public ElevatorController(Building building) {
        this(building, SimulationConfig.createDefault());
    }

    public ElevatorController(Building building, SimulationConfig config) {
        this.building = building;
        this.config = config;
        this.scheduler = new ClosestElevatorScheduler();
        this.logService = LogService.getInstance();
        this.elevatorThreads = new ArrayList<>();
        this.started = false;
    }

    public void handleHallCall(int floor) {
        List<Elevator> elevators = building.getElevators();
        Elevator best = scheduler.selectBestElevator(elevators, floor);
        Request request = new Request(floor, Direction.IDLE);
        best.addDestination(floor, request);
        logService.logUserCall(floor, "Appel", best.getId());
    }

    public void handleCabCall(int elevatorId, int targetFloor) {
        Elevator elevator = building.getElevator(elevatorId);
        int currentFloor = elevator.getCurrentFloor();
        Request request = new Request(currentFloor, targetFloor);
        elevator.addDestination(targetFloor, request);
        logService.logCabCall(elevatorId, targetFloor);
    }

    public void start() {
        if (started) return;

        int count = building.getElevators().size();
        executorService = Executors.newFixedThreadPool(count);

        for (Elevator elevator : building.getElevators()) {
            ElevatorThread thread = new ElevatorThread(elevator, config);
            thread.setOnStateChanged(() -> {
                if (onUIUpdate != null) {
                    onUIUpdate.run();
                }
            });
            elevatorThreads.add(thread);
            executorService.submit(thread);
        }

        started = true;
        logService.log("CONTROLEUR", String.format(
                "Simulation demarree - %d ascenseurs actifs dans l'immeuble '%s'",
                count, building.getName()));
    }

    public void stop() {
        if (!started) return;

        logService.log("CONTROLEUR", "Arret de la simulation en cours...");

        for (Elevator elevator : building.getElevators()) {
            elevator.shutdown();
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logService.close();
        started = false;
    }

    public Building getBuilding() {
        return building;
    }

    public void setOnUIUpdate(Runnable onUIUpdate) {
        this.onUIUpdate = onUIUpdate;
    }

    public void setHallCallArrivalCallback(Function<Integer, Integer> callback) {
        for (ElevatorThread thread : elevatorThreads) {
            thread.setOnHallCallArrival(callback);
        }
    }

    public boolean isStarted() {
        return started;
    }
}
