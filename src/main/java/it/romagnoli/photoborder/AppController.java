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

    @FXML
    private TextField fontScaleTextField;

    @FXML
    private Button fontScaleMinusBtn;

    @FXML
    private Button fontScalePlusBtn;

    private Image originalImage;
    private WritableImage borderedImage; // immagine con i bordi, usata anche per il salvataggio

    @FXML
    private void initialize() {
        // Configura TextField per accettare solo interi
        configureIntegerTextField(borderTextField);
        configureIntegerTextField(blackBorderTextField);
        configureFontScaleTextField(fontScaleTextField);

        // Imposta i valori iniziali DOPO la configurazione del TextFormatter
        borderTextField.setText("400");
        blackBorderTextField.setText("30");
        fontScaleTextField.setText("6");

        // Inizializza il ComboBox con i font disponibili
        fontComboBox.setItems(FXCollections.observableArrayList(
            javafx.scene.text.Font.getFamilies()
        ));

        // Listener per il cambio del font (aggiunto PRIMA di impostare il valore iniziale)
        fontComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                updateBorders();
            }
        });

        fontComboBox.setValue("Arial");

        // Listener per i pulsanti del bordo bianco
        borderMinusBtn.setOnAction(event -> decrementValue(borderTextField));
        borderPlusBtn.setOnAction(event -> incrementValue(borderTextField));

        // Listener per i pulsanti del bordo nero
        blackBorderMinusBtn.setOnAction(event -> decrementValue(blackBorderTextField));
        blackBorderPlusBtn.setOnAction(event -> incrementValue(blackBorderTextField));

        // Listener per i pulsanti della scala font
        fontScaleMinusBtn.setOnAction(event -> decrementValue(fontScaleTextField, 1, 10));
        fontScalePlusBtn.setOnAction(event -> incrementValue(fontScaleTextField, 1, 10));

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

        fontScaleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (originalImage != null) {
                updateBorders();
            }
        });

        // Listener per il copyright: disegna solo quando premi Enter
        copyrightTextField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (originalImage != null) {
                    updateBorders();
                }
            }
        });

        // Aggiungi un listener per verificare quando la scena è pronta
        imageView.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                imageView.fitWidthProperty().bind(newScene.widthProperty());
                imageView.fitHeightProperty().bind(newScene.heightProperty().subtract(70));
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

    private void configureFontScaleTextField(TextField textField) {
        textField.setTextFormatter(new javafx.scene.control.TextFormatter<Integer>(
            new javafx.util.converter.IntegerStringConverter(),
            6,
            change -> {
                String newText = change.getControlNewText();
                if (newText.isEmpty()) {
                    return change;
                }
                try {
                    int value = Integer.parseInt(newText);
                    if (value >= 1 && value <= 10) {
                        return change;
                    }
                } catch (NumberFormatException e) {
                }
                return null;
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

    private void incrementValue(TextField textField, int min, int max) {
        try {
            int value = Integer.parseInt(textField.getText());
            if (value < max) {
                textField.setText(String.valueOf(value + 1));
            }
        } catch (NumberFormatException e) {
            textField.setText(String.valueOf(min));
        }
    }

    private void decrementValue(TextField textField, int min, int max) {
        try {
            int value = Integer.parseInt(textField.getText());
            if (value > min) {
                textField.setText(String.valueOf(value - 1));
            }
        } catch (NumberFormatException e) {
            textField.setText(String.valueOf(min));
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
                
                // Calcola la scala del font dal valore 1-10
                // 1-10 corrisponde a 0.1-0.6
                int fontScaleValue = 4; // valore predefinito
                try {
                    String scaleText = fontScaleTextField.getText();
                    if (scaleText != null && !scaleText.isEmpty()) {
                        fontScaleValue = Integer.parseInt(scaleText);
                        fontScaleValue = Math.max(1, Math.min(10, fontScaleValue));
                    }
                } catch (NumberFormatException e) {
                    fontScaleValue = 4;
                }
                
                // Converti da scala 1-10 a fattore 0.1-0.6
                double scaleFactor = 0.1 + (fontScaleValue - 1) * (0.5 / 9.0);
                
                // Calcola la dimensione del font proporzionata al bordo bianco
                int fontSize = Math.max((int)(whiteBorderSize * scaleFactor), 12);
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