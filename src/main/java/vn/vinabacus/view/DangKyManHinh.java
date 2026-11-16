package vn.vinabacus.view;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * Cho phep nhom gan JPanel cua thanh vien vao ma menu.
 * Khong can tao ModuleView, khong ep moi module phai co nut Them/Sua/Xoa.
 * Khong dang ky san bat ky module hay du lieu mau nao.
 *
 * Vi du (sau khi thanh vien hoan thanh):
 *   DangKyManHinh.dangKy("tt-mon-hoc", MonHocPanel::new);
 */
public final class DangKyManHinh {
    private static final Map<String, Supplier<? extends JComponent>> DANH_SACH =
            new HashMap<>();

    private DangKyManHinh() {}

    public static void dangKy(String maMenu, Supplier<? extends JComponent> factory) {
        if (maMenu == null || maMenu.isBlank()) {
            throw new IllegalArgumentException("Mã menu không được để trống");
        }
        Objects.requireNonNull(factory, "Module factory không được null");
        if (DANH_SACH.putIfAbsent(maMenu, factory) != null) {
            throw new IllegalArgumentException("Mã menu đã đăng ký: " + maMenu);
        }
    }

    public static JComponent lay(String maMenu) {
        Supplier<? extends JComponent> factory = DANH_SACH.get(maMenu);
        return factory == null ? null : Objects.requireNonNull(
                factory.get(), "JPanel của module không được null: " + maMenu);
    }
}
