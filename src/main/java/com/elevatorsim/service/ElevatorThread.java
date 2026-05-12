package com.elevatorsim.service;

import com.elevatorsim.config.SimulationConfig;
import com.elevatorsim.model.Direction;
import com.elevatorsim.model.Elevator;
import com.elevatorsim.model.ElevatorState;
import com.elevatorsim.model.Request;
import com.elevatorsim.utils.LogService;
import java.util.function.Function;

public class ElevatorThread implements Runnable {

    private final Elevator elevator;
    private final LogService logService;
    private final SimulationConfig config;

    private volatile Runnable onStateChanged;
    private volatile Function<Integer, Integer> onHallCallArrival;

    public ElevatorThread(Elevator elevator, SimulationConfig config) {
        this.elevator = elevator;
        this.config = config;
        this.logService = LogService.getInstance();
    }

    public void setOnStateChanged(Runnable onStateChanged) {
        this.onStateChanged = onStateChanged;
    }

    public void setOnHallCallArrival(Function<Integer, Integer> onHallCallArrival) {
        this.onHallCallArrival = onHallCallArrival;
    }

    @Override
    public void run() {
        logService.log(elevator.getDisplayName(), "Thread demarre");

        while (elevator.isRunning()) {
            try {
                if (!elevator.hasDestinations()) {
                    elevator.setDirection(Direction.IDLE);
                    elevator.setState(ElevatorState.IDLE);
                    notifyUI();
                    logService.log(elevator.getDisplayName(), "En attente de requetes...");
                    elevator.waitForRequest();
                    if (!elevator.isRunning()) break;
                }

                processDestinations();

            } catch (InterruptedException e) {
                if (!elevator.isRunning()) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }

        logService.log(elevator.getDisplayName(), "Thread arrete");
    }

    private void processDestinations() throws InterruptedException {
        while (elevator.hasDestinations() && elevator.isRunning()) {
            int nextDest = elevator.getNextDestination();
            if (nextDest == -1) break;

            int currentFloor = elevator.getCurrentFloor();
            if (nextDest > currentFloor) {
                elevator.setDirection(Direction.UP);
                elevator.setState(ElevatorState.MOVING);
            } else if (nextDest < currentFloor) {
                elevator.setDirection(Direction.DOWN);
                elevator.setState(ElevatorState.MOVING);
            } else {
                elevator.setDirection(Direction.IDLE);
            }
            notifyUI();

            while (elevator.getCurrentFloor() != nextDest && elevator.isRunning()) {
                int fromFloor = elevator.getCurrentFloor();

                Thread.sleep(config.floorTravelTimeMs());

                elevator.moveOneFloor();
                int toFloor = elevator.getCurrentFloor();

                logService.logMovement(elevator.getId(), fromFloor, toFloor,
                        elevator.getDirection().getLabel());
                notifyUI();

                if (elevator.arrivedAtFloor()) {
                    handleStop();
                }
            }

            if (elevator.getCurrentFloor() == nextDest && elevator.isRunning()) {
                if (elevator.arrivedAtFloor()) {
                    handleStop();
                }
            }
        }
    }

    private void handleStop() throws InterruptedException {
        int floor = elevator.getCurrentFloor();
        elevator.setState(ElevatorState.STOPPED);
        notifyUI();
        long waitTime = elevator.getLastWaitTimeMs();
        logService.logStop(elevator.getId(), floor);
        if (waitTime > 0) {
            logService.logWaitTime(elevator.getId(), floor, waitTime);
        }

        if (elevator.isLastStopWasHallCall() && onHallCallArrival != null) {
            int targetFloor = onHallCallArrival.apply(floor);
            if (targetFloor >= elevator.getMinFloor() && targetFloor <= elevator.getMaxFloor()
                    && targetFloor != floor) {
                Request cabRequest = new Request(floor, targetFloor);
                elevator.addDestination(targetFloor, cabRequest);
                logService.logCabCall(elevator.getId(), targetFloor);
            }
        } else {
            Thread.sleep(config.doorOpenTimeMs());
        }
    }

    private void notifyUI() {
        if (onStateChanged != null) {
            onStateChanged.run();
        }
    }
}
