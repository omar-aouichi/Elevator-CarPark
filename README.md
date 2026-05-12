# Elevator Simulation

A JavaFX-based simulation of 2 intelligent elevators in the "Tour Horizon" building (floors 0–9). Users request elevators from any floor, board them, and select destinations — all visualized in a real-time dark-themed UI with per-elevator threading and a scheduling algorithm that balances proximity and occupancy.

## Features

- **Real-time JavaFX UI** — Animated elevator shafts, live status panels, and an event log
- **Intelligent scheduling** — `ClosestElevatorScheduler` uses a weighted score (`distance + 2 × occupancy`) to dispatch the best elevator
- **Per-elevator threads** — Each elevator runs independently with blocking waits (`ReentrantLock` + `Condition`) — no busy-waiting
- **Passenger simulation** — Hall calls increment capacity, cab calls decrement it; full elevators (max 6) are excluded from scheduling
- **Destination dialog** — Modal floor-picker appears when a passenger boards, blocking the elevator thread until the user responds
- **Comprehensive logging** — Console, file (`logs/elevator_simulation.log`), and in-memory circular buffer (5000 entries)
- **32 unit tests** — Covering `Elevator` model (23 tests) and `Request` model (9 tests)

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  MainApp (JavaFX Application)                                   │
│  ┌──────────────┐  ┌──────────────────────┐  ┌───────────────┐ │
│  │Building      │  │ElevatorController    │  │SimulationPane │ │
│  │  - name      │  │  - handleHallCall()  │  │  - shafts     │ │
│  │  - floors    │  │  - handleCabCall()   │  │  - buttons    │ │
│  │  - elevators │  │  - start() / stop()  │  │  - info cards │ │
│  └──────┬───────┘  └──────┬───────────────┘  └───────┬───────┘ │
│         │                 │                           │         │
│         ▼                 ▼                           ▼         │
│  ┌──────────────┐  ┌──────────────────────┐  ┌───────────────┐ │
│  │Elevator x2   │  │ElevatorScheduler     │  │LogService     │ │
│  │  - floor     │  │  (strategy pattern)  │  │  (singleton)  │ │
│  │  - state     │  │ClosestElevatorAlgo   │  │  - file       │ │
│  │  - passengers│  │  score=dist+occup*2  │  │  - console    │ │
│  │  - queue     │  └──────────────────────┘  │  - buffer     │ │
│  └──────┬───────┘                             └───────────────┘ │
│         ▼                                                       │
│  ┌──────────────────────┐                                       │
│  │ElevatorThread (x2)   │                                       │
│  │  - move floor by     │                                       │
│  │    floor (1s each)   │                                       │
│  │  - stop, open doors  │                                       │
│  │    (2s), continue    │                                       │
│  └──────────────────────┘                                       │
└──────────────────────────────────────────────────────────────────┘
```

A PlantUML class diagram is available at `docs/diagram.puml`.

### Key design patterns

| Pattern | Where | Purpose |
|---|---|---|
| **Strategy** | `ElevatorScheduler` interface + `ClosestElevatorScheduler` | Pluggable scheduling algorithms |
| **MVC** | Model (`Building`, `Elevator`, `Request`), View (`ElevatorSimulationPane`), Controller (`ElevatorController`) | Separation of concerns |
| **Producer-Consumer** | `ElevatorController` → `ElevatorThread` via `Condition` | Thread-safe destination processing |
| **Singleton** | `LogService.getInstance()` | Single shared logger |
| **Observer/Callback** | `onUIUpdate`, `onStateChanged`, `onHallCallArrival` | Loose coupling between threads and UI |

## Tech Stack

- **Java 17** — Language
- **JavaFX 21.0.2** — UI framework
- **Maven** — Build system
- **JUnit Jupiter 5.10.2** — Testing

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 17 or later
- Apache Maven 3.6+

### Run the simulation

```bash
mvn clean compile javafx:run
```

### Run tests

```bash
mvn test
```

## Usage

1. **Call an elevator** — Click the **"Appel"** button next to any floor label
2. **Board the elevator** — The closest available elevator will arrive
3. **Select a destination** — A dialog appears asking where you want to go; choose a floor
4. **Watch the simulation** — Elevators move floor-by-floor, doors open for 2 seconds, and the info panels update live
5. **Monitor the log** — The bottom panel shows real-time event logs

## Project Structure

```
src/
├── main/java/com/elevatorsim/
│   ├── MainApp.java                          # JavaFX entry point
│   ├── config/SimulationConfig.java          # Configuration (floors, timing, etc.)
│   ├── model/
│   │   ├── Building.java                     # Building with floor range and elevators
│   │   ├── BuildingComponent.java            # Abstract base for building elements
│   │   ├── Direction.java                    # UP / DOWN / IDLE enum
│   │   ├── Elevator.java                     # Elevator model with destination queue
│   │   ├── ElevatorState.java                # MOVING / STOPPED / IDLE enum
│   │   └── Request.java                      # Hall call or cab call request
│   ├── service/
│   │   ├── ElevatorController.java           # Central orchestrator
│   │   ├── ElevatorScheduler.java            # Scheduler interface (strategy)
│   │   ├── ClosestElevatorScheduler.java     # Scoring algorithm
│   │   └── ElevatorThread.java               # Per-elevator simulation loop
│   ├── ui/ElevatorSimulationPane.java        # JavaFX UI
│   └── utils/LogService.java                 # Thread-safe logging singleton
└── test/java/com/elevatorsim/model/
    ├── ElevatorTest.java                     # 23 tests
    └── RequestTest.java                      # 9 tests
docs/
└── diagram.puml                              # PlantUML class diagram
logs/
└── elevator_simulation.log                   # Runtime log output
```

## Build & Run Commands

| Command | Description |
|---|---|
| `mvn clean compile` | Compile the project |
| `mvn javafx:run` | Run the JavaFX application |
| `mvn test` | Run all unit tests |
| `mvn clean package` | Build a JAR (requires additional shade/assembly plugin config) |
