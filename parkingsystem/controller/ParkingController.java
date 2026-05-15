package com.parkingsystem.controller;

import com.parkingsystem.Parking;
import com.parkingsystem.Voiture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ParkingController {
    private Parking parking;
    private ExecutorService executor;
    private ScheduledExecutorService autoExecutor;
    private ParkingEventListener listener;
    private final AtomicInteger compteurVoitures = new AtomicInteger(0);
    private final Random random = new Random();

    public void setListener(ParkingEventListener listener) {
        this.listener = listener;
    }

    public void initialiserParking(String nom, int capacite, double tarifParHeure) {
        arreterSansDump();
        this.parking = new Parking(nom, capacite, tarifParHeure);
        this.executor = Executors.newCachedThreadPool();
        demarrerSimulationAuto();
    }

    public boolean estInitialise() {
        return parking != null && executor != null && !executor.isShutdown();
    }

    public void ajouterVoiture(String matricule, int dureeStationnementMs) {
        if (!estInitialise()) {
            if (listener != null) listener.onErreur(matricule, "Parking non initialise");
            return;
        }
        Voiture v = new Voiture(matricule, parking, dureeStationnementMs, listener);
        executor.submit(v);
        compteurVoitures.incrementAndGet();
    }

    private void demarrerSimulationAuto() {
        autoExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "parking-auto-spawn");
            t.setDaemon(true);
            return t;
        });
        scheduleNextSpawn(400);
    }

    private void scheduleNextSpawn(long delayMs) {
        ScheduledExecutorService exec = autoExecutor;
        if (exec == null || exec.isShutdown()) return;
        exec.schedule(() -> {
            if (!estInitialise()) return;
            String plate = genererMatricule();
            int duree = 4000 + random.nextInt(8000);
            ajouterVoiture(plate, duree);
            long nextDelay = 900 + random.nextInt(1800);
            scheduleNextSpawn(nextDelay);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private String genererMatricule() {
        char a = (char) ('A' + random.nextInt(26));
        char b = (char) ('A' + random.nextInt(26));
        int n = 100 + random.nextInt(900);
        char c = (char) ('A' + random.nextInt(26));
        char d = (char) ('A' + random.nextInt(26));
        return "" + a + b + "-" + n + "-" + c + d;
    }

    private void arreterAuto() {
        if (autoExecutor != null) {
            autoExecutor.shutdownNow();
            autoExecutor = null;
        }
    }

    public int getPlacesDisponibles() {
        return parking == null ? 0 : parking.getPlacesDisponibles();
    }

    public int getCapacite() {
        return parking == null ? 0 : parking.getCapacite();
    }

    public String getNomParking() {
        return parking == null ? "" : parking.getNom();
    }

    public int getNombreVoituresEnvoyees() {
        return compteurVoitures.get();
    }

    public Path dumpTransactionsCsv() throws IOException {
        if (parking == null) return null;
        return parking.getStore().dumpCsv();
    }

    public void arreter() {
        Path csv = null;
        try {
            csv = dumpTransactionsCsv();
        } catch (IOException e) {
            if (listener != null) listener.onErreur("CSV", "Echec ecriture: " + e.getMessage());
        }
        arreterSansDump();
        if (listener != null) listener.onSimulationTerminee();
        if (csv != null && listener != null) {
            listener.onCsvSauvegarde(csv.toAbsolutePath().toString());
        }
    }

    private void arreterSansDump() {
        arreterAuto();
        if (executor != null) {
            executor.shutdownNow();
        }
        executor = null;
        parking = null;
        compteurVoitures.set(0);
    }
}
