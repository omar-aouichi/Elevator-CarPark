package com.parkingsystem;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;

public class Parking {
    private final String nom;
    private final int capacite;
    private final double tarifParHeure;
    private final Semaphore places;
    private final TransactionStore store;

    public Parking(String nom, int capacite, double tarifParHeure, TransactionStore store) {
        this.nom = nom;
        this.capacite = capacite;
        this.tarifParHeure = tarifParHeure;
        this.places = new Semaphore(capacite, true);
        this.store = store;
    }

    public Parking(String nom, int capacite, double tarifParHeure) {
        this(nom, capacite, tarifParHeure, new TransactionStore());
    }

    public void entrer(Voiture voiture) throws InterruptedException {
        places.acquire();
        voiture.setHeureEntree(LocalDateTime.now());
    }

    public Transaction sortir(Voiture voiture) {
        voiture.setHeureSortie(LocalDateTime.now());
        Transaction transaction = calculer(voiture);
        store.ajouter(transaction);
        places.release();
        return transaction;
    }

    public Transaction calculer(Voiture voiture) {
        Duration duree = Duration.between(voiture.getHeureEntree(), voiture.getHeureSortie());
        long secondes = Math.max(1, duree.getSeconds());
        long minutes = Math.max(1, duree.toMinutes() == 0 ? 1 : duree.toMinutes());
        double montant = Math.round((secondes / 3600.0) * tarifParHeure * 100.0) / 100.0;
        return new Transaction(voiture.getMatricule(),
                voiture.getHeureEntree(), voiture.getHeureSortie(), minutes, montant);
    }

    public int getCapacite() { return capacite; }
    public int getPlacesDisponibles() { return places.availablePermits(); }
    public String getNom() { return nom; }
    public TransactionStore getStore() { return store; }
}
