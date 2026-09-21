package com.github.ferigeek.appdooni.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.util.Duration;

/**
 * Central motion language for AppDooni.
 *
 * <p>Quiet archive handling: short fades and small slides that answer a user
 * action, plus one orchestrated startup reveal. No loops, no bounce, nothing
 * longer than 280 ms. Every helper completes instantly when reduced motion is
 * requested.</p>
 */
public final class Motion {

    private static final Duration PRESS = Duration.millis(80);
    private static final Duration RESPONSE = Duration.millis(160);
    private static final Duration ENTRANCE = Duration.millis(220);

    private Motion() {
    }

    /**
     * Returns {@code true} when all motion should complete instantly.
     *
     * <p>JavaFX exposes no cross-platform reduced-motion query, so this checks
     * the {@code appdooni.reduceMotion} system property and the
     * {@code APPDOONI_REDUCE_MOTION} environment variable ({@code 1},
     * {@code true}, or {@code reduced}). Short durations and no ambient loops
     * keep the default motion reduced-motion friendly.</p>
     *
     * @return whether motion should be skipped
     */
    public static boolean isReduced() {
        String property = System.getProperty("appdooni.reduceMotion", "").trim().toLowerCase();
        String env = System.getenv().getOrDefault("APPDOONI_REDUCE_MOTION", "").trim().toLowerCase();
        return isReducedValue(property) || isReducedValue(env);
    }

    private static boolean isReducedValue(String value) {
        return "1".equals(value) || "true".equals(value) || "reduced".equals(value);
    }

    /**
     * Plays the single startup reveal: catalog first, then the OS strip, then
     * the tag pane. The ledger stays still.
     *
     * @param catalog the center catalog pane
     * @param osStrip the operating-system strip content
     * @param tagPane the right tag pane
     */
    public static void startupReveal(Node catalog, Node osStrip, Node tagPane) {
        if (isReduced()) {
            return;
        }
        Platform.runLater(() -> {
            fadeIn(catalog, Duration.ZERO);
            slideIn(osStrip, -8, Duration.millis(60));
            fadeIn(tagPane, Duration.millis(120));
        });
    }

    /** Fades a node from 45% to full opacity to acknowledge a refresh. */
    public static void crossfadeOnRefresh(Node node) {
        tick(node, 0.45, RESPONSE);
    }

    /** Fades a node from 60% to full opacity for low-key list updates. */
    public static void tick(Node node) {
        tick(node, 0.6, RESPONSE);
    }

    /** Fades in a freshly selected OS tab with a small settle slide. */
    public static void pulseSelect(Node node) {
        if (node == null || isReduced()) {
            return;
        }
        stopPrevious(node, "motion-pulse");
        FadeTransition fade = new FadeTransition(RESPONSE, node);
        fade.setFromValue(0.4);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(RESPONSE, node);
        slide.setFromX(-4);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition both = new ParallelTransition(fade, slide);
        track(node, "motion-pulse", both);
        both.play();
    }

    /**
     * Animates a dialog as an index card being pulled: fade plus a 6px rise
     * and a 0.97 scale settle. Hooks {@code onShown} because the dialog pane
     * has no scene until it is displayed. Existing {@code onShown} handlers
     * are kept and run first.
     *
     * @param dialog the dialog to animate
     */
    public static void dialogEntrance(Dialog<?> dialog) {
        if (dialog == null || isReduced()) {
            return;
        }
        DialogPane pane = dialog.getDialogPane();
        javafx.event.EventHandler<javafx.scene.control.DialogEvent> previous = dialog.getOnShown();
        dialog.setOnShown(event -> {
            if (previous != null) {
                previous.handle(event);
            }
            FadeTransition fade = new FadeTransition(ENTRANCE, pane);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);
            TranslateTransition rise = new TranslateTransition(ENTRANCE, pane);
            rise.setFromY(6);
            rise.setToY(0);
            rise.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition settle = new ScaleTransition(ENTRANCE, pane);
            settle.setFromX(0.97);
            settle.setFromY(0.97);
            settle.setToX(1.0);
            settle.setToY(1.0);
            settle.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, rise, settle).play();
        });
    }

    /** Shakes a dialog once shown, marking a rejected validation or error. */
    public static void shakeOnShown(Dialog<?> dialog) {
        if (dialog == null || isReduced()) {
            return;
        }
        DialogPane pane = dialog.getDialogPane();
        javafx.event.EventHandler<javafx.scene.control.DialogEvent> previous = dialog.getOnShown();
        dialog.setOnShown(event -> {
            if (previous != null) {
                previous.handle(event);
            }
            javafx.animation.PauseTransition wait =
                    new javafx.animation.PauseTransition(Duration.millis(180));
            wait.setOnFinished(done -> shake(pane));
            wait.play();
        });
    }

    /** Shakes a node sideways to mark a validation failure. */
    public static void shake(Node node) {
        if (node == null || isReduced()) {
            return;
        }
        stopPrevious(node, "motion-shake");
        Timeline shake = new Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(node.translateXProperty(), 0, Interpolator.LINEAR)),
                new javafx.animation.KeyFrame(Duration.millis(50),
                        new javafx.animation.KeyValue(node.translateXProperty(), -6, Interpolator.LINEAR)),
                new javafx.animation.KeyFrame(Duration.millis(110),
                        new javafx.animation.KeyValue(node.translateXProperty(), 5, Interpolator.LINEAR)),
                new javafx.animation.KeyFrame(Duration.millis(165),
                        new javafx.animation.KeyValue(node.translateXProperty(), -3, Interpolator.LINEAR)),
                new javafx.animation.KeyFrame(Duration.millis(220),
                        new javafx.animation.KeyValue(node.translateXProperty(), 0, Interpolator.EASE_OUT)));
        track(node, "motion-shake", shake);
        shake.play();
    }

    /** Rotates a toggle graphic once (for example the theme sun/moon swap). */
    public static void spinOnce(Node graphic) {
        if (graphic == null || isReduced()) {
            return;
        }
        stopPrevious(graphic, "motion-spin");
        RotateTransition spin = new RotateTransition(Duration.millis(200), graphic);
        spin.setByAngle(180);
        spin.setInterpolator(Interpolator.EASE_OUT);
        track(graphic, "motion-spin", spin);
        spin.play();
    }

    /** Adds a subtle press squash to a button; release always restores scale. */
    public static void pressScale(ButtonBase button) {
        if (button == null || isReduced()) {
            return;
        }
        button.pressedProperty().addListener((observable, wasPressed, pressed) -> {
            ScaleTransition press = new ScaleTransition(PRESS, button);
            press.setToX(pressed ? 0.98 : 1.0);
            press.setToY(pressed ? 0.98 : 1.0);
            press.setInterpolator(Interpolator.EASE_OUT);
            press.play();
        });
    }

    private static void fadeIn(Node node, Duration delay) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(ENTRANCE, node);
        fade.setFromValue(0);
        fade.setToValue(1.0);
        fade.setDelay(delay);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }

    private static void slideIn(Node node, double fromX, Duration delay) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(240), node);
        fade.setFromValue(0);
        fade.setToValue(1.0);
        fade.setDelay(delay);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(240), node);
        slide.setFromX(fromX);
        slide.setToX(0);
        slide.setDelay(delay);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private static void tick(Node node, double fromOpacity, Duration duration) {
        if (node == null || node.getScene() == null || isReduced()) {
            return;
        }
        stopPrevious(node, "motion-tick");
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(fromOpacity);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        track(node, "motion-tick", fade);
        fade.play();
    }

    private static void stopPrevious(Node node, String key) {
        Object previous = node.getProperties().get(key);
        if (previous instanceof Animation animation) {
            animation.stop();
        }
    }

    private static void track(Node node, String key, Animation animation) {
        node.getProperties().put(key, animation);
        animation.setOnFinished(event -> node.getProperties().remove(key));
    }
}
