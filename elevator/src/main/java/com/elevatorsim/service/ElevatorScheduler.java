package com.elevatorsim.service;

import com.elevatorsim.model.Elevator;
import java.util.List;

public interface ElevatorScheduler {
    Elevator selectBestElevator(List<Elevator> elevators, int targetFloor);
}
