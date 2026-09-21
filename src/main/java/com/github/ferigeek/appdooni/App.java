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
import java.net.URL;
import java.util.prefs.Preferences;

/**
 * Entry point of the JavaFX application. Initializes logging and the database,
 * then shows the main window.
 *
 * <p>Owns the light/dark theme: the choice is persisted with
 * {@link Preferences} (kept out of the portable {@code .db} file) and applied
 * as a single stylesheet on the main scene.</p>
 */
public class App extends Application {

    /** Theme name for the light reading-room stylesheet. */
    public static final String THEME_LIGHT = "light";
    /** Theme name for the dark closed-stacks stylesheet. */
    public static final String THEME_DARK = "dark";

    private static final String THEME_PREF_KEY = "theme";

    @Override
    public void start(Stage stage) throws IOException {
        configureLogging();
        new DatabaseManager();

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("appdooni.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 640);
        applyTheme(scene, currentTheme());
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

    /**
     * Returns the persisted theme name, defaulting to light.
     *
     * @return {@link #THEME_LIGHT} or {@link #THEME_DARK}
     */
    public static String currentTheme() {
        String saved = Preferences.userNodeForPackage(App.class).get(THEME_PREF_KEY, THEME_LIGHT);
        return THEME_DARK.equals(saved) ? THEME_DARK : THEME_LIGHT;
    }

    /**
     * Applies the given theme to the scene, replacing any AppDooni theme
     * stylesheet already present, and persists the choice.
     *
     * @param scene the scene to style
     * @param theme {@link #THEME_LIGHT} or {@link #THEME_DARK}
     */
    public static void applyTheme(Scene scene, String theme) {
        String resolved = THEME_DARK.equals(theme) ? THEME_DARK : THEME_LIGHT;
        URL light = App.class.getResource("appdooni-light.css");
        URL dark = App.class.getResource("appdooni-dark.css");
        if (light != null) {
            scene.getStylesheets().remove(light.toExternalForm());
        }
        if (dark != null) {
            scene.getStylesheets().remove(dark.toExternalForm());
        }
        URL selected = THEME_DARK.equals(resolved) ? dark : light;
        if (selected != null) {
            scene.getStylesheets().add(selected.toExternalForm());
        }
        Preferences.userNodeForPackage(App.class).put(THEME_PREF_KEY, resolved);
    }

    /**
     * Returns the stylesheet URL for the given theme, used to style dialog
     * scenes that do not inherit the main scene's stylesheets.
     *
     * @param theme {@link #THEME_LIGHT} or {@link #THEME_DARK}
     * @return stylesheet URL string, or {@code null} when not found
     */
    public static String stylesheetFor(String theme) {
        String file = THEME_DARK.equals(theme) ? "appdooni-dark.css" : "appdooni-light.css";
        URL url = App.class.getResource(file);
        return url == null ? null : url.toExternalForm();
    }

    public static void main(String[] args) {
        launch(args);
    }
}