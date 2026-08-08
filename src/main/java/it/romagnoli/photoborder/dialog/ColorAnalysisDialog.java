package it.romagnoli.photoborder.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.collections.ObservableSet;

/**
 * Dialog non modale e ridimensionabile che mostra una GridView di ControlsFX con 9 celle (3x3)
 * rappresentanti i colori dell'immagine campionati nei punti di intersezione della regola dei terzi.
 */
public class ColorAnalysisDialog {

    private static final int ROWS = 3;
    private static final int COLS = 3;
    private static final double SWATCH_SIZE = 60;
    private static final double CELL_WIDTH = 460;
    private static final double CELL_HEIGHT = 70;
    private static final double CELL_SPACING = 2;
    /** Numero di righe visualizzate a colonna singola (una sotto l'altra) nel dialog. */
    private static final int DISPLAY_ROWS = ROWS * COLS;

    /** Frazioni di larghezza/altezza per i 3 punti della regola dei terzi (1/6, 1/2, 5/6). */
    private static final double[] FRACTIONS = { 1.0 / 6.0, 0.5, 5.0 / 6.0 };

    private final GridView<ColorSample> gridView = new GridView<>();
    private final ObservableList<ColorSample> samples = FXCollections.observableArrayList();
    private final Label infoLabel = new Label("Apri un'immagine per analizzare i colori");
    private final Label skinToneWarningLabel = new Label("Toni incarnato non validi");
    private final SimpleIntegerProperty selectedIndex = new SimpleIntegerProperty(-1);
    private final SimpleIntegerProperty hoveredIndex = new SimpleIntegerProperty(-1);
    private final ObservableSet<Integer> invalidSkinTones = FXCollections.observableSet(new HashSet<>());
    private boolean skinToneChecked = false;

    private Dialog<Void> dialog;
    private Runnable onClose;
    private Runnable onReset;
    private Runnable onSkinToneCheckChanged;

    public ColorAnalysisDialog() {
        gridView.setItems(samples);
        gridView.setCellWidth(CELL_WIDTH);
        gridView.setCellHeight(CELL_HEIGHT);
        gridView.setHorizontalCellSpacing(CELL_SPACING);
        gridView.setVerticalCellSpacing(CELL_SPACING);
        gridView.setCellFactory(gv -> new ColorGridCell(selectedIndex, hoveredIndex, invalidSkinTones));
        // Larghezza esatta di una singola colonna: forza la disposizione verticale (1 colonna x 9 righe)
        gridView.setPrefWidth(CELL_WIDTH + CELL_SPACING * 2 + 20);
        gridView.setMaxWidth(CELL_WIDTH + CELL_SPACING * 2 + 20);
        gridView.setPrefHeight(DISPLAY_ROWS * (CELL_HEIGHT + CELL_SPACING) + 10);

        samples.setAll(Collections.nCopies(ROWS * COLS, new ColorSample(null, Color.GRAY)));

        skinToneWarningLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        skinToneWarningLabel.setVisible(false);
        skinToneWarningLabel.setManaged(false);
    }

    /** Callback invocata quando il dialog viene chiuso (per rimuovere i marker dall'immagine). */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /** Callback invocata quando l'utente preme il bottone Reset (per ricentrare i marker sull'immagine). */
    public void setOnReset(Runnable onReset) {
        this.onReset = onReset;
    }

    /** Callback invocata quando cambia l'esito della verifica "Toni incarnato" (per ridisegnare i marker). */
    public void setOnSkinToneCheckChanged(Runnable onSkinToneCheckChanged) {
        this.onSkinToneCheckChanged = onSkinToneCheckChanged;
    }

    /** Indica se la verifica "Toni incarnato" è stata eseguita sulla campionatura corrente. */
    public boolean isSkinToneChecked() {
        return skinToneChecked;
    }

    /** Indica se la cella all'indice indicato non rispetta il criterio dei toni incarnato. */
    public boolean isInvalidSkinTone(int index) {
        return invalidSkinTones.contains(index);
    }

    /**
     * Proprietà osservabile dell'indice della cella attualmente selezionata (-1 se nessuna selezione).
     * Quando cambia, il chiamante può decidere di mostrare solo il marker corrispondente.
     */
    public ReadOnlyIntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }

    public int getSelectedIndex() {
        return selectedIndex.get();
    }

    /** Seleziona direttamente una cella (usato ad es. quando si clicca sul marker sull'immagine). */
    public void selectIndex(int index) {
        selectedIndex.set(index);
    }

    /** Deseleziona la cella corrente, se presente (mostra di nuovo tutti i marker). */
    public void clearSelection() {
        selectedIndex.set(-1);
    }

    /** Imposta la cella attualmente "in hover" (evidenziata) senza modificare la selezione, -1 per nessuna. */
    public void setHoveredIndex(int index) {
        hoveredIndex.set(index);
    }

    public int getHoveredIndex() {
        return hoveredIndex.get();
    }

    /**
     * Aggiorna punto e colore originale di una singola cella (ad es. dopo un click sull'immagine),
     * senza toccare le altre celle.
     */
    public void updateSampleAt(int index, Point2D point, Color color) {
        if (index >= 0 && index < samples.size()) {
            samples.set(index, new ColorSample(point, color));
        }
    }

    /** Mostra il dialog (creandolo alla prima chiamata). */
    public void show() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Analizza colori");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.setResizable(true);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            MenuBar menuBar = createMenuBar();

            Button resetButton = new Button("Reset");
            resetButton.setOnAction(event -> {
                if (onReset != null) {
                    onReset.run();
                }
            });

            HBox bottomBar = new HBox(10, infoLabel, resetButton);
            bottomBar.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoLabel, Priority.ALWAYS);

            VBox content = new VBox(10, menuBar, gridView, skinToneWarningLabel, bottomBar);
            content.setPadding(new Insets(15));
            VBox.setVgrow(gridView, Priority.ALWAYS);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefSize(CELL_WIDTH + 70,
                    DISPLAY_ROWS * (CELL_HEIGHT + CELL_SPACING) + 150);

            dialog.setOnHidden(event -> {
                if (onClose != null) {
                    onClose.run();
                }
            });
        }
        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    /** Crea la barra dei menu con le voci "Verifica colori" -> "Toni incarnato" / "Verifica incarnato caucasico". */
    private MenuBar createMenuBar() {
        MenuItem skinToneItem = new MenuItem("Toni incarnato");
        skinToneItem.setOnAction(event -> checkSkinTones());

        MenuItem caucasianSkinToneItem = new MenuItem("Verifica incarnato caucasico");
        caucasianSkinToneItem.setOnAction(event -> checkCaucasianSkinTones());

        MenuItem latinSkinToneItem = new MenuItem("Verifica incarnato latino");
        latinSkinToneItem.setOnAction(event -> checkLatinSkinTones());

        MenuItem orientalSkinToneItem = new MenuItem("Verifica incarnato orientale");
        orientalSkinToneItem.setOnAction(event -> checkOrientalSkinTones());

        MenuItem africanSkinToneItem = new MenuItem("Verifica incarnato africano");
        africanSkinToneItem.setOnAction(event -> checkAfricanSkinTones());

        Menu verificaColoriMenu = new Menu("Verifica colori");
        verificaColoriMenu.getItems().addAll(skinToneItem, caucasianSkinToneItem, latinSkinToneItem, orientalSkinToneItem, africanSkinToneItem);

        MenuItem vegetationItem = new MenuItem("Verifica vegetazione");
        vegetationItem.setOnAction(event -> checkVegetation());

        Menu verificaVegetazioneMenu = new Menu("Verifica vegetazione");
        verificaVegetazioneMenu.getItems().add(vegetationItem);

        MenuItem skyItem = new MenuItem("Verifica cielo");
        skyItem.setOnAction(event -> checkSky());

        Menu verificaCieloMenu = new Menu("Verifica cielo");
        verificaCieloMenu.getItems().add(skyItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(verificaColoriMenu, verificaVegetazioneMenu, verificaCieloMenu);
        return menuBar;
    }

    /**
     * Verifica per ciascun colore campionato se la terna Lab rispetta il criterio
     * dei toni incarnato (a > 0, b > 0, a >= b). Le celle che non rispettano il
     * criterio vengono marcate ed evidenziate in rosso; viene inoltre mostrata
     * un'etichetta di avviso se almeno una cella non è valida.
     */
    private void checkSkinTones() {
        skinToneWarningLabel.setText("Toni incarnato non validi");
        applySkinToneCheck(ColorAnalysisDialog::isBasicSkinTone);
    }

    /**
     * Verifica per ciascun colore campionato il criterio "incarnato caucasico": oltre alla
     * regola base (a > 0, b > 0, b >= a), impone che il Croma (radice di a^2 + b^2) sia
     * compreso tra 7,6 e 21,6 (esclusi) e che L, a, b rientrino rispettivamente negli
     * intervalli [76, 92], [6, 16] e [5, 15]. Le celle che non rispettano tutti i criteri
     * vengono evidenziate in rosso.
     */
    private void checkCaucasianSkinTones() {
        skinToneWarningLabel.setText("Toni incarnato caucasico non validi");
        applySkinToneCheck(ColorAnalysisDialog::isCaucasianSkinTone);
    }

    /**
     * Verifica per ciascun colore campionato il criterio "incarnato latino": oltre alla
     * regola base (a > 0, b > 0, b >= a), impone che il Croma (radice di a^2 + b^2) sia
     * compreso tra 18,8 e 39,5 (esclusi) e che L, a, b rientrino rispettivamente negli
     * intervalli [60, 86], [10, 24] e [15, 33]. Le celle che non rispettano tutti i criteri
     * vengono evidenziate in rosso.
     */
    private void checkLatinSkinTones() {
        skinToneWarningLabel.setText("Toni incarnato latino non validi");
        applySkinToneCheck(ColorAnalysisDialog::isLatinSkinTone);
    }

    /**
     * Verifica per ciascun colore campionato il criterio "incarnato orientale": oltre alla
     * regola base (a > 0, b > 0, b >= a), impone che il Croma (radice di a^2 + b^2) sia
     * compreso tra 22,2 e 37,3 (esclusi) e che L, a, b rientrino rispettivamente negli
     * intervalli [45, 75], [14, 26] e [16, 28]. Le celle che non rispettano tutti i criteri
     * vengono evidenziate in rosso.
     */
    private void checkOrientalSkinTones() {
        skinToneWarningLabel.setText("Toni incarnato orientale non validi");
        applySkinToneCheck(ColorAnalysisDialog::isOrientalSkinTone);
    }

    /**
     * Verifica per ciascun colore campionato il criterio "incarnato africano": oltre alla
     * regola base (a > 0, b > 0, b >= a), impone che il Croma (radice di a^2 + b^2) sia
     * compreso tra 16 e 40,8 (esclusi) e che L, a, b rientrino rispettivamente negli
     * intervalli [32, 68], [11, 28] e [11, 29]. Le celle che non rispettano tutti i criteri
     * vengono evidenziate in rosso.
     */
    private void checkAfricanSkinTones() {
        skinToneWarningLabel.setText("Toni incarnato africano non validi");
        applySkinToneCheck(ColorAnalysisDialog::isAfricanSkinTone);
    }

    /**
     * Verifica per ciascun colore campionato il criterio "vegetazione": a deve essere
     * negativo, b positivo, e b deve essere compreso tra 1,2 e 3 volte il valore assoluto
     * di a (1,2*|a| &lt;= b &lt;= 3*|a|). Le celle che non rispettano il criterio vengono
     * evidenziate in rosso.
     */
    private void checkVegetation() {
        skinToneWarningLabel.setText("Toni vegetazione non validi");
        applySkinToneCheck(ColorAnalysisDialog::isVegetation);
    }

    /**
     * Verifica per ciascun colore campionato il criterio "cielo": b deve essere negativo
     * e a deve essere compreso tra -5 e 3. Le celle che non rispettano il criterio
     * vengono evidenziate in rosso.
     */
    private void checkSky() {
        skinToneWarningLabel.setText("Toni cielo non validi");
        applySkinToneCheck(ColorAnalysisDialog::isSky);
    }

    /** Applica il predicato di validità fornito a tutte le celle campionate ed evidenzia quelle non valide. */
    private void applySkinToneCheck(java.util.function.Predicate<double[]> validator) {
        Set<Integer> invalid = new HashSet<>();
        for (int i = 0; i < samples.size(); i++) {
            ColorSample sample = samples.get(i);
            if (sample == null || sample.original == null) {
                continue;
            }
            double[] lab = rgbToLab(sample.original);
            if (!validator.test(lab)) {
                invalid.add(i);
            }
        }
        invalidSkinTones.clear();
        invalidSkinTones.addAll(invalid);
        skinToneChecked = true;

        boolean anyInvalid = !invalid.isEmpty();
        skinToneWarningLabel.setVisible(anyInvalid);
        skinToneWarningLabel.setManaged(anyInvalid);

        if (onSkinToneCheckChanged != null) {
            onSkinToneCheckChanged.run();
        }
    }

    /** Regola base dei toni incarnato: a > 0, b > 0, b >= a. */
    private static boolean isBasicSkinTone(double[] lab) {
        double a = lab[1];
        double b = lab[2];
        return a > 0 && b > 0 && b >= a;
    }

    /**
     * Regola "incarnato caucasico": regola base + Croma in (7,6; 21,6) + L in [76, 92],
     * a in [6, 16], b in [5, 15].
     */
    private static boolean isCaucasianSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 7.6 && chroma < 21.6
                && l >= 76 && l <= 92
                && a >= 6 && a <= 16
                && b >= 5 && b <= 15;
    }

    /**
     * Regola "incarnato latino": regola base + Croma in (18,8; 39,5) + L in [60, 86],
     * a in [10, 24], b in [15, 33].
     */
    private static boolean isLatinSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 18.8 && chroma < 39.5
                && l >= 60 && l <= 86
                && a >= 10 && a <= 24
                && b >= 15 && b <= 33;
    }

    /**
     * Regola "incarnato orientale": regola base + Croma in (22,2; 37,3) + L in [45, 75],
     * a in [14, 26], b in [16, 28].
     */
    private static boolean isOrientalSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 22.2 && chroma < 37.3
                && l >= 45 && l <= 75
                && a >= 14 && a <= 26
                && b >= 16 && b <= 28;
    }

    /**
     * Regola "incarnato africano": regola base + Croma in (16; 40,8) + L in [32, 68],
     * a in [11, 28], b in [11, 29].
     */
    private static boolean isAfricanSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 16 && chroma < 40.8
                && l >= 32 && l <= 68
                && a >= 11 && a <= 28
                && b >= 11 && b <= 29;
    }

    /**
     * Regola "vegetazione": a &lt; 0, b &gt; 0, e 1,2*|a| &lt;= b &lt;= 3*|a|.
     */
    private static boolean isVegetation(double[] lab) {
        double a = lab[1];
        double b = lab[2];
        if (!(a < 0 && b > 0)) {
            return false;
        }
        double absA = Math.abs(a);
        return b >= 1.2 * absA && b <= 3 * absA;
    }

    /**
     * Regola "cielo": b &lt; 0, a compreso tra -5 e 3.
     */
    private static boolean isSky(double[] lab) {
        double a = lab[1];
        double b = lab[2];
        return b < 0 && a >= -5 && a <= 3;
    }

    /**
     * Calcola i 9 punti (in coordinate pixel dell'immagine) corrispondenti alle
     * intersezioni della regola dei terzi, ordinati per righe (alto-sinistra -> basso-destra).
     */
    public static List<Point2D> computeSamplePoints(double width, double height) {
        List<Point2D> points = new ArrayList<>(ROWS * COLS);
        for (double fy : FRACTIONS) {
            for (double fx : FRACTIONS) {
                points.add(new Point2D(width * fx, height * fy));
            }
        }
        return points;
    }

    /**
     * Aggiorna la griglia leggendo i colori dell'immagine nei punti della regola dei terzi.
     * Se image è null, mostra dei placeholder grigi. Azzera anche eventuali colori di confronto
     * precedenti, dato che si riferiscono a un'altra immagine di origine.
     *
     * @return la lista dei punti campionati (coordinate pixel dell'immagine originale), utile al
     *         chiamante per disegnare i marker senza alterare l'immagine.
     */
    public List<Point2D> updateColors(Image image) {
        selectedIndex.set(-1); // nuova immagine: azzera l'eventuale selezione precedente
        invalidSkinTones.clear();
        skinToneChecked = false;
        skinToneWarningLabel.setVisible(false);
        skinToneWarningLabel.setManaged(false);

        if (image == null) {
            samples.setAll(Collections.nCopies(ROWS * COLS, new ColorSample(null, Color.GRAY)));
            infoLabel.setText("Apri un'immagine per analizzare i colori");
            return List.of();
        }

        double width = image.getWidth();
        double height = image.getHeight();
        List<Point2D> points = computeSamplePoints(width, height);
        PixelReader reader = image.getPixelReader();

        List<ColorSample> newSamples = new ArrayList<>(points.size());
        for (Point2D p : points) {
            int x = clamp((int) Math.round(p.getX()), 0, (int) width - 1);
            int y = clamp((int) Math.round(p.getY()), 0, (int) height - 1);
            newSamples.add(new ColorSample(p, reader.getColor(x, y)));
        }

        samples.setAll(newSamples);
        infoLabel.setText("Colori campionati sui punti della regola dei terzi ("
                + (int) width + "x" + (int) height + " px)");
        return points;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Converte un colore RGB (componenti 0-1) nello spazio CIE L*a*b* (D65),
     * restituendo un array {L, a, b}.
     */
    private static double[] rgbToLab(Color color) {
        double r = pivotRgb(color.getRed());
        double g = pivotRgb(color.getGreen());
        double b = pivotRgb(color.getBlue());

        // Conversione sRGB lineare -> XYZ (matrice standard, illuminante D65)
        double x = r * 0.4124 + g * 0.3576 + b * 0.1805;
        double y = r * 0.2126 + g * 0.7152 + b * 0.0722;
        double z = r * 0.0193 + g * 0.1192 + b * 0.9505;

        // Bianco di riferimento D65
        double xn = 0.95047;
        double yn = 1.00000;
        double zn = 1.08883;

        double fx = pivotXyz(x / xn);
        double fy = pivotXyz(y / yn);
        double fz = pivotXyz(z / zn);

        double l = 116 * fy - 16;
        double a = 500 * (fx - fy);
        double bLab = 200 * (fy - fz);

        return new double[] { l, a, bLab };
    }

    private static double pivotRgb(double channel) {
        return channel > 0.04045 ? Math.pow((channel + 0.055) / 1.055, 2.4) : channel / 12.92;
    }

    private static double pivotXyz(double t) {
        double delta = 6.0 / 29.0;
        return t > Math.pow(delta, 3) ? Math.cbrt(t) : (t / (3 * delta * delta) + 4.0 / 29.0);
    }

    /**
     * Dati associati a una cella: il punto campionato (coordinate pixel immagine originale)
     * e il colore originale.
     */
    private static class ColorSample {
        final Point2D point;
        final Color original;

        ColorSample(Point2D point, Color original) {
            this.point = point;
            this.original = original;
        }
    }

    /**
     * Cella della GridView: box colorato + etichette RGB/Lab. Gestisce selezione e hover
     * al click/mouse sul box.
     */
    private static class ColorGridCell extends GridCell<ColorSample> {
        private final Region swatch = new Region();
        private final Label rgbLabel = new Label();
        private final Label labLabel = new Label();
        private final HBox root = new HBox(10);
        private final SimpleIntegerProperty selectedIndex;
        private final SimpleIntegerProperty hoveredIndex;
        private final ObservableSet<Integer> invalidSkinTones;

        ColorGridCell(SimpleIntegerProperty selectedIndex, SimpleIntegerProperty hoveredIndex,
                ObservableSet<Integer> invalidSkinTones) {
            this.selectedIndex = selectedIndex;
            this.hoveredIndex = hoveredIndex;
            this.invalidSkinTones = invalidSkinTones;

            swatch.setPrefSize(SWATCH_SIZE, SWATCH_SIZE);
            swatch.setMinSize(SWATCH_SIZE, SWATCH_SIZE);
            swatch.setMaxSize(SWATCH_SIZE, SWATCH_SIZE);

            rgbLabel.setStyle("-fx-font-size: 11px;");
            labLabel.setStyle("-fx-font-size: 11px;");
            VBox labelsBox = new VBox(4, rgbLabel, labLabel);
            labelsBox.setAlignment(Pos.CENTER_LEFT);

            root.setAlignment(Pos.CENTER_LEFT);
            root.getChildren().addAll(swatch, labelsBox);
            setGraphic(root);

            setOnMouseClicked(event -> {
                int index = getIndex();
                if (index >= 0 && index < getGridView().getItems().size()) {
                    // Click sulla cella già selezionata: deseleziona, altrimenti seleziona questa
                    selectedIndex.set(selectedIndex.get() == index ? -1 : index);
                }
            });

            selectedIndex.addListener((obs, oldVal, newVal) -> updateStyle());
            hoveredIndex.addListener((obs, oldVal, newVal) -> updateStyle());
            invalidSkinTones.addListener((javafx.collections.SetChangeListener<Integer>) change -> {
                updateStyle();
                if (getItem() != null) {
                    updateLabels(getItem());
                }
            });
        }

        private void updateStyle() {
            int index = getIndex();
            boolean isSelected = !isEmpty() && index == selectedIndex.get();
            boolean isHovered = !isEmpty() && !isSelected && index == hoveredIndex.get();
            boolean isInvalidSkinTone = !isEmpty() && invalidSkinTones.contains(index);

            String borderColor = isSelected ? "red" : (isHovered ? "dodgerblue" : "black");
            String borderWidth = (isSelected || isHovered) ? "3" : "1";

            ColorSample sample = getItem();
            if (sample != null && sample.original != null) {
                String hex = toHex(sample.original);
                String style = "-fx-background-color: " + hex + "; -fx-border-color: " + borderColor
                        + "; -fx-border-width: " + borderWidth + ";";
                if (isInvalidSkinTone) {
                    style += " -fx-border-color: red; -fx-border-width: 3;";
                }
                swatch.setStyle(style);
            }
        }

        private static String toHex(Color item) {
            return String.format("#%02X%02X%02X",
                    (int) (item.getRed() * 255),
                    (int) (item.getGreen() * 255),
                    (int) (item.getBlue() * 255));
        }

        private void updateLabels(ColorSample sample) {
            if (sample.original != null) {
                int r = (int) Math.round(sample.original.getRed() * 255);
                int g = (int) Math.round(sample.original.getGreen() * 255);
                int b = (int) Math.round(sample.original.getBlue() * 255);
                rgbLabel.setText(String.format("RGB: %d, %d, %d", r, g, b));

                double[] lab = rgbToLab(sample.original);
                labLabel.setText(String.format(Locale.ITALIAN, "Lab: %.1f, %.1f, %.1f", lab[0], lab[1], lab[2]));

                boolean isInvalidSkinTone = !isEmpty() && invalidSkinTones.contains(getIndex());
                labLabel.setStyle(isInvalidSkinTone
                        ? "-fx-font-size: 11px; -fx-text-fill: red; -fx-font-weight: bold;"
                        : "-fx-font-size: 11px;");
            } else {
                rgbLabel.setText("");
                labLabel.setText("");
                labLabel.setStyle("-fx-font-size: 11px;");
            }
        }

        @Override
        protected void updateItem(ColorSample item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                swatch.setStyle("");
                rgbLabel.setText("");
                labLabel.setText("");
            } else {
                updateStyle();
                updateLabels(item);
            }
        }
    }
}
