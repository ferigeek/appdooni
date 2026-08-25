module com.github.ferigeek.appdooni {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens com.github.ferigeek.appdooni to javafx.fxml;
    exports com.github.ferigeek.appdooni;
}