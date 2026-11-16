package vn.vinabacus.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/** Ve icon nho bang Java2D, khong can file anh va thu vien ngoai. */
public final class BieuTuong implements Icon {
    private final String loai;
    private final int size;
    public BieuTuong(String loai, int size) {
        this.loai = loai;
        this.size = size;
    }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component component, Graphics gg, int x, int y) {
        Graphics2D g = (Graphics2D) gg.create();
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(component.getForeground() == null ? Color.WHITE : component.getForeground());
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (loai) {
            case "logo" -> {
                g.drawPolyline(new int[]{12, 21, 21, 12, 3, 3, 12},
                        new int[]{2, 7, 17, 22, 17, 7, 2}, 7);
            }
            case "home" -> {
                g.drawLine(3, 11, 12, 4); g.drawLine(12, 4, 21, 11);
                g.drawRect(6, 11, 12, 10); g.drawRect(10, 15, 4, 6);
            }
            case "people" -> {
                g.drawOval(8, 3, 8, 8); g.drawArc(4, 11, 16, 13, 0, 180);
                g.drawArc(0, 8, 9, 13, 0, 130); g.drawArc(15, 8, 9, 13, 50, 130);
            }
            case "book" -> {
                g.drawLine(12, 5, 12, 20); g.drawPolyline(new int[]{3, 7, 12}, new int[]{6, 5, 7}, 3);
                g.drawPolyline(new int[]{12, 17, 21}, new int[]{7, 5, 6}, 3);
                g.drawPolyline(new int[]{3, 7, 12}, new int[]{6, 19, 21}, 3);
                g.drawPolyline(new int[]{12, 17, 21}, new int[]{21, 19, 6}, 3);
            }
            case "layers" -> {
                g.drawPolyline(new int[]{3, 12, 21, 12, 3}, new int[]{9, 4, 9, 14, 9}, 5);
                g.drawPolyline(new int[]{3, 12, 21}, new int[]{14, 19, 14}, 3);
                g.drawPolyline(new int[]{3, 12, 21}, new int[]{18, 23, 18}, 3);
            }
            case "calendar" -> {
                g.drawRect(3, 5, 18, 16); g.drawLine(3, 10, 21, 10);
                g.drawLine(8, 3, 8, 7); g.drawLine(16, 3, 16, 7);
                g.fillOval(7, 13, 3, 3); g.fillOval(14, 13, 3, 3);
            }
            case "check" -> {
                g.drawRoundRect(3, 3, 18, 18, 3, 3);
                g.drawPolyline(new int[]{7, 11, 18}, new int[]{12, 17, 8}, 3);
            }
            case "money" -> {
                g.drawOval(3, 3, 18, 18); g.drawLine(12, 5, 12, 19);
                g.drawArc(8, 6, 8, 7, -30, 215); g.drawArc(8, 11, 8, 7, 150, 215);
            }
            case "warehouse" -> {
                g.drawPolyline(new int[]{2, 12, 22}, new int[]{9, 3, 9}, 3);
                g.drawRect(5, 10, 14, 11); g.drawRect(9, 14, 6, 7);
            }
            case "chart" -> {
                g.drawLine(3, 21, 21, 21); g.drawLine(3, 21, 3, 3);
                g.drawRect(6, 13, 3, 8); g.drawRect(11, 9, 3, 12); g.drawRect(16, 5, 3, 16);
            }
            case "settings" -> {
                g.drawOval(6, 6, 12, 12); g.drawOval(10, 10, 4, 4);
                for (int d = 0; d < 8; d++) {
                    double a = d * Math.PI / 4;
                    g.drawLine((int)(12+9*Math.cos(a)), (int)(12+9*Math.sin(a)),
                            (int)(12+11*Math.cos(a)), (int)(12+11*Math.sin(a)));
                }
            }
            default -> g.drawRoundRect(4, 4, 16, 16, 3, 3);
        }
        g.dispose();
    }
}
