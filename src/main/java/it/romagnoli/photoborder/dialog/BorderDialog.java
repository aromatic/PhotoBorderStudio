package it.romagnoli.photoborder.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.IntegerStringConverter;
import org.controlsfx.control.RangeSlider;

/**
 * Dialog non modale per impostare i valori del bordo nero e del bordo bianco.
 * Espone un RangeSlider sincronizzato con due TextField editabili manualmente.
 */
public class BorderDialog {

    private final TextField blackBorderTextField = new TextField();
    private final TextField borderTextField = new TextField();
    private final RangeSlider borderRangeSlider = new RangeSlider(0, 500, 30, 400);

    private Dialog<Void> dialog;

    private boolean updatingFromSlider = false;
    private boolean updatingFromTextField = false;

    private Runnable onChange = () -> {};

    public BorderDialog() {
        configureIntegerTextField(borderTextField);
        configureIntegerTextField(blackBorderTextField);

        borderTextField.setPrefWidth(60);
        blackBorderTextField.setPrefWidth(60);
        borderTextField.setStyle("-fx-alignment: center;");
        blackBorderTextField.setStyle("-fx-alignment: center;");

        borderTextField.setText("400");
        blackBorderTextField.setText("30");

        borderRangeSlider.setShowTickMarks(true);
        borderRangeSlider.setPrefWidth(260);

        // Sincronizza il RangeSlider con i TextField (nero = lowValue, bianco = highValue)
        borderRangeSlider.lowValueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingFromTextField) {
                int value = newVal.intValue();
                updatingFromSlider = true;
                blackBorderTextField.setText(String.valueOf(value));
                updatingFromSlider = false;
            }
        });

        borderRangeSlider.highValueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingFromTextField) {
                int value = newVal.intValue();
                updatingFromSlider = true;
                borderTextField.setText(String.valueOf(value));
                updatingFromSlider = false;
            }
        });

        borderTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingFromSlider) {
                try {
                    double value = Double.parseDouble(newValue);
                    updatingFromTextField = true;
                    borderRangeSlider.setHighValue(value);
                    updatingFromTextField = false;
                } catch (NumberFormatException e) {
                    // ignora
                }
            }
            onChange.run();
        });

        blackBorderTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingFromSlider) {
                try {
                    double value = Double.parseDouble(newValue);
                    updatingFromTextField = true;
                    borderRangeSlider.setLowValue(value);
                    updatingFromTextField = false;
                } catch (NumberFormatException e) {
                    // ignora
                }
            }
            onChange.run();
        });
    }

    private void configureIntegerTextField(TextField textField) {
        textField.setTextFormatter(new javafx.scene.control.TextFormatter<Integer>(
            new IntegerStringConverter(),
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

    /**
     * Imposta il callback invocato ogni volta che il bordo nero o bianco cambia valore.
     */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange != null ? onChange : () -> {};
    }

    /** Mostra il dialog (creandolo alla prima chiamata). */
    public void show() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Impostazioni bordo");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            HBox sliderRow = new HBox(10,
                new Label("Nero"), blackBorderTextField,
                borderRangeSlider,
                borderTextField, new Label("Bianco")
            );
            sliderRow.setAlignment(Pos.CENTER);

            VBox content = new VBox(15, new Label("Bordo nero / bordo bianco (px)"), sliderRow);
            content.setPadding(new Insets(15));
            content.setAlignment(Pos.CENTER);

            dialog.getDialogPane().setContent(content);
        }
        dialog.show();
    }

    public double getBlackBorderValue() {
        return parseOrDefault(blackBorderTextField.getText(), 30);
    }

    public double getWhiteBorderValue() {
        return parseOrDefault(borderTextField.getText(), 400);
    }

    private double parseOrDefault(String text, double defaultValue) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
