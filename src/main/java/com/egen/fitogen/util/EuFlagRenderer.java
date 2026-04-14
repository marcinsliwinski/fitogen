package com.egen.fitogen.util;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class EuFlagRenderer {

    private static final java.awt.Color EU_BLUE = new java.awt.Color(0x00, 0x33, 0x99);
    private static final java.awt.Color EU_YELLOW = new java.awt.Color(0xFF, 0xCC, 0x00);

    private EuFlagRenderer() {
    }

    public static Image createFxImage(int width, int height) {
        return new Image(new ByteArrayInputStream(createPngBytes(width, height)));
    }

    public static byte[] createPngBytes(int width, int height) {
        int safeWidth = Math.max(24, width);
        int safeHeight = Math.max(16, height);
        BufferedImage image = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(EU_BLUE);
            g.fillRect(0, 0, safeWidth, safeHeight);

            double cx = safeWidth / 2.0;
            double cy = safeHeight / 2.0;
            double radius = Math.min(safeWidth, safeHeight) * 0.34;
            double outer = Math.min(safeWidth, safeHeight) * 0.072;
            double inner = outer * 0.42;

            g.setColor(EU_YELLOW);
            g.setStroke(new BasicStroke(Math.max(1f, Math.min(safeWidth, safeHeight) * 0.008f)));
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(-90 + i * 30);
                double sx = cx + Math.cos(angle) * radius;
                double sy = cy + Math.sin(angle) * radius;
                Path2D star = createStar(sx, sy, outer, inner);
                g.fill(star);
            }

            if (!hasReferenceSvg()) {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, safeHeight / 12)));
                FontMetrics metrics = g.getFontMetrics();
                String note = "EU";
                int textWidth = metrics.stringWidth(note);
                g.drawString(note, (safeWidth - textWidth) / 2, safeHeight - metrics.getDescent());
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Nie udało się wygenerować flagi UE.", ex);
        }
    }

    private static boolean hasReferenceSvg() {
        try (InputStream in = EuFlagRenderer.class.getResourceAsStream("/images/Flag_of_Europe.svg")) {
            return in != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static Path2D createStar(double centerX, double centerY, double outerRadius, double innerRadius) {
        Path2D path = new Path2D.Double();
        for (int point = 0; point < 10; point++) {
            double angle = Math.toRadians(-90 + point * 36);
            double radius = point % 2 == 0 ? outerRadius : innerRadius;
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(angle) * radius;
            if (point == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();
        return path;
    }
}
