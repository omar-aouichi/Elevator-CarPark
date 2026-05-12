package com.elevatorsim.utils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service de journalisation.
 * Écrit les logs dans un fichier texte et fournit un accès en mémoire
 * pour l'affichage dans l'interface graphique.
 *
 * Thread-safe grâce à ReentrantLock.
 */
public class LogService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "elevator_simulation.log";
    private static final int MAX_MEMORY_ENTRIES = 5000;

    private final Deque<String> inMemoryLog;
    private final ReentrantLock lock;
    private BufferedWriter fileWriter;
    private final Path logFilePath;

    /** Callback pour notifier l'UI d'un nouveau log */
    private volatile Runnable onLogUpdated;

    private static LogService instance;

    /**
     * Retourne l'instance singleton du service de logs.
     */
    public static synchronized LogService getInstance() {
        if (instance == null) {
            instance = new LogService();
        }
        return instance;
    }

    private LogService() {
        this.inMemoryLog = new ArrayDeque<>(MAX_MEMORY_ENTRIES + 1);
        this.lock = new ReentrantLock();

        // Créer le répertoire de logs
        Path logDir = Paths.get(LOG_DIR);
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            System.err.println("Impossible de créer le répertoire de logs: " + e.getMessage());
        }

        this.logFilePath = logDir.resolve(LOG_FILE);

        // Ouvrir le fichier en mode append
        try {
            fileWriter = new BufferedWriter(new FileWriter(logFilePath.toFile(), true));
            log("SYSTÈME", "=== Simulation d'Ascenseurs démarrée ===");
            log("SYSTÈME", "Fichier de logs: " + logFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir le fichier de logs: " + e.getMessage());
        }
    }

    /**
     * Enregistre un message de log.
     *
     * @param source  la source du log (ex: "Ascenseur 1", "Contrôleur")
     * @param message le message à enregistrer
     */
    public void log(String source, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] [%-15s] %s%n", timestamp, source, message);

        lock.lock();
        try {
            // Écriture en mémoire (tampon circulaire)
            if (inMemoryLog.size() >= MAX_MEMORY_ENTRIES) {
                inMemoryLog.pollFirst();
            }
            inMemoryLog.addLast(logEntry);

            // Écriture dans le fichier
            if (fileWriter != null) {
                try {
                    fileWriter.write(logEntry);
                    fileWriter.flush();
                } catch (IOException e) {
                    System.err.println("Erreur d'écriture dans le fichier de logs: " + e.getMessage());
                }
            }

            // Impression dans la console
            System.out.print(logEntry);
        } finally {
            lock.unlock();
        }

        // Notifier l'UI
        if (onLogUpdated != null) {
            onLogUpdated.run();
        }
    }

    /**
     * Log un déplacement d'ascenseur.
     */
    public void logMovement(int elevatorId, int fromFloor, int toFloor, String direction) {
        log(String.format("Ascenseur %d", elevatorId + 1),
                String.format("Déplacement: Étage %d → Étage %d (%s)", fromFloor, toFloor, direction));
    }

    /**
     * Log un arrêt d'ascenseur.
     */
    public void logStop(int elevatorId, int floor) {
        log(String.format("Ascenseur %d", elevatorId + 1),
                String.format("★ Arrêt à l'étage %d — Portes ouvertes", floor));
    }

    /**
     * Log un appel utilisateur.
     */
    public void logUserCall(int floor, String direction, int assignedElevatorId) {
        log("APPEL",
                String.format("Étage %d, Direction: %s → Assigné à Ascenseur %d",
                        floor, direction, assignedElevatorId + 1));
    }

    /**
     * Log une sélection d'étage depuis la cabine.
     */
    public void logCabCall(int elevatorId, int targetFloor) {
        log(String.format("Ascenseur %d", elevatorId + 1),
                String.format("Passager sélectionne l'étage %d", targetFloor));
    }

    /**
     * Log le temps d'attente.
     */
    public void logWaitTime(int elevatorId, int floor, long waitTimeMs) {
        log(String.format("Ascenseur %d", elevatorId + 1),
                String.format("Temps d'attente à l'étage %d: %d ms", floor, waitTimeMs));
    }

    /**
     * Retourne tout le contenu du log en mémoire.
     */
    public String getFullLog() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (String entry : inMemoryLog) {
                sb.append(entry);
            }
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retourne les N dernières lignes du log.
     */
    public String getRecentLog(int maxLines) {
        lock.lock();
        try {
            if (inMemoryLog.isEmpty()) return "";
            int size = inMemoryLog.size();
            int start = Math.max(0, size - maxLines);
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (String entry : inMemoryLog) {
                if (i >= start) {
                    sb.append(entry);
                }
                i++;
            }
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Définit le callback de notification pour l'UI.
     */
    public void setOnLogUpdated(Runnable callback) {
        this.onLogUpdated = callback;
    }

    /**
     * Ferme proprement le fichier de logs.
     */
    public void close() {
        log("SYSTÈME", "=== Simulation terminée ===");
        lock.lock();
        try {
            if (fileWriter != null) {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    System.err.println("Erreur de fermeture du fichier de logs: " + e.getMessage());
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
