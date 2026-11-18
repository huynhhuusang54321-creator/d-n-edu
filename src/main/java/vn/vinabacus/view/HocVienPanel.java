package vn.vinabacus.view;

import vn.vinabacus.controller.HocVienController;
import vn.vinabacus.model.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public final class HocVienPanel extends JPanel {

    private final HocVienController controller;
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{"Tất cả", "Đang hoạt động", "Ngừng sử dụng"});
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Mã", "Họ tên", "Ngày sinh", "Giới tính", "Người liên hệ", "SĐT", "Địa chỉ", "Trạng thái", "Ghi chú"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable bang = new JTable(model);
    private List<HocVien> danhSach = List.of();
    private final JLabel thongBao = new JLabel("Sẵn sàng");

    public HocVienPanel(CoSo coSo) {
        super(new BorderLayout(10, 10));
        this.controller = new HocVienController(coSo);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel tren = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("QUẢN LÝ " + (coSo == CoSo.MAM_NON ? "HỌC SINH MẦM NON" : "HỌC VIÊN TRUNG TÂM"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        tren.add(title, BorderLayout.NORTH);
        JPanel thaoTac = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton tim = new JButton("Tìm kiếm"), taiLai = new JButton("Tải lại"), them = new JButton("Thêm"), sua = new JButton("Sửa");
        thaoTac.add(new JLabel("Mã / tên / SĐT:"));
        thaoTac.add(txtTim);
        thaoTac.add(new JLabel("Trạng thái:"));
        thaoTac.add(locTrangThai);
        thaoTac.add(tim);
        thaoTac.add(taiLai);
        thaoTac.add(them);
        thaoTac.add(sua);
        tren.add(thaoTac, BorderLayout.CENTER);
        add(tren, BorderLayout.NORTH);
        bang.setRowHeight(28);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ngay hien thi dd/MM/yyyy nhung sap xep phai theo gia tri ngay thuc.
        TableRowSorter<DefaultTableModel> sapXep = new TableRowSorter<>(model);
        DateTimeFormatter ngayVN = DateTimeFormatter.ofPattern("dd/MM/uuuu");
        sapXep.setComparator(2, (java.util.Comparator<String>) (a, b) -> {
            if (a == null || a.isBlank()) return (b == null || b.isBlank()) ? 0 : -1;
            if (b == null || b.isBlank()) return 1;
            return LocalDate.parse(a, ngayVN).compareTo(LocalDate.parse(b, ngayVN));
        });
        bang.setRowSorter(sapXep);
        add(new JScrollPane(bang), BorderLayout.CENTER);
        add(thongBao, BorderLayout.SOUTH);
        tim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        taiLai.addActionListener(e -> {
            txtTim.setText("");
            locTrangThai.setSelectedIndex(0);
            taiDuLieu();
        });
        them.addActionListener(e -> form(null));
        sua.addActionListener(e -> {
            HocVien h = duocChon();
            if (h != null) {
                form(h);
            }
        });
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private String maTrangThai() {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 ->
                "HOAT_DONG";
            case 2 ->
                "NGUNG_SU_DUNG";
            default ->
                "TAT_CA";
        };
    }

    private void taiDuLieu() {
        thongBao.setText("Đang tải dữ liệu...");
        final String tuKhoa = txtTim.getText().trim(), trangThai = maTrangThai();
        SwingWorker<List<HocVien>, Void> w = new SwingWorker<>() {
            @Override
            protected List<HocVien> doInBackground() throws Exception {
                return controller.timKiem(tuKhoa, trangThai);
            }

            @Override
            protected void done() {
                try {
                    danhSach = get();
                    model.setRowCount(0);
                    for (HocVien h : danhSach) {
                        model.addRow(new Object[]{h.maHocVien(), h.hoTen(), NgaySinhPicker.hienThiNgay(h.ngaySinh()), h.gioiTinh(),
                            h.tenNguoiLienHe(), h.soDienThoaiLienHe(), h.diaChi(), h.trangThaiHoSo(), h.ghiChu()});
                    }
                    thongBao.setText("Tìm thấy " + danhSach.size() + " học viên");
                } catch (Exception ex) {
                    hienLoi(ex);
                }
            }
        };
        w.execute();
    }

    private HocVien duocChon() {
        int r = bang.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một học viên trong bảng");
            return null;
        }
        return danhSach.get(bang.convertRowIndexToModel(r));
    }

    private void form(HocVien cu) {
        JTextField ten = new JTextField(cu == null ? "" : cu.hoTen(), 24);
        NgaySinhPicker ngay = new NgaySinhPicker(cu == null ? null : cu.ngaySinh());
        JComboBox<String> gioiTinh = new JComboBox<>(new String[]{"", "NAM", "NU", "KHAC"});
        JTextField lienHe = new JTextField(cu == null ? "" : chuoi(cu.tenNguoiLienHe()));
        JTextField sdt = new JTextField(cu == null ? "" : chuoi(cu.soDienThoaiLienHe()));
        JTextField diaChi = new JTextField(cu == null ? "" : chuoi(cu.diaChi()));
        JTextField ghiChu = new JTextField(cu == null ? "" : chuoi(cu.ghiChu()));
        JComboBox<String> trangThai = new JComboBox<>(new String[]{"HOAT_DONG", "NGUNG_SU_DUNG"});
        if (cu != null) {
            gioiTinh.setSelectedItem(cu.gioiTinh() == null ? "" : cu.gioiTinh());
            trangThai.setSelectedItem(cu.trangThaiHoSo());
        }
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Họ tên (*)"));
        p.add(ten);
        p.add(new JLabel("Ngày sinh (dd/MM/yyyy)"));
        p.add(ngay);
        p.add(new JLabel("Giới tính"));
        p.add(gioiTinh);
        p.add(new JLabel("Người liên hệ"));
        p.add(lienHe);
        p.add(new JLabel("Số điện thoại"));
        p.add(sdt);
        p.add(new JLabel("Địa chỉ"));
        p.add(diaChi);
        p.add(new JLabel("Ghi chú"));
        p.add(ghiChu);
        p.add(new JLabel("Trạng thái hồ sơ"));
        p.add(trangThai);
        if (JOptionPane.showConfirmDialog(this, p, cu == null ? "Thêm học viên" : "Sửa học viên", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            LocalDate d = ngay.layNgay();
            HocVien h = new HocVien(cu == null ? 0 : cu.maHocVien(), 0, ten.getText(), d,
                    rongThanhNull((String) gioiTinh.getSelectedItem()), rongThanhNull(lienHe.getText()),
                    rongThanhNull(sdt.getText()), rongThanhNull(diaChi.getText()), rongThanhNull(ghiChu.getText()),
                    (String) trangThai.getSelectedItem());
            if (cu == null) {
                int id = controller.them(h);
                JOptionPane.showMessageDialog(this, "Đã thêm học viên mã " + id);
            } else {
                controller.sua(h);
                JOptionPane.showMessageDialog(this, "Đã lưu thay đổi");
            }
            taiDuLieu();
        } catch (SQLException | IllegalArgumentException ex) {
            hienLoi(ex);
        }
    }

    private static String chuoi(String s) {
        return s == null ? "" : s;
    }

    private static String rongThanhNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private void hienLoi(Exception ex) {
        Throwable t = ex;
        while (t.getCause() != null && t != t.getCause()) {
            t = t.getCause();
        }
        thongBao.setText("Lỗi: " + t.getMessage());
        JOptionPane.showMessageDialog(this, t.getMessage(), "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
