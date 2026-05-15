package com.parkingsystem.controller;

import com.parkingsystem.Transaction;

public interface ParkingEventListener {
    void onAttente(String matricule, int placesDisponibles);
    void onEntree(String matricule, int placesDisponibles);
    void onSortie(String matricule, Transaction transaction, int placesDisponibles);
    void onErreur(String matricule, String message);
    void onSimulationTerminee();
    default void onCsvSauvegarde(String cheminCsv) {}
}
