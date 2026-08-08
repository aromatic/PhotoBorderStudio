package it.romagnoli.photoborder.dialog;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Dialog non modale che mostra la distribuzione dei valori Hue/Saturation (H, S) dello spazio
 * colore HSV dell'immagine caricata, calcolati a partire dai pixel RGB. I valori vengono
 * rappresentati su un piano polare: l'angolo corrisponde alla tonalità H (0-360°) e la distanza
 * dal centro corrisponde alla saturazione S (espressa in percentuale, 0-100%, scalata su un
 * cerchio di raggio logico 4). Lungo la circonferenza esterna sono indicate le tonalità di
 * riferimento: 0° rosso, 60° giallo, 120° verde, 180° ciano, 240° blu, 300° magenta.
 * Il calcolo viene eseguito in un thread separato per non bloccare la UI.
 */
public class HueSaturationDialog {

    /** Raggio logico del cerchio: la saturazione 100% viene mappata a questo raggio. */
    private static final double LOGICAL_RADIUS = 4.0;

    private static final int H_BINS = 360; // un bin per grado di tonalità
    private static final int S_BINS = 101; // saturazione da 0 a 100 (%)

    private static final int CANVAS_SIZE = 420;
    private static final double MARGIN = 40;
    private static final double CANVAS_RADIUS = CANVAS_SIZE / 2.0 - MARGIN;
    private static final double PIXELS_PER_UNIT = CANVAS_RADIUS / LOGICAL_RADIUS;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final Label infoLabel = new Label("Apri un'immagine per calcolare la distribuzione Hue/Saturation");

    private Dialog<Void> dialog;

    public HueSaturationDialog() {
        canvas.setStyle("-fx-background-color: black;");
    }

    /** Mostra il dialog (creandolo alla prima chiamata). */
    public void show() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Hue / Saturation");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            StackPane canvasBox = new StackPane(canvas);

            VBox content = new VBox(10, canvasBox, infoLabel);
            content.setPadding(new Insets(15));
            content.setAlignment(Pos.CENTER);

            dialog.getDialogPane().setContent(content);
            drawWheelBackground();
        }
        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    /**
     * Aggiorna la distribuzione Hue/Saturation con la nuova immagine, se il dialog è aperto.
     * Se image è null, pulisce il canvas mostrando un messaggio informativo.
     */
    public void updateImage(Image image) {
        if (!isShowing()) {
            return;
        }
        if (image == null) {
            drawWheelBackground();
            infoLabel.setText("Apri un'immagine per calcolare la distribuzione Hue/Saturation");
            return;
        }
        computeAndDraw(image);
    }

    private void computeAndDraw(Image image) {
        infoLabel.setText("Calcolo distribuzione Hue/Saturation in corso...");

        Task<int[][]> task = new Task<>() {
            @Override
            protected int[][] call() {
                int width = (int) image.getWidth();
                int height = (int) image.getHeight();
                PixelReader reader = image.getPixelReader();

                int[][] counts = new int[H_BINS][S_BINS];

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Color color = reader.getColor(x, y);
                        double hue = color.getHue();               // 0-360
                        double saturation = color.getSaturation();  // 0-1

                        int hBin = clamp((int) Math.round(hue), 0, H_BINS - 1);
                        int sBin = clamp((int) Math.round(saturation * 100), 0, S_BINS - 1);
                        counts[hBin][sBin]++;
                    }
                }

                return counts;
            }
        };

        task.setOnSucceeded(event -> {
            int[][] counts = task.getValue();
            drawDistribution(counts);
            infoLabel.setText("Distribuzione Hue/Saturation ("
                    + (int) image.getWidth() + "x" + (int) image.getHeight() + " px)");
        });

        task.setOnFailed(event -> infoLabel.setText("Errore nel calcolo della distribuzione Hue/Saturation"));

        Thread thread = new Thread(task, "hue-saturation-calc");
        thread.setDaemon(true);
        thread.start();
    }

    /** Disegna solo la ruota di riferimento (assi, cerchio e marcatori di tonalità), senza dati. */
    private void drawWheelBackground() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        drawWheelGuides(gc);
    }

    /**
     * Disegna sul piano polare la distribuzione H/S calcolata: ogni bin (H, S) diventa un punto
     * colorato (colore pieno a quella tonalità/saturazione) con luminosità proporzionale alla
     * densità di campioni (scala logaritmica), sovrapposto alla ruota di riferimento.
     */
    private void drawDistribution(int[][] counts) {
        int max = 1;
        for (int[] row : counts) {
            for (int value : row) {
                max = Math.max(max, value);
            }
        }
        double logMax = Math.log1p(max);

        WritableImage image = new WritableImage(CANVAS_SIZE, CANVAS_SIZE);
        PixelWriter writer = image.getPixelWriter();
        double cx = CANVAS_SIZE / 2.0;
        double cy = CANVAS_SIZE / 2.0;

        for (int hBin = 0; hBin < H_BINS; hBin++) {
            for (int sBin = 0; sBin < S_BINS; sBin++) {
                int count = counts[hBin][sBin];
                if (count == 0) {
                    continue;
                }
                double saturation = sBin / 100.0;
                double radiusLogical = saturation * LOGICAL_RADIUS;
                double radiusPixels = radiusLogical * PIXELS_PER_UNIT;
                double theta = Math.toRadians(hBin);

                double x = cx + radiusPixels * Math.cos(theta);
                double y = cy - radiusPixels * Math.sin(theta);

                double intensity = Math.log1p(count) / logMax;
                Color pointColor = Color.hsb(hBin, saturation, Math.max(0.35, intensity));

                plotPoint(writer, x, y, pointColor);
            }
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        gc.drawImage(image, 0, 0);
        drawWheelGuides(gc);
    }

    /** Disegna un piccolo punto (2x2 px) nell'immagine, ignorando le coordinate fuori bordo. */
    private void plotPoint(PixelWriter writer, double x, double y, Color color) {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                int px = ix + dx;
                int py = iy + dy;
                if (px >= 0 && px < CANVAS_SIZE && py >= 0 && py < CANVAS_SIZE) {
                    writer.setColor(px, py, color);
                }
            }
        }
    }

    /**
     * Disegna la circonferenza di riferimento (raggio logico {@link #LOGICAL_RADIUS}, pari al
     * 100% di saturazione) e i marcatori delle tonalità principali: 0° rosso, 60° giallo,
     * 120° verde, 180° ciano, 240° blu, 300° magenta.
     */
    private void drawWheelGuides(GraphicsContext gc) {
        double cx = CANVAS_SIZE / 2.0;
        double cy = CANVAS_SIZE / 2.0;

        // Cerchio esterno (100% di saturazione)
        gc.setStroke(Color.rgb(180, 180, 180));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - CANVAS_RADIUS, cy - CANVAS_RADIUS, CANVAS_RADIUS * 2, CANVAS_RADIUS * 2);

        // Assi di riferimento
        gc.setLineWidth(0.75);
        gc.strokeLine(cx - CANVAS_RADIUS, cy, cx + CANVAS_RADIUS, cy);
        gc.strokeLine(cx, cy - CANVAS_RADIUS, cx, cy + CANVAS_RADIUS);

        // Marcatori delle tonalità principali sulla circonferenza
        int[] hues = { 0, 60, 120, 180, 240, 300 };
        String[] labels = { "0° R", "60° Y", "120° G", "180° C", "240° B", "300° M" };

        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        double labelRadius = CANVAS_RADIUS + 18;

        for (int i = 0; i < hues.length; i++) {
            double theta = Math.toRadians(hues[i]);
            Color hueColor = Color.hsb(hues[i], 1.0, 1.0);

            double mx = cx + CANVAS_RADIUS * Math.cos(theta);
            double my = cy - CANVAS_RADIUS * Math.sin(theta);
            gc.setFill(hueColor);
            gc.fillOval(mx - 5, my - 5, 10, 10);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(mx - 5, my - 5, 10, 10);

            double lx = cx + labelRadius * Math.cos(theta);
            double ly = cy - labelRadius * Math.sin(theta);
            gc.setFill(Color.WHITE);
            gc.fillText(labels[i], lx - 18, ly + 4);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
