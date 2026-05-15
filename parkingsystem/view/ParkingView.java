package com.parkingsystem.view;

import com.parkingsystem.Transaction;
import com.parkingsystem.controller.ParkingController;
import com.parkingsystem.controller.ParkingEventListener;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingView implements ParkingEventListener {

    private static final String CREAM = "#efeae0";
    private static final String CARD_BG = "#fbf9f4";
    private static final String INK = "#1a1916";
    private static final String INK_MUTED = "rgba(26,25,22,0.55)";
    private static final String LINE = "rgba(26,25,22,0.10)";
    private static final String WARN = "#c89028";
    private static final String FULL = "#b6452f";
    private static final String OK = "#3f7a48";
    private static final String BLUE = "#2a5b8a";
    private static final String LOT_DARK1 = "#312e2a";
    private static final String LOT_DARK2 = "#232120";
    private static final String SPOT_EMPTY = "#2a2825";

    private static final String FONT_MONO = "Consolas";

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final double LOT_W = 760;
    private static final double LOT_H = 520;
    private static final double CAR_W = 46;
    private static final double CAR_H = 26;
    private static final double ENTRY_X = 30;
    private static final double ENTRY_Y = 26;
    private static final double EXIT_X = LOT_W - 30;
    private static final double EXIT_Y = 26;
    private static final Duration DRIVE_DUR = Duration.millis(900);

    private static final String[] CAR_PALETTE = {
            "#b6452f", "#2a5b8a", "#3f7a48", "#c89028",
            "#5e4a6e", "#1f7773", "#a44a22", "#3c3a36",
            "#8f8d88", "#7d3e3e"
    };

    private final ParkingController controller;
    private final Stage stage;

    private Label clockLabel;
    private Label occupiedVal;
    private Label queueVal;
    private Label revenueVal;
    private Label servedVal;

    private TextField nomField;
    private Spinner<Integer> capaciteSpinner;
    private Spinner<Double> tarifSpinner;
    private Button initButton;

    private TextField matriculeField;
    private Spinner<Integer> dureeSpinner;
    private Button spawnButton;
    private Button stopButton;
    private Label entryHint;

    private StackPane lot;
    private GridPane lotGrid;
    private Pane carLayer;
    private Label legendText;
    private final Map<Integer, Rectangle> spotRects = new HashMap<>();
    private final Map<String, Integer> plateToSpot = new HashMap<>();
    private final Map<String, Node> carNodes = new HashMap<>();
    private final BitSet usedSpots = new BitSet();

    private VBox sessionsBox;
    private final Map<String, ActiveSession> activeByPlate = new ConcurrentHashMap<>();

    private VBox logBox;
    private ScrollPane logScroll;

    private int queueCount = 0;
    private int occupiedCount = 0;
    private int servedCount = 0;
    private double revenue = 0.0;
    private int capacity = 0;
    private double tarifParHeure = 0.0;

    public ParkingView(Stage stage, ParkingController controller) {
        this.stage = stage;
        this.controller = controller;
        this.controller.setListener(this);
        buildScene();
        startTimers();
    }

    private void buildScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + CREAM + ";");
        root.setPadding(new Insets(28, 32, 40, 32));

        root.setTop(buildHeader());
        root.setCenter(buildMain());

        Scene scene = new Scene(root, 1240, 880);
        stage.setTitle("CityPark · Lot 03");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> controller.arreter());
        refreshStats();
    }

    private HBox buildHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.BOTTOM_LEFT);
        header.setPadding(new Insets(0, 0, 18, 0));
        header.setStyle("-fx-border-color: transparent transparent " + LINE + " transparent; -fx-border-width: 0 0 1 0;");

        Label logo = new Label("P");
        logo.setMinSize(38, 38);
        logo.setMaxSize(38, 38);
        logo.setAlignment(Pos.CENTER);
        logo.setStyle("-fx-background-color: " + INK + "; -fx-text-fill: " + CREAM
                + "; -fx-background-radius: 7; -fx-font-weight: 700; -fx-font-size: 16;");

        Label title = new Label("CityPark · Lot 03");
        title.setStyle("-fx-font-weight: 600; -fx-font-size: 17;");

        clockLabel = new Label("NORTH ANNEX · LEVEL 1 · 00:00:00");
        clockLabel.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11;");

        VBox brandText = new VBox(2, title, clockLabel);
        HBox brand = new HBox(14, logo, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(36);
        stats.setAlignment(Pos.BOTTOM_LEFT);
        occupiedVal = new Label("0 / 0");
        queueVal = new Label("0");
        revenueVal = new Label("0.00");
        servedVal = new Label("0");
        stats.getChildren().addAll(
                statBlock("OCCUPIED", occupiedVal),
                statBlock("IN QUEUE", queueVal),
                statBlock("TODAY'S REVENUE", revenueVal),
                statBlock("VEHICLES SERVED", servedVal)
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(brand, spacer, stats);
        return header;
    }

    private VBox statBlock(String label, Label valNode) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-size: 10; -fx-font-weight: 500;");
        styleStatVal(valNode, INK);
        return new VBox(3, l, valNode);
    }

    private void styleStatVal(Label l, String color) {
        l.setStyle("-fx-font-size: 24; -fx-font-weight: 700; -fx-font-family: '" + FONT_MONO + "'; -fx-text-fill: " + color + ";");
    }

    private HBox buildMain() {
        HBox main = new HBox(24);
        main.setPadding(new Insets(22, 0, 0, 0));
        main.getChildren().add(buildLotPane());
        VBox side = buildSidePanel();
        main.getChildren().add(side);
        HBox.setHgrow(side, Priority.ALWAYS);
        return main;
    }

    private VBox buildLotPane() {
        VBox box = new VBox(10);
        box.setPrefWidth(LOT_W);

        lot = new StackPane();
        lot.setPrefSize(LOT_W, LOT_H);
        lot.setMinSize(LOT_W, LOT_H);
        lot.setMaxSize(LOT_W, LOT_H);
        lot.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, " + LOT_DARK1 + ", " + LOT_DARK2 + ");"
                        + "-fx-background-radius: 14;"
        );

        Label entrySign = new Label("→ ENTRY");
        entrySign.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 9;");
        Label exitSign = new Label("EXIT →");
        exitSign.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 9;");
        HBox signRow = new HBox();
        signRow.setPadding(new Insets(0, 30, 0, 30));
        Region midSpace = new Region();
        HBox.setHgrow(midSpace, Priority.ALWAYS);
        signRow.getChildren().addAll(entrySign, midSpace, exitSign);

        lotGrid = new GridPane();
        lotGrid.setHgap(10);
        lotGrid.setVgap(14);
        lotGrid.setAlignment(Pos.CENTER);

        VBox lotInner = new VBox(8, signRow, lotGrid);
        lotInner.setAlignment(Pos.CENTER);
        lotInner.setPadding(new Insets(24));

        carLayer = new Pane();
        carLayer.setMouseTransparent(true);
        carLayer.setPrefSize(LOT_W, LOT_H);
        carLayer.setMaxSize(LOT_W, LOT_H);

        lot.getChildren().addAll(lotInner, carLayer);

        legendText = new Label("box layout · awaiting init");
        legendText.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11;");

        HBox legend = new HBox(14);
        legend.setPadding(new Insets(0, 4, 0, 4));
        legend.setAlignment(Pos.CENTER_LEFT);
        Label paletteHint = new Label("■ asphalt   ■ occupied   ■ empty");
        paletteHint.setStyle("-fx-text-fill: rgba(26,25,22,0.45); -fx-font-size: 10.5;");
        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);
        legend.getChildren().addAll(paletteHint, s, legendText);

        box.getChildren().addAll(lot, legend);
        return box;
    }

    private void buildLotGrid(int capacite) {
        lotGrid.getChildren().clear();
        spotRects.clear();
        int cols = Math.min(4, Math.max(1, capacite));
        for (int i = 0; i < capacite; i++) {
            int col = i % cols;
            int row = i / cols;
            lotGrid.add(buildSpotCell(i), col, row);
        }
        legendText.setText(String.format("capacity %d · %.2f MAD/h", capacite, tarifParHeure));
    }

    private VBox buildSpotCell(int index) {
        Rectangle rect = new Rectangle(120, 78);
        rect.setArcWidth(6);
        rect.setArcHeight(6);
        rect.setFill(Color.web(SPOT_EMPTY));
        rect.setStroke(Color.web("rgba(255,255,255,0.45)"));
        rect.setStrokeWidth(1.5);
        rect.getStrokeDashArray().setAll(2.0, 4.0);
        spotRects.put(index, rect);

        Label label = new Label(String.format("SPOT %02d", index + 1));
        label.setStyle("-fx-text-fill: rgba(255,255,255,0.32); -fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 9;");

        VBox cell = new VBox(6, rect, label);
        cell.setAlignment(Pos.CENTER);
        return cell;
    }

    private VBox buildSidePanel() {
        VBox side = new VBox(12);
        side.setPrefWidth(420);
        side.getChildren().addAll(
                buildConfigCard(),
                buildEntryCard(),
                buildSessionsCard(),
                buildLogCard()
        );
        return side;
    }

    private VBox buildConfigCard() {
        VBox card = card();
        nomField = new TextField("Parking Central");
        styleInput(nomField);
        capaciteSpinner = new Spinner<>(1, 50, 8);
        capaciteSpinner.setEditable(true);
        capaciteSpinner.setPrefWidth(100);
        tarifSpinner = new Spinner<>(0.5, 200.0, 10.0, 0.5);
        tarifSpinner.setEditable(true);
        tarifSpinner.setPrefWidth(100);
        initButton = new Button("Initialize lot & start simulation");
        stylePrimaryButton(initButton);
        initButton.setMaxWidth(Double.MAX_VALUE);
        initButton.setOnAction(e -> onInitialiser());

        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.add(small("NAME"), 0, 0);
        g.add(nomField, 1, 0);
        GridPane.setHgrow(nomField, Priority.ALWAYS);
        g.add(small("CAPACITY"), 0, 1);
        g.add(capaciteSpinner, 1, 1);
        g.add(small("RATE/HOUR (MAD)"), 0, 2);
        g.add(tarifSpinner, 1, 2);

        card.getChildren().addAll(sectionTitle("CONFIGURATION"), g, initButton);
        return card;
    }

    private VBox buildEntryCard() {
        VBox card = card();

        matriculeField = new TextField();
        matriculeField.setPromptText("AB-123-CD");
        matriculeField.setStyle(
                "-fx-background-color: #fff8e6; -fx-border-color: rgba(26,25,22,0.18); -fx-border-radius: 6;"
                        + "-fx-background-radius: 6; -fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 15;"
                        + "-fx-font-weight: 700; -fx-padding: 9 12;"
        );

        dureeSpinner = new Spinner<>(500, 60000, 4000, 500);
        dureeSpinner.setEditable(true);
        dureeSpinner.setPrefWidth(110);

        HBox plateRow = new HBox(6, matriculeField, dureeSpinner);
        HBox.setHgrow(matriculeField, Priority.ALWAYS);

        spawnButton = new Button("Add manual car →");
        stylePrimaryButton(spawnButton);
        spawnButton.setMaxWidth(Double.MAX_VALUE);
        spawnButton.setDisable(true);
        spawnButton.setOnAction(e -> onAjouterVoiture());

        stopButton = new Button("Stop simulation");
        stopButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " + FULL
                + "; -fx-border-color: " + FULL + "; -fx-border-radius: 7; -fx-background-radius: 7;"
                + " -fx-padding: 8 14; -fx-font-size: 12;");
        stopButton.setMaxWidth(Double.MAX_VALUE);
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> onArreter());

        entryHint = new Label("— lot not initialized —");
        entryHint.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-size: 11;");

        card.getChildren().addAll(
                sectionTitle("ENTRY TERMINAL  ·  auto-sim runs on init"),
                small("OPTIONAL MANUAL PLATE  /  STAY (MS)"),
                plateRow,
                spawnButton,
                stopButton,
                entryHint
        );
        return card;
    }

    private VBox buildSessionsCard() {
        VBox card = card();
        sessionsBox = new VBox(0);
        sessionsBox.getChildren().add(emptyLabel("— no vehicles parked —"));
        card.getChildren().addAll(sectionTitle("ACTIVE SESSIONS"), sessionsBox);
        return card;
    }

    private VBox buildLogCard() {
        VBox card = card();
        logBox = new VBox(5);
        logBox.getChildren().add(emptyLabel("— waiting for first vehicle —"));
        logScroll = new ScrollPane(logBox);
        logScroll.setFitToWidth(true);
        logScroll.setPrefViewportHeight(160);
        logScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        card.getChildren().addAll(sectionTitle("ACTIVITY LOG"), logScroll);
        return card;
    }

    private VBox card() {
        VBox c = new VBox(10);
        c.setPadding(new Insets(14, 16, 15, 16));
        c.setStyle(
                "-fx-background-color: " + CARD_BG + ";"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: " + LINE + "; -fx-border-radius: 12; -fx-border-width: 0.5;"
        );
        return c;
    }

    private Label sectionTitle(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-font-size: 10.5; -fx-font-weight: 700; -fx-text-fill: rgba(26,25,22,0.7);");
        return l;
    }

    private Label small(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-size: 10.5; -fx-font-weight: 500;");
        return l;
    }

    private Label emptyLabel(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: rgba(26,25,22,0.4); -fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11; -fx-padding: 6 0;");
        return l;
    }

    private void styleInput(TextField tf) {
        tf.setStyle("-fx-background-color: white; -fx-border-color: rgba(26,25,22,0.15); -fx-border-radius: 6;"
                + " -fx-background-radius: 6; -fx-padding: 6 10;");
    }

    private void stylePrimaryButton(Button b) {
        b.setStyle("-fx-background-color: " + INK + "; -fx-text-fill: " + CREAM
                + "; -fx-background-radius: 7; -fx-padding: 10 14; -fx-font-size: 12.5; -fx-font-weight: 500;");
    }

    private void onInitialiser() {
        String nom = nomField.getText().trim();
        if (nom.isEmpty()) {
            entryHint.setText("⚠ Lot name required");
            return;
        }
        int cap = capaciteSpinner.getValue();
        double tarif = tarifSpinner.getValue();
        capacity = cap;
        tarifParHeure = tarif;
        occupiedCount = 0;
        queueCount = 0;
        servedCount = 0;
        revenue = 0.0;
        activeByPlate.clear();
        plateToSpot.clear();
        usedSpots.clear();
        carLayer.getChildren().clear();
        carNodes.clear();
        controller.initialiserParking(nom, cap, tarif);
        buildLotGrid(cap);
        rebuildSessions();
        clearLog();
        logEvent("·", String.format("LOT INITIALIZED · %s · cap %d · %.2f MAD/h · auto-sim ON", nom, cap, tarif), INK_MUTED);
        spawnButton.setDisable(false);
        stopButton.setDisable(false);
        refreshStats();
    }

    private void onAjouterVoiture() {
        String matricule = matriculeField.getText().trim();
        if (matricule.isEmpty()) {
            entryHint.setText("⚠ Plate required");
            return;
        }
        int duree = dureeSpinner.getValue();
        queueCount++;
        controller.ajouterVoiture(matricule, duree);
        matriculeField.clear();
        matriculeField.requestFocus();
        refreshStats();
    }

    private void onArreter() {
        controller.arreter();
        spawnButton.setDisable(true);
        stopButton.setDisable(true);
    }

    private void refreshStats() {
        occupiedVal.setText(occupiedCount + " / " + capacity);
        queueVal.setText(String.valueOf(queueCount));
        revenueVal.setText(String.format("%.2f", revenue));
        servedVal.setText(String.valueOf(servedCount));

        boolean full = capacity > 0 && occupiedCount >= capacity;
        String occColor = full ? FULL : (capacity > 0 && occupiedCount * 4 >= capacity * 3 ? WARN : INK);
        styleStatVal(occupiedVal, occColor);
        String qColor = queueCount > 0 && full ? FULL : (queueCount > 0 ? WARN : INK);
        styleStatVal(queueVal, qColor);
        styleStatVal(revenueVal, INK);
        styleStatVal(servedVal, INK);

        int free = Math.max(0, capacity - occupiedCount);
        if (capacity == 0) {
            entryHint.setText("— lot not initialized —");
        } else if (full) {
            entryHint.setText("● LOT FULL — new cars queue");
        } else if (queueCount > 0) {
            entryHint.setText(String.format("● %d free · %d ahead", free, queueCount));
        } else {
            entryHint.setText(String.format("● %d spot%s free", free, free == 1 ? "" : "s"));
        }
    }

    private int allocateSpot() {
        int s = usedSpots.nextClearBit(0);
        if (s >= capacity) return -1;
        usedSpots.set(s);
        return s;
    }

    private void releaseSpot(int s) {
        usedSpots.clear(s);
    }

    private void colorSpot(int s, String hexColor) {
        Rectangle r = spotRects.get(s);
        if (r != null) {
            r.setFill(Color.web(hexColor));
            r.setStroke(Color.web("rgba(255,255,255,0.55)"));
            r.getStrokeDashArray().clear();
        }
    }

    private void emptySpot(int s) {
        Rectangle r = spotRects.get(s);
        if (r != null) {
            r.setFill(Color.web(SPOT_EMPTY));
            r.setStroke(Color.web("rgba(255,255,255,0.45)"));
            r.getStrokeDashArray().setAll(2.0, 4.0);
        }
    }

    private String pickColor(String plate) {
        int h = Math.floorMod(plate.hashCode(), CAR_PALETTE.length);
        return CAR_PALETTE[h];
    }

    private double[] spotCenterInLayer(int spotIndex) {
        Rectangle rect = spotRects.get(spotIndex);
        if (rect == null) return new double[]{LOT_W / 2.0, LOT_H / 2.0};
        lot.applyCss();
        lot.layout();
        Bounds sceneBounds = rect.localToScene(rect.getBoundsInLocal());
        Bounds local = carLayer.sceneToLocal(sceneBounds);
        return new double[]{
                local.getMinX() + local.getWidth() / 2.0,
                local.getMinY() + local.getHeight() / 2.0
        };
    }

    private Group buildCarNode(String hexColor) {
        Color body = Color.web(hexColor);
        Color bodyDark = body.darker();
        Color cabin = Color.web("rgba(20,28,40,0.85)");
        Color wheel = Color.web("#15130f");
        Color headlight = Color.web("#f4ecc3");
        Color tail = Color.web("#a02a1c");

        double wheelW = 5;
        double wheelH = 7;

        Rectangle rearWheelL = new Rectangle(-1, 2, wheelW, wheelH);
        Rectangle rearWheelR = new Rectangle(-1, CAR_H - wheelH - 2, wheelW, wheelH);
        Rectangle frontWheelL = new Rectangle(CAR_W - wheelW + 1, 2, wheelW, wheelH);
        Rectangle frontWheelR = new Rectangle(CAR_W - wheelW + 1, CAR_H - wheelH - 2, wheelW, wheelH);
        for (Rectangle w : new Rectangle[]{rearWheelL, rearWheelR, frontWheelL, frontWheelR}) {
            w.setFill(wheel);
            w.setArcWidth(2);
            w.setArcHeight(2);
        }

        Rectangle chassis = new Rectangle(0, 0, CAR_W, CAR_H);
        chassis.setArcWidth(11);
        chassis.setArcHeight(11);
        chassis.setFill(body);
        chassis.setStroke(bodyDark);
        chassis.setStrokeWidth(1);

        double cabinX = CAR_W * 0.22;
        double cabinW = CAR_W * 0.50;
        double cabinInsetY = 3.5;
        Rectangle cabinShape = new Rectangle(cabinX, cabinInsetY, cabinW, CAR_H - cabinInsetY * 2);
        cabinShape.setArcWidth(6);
        cabinShape.setArcHeight(6);
        cabinShape.setFill(cabin);

        Rectangle windshieldSplit = new Rectangle(cabinX + cabinW * 0.55, cabinInsetY + 1, 1, CAR_H - cabinInsetY * 2 - 2);
        windshieldSplit.setFill(bodyDark);

        double lightW = 2.2;
        double lightH = 3;
        Rectangle headLightL = new Rectangle(CAR_W - lightW - 0.5, 4, lightW, lightH);
        Rectangle headLightR = new Rectangle(CAR_W - lightW - 0.5, CAR_H - 4 - lightH, lightW, lightH);
        headLightL.setFill(headlight);
        headLightR.setFill(headlight);
        headLightL.setArcWidth(1.5);
        headLightL.setArcHeight(1.5);
        headLightR.setArcWidth(1.5);
        headLightR.setArcHeight(1.5);

        Rectangle tailLightL = new Rectangle(0, 4, lightW, lightH);
        Rectangle tailLightR = new Rectangle(0, CAR_H - 4 - lightH, lightW, lightH);
        tailLightL.setFill(tail);
        tailLightR.setFill(tail);
        tailLightL.setArcWidth(1.5);
        tailLightL.setArcHeight(1.5);
        tailLightR.setArcWidth(1.5);
        tailLightR.setArcHeight(1.5);

        Circle roofDot = new Circle(CAR_W * 0.47, CAR_H / 2.0, 1.3);
        roofDot.setFill(Color.web("rgba(255,255,255,0.25)"));

        Group car = new Group(
                rearWheelL, rearWheelR, frontWheelL, frontWheelR,
                chassis,
                tailLightL, tailLightR, headLightL, headLightR,
                cabinShape, windshieldSplit, roofDot
        );
        return car;
    }

    private void animateCarEntry(String matricule, int spotIndex, String color) {
        double[] target = spotCenterInLayer(spotIndex);
        Group car = buildCarNode(color);
        car.setLayoutX(ENTRY_X);
        car.setLayoutY(ENTRY_Y);
        carLayer.getChildren().add(car);
        carNodes.put(matricule, car);

        TranslateTransition t = new TranslateTransition(DRIVE_DUR, car);
        t.setToX(target[0] - ENTRY_X - CAR_W / 2.0);
        t.setToY(target[1] - ENTRY_Y - CAR_H / 2.0);
        t.play();
    }

    private void animateCarExit(String matricule, Runnable onFinish) {
        Node car = carNodes.remove(matricule);
        if (car == null) {
            if (onFinish != null) onFinish.run();
            return;
        }
        TranslateTransition t = new TranslateTransition(DRIVE_DUR, car);
        t.setToX(EXIT_X - car.getLayoutX() - CAR_W / 2.0);
        t.setToY(EXIT_Y - car.getLayoutY() - CAR_H / 2.0);
        t.setOnFinished(e -> {
            carLayer.getChildren().remove(car);
            if (onFinish != null) onFinish.run();
        });
        t.play();
    }

    private void rebuildSessions() {
        sessionsBox.getChildren().clear();
        if (activeByPlate.isEmpty()) {
            sessionsBox.getChildren().add(emptyLabel("— no vehicles parked —"));
            return;
        }
        for (ActiveSession s : activeByPlate.values()) {
            sessionsBox.getChildren().add(sessionRow(s));
        }
    }

    private HBox sessionRow(ActiveSession s) {
        Rectangle dot = new Rectangle(10, 10);
        dot.setArcWidth(3);
        dot.setArcHeight(3);
        dot.setFill(Color.web(s.color));

        Label plate = new Label(s.matricule);
        plate.setStyle("-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 12; -fx-font-weight: 700;");
        Label meta = new Label(String.format("SPOT %02d · IN @ %s", s.spotIndex + 1, s.entree.format(CLOCK_FMT)));
        meta.setStyle("-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 10; -fx-text-fill: " + INK_MUTED + ";");
        VBox left = new VBox(2, plate, meta);

        Label elapsed = new Label("00:00");
        elapsed.setStyle("-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11; -fx-text-fill: rgba(26,25,22,0.75);");
        Label fee = new Label("0.00 MAD");
        fee.setStyle("-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 12; -fx-font-weight: 700;");
        VBox right = new VBox(2, elapsed, fee);
        right.setAlignment(Pos.CENTER_RIGHT);

        s.elapsedLabel = elapsed;
        s.feeLabel = fee;

        HBox row = new HBox(10, dot, left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 0, 9, 0));
        row.setStyle("-fx-border-color: transparent transparent " + LINE + " transparent; -fx-border-style: dashed; -fx-border-width: 0 0 1 0;");
        HBox.setHgrow(left, Priority.ALWAYS);
        return row;
    }

    private void logEvent(String sym, String msg, String symColor) {
        if (logBox.getChildren().size() == 1 && logBox.getChildren().get(0) instanceof Label l
                && l.getText() != null && l.getText().startsWith("—")) {
            logBox.getChildren().clear();
        }
        Label ts = new Label(LocalTime.now().format(CLOCK_FMT));
        ts.setStyle("-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11; -fx-text-fill: rgba(26,25,22,0.4);");
        Label symL = new Label(sym);
        symL.setStyle("-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11; -fx-text-fill: " + symColor + "; -fx-font-weight: 700;");
        Label m = new Label(msg);
        m.setStyle("-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11;");
        symL.setMinWidth(14);
        ts.setMinWidth(60);
        HBox row = new HBox(8, ts, symL, m);
        row.setAlignment(Pos.CENTER_LEFT);
        logBox.getChildren().add(0, row);
        while (logBox.getChildren().size() > 80) {
            logBox.getChildren().remove(logBox.getChildren().size() - 1);
        }
    }

    private void clearLog() {
        logBox.getChildren().clear();
        logBox.getChildren().add(emptyLabel("— waiting for first vehicle —"));
    }

    private void startTimers() {
        AnimationTimer timer = new AnimationTimer() {
            long lastTick = 0;

            @Override
            public void handle(long now) {
                if (now - lastTick < 250_000_000L) return;
                lastTick = now;
                clockLabel.setText("NORTH ANNEX · LEVEL 1 · " + LocalTime.now().format(CLOCK_FMT));
                long nowMs = System.currentTimeMillis();
                for (ActiveSession s : activeByPlate.values()) {
                    long elapsedSec = (nowMs - s.entreeMillis) / 1000;
                    double fee = (elapsedSec / 3600.0) * tarifParHeure;
                    if (s.elapsedLabel != null) {
                        s.elapsedLabel.setText(String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60));
                    }
                    if (s.feeLabel != null) {
                        s.feeLabel.setText(String.format("%.2f MAD", fee));
                    }
                }
            }
        };
        timer.start();
    }

    @Override
    public void onAttente(String matricule, int placesDisponibles) {
        Platform.runLater(() ->
                logEvent("↘", String.format("approach · %s · waiting (%d free)", matricule, placesDisponibles), BLUE)
        );
    }

    @Override
    public void onEntree(String matricule, int placesDisponibles) {
        Platform.runLater(() -> {
            int spot = allocateSpot();
            if (spot >= 0) {
                plateToSpot.put(matricule, spot);
                String color = pickColor(matricule);
                colorSpot(spot, color);
                ActiveSession s = new ActiveSession(matricule, spot, color,
                        LocalDateTime.now(), System.currentTimeMillis());
                activeByPlate.put(matricule, s);
                rebuildSessions();
                animateCarEntry(matricule, spot, color);
                logEvent("↘", String.format("admitted · %s → spot %02d", matricule, spot + 1), BLUE);
            }
            occupiedCount = capacity - placesDisponibles;
            queueCount = Math.max(0, queueCount - 1);
            refreshStats();
        });
    }

    @Override
    public void onSortie(String matricule, Transaction transaction, int placesDisponibles) {
        Platform.runLater(() -> {
            Integer spot = plateToSpot.remove(matricule);
            activeByPlate.remove(matricule);
            rebuildSessions();
            servedCount++;
            revenue += transaction.montant();
            occupiedCount = capacity - placesDisponibles;
            if (spot != null) releaseSpot(spot);
            logEvent("↗", String.format("exit · %s · %d min · %.2f MAD",
                    matricule, transaction.dureeMinutes(), transaction.montant()), OK);
            refreshStats();
            Color departingColor = Color.web(pickColor(matricule));
            animateCarExit(matricule, () -> {
                if (spot != null) {
                    Rectangle r = spotRects.get(spot);
                    if (r != null && departingColor.equals(r.getFill())) {
                        emptySpot(spot);
                    }
                }
            });
        });
    }

    @Override
    public void onErreur(String matricule, String message) {
        Platform.runLater(() ->
                logEvent("■", String.format("error · %s · %s", matricule, message), FULL)
        );
    }

    @Override
    public void onSimulationTerminee() {
        Platform.runLater(() -> {
            logEvent("·", "simulation stopped", INK_MUTED);
            spawnButton.setDisable(true);
            stopButton.setDisable(true);
        });
    }

    @Override
    public void onCsvSauvegarde(String cheminCsv) {
        Platform.runLater(() ->
                logEvent("·", "CSV saved · " + cheminCsv, OK)
        );
    }

    private static class ActiveSession {
        final String matricule;
        final int spotIndex;
        final String color;
        final LocalDateTime entree;
        final long entreeMillis;
        Label elapsedLabel;
        Label feeLabel;

        ActiveSession(String m, int s, String c, LocalDateTime e, long ms) {
            this.matricule = m;
            this.spotIndex = s;
            this.color = c;
            this.entree = e;
            this.entreeMillis = ms;
        }
    }
}
