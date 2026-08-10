package it.romagnoli.photoborder.utils;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;

public interface CanvasBorderedImage {
    Canvas getMarkerCanvas();
    int getCurrentBorderOffset();
    ImageView getImageView();
}
