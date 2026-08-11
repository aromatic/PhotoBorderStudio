package it.romagnoli.photoborder.dialog;

import it.romagnoli.photoborder.view.ZoneSystemView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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

public class ZoneSystemDialog {
    
    private Dialog dialog;
    private ImageView imageView;


    public ZoneSystemDialog() {}

    public void show() {
        if (dialog == null) {
            dialog = new Dialog();
            dialog.setTitle("Zone System");
            dialog.setHeaderText("Regolazione Zone System");
            dialog.initModality(javafx.stage.Modality.NONE);
            dialog.setResizable(true);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            Button closeButton =
                    (Button) dialog.getDialogPane()
                            .lookupButton(ButtonType.CLOSE);

            closeButton.addEventFilter(ActionEvent.ACTION, event -> {
                reset();
                dialog.close();
                dialog = null;
            });

            MenuBar menuBar = createMenuBar();
            
            // Creazione del contenuto del dialogo
            VBox content = new VBox();
            content.setSpacing(10);
            content.setPadding(new Insets(10));

            // Aggiungi qui i controlli per il Zone System (slider, etichette, ecc.)
            // Ad esempio:
            Label label = new Label("Controlli Zone System:");
            content.getChildren().add(menuBar);
            content.getChildren().add(label);

            // 1. Istanzia la vista non distruttiva definita in precedenza
            ZoneSystemView zoneView = new ZoneSystemView();
            zoneView.setImage(imageView.getImage());
            content.getChildren().add(zoneView);

            // 2. Controlli dell'interfaccia
        // Slider per selezionare la Zona (da 0 a 10)
        Slider zoneSlider = new Slider(0, 10, zoneView.getTargetZone());
        zoneSlider.setMajorTickUnit(1);
        zoneSlider.setMinorTickCount(0);
        zoneSlider.setSnapToTicks(true);
        zoneSlider.setShowTickMarks(true);
        zoneSlider.setShowTickLabels(true);

        Label zoneValueLabel = new Label("Zona: " + (int) zoneSlider.getValue());

        // Binda lo slider direttamente alla proprietà della ZoneSystemView
        zoneSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int zone = newVal.intValue();
            zoneView.setTargetZone(zone);
            zoneValueLabel.setText("Zona: " + zone);
        });

        // CheckBox per attivare/disattivare l'overlay non distruttivo
        CheckBox enableOverlayCheckBox = new CheckBox("Attiva Evidenziazione Gialla");
        enableOverlayCheckBox.setSelected(zoneView.isOverlayEnabled());
        zoneView.overlayEnabledProperty().bind(enableOverlayCheckBox.selectedProperty());
        content.getChildren().addAll(zoneSlider, zoneValueLabel, enableOverlayCheckBox);
        
            // Aggiungi il contenuto al dialogo
            dialog.getDialogPane().setContent(content);

            // Aggiungi pulsanti OK e Cancel
            //dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        }
        dialog.show();
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
    
    public void reset() {
        imageView.setImage(null);
    }
    
    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }
}
