package it.romagnoli.photoborder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/main.fxml"));
        // Raddoppia le dimensioni iniziali dell'applicazione
        Scene scene = new Scene(fxmlLoader.load(), 1490, 1000); // Dimensioni iniziali: 1600x1200
        stage.setTitle("PhotoBorder Studio");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}