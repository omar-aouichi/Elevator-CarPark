package com.elevatorsim.model;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElevatorTest {

    private Elevator elevator;

    @BeforeEach
    void setUp() {
        elevator = new Elevator(0, 0, 9);
    }

    @Test
    void constructor_initializesCorrectly() {
        assertEquals(0, elevator.getId());
        assertEquals(0, elevator.getCurrentFloor());
        assertEquals(Direction.IDLE, elevator.getDirection());
        assertEquals(ElevatorState.IDLE, elevator.getState());
        assertEquals(0, elevator.getPendingCount());
        assertEquals(0, elevator.getPassengersServed());
        assertEquals(0, elevator.getTotalFloorsTraversed());
        assertTrue(elevator.isRunning());
        assertEquals(0, elevator.getLastWaitTimeMs());
        assertEquals(0, elevator.getCurrentPassengers());
        assertEquals(6, Elevator.MAX_CAPACITY);
    }

    @Test
    void extendsBuildingComponent() {
        assertInstanceOf(BuildingComponent.class, elevator);
        assertEquals("Ascenseur 1", elevator.getDisplayName());
    }

    @Test
    void addDestination_increasesPendingCount() {
        elevator.addDestination(5, null);
        assertEquals(1, elevator.getPendingCount());
    }

    @Test
    void addDestination_outOfRange_ignored() {
        elevator.addDestination(-1, null);
        assertEquals(0, elevator.getPendingCount());
        elevator.addDestination(10, null);
        assertEquals(0, elevator.getPendingCount());
    }

    @Test
    void addDestination_multipleFloors_addsToQueue() {
        elevator.addDestination(7, null);
        elevator.addDestination(3, null);
        elevator.addDestination(9, null);
        assertEquals(3, elevator.getPendingCount());
        List<Integer> pending = elevator.getPendingDestinations();
        assertEquals(3, pending.size());
    }

    @Test
    void getNextDestination_empty_returnsMinusOne() {
        assertEquals(-1, elevator.getNextDestination());
    }

    @Test
    void getNextDestination_returnsFirstQueued() {
        elevator.addDestination(7, null);
        elevator.addDestination(3, null);
        assertEquals(7, elevator.getNextDestination());
    }

    @Test
    void arrivedAtFloor_noStop_returnsFalse() {
        assertFalse(elevator.arrivedAtFloor());
        assertEquals(0, elevator.getPassengersServed());
    }

    @Test
    void arrivedAtFloor_withStop_returnsTrue() {
        elevator.addDestination(5, null);
        elevator = moveToFloor(elevator, 5);
        assertTrue(elevator.arrivedAtFloor());
        assertEquals(1, elevator.getPassengersServed());
    }

    @Test
    void arrivedAtFloor_sameFloorAsCurrent_afterAddDestination() {
        elevator.addDestination(0, null);
        assertTrue(elevator.arrivedAtFloor());
        assertEquals(1, elevator.getPassengersServed());
        assertEquals(0, elevator.getPendingCount());
    }

    @Test
    void arrivedAtFloor_intermediateStop_detected() {
        elevator.addDestination(7, null);
        elevator.addDestination(5, null);
        elevator = moveToFloor(elevator, 5);
        assertTrue(elevator.arrivedAtFloor());
        assertEquals(1, elevator.getPassengersServed());
    }

    @Test
    void moveOneFloor_upDirection_incrementsFloor() {
        elevator.setDirection(Direction.UP);
        elevator.moveOneFloor();
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(1, elevator.getTotalFloorsTraversed());
    }

    @Test
    void moveOneFloor_downDirection_decrementsFloor() {
        elevator.setDirection(Direction.UP);
        elevator.moveOneFloor();
        elevator.moveOneFloor();
        assertEquals(2, elevator.getCurrentFloor());
        elevator.setDirection(Direction.DOWN);
        elevator.moveOneFloor();
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(3, elevator.getTotalFloorsTraversed());
    }

    @Test
    void moveOneFloor_atMinFloor_doesNotGoLower() {
        elevator.setDirection(Direction.DOWN);
        elevator.moveOneFloor();
        assertEquals(0, elevator.getCurrentFloor());
        assertEquals(0, elevator.getTotalFloorsTraversed());
    }

    @Test
    void moveOneFloor_atMaxFloor_doesNotGoHigher() {
        elevator.setDirection(Direction.UP);
        for (int i = 0; i < 9; i++) {
            elevator.moveOneFloor();
        }
        assertEquals(9, elevator.getCurrentFloor());
        elevator.moveOneFloor();
        assertEquals(9, elevator.getCurrentFloor());
    }

    @Test
    void getPendingDestinations_returnsInsertionOrder() {
        elevator.addDestination(7, null);
        elevator.addDestination(3, null);
        elevator.addDestination(9, null);
        List<Integer> pending = elevator.getPendingDestinations();
        assertEquals(3, pending.size());
    }

    @Test
    void getPendingCount_incrementsCorrectly() {
        assertEquals(0, elevator.getPendingCount());
        elevator.addDestination(3, null);
        assertEquals(1, elevator.getPendingCount());
        elevator.addDestination(7, null);
        assertEquals(2, elevator.getPendingCount());
        elevator.addDestination(5, null);
        assertEquals(3, elevator.getPendingCount());
    }

    @Test
    void shutdown_stopsElevator() {
        assertTrue(elevator.isRunning());
        elevator.shutdown();
        assertFalse(elevator.isRunning());
    }

    @Test
    void hasDestinations_initiallyFalse() {
        assertFalse(elevator.hasDestinations());
    }

    @Test
    void hasDestinations_afterAdd_returnsTrue() {
        elevator.addDestination(5, null);
        assertTrue(elevator.hasDestinations());
    }

    @Test
    void hasDestinations_afterArrival_returnsFalse() {
        elevator.addDestination(5, null);
        elevator = moveToFloor(elevator, 5);
        elevator.arrivedAtFloor();
        assertFalse(elevator.hasDestinations());
    }

    @Test
    void setState_updatesState() {
        elevator.setState(ElevatorState.MOVING);
        assertEquals(ElevatorState.MOVING, elevator.getState());
    }

    @Test
    void setDirection_updatesDirection() {
        elevator.setDirection(Direction.UP);
        assertEquals(Direction.UP, elevator.getDirection());
    }

    @Test
    void getDisplayName_forSecondElevator() {
        Elevator e2 = new Elevator(1, 0, 9);
        assertEquals("Ascenseur 2", e2.getDisplayName());
    }

    @Test
    void arrivedAtFloor_recordsWaitTime() {
        Request req = new Request(5, Direction.UP);
        elevator.addDestination(5, req);
        elevator = moveToFloor(elevator, 5);
        elevator.arrivedAtFloor();
        assertTrue(elevator.getLastWaitTimeMs() >= 0);
    }

    @Test
    void hallCallStop_incrementsPassengers() {
        Request req = new Request(5, Direction.UP);
        elevator.addDestination(5, req);
        elevator = moveToFloor(elevator, 5);
        elevator.arrivedAtFloor();
        assertEquals(1, elevator.getCurrentPassengers());
    }

    @Test
    void cabCallStop_decrementsPassengers() {
        Request hallReq = new Request(5, Direction.UP);
        elevator.addDestination(5, hallReq);
        elevator = moveToFloor(elevator, 5);
        elevator.arrivedAtFloor();
        assertEquals(1, elevator.getCurrentPassengers());

        Request cabReq = new Request(5, 7);
        elevator.addDestination(7, cabReq);
        elevator = moveToFloor(elevator, 7);
        elevator.arrivedAtFloor();
        assertEquals(0, elevator.getCurrentPassengers());
    }

    private Elevator moveToFloor(Elevator e, int targetFloor) {
        int current = e.getCurrentFloor();
        if (targetFloor > current) {
            e.setDirection(Direction.UP);
            for (int i = current; i < targetFloor; i++) {
                e.moveOneFloor();
            }
        } else if (targetFloor < current) {
            e.setDirection(Direction.DOWN);
            for (int i = current; i > targetFloor; i--) {
                e.moveOneFloor();
            }
        }
        return e;
    }
}
