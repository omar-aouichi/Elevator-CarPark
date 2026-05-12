package com.elevatorsim;

import com.elevatorsim.config.SimulationConfig;
import com.elevatorsim.model.Building;
import com.elevatorsim.service.ElevatorController;
import com.elevatorsim.ui.ElevatorSimulationPane;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application "Simulation d'Ascenseurs".
 *
 * <p>Configuration de l'immeuble :</p>
 * <ul>
 *   <li>Nom : Tour Horizon</li>
 *   <li>Étages : 0 (RDC) à 9</li>
 *   <li>Ascenseurs : 2</li>
 * </ul>
 *
 * <p>L'application utilise JavaFX pour l'interface graphique et
 * un ExecutorService pour gérer les threads des ascenseurs.</p>
 */
public class MainApp extends Application {

    private ElevatorController controller;
    private SimulationConfig config;

    @Override
    public void start(Stage primaryStage) {
        // Création de la configuration
        config = SimulationConfig.createDefault();

        // Création du modèle
        Building building = new Building(
            config.buildingName(), config.minFloor(), config.maxFloor(), config.elevatorCount()
        );

        // Création du contrôleur
        controller = new ElevatorController(building, config);

        // Création de l'interface graphique
        ElevatorSimulationPane simulationPane = new ElevatorSimulationPane(controller);

        // Connecter le contrôleur à l'UI
        controller.setOnUIUpdate(simulationPane::updateUI);

        // Démarrer la simulation
        controller.start();

        // Configurer la popup de destination pour les appels
        simulationPane.setupHallCallDialogs();

        // Configurer la scène
        Scene scene = new Scene(simulationPane, 820, 900);
        scene.setFill(javafx.scene.paint.Color.web("#0F0F1A"));

        // Configurer la fenêtre
        primaryStage.setTitle("Simulation d'Ascenseurs — " + config.buildingName());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(750);
        primaryStage.setResizable(true);

        // Fermeture propre
        primaryStage.setOnCloseRequest(event -> {
            controller.stop();
        });

        primaryStage.show();
    }

    @Override
    public void stop() {
        if (controller != null && controller.isStarted()) {
            controller.stop();
        }
    }

    /**
     * Point d'entrée principal.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
