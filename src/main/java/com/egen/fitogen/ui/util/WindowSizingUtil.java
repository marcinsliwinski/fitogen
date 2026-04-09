package com.egen.fitogen.ui.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class WindowSizingUtil {

    private static final double DEFAULT_SCREEN_USAGE = 0.94;
    private static final double DEFAULT_MIN_SCREEN_USAGE = 0.72;

    private WindowSizingUtil() {
    }

    public static double resolveInitialWidth(double preferredWidth) {
        Rectangle2D bounds = getVisualBounds();
        return Math.max(640, Math.min(preferredWidth, bounds.getWidth() * DEFAULT_SCREEN_USAGE));
    }

    public static double resolveInitialHeight(double preferredHeight) {
        Rectangle2D bounds = getVisualBounds();
        return Math.max(480, Math.min(preferredHeight, bounds.getHeight() * DEFAULT_SCREEN_USAGE));
    }

    public static double resolveMinWidth(double preferredMinWidth) {
        Rectangle2D bounds = getVisualBounds();
        return Math.max(560, Math.min(preferredMinWidth, bounds.getWidth() * DEFAULT_MIN_SCREEN_USAGE));
    }

    public static double resolveMinHeight(double preferredMinHeight) {
        Rectangle2D bounds = getVisualBounds();
        return Math.max(420, Math.min(preferredMinHeight, bounds.getHeight() * DEFAULT_MIN_SCREEN_USAGE));
    }

    public static void applyStageSize(
            Stage stage,
            double preferredWidth,
            double preferredHeight,
            double preferredMinWidth,
            double preferredMinHeight
    ) {
        if (stage == null) {
            return;
        }

        double initialWidth = resolveInitialWidth(preferredWidth);
        double initialHeight = resolveInitialHeight(preferredHeight);
        double minWidth = Math.min(initialWidth, resolveMinWidth(preferredMinWidth));
        double minHeight = Math.min(initialHeight, resolveMinHeight(preferredMinHeight));

        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        stage.setWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.centerOnScreen();
    }

    private static Rectangle2D getVisualBounds() {
        Screen screen = Screen.getPrimary();
        if (screen == null) {
            return new Rectangle2D(0, 0, 1366, 768);
        }
        return screen.getVisualBounds();
    }
}
