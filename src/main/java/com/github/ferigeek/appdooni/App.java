package com.github.ferigeek.appdooni;

import com.github.ferigeek.appdooni.controller.MainController;
import com.github.ferigeek.appdooni.repository.DatabaseManager;
import com.github.ferigeek.appdooni.util.AppDirectories;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Entry point of the JavaFX application. Initializes logging and the database,
 * then shows the main window.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        configureLogging();
        new DatabaseManager();

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("appdooni.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 640);
        stage.setTitle("AppDooni");
        try (InputStream iconStream = App.class.getResourceAsStream("appdooni.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        }
        stage.setScene(scene);
        stage.show();

        MainController controller = fxmlLoader.getController();
        if (controller != null) {
            controller.setHostServices(getHostServices());
        }
    }

    private void configureLogging() {
        System.setProperty("appdooni.log", AppDirectories.getLogPath().toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}