package it.romagnoli.photoborder.dialog;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Dialog non modale per impostare il testo del copyright, il font e la scala del testo.
 */
public class CopyrightDialog {

    private final TextField copyrightTextField = new TextField();
    private final ComboBox<String> fontComboBox = new ComboBox<>();
    private final Slider fontScaleSlider = new Slider(1, 10, 6);
    private final Label fontScaleValueLabel = new Label("6");

    private Dialog<Void> dialog;

    private Runnable onChange = () -> {};

    public CopyrightDialog() {
        fontComboBox.setItems(FXCollections.observableArrayList(
            javafx.scene.text.Font.getFamilies()
        ));
        fontComboBox.setPrefWidth(160);
        fontComboBox.setValue("Arial");
        fontComboBox.valueProperty().addListener((observable, oldValue, newValue) -> onChange.run());

        fontScaleSlider.setPrefWidth(160);
        fontScaleSlider.setMajorTickUnit(1);
        fontScaleSlider.setMinorTickCount(0);
        fontScaleSlider.setSnapToTicks(true);
        fontScaleSlider.setShowTickMarks(true);
        fontScaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int rounded = (int) Math.round(newVal.doubleValue());
            fontScaleValueLabel.setText(String.valueOf(rounded));
            onChange.run();
        });

        copyrightTextField.setPromptText("Inserisci testo copyright");
        copyrightTextField.setPrefWidth(220);
        copyrightTextField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                onChange.run();
            }
        });
    }

    /**
     * Imposta il callback invocato quando cambiano font o scala testo,
     * oppure quando si preme Invio nel campo copyright.
     */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange != null ? onChange : () -> {};
    }

    /** Mostra il dialog (creandolo alla prima chiamata). */
    public void show() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Impostazioni copyright");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            HBox textRow = new HBox(10, new Label("Testo"), copyrightTextField, new Label("Font"), fontComboBox);
            textRow.setAlignment(Pos.CENTER_LEFT);

            HBox scaleRow = new HBox(10, new Label("Scala testo (1-10)"), fontScaleSlider, fontScaleValueLabel);
            scaleRow.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(15, textRow, scaleRow);
            content.setPadding(new Insets(15));

            dialog.getDialogPane().setContent(content);
        }
        dialog.show();
    }

    public String getCopyrightText() {
        return copyrightTextField.getText();
    }

    public String getSelectedFont() {
        return fontComboBox.getValue() != null ? fontComboBox.getValue() : "Arial";
    }

    public int getFontScaleValue() {
        int value = (int) Math.round(fontScaleSlider.getValue());
        return Math.max(1, Math.min(10, value));
    }
}
