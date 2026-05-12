package com.elevatorsim.model;

/**
 * Enum représentant la direction de déplacement d'un ascenseur.
 */
public enum Direction {
    UP("▲ Montée"),
    DOWN("▼ Descente"),
    IDLE("● Inactif");

    private final String label;

    Direction(String label) {
        this.label = label;
    }

    /**
     * Retourne le label d'affichage de la direction.
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
