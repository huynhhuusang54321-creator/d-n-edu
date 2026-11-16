package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import vn.vinabacus.controller.HangHoaController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HangHoa;

public class HangHoaPanel extends JPanel {

    private final CoSo coSo;
    private final HangHoaController controller;
    private final DefaultTableModel model;
    private final JTable bangHangHoa;
    private final TableRowSorter<DefaultTableModel> sorter;

    private final JButton btnThem;
    private final JButton btnNhapKho;
    private final JButton btnXuatKho;
    private final JButton btnSua;
    private final JButton btnNgungSuDung;
    private final JButton btnSuDungTroLai;
    private final JButton btnLamMoi;

    private final JTextField txtTimKiem;
    private final JComboBox<String> cboTrangThai;
    private String trangThaiDangLoc = "Tất cả";

    public HangHoaPanel(CoSo coSo) {
        this.coSo = coSo;
        this.controller = new HangHoaController();
        setLayout(new BorderLayout(8, 8));

        JLabel tieuDe = new JLabel("QUẢN LÝ KHO - DỤNG CỤ");
        tieuDe.setFont(new Font("Arial", Font.BOLD, 20));
        tieuDe.setBorder(BorderFactory.createEmptyBorder(5, 8, 3, 0));
        add(tieuDe, BorderLayout.NORTH);

        // ===== CAC NUT CHI THUOC TAB HANG HOA =====
        btnThem = new JButton("Thêm hàng hóa");
        btnSua = new JButton("Sửa hàng hóa");
        btnNgungSuDung = new JButton("Ngừng sử dụng");
        btnSuDungTroLai = new JButton("Sử dụng trở lại");
        btnNhapKho = new JButton("Nhập kho (xem trước)");
        btnXuatKho = new JButton("Xuất kho (xem trước)");
        btnLamMoi = new JButton("Làm mới");

        JPanel panelQuanLy = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelQuanLy.setBorder(BorderFactory.createTitledBorder("Quản lý hàng hóa"));
        panelQuanLy.add(btnThem);
        panelQuanLy.add(btnSua);
        panelQuanLy.add(btnNgungSuDung);
        panelQuanLy.add(btnSuDungTroLai);

        JPanel panelNghiepVu = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelNghiepVu.setBorder(BorderFactory.createTitledBorder("Nghiệp vụ kho - chưa lưu dữ liệu"));
        panelNghiepVu.add(btnNhapKho);
        panelNghiepVu.add(btnXuatKho);

        txtTimKiem = new JTextField(21);
        cboTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Đang sử dụng", "Ngừng sử dụng"
        });
        JPanel panelTimKiem = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelTimKiem.setBorder(BorderFactory.createTitledBorder("Tìm kiếm và lọc"));
        panelTimKiem.add(new JLabel("Tìm kiếm:"));
        panelTimKiem.add(txtTimKiem);
        panelTimKiem.add(new JLabel("Trạng thái:"));
        panelTimKiem.add(cboTrangThai);
        panelTimKiem.add(btnLamMoi);

        JPanel thanhCongCu = new JPanel(new GridLayout(3, 1, 0, 0));
        thanhCongCu.add(panelQuanLy);
        thanhCongCu.add(panelNghiepVu);
        thanhCongCu.add(panelTimKiem);

        // ===== BANG HANG HOA =====
        String[] tenCot = {
            "Mã hàng hóa", "Mã sản phẩm", "Tên hàng hóa", "Đơn vị tính",
            "Số lượng tồn", "Giá sỉ", "Giá xuất bán", "Trạng thái"
        };
        model = new DefaultTableModel(tenCot, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Integer.class : String.class;
            }
        };
        bangHangHoa = new JTable(model);
        bangHangHoa.setRowHeight(32);
        bangHangHoa.setFont(new Font("Arial", Font.PLAIN, 13));
        bangHangHoa.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        bangHangHoa.getTableHeader().setReorderingAllowed(false);
        bangHangHoa.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(model);
        bangHangHoa.setRowSorter(sorter);

        JPanel tabHangHoa = new JPanel(new BorderLayout(0, 6));
        tabHangHoa.add(thanhCongCu, BorderLayout.NORTH);
        tabHangHoa.add(new JScrollPane(bangHangHoa), BorderLayout.CENTER);

        // Tab Lich su tu quan ly cong cu tim kiem/loc cua chinh no.
        // Khong tao nut "Lich su" bi trung voi tab.
        JTabbedPane tabKho = new JTabbedPane();
        tabKho.addTab("Hàng hóa & tồn kho", tabHangHoa);
        tabKho.addTab("Lịch sử nhập - xuất", new LichSuKhoPanel(coSo));
        tabKho.addTab("Báo cáo tồn kho", new BaoCaoTonKhoPanel(coSo));
        add(tabKho, BorderLayout.CENTER);

        // ===== SU KIEN =====
        btnThem.addActionListener(e -> moFormThemHangHoa());
        btnSua.addActionListener(e -> moFormSuaHangHoa());
        btnNgungSuDung.addActionListener(e -> capNhatTrangThai(false));
        btnSuDungTroLai.addActionListener(e -> capNhatTrangThai(true));
        btnNhapKho.addActionListener(e -> moFormNhapKho());
        btnXuatKho.addActionListener(e -> moFormXuatKho());
        btnLamMoi.addActionListener(e -> taiDanhSach());

        // Chon trang thai la loc ngay, khong can nut "Loc".
        cboTrangThai.addActionListener(e -> {
            trangThaiDangLoc = (String) cboTrangThai.getSelectedItem();
            apDungBoLoc();
        });
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override
            public void removeUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override
            public void changedUpdate(DocumentEvent e) { apDungBoLoc(); }
        });

        // Chi cho thao tac voi hang da chon. Tuy trang thai ma khoa nut.
        bangHangHoa.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNutTheoDongChon();
            }
        });

        taiDanhSach();
        capNhatNutTheoDongChon();
    }

    // ===== BAT/TAT NUT TUY THEO HANG DUOC CHON =====
    private void capNhatNutTheoDongChon() {
        int dongHienThi = bangHangHoa.getSelectedRow();
        boolean coChon = dongHienThi >= 0;
        boolean dangSuDung = false;
        if (coChon) {
            int dongModel = bangHangHoa.convertRowIndexToModel(dongHienThi);
            dangSuDung = "Đang sử dụng".equals(
                String.valueOf(model.getValueAt(dongModel, 7))
            );
        }
        btnSua.setEnabled(coChon);
        btnNgungSuDung.setEnabled(coChon && dangSuDung);
        btnSuDungTroLai.setEnabled(coChon && !dangSuDung);
        btnNhapKho.setEnabled(coChon && dangSuDung);
        btnXuatKho.setEnabled(coChon && dangSuDung);
    }

    // ========== DINH DANG SO ==========
    private String dinhDangSo(BigDecimal giaTri) {

        if (giaTri == null) {
            return "0";
        }

        return giaTri.stripTrailingZeros().toPlainString();
    }

    // ========== TAI DANH SACH ==========
    private void taiDanhSach() {

        try {

            List<HangHoa> danhSach
                    = controller.layDanhSach(coSo);

            model.setRowCount(0);

            for (HangHoa hh : danhSach) {

                model.addRow(new Object[]{
                    hh.getMaHangHoa(),
                    hh.getMaSanPham(),
                    hh.getTenHangHoa(),
                    hh.getDonViTinh(),
                    dinhDangSo(hh.getSoLuongTon()),
                    dinhDangSo(hh.getDonGiaSi()),
                    dinhDangSo(hh.getDonGiaXuatBan()),
                    hh.isTrangThai()
                    ? "Đang sử dụng"
                    : "Ngừng sử dụng"
                });
            }

            // Giu nguyen bo loc dang su dung
            apDungBoLoc();
            capNhatNutTheoDongChon();

        } catch (SQLException | IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Không thể tải danh sách:\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ========== TIM KIEM VA LOC TRANG THAI ==========
    private void apDungBoLoc() {

        String tuKhoa = txtTimKiem.getText()
                .trim()
                .toLowerCase(Locale.ROOT);

        String trangThai = trangThaiDangLoc;

        if (tuKhoa.isEmpty()
                && "Tất cả".equals(trangThai)) {

            sorter.setRowFilter(null);
            capNhatNutTheoDongChon();
            return;
        }

        sorter.setRowFilter(
                new RowFilter<DefaultTableModel, Integer>() {

            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel, ? extends Integer> entry) {

                String maSanPham = String.valueOf(
                        entry.getValue(1)
                ).toLowerCase(Locale.ROOT);

                String tenHangHoa = String.valueOf(
                        entry.getValue(2)
                ).toLowerCase(Locale.ROOT);

                String donViTinh = String.valueOf(
                        entry.getValue(3)
                ).toLowerCase(Locale.ROOT);

                String trangThaiHang = String.valueOf(
                        entry.getValue(7)
                );

                // Loc theo tu khoa
                boolean khopTuKhoa
                        = tuKhoa.isEmpty()
                        || maSanPham.contains(tuKhoa)
                        || tenHangHoa.contains(tuKhoa)
                        || donViTinh.contains(tuKhoa);

                // Loc theo trang thai
                boolean khopTrangThai
                        = "Tất cả".equals(trangThai)
                        || trangThaiHang.equals(trangThai);

                return khopTuKhoa && khopTrangThai;
            }
        }
        );
        capNhatNutTheoDongChon();
    }

    // ===== DANH SACH DON VI TINH =====
    private JComboBox<String> taoComboDonViTinh(String giaTriCu) {
        JComboBox<String> cbo = new JComboBox<>(new String[]{
            "-- Chọn đơn vị tính --",
            "Cái", "Chiếc", "Cây", "Quyển", "Cuốn", "Bộ",
            "Hộp", "Tập", "Tờ", "Gói", "Thùng", "Chai",
            "Lọ", "Lốc", "Bịch", "Cuộn", "Kg", "Mét", "Lít"
        });
        cbo.setEditable(false);

        // Giu don vi tinh cu ngay ca khi khong co trong danh sach.
        if (giaTriCu != null && !giaTriCu.trim().isEmpty()) {
            String cu = giaTriCu.trim();
            for (int i = 0; i < cbo.getItemCount(); i++) {
                if (cu.equalsIgnoreCase(cbo.getItemAt(i))) {
                    cbo.setSelectedIndex(i);
                    return cbo;
                }
            }
            cbo.addItem(cu);
            cbo.setSelectedItem(cu);
        } else {
            cbo.setSelectedIndex(0);
        }
        return cbo;
    }

    private String layDonViTinh(JComboBox<String> cbo) {
        String donVi = (String) cbo.getSelectedItem();
        if (donVi == null
                || donVi.isBlank()
                || "-- Chọn đơn vị tính --".equals(donVi)) {
            throw new IllegalArgumentException("Vui lòng chọn đơn vị tính!");
        }
        return donVi.trim();
    }

    // ========== TAO FORM NHAP ==========
    private JPanel taoForm(
            JTextField txtMaSanPham,
            JTextField txtTenHangHoa,
            JComboBox<String> cboDonViTinh,
            JTextField txtGiaSi,
            JTextField txtGiaXuatBan) {

        JPanel form = new JPanel(
                new GridLayout(0, 2, 10, 10)
        );

        form.add(new JLabel("Mã sản phẩm:"));
        form.add(txtMaSanPham);

        form.add(new JLabel("Tên hàng hóa:"));
        form.add(txtTenHangHoa);

        form.add(new JLabel("Đơn vị tính:"));
        form.add(cboDonViTinh);

        form.add(new JLabel("Giá sỉ:"));
        form.add(txtGiaSi);

        form.add(new JLabel("Giá xuất bán:"));
        form.add(txtGiaXuatBan);

        return form;
    }

    // ========== THEM HANG HOA ==========
    private void moFormThemHangHoa() {

        JTextField txtMaSanPham = new JTextField(20);
        JTextField txtTenHangHoa = new JTextField(20);
        JComboBox<String> cboDonViTinh = taoComboDonViTinh(null);
        JTextField txtGiaSi = new JTextField(20);
        JTextField txtGiaXuatBan = new JTextField(20);

        JPanel form = taoForm(
                txtMaSanPham,
                txtTenHangHoa,
                cboDonViTinh,
                txtGiaSi,
                txtGiaXuatBan
        );

        int luaChon = JOptionPane.showConfirmDialog(
                this,
                form,
                "THÊM HÀNG HÓA",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (luaChon != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            BigDecimal giaSi = new BigDecimal(
                    txtGiaSi.getText().trim()
            );

            BigDecimal giaXuatBan = new BigDecimal(
                    txtGiaXuatBan.getText().trim()
            );

            int maMoi = controller.themHangHoa(
                    coSo,
                    txtMaSanPham.getText(),
                    txtTenHangHoa.getText(),
                    layDonViTinh(cboDonViTinh),
                    giaSi,
                    giaXuatBan
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Thêm hàng hóa thành công!\n"
                    + "Mã hàng hóa: " + maMoi
            );

            taiDanhSach();

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Giá sỉ và giá bán phải là số hợp lệ!",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Dữ liệu không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Không thể thêm hàng hóa:\n" + e.getMessage(),
                    "Lỗi SQL Server",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ========== SUA HANG HOA ==========
    private void moFormSuaHangHoa() {

        int dongChon = bangHangHoa.getSelectedRow();

        if (dongChon == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn hàng hóa cần sửa!"
            );
            return;
        }

        int dongModel
                = bangHangHoa.convertRowIndexToModel(dongChon);

        int maHangHoa = ((Number) model.getValueAt(
                dongModel, 0
        )).intValue();

        JTextField txtMaSanPham = new JTextField(
                model.getValueAt(dongModel, 1).toString(), 20
        );

        JTextField txtTenHangHoa = new JTextField(
                model.getValueAt(dongModel, 2).toString(), 20
        );

        JComboBox<String> cboDonViTinh = taoComboDonViTinh(
                model.getValueAt(dongModel, 3).toString()
        );

        JTextField txtGiaSi = new JTextField(
                model.getValueAt(dongModel, 5).toString(), 20
        );

        JTextField txtGiaXuatBan = new JTextField(
                model.getValueAt(dongModel, 6).toString(), 20
        );

        JPanel form = taoForm(
                txtMaSanPham,
                txtTenHangHoa,
                cboDonViTinh,
                txtGiaSi,
                txtGiaXuatBan
        );

        int luaChon = JOptionPane.showConfirmDialog(
                this,
                form,
                "SỬA HÀNG HÓA - MÃ " + maHangHoa,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (luaChon != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            BigDecimal giaSi = new BigDecimal(
                    txtGiaSi.getText().trim()
            );

            BigDecimal giaXuatBan = new BigDecimal(
                    txtGiaXuatBan.getText().trim()
            );

            boolean ketQua = controller.suaHangHoa(
                    coSo,
                    maHangHoa,
                    txtMaSanPham.getText(),
                    txtTenHangHoa.getText(),
                    layDonViTinh(cboDonViTinh),
                    giaSi,
                    giaXuatBan
            );

            if (ketQua) {

                JOptionPane.showMessageDialog(
                        this,
                        "Cập nhật hàng hóa thành công!"
                );

                taiDanhSach();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Không tìm thấy hàng hóa cần sửa!"
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Giá sỉ và giá bán phải là số hợp lệ!",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Dữ liệu không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Không thể sửa hàng hóa:\n" + e.getMessage(),
                    "Lỗi SQL Server",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ========== CAP NHAT TRANG THAI ==========
    private void capNhatTrangThai(boolean trangThaiMoi) {

        int dongChon = bangHangHoa.getSelectedRow();

        if (dongChon == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn hàng hóa!"
            );
            return;
        }

        int dongModel
                = bangHangHoa.convertRowIndexToModel(dongChon);

        int maHangHoa = ((Number) model.getValueAt(
                dongModel, 0
        )).intValue();

        String tenHangHoa = model.getValueAt(
                dongModel, 2
        ).toString();

        String trangThaiHienTai = model.getValueAt(
                dongModel, 7
        ).toString();

        boolean dangSuDung
                = trangThaiHienTai.equals("Đang sử dụng");

        if (dangSuDung == trangThaiMoi) {

            JOptionPane.showMessageDialog(
                    this,
                    trangThaiMoi
                            ? "Hàng hóa này đang được sử dụng!"
                            : "Hàng hóa này đã ngừng sử dụng!"
            );
            return;
        }

        String hanhDong = trangThaiMoi
                ? "Sử dụng trở lại"
                : "Ngừng sử dụng";

        int xacNhan = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn "
                + hanhDong.toLowerCase()
                + " hàng hóa:\n"
                + tenHangHoa + "?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION
        );

        if (xacNhan != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            boolean ketQua
                    = controller.capNhatTrangThai(
                            coSo,
                            maHangHoa,
                            trangThaiMoi
                    );

            if (ketQua) {

                JOptionPane.showMessageDialog(
                        this,
                        hanhDong + " thành công!"
                );

                taiDanhSach();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Không tìm thấy hàng hóa cần cập nhật!"
                );
            }

        } catch (SQLException | IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi cập nhật trạng thái:\n"
                    + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void moFormNhapKho() {

        // Lay hang hoa dang chon
        int dongChon = bangHangHoa.getSelectedRow();

        if (dongChon == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn hàng hóa cần nhập kho!"
            );
            return;
        }

        // Lay dung dong khi dang loc hoac sap xep
        int dongModel
                = bangHangHoa.convertRowIndexToModel(dongChon);

        String tenHangHoa
                = model.getValueAt(dongModel, 2).toString();

        String donViTinh
                = model.getValueAt(dongModel, 3).toString();

        String trangThai
                = model.getValueAt(dongModel, 7).toString();

        if (!trangThai.equals("Đang sử dụng")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Hàng hóa đã ngừng sử dụng, không thể nhập kho!"
            );
            return;
        }

        BigDecimal tonHienTai = new BigDecimal(
                model.getValueAt(dongModel, 4).toString()
        );

        // Tao cac o nhap
        JTextField txtSoLuong = new JTextField(15);
        JTextField txtGhiChu = new JTextField(20);

        JPanel form = new JPanel(
                new GridLayout(0, 2, 10, 10)
        );

        form.add(new JLabel("Tên hàng hóa:"));
        form.add(new JLabel(tenHangHoa));

        form.add(new JLabel("Đơn vị tính:"));
        form.add(new JLabel(donViTinh));

        form.add(new JLabel("Tồn hiện tại:"));
        form.add(new JLabel(
                dinhDangSo(tonHienTai)
        ));

        form.add(new JLabel("Số lượng nhập:"));
        form.add(txtSoLuong);

        form.add(new JLabel("Ghi chú:"));
        form.add(txtGhiChu);

        // Hien thi form
        int luaChon = JOptionPane.showConfirmDialog(
                this,
                form,
                "NHẬP KHO - XEM TRƯỚC",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (luaChon != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            BigDecimal soLuongNhap = new BigDecimal(
                    txtSoLuong.getText().trim()
            );

            soLuongNhap = soLuongNhap.stripTrailingZeros();

            // Kiem tra so luong
            if (soLuongNhap.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Số lượng nhập phải lớn hơn 0!"
                );
                return;
            }

            if (soLuongNhap.scale() > 3
                    || soLuongNhap.precision()
                    - soLuongNhap.scale() > 15) {
                JOptionPane.showMessageDialog(
                        this,
                        "Số lượng nhập không hợp lệ!"
                );
                return;
            }

            String ghiChu = txtGhiChu.getText().trim();

            if (ghiChu.length() > 500) {
                JOptionPane.showMessageDialog(
                        this,
                        "Ghi chú tối đa 500 ký tự!"
                );
                return;
            }

            // Tinh ton kho du kien
            BigDecimal tonSauNhap
                    = tonHienTai.add(soLuongNhap);

            JOptionPane.showMessageDialog(
                    this,
                    "Hàng hóa: " + tenHangHoa
                    + "\nTồn hiện tại: "
                    + dinhDangSo(tonHienTai)
                    + "\nSố lượng nhập: "
                    + dinhDangSo(soLuongNhap)
                    + "\nTồn dự kiến: "
                    + dinhDangSo(tonSauNhap)
                    + "\nGhi chú: " + ghiChu
                    + "\n\nĐây chỉ là bản xem trước."
                    + "\nChưa lưu phiếu nhập vào SQL Server.",
                    "XEM TRƯỚC NHẬP KHO",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Số lượng phải là số hợp lệ!\n"
                    + "Ví dụ: 10 hoặc 2.5",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void moFormXuatKho() {

        // Lay hang hoa dang chon
        int dongChon = bangHangHoa.getSelectedRow();

        if (dongChon == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn hàng hóa cần xuất kho!"
            );
            return;
        }

        // Lay dung dong khi dang tim kiem hoac loc
        int dongModel
                = bangHangHoa.convertRowIndexToModel(dongChon);

        String tenHangHoa
                = model.getValueAt(dongModel, 2).toString();

        String donViTinh
                = model.getValueAt(dongModel, 3).toString();

        String trangThai
                = model.getValueAt(dongModel, 7).toString();

        if (!trangThai.equals("Đang sử dụng")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Hàng hóa đã ngừng sử dụng!"
            );
            return;
        }

        BigDecimal tonHienTai = new BigDecimal(
                model.getValueAt(dongModel, 4).toString()
        );

        // Tao form xuat kho
        JComboBox<String> cboLoaiXuat
                = new JComboBox<>(new String[]{
            "Xuất cho học viên",
            "Xuất cho cơ sở"
        });

        JTextField txtSoLuong = new JTextField(15);
        JTextField txtGhiChu = new JTextField(20);

        JPanel form = new JPanel(
                new GridLayout(0, 2, 10, 10)
        );

        form.add(new JLabel("Tên hàng hóa:"));
        form.add(new JLabel(tenHangHoa));

        form.add(new JLabel("Đơn vị tính:"));
        form.add(new JLabel(donViTinh));

        form.add(new JLabel("Tồn hiện tại:"));
        form.add(new JLabel(dinhDangSo(tonHienTai)));

        form.add(new JLabel("Loại xuất kho:"));
        form.add(cboLoaiXuat);

        form.add(new JLabel("Số lượng xuất:"));
        form.add(txtSoLuong);

        form.add(new JLabel("Ghi chú:"));
        form.add(txtGhiChu);

        int luaChon = JOptionPane.showConfirmDialog(
                this,
                form,
                "XUẤT KHO - XEM TRƯỚC",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (luaChon != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            BigDecimal soLuongXuat = new BigDecimal(
                    txtSoLuong.getText().trim()
            ).stripTrailingZeros();

            // Kiem tra so luong xuat
            if (soLuongXuat.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Số lượng xuất phải lớn hơn 0!"
                );
                return;
            }

            if (soLuongXuat.scale() > 3
                    || soLuongXuat.precision()
                    - soLuongXuat.scale() > 15) {

                JOptionPane.showMessageDialog(
                        this,
                        "Số lượng xuất không hợp lệ!"
                );
                return;
            }

            // Khong cho xuat vuot ton kho
            if (soLuongXuat.compareTo(tonHienTai) > 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Không đủ hàng trong kho!\n"
                        + "Tồn hiện tại: "
                        + dinhDangSo(tonHienTai)
                );
                return;
            }

            String ghiChu = txtGhiChu.getText().trim();

            if (ghiChu.length() > 500) {
                JOptionPane.showMessageDialog(
                        this,
                        "Ghi chú tối đa 500 ký tự!"
                );
                return;
            }

            // Tinh ton kho du kien sau khi xuat
            BigDecimal tonSauXuat
                    = tonHienTai.subtract(soLuongXuat);

            String loaiXuat
                    = (String) cboLoaiXuat.getSelectedItem();

            JOptionPane.showMessageDialog(
                    this,
                    "Hàng hóa: " + tenHangHoa
                    + "\nLoại xuất: " + loaiXuat
                    + "\nTồn hiện tại: "
                    + dinhDangSo(tonHienTai)
                    + "\nSố lượng xuất: "
                    + dinhDangSo(soLuongXuat)
                    + "\nTồn dự kiến: "
                    + dinhDangSo(tonSauXuat)
                    + "\nGhi chú: " + ghiChu
                    + "\n\nĐây chỉ là bản xem trước."
                    + "\nChưa lưu phiếu xuất vào SQL Server.",
                    "XEM TRƯỚC XUẤT KHO",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Số lượng phải là số hợp lệ!\n"
                    + "Ví dụ: 5 hoặc 2.5",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

}
