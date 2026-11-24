package vn.vinabacus;

import java.util.HashSet;
import java.util.Set;
import vn.vinabacus.config.MenuConfig;
import vn.vinabacus.controller.MenuController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MucMenu;
import vn.vinabacus.service.MenuService;
import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.KhungNoiDungPanel;

/** Test thu cong khong can JUnit, SQL Server, du lieu mau. */
public final class KiemThuMenu {
    public static void main(String[] args) {
        MenuController controller = new MenuController(new MenuService());
        for (CoSo coSo : CoSo.values()) {
            controller.chonCoSo(coSo);
            var ds = controller.layMenu();
            if (ds.size() != (coSo == CoSo.TRUNG_TAM ? 15 : 13)) throw new AssertionError("Sai so menu: " + coSo);
            Set<String> ids = new HashSet<>();
            for (MucMenu m : ds) {
                if (!ids.add(m.ma())) throw new AssertionError("Trung ma menu: " + m.ma());
                if (!m.ma().startsWith(coSo == CoSo.TRUNG_TAM ? "tt-" : "mn-"))
                    throw new AssertionError("Menu lan co so: " + m.ma());
                if (!controller.chonMenu(m.ma()).equals(m)) throw new AssertionError();
                if (DangKyManHinh.lay(m.ma()) != null)
                    throw new AssertionError("Khong duoc dang ky view mau: " + m.ma());
            }
            System.out.println(coSo + ": " + ds.size() + " menu, khong trung, khong co module/dulieu mau");
        }
        // Tai thoi diem ban giao, moi menu phai chi mo duoc khung trong.
        for (CoSo coSo : CoSo.values()) {
            controller.chonCoSo(coSo);
            for (MucMenu menu : controller.layMenu()) {
                if (new KhungNoiDungPanel(menu).getComponentCount() != 1)
                    throw new AssertionError("Khung noi dung khong trong: " + menu.ma());
            }
        }
        System.out.println("KIEM THU MENU: DAT");
    }
}
