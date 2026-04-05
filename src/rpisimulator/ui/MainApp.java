package rpisimulator.ui;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import rpisimulator.cpu.CPU;
import rpisimulator.cpu.DemoPrograms;

import java.util.List;

public class MainApp extends Application {

    private CPU cpu = new CPU();
    private List<DemoPrograms.Step> currentProgram;
    private int stepIndex = 0;

    // Registres UI
    private Label[] regLabels = new Label[13];
    private Label spLabel, lrLabel, pcLabel;
    private Label flagN, flagZ, flagC, flagV;
    private Label cpsrLabel;
    private TextArea logArea;
    private Label statusLabel;
    private Label stepCountLabel;
    private ComboBox<String> programSelector;
    private Button btnStep, btnRun, btnReset;
    private Label instrLabel;

    // Grille mémoire RAM
    private static final int MEM_COLS = 16;
    private static final int MEM_ROWS = 16; // 256 cellules visibles
    private Label[][] memCells = new Label[MEM_ROWS][MEM_COLS];
    private Label[] memAddrLabels = new Label[MEM_ROWS];

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0D1117;");

        root.setTop(buildHeader());
        root.setLeft(buildRegistersPanel());
        root.setRight(buildFlagsPanel());
        root.setBottom(buildBottomBar());

        // Centre : journal + mémoire RAM en dessous
        VBox centerAndMem = new VBox(0);
        javafx.scene.Node centerPanel = buildCenterPanel();
        VBox.setVgrow(centerPanel, Priority.ALWAYS);
        centerAndMem.getChildren().addAll(centerPanel, buildMemoryPanel());
        root.setCenter(centerAndMem);

        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/rpisimulator/ui/style.css").toExternalForm());

        stage.setTitle("ARM Cortex-A — Simulateur Raspberry Pi");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        loadProgram("Addition simple");
    }

    // ─── Header ──────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setSpacing(16);
        header.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-width: 0 0 1 0;");

        // Logo / titre
        VBox titleBox = new VBox(2);
        Label title = new Label("ARM CORTEX-A");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #58A6FF;");
        Label subtitle = new Label("Raspberry Pi — Register Simulator");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B949E;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sélection programme
        Label progLabel = new Label("Programme :");
        progLabel.setStyle("-fx-text-fill: #8B949E; -fx-font-size: 12px;");
        programSelector = new ComboBox<>();
        programSelector.getItems().addAll(
            "Addition simple", "Opérations logiques",
            "Décalages de bits", "Multiplication & CMP", "Appel de fonction"
        );
        programSelector.setValue("Addition simple");
        programSelector.setStyle(
            "-fx-background-color: #21262D; -fx-border-color: #58A6FF;" +
            "-fx-border-radius: 6; -fx-background-radius: 6;"
        );
        // Fix: text-fill ignoré par ComboBox sous Windows — on force via cellFactory
        programSelector.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setStyle("-fx-text-fill: #E6EDF3; -fx-background-color: #21262D; -fx-font-family: 'Courier New'; -fx-font-size: 12px;");
            }
        });
        programSelector.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setStyle("-fx-text-fill: #E6EDF3; -fx-background-color: #21262D; -fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-padding: 6 12;");
            }
        });
        programSelector.setOnAction(e -> loadProgram(programSelector.getValue()));

        // Boutons contrôle
        btnReset = buildButton("⟳ Reset", "#21262D", "#8B949E");
        btnStep  = buildButton("▶ Step",  "#1F6FEB", "#FFFFFF");
        btnRun   = buildButton("⚡ Run All", "#238636", "#FFFFFF");

        btnReset.setOnAction(e -> resetCPU());
        btnStep.setOnAction(e  -> executeStep());
        btnRun.setOnAction(e   -> runAll());

        header.getChildren().addAll(titleBox, spacer, progLabel, programSelector,
                                    new Separator(Orientation.VERTICAL), btnReset, btnStep, btnRun);
        return header;
    }

    // ─── Panel registres (gauche) ─────────────────────────────────────────────

    private VBox buildRegistersPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(230);
        panel.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-width: 0 1 0 0;");

        Label title = sectionTitle("REGISTRES ARM");
        panel.getChildren().add(title);

        // R0-R12
        for (int i = 0; i < 13; i++) {
            regLabels[i] = new Label("0x00000000");
            regLabels[i].setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: #79C0FF;");
            panel.getChildren().add(buildRegRow("R" + i, regLabels[i]));
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #30363D;");
        panel.getChildren().add(sep);

        // Registres spéciaux
        spLabel = new Label("0x00001000");
        lrLabel = new Label("0x00000000");
        pcLabel = new Label("0x00000000");

        for (Label l : new Label[]{spLabel, lrLabel, pcLabel})
            l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: #FFA657;");

        panel.getChildren().addAll(
            buildRegRow("SP  (R13)", spLabel),
            buildRegRow("LR  (R14)", lrLabel),
            buildRegRow("PC  (R15)", pcLabel)
        );

        return panel;
    }

    private HBox buildRegRow(String name, Label valLabel) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 14, 6, 14));
        row.setStyle("-fx-border-color: transparent transparent #21262D transparent; -fx-border-width: 0 0 1 0;");

        Label nameLabel = new Label(name);
        nameLabel.setPrefWidth(80);
        nameLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #8B949E;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        row.getChildren().addAll(nameLabel, sp, valLabel);

        // Hover
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #21262D; -fx-border-color: transparent transparent #30363D transparent; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-border-color: transparent transparent #21262D transparent; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    // ─── Panel central : instruction + log ────────────────────────────────────

    private VBox buildCenterPanel() {
        VBox center = new VBox(0);
        center.setStyle("-fx-background-color: #0D1117;");

        // Instruction courante
        VBox instrBox = new VBox(8);
        instrBox.setPadding(new Insets(20, 24, 20, 24));
        instrBox.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-width: 0 0 1 0;");

        Label instrTitle = new Label("INSTRUCTION EN COURS");
        instrTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E; -fx-font-family: 'Courier New'; -fx-letter-spacing: 1.5;");

        instrLabel = new Label("—");
        instrLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-text-fill: #7EE787; -fx-font-weight: bold;");

        stepCountLabel = new Label("Étape 0 / 0");
        stepCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B949E;");

        instrBox.getChildren().addAll(instrTitle, instrLabel, stepCountLabel);

        // Description de l'instruction
        statusLabel = new Label("Sélectionnez un programme et appuyez sur Step");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8B949E; -fx-padding: 0 24 12 24;");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #CDD9E5; -fx-padding: 10 24 10 24;");

        // Log d'exécution
        Label logTitle = sectionTitle("JOURNAL D'EXÉCUTION");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-control-inner-background: #0D1117; -fx-text-fill: #8B949E; -fx-border-color: transparent;");
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        center.getChildren().addAll(instrBox, statusLabel, logTitle, logArea);
        return center;
    }

    // ─── Panel flags (droite) ─────────────────────────────────────────────────

    private VBox buildFlagsPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(180);
        panel.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-width: 0 0 0 1;");

        panel.getChildren().add(sectionTitle("FLAGS CPSR"));

        flagN = buildFlagBox("N", "Negative", "#FF7B72");
        flagZ = buildFlagBox("Z", "Zero",     "#7EE787");
        flagC = buildFlagBox("C", "Carry",    "#FFA657");
        flagV = buildFlagBox("V", "Overflow", "#D2A8FF");

        panel.getChildren().addAll(flagN, flagZ, flagC, flagV);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #30363D;");
        panel.getChildren().add(sep);

        // CPSR valeur
        VBox cpsrBox = new VBox(4);
        cpsrBox.setPadding(new Insets(14));
        Label cpsrTitle = new Label("CPSR");
        cpsrTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E; -fx-font-family: 'Courier New';");
        cpsrLabel = new Label("0x00000000");
        cpsrLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-text-fill: #E6EDF3;");
        cpsrBox.getChildren().addAll(cpsrTitle, cpsrLabel);
        panel.getChildren().add(cpsrBox);

        sep = new Separator();
        sep.setStyle("-fx-background-color: #30363D;");
        panel.getChildren().add(sep);

        // Info ARM
        VBox infoBox = new VBox(6);
        infoBox.setPadding(new Insets(14));
        Label infoTitle = new Label("ARCHITECTURE");
        infoTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E; -fx-font-family: 'Courier New';");

        String[] infos = {"ARM Cortex-A72", "ARMv8 32-bit", "16 registres", "RISC — 4 octets/instr", "Raspberry Pi 4"};
        String[] colors = {"#58A6FF", "#8B949E", "#8B949E", "#8B949E", "#3FB950"};
        infoBox.getChildren().add(infoTitle);
        for (int i = 0; i < infos.length; i++) {
            Label l = new Label(infos[i]);
            l.setStyle("-fx-font-size: 11px; -fx-text-fill: " + colors[i] + "; -fx-font-family: 'Courier New';");
            infoBox.getChildren().add(l);
        }
        panel.getChildren().add(infoBox);

        return panel;
    }

    private Label buildFlagBox(String letter, String name, String color) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle("-fx-border-color: transparent transparent #21262D transparent; -fx-border-width: 0 0 1 0;");

        Label ltr = new Label(letter + " = 0");
        ltr.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #30363D;");
        Label nm = new Label(name);
        nm.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E;");
        box.getChildren().addAll(ltr, nm);

        Label result = ltr;
        result.setUserData(new String[]{letter, color});
        box.setUserData(result);
        return result;
    }


    // ─── Panel mémoire RAM ────────────────────────────────────────────────────

    private VBox buildMemoryPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0D1117; -fx-border-color: #30363D; -fx-border-width: 1 0 0 0;");

        // Header avec titre + légende
        HBox header = new HBox(16);
        header.setPadding(new Insets(8, 14, 8, 14));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-width: 0 0 1 0;");

        Label title = new Label("RAM — 256 OCTETS");
        title.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E; -fx-font-family: 'Courier New';");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Légende couleurs
        HBox leg1 = makeLegend("#FFA657", "Ecrit (STR)");
        HBox leg2 = makeLegend("#58A6FF", "Lu (LDR)");
        HBox leg3 = makeLegend("#30363D", "Vide (0x00)");

        header.getChildren().addAll(title, spacer, leg3, leg1, leg2);

        // Grille hexadécimale scrollable
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(180);
        scroll.setStyle("-fx-background-color: #0D1117; -fx-background: #0D1117;");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8, 14, 8, 14));
        grid.setHgap(3);
        grid.setVgap(3);

        // Colonne header hex (00-0F)
        Label corner = new Label("ADDR");
        corner.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #484F58; -fx-min-width: 50px;");
        grid.add(corner, 0, 0);
        for (int c = 0; c < MEM_COLS; c++) {
            Label h = new Label(String.format("+%X", c));
            h.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #484F58; -fx-min-width: 32px; -fx-alignment: center;");
            grid.add(h, c + 1, 0);
        }

        // Lignes de mémoire
        for (int row = 0; row < MEM_ROWS; row++) {
            int baseAddr = row * MEM_COLS;
            memAddrLabels[row] = new Label(String.format("0x%02X", baseAddr));
            memAddrLabels[row].setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #484F58; -fx-min-width: 50px;");
            grid.add(memAddrLabels[row], 0, row + 1);

            for (int col = 0; col < MEM_COLS; col++) {
                Label cell = new Label("00");
                cell.setMinWidth(32);
                cell.setAlignment(Pos.CENTER);
                cell.setStyle(cellStyle(false, false));
                memCells[row][col] = cell;
                grid.add(cell, col + 1, row + 1);
            }
        }

        scroll.setContent(grid);
        panel.getChildren().addAll(header, scroll);
        return panel;
    }

    private HBox makeLegend(String color, String label) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(10, 10);
        rect.setFill(javafx.scene.paint.Color.web(color));
        rect.setArcWidth(3); rect.setArcHeight(3);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E; -fx-font-family: 'Courier New';");
        box.getChildren().addAll(rect, l);
        return box;
    }

    private String cellStyle(boolean written, boolean nonZero) {
        String bg = written  ? "#2D1E0F" :
                    nonZero  ? "#0D1E2D" : "#161B22";
        String fg = written  ? "#FFA657" :
                    nonZero  ? "#58A6FF" : "#30363D";
        String border = written ? "#FFA657" : nonZero ? "#1F6FEB" : "#21262D";
        return "-fx-font-family: 'Courier New';" +
               "-fx-font-size: 11px;" +
               "-fx-text-fill: " + fg + ";" +
               "-fx-background-color: " + bg + ";" +
               "-fx-border-color: " + border + ";" +
               "-fx-border-width: 1;" +
               "-fx-background-radius: 3;" +
               "-fx-border-radius: 3;" +
               "-fx-alignment: center;" +
               "-fx-padding: 2 4;";
    }

    // ─── Bottom bar ───────────────────────────────────────────────────────────

    private HBox buildBottomBar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-width: 1 0 0 0;");

        Label hint = new Label("💡 Step : exécute une instruction | Run All : exécute le programme complet | Reset : remet à zéro le CPU");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B949E;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label ver = new Label("v2.0 — JavaFX + RAM");
        ver.setStyle("-fx-font-size: 11px; -fx-text-fill: #30363D; -fx-font-family: 'Courier New';");

        bar.getChildren().addAll(hint, sp, ver);
        return bar;
    }

    // ─── Logique ──────────────────────────────────────────────────────────────

    private void loadProgram(String name) {
        cpu.reset();
        stepIndex = 0;
        switch (name) {
            case "Addition simple":      currentProgram = DemoPrograms.addition(cpu);      break;
            case "Opérations logiques":  currentProgram = DemoPrograms.logique(cpu);       break;
            case "Décalages de bits":    currentProgram = DemoPrograms.decalages(cpu);     break;
            case "Multiplication & CMP": currentProgram = DemoPrograms.multiplication(cpu);break;
            case "Appel de fonction":    currentProgram = DemoPrograms.appelFonction(cpu); break;
            default:                     currentProgram = DemoPrograms.addition(cpu);
        }
        instrLabel.setText("—");
        instrLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-text-fill: #8B949E; -fx-font-weight: bold;");
        statusLabel.setText("Programme chargé — appuyez sur Step ou Run All");
        stepCountLabel.setText("Étape 0 / " + currentProgram.size());
        logArea.setText("[CPU] " + name + " chargé — " + currentProgram.size() + " instructions\n");
        updateUI();
        btnStep.setDisable(false);
        btnRun.setDisable(false);
    }

    private void executeStep() {
        if (currentProgram == null || stepIndex >= currentProgram.size()) {
            instrLabel.setText("✓ FIN");
            instrLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-text-fill: #3FB950; -fx-font-weight: bold;");
            statusLabel.setText("Programme terminé.");
            btnStep.setDisable(true);
            return;
        }

        DemoPrograms.Step step = currentProgram.get(stepIndex);
        step.action().run();

        instrLabel.setText(step.instruction());
        instrLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-text-fill: #7EE787; -fx-font-weight: bold;");
        statusLabel.setText("→ " + step.description());
        stepCountLabel.setText("Étape " + (stepIndex + 1) + " / " + currentProgram.size());
        logArea.appendText(cpu.getLog());
        cpu.clearLog();

        stepIndex++;
        updateUI();

        if (stepIndex >= currentProgram.size()) {
            btnStep.setDisable(true);
            instrLabel.setText("✓ TERMINÉ");
        }
    }

    private void runAll() {
        if (currentProgram == null) return;
        cpu.reset();
        stepIndex = 0;
        loadProgram(programSelector.getValue());

        Timeline tl = new Timeline();
        for (int i = 0; i < currentProgram.size(); i++) {
            final int idx = i;
            KeyFrame kf = new KeyFrame(Duration.millis(600 * (idx + 1)), e -> executeStep());
            tl.getKeyFrames().add(kf);
        }
        tl.play();
    }

    private void resetCPU() {
        loadProgram(programSelector.getValue());
    }

    private void updateUI() {
        // R0-R12
        for (int i = 0; i < 13; i++) {
            regLabels[i].setText(String.format("0x%08X", cpu.getReg(i)));
        }
        spLabel.setText(String.format("0x%08X", cpu.getSP()));
        lrLabel.setText(String.format("0x%08X", cpu.getLR()));
        pcLabel.setText(String.format("0x%08X", cpu.getPC()));

        // Flags
        updateFlag(flagN, "N", cpu.isFlagN(), "#FF7B72");
        updateFlag(flagZ, "Z", cpu.isFlagZ(), "#7EE787");
        updateFlag(flagC, "C", cpu.isFlagC(), "#FFA657");
        updateFlag(flagV, "V", cpu.isFlagV(), "#D2A8FF");

        cpsrLabel.setText(String.format("0x%08X", cpu.getCPSR()));
        updateMemory();
    }

    private void updateMemory() {
        int lastAddr = cpu.getLastWrittenAddr();
        for (int row = 0; row < MEM_ROWS; row++) {
            for (int col = 0; col < MEM_COLS; col++) {
                int addr = row * MEM_COLS + col;
                int val = cpu.getMemoryAt(addr);
                boolean written = (addr == lastAddr);
                boolean nonZero = (val != 0 && !written);
                memCells[row][col].setText(String.format("%02X", val & 0xFF));
                memCells[row][col].setStyle(cellStyle(written, nonZero));
            }
        }
        // Animation flash sur la cellule écrite
        if (lastAddr >= 0 && lastAddr < MEM_ROWS * MEM_COLS) {
            int r2 = lastAddr / MEM_COLS;
            int c2 = lastAddr % MEM_COLS;
            Label cell = memCells[r2][c2];
            ScaleTransition st = new ScaleTransition(Duration.millis(200), cell);
            st.setFromX(1.3); st.setFromY(1.3);
            st.setToX(1.0);   st.setToY(1.0);
            st.play();
        }
    }

    private void updateFlag(Label lbl, String letter, boolean active, String color) {
        int val = active ? 1 : 0;
        lbl.setText(letter + " = " + val);
        lbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                + (active ? color : "#30363D") + ";");

        if (active) {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), lbl);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(1.15);  st.setToY(1.15);
            st.setAutoReverse(true); st.setCycleCount(2);
            st.play();
        }
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setPadding(new Insets(12, 14, 10, 14));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E; -fx-font-family: 'Courier New';"
                 + "-fx-background-color: #0D1117; -fx-border-color: #30363D; -fx-border-width: 0 0 1 0;");
        return l;
    }

    private Button buildButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";"
                   + "-fx-font-family: 'Courier New'; -fx-font-size: 12px;"
                   + "-fx-border-color: #30363D; -fx-border-radius: 6; -fx-background-radius: 6;"
                   + "-fx-padding: 7 16 7 16; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
