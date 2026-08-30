module com.github.ferigeek.appdooni {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.material2;

    requires org.xerial.sqlitejdbc;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    opens com.github.ferigeek.appdooni to javafx.fxml;
    opens com.github.ferigeek.appdooni.controller to javafx.fxml;
    opens com.github.ferigeek.appdooni.model to javafx.base;
    exports com.github.ferigeek.appdooni.logging to ch.qos.logback.core;
    exports com.github.ferigeek.appdooni;
}