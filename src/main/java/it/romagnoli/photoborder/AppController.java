package it.romagnoli.photoborder;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class AppController {

    @FXML
    private ImageView imageView;

    @FXML
    private TextField borderTextField;

    @FXML
    private TextField blackBorderTextField;

    @FXML
    private Button borderMinusBtn;

    @FXML
    private Button borderPlusBtn;

    @FXML
    private Button blackBorderMinusBtn;

    @FXML
    private Button blackBorderPlusBtn;

    @FXML
    private Button saveButton;

    @FXML
    private TextField copyrightTextField;

    @FXML
    private ComboBox<String> fontComboBox;

    private Image originalImage;
    private WritableImage borderedImage; // immagine con i bordi, usata anche per il salvataggio

    @FXML
    private void initialize() {
        // Imposta i valori iniziali
        borderTextField.setText("8");
        blackBorderTextField.setText("1");

        // Configura TextField per accettare solo interi
        configureIntegerTextField(borderTextField);
        configureIntegerTextField(blackBorderTextField);

        // Inizializza il ComboBox con i font disponibili
        fontComboBox.setItems(FXCollections.observableArrayList(
            javafx.scene.text.Font.getFamilies()
        ));
        fontComboBox.setValue("Arial");

        // Listener per i pulsanti del bordo bianco
        borderMinusBtn.setOnAction(event -> decrementValue(borderTextField));
        borderPlusBtn.setOnAction(event -> incrementValue(borderTextField));

        // Listener per i pulsanti del bordo nero
        blackBorderMinusBtn.setOnAction(event -> decrementValue(blackBorderTextField));
        blackBorderPlusBtn.setOnAction(event -> incrementValue(blackBorderTextField));

        // Listener per le modifiche ai campi di testo
        borderTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                updateBorders();
            }
        });

        blackBorderTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                updateBorders();
            }
        });

        // Listener per il copyright
        copyrightTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                updateBorders();
            }
        });

        // Listener per il cambio del font
        fontComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                updateBorders();
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

    private void configureIntegerTextField(TextField textField) {
        textField.setTextFormatter(new javafx.scene.control.TextFormatter<Integer>(
            new javafx.util.converter.IntegerStringConverter(),
            0,
            change -> {
                String newText = change.getControlNewText();
                if (newText.isEmpty()) {
                    return change;
                }
                try {
                    Integer.parseInt(newText);
                    return change;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        ));
    }

    private void incrementValue(TextField textField) {
        try {
            int value = Integer.parseInt(textField.getText());
            textField.setText(String.valueOf(value + 1));
        } catch (NumberFormatException e) {
            textField.setText("0");
        }
    }

    private void decrementValue(TextField textField) {
        try {
            int value = Integer.parseInt(textField.getText());
            if (value > 0) {
                textField.setText(String.valueOf(value - 1));
            }
        } catch (NumberFormatException e) {
            textField.setText("0");
        }
    }

    private void updateBorders() {
        try {
            double whiteBorder = Double.parseDouble(borderTextField.getText());
            double blackBorder = Double.parseDouble(blackBorderTextField.getText());
            applyBorders(whiteBorder, blackBorder);
        } catch (NumberFormatException e) {
            // Valore non valido, ignora
        }
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
        }
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
            String copyrightText = copyrightTextField.getText();
            if (copyrightText != null && !copyrightText.trim().isEmpty()) {
                gc.setFill(Color.BLACK);
                String selectedFont = fontComboBox.getValue() != null ? fontComboBox.getValue() : "Arial";
                
                // Calcola la dimensione del font proporzionata al bordo bianco (metà dell'altezza del bordo)
                int fontSize = Math.max((int)(whiteBorderSize * 0.5), 8);
                gc.setFont(new javafx.scene.text.Font(selectedFont, fontSize));
                
                // Posiziona il copyright al centro del bordo bianco inferiore
                gc.fillText(copyrightText, whiteBorderSize + 10, totalHeight - whiteBorderSize / 2);
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