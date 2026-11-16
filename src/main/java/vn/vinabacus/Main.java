
package vn.vinabacus;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import vn.vinabacus.controller.MenuController;
import vn.vinabacus.service.MenuService;
import vn.vinabacus.view.MainFrame;
import vn.vinabacus.view.DangKyManHinh;


public final class Main {

    private Main() {}

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

           

            // Mo giao dien VINABACUS
            MainFrame frame = new MainFrame(
                new MenuController(new MenuService())
            );

            frame.setVisible(true);
        });
    }
}
