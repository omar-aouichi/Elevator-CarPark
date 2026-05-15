package com.parkingsystem;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestParking {
    public static void main(String[] args) throws Exception {
        Parking parking = new Parking("Parking Central", 3, 10.0);

        System.out.println("=== " + parking.getNom() + " ===");
        System.out.println("Capacite: " + parking.getCapacite() + " places | Tarif: 10.00 MAD/heure");
        System.out.println("6 voitures vont arriver simultanement (3 attendront une place libre)\n");

        List<Voiture> voitures = Arrays.asList(
                new Voiture("AB-123-CD", parking, 4000),
                new Voiture("EF-456-GH", parking, 6000),
                new Voiture("IJ-789-KL", parking, 3000),
                new Voiture("MN-012-OP", parking, 7000),
                new Voiture("QR-345-ST", parking, 2000),
                new Voiture("UV-678-WX", parking, 5000)
        );

        ExecutorService executor = Executors.newFixedThreadPool(voitures.size());
        for (Voiture v : voitures) {
            executor.submit(v);
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        var csv = parking.getStore().dumpCsv();
        System.out.println("\n=== Simulation terminee. CSV: " + csv.toAbsolutePath() + " ===");
    }
}
