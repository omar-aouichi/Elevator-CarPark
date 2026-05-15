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

---

## Parking System — Simulation de Parking

A JavaFX-based car-park simulation with multi-threaded vehicle handling, semaphore-controlled spot allocation, live UI, and CSV transaction export.

### Overview

Vehicles (`Voiture`) arrive on independent threads and contend for a fixed number of parking spots guarded by a `Semaphore`. Each car waits in queue if the lot is full, parks for a randomized duration, exits, and emits a billed `Transaction` (rate in MAD/hour). A JavaFX UI animates cars driving in/out of numbered spots, tracks active sessions in real time, and persists all completed transactions to CSV on shutdown.

### Architecture

```
parkingsystem/
├── Parking.java               Core lot — Semaphore-guarded entry/exit + fee calculation
├── Voiture.java               Runnable car — waits, parks, exits, notifies listener
├── Transaction.java           Record — matricule, entry/exit times, duration, amount
├── TransactionStore.java      Thread-safe (CopyOnWriteArrayList) store + CSV dump
├── TestParking.java           Headless CLI test driver (6 cars, 3 spots)
├── controller/
│   ├── ParkingController.java     Orchestrates Parking, ExecutorService, auto-spawn
│   └── ParkingEventListener.java  Event callbacks (entry / exit / wait / error / CSV)
└── view/
    ├── MainParkingApp.java        JavaFX entry point
    ├── ParkingView.java           UI: animated lot, sessions, log, stats
    └── Parking System.html        Static HTML mockup of the UI
```

**Layering:**

- **Model** (`Parking`, `Voiture`, `Transaction`, `TransactionStore`) — domain + concurrency primitives, no UI.
- **Controller** (`ParkingController`) — lifecycle, thread pools (`ExecutorService` for cars, `ScheduledExecutorService` for auto-spawn), event dispatch.
- **View** (`ParkingView`, `MainParkingApp`) — JavaFX scene; implements `ParkingEventListener` to receive controller events on the FX thread via `Platform.runLater`.

### Features

- **Concurrency**: One thread per `Voiture` via `Executors.newCachedThreadPool()`; spot contention enforced by a fair `Semaphore(capacity, true)`.
- **Auto-simulation**: `ScheduledExecutorService` spawns random plates at 900–2700 ms intervals after init.
- **Billing**: Per-second pro-rata charge at configured MAD/hour rate, rounded to 2 decimals.
- **UI**: Animated car nodes (chassis, wheels, headlights), live spot grid, active-sessions panel with running elapsed time and fee, scrolling activity log, stats header (occupied / queue / revenue / served).
- **Persistence**: `TransactionStore.dumpCsv()` writes `transactions_<date>.csv` (`;`-separated) on stop.

### Prerequisites

- Java 17+ (uses `record`)
- JavaFX SDK 17+ (for the GUI mode)

### How to Run

The parking sources are plain `.java` files (no Maven module). Run from the repo root.

**1. GUI mode (JavaFX):**

```bash
cd parkingsystem
javac --module-path "<path-to-javafx-sdk>/lib" --add-modules javafx.controls \
      -d out $(find . -name "*.java")
java  --module-path "<path-to-javafx-sdk>/lib" --add-modules javafx.controls \
      -cp out com.parkingsystem.view.MainParkingApp
```

PowerShell (Windows):

```powershell
cd parkingsystem
$JFX = "C:\path\to\javafx-sdk-17\lib"
javac --module-path $JFX --add-modules javafx.controls -d out (Get-ChildItem -Recurse -Filter *.java).FullName
java  --module-path $JFX --add-modules javafx.controls -cp out com.parkingsystem.view.MainParkingApp
```

**2. Headless CLI test (no JavaFX needed):**

```bash
cd parkingsystem
javac -d out Parking.java Voiture.java Transaction.java TransactionStore.java TestParking.java controller/ParkingEventListener.java
java  -cp out com.parkingsystem.TestParking
```

Output: 6 cars compete for 3 spots, then a `transactions_<date>.csv` is written to the working directory.

### Usage (GUI)

1. Enter **Name**, **Capacity**, **Rate/hour** → click **Initialize lot & start simulation**.
2. Auto-spawn begins immediately. Optionally type a plate + stay-duration (ms) and click **Add manual car**.
3. Watch cars animate to their spots; sessions panel updates live elapsed time and fee.
4. Click **Stop simulation** → CSV dumped, path logged in the activity panel.

### CSV Output Format

```
Matricule;HeureEntree;HeureSortie;DureeMinutes;Montant(MAD)
AB-123-CD;2026-05-15 14:22:01;2026-05-15 14:22:05;1;0.01
```

File name: `transactions_YYYY-MM-DD.csv` in the working directory.
