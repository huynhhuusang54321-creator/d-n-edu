package vn.vinabacus.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/** Nút chính/phụ dùng chung cho màn hình lịch học và phòng học. */
public final class NutMau extends JButton {
    public enum Kieu { CHINH, PHU }

    private final Kieu kieu;

    public NutMau(String text, Kieu kieu) {
        super(text);
        this.kieu = java.util.Objects.requireNonNull(kieu);
        setFont(GiaoDienMau.font(Font.BOLD, 13));
        setForeground(kieu == Kieu.CHINH ? Color.WHITE : GiaoDienMau.XANH_DAM);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = kieu == Kieu.CHINH
                ? (getModel().isRollover() ? GiaoDienMau.XANH_NUT : GiaoDienMau.XANH_DAM)
                : (getModel().isRollover() ? GiaoDienMau.XANH_NHAT : Color.WHITE);
        if (!isEnabled()) fill = GiaoDienMau.DUONG_VIEN;
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
        if (kieu == Kieu.PHU) {
            g.setColor(GiaoDienMau.DUONG_VIEN);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
