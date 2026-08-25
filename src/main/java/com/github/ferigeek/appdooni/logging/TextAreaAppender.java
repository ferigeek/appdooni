package com.github.ferigeek.appdooni.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * Logback appender that mirrors log events into a JavaFX {@link TextArea}.
 * The text area is set by the main controller once the UI is ready; events
 * emitted before then are ignored.
 */
public class TextAreaAppender extends AppenderBase<ILoggingEvent> {

    private static volatile TextArea textArea;

    public static void setTextArea(TextArea area) {
        textArea = area;
    }

    @Override
    protected void append(ILoggingEvent event) {
        TextArea target = textArea;
        if (target == null) {
            return;
        }
        String text = String.format("%s %-5s %s - %s%n",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        .format(new java.util.Date(event.getTimeStamp())),
                event.getLevel(),
                event.getLoggerName(),
                event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            text += event.getThrowableProxy().getClassName() + ": "
                    + event.getThrowableProxy().getMessage() + System.lineSeparator();
        }
        String toAppend = text;
        Platform.runLater(() -> target.appendText(toAppend));
    }
}