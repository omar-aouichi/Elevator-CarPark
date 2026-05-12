package com.elevatorsim.ui;

import com.elevatorsim.model.Building;
import com.elevatorsim.model.Direction;
import com.elevatorsim.model.Elevator;
import com.elevatorsim.model.ElevatorState;
import com.elevatorsim.service.ElevatorController;
import com.elevatorsim.utils.LogService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Panneau principal de l'interface graphique.
 *
 * Affiche :
 * - La représentation graphique de l'immeuble avec les gaines d'ascenseur
 * - Les boutons d'appel à chaque étage
 * - Les panneaux d'information pour chaque ascenseur
 * - Les panneaux de sélection d'étage (cab call)
 * - La console de logs
 */
public class ElevatorSimulationPane extends BorderPane {

    // --- Configuration visuelle ---
    private static final double FLOOR_HEIGHT = 70;
    private static final double SHAFT_WIDTH = 80;
    private static final double CABIN_WIDTH = 64;
    private static final double CABIN_HEIGHT = 52;
    private static final double SHAFT_GAP = 30;

    // --- Couleurs des ascenseurs ---
    private static final Color ELEVATOR_1_COLOR = Color.web("#6C63FF");
    private static final Color ELEVATOR_2_COLOR = Color.web("#FF6584");
    private static final Color ELEVATOR_1_STOPPED = Color.web("#A29BFE");
    private static final Color ELEVATOR_2_STOPPED = Color.web("#FD79A8");
    private static final Color SHAFT_COLOR = Color.web("#1E1E2E");
    private static final Color FLOOR_LINE_COLOR = Color.web("#3A3A5C");

    // --- Références aux composants ---
    private final ElevatorController controller;
    private final Building building;
    private final LogService logService;

    /** Rectangles représentant les cabines d'ascenseur */
    private final Rectangle[] cabins;

    /** Textes affichant l'étage courant dans la cabine */
    private final Text[] cabinTexts;

    /** Labels d'information pour chaque ascenseur */
    private final Map<String, Label> infoLabels;

    /** Zone de texte pour les logs */
    private TextArea logTextArea;

    /** Conteneur principal des gaines */
    private Pane shaftPane;

    /** Évite les mises à jour UI redondantes */
    private final AtomicBoolean uiUpdatePending = new AtomicBoolean(false);

    public ElevatorSimulationPane(ElevatorController controller) {
        this.controller = controller;
        this.building = controller.getBuilding();
        this.logService = LogService.getInstance();

        int elevatorCount = building.getElevators().size();
        this.cabins = new Rectangle[elevatorCount];
        this.cabinTexts = new Text[elevatorCount];
        this.infoLabels = new HashMap<>();

        // Style de fond sombre
        setStyle("-fx-background-color: #0F0F1A;");

        buildUI();
        setupLogCallback();
    }

    /**
     * Construit l'ensemble de l'interface.
     */
    private void buildUI() {
        // === HEADER ===
        VBox header = createHeader();
        setTop(header);

        // === CENTRE : Immeuble (gaines + boutons d'appel) ===
        HBox centerContent = new HBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(10, 20, 10, 20));

        // Boutons d'appel à gauche
        VBox callButtons = createCallButtonsPanel();

        // Gaines d'ascenseur au centre
        StackPane shaftContainer = createShaftView();

        // Panneaux d'information à droite
        VBox infoPanel = createInfoPanel();

        centerContent.getChildren().addAll(callButtons, shaftContainer, infoPanel);
        setCenter(centerContent);

        // === BAS : Logs + contrôle cabine ===
        VBox bottomPanel = createBottomPanel();
        setBottom(bottomPanel);
    }

    // =====================================================
    //  Création du header
    // =====================================================

    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(15, 20, 10, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #1a1a2e, #16213e, #1a1a2e);"
                + "-fx-border-color: #6C63FF; -fx-border-width: 0 0 2 0;");

        Label title = new Label("⬆ Simulation d'Ascenseurs ⬇");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#E0E0FF"));

        Label subtitle = new Label(building.toString());
        subtitle.setFont(Font.font("System", 13));
        subtitle.setTextFill(Color.web("#8888AA"));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    // =====================================================
    //  Boutons d'appel par étage
    // =====================================================

    private VBox createCallButtonsPanel() {
        VBox panel = new VBox(0);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(5));

        Label titleLabel = new Label("Appels");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#CCCCDD"));
        titleLabel.setPadding(new Insets(0, 0, 8, 0));
        panel.getChildren().add(titleLabel);

        // Étages affichés du plus haut au plus bas
        for (int floor = building.getMaxFloor(); floor >= building.getMinFloor(); floor--) {
            HBox floorRow = createFloorCallRow(floor);
            panel.getChildren().add(floorRow);
        }

        return panel;
    }

    private HBox createFloorCallRow(int floor) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER);
        row.setPrefHeight(FLOOR_HEIGHT);
        row.setPadding(new Insets(2, 5, 2, 5));

        Label floorLabel = new Label(Building.getFloorLabel(floor));
        floorLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        floorLabel.setTextFill(Color.web("#AAAACC"));
        floorLabel.setPrefWidth(40);
        floorLabel.setAlignment(Pos.CENTER);

        Button callBtn = new Button("Appel");
        callBtn.setPrefSize(60, 30);
        styleCallButton(callBtn, "#6C63FF", "#A29BFE");
        final int f = floor;
        callBtn.setOnAction(e -> {
            controller.handleHallCall(f);
            flashButton(callBtn, "#A29BFE");
        });

        row.getChildren().addAll(floorLabel, callBtn);
        return row;
    }

    // =====================================================
    //  Vue des gaines d'ascenseur
    // =====================================================

    private StackPane createShaftView() {
        int totalFloors = building.getTotalFloors();
        int elevatorCount = building.getElevators().size();

        double totalWidth = elevatorCount * SHAFT_WIDTH + (elevatorCount - 1) * SHAFT_GAP + 40;
        double totalHeight = totalFloors * FLOOR_HEIGHT + 20;

        shaftPane = new Pane();
        shaftPane.setPrefSize(totalWidth, totalHeight);
        shaftPane.setMinSize(totalWidth, totalHeight);

        // Dessiner les gaines
        for (int e = 0; e < elevatorCount; e++) {
            double x = 20 + e * (SHAFT_WIDTH + SHAFT_GAP);

            // Fond de la gaine
            Rectangle shaft = new Rectangle(x, 0, SHAFT_WIDTH, totalHeight);
            shaft.setFill(SHAFT_COLOR);
            shaft.setStroke(Color.web("#2A2A4A"));
            shaft.setStrokeWidth(1.5);
            shaft.setArcWidth(8);
            shaft.setArcHeight(8);
            shaftPane.getChildren().add(shaft);

            // Lignes de séparation des étages
            for (int f = 0; f <= totalFloors; f++) {
                double y = totalHeight - (f * FLOOR_HEIGHT) - 10;
                javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, y, x + SHAFT_WIDTH, y);
                line.setStroke(FLOOR_LINE_COLOR);
                line.setStrokeWidth(0.5);
                line.getStrokeDashArray().addAll(4.0, 4.0);
                shaftPane.getChildren().add(line);
            }

            // Cabine de l'ascenseur
            double cabinX = x + (SHAFT_WIDTH - CABIN_WIDTH) / 2;
            double cabinY = totalHeight - CABIN_HEIGHT - 10; // Commence au RDC

            Color cabinColor = (e == 0) ? ELEVATOR_1_COLOR : ELEVATOR_2_COLOR;

            Rectangle cabin = new Rectangle(cabinX, cabinY, CABIN_WIDTH, CABIN_HEIGHT);
            cabin.setFill(cabinColor);
            cabin.setStroke(cabinColor.brighter());
            cabin.setStrokeWidth(2);
            cabin.setArcWidth(10);
            cabin.setArcHeight(10);
            // Effet d'ombre
            cabin.setEffect(new javafx.scene.effect.DropShadow(10, 0, 3, Color.rgb(0, 0, 0, 0.5)));
            cabins[e] = cabin;

            // Texte dans la cabine
            Text cabinText = new Text(cabinX + CABIN_WIDTH / 2 - 6, cabinY + CABIN_HEIGHT / 2 + 5, "0");
            cabinText.setFont(Font.font("System", FontWeight.BOLD, 16));
            cabinText.setFill(Color.WHITE);
            cabinTexts[e] = cabinText;

            shaftPane.getChildren().addAll(cabin, cabinText);
        }

        // Labels d'étages sur le côté droit
        for (int f = building.getMinFloor(); f <= building.getMaxFloor(); f++) {
            double y = totalHeight - ((f - building.getMinFloor()) * FLOOR_HEIGHT) - 10 - FLOOR_HEIGHT / 2;
            Text floorText = new Text(totalWidth - 15, y + 5, String.valueOf(f));
            floorText.setFont(Font.font("System", 10));
            floorText.setFill(Color.web("#666688"));
            shaftPane.getChildren().add(floorText);
        }

        StackPane container = new StackPane(shaftPane);
        container.setStyle("-fx-background-color: #12121F; -fx-background-radius: 12;"
                + "-fx-border-color: #2A2A4A; -fx-border-radius: 12; -fx-border-width: 1;");
        container.setPadding(new Insets(10));

        return container;
    }

    // =====================================================
    //  Panneau d'informations des ascenseurs
    // =====================================================

    private VBox createInfoPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(5));
        panel.setAlignment(Pos.TOP_CENTER);

        for (Elevator elevator : building.getElevators()) {
            VBox card = createElevatorInfoCard(elevator);
            panel.getChildren().add(card);
        }

        return panel;
    }

    private VBox createElevatorInfoCard(Elevator elevator) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setPrefWidth(200);

        Color accentColor = (elevator.getId() == 0) ? ELEVATOR_1_COLOR : ELEVATOR_2_COLOR;
        String colorHex = toHex(accentColor);

        card.setStyle(String.format(
                "-fx-background-color: #1A1A2E; -fx-background-radius: 10;"
                        + "-fx-border-color: %s; -fx-border-radius: 10; -fx-border-width: 1.5;",
                colorHex));

        // Titre
        Label title = new Label(elevator.getDisplayName());
        title.setFont(Font.font("System", FontWeight.BOLD, 15));
        title.setTextFill(accentColor);

        // Indicateur d'état (cercle coloré)
        Circle statusDot = new Circle(5, Color.GRAY);
        HBox titleBox = new HBox(8, statusDot, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // Informations
        Label floorLabel = createInfoLabel(elevator.getId(), "floor", "Étage: 0");
        Label dirLabel = createInfoLabel(elevator.getId(), "direction", "Direction: ● Inactif");
        Label stateLabel = createInfoLabel(elevator.getId(), "state", "État: Inactif");
        Label occupancyLabel = createInfoLabel(elevator.getId(), "occupancy", "Occupants: 0/6");
        Label pendingLabel = createInfoLabel(elevator.getId(), "pending", "En attente: 0");
        Label statsLabel = createInfoLabel(elevator.getId(), "stats", "Servis: 0 | Étages: 0");

        // Stocker la référence au dot pour la mise à jour
        infoLabels.put("dot_" + elevator.getId(), new Label());

        card.getChildren().addAll(titleBox, new Separator(), floorLabel, dirLabel, stateLabel, occupancyLabel, pendingLabel, statsLabel);
        return card;
    }

    private Label createInfoLabel(int elevatorId, String key, String defaultText) {
        Label label = new Label(defaultText);
        label.setFont(Font.font("System", 12));
        label.setTextFill(Color.web("#BBBBDD"));
        infoLabels.put(key + "_" + elevatorId, label);
        return label;
    }

    // =====================================================
    //  Panneau du bas : Logs
    // =====================================================

    private VBox createBottomPanel() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10, 20, 15, 20));
        panel.setStyle("-fx-background-color: #0D0D18; -fx-border-color: #2A2A4A; -fx-border-width: 1 0 0 0;");

        Label logTitle = new Label("📋 Journal des événements");
        logTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        logTitle.setTextFill(Color.web("#8888BB"));

        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setPrefHeight(160);
        logTextArea.setWrapText(true);
        logTextArea.setStyle(
                "-fx-control-inner-background: #0A0A15; -fx-text-fill: #00FF88;"
                        + "-fx-font-family: 'Monospace'; -fx-font-size: 11;"
                        + "-fx-border-color: #2A2A4A; -fx-border-radius: 6;"
                        + "-fx-background-radius: 6;");

        panel.getChildren().addAll(logTitle, logTextArea);
        return panel;
    }

    // =====================================================
    //  Mise à jour de l'affichage
    // =====================================================

    /**
     * Met à jour toute l'interface graphique.
     * Doit être appelée depuis le JavaFX Application Thread.
     */
    public void updateUI() {
        if (!uiUpdatePending.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            uiUpdatePending.set(false);
            int totalFloors = building.getTotalFloors();
            double totalHeight = totalFloors * FLOOR_HEIGHT + 20;

            for (Elevator elevator : building.getElevators()) {
                int id = elevator.getId();
                int floor = elevator.getCurrentFloor();
                Direction dir = elevator.getDirection();
                ElevatorState state = elevator.getState();

                // --- Mise à jour de la position de la cabine ---
                double targetY = totalHeight - ((floor - building.getMinFloor() + 1) * FLOOR_HEIGHT) - 10
                        + (FLOOR_HEIGHT - CABIN_HEIGHT) / 2;

                Rectangle cabin = cabins[id];
                Text cabinText = cabinTexts[id];

                // Animation fluide
                cabin.setY(targetY);
                cabinText.setY(targetY + CABIN_HEIGHT / 2 + 5);

                // Couleur selon l'état
                Color baseColor = (id == 0) ? ELEVATOR_1_COLOR : ELEVATOR_2_COLOR;
                Color stoppedColor = (id == 0) ? ELEVATOR_1_STOPPED : ELEVATOR_2_STOPPED;

                if (state == ElevatorState.STOPPED) {
                    cabin.setFill(stoppedColor);
                    cabin.setStroke(Color.WHITE);
                } else if (state == ElevatorState.MOVING) {
                    cabin.setFill(baseColor);
                    cabin.setStroke(baseColor.brighter());
                } else {
                    cabin.setFill(baseColor.darker());
                    cabin.setStroke(baseColor);
                }

                // Texte de la cabine
                cabinText.setText(String.valueOf(floor));

                // --- Mise à jour du panneau d'informations ---
                updateInfoLabel("floor_" + id, "Étage: " + Building.getFloorLabel(floor));
                updateInfoLabel("direction_" + id, "Direction: " + dir.getLabel());
                updateInfoLabel("state_" + id, "État: " + state.getLabel());

                updateInfoLabel("occupancy_" + id, String.format("Occupants: %d/%d",
                        elevator.getCurrentPassengers(), Elevator.MAX_CAPACITY));

                List<Integer> pending = elevator.getPendingDestinations();
                String pendingStr = pending.isEmpty() ? "aucune" : pending.toString();
                updateInfoLabel("pending_" + id, "En attente: " + pendingStr);

                updateInfoLabel("stats_" + id, String.format("Servis: %d | Étages: %d",
                        elevator.getPassengersServed(), elevator.getTotalFloorsTraversed()));
            }
        });
    }

    /**
     * Met à jour la zone de logs.
     */
    public void updateLogs() {
        Platform.runLater(() -> {
            if (logTextArea != null) {
                logTextArea.setText(logService.getRecentLog(100));
                logTextArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    private void updateInfoLabel(String key, String text) {
        Label label = infoLabels.get(key);
        if (label != null) {
            label.setText(text);
        }
    }

    // =====================================================
    //  Utilitaires de style
    // =====================================================

    private void styleCallButton(Button btn, String bgColor, String hoverColor) {
        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold;"
                        + "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 13;",
                bgColor);
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold;"
                        + "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 13;"
                        + "-fx-effect: dropshadow(gaussian, %s, 8, 0.3, 0, 0);",
                hoverColor, hoverColor);

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
    }

    private void flashButton(Button btn, String flashColor) {
        String flashStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: black; -fx-font-weight: bold;"
                        + "-fx-background-radius: 6; -fx-font-size: 11;", flashColor);
        String originalStyle = btn.getStyle();
        btn.setStyle(flashStyle);

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> btn.setStyle(originalStyle));
        pause.play();
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Configure le callback de mise à jour des logs.
     */
    private void setupLogCallback() {
        logService.setOnLogUpdated(this::updateLogs);
    }

    public void setupHallCallDialogs() {
        controller.setHallCallArrivalCallback(floor -> {
            CompletableFuture<Integer> future = new CompletableFuture<>();
            Platform.runLater(() -> {
                List<String> choices = new ArrayList<>();
                Map<String, Integer> choiceMap = new HashMap<>();
                for (int f = building.getMinFloor(); f <= building.getMaxFloor(); f++) {
                    if (f != floor) {
                        String label = Building.getFloorLabel(f);
                        choices.add(label);
                        choiceMap.put(label, f);
                    }
                }
                ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
                dialog.setTitle("Selection d'etage");
                dialog.setHeaderText("Destination");
                dialog.setContentText("Vous etes a l'etage " + Building.getFloorLabel(floor));
                dialog.showAndWait().ifPresentOrElse(
                    selected -> future.complete(choiceMap.get(selected)),
                    () -> future.complete(floor)
                );
            });
            try {
                return future.get();
            } catch (Exception e) {
                return floor;
            }
        });
    }
}
