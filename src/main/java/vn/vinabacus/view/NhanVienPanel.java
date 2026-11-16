
package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import vn.vinabacus.controller.NhanVienController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.NhanVien;

public class NhanVienPanel extends JPanel {

    private final JTable bangNhanVien;
    private final DefaultTableModel model;
    private final NhanVienController controller;

    public NhanVienPanel(CoSo coSo) {

        setLayout(new BorderLayout(10, 10));

        controller = new NhanVienController();

        // Tieu de giao dien
        JLabel tieuDe = new JLabel(
            "DANH SACH NHAN VIEN - GIAO VIEN"
        );

        tieuDe.setFont(new Font("Arial", Font.BOLD, 20));

        add(tieuDe, BorderLayout.NORTH);

        // Cac cot trong bang
        String[] tenCot = {
            "Ma NV",
            "Ho ten",
            "Ngay sinh",
            "Gioi tinh",
            "So dien thoai",
            "Email",
            "Trang thai",
            "Tai co so"
        };

        model = new DefaultTableModel(tenCot, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bangNhanVien = new JTable(model);
        bangNhanVien.setRowHeight(30);

        add(new JScrollPane(bangNhanVien),
            BorderLayout.CENTER);

        // Lay du lieu SQL Server
        taiDanhSach(coSo);
    }

    private void taiDanhSach(CoSo coSo) {

        try {
            List<NhanVien> danhSach =
                controller.layDanhSach(coSo);

            model.setRowCount(0);

            for (NhanVien nv : danhSach) {

                Object[] dong = {
                    nv.getMaNhanVien(),
                    nv.getHoTen(),
                    nv.getNgaySinh(),
                    nv.getGioiTinh(),
                    nv.getSoDienThoai(),
                    nv.getEmail(),
                    nv.isTrangThai()
                        ? "Dang lam" : "Da nghi",
                    nv.isTrangThaiTaiCoSo()
                        ? "Dang cong tac" : "Ngung cong tac"
                };

                model.addRow(dong);
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                this,
                "Loi tai danh sach nhan vien:\n"
                    + e.getMessage(),
                "Loi SQL Server",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
