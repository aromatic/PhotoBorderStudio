package it.romagnoli.photoborder;

import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.FileChooser;

import java.io.File;

public class AppController {

    @FXML
    private ImageView imageView;

    @FXML
    private Slider borderSlider;

    @FXML
    private Slider blackBorderSlider;

    private Image originalImage;

    @FXML
    private void initialize() {
        // Listener per lo slider del bordo bianco
        borderSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                applyBorders(borderSlider.getValue(), blackBorderSlider.getValue());
            }
        });

        // Listener per lo slider del bordo nero
        blackBorderSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                applyBorders(borderSlider.getValue(), blackBorderSlider.getValue());
            }
        });

        // Aggiungi un listener per verificare quando la scena è pronta
        imageView.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                imageView.fitWidthProperty().bind(newScene.widthProperty());
                imageView.fitHeightProperty().bind(newScene.heightProperty());
            }
        });
    }

    @FXML
    private void handleOpenImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini JPG", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            originalImage = new Image(file.toURI().toString());
            imageView.setImage(originalImage);
        }
    }

    private void applyBorders(double whiteBorderPercentage, double blackBorderPercentage) {
        try {
            if (originalImage == null) {
                return; // Evita di eseguire il metodo se l'immagine originale non è caricata
            }
    
            int width = (int) originalImage.getWidth();
            int height = (int) originalImage.getHeight();
    
            // Calcola i bordi come percentuali delle dimensioni dell'immagine
            int whiteBorderSize = (int) (Math.min(width, height) * (whiteBorderPercentage / 100.0));
            int blackBorderSize = (int) (Math.min(width, height) * (blackBorderPercentage / 100.0));
    
            // Verifica che i bordi siano validi
            if (whiteBorderSize < 0 || blackBorderSize < 0 || 
                whiteBorderSize + blackBorderSize > Math.min(width, height) / 2) {
                return; // Evita di creare bordi con dimensioni non valide
            }
    
            WritableImage borderedImage = new WritableImage(
                width + (whiteBorderSize + blackBorderSize) * 2,
                height + (whiteBorderSize + blackBorderSize) * 2
            );
            Canvas canvas = new Canvas(borderedImage.getWidth(), borderedImage.getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();
    
            // Disegna il bordo bianco
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, borderedImage.getWidth(), borderedImage.getHeight());
    
            // Disegna il bordo nero
            gc.setFill(Color.BLACK);
            gc.fillRect(
                whiteBorderSize,
                whiteBorderSize,
                borderedImage.getWidth() - whiteBorderSize * 2,
                borderedImage.getHeight() - whiteBorderSize * 2
            );
    
            // Disegna l'immagine originale
            gc.drawImage(originalImage, whiteBorderSize + blackBorderSize, whiteBorderSize + blackBorderSize);
    
            // Imposta l'immagine con i bordi nell'ImageView
            imageView.setImage(canvas.snapshot(null, borderedImage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}