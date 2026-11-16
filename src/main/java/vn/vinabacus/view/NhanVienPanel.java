package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import java.util.regex.Pattern;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.JTextField;
import javax.swing.JComboBox;

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

        JPanel panelDau = new JPanel(
                new BorderLayout(10, 8)
        );

// Tieu de nam o tren
        panelDau.add(tieuDe, BorderLayout.NORTH);

// Thanh cong cu: nut + tim kiem + loc
        JPanel thanhCongCu = new JPanel(
                new BorderLayout(10, 0)
        );

// O tim kiem ben phai
        JTextField txtTimKiem = new JTextField(18);

        JPanel panelTimKiem = new JPanel();

        panelTimKiem.add(
                new JLabel("Tìm nhân viên:")
        );
        panelTimKiem.add(txtTimKiem);

        JComboBox<String> cboLocTrangThai = new JComboBox<>(
                new String[]{
                    "Tất cả",
                    "Đang công tác",
                    "Ngừng công tác"
                }
        );

        panelTimKiem.add(new JLabel("Trạng thái:"));
        panelTimKiem.add(cboLocTrangThai);

        thanhCongCu.add(
                panelTimKiem,
                BorderLayout.EAST
        );

// Dua thanh cong cu len tren bang
        panelDau.add(
                thanhCongCu,
                BorderLayout.CENTER
        );

        add(panelDau, BorderLayout.NORTH);

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

        TableRowSorter<DefaultTableModel> sorter
                = new TableRowSorter<>(model);

        bangNhanVien.setRowSorter(sorter);

// Loc danh sach khi nguoi dung nhap
        Runnable timKiem = () -> {

            String tuKhoa = txtTimKiem.getText().trim();

            String trangThai = (String) cboLocTrangThai.getSelectedItem();

            java.util.List<RowFilter<DefaultTableModel, Integer>> danhSachLoc = new java.util.ArrayList<>();

            // Tim theo ma, ten hoac so dien thoai
            if (!tuKhoa.isEmpty()) {
                danhSachLoc.add(
                        RowFilter.regexFilter(
                                "(?iu)" + Pattern.quote(tuKhoa),
                                0, 1, 4
                        )
                );
            }

            // Loc nhan vien dang cong tac
            if ("Đang công tác".equals(trangThai)) {
                danhSachLoc.add(
                        RowFilter.regexFilter(
                                "^Dang cong tac$", 7
                        )
                );
            }

            // Loc nhan vien ngung cong tac
            if ("Ngừng công tác".equals(trangThai)) {
                danhSachLoc.add(
                        RowFilter.regexFilter(
                                "^Ngung cong tac$", 7
                        )
                );
            }

            bangNhanVien.clearSelection();

            if (danhSachLoc.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(
                        RowFilter.andFilter(danhSachLoc)
                );
            }
        };

        cboLocTrangThai.addActionListener(e -> {
            timKiem.run();
        });

        txtTimKiem.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {

            @Override
            public void insertUpdate(
                    javax.swing.event.DocumentEvent e) {
                timKiem.run();
            }

            @Override
            public void removeUpdate(
                    javax.swing.event.DocumentEvent e) {
                timKiem.run();
            }

            @Override
            public void changedUpdate(
                    javax.swing.event.DocumentEvent e) {
                timKiem.run();
            }
        }
        );

        add(new JScrollPane(bangNhanVien),
                BorderLayout.CENTER);

        JButton btnThem = new JButton("Thêm nhân viên");

        btnThem.addActionListener(e -> {

            // Tao cac o nhap lieu
            JTextField txtHoTen = new JTextField(20);
            JTextField txtNgaySinh = new JTextField(20);
            JTextField txtSoDienThoai = new JTextField(20);
            JTextField txtEmail = new JTextField(20);

            JComboBox<String> cboGioiTinh = new JComboBox<>(
                    new String[]{"Chưa chọn", "NAM", "NU", "KHAC"}
            );

            // Tao bieu mau
            JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));

            form.add(new JLabel("Họ tên:"));
            form.add(txtHoTen);

            form.add(new JLabel("Ngày sinh (yyyy-MM-dd):"));
            form.add(txtNgaySinh);

            form.add(new JLabel("Giới tính:"));
            form.add(cboGioiTinh);

            form.add(new JLabel("Số điện thoại:"));
            form.add(txtSoDienThoai);

            form.add(new JLabel("Email:"));
            form.add(txtEmail);

            // Hien thi hop thoai them nhan vien
            int ketQua = JOptionPane.showConfirmDialog(
                    this,
                    form,
                    "Thêm nhân viên",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            // Neu nhan Huy thi khong them
            if (ketQua != JOptionPane.OK_OPTION) {
                return;
            }

            try {

                String hoTen = txtHoTen.getText().trim();
                String ngaySinhChuoi = txtNgaySinh.getText().trim();

                LocalDate ngaySinh = null;

                if (!ngaySinhChuoi.isEmpty()) {
                    ngaySinh = LocalDate.parse(ngaySinhChuoi);
                }

                String gioiTinh = null;

                if (cboGioiTinh.getSelectedIndex() > 0) {
                    gioiTinh = (String) cboGioiTinh.getSelectedItem();
                }

                String soDienThoai = txtSoDienThoai.getText().trim();
                String email = txtEmail.getText().trim();

                // Goi Controller de luu vao SQL Server
                int maNhanVien = controller.themNhanVien(
                        coSo,
                        hoTen,
                        ngaySinh,
                        gioiTinh,
                        soDienThoai,
                        email
                );

                JOptionPane.showMessageDialog(
                        this,
                        "Thêm nhân viên thành công!\nMã NV: "
                        + maNhanVien
                );

                // Cap nhat lai bang nhan vien
                taiDanhSach(coSo);

            } catch (DateTimeParseException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Ngày sinh không hợp lệ!\n"
                        + "Vui lòng nhập theo dạng yyyy-MM-dd.",
                        "Lỗi ngày sinh",
                        JOptionPane.ERROR_MESSAGE
                );

            } catch (SQLException | IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Không thể thêm nhân viên",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        JButton btnSua = new JButton("Sửa nhân viên");
        JButton btnNgung = new JButton("Ngừng công tác");
        JButton btnTroLai = new JButton("Làm việc trở lại");
// Tao khung chua 3 nut
        JPanel panelNut = new JPanel(
                new GridLayout(1, 4, 10, 0)
        );

// Them cac nut vao khung
        panelNut.add(btnThem);
        panelNut.add(btnSua);
        panelNut.add(btnNgung);
        panelNut.add(btnTroLai);
// Tam khoa cac nut chua duoc chon
        btnSua.setEnabled(false);
        btnNgung.setEnabled(false);
        btnTroLai.setEnabled(false);
// Hien thi cac nut phia duoi bang

        thanhCongCu.add(
                panelNut,
                BorderLayout.WEST
        );

        bangNhanVien.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {

                int dong = bangNhanVien.getSelectedRow();

                // Neu chua chon nhan vien
                if (dong < 0) {
                    btnSua.setEnabled(false);
                    btnNgung.setEnabled(false);
                    btnTroLai.setEnabled(false);
                    return;
                }

                // Cho phep sua nhan vien da chon
                btnSua.setEnabled(true);

                // Lay trang thai tu bang
                String trangThaiNhanVien = String.valueOf(
                        bangNhanVien.getValueAt(dong, 6)
                );

                String trangThaiCoSo = String.valueOf(
                        bangNhanVien.getValueAt(dong, 7)
                );

                boolean hoSoHoatDong
                        = trangThaiNhanVien.equals("Dang lam");

                boolean dangCongTac
                        = trangThaiCoSo.equals("Dang cong tac");

                // Nhan vien dang cong tac
                btnNgung.setEnabled(dangCongTac);

                // Nhan vien da ngung cong tac,
                // nhung ho so van hoat dong
                btnTroLai.setEnabled(
                        hoSoHoatDong && !dangCongTac
                );
            }
        });

        btnTroLai.addActionListener(e -> {

            // Kiem tra dong duoc chon
            int dong = bangNhanVien.getSelectedRow();

            if (dong < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Vui lòng chọn nhân viên!"
                );
                return;
            }

            // Lay thong tin nhan vien
            int maNhanVien = Integer.parseInt(
                    bangNhanVien.getValueAt(dong, 0).toString()
            );

            String hoTen = bangNhanVien
                    .getValueAt(dong, 1).toString();

            // Hoi xac nhan
            int xacNhan = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có muốn cho nhân viên:\n"
                    + hoTen
                    + "\n(Mã NV: " + maNhanVien + ")"
                    + "\nlàm việc trở lại không?",
                    "Xác nhận làm việc trở lại",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (xacNhan != JOptionPane.YES_OPTION) {
                return;
            }

            try {

                boolean thanhCong = controller.lamViecTroLai(
                        coSo,
                        maNhanVien
                );

                if (thanhCong) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Nhân viên đã làm việc trở lại!"
                    );

                    // Tai lai danh sach
                    taiDanhSach(coSo);

                    // Bo chon dong cu
                    bangNhanVien.clearSelection();

                    btnTroLai.setEnabled(false);
                    btnNgung.setEnabled(false);
                    btnSua.setEnabled(false);

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Không thể kích hoạt lại nhân viên!"
                    );
                }

            } catch (SQLException | IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Lỗi làm việc trở lại",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        btnNgung.addActionListener(e -> {

            // Kiem tra nhan vien duoc chon
            int dong = bangNhanVien.getSelectedRow();

            if (dong < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Vui lòng chọn nhân viên!"
                );
                return;
            }

            // Lay ma va ten nhan vien
            int maNhanVien = Integer.parseInt(
                    bangNhanVien.getValueAt(dong, 0).toString()
            );

            String hoTen = bangNhanVien
                    .getValueAt(dong, 1).toString();

            // Hoi xac nhan truoc khi cap nhat
            int xacNhan = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn ngừng công tác nhân viên:\n"
                    + hoTen + " (Mã NV: " + maNhanVien + ")?",
                    "Xác nhận ngừng công tác",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (xacNhan != JOptionPane.YES_OPTION) {
                return;
            }

            try {

                // Goi Controller cap nhat trang thai
                boolean thanhCong = controller.ngungCongTac(
                        coSo,
                        maNhanVien
                );

                if (thanhCong) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Đã ngừng công tác nhân viên thành công!"
                    );

                    // Tai lai danh sach
                    taiDanhSach(coSo);

                    btnNgung.setEnabled(false);

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Nhân viên không còn đang công tác "
                            + "tại cơ sở này!"
                    );
                }

            } catch (SQLException | IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Lỗi ngừng công tác",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        btnSua.addActionListener(e -> {

            int dong = bangNhanVien.getSelectedRow();

            if (dong < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Vui lòng chọn nhân viên cần sửa!"
                );
                return;
            }

            // Lay thong tin cu tu bang
            int maNhanVien = Integer.parseInt(
                    bangNhanVien.getValueAt(dong, 0).toString()
            );

            String hoTenCu = String.valueOf(
                    bangNhanVien.getValueAt(dong, 1)
            );

            Object ngaySinhCu = bangNhanVien.getValueAt(dong, 2);
            Object gioiTinhCu = bangNhanVien.getValueAt(dong, 3);
            Object sdtCu = bangNhanVien.getValueAt(dong, 4);
            Object emailCu = bangNhanVien.getValueAt(dong, 5);

            // Tao cac o nhap, dien san du lieu cu
            JTextField txtHoTen = new JTextField(hoTenCu);

            JTextField txtNgaySinh = new JTextField(
                    ngaySinhCu == null ? "" : ngaySinhCu.toString()
            );

            JComboBox<String> cboGioiTinh = new JComboBox<>(
                    new String[]{"Chưa chọn", "NAM", "NU", "KHAC"}
            );

            if (gioiTinhCu != null) {
                cboGioiTinh.setSelectedItem(gioiTinhCu.toString());
            }

            JTextField txtSoDienThoai = new JTextField(
                    sdtCu == null ? "" : sdtCu.toString()
            );

            JTextField txtEmail = new JTextField(
                    emailCu == null ? "" : emailCu.toString()
            );

            // Tao bieu mau sua
            JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));

            form.add(new JLabel("Họ tên:"));
            form.add(txtHoTen);

            form.add(new JLabel("Ngày sinh (yyyy-MM-dd):"));
            form.add(txtNgaySinh);

            form.add(new JLabel("Giới tính:"));
            form.add(cboGioiTinh);

            form.add(new JLabel("Số điện thoại:"));
            form.add(txtSoDienThoai);

            form.add(new JLabel("Email:"));
            form.add(txtEmail);

            // Mo hop thoai sua
            int ketQua = JOptionPane.showConfirmDialog(
                    this,
                    form,
                    "Sửa nhân viên - Mã " + maNhanVien,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (ketQua != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                String hoTen = txtHoTen.getText().trim();

                String ngaySinhChuoi
                        = txtNgaySinh.getText().trim();

                LocalDate ngaySinh = null;

                if (!ngaySinhChuoi.isEmpty()) {
                    ngaySinh = LocalDate.parse(ngaySinhChuoi);
                }

                String gioiTinh = null;

                if (cboGioiTinh.getSelectedIndex() > 0) {
                    gioiTinh
                            = (String) cboGioiTinh.getSelectedItem();
                }

                String soDienThoai
                        = txtSoDienThoai.getText().trim();

                String email = txtEmail.getText().trim();

                // Cap nhat vao SQL Server
                boolean thanhCong = controller.suaNhanVien(
                        coSo,
                        maNhanVien,
                        hoTen,
                        ngaySinh,
                        gioiTinh,
                        soDienThoai,
                        email
                );

                if (thanhCong) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Cập nhật nhân viên thành công!"
                    );

                    taiDanhSach(coSo);

                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Không tìm thấy nhân viên tại cơ sở này!"
                    );
                }

            } catch (DateTimeParseException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Ngày sinh không hợp lệ! Dùng yyyy-MM-dd.",
                        "Lỗi ngày sinh",
                        JOptionPane.ERROR_MESSAGE
                );

            } catch (SQLException | IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Lỗi cập nhật nhân viên",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // Lay du lieu SQL Server
        taiDanhSach(coSo);
    }

    private void taiDanhSach(CoSo coSo) {

        try {
            List<NhanVien> danhSach
                    = controller.layDanhSach(coSo);

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
