package vn.vinabacus;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import vn.vinabacus.controller.DieuKhienPhanHe;
import vn.vinabacus.view.GiaoDienChinh;
import vn.vinabacus.view.GiaoDienPhanHe;
import vn.vinabacus.model.CoSo;

/** Chạy với desktop hoặc Xvfb: dựng cửa sổ, tất cả menu và tất cả tab của hai cơ sở. */
public final class KiemThuDieuHuong {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GiaoDienChinh frame = new GiaoDienChinh();
            int menuCount = 0, tabCount = 0;
            try {
                assert frame.layCoSo() == CoSo.MAM_NON;
                frame.setVisible(true);
                Method chonTab = GiaoDienPhanHe.class.getDeclaredMethod("hienTab", int.class);
                chonTab.setAccessible(true);
                for (CoSo coSo : CoSo.values()) {
                    frame.chonCoSo(coSo);
                    assert frame.layCoSo() == coSo;
                    DieuKhienPhanHe controller = new DieuKhienPhanHe(coSo);
                    for (String menu : coSo.layMenu()) {
                        frame.moMuc(menu);
                        frame.validate();
                        menuCount++;
                        if (menu.equals("Trang chủ")) continue;
                        GiaoDienPhanHe view = tim(frame, GiaoDienPhanHe.class);
                        assert view != null : coSo + "/" + menu;
                        for (int index = 0; index < controller.layTrang(menu).size(); index++) {
                            chonTab.invoke(view, index);
                            frame.validate();
                            tabCount++;
                        }
                    }
                }
                frame.chonCoSo(CoSo.MAM_NON);
                frame.moMuc("Trang chủ");
                System.out.println("PASS: cửa sổ Swing, " + menuCount + " menu, " + tabCount
                        + " tab, chuyển cơ sở và trở về trang chủ.");
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Không dựng được tab giao diện", ex);
            } finally {
                frame.dispose();
            }
        });
    }

    private static <T> T tim(Component component, Class<T> type) {
        if (type.isInstance(component)) return type.cast(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = tim(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
