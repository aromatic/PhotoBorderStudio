package it.romagnoli.photoborder.view;

import javafx.beans.property.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ZoneSystemView extends StackPane {

    private final ImageView imageView = new ImageView();
    private final Canvas overlayCanvas = new Canvas();

    // Proprietà reattive per controllare la visualizzazione non distruttiva
    private final IntegerProperty targetZone = new SimpleIntegerProperty(5);
    private final DoubleProperty zoneRadius = new SimpleDoubleProperty(1.0);
    private final BooleanProperty overlayEnabled = new SimpleBooleanProperty(true);

    public ZoneSystemView() {
        getChildren().addAll(imageView, overlayCanvas);
        
        // Ridimensiona il canvas di overlay quando l'immagine o il contenitore cambia dimensione
        imageView.imageProperty().addListener((obs, oldImg, newImg) -> {
            if (newImg != null) {
                overlayCanvas.setWidth(newImg.getWidth());
                overlayCanvas.setHeight(newImg.getHeight());
                renderOverlay();
            }
        });

        // Ridisegna automaticamente al variare dei parametri
        targetZone.addListener((obs, o, n) -> renderOverlay());
        zoneRadius.addListener((obs, o, n) -> renderOverlay());
        overlayEnabled.addListener((obs, o, n) -> renderOverlay());
    }

    /**
     * Imposta l'immagine originale SENZA mai modificarla.
     */
    public void setImage(Image image) {
        imageView.setImage(image);
    }

    public Image getImage() {
        return imageView.getImage();
    }

    // --- Getters e Setters per i controlli non distruttivi ---

    public int getTargetZone() { return targetZone.get(); }
    public void setTargetZone(int zone) { this.targetZone.set(zone); }
    public IntegerProperty targetZoneProperty() { return targetZone; }

    public double getZoneRadius() { return zoneRadius.get(); }
    public void setZoneRadius(double radius) { this.zoneRadius.set(radius); }
    public DoubleProperty zoneRadiusProperty() { return zoneRadius; }

    public boolean isOverlayEnabled() { return overlayEnabled.get(); }
    public void setOverlayEnabled(boolean enabled) { this.overlayEnabled.set(enabled); }
    public BooleanProperty overlayEnabledProperty() { return overlayEnabled; }


    /**
     * Disegna l'analisi del Sistema Zonale in sovrapposizione trasparente (Overlay).
     */
    private void renderOverlay() {
        Image img = imageView.getImage();
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();

        // Se l'overlay è disattivato o non c'è immagine, pulisci il canvas e mostra la foto originale intatta
        if (!isOverlayEnabled() || img == null) {
            gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
            return;
        }

        int width = (int) img.getWidth();
        int height = (int) img.getHeight();

        PixelReader reader = img.getPixelReader();
        var pixelWriter = gc.getPixelWriter();

        double zoneStep = 100.0 / 11.0;
        double targetL = getTargetZone() * zoneStep + (zoneStep / 2.0);
        double maxDeltaL = getZoneRadius() * zoneStep;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);

                // Conversione RGB -> L* (CIELAB)
                double L = rgbToLabL(color.getRed(), color.getGreen(), color.getBlue());
                double deltaL = Math.abs(L - targetL);

                if (deltaL <= maxDeltaL) {
                    // Calcola l'opacità del giallo in base alla distanza dal centro della zona
                    double alpha = 1.0 - (deltaL / maxDeltaL);
                    
                    // Disegna solo il layer di evidenziazione giallo con il canale Alpha (trasparenza)
                    pixelWriter.setColor(x, y, new Color(1.0, 1.0, 0.0, alpha * 0.75));
                } else {
                    // Trasparenza totale per i pixel fuori zona
                    pixelWriter.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
    }

    private static double rgbToLabL(double r, double g, double b) {
        r = (r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : (r / 12.92);
        g = (g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : (g / 12.92);
        b = (b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : (b / 12.92);

        double y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750;
        double fy = (y > 0.008856) ? Math.cbrt(y) : (7.787 * y + 16.0 / 116.0);

        return (116.0 * fy) - 16.0;
    }
}
