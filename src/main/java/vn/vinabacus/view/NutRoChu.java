package vn.vinabacus.view;

import java.awt.*;
import javax.swing.*;

/** Nut tu ve, chu/icon tuong phan trong ca Windows Look & Feel. */
public final class NutRoChu extends JButton {
    public NutRoChu(String text, Icon icon) {
        super(text, icon);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = getBackground();
        if (getModel().isRollover() && !fill.equals(GiaoDienMau.VANG)) {
            fill = new Color(11, 115, 50);
        }
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 10, 10);
        g.dispose();
        super.paintComponent(graphics);
    }
}
