package com.github.ferigeek.appdooni;

import com.github.ferigeek.appdooni.util.AppDirectories;
import javafx.application.Application;

/**
 * Launcher used for building and packaging, avoiding module issues when the
 * JavaFX {@link Application} class is resolved from an unnamed module.
 */
public class Launcher {

    public static void main(String[] args) {
        System.setProperty("appdooni.log", AppDirectories.getLogPath().toString());
        Application.launch(App.class, args);
    }
}