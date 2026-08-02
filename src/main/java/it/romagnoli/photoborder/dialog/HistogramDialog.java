package it.romagnoli.photoborder.dialog;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Dialog non modale che mostra l'istogramma RGB dell'immagine caricata.
 * Il calcolo viene eseguito in un thread separato per non bloccare la UI.
 */
public class HistogramDialog {

    private final Canvas canvas = new Canvas(400, 200);
    private final Label infoLabel = new Label("Apri un'immagine per calcolare l'istogramma");

    private Dialog<Void> dialog;

    public HistogramDialog() {
        canvas.setStyle("-fx-background-color: black;");
    }

    /** Mostra il dialog (creandolo alla prima chiamata). */
    public void show() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Istogramma");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            VBox content = new VBox(10, canvas, infoLabel);
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
        if (image == null) {
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

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = reader.getArgb(x, y);
                        histR[(argb >> 16) & 0xFF]++;
                        histG[(argb >> 8) & 0xFF]++;
                        histB[argb & 0xFF]++;
                    }
                }

                return new int[][] { histR, histG, histB };
            }
        };

        task.setOnSucceeded(event -> {
            int[][] result = task.getValue();
            drawHistogram(result[0], result[1], result[2]);
            infoLabel.setText("Istogramma RGB (" + (int) image.getWidth() + "x" + (int) image.getHeight() + " px)");
        });

        task.setOnFailed(event -> infoLabel.setText("Errore nel calcolo dell'istogramma"));

        Thread thread = new Thread(task, "histogram-calc");
        thread.setDaemon(true);
        thread.start();
    }

    private void drawHistogram(int[] histR, int[] histG, int[] histB) {
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
        drawChannel(gc, histR, max, Color.rgb(255, 60, 60, 0.85));
        drawChannel(gc, histG, max, Color.rgb(60, 255, 60, 0.85));
        drawChannel(gc, histB, max, Color.rgb(60, 60, 255, 0.85));
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    private void drawChannel(GraphicsContext gc, int[] hist, int max, Color color) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double stepX = w / 256.0;

        double[] xs = new double[258];
        double[] ys = new double[258];
        xs[0] = 0;
        ys[0] = h;
        for (int i = 0; i < 256; i++) {
            xs[i + 1] = i * stepX;
            ys[i + 1] = h - (hist[i] / (double) max) * h;
        }
        xs[257] = 256 * stepX;
        ys[257] = h;

        gc.setFill(color);
        gc.fillPolygon(xs, ys, 258);
    }
}
