package it.romagnoli.photoborder.dialog;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;


public class ColorAdjustDialog {

    private final ColorAdjust colorAdjust;
    private Dialog<ColorAdjust> dialog;
    private Runnable onClose;
    private Runnable onReset;
    private ImageView imageView;
    // Esponente della scala: più è alto, più la risposta è piatta vicino a 0
    // Un valore p = 5 rende 0.5 lineare pari a 0.03125 reale
    private static final double EXPONENT = 5.0;
    // Costante base per la curvatura logaritmica/esponenziale (es. E = 2.718...)
    // Aumentare questo valore accentua la concentrazione dei valori vicino allo 0
    private static final double EXP_BASE = 7*Math.E;
    private static final double SLIDER_RANGE = 0.7;

    public ColorAdjustDialog() {
        this(new ColorAdjust());
    }

    public ColorAdjustDialog(ColorAdjust initialEffect) {
        this.colorAdjust = new ColorAdjust();
        if (initialEffect != null) {
            this.colorAdjust.setHue(initialEffect.getHue());
            this.colorAdjust.setSaturation(initialEffect.getSaturation());
            this.colorAdjust.setBrightness(initialEffect.getBrightness());
            this.colorAdjust.setContrast(initialEffect.getContrast());
        }
    }

    /**
     * Costruisce e mostra il Dialog, restituendo l'effetto regolato se l'utente conferma con OK.
     */
    public void show() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Regolazione Colore");
            dialog.setHeaderText("Modifica i parametri dell'effetto ColorAdjust");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.setResizable(true);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            MenuBar menuBar = createMenuBar();

            Button closeButton =
                    (Button) dialog.getDialogPane()
                            .lookupButton(ButtonType.CLOSE);

            closeButton.addEventFilter(ActionEvent.ACTION, event -> {
                reset();
                dialog.close();
                dialog = null;
            });

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // 1. Hue Slider (-1.0 a 1.0)
            Slider hueSlider = createCustomHueSlider();

            // 2. Saturation Slider (-1.0 a 1.0)
            Slider saturationSlider = createCustomSaturationSlider();

            // 3. Brightness Slider 
            Slider brightnessSlider = createCustomBrightnessSlider();

            // 4. Contrast Slider 
            Slider contrastSlider = createCustomContrastSlider();           


            // Aggiunta controlli al layout
            addControlToGrid(grid, "Hue (Tonalità):", hueSlider, 0);
            addControlToGrid(grid, "Saturation (Saturazione):", saturationSlider, 1);
            addControlToGrid(grid, "Brightness (Luminosità):", brightnessSlider, 2);
            addControlToGrid(grid, "Contrast (Contrasto):", contrastSlider, 3);

            
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            VBox content = new VBox(10, menuBar, grid);
            dialog.getDialogPane().setContent(content);
            dialog.setOnHidden(event -> {
                    if (onClose != null) {
                        onClose.run();
                    }
                });
            dialog.getDialogPane().setContent(content);
        }
        
        
        dialog.show();
    }

    private Slider createCustomHueSlider() {
        // Inizializza la posizione lineare dello slider partendo dal valore reale memorizzato
        double initialLinearPos = realToSlider(colorAdjust.getHue());
        Slider slider = createExponentialSlider(-0.5, 0.5, initialLinearPos);

        // Aggiorna il valore reale di Hue ad ogni spostamento dello slider
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            colorAdjust.setHue(realValue);
        });

        // Label per il testo e per la lettura del valore esatto corrente
        Label label = new Label("Hue:");
        double currentRealValue = sliderToReal(slider.getValue());
        Label valueLabel = new Label(String.format("%.3f", currentRealValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            valueLabel.setText(String.format("%.3f", realValue));
        });

        // Listener per aggiornare l'effetto sull'immagine in tempo reale
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                try {
                    imageView.setEffect(colorAdjust);  
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return slider;
    }

    private Slider createCustomSaturationSlider() {
        // Inizializza la posizione lineare dello slider partendo dal valore reale memorizzato
        double initialLinearPos = realToSlider(colorAdjust.getSaturation());
        Slider slider = createExponentialSlider(-0.5, 0.5, initialLinearPos);

        // Aggiorna il valore reale di Saturation ad ogni spostamento dello slider
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            colorAdjust.setSaturation(realValue);
        });

        // Label per il testo e per la lettura del valore esatto corrente
        Label label = new Label("Saturation:");
        double currentRealValue = sliderToReal(slider.getValue());
        Label valueLabel = new Label(String.format("%.3f", currentRealValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            valueLabel.setText(String.format("%.3f", realValue));
        });

        // Listener per aggiornare l'effetto sull'immagine in tempo reale
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                try {
                    imageView.setEffect(colorAdjust);  
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return slider;
    }

    private Slider createCustomBrightnessSlider() {
        // Inizializza la posizione lineare dello slider partendo dal valore reale memorizzato
        double initialLinearPos = realToSlider(colorAdjust.getBrightness());
        Slider slider = createExponentialSlider(-0.5, 0.5, initialLinearPos);

        // Aggiorna il valore reale di Brightness ad ogni spostamento dello slider
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            colorAdjust.setBrightness(realValue);
        });

        // Label per il testo e per la lettura del valore esatto corrente
        Label label = new Label("Brightness:");
        double currentRealValue = sliderToReal(slider.getValue());
        Label valueLabel = new Label(String.format("%.3f", currentRealValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            valueLabel.setText(String.format("%.3f", realValue));
        });

        // Listener per aggiornare l'effetto sull'immagine in tempo reale
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                try {
                    imageView.setEffect(colorAdjust);  
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return slider;
    }

    private Slider createCustomContrastSlider() {
        // Inizializza la posizione lineare dello slider partendo dal valore reale memorizzato
        double initialLinearPos = realToSlider(colorAdjust.getContrast());
        Slider slider = createExponentialSlider(-0.5, 0.5, initialLinearPos);

        // Aggiorna il valore reale di Contrast ad ogni spostamento dello slider
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            colorAdjust.setContrast(realValue);
        });

        // Label per il testo e per la lettura del valore esatto corrente
        Label label = new Label("Contrast:");
        double currentRealValue = sliderToReal(slider.getValue());
        Label valueLabel = new Label(String.format("%.3f", currentRealValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double realValue = sliderToReal(newVal.doubleValue());
            valueLabel.setText(String.format("%.3f", realValue));
        });

        // Listener per aggiornare l'effetto sull'immagine in tempo reale
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                try {
                    imageView.setEffect(colorAdjust);  
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return slider;
    }

    private Slider createExponentialSlider(double min, double max, double defaultValue) {
        Slider slider = new Slider(min, max, defaultValue);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.5);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(0.05);

        // Formattazione diretta delle etichette dello slider in base alla funzione di mappa
        slider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double sliderVal) {
                if (sliderVal == null) return "";
                double realValue = sliderToReal(sliderVal);
                return String.format("%.2f", realValue);
            }

            @Override
            public Double fromString(String string) {
                return 0.0;
            }
        });

        return slider;
    }

    /**
     * Mappa la posizione dello slider [-SLIDER_RANGE, SLIDER_RANGE] nel valore reale esponenziale [-SLIDER_RANGE, SLIDER_RANGE]
     */
    private static double sliderToReal(double sliderVal) {
        if (sliderVal == 0) return 0.0;
        double sign = Math.signum(sliderVal);
        double absVal = Math.abs(sliderVal);
        
        // Curva esponenziale: (e^x - SLIDER_RANGE) / (e - SLIDER_RANGE)
        return sign * (Math.exp(absVal * Math.log(EXP_BASE)) - SLIDER_RANGE) / (EXP_BASE - SLIDER_RANGE);
    }

    /**
     * Mappa il valore reale [-SLIDER_RANGE, SLIDER_RANGE] nella posizione corrispondente dello slider [-SLIDER_RANGE, SLIDER_RANGE]
     */
    private static double realToSlider(double realVal) {
        if (realVal == 0) return 0.0;
        double sign = Math.signum(realVal);
        double absVal = Math.abs(realVal);
        
        // Inversa logaritmica: ln(SLIDER_RANGE + x * (e - SLIDER_RANGE)) / ln(e)
        return sign * Math.log(SLIDER_RANGE + absVal * (EXP_BASE - SLIDER_RANGE)) / Math.log(EXP_BASE);
    }

    /**
     * Mappa un valore lineare [-1, 1] in un valore esponenziale [-1, 1]
     */
    private static double toExponential(double val, double exp) {
        return Math.signum(val) * Math.pow(Math.abs(val), exp);
    }

    /**
     * Mappa un valore esponenziale [-1, 1] nel corrispondente valore lineare dello slider
     */
    private static double toLinear(double val, double exp) {
        return Math.signum(val) * Math.pow(Math.abs(val), 1.0 / exp);
    }

    private void bindExponentialProperty(Slider slider, javafx.beans.property.DoubleProperty targetProperty, double exp) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> 
            targetProperty.set(toExponential(newVal.doubleValue(), exp))
        );
        // Imposta il valore iniziale della proprietà
        targetProperty.set(toExponential(slider.getValue(), exp));
    }
    
    /** Crea la barra dei menu con le voci "Verifica colori" -> "Toni incarnato" / "Verifica incarnato caucasico". */
    private MenuBar createMenuBar() {
        // 1. Crea la singola voce del menu
        MenuItem schemaPointsItem = new MenuItem("Non fa niente");
        schemaPointsItem.setOnAction(event -> nienteDaFare());

        // 2. Crea il menu principale assegnando un titolo visibile (es. "File")
        Menu menuFile = new Menu("File", null, schemaPointsItem);

        // 3. Aggiungi il menu alla barra
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(menuFile);

        return menuBar;
    }

    private void nienteDaFare() {
        
    }

    private Slider createSlider(double min, double max, double defaultValue) {
        Slider slider = new Slider(min, max, defaultValue);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.5);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(0.1);
        return slider;
    }

    private void addControlToGrid(GridPane grid, String labelText, Slider slider, int row) {
        Label label = new Label(labelText);
        Label valueLabel = new Label(String.format("%.2f", slider.getValue()));

        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                valueLabel.setText(String.format("%.2f", newVal.doubleValue()))
        );

        grid.add(label, 0, row);
        grid.add(slider, 1, row);
        grid.add(valueLabel, 2, row);
    }

    /** Callback invocata quando il dialog viene chiuso (per rimuovere i marker dall'immagine). */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /** Callback invocata quando l'utente preme il bottone Reset (per ricentrare i marker sull'immagine). */
    public void setOnReset(Runnable onReset) {
        this.onReset = onReset;
    }

    public void reset() {
        imageView.setImage(null);
    }
    
    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }   
}