package it.romagnoli.photoborder.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Selettore di file scritto interamente in JavaFX (nessuna dipendenza dal {@code FileChooser}
 * nativo). Nasce per evitare un bug del {@code FileChooser} di JavaFX su Linux/GTK per cui i
 * filtri per estensione (compreso il filtro "*.*"/Tutti i file) non mostrano in modo affidabile
 * i file con determinate estensioni (es. maiuscole, o meno comuni come .rw2). Elencando
 * manualmente il contenuto della directory con {@link File#listFiles()} si ha il pieno
 * controllo su cosa viene mostrato, senza dipendere dal comportamento del toolkit nativo.
 */
public final class SimpleFileChooser {

    private SimpleFileChooser() {
    }

    /**
     * Mostra un dialog modale per la selezione di un singolo file, partendo dalla directory
     * indicata (o dalla home utente se {@code null}).
     *
     * @param owner        finestra proprietaria del dialog (può essere {@code null})
     * @param title        titolo del dialog
     * @param startDir     directory iniziale da mostrare (se {@code null} o non valida, si usa
     *                     la home utente)
     * @param extensions   estensioni accettate (es. ".jpg", ".cr2"), confrontate in modo
     *                     case-insensitive; se {@code null} o vuota, vengono mostrati tutti i
     *                     file. Le cartelle sono sempre mostrate, indipendentemente dal filtro.
     * @return il file selezionato, oppure {@link Optional#empty()} se l'utente ha annullato
     */
    public static Optional<File> show(Window owner, String title, File startDir, List<String> extensions) {
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        File initialDir = (startDir != null && startDir.isDirectory())
                ? startDir
                : new File(System.getProperty("user.home"));

        // Normalizziamo le estensioni in minuscolo una sola volta, cosi il confronto con il
        // nome del file (anch'esso normalizzato) sarà sempre case-insensitive: in questo modo
        // vengono mostrati sia i file con estensione minuscola sia quelli con estensione
        // maiuscola (es. .CR2, .JPG).
        List<String> normalizedExtensions = (extensions == null) ? List.of()
                : extensions.stream().map(ext -> ext.toLowerCase(Locale.ROOT)).toList();

        TextField pathField = new TextField(initialDir.getAbsolutePath());
        pathField.setEditable(false);
        Button upButton = new Button("⬆ Su");

        ListView<File> listView = new ListView<>();
        listView.setPrefSize(600, 420);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.isDirectory() ? "📁 " + item.getName() : "🖼 " + item.getName());
                }
            }
        });

        // Wrapper mutabile per tenere traccia della directory correntemente mostrata.
        File[] currentDir = { initialDir };

        Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            pathField.setText(currentDir[0].getAbsolutePath());
            listView.getItems().setAll(listFilesSorted(currentDir[0], normalizedExtensions));
        };
        Runnable refresh = refreshHolder[0];
        refresh.run();

        upButton.setOnAction(e -> {
            File parent = currentDir[0].getParentFile();
            if (parent != null) {
                currentDir[0] = parent;
                refresh.run();
            }
        });

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                File selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.isDirectory()) {
                    currentDir[0] = selected;
                    refresh.run();
                }
            }
        });

        HBox topBar = new HBox(8, upButton, pathField);
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        VBox content = new VBox(10, topBar, listView);
        content.setPadding(new Insets(15));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Il pulsante OK è abilitato solo quando è selezionato un file (non una cartella).
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                okButton.setDisable(newVal == null || newVal.isDirectory()));

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Sovraccarico senza filtro per estensione: mostra tutti i file, comportamento identico a
     * {@link #show(Window, String, File, List)} chiamato con {@code extensions = null}.
     */
    public static Optional<File> show(Window owner, String title, File startDir) {
        return show(owner, title, startDir, null);
    }

    /**
     * Elenca le cartelle e i file di {@code dir} che rispettano il filtro per estensione
     * (case-insensitive), ordinando prima le cartelle poi i file, ed entrambi in ordine
     * alfabetico case-insensitive. Le cartelle vengono sempre incluse, indipendentemente dal
     * filtro, per permettere la navigazione. In caso di errore di lettura mostra un
     * {@link Alert} e ritorna una lista vuota.
     */
    private static List<File> listFilesSorted(File dir, List<String> normalizedExtensions) {
        File[] children = dir.listFiles();
        if (children == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Impossibile leggere il contenuto della cartella: " + dir.getAbsolutePath());
            alert.showAndWait();
            return List.of();
        }

        List<File> result = new ArrayList<>();
        for (File child : children) {
            if (child.isDirectory() || normalizedExtensions.isEmpty() || matchesExtension(child, normalizedExtensions)) {
                result.add(child);
            }
        }

        result.sort(Comparator
                .comparing(File::isDirectory).reversed()
                .thenComparing(f -> f.getName().toLowerCase(Locale.ROOT)));
        return result;
    }

    /** Verifica se il nome del file termina con una delle estensioni indicate, ignorando il case. */
    private static boolean matchesExtension(File file, List<String> normalizedExtensions) {
        String lowerName = file.getName().toLowerCase(Locale.ROOT);
        return normalizedExtensions.stream().anyMatch(lowerName::endsWith);
    }
}
