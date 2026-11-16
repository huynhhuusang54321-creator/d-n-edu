package vn.vinabacus;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import vn.vinabacus.controller.MenuController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.service.MenuService;
import vn.vinabacus.view.MainFrame;
import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.NhanVienPanel;
import vn.vinabacus.view.HangHoaPanel;

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
            // Dang ky giao dien nhan su Trung tam
            DangKyManHinh.dangKy(
                    "tt-nhan-su",
                    () -> new NhanVienPanel(CoSo.TRUNG_TAM)
            );

            // Dang ky giao dien nhan su Mam non
            DangKyManHinh.dangKy(
                    "mn-nhan-su",
                    () -> new NhanVienPanel(CoSo.MAM_NON)
            );

            // Giao dien kho Trung tam
            DangKyManHinh.dangKy(
                    "tt-kho",
                    () -> new HangHoaPanel(CoSo.TRUNG_TAM)
            );

// Giao dien kho Mam non
            DangKyManHinh.dangKy(
                    "mn-kho",
                    () -> new HangHoaPanel(CoSo.MAM_NON)
            );

            // Mo giao dien VINABACUS
            MainFrame frame = new MainFrame(
                    new MenuController(new MenuService())
            );

            frame.setVisible(true);
        });
    }
}
