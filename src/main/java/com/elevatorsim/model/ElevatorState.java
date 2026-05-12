package com.elevatorsim.model;

/**
 * Enum représentant l'état opérationnel d'un ascenseur.
 */
public enum ElevatorState {
    MOVING("En mouvement"),
    STOPPED("Arrêté (portes ouvertes)"),
    IDLE("Inactif");

    private final String label;

    ElevatorState(String label) {
        this.label = label;
    }

    /**
     * Retourne le label d'affichage de l'état.
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
