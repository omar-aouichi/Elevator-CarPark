package com.elevatorsim.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Représente une demande d'ascenseur faite par un utilisateur.
 * Peut être un appel depuis un étage (hall call) ou une sélection d'étage
 * depuis l'intérieur de la cabine (cab call).
 */
public class Request {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Type de requête */
    public enum Type {
        /** Appel depuis le palier d'un étage */
        HALL_CALL,
        /** Sélection d'étage depuis l'intérieur de la cabine */
        CAB_CALL
    }

    private final int fromFloor;
    private final int toFloor;
    private final Direction direction;
    private final Type type;
    private final LocalDateTime timestamp;
    private int assignedElevatorId;
    private boolean served;

    /**
     * Constructeur pour un appel depuis un palier (hall call).
     *
     * @param fromFloor l'étage d'appel
     * @param direction la direction souhaitée
     */
    public Request(int fromFloor, Direction direction) {
        this.fromFloor = fromFloor;
        this.toFloor = -1; // Pas encore connu
        this.direction = direction;
        this.type = Type.HALL_CALL;
        this.timestamp = LocalDateTime.now();
        this.assignedElevatorId = -1;
        this.served = false;
    }

    /**
     * Constructeur pour une sélection d'étage depuis la cabine (cab call).
     *
     * @param fromFloor l'étage actuel
     * @param toFloor   l'étage de destination
     */
    public Request(int fromFloor, int toFloor) {
        this.fromFloor = fromFloor;
        this.toFloor = toFloor;
        this.direction = toFloor > fromFloor ? Direction.UP : Direction.DOWN;
        this.type = Type.CAB_CALL;
        this.timestamp = LocalDateTime.now();
        this.assignedElevatorId = -1;
        this.served = false;
    }

    // --- Getters ---

    public int getFromFloor() {
        return fromFloor;
    }

    public int getToFloor() {
        return toFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public Type getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getAssignedElevatorId() {
        return assignedElevatorId;
    }

    public boolean isServed() {
        return served;
    }

    // --- Setters ---

    public void setAssignedElevatorId(int assignedElevatorId) {
        this.assignedElevatorId = assignedElevatorId;
    }

    public void setServed(boolean served) {
        this.served = served;
    }

    /**
     * Calcule le temps d'attente en millisecondes depuis la création de la requête.
     */
    public long getWaitTimeMs() {
        return java.time.Duration.between(timestamp, LocalDateTime.now()).toMillis();
    }

    @Override
    public String toString() {
        String base = String.format("[%s] %s étage %d %s",
                timestamp.format(FORMATTER),
                type == Type.HALL_CALL ? "Appel" : "Cabine",
                fromFloor,
                direction.getLabel());
        if (toFloor >= 0) {
            base += String.format(" → étage %d", toFloor);
        }
        if (assignedElevatorId >= 0) {
            base += String.format(" (Ascenseur %d)", assignedElevatorId + 1);
        }
        return base;
    }
}
