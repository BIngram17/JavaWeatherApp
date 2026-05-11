package weatherapp;

import javax.swing.*;
import java.awt.*;

public class BackgroundPanel extends JPanel {

    public BackgroundPanel() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2d = (Graphics2D) graphics.create();

        int width = getWidth();
        int height = getHeight();

        Color topColor = new Color(70, 130, 180);
        Color bottomColor = new Color(25, 25, 112);

        GradientPaint gradient = new GradientPaint(
                0,
                0,
                topColor,
                0,
                height,
                bottomColor
        );

        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(new Color(255, 255, 255, 45));

        for (int i = 0; i < width; i += 120) {
            g2d.fillOval(i, 60, 90, 35);
            g2d.fillOval(i + 35, 45, 80, 45);
            g2d.fillOval(i + 75, 60, 90, 35);
        }

        g2d.dispose();
    }
}
