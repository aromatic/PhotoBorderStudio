package it.romagnoli.photoborder;

import it.romagnoli.photoborder.dialog.BorderDialog;
import it.romagnoli.photoborder.dialog.CopyrightDialog;
import it.romagnoli.photoborder.dialog.HistogramDialog;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
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
    private MenuItem saveMenuItem;

    private final BorderDialog borderDialog = new BorderDialog();
    private final CopyrightDialog copyrightDialog = new CopyrightDialog();
    private final HistogramDialog histogramDialog = new HistogramDialog();

    private Image originalImage;
    private WritableImage borderedImage; // immagine con i bordi, usata anche per il salvataggio

    @FXML
    private void initialize() {
        // I dialog notificano il controller quando i loro valori cambiano
        borderDialog.setOnChange(() -> {
            if (originalImage != null) {
                updateBorders();
            }
        });

        copyrightDialog.setOnChange(() -> {
            if (originalImage != null) {
                updateBorders();
            }
        });

        // Aggiungi un listener per verificare quando la scena è pronta
        imageView.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                imageView.fitWidthProperty().bind(newScene.widthProperty());
                imageView.fitHeightProperty().bind(newScene.heightProperty().subtract(40));
            }
        });

        // Listener per il salvataggio immagine
        saveMenuItem.setOnAction(event -> saveImageWithBorders());
    }

    private void updateBorders() {
        double whiteBorder = borderDialog.getWhiteBorderValue();
        double blackBorder = borderDialog.getBlackBorderValue();
        applyBorders(whiteBorder, blackBorder);
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
            updateBorders();
            histogramDialog.updateImage(originalImage);
        }
    }

    @FXML
    private void handleExit() {
        javafx.application.Platform.exit();
    }

    @FXML
    private void handleShowBorderDialog() {
        borderDialog.show();
    }

    @FXML
    private void handleShowCopyrightDialog() {
        copyrightDialog.show();
    }

    @FXML
    private void handleShowHistogramDialog() {
        histogramDialog.show();
        histogramDialog.updateImage(originalImage);
    }

    private void applyBorders(double whiteBorderPixels, double blackBorderPixels) {
        try {
            if (originalImage == null) {
                return;
            }

            int width = (int) originalImage.getWidth();
            int height = (int) originalImage.getHeight();

            int whiteBorderSize = (int) whiteBorderPixels;
            int blackBorderSize = (int) blackBorderPixels;

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

            // Aggiungi il testo del copyright se presente
            String copyrightText = copyrightDialog.getCopyrightText();
            if (copyrightText != null && !copyrightText.trim().isEmpty()) {
                gc.setFill(Color.BLACK);
                String selectedFont = copyrightDialog.getSelectedFont();

                // Valore scala 1-10 preso dal dialog
                int fontScaleValue = copyrightDialog.getFontScaleValue();

                // Converti da scala 1-10 a fattore 0.1-0.6
                double scaleFactor = 0.1 + (fontScaleValue - 1) * (0.5 / 9.0);

                // Calcola la dimensione del font proporzionata al bordo bianco
                int fontSize = Math.max((int) (whiteBorderSize * scaleFactor), 12);
                javafx.scene.text.Font font = new javafx.scene.text.Font(selectedFont, fontSize);
                gc.setFont(font);

                // Aggiungi il carattere del copyright al testo
                String fullCopyrightText = "© " + copyrightText;

                // Misura la larghezza del testo usando Text node per calcolo preciso
                javafx.scene.text.Text textNode = new javafx.scene.text.Text(fullCopyrightText);
                textNode.setFont(font);
                javafx.geometry.Bounds textBounds = textNode.getLayoutBounds();
                double textWidth = textBounds.getWidth();

                // Posiziona il copyright in basso a destra nel bordo bianco con margine
                double marginRight = 15;
                double marginBottom = whiteBorderSize / 1.5;
                double x = totalWidth - whiteBorderSize - textWidth - marginRight;
                double y = totalHeight - marginBottom;
                gc.fillText(fullCopyrightText, x, y);
            }

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
