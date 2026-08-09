package it.romagnoli.photoborder;

import it.romagnoli.photoborder.dialog.BorderDialog;
import it.romagnoli.photoborder.dialog.ColorAnalysisDialog;
import it.romagnoli.photoborder.dialog.CopyrightDialog;
import it.romagnoli.photoborder.dialog.HistogramDialog;
import it.romagnoli.photoborder.dialog.HueSaturationDialog;
import it.romagnoli.photoborder.dialog.ImageCompareDialog;
import it.romagnoli.photoborder.dialog.Utility;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import it.romagnoli.photoborder.raw.RawImageReader;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppController {

    @FXML
    private ImageView imageView;

    @FXML
    private Canvas markerCanvas;

    @FXML
    private MenuItem saveMenuItem;
    
    private File file; // file immagine attualmente aperto

    private final BorderDialog borderDialog = new BorderDialog();
    private final CopyrightDialog copyrightDialog = new CopyrightDialog();
    private final HistogramDialog histogramDialog = new HistogramDialog();
    private final HueSaturationDialog hueSaturationDialog = new HueSaturationDialog();
    private final ColorAnalysisDialog colorAnalysisDialog = new ColorAnalysisDialog();
    private final ImageCompareDialog imageCompareDialog = new ImageCompareDialog();

    private Image originalImage;
    private WritableImage borderedImage; // immagine con i bordi, usata anche per il salvataggio
    private List<Point2D> colorSamplePoints = List.of(); // punti (coord. immagine originale) marcati sulla preview
    private int currentBorderOffset = 0; // whiteBorderSize + blackBorderSize applicati all'ultima renderizzazione

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

        // Il canvas dei marker segue sempre le dimensioni dell'area immagine
        markerCanvas.widthProperty().bind(imageView.fitWidthProperty());
        markerCanvas.heightProperty().bind(imageView.fitHeightProperty());
        markerCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawColorMarkers());
        markerCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawColorMarkers());

        // Rimuove i marker quando il dialog colori viene chiuso
        colorAnalysisDialog.setOnClose(this::drawColorMarkers);

        // Quando si seleziona/deseleziona una cella colorata, mostra solo il marker corrispondente
        colorAnalysisDialog.selectedIndexProperty().addListener((obs, oldVal, newVal) -> drawColorMarkers());

        // Bottone Reset nel dialog colori: ricampiona e ricentra i marker, azzerando la verifica
        colorAnalysisDialog.setOnReset(this::refreshColorAnalysis);

        // Dopo la verifica "Toni incarnato", ridisegna i marker colorandoli in verde/rosso
        colorAnalysisDialog.setOnSkinToneCheckChanged(this::drawColorMarkers);

        // Click sull'immagine: se una cella è selezionata, ricampiona colore e punto in quella posizione
        imageView.setOnMouseClicked(this::handleImageClicked);

        // Muovendo il mouse sull'immagine, evidenzia nel dialog la cella corrispondente al marker sotto il cursore
        imageView.setOnMouseMoved(this::handleImageMouseMoved);
        imageView.setOnMouseExited(event -> colorAnalysisDialog.setHoveredIndex(-1));

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
        fileChooser.setTitle("Open Image File");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Image Files", Utility.ACCEPTED_EXTENSIONS.stream().map(ext -> "*" + ext).toArray(String[]::new)),
                new ExtensionFilter("Raw Files", Utility.RAW_EXTENSIONS.stream().map(ext -> "*" + ext).toArray(String[]::new)),
                new ExtensionFilter("All Files", "*.*"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini",
                String.join(", *", Utility.ACCEPTED_EXTENSIONS)));
        file = fileChooser.showOpenDialog(null);

        if (file != null) {
            originalImage = loadImage(file);
            imageView.setImage(originalImage);
            currentBorderOffset = 0;
            borderedImage = null;

            histogramDialog.updateImage(originalImage);
            hueSaturationDialog.updateImage(originalImage);

            colorAnalysisDialog.setImageView(imageView);
            colorAnalysisDialog.setCurrentImageFile(file);

            if (colorAnalysisDialog.isShowing()) {
                refreshColorAnalysis();
            }
        }
    }

    @FXML
    private void handleExit() {
        javafx.application.Platform.exit();
    }

    /**
     * Carica un'immagine da file, supportando anche il formato TIFF (non gestito nativamente
     * da JavaFX) tramite ImageIO e conversione con {@link SwingFXUtils#toFXImage}. Per gli altri
     * formati (JPG, GIF, PNG, ...) usa direttamente il costruttore di {@link Image}.
     */
    private Image loadImage(File file) {
        String name = file.getName().toLowerCase(java.util.Locale.ROOT);
        // raw
        if (Utility.RAW_EXTENSIONS.stream().anyMatch(name::endsWith)) {
            try {
                java.awt.image.BufferedImage bufferedImage = RawImageReader.read(file);
                return SwingFXUtils.toFXImage(bufferedImage, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        // tiff
        if (name.endsWith(".tif") || name.endsWith(".tiff")) {
            try {
                java.awt.image.BufferedImage bufferedImage = ImageIO.read(file);
                if (bufferedImage != null) {
                    return SwingFXUtils.toFXImage(bufferedImage, null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        return new Image(file.toURI().toString());
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

    @FXML
    private void handleShowHueSaturationDialog() {
        hueSaturationDialog.show();
        hueSaturationDialog.updateImage(originalImage);
    }

    @FXML
    private void handleShowColorAnalysisDialog() {
        colorAnalysisDialog.show();
        refreshColorAnalysis();
    }

    @FXML
    private void handleShowCompareDialog() {
        imageCompareDialog.show();
    }

    /** Ricalcola i colori campionati dall'immagine e ridisegna i marker in overlay. */
    private void refreshColorAnalysis() {
        colorSamplePoints = colorAnalysisDialog.updateColors(originalImage, null);
        drawColorMarkers();
    }

    /**
     * Disegna dei cerchietti rossi sull'overlay in corrispondenza dei punti della
     * regola dei terzi, senza alterare in alcun modo l'immagine sottostante.
     */
    private void drawColorMarkers() {
        GraphicsContext gc = markerCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, markerCanvas.getWidth(), markerCanvas.getHeight());

        Image displayedImage = imageView.getImage();
        if (!colorAnalysisDialog.isShowing() || displayedImage == null || colorSamplePoints.isEmpty()) {
            return;
        }

        double imgWidth = displayedImage.getWidth();
        double imgHeight = displayedImage.getHeight();
        double canvasWidth = markerCanvas.getWidth();
        double canvasHeight = markerCanvas.getHeight();
        if (imgWidth <= 0 || imgHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            return;
        }

        double scale = Math.min(canvasWidth / imgWidth, canvasHeight / imgHeight);
        double displayedWidth = imgWidth * scale;
        double displayedHeight = imgHeight * scale;
        double offsetX = (canvasWidth - displayedWidth) / 2;
        double offsetY = (canvasHeight - displayedHeight) / 2;

        double radius = 6;
        gc.setLineWidth(2);

        int selectedIndex = colorAnalysisDialog.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < colorSamplePoints.size()) {
            // Una cella è selezionata: mostra solo il marker di riferimento di quel colore
            drawSingleMarker(gc, colorSamplePoints.get(selectedIndex), selectedIndex, scale, offsetX, offsetY, radius);
        } else {
            for (int i = 0; i < colorSamplePoints.size(); i++) {
                drawSingleMarker(gc, colorSamplePoints.get(i), i, scale, offsetX, offsetY, radius);
            }
        }
    }

    /** Disegna un singolo cerchietto (in coordinate immagine originale) sull'overlay: verde se il
     * colore rispetta il criterio dei toni incarnato (dopo la verifica), rosso altrimenti. */
    private void drawSingleMarker(GraphicsContext gc, Point2D point, int index, double scale,
                                   double offsetX, double offsetY, double radius) {
        boolean isValid = colorAnalysisDialog.isSkinToneChecked() && !colorAnalysisDialog.isInvalidSkinTone(index);
        gc.setStroke(isValid ? Color.GREEN : Color.RED);
        // Sposta il punto (in coordinate immagine originale) nelle coordinate
        // dell'immagine effettivamente mostrata, tenendo conto dei bordi aggiunti.
        double cx = offsetX + (point.getX() + currentBorderOffset) * scale;
        double cy = offsetY + (point.getY() + currentBorderOffset) * scale;
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    /**
     * Calcola il fattore di scala tra l'immagine effettivamente mostrata e l'area disponibile,
     * oppure -1 se non calcolabile (nessuna immagine o area non ancora dimensionata).
     */
    private double computeDisplayScale() {
        Image displayedImage = imageView.getImage();
        if (displayedImage == null) {
            return -1;
        }
        double imgWidth = displayedImage.getWidth();
        double imgHeight = displayedImage.getHeight();
        double canvasWidth = markerCanvas.getWidth();
        double canvasHeight = markerCanvas.getHeight();
        if (imgWidth <= 0 || imgHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            return -1;
        }
        return Math.min(canvasWidth / imgWidth, canvasHeight / imgHeight);
    }

    /**
     * Converte un punto in coordinate immagine originale nelle coordinate locali dell'ImageView
     * (stesso sistema di riferimento degli eventi mouse su imageView, senza offset di centratura).
     */
    private Point2D toImageViewLocalCoordinates(Point2D originalPoint, double scale) {
        return new Point2D(
            (originalPoint.getX() + currentBorderOffset) * scale,
            (originalPoint.getY() + currentBorderOffset) * scale
        );
    }

    /**
     * Gestisce il movimento del mouse sull'immagine: se il cursore si trova sopra uno dei
     * cerchietti attualmente visibili, evidenzia (hover) la cella corrispondente nel dialog.
     */
    private void handleImageMouseMoved(MouseEvent event) {
        if (!colorAnalysisDialog.isShowing() || colorSamplePoints.isEmpty()) {
            colorAnalysisDialog.setHoveredIndex(-1);
            return;
        }

        double scale = computeDisplayScale();
        if (scale <= 0) {
            colorAnalysisDialog.setHoveredIndex(-1);
            return;
        }

        double hitRadius = 10;
        int hovered = -1;
        int selectedIndex = colorAnalysisDialog.getSelectedIndex();

        if (selectedIndex >= 0 && selectedIndex < colorSamplePoints.size()) {
            // Solo il marker selezionato è visibile: considera solo quello per l'hover
            Point2D local = toImageViewLocalCoordinates(colorSamplePoints.get(selectedIndex), scale);
            if (Math.hypot(local.getX() - event.getX(), local.getY() - event.getY()) <= hitRadius) {
                hovered = selectedIndex;
            }
        } else {
            for (int i = 0; i < colorSamplePoints.size(); i++) {
                Point2D local = toImageViewLocalCoordinates(colorSamplePoints.get(i), scale);
                if (Math.hypot(local.getX() - event.getX(), local.getY() - event.getY()) <= hitRadius) {
                    hovered = i;
                    break;
                }
            }
        }

        colorAnalysisDialog.setHoveredIndex(hovered);
    }

    /**
     * Gestisce il click sull'immagine.
     * <ul>
     *   <li>Tasto destro: deseleziona la cella corrente e ridisegna tutti i marker.</li>
     *   <li>Tasto sinistro sopra un marker (hover): seleziona quella cella, come se si fosse
     *       cliccato direttamente sul riquadro nel dialog (nasconde gli altri marker, senza spostarlo).</li>
     *   <li>Tasto sinistro altrove, con una cella già selezionata: ricampiona il colore
     *       dell'immagine originale nel punto cliccato e sposta il marker in quella posizione.</li>
     * </ul>
     */
    private void handleImageClicked(MouseEvent event) {
        if (originalImage == null || !colorAnalysisDialog.isShowing()) {
            return;
        }

        if (event.getButton() == MouseButton.SECONDARY) {
            colorAnalysisDialog.clearSelection();
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        int hoveredIndex = colorAnalysisDialog.getHoveredIndex();
        if (hoveredIndex >= 0) {
            // Click direttamente su un marker: si comporta come il click sul riquadro nel dialog
            colorAnalysisDialog.selectIndex(hoveredIndex);
            return;
        }

        int selectedIndex = colorAnalysisDialog.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= colorSamplePoints.size()) {
            return;
        }

        Image displayedImage = imageView.getImage();
        if (displayedImage == null) {
            return;
        }

        double scale = computeDisplayScale();
        if (scale <= 0) {
            return;
        }

        // Nota: a differenza del canvas (che occupa sempre l'intera area fitW x fitH),
        // i bounds locali di una ImageView con preserveRatio corrispondono già alle
        // dimensioni effettive dell'immagine renderizzata (senza letterbox), quindi
        // event.getX()/getY() sono già relativi all'angolo in alto a sinistra
        // dell'immagine visibile: non va sottratto alcun offset di centratura.
        double origX = event.getX() / scale - currentBorderOffset;
        double origY = event.getY() / scale - currentBorderOffset;

        int originalWidth = (int) originalImage.getWidth();
        int originalHeight = (int) originalImage.getHeight();
        int px = clamp((int) Math.round(origX), 0, originalWidth - 1);
        int py = clamp((int) Math.round(origY), 0, originalHeight - 1);

        // Se il click cade fuori dall'area dell'immagine originale (sul bordo), ignora
        if (origX < 0 || origY < 0 || origX >= originalWidth || origY >= originalHeight) {
            return;
        }

        PixelReader reader = originalImage.getPixelReader();
        Color newColor = reader.getColor(px, py);

        List<Point2D> updatedPoints = new ArrayList<>(colorSamplePoints);
        updatedPoints.set(selectedIndex, new Point2D(px, py));
        colorSamplePoints = updatedPoints;

        colorAnalysisDialog.updateSampleAt(selectedIndex, new Point2D(px, py), newColor);
        drawColorMarkers();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

            currentBorderOffset = whiteBorderSize + blackBorderSize;

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

            // Il bordo può essere cambiato: riallinea la posizione dei marker sull'overlay
            drawColorMarkers();
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
