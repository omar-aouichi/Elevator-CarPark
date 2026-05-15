package com.parkingsystem;

import java.time.LocalDateTime;

public record Transaction(String matricule, LocalDateTime heureEntree, LocalDateTime heureSortie, long dureeMinutes,
                          double montant) {

    @Override
    public String toString() {
        return String.format("Transaction[%s | entree=%s | sortie=%s | duree=%d min | montant=%.2f MAD]",
                matricule, heureEntree, heureSortie, dureeMinutes, montant);
    }
}
