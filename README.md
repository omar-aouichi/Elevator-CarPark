# Elevator-CarPark

## Elevator Simulation — Simulation d'Ascenseurs

A JavaFX-based elevator simulation with a closest-elevator scheduling algorithm.

### Overview

Multi-threaded elevator simulation for a building (floors 0-9, 2 elevators) with a dark-themed JavaFX UI showing real-time elevator movement, hall call buttons, and event logging.

### Architecture

```
elevator/src/main/java/com/elevatorsim
├── config/       SimulationConfig (record)
├── model/        Building, Elevator, Request, Direction, ElevatorState, BuildingComponent
├── service/      ElevatorController, ElevatorScheduler, ClosestElevatorScheduler, ElevatorThread
├── ui/           ElevatorSimulationPane (JavaFX)
├── utils/        LogService (singleton, thread-safe)
└── MainApp       Entry point
```

### Features

- **Scheduling**: Closest-elevator algorithm with occupancy penalty
- **Concurrency**: Each elevator runs on its own thread via `ExecutorService`
- **UI**: Animated cabins, info panels, hall call buttons, destination dialog, live log console
- **Logging**: Thread-safe file + in-memory logging with real-time UI updates
- **Tests**: JUnit 5 tests for `Elevator` and `Request` models

### Prerequisites

- Java 17+
- Maven 3.8+

### Usage

```bash
cd elevator
mvn clean javafx:run
```

### Tests

```bash
cd elevator && mvn test
```

### Configuration

Edit `SimulationConfig.java` or the `SimulationConfig` record:
- `buildingName`, `minFloor`, `maxFloor`, `elevatorCount`
- `floorTravelTimeMs` (default: 1000ms), `doorOpenTimeMs` (default: 2000ms)

### Notes

- Labels are in French (RDC, 1er, Ascenseur, etc.)
- Logs are written to `elevator/logs/elevator_simulation.log`
