package it.romagnoli.photoborder.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.Scene;

import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import nu.pattern.OpenCV;

import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import it.romagnoli.photoborder.utils.CanvasBorderedImage;


/**
 * Dialog non modale e ridimensionabile che mostra una GridView di ControlsFX con 9 celle (3x3)
 * rappresentanti i colori dell'immagine campionati nei punti di intersezione della regola dei terzi.
 */
public class AlphaEditorDialog {

    private final Label infoLabel = new Label("Editor immagine");

    private Dialog<Void> dialog;
    private Runnable onClose;
    private Runnable onReset;
    ImageView imageView;

    double contrast = 1;
    private final int rtype = -1;
    double alpha = 1;
    double beta = 0;
    Slider slider1;
    Slider slider2;
    Mat src = null;
    private CanvasBorderedImage canvasBorderedImage;
    WritableImage writableImage = null;
    private File file;

    public AlphaEditorDialog() {

    }

    public void show() {
    
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.setTitle("Regolazione Luminosità e Contrasto (OpenCV)");
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

            Label label1 = new Label("Alpha value (Contrasto)");

            // Setting del primo slider (Alpha)
            slider1 = new Slider(0.1, 3, 1);
            slider1.setShowTickLabels(true);
            slider1.setShowTickMarks(true);
            slider1.setMajorTickUnit(1);
            slider1.setBlockIncrement(0.05);

            Label label2 = new Label("α-value: 1.0");
            Label label3 = new Label("Beta value (Luminosità)");

            // Setting del secondo slider (Beta)
            slider2 = new Slider(-100, 100, 0);
            slider2.setShowTickLabels(true);
            slider2.setShowTickMarks(true);
            slider2.setMajorTickUnit(25);
            slider2.setBlockIncrement(10);

            Label label4 = new Label("β-value: 0.0");

            System.out.println("******************************* src: " + src);
            // Listener per slider1 (Alpha)
            slider1.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    try {
                        label2.setText(String.format("α-value: %.2f", newValue.doubleValue()));
                        alpha = newValue.doubleValue();
                        Mat dest = new Mat(src.rows(), src.cols(), src.type());
                        src.convertTo(dest, rtype, alpha, beta);
                        imageView.setImage(loadImage(dest));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Listener per slider2 (Beta)
            slider2.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    try {
                        label4.setText(String.format("β-value: %.2f", newValue.doubleValue()));
                        beta = newValue.doubleValue();
                        Mat dest = new Mat(src.rows(), src.cols(), src.type());
                        src.convertTo(dest, rtype, alpha, beta);
                        imageView.setImage(loadImage(dest));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // VBox per organizzare i componenti
            VBox vbox = new VBox();
            vbox.setPadding(new Insets(20));
            vbox.setSpacing(10);
            vbox.getChildren().addAll(label1, slider1, label2, label3, slider2, label4);

           /* Button savePointsButton = new Button("Salva punti");

            savePointsButton.setOnAction(
                    e -> Utility.salvaSchemaPunti(schemaPointsFile, canvasBorderedImage.getImageView(), colorSamplePoints)
            );

            Button loadPointsButton = new Button("Leggi schema punti");

            loadPointsButton.setOnAction(
                    e -> {
                        colorSamplePoints = Utility.leggiSchemaPunti(canvasBorderedImage.getImageView());
                        updateColors(canvasBorderedImage.getImageView().getImage(), colorSamplePoints);
                        drawColorMarkers(canvasBorderedImage.getMarkerCanvas(), canvasBorderedImage.getCurrentBorderOffset());
                    }
            ); */

            HBox bottomBar = new HBox(10, infoLabel, /*savePointsButton, loadPointsButton, */ vbox);
            bottomBar.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoLabel, Priority.ALWAYS);

            VBox content = new VBox(10, menuBar, bottomBar);
            dialog.getDialogPane().setContent(content);
            dialog.setOnHidden(event -> {
                if (onClose != null) {
                    onClose.run();
                }
            });
        }
        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
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


    public WritableImage loadImage(Mat image) throws IOException {
      MatOfByte matOfByte = new MatOfByte();
      Imgcodecs.imencode(".jpg", image, matOfByte);
      //Storing the encoded Mat in a byte array
      byte[] byteArray = matOfByte.toArray();
      //Displaying the image
      InputStream in = new ByteArrayInputStream(byteArray);
      BufferedImage bufImage = ImageIO.read(in);
      System.out.println("******************************* Image Loaded");
      WritableImage writableImage = SwingFXUtils.toFXImage(bufImage, null);
      return writableImage;
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
        Image sourceImage = imageView.getImage();
        if (sourceImage != null) {
            // Converti l'immagine in Mat
            BufferedImage bufImage  = SwingFXUtils.fromFXImage(sourceImage, null);
            writableImage = SwingFXUtils.toFXImage(bufImage, null);
            System.out.println("******************************* Image Loaded from ImageView");  
        }
    }

    public void setFile(File file) {
        this.file = file;
        src = Imgcodecs.imread(file.getAbsolutePath());
        System.out.println("******************************* Image Loaded from file: " + file.getAbsolutePath());    
    }
}
