package vn.vinabacus.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/** Bang mau tu bien hieu Vinabacus Can Tho: xanh la - vang - trang. */
public final class GiaoDienMau {
    private GiaoDienMau() {}

    public static final Color XANH_DAM = new Color(0, 87, 38);
    public static final Color XANH_THANH = new Color(0, 133, 34);
    public static final Color XANH_SANG = new Color(90, 205, 7);
    public static final Color XANH_NUT = new Color(0, 142, 47);
    public static final Color XANH_NHAT = new Color(215, 255, 213);
    public static final Color NEN = Color.WHITE;
    public static final Color DUONG_VIEN = new Color(216, 230, 216);
    public static final Color CHU = new Color(24, 53, 31);
    public static final Color CHU_MO = new Color(82, 94, 83);
    public static final Color VANG = new Color(255, 224, 0);

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }
    public static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
}
