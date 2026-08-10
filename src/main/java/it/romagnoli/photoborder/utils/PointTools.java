package it.romagnoli.photoborder.utils;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.canvas.Canvas;

public class PointTools {
    
        /**
     * Calcola il fattore di scala tra l'immagine effettivamente mostrata e l'area disponibile,
     * oppure -1 se non calcolabile (nessuna immagine o area non ancora dimensionata).
     */
    public static double computeDisplayScale(ImageView imageView, Canvas markerCanvas) {
        Image displayedImage = imageView.getImage();
        if (displayedImage == null) {
            return -1;
        }
        double imgWidth = displayedImage.getWidth();
        double imgHeight = displayedImage.getHeight();
        double canvasWidth = markerCanvas.getWidth();
        double canvasHeight = markerCanvas.getHeight();
        if (imgWidth <= 0 || imgHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            return -1;
        }
        return Math.min(canvasWidth / imgWidth, canvasHeight / imgHeight);
    }

    /**
     * Converte un punto in coordinate immagine originale nelle coordinate locali dell'ImageView
     * (stesso sistema di riferimento degli eventi mouse su imageView, senza offset di centratura).
     */
    public static Point2D toImageViewLocalCoordinates(Point2D originalPoint, double scale, int currentBorderOffset) {
        return new Point2D(
            (originalPoint.getX() + currentBorderOffset) * scale,
            (originalPoint.getY() + currentBorderOffset) * scale
        );
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
