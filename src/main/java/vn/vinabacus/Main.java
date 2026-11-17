
package vn.vinabacus;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import vn.vinabacus.controller.MenuController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.service.MenuService;
import vn.vinabacus.tv2.TV2Module;
import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.HangHoaPanel;
import vn.vinabacus.view.MainFrame;
import vn.vinabacus.view.NhanVienPanel;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            // ===== TV6: NHAN SU =====

            // Nhan su Trung tam
            DangKyManHinh.dangKy(
                    "tt-nhan-su",
                    () -> new NhanVienPanel(CoSo.TRUNG_TAM)
            );

            // Nhan su Mam non
            DangKyManHinh.dangKy(
                    "mn-nhan-su",
                    () -> new NhanVienPanel(CoSo.MAM_NON)
            );

            // ===== TV6: KHO - DUNG CU =====

            // Kho Trung tam
            DangKyManHinh.dangKy(
                    "tt-kho",
                    () -> new HangHoaPanel(CoSo.TRUNG_TAM)
            );

            // Kho Mam non
            DangKyManHinh.dangKy(
                    "mn-kho",
                    () -> new HangHoaPanel(CoSo.MAM_NON)
            );

            // ===== TV2: HOC VIEN - TUYEN SINH =====

            TV2Module.dangKy();

            // ===== MO GIAO DIEN CHINH =====

            MainFrame frame = new MainFrame(
                    new MenuController(new MenuService())
            );

            frame.setVisible(true);
        });
    }
}
