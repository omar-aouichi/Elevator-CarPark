package com.parkingsystem;

import com.parkingsystem.controller.ParkingEventListener;

import java.time.LocalDateTime;

public class Voiture implements Runnable {
    private final String matricule;
    private final Parking parking;
    private final int dureeStationnementMs;
    private final ParkingEventListener listener;
    private LocalDateTime heureEntree;
    private LocalDateTime heureSortie;

    public Voiture(String matricule, Parking parking, int dureeStationnementMs) {
        this(matricule, parking, dureeStationnementMs, null);
    }

    public Voiture(String matricule, Parking parking, int dureeStationnementMs, ParkingEventListener listener) {
        this.matricule = matricule;
        this.parking = parking;
        this.dureeStationnementMs = dureeStationnementMs;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            if (listener != null) {
                listener.onAttente(matricule, parking.getPlacesDisponibles());
            }
            parking.entrer(this);
            if (listener != null) {
                listener.onEntree(matricule, parking.getPlacesDisponibles());
            }
            Thread.sleep(dureeStationnementMs);
            Transaction transaction = parking.sortir(this);
            if (listener != null) {
                listener.onSortie(matricule, transaction, parking.getPlacesDisponibles());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (listener != null) {
                listener.onErreur(matricule, "Interrompu");
            } else {
                System.err.println(matricule + " interrompu.");
            }
        }
    }

    public String getMatricule() { return matricule; }
    public LocalDateTime getHeureEntree() { return heureEntree; }
    public LocalDateTime getHeureSortie() { return heureSortie; }
    public void setHeureEntree(LocalDateTime heureEntree) { this.heureEntree = heureEntree; }
    public void setHeureSortie(LocalDateTime heureSortie) { this.heureSortie = heureSortie; }
}
