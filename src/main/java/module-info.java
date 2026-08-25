module com.github.ferigeek.appdooni {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    requires org.xerial.sqlitejdbc;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    opens com.github.ferigeek.appdooni to javafx.fxml;
    exports com.github.ferigeek.appdooni;
}