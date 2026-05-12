package com.elevatorsim.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Elevator extends BuildingComponent {

    public static final int MAX_CAPACITY = 6;

    private int currentFloor;
    private Direction direction;
    private ElevatorState state;
    private int currentPassengers;
    private boolean lastStopWasHallCall;

    private final Queue<Destination> destinations;
    private final List<Request> servedRequests;
    private final ReentrantLock lock;
    private final Condition newRequestCondition;
    private volatile boolean running;
    private int passengersServed;
    private int totalFloorsTraversed;
    private long lastWaitTimeMs;

    private static class Destination {
        final int floor;
        final Request request;

        Destination(int floor, Request request) {
            this.floor = floor;
            this.request = request;
        }
    }

    public Elevator(int id, int minFloor, int maxFloor) {
        super(id, minFloor, maxFloor);
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.state = ElevatorState.IDLE;
        this.destinations = new LinkedList<>();
        this.servedRequests = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.newRequestCondition = lock.newCondition();
        this.running = true;
        this.passengersServed = 0;
        this.totalFloorsTraversed = 0;
        this.lastWaitTimeMs = 0;
        this.currentPassengers = 0;
        this.lastStopWasHallCall = false;
    }

    public void addDestination(int floor, Request request) {
        lock.lock();
        try {
            if (floor < getMinFloor() || floor > getMaxFloor()) {
                return;
            }
            if (request != null) {
                request.setAssignedElevatorId(id);
            }
            destinations.add(new Destination(floor, request));
            newRequestCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int getNextDestination() {
        lock.lock();
        try {
            Destination d = destinations.peek();
            return d != null ? d.floor : -1;
        } finally {
            lock.unlock();
        }
    }

    public boolean arrivedAtFloor() {
        lock.lock();
        try {
            Iterator<Destination> it = destinations.iterator();
            while (it.hasNext()) {
                Destination d = it.next();
                if (d.floor == currentFloor) {
                    it.remove();
                    passengersServed++;
                    Request served = d.request;
                    if (served != null) {
                        lastWaitTimeMs = served.getWaitTimeMs();
                        lastStopWasHallCall = served.getType() == Request.Type.HALL_CALL;
                        if (lastStopWasHallCall) {
                            currentPassengers++;
                        } else {
                            currentPassengers--;
                        }
                    }
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean hasDestinations() {
        lock.lock();
        try {
            return !destinations.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public int getPendingCount() {
        lock.lock();
        try {
            return destinations.size();
        } finally {
            lock.unlock();
        }
    }

    public List<Integer> getPendingDestinations() {
        lock.lock();
        try {
            List<Integer> all = new ArrayList<>();
            for (Destination d : destinations) {
                all.add(d.floor);
            }
            Collections.sort(all);
            return all;
        } finally {
            lock.unlock();
        }
    }

    public void moveOneFloor() {
        lock.lock();
        try {
            if (direction == Direction.UP && currentFloor < getMaxFloor()) {
                currentFloor++;
                totalFloorsTraversed++;
            } else if (direction == Direction.DOWN && currentFloor > getMinFloor()) {
                currentFloor--;
                totalFloorsTraversed++;
            }
        } finally {
            lock.unlock();
        }
    }

    public void waitForRequest() throws InterruptedException {
        lock.lock();
        try {
            while (destinations.isEmpty() && running) {
                newRequestCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            running = false;
            newRequestCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int getCurrentFloor() {
        lock.lock();
        try {
            return currentFloor;
        } finally {
            lock.unlock();
        }
    }

    public Direction getDirection() {
        lock.lock();
        try {
            return direction;
        } finally {
            lock.unlock();
        }
    }

    public ElevatorState getState() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    public void setState(ElevatorState state) {
        lock.lock();
        try {
            this.state = state;
        } finally {
            lock.unlock();
        }
    }

    public void setDirection(Direction direction) {
        lock.lock();
        try {
            this.direction = direction;
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPassengersServed() {
        lock.lock();
        try {
            return passengersServed;
        } finally {
            lock.unlock();
        }
    }

    public int getTotalFloorsTraversed() {
        lock.lock();
        try {
            return totalFloorsTraversed;
        } finally {
            lock.unlock();
        }
    }

    public long getLastWaitTimeMs() {
        lock.lock();
        try {
            return lastWaitTimeMs;
        } finally {
            lock.unlock();
        }
    }

    public int getCurrentPassengers() {
        lock.lock();
        try {
            return currentPassengers;
        } finally {
            lock.unlock();
        }
    }

    public boolean isLastStopWasHallCall() {
        lock.lock();
        try {
            return lastStopWasHallCall;
        } finally {
            lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public String getDisplayName() {
        return "Ascenseur " + (id + 1);
    }

    @Override
    public String toString() {
        return String.format("%s | Etage %d | %s | %s | %d en attente",
                getDisplayName(), getCurrentFloor(),
                getDirection().getLabel(), getState().getLabel(),
                getPendingCount());
    }
}
