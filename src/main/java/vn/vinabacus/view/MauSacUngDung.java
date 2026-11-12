package vn.vinabacus.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

/** Chỉ cần chỉnh màu tại đây để đồng bộ toàn bộ hai cơ sở. */
public final class MauSacUngDung {
    private MauSacUngDung() {}
    public static final Color XANH = new Color(0x2E7D32);
    public static final Color XANH_DAM = new Color(0x1B5E20);
    public static final Color XANH_MENU = new Color(0x005633);
    public static final Color XANH_MENU_DAM = new Color(0x003D27);
    public static final Color XANH_SANG = new Color(0xDEFFD1);
    public static final Color VANG = new Color(0xF4C542);
    public static final Color NEN = new Color(0xF7F9F8);
    public static final Color TRANG = Color.WHITE;
    public static final Color VIEN = new Color(0xDEE5DE);
    public static final Color CHU = new Color(0x263238);
    public static final Color PHU = new Color(0x657177);
    public static final Color DO = new Color(0xF44353);
    public static final Color XANH_DUONG = new Color(0x1678E9);
    public static final Color VANG_TUOI = new Color(0xFFBE13);
    public static final Color NAVY = new Color(0x111848);
    private static final String FONT = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()).contains("Segoe UI") ? "Segoe UI" : "SansSerif";
    public static Font font(int size, boolean dam) { return new Font(FONT, dam ? Font.BOLD : Font.PLAIN, size); }
}
