package com.parkingsystem;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TransactionStore {
    private static final String SEPARATEUR = ";";
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

    public void ajouter(Transaction transaction) {
        transactions.add(transaction);
    }

    public List<Transaction> getAll() {
        return List.copyOf(transactions);
    }

    public Path dumpCsv() throws IOException {
        Path fichier = Paths.get("transactions_" + LocalDate.now() + ".csv");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(fichier))) {
            writer.println("Matricule;HeureEntree;HeureSortie;DureeMinutes;Montant(MAD)");
            for (Transaction t : transactions) {
                writer.println(
                        t.matricule() + SEPARATEUR +
                                t.heureEntree().format(HORODATAGE) + SEPARATEUR +
                                t.heureSortie().format(HORODATAGE) + SEPARATEUR +
                                t.dureeMinutes() + SEPARATEUR +
                                String.format("%.2f", t.montant())
                );
            }
        }
        return fichier;
    }
}
