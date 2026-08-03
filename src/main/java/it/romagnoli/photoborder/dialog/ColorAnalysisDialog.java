package it.romagnoli.photoborder.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Dialog non modale e ridimensionabile che mostra una GridView di ControlsFX con 9 celle (3x3)
 * rappresentanti i colori dell'immagine campionati nei punti di intersezione della regola dei terzi.
 */
public class ColorAnalysisDialog {

    private static final int ROWS = 3;
    private static final int COLS = 3;
    private static final double SWATCH_SIZE = 60;
    private static final double CELL_WIDTH = 210;
    private static final double CELL_HEIGHT = 80;

    /** Frazioni di larghezza/altezza per i 3 punti della regola dei terzi (1/6, 1/2, 5/6). */
    private static final double[] FRACTIONS = { 1.0 / 6.0, 0.5, 5.0 / 6.0 };

    private final GridView<Color> gridView = new GridView<>();
    private final ObservableList<Color> colors = FXCollections.observableArrayList();
    private final Label infoLabel = new Label("Apri un'immagine per analizzare i colori");
    private final SimpleIntegerProperty selectedIndex = new SimpleIntegerProperty(-1);
    private final SimpleIntegerProperty hoveredIndex = new SimpleIntegerProperty(-1);

    private Dialog<Void> dialog;
    private Runnable onClose;

    public ColorAnalysisDialog() {
        gridView.setItems(colors);
        gridView.setCellWidth(CELL_WIDTH);
        gridView.setCellHeight(CELL_HEIGHT);
        gridView.setHorizontalCellSpacing(6);
        gridView.setVerticalCellSpacing(6);
        gridView.setCellFactory(gv -> new ColorGridCell(selectedIndex, hoveredIndex));
        gridView.setPrefSize(COLS * (CELL_WIDTH + 6) + 10, ROWS * (CELL_HEIGHT + 6) + 10);

        colors.setAll(Collections.nCopies(ROWS * COLS, Color.GRAY));
    }

    /** Callback invocata quando il dialog viene chiuso (per rimuovere i marker dall'immagine). */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
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

    /** Aggiorna il colore di una singola cella (ad es. dopo un click sull'immagine) senza toccare le altre. */
    public void updateColorAt(int index, Color color) {
        if (index >= 0 && index < colors.size()) {
            colors.set(index, color);
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

            VBox content = new VBox(10, gridView, infoLabel);
            content.setPadding(new Insets(15));
            VBox.setVgrow(gridView, Priority.ALWAYS);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefSize(COLS * (CELL_WIDTH + 6) + 40, ROWS * (CELL_HEIGHT + 6) + 100);

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
     * Se image è null, mostra dei placeholder grigi.
     *
     * @return la lista dei punti campionati (coordinate pixel dell'immagine originale), utile al
     *         chiamante per disegnare i marker senza alterare l'immagine.
     */
    public List<Point2D> updateColors(Image image) {
        selectedIndex.set(-1); // nuova immagine: azzera l'eventuale selezione precedente

        if (image == null) {
            colors.setAll(Collections.nCopies(ROWS * COLS, Color.GRAY));
            infoLabel.setText("Apri un'immagine per analizzare i colori");
            return List.of();
        }

        double width = image.getWidth();
        double height = image.getHeight();
        List<Point2D> points = computeSamplePoints(width, height);
        PixelReader reader = image.getPixelReader();

        List<Color> sampled = new ArrayList<>(points.size());
        for (Point2D p : points) {
            int x = clamp((int) Math.round(p.getX()), 0, (int) width - 1);
            int y = clamp((int) Math.round(p.getY()), 0, (int) height - 1);
            sampled.add(reader.getColor(x, y));
        }

        colors.setAll(sampled);
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

    /** Cella della GridView: box colorato + pannello con le label RGB e Lab, gestisce selezione e hover al click/mouse. */
    private static class ColorGridCell extends GridCell<Color> {
        private final StackPane swatch = new StackPane();
        private final Label rgbLabel = new Label();
        private final Label labLabel = new Label();
        private final HBox root = new HBox(10);
        private final SimpleIntegerProperty selectedIndex;
        private final SimpleIntegerProperty hoveredIndex;

        ColorGridCell(SimpleIntegerProperty selectedIndex, SimpleIntegerProperty hoveredIndex) {
            this.selectedIndex = selectedIndex;
            this.hoveredIndex = hoveredIndex;
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
        }

        private void updateStyle() {
            int index = getIndex();
            boolean isSelected = !isEmpty() && index == selectedIndex.get();
            boolean isHovered = !isEmpty() && !isSelected && index == hoveredIndex.get();

            String borderColor = isSelected ? "red" : (isHovered ? "dodgerblue" : "black");
            String borderWidth = (isSelected || isHovered) ? "3" : "1";

            Color item = getItem();
            if (item != null) {
                String hex = String.format("#%02X%02X%02X",
                        (int) (item.getRed() * 255),
                        (int) (item.getGreen() * 255),
                        (int) (item.getBlue() * 255));
                swatch.setStyle("-fx-background-color: " + hex + "; -fx-border-color: " + borderColor
                        + "; -fx-border-width: " + borderWidth + ";");
            }
        }

        private void updateLabels(Color item) {
            int r = (int) Math.round(item.getRed() * 255);
            int g = (int) Math.round(item.getGreen() * 255);
            int b = (int) Math.round(item.getBlue() * 255);
            rgbLabel.setText(String.format("RGB: %d, %d, %d", r, g, b));

            double[] lab = rgbToLab(item);
            labLabel.setText(String.format(Locale.ITALIAN, "Lab: %.1f, %.1f, %.1f", lab[0], lab[1], lab[2]));
        }

        @Override
        protected void updateItem(Color item, boolean empty) {
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
