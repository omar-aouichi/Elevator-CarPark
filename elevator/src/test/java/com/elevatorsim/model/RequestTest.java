package com.elevatorsim.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void hallCall_constructor_setsCorrectFields() {
        Request req = new Request(3, Direction.UP);
        assertEquals(3, req.getFromFloor());
        assertEquals(-1, req.getToFloor());
        assertEquals(Direction.UP, req.getDirection());
        assertEquals(Request.Type.HALL_CALL, req.getType());
        assertEquals(-1, req.getAssignedElevatorId());
        assertFalse(req.isServed());
        assertNotNull(req.getTimestamp());
    }

    @Test
    void cabCall_constructor_setsCorrectFields() {
        Request req = new Request(3, 7);
        assertEquals(3, req.getFromFloor());
        assertEquals(7, req.getToFloor());
        assertEquals(Direction.UP, req.getDirection());
        assertEquals(Request.Type.CAB_CALL, req.getType());
        assertEquals(-1, req.getAssignedElevatorId());
        assertFalse(req.isServed());
    }

    @Test
    void cabCall_downDirection_detectedCorrectly() {
        Request req = new Request(7, 3);
        assertEquals(Direction.DOWN, req.getDirection());
    }

    @Test
    void cabCall_sameFloor_directionIsDown() {
        Request req = new Request(5, 5);
        assertEquals(Direction.DOWN, req.getDirection());
    }

    @Test
    void setAssignedElevatorId_updatesId() {
        Request req = new Request(3, Direction.UP);
        req.setAssignedElevatorId(1);
        assertEquals(1, req.getAssignedElevatorId());
    }

    @Test
    void setServed_marksAsServed() {
        Request req = new Request(3, Direction.UP);
        assertFalse(req.isServed());
        req.setServed(true);
        assertTrue(req.isServed());
    }

    @Test
    void getWaitTimeMs_returnsNonNegative() {
        Request req = new Request(3, Direction.UP);
        assertTrue(req.getWaitTimeMs() >= 0);
    }

    @Test
    void toString_hallCall_containsExpectedInfo() {
        Request req = new Request(3, Direction.UP);
        String str = req.toString();
        assertTrue(str.contains("Appel"));
        assertTrue(str.contains("3"));
        assertTrue(str.contains("▲"));
    }

    @Test
    void toString_cabCall_containsExpectedInfo() {
        Request req = new Request(3, 7);
        String str = req.toString();
        assertTrue(str.contains("Cabine"));
        assertTrue(str.contains("3"));
        assertTrue(str.contains("7"));
    }

    @Test
    void toString_withAssignedElevator_showsElevator() {
        Request req = new Request(3, Direction.UP);
        req.setAssignedElevatorId(0);
        String str = req.toString();
        assertTrue(str.contains("Ascenseur 1"));
    }
}
