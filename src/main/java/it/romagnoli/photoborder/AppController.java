package it.romagnoli.photoborder;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class AppController {

    @FXML
    private ImageView imageView;

    @FXML
    private Slider borderSlider;

    @FXML
    private Slider blackBorderSlider;

    @FXML
    private Button saveButton;

    private Image originalImage;
    private WritableImage borderedImage; // immagine con i bordi, usata anche per il salvataggio

    @FXML
    private void initialize() {
        // Imposta i valori iniziali degli slider
        borderSlider.setValue(8); // Bordo bianco iniziale
        blackBorderSlider.setValue(1); // Bordo nero iniziale

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
                imageView.fitHeightProperty().bind(newScene.heightProperty().subtract(50));
            }
        });

        // Listener per il bottone "Salva immagine"
        saveButton.setOnAction(event -> saveImageWithBorders());
    }

    @FXML
    private void handleOpenImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini JPG", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            originalImage = new Image(file.toURI().toString());
            imageView.setImage(originalImage);

            // Applica i bordi iniziali
            applyBorders(borderSlider.getValue(), blackBorderSlider.getValue());
        }
    }

    private void applyBorders(double whiteBorderPercentage, double blackBorderPercentage) {
        try {
            if (originalImage == null) {
                return;
            }

            int width = (int) originalImage.getWidth();
            int height = (int) originalImage.getHeight();

            int whiteBorderSize = (int) (Math.min(width, height) * (whiteBorderPercentage / 100.0));
            int blackBorderSize = (int) (Math.min(width, height) * (blackBorderPercentage / 100.0));

            if (whiteBorderSize < 0 || blackBorderSize < 0 ||
                whiteBorderSize + blackBorderSize > Math.min(width, height) / 2) {
                return;
            }

            int totalWidth = width + (whiteBorderSize + blackBorderSize) * 2;
            int totalHeight = height + (whiteBorderSize + blackBorderSize) * 2;

            Canvas canvas = new Canvas(totalWidth, totalHeight);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // Bordo bianco (esterno)
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, totalWidth, totalHeight);

            // Bordo nero (interno al bianco)
            gc.setFill(Color.BLACK);
            gc.fillRect(
                whiteBorderSize,
                whiteBorderSize,
                totalWidth - whiteBorderSize * 2,
                totalHeight - whiteBorderSize * 2
            );

            // Immagine originale al centro, alle sue dimensioni reali (width x height)
            gc.drawImage(
                originalImage,
                whiteBorderSize + blackBorderSize,
                whiteBorderSize + blackBorderSize,
                width,
                height
            );

            // Snapshot alle dimensioni REALI del canvas (non dello schermo)
            borderedImage = new WritableImage(totalWidth, totalHeight);
            canvas.snapshot(null, borderedImage);

            imageView.setImage(borderedImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveImageWithBorders() {
        if (borderedImage == null) {
            return; // Nessuna immagine con bordi da salvare
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini PNG", "*.png"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                // Salva direttamente l'immagine con i bordi, senza passare da ImageView
                ImageIO.write(SwingFXUtils.fromFXImage(borderedImage, null), "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}