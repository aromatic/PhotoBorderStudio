package it.romagnoli.photoborder.dialog;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Dialog non modale che mostra l'istogramma dell'immagine caricata, in diverse modalità
 * selezionabili tramite pulsanti: "RGB" (default), che mostra i tre canali di colore insieme,
 * "R", "G", "B", che mostrano il singolo canale selezionato, e "Lab (L)", che mostra la
 * distribuzione della luminosità L* (spazio colore CIE L*a*b*).
 * Il calcolo viene eseguito in un thread separato per non bloccare la UI.
 */
public class HistogramDialog {

    private static final int L_BINS = 101; // L* varia da 0 a 100

    private final Canvas canvas = new Canvas(400, 200);
    private final Label infoLabel = new Label("Apri un'immagine per calcolare l'istogramma");

    private final ToggleButton rgbButton = new ToggleButton("RGB");
    private final ToggleButton logRgbButton = new ToggleButton("Log(RGB)");
    private final ToggleButton rButton = new ToggleButton("R");
    private final ToggleButton gButton = new ToggleButton("G");
    private final ToggleButton bButton = new ToggleButton("B");
    private final ToggleButton labButton = new ToggleButton("Lab (L)");

    private Dialog<Void> dialog;
    private Image currentImage;

    /** Istogrammi calcolati per l'ultima immagine, riutilizzati al cambio modalità. */
    private int[] histR;
    private int[] histG;
    private int[] histB;
    private int[] histL;

    public HistogramDialog() {
        canvas.setStyle("-fx-background-color: black;");

        ToggleGroup modeGroup = new ToggleGroup();
        rgbButton.setToggleGroup(modeGroup);
        logRgbButton.setToggleGroup(modeGroup);
        rButton.setToggleGroup(modeGroup);
        gButton.setToggleGroup(modeGroup);
        bButton.setToggleGroup(modeGroup);
        labButton.setToggleGroup(modeGroup);
        rgbButton.setSelected(true);

        rgbButton.setOnAction(event -> {
            if (rgbButton.isSelected()) {
                redraw();
            }
        });
        logRgbButton.setOnAction(event -> {
            if (logRgbButton.isSelected()) {
                redraw();
            }
        });
        rButton.setOnAction(event -> {
            if (rButton.isSelected()) {
                redraw();
            }
        });
        gButton.setOnAction(event -> {
            if (gButton.isSelected()) {
                redraw();
            }
        });
        bButton.setOnAction(event -> {
            if (bButton.isSelected()) {
                redraw();
            }
        });
        labButton.setOnAction(event -> {
            if (labButton.isSelected()) {
                redraw();
            }
        });
    }

    /** Mostra il dialog (creandolo alla prima chiamata). */
    public void show() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Istogramma");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            HBox buttonsBar = new HBox(10, rgbButton, logRgbButton, rButton, gButton, bButton, labButton);
            buttonsBar.setAlignment(Pos.CENTER);

            VBox content = new VBox(10, buttonsBar, canvas, infoLabel);
            content.setPadding(new Insets(15));
            content.setAlignment(Pos.CENTER);

            dialog.getDialogPane().setContent(content);
        }
        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    /**
     * Aggiorna l'istogramma con la nuova immagine, se il dialog è aperto.
     * Se image è null, pulisce il canvas mostrando un messaggio informativo.
     */
    public void updateImage(Image image) {
        if (!isShowing()) {
            return;
        }
        currentImage = image;
        if (image == null) {
            histR = histG = histB = histL = null;
            clearCanvas();
            infoLabel.setText("Apri un'immagine per calcolare l'istogramma");
            return;
        }
        computeAndDraw(image);
    }

    private void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /** Ridisegna l'istogramma già calcolato secondo la modalità attualmente selezionata. */
    private void redraw() {
        if (histR == null || histL == null || currentImage == null) {
            return;
        }
        String dimensions = (int) currentImage.getWidth() + "x" + (int) currentImage.getHeight() + " px";
        if (labButton.isSelected()) {
            drawLabHistogram(histL);
            infoLabel.setText("Istogramma luminosità L (Lab) (" + dimensions + ")");
        } else if (logRgbButton.isSelected()) {
            drawRgbHistogram(histR, histG, histB, true);
            infoLabel.setText("Istogramma RGB (scala logaritmica) (" + dimensions + ")");
        } else if (rButton.isSelected()) {
            drawSingleChannelHistogram(histR, Color.rgb(255, 60, 60, 0.95));
            infoLabel.setText("Istogramma canale Rosso (" + dimensions + ")");
        } else if (gButton.isSelected()) {
            drawSingleChannelHistogram(histG, Color.rgb(60, 255, 60, 0.95));
            infoLabel.setText("Istogramma canale Verde (" + dimensions + ")");
        } else if (bButton.isSelected()) {
            drawSingleChannelHistogram(histB, Color.rgb(60, 60, 255, 0.95));
            infoLabel.setText("Istogramma canale Blu (" + dimensions + ")");
        } else {
            drawRgbHistogram(histR, histG, histB, false);
            infoLabel.setText("Istogramma RGB (" + dimensions + ")");
        }
    }

    private void computeAndDraw(Image image) {
        infoLabel.setText("Calcolo istogramma in corso...");

        Task<int[][]> task = new Task<>() {
            @Override
            protected int[][] call() {
                int width = (int) image.getWidth();
                int height = (int) image.getHeight();
                PixelReader reader = image.getPixelReader();

                int[] histR = new int[256];
                int[] histG = new int[256];
                int[] histB = new int[256];
                int[] histL = new int[L_BINS];

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = reader.getArgb(x, y);
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        histR[r]++;
                        histG[g]++;
                        histB[b]++;

                        double l = rgbToL(r / 255.0, g / 255.0, b / 255.0);
                        int lBin = clamp((int) Math.round(l), 0, L_BINS - 1);
                        histL[lBin]++;
                    }
                }

                return new int[][] { histR, histG, histB, histL };
            }
        };

        task.setOnSucceeded(event -> {
            int[][] result = task.getValue();
            histR = result[0];
            histG = result[1];
            histB = result[2];
            histL = result[3];
            redraw();
        });

        task.setOnFailed(event -> infoLabel.setText("Errore nel calcolo dell'istogramma"));

        Thread thread = new Thread(task, "histogram-calc");
        thread.setDaemon(true);
        thread.start();
    }

    private void drawRgbHistogram(int[] histR, int[] histG, int[] histB, boolean useLogScale) {
        int max = 1;
        for (int i = 0; i < 256; i++) {
            max = Math.max(max, Math.max(histR[i], Math.max(histG[i], histB[i])));
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        gc.setGlobalBlendMode(BlendMode.SCREEN);
        drawChannel(gc, histR, max, Color.rgb(255, 60, 60, 0.85), useLogScale);
        drawChannel(gc, histG, max, Color.rgb(60, 255, 60, 0.85), useLogScale);
        drawChannel(gc, histB, max, Color.rgb(60, 60, 255, 0.85), useLogScale);
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    /** Disegna l'istogramma della sola luminosità L* (spazio Lab), in scala di grigi/bianco. */
    private void drawLabHistogram(int[] histL) {
        int max = 1;
        for (int value : histL) {
            max = Math.max(max, value);
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        drawChannel(gc, histL, max, Color.rgb(230, 230, 230, 0.95), false);
    }

    /** Disegna l'istogramma di un singolo canale colore (R, G o B) con il proprio colore pieno. */
    private void drawSingleChannelHistogram(int[] hist, Color color) {
        int max = 1;
        for (int value : hist) {
            max = Math.max(max, value);
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        drawChannel(gc, hist, max, color, false);
    }

    private void drawChannel(GraphicsContext gc, int[] hist, int max, Color color, boolean useLogScale) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        int bins = hist.length;
        double stepX = w / bins;

        double logMax = Math.log1p(max);

        double[] xs = new double[bins + 2];
        double[] ys = new double[bins + 2];
        xs[0] = 0;
        ys[0] = h;
        for (int i = 0; i < bins; i++) {
            double value = useLogScale ? Math.log1p(hist[i]) / logMax : hist[i] / (double) max;
            xs[i + 1] = i * stepX;
            ys[i + 1] = h - value * h;
        }
        xs[bins + 1] = bins * stepX;
        ys[bins + 1] = h;

        gc.setFill(color);
        gc.fillPolygon(xs, ys, bins + 2);
    }

    /**
     * Converte una componente RGB (0-1) nella sola luminosità L* dello spazio CIE L*a*b*
     * (illuminante D65), restituendo un valore compreso tra 0 e 100.
     */
    private static double rgbToL(double r, double g, double b) {
        double rl = pivotRgb(r);
        double gl = pivotRgb(g);
        double bl = pivotRgb(b);

        // Luminanza relativa Y (illuminante D65)
        double y = rl * 0.2126 + gl * 0.7152 + bl * 0.0722;
        double yn = 1.00000;

        double fy = pivotXyz(y / yn);
        return 116 * fy - 16;
    }

    private static double pivotRgb(double channel) {
        return channel > 0.04045 ? Math.pow((channel + 0.055) / 1.055, 2.4) : channel / 12.92;
    }

    private static double pivotXyz(double t) {
        double delta = 6.0 / 29.0;
        return t > Math.pow(delta, 3) ? Math.cbrt(t) : (t / (3 * delta * delta) + 4.0 / 29.0);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
