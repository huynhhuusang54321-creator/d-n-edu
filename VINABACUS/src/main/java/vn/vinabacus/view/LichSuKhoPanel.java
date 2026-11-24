
package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import vn.vinabacus.controller.LichSuKhoController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.LichSuKho;

public class LichSuKhoPanel extends JPanel {

    private final CoSo coSo;
    private final LichSuKhoController controller;

    private final DefaultTableModel model;
    private final JTable bangLichSu;
    private final TableRowSorter<DefaultTableModel> sorter;

    private final JTextField txtTimKiem;
    private final JComboBox<String> cboLoaiPhieu;
    private final JComboBox<String> cboTrangThai;
    private final JButton btnLamMoi;

    private static final DateTimeFormatter DINH_DANG_NGAY =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public LichSuKhoPanel(CoSo coSo) {

        this.coSo = coSo;
        this.controller = new LichSuKhoController();

        setLayout(new BorderLayout(10, 10));

        // ===== PHAN TREN =====

        JPanel panelTren = new JPanel(
            new BorderLayout(0, 10)
        );

        
        // ===== THANH CONG CU =====

        JPanel thanhCongCu = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 10, 5)
        );

        btnLamMoi = new JButton("Làm mới");

        txtTimKiem = new JTextField(17);

        cboLoaiPhieu = new JComboBox<>(
            new String[]{
                "Tất cả loại phiếu",
                "Tồn đầu kỳ",
                "Nhập kho",
                "Xuất cho học viên",
                "Xuất cho cơ sở",
                "Điều chỉnh kho"
            }
        );

        cboTrangThai = new JComboBox<>(
            new String[]{
                "Tất cả trạng thái",
                "Đã xác nhận",
                "Phiếu nháp",
                "Đã hủy"
            }
        );

        thanhCongCu.add(btnLamMoi);
        thanhCongCu.add(new JLabel("Tìm kiếm:"));
        thanhCongCu.add(txtTimKiem);
        thanhCongCu.add(new JLabel("Loại phiếu:"));
        thanhCongCu.add(cboLoaiPhieu);
        thanhCongCu.add(new JLabel("Trạng thái:"));
        thanhCongCu.add(cboTrangThai);

        panelTren.add(thanhCongCu, BorderLayout.CENTER);

        add(panelTren, BorderLayout.NORTH);

        // ===== BANG LICH SU =====

        String[] tenCot = {
            "Mã phiếu",
            "Ngày lập",
            "Loại phiếu",
            "Mã sản phẩm",
            "Tên hàng hóa",
            "Đơn vị tính",
            "Số lượng",
            "Trạng thái",
            "Ghi chú"
        };

        model = new DefaultTableModel(tenCot, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bangLichSu = new JTable(model);
        bangLichSu.setRowHeight(30);

        bangLichSu.setFont(
            new Font("Arial", Font.PLAIN, 13)
        );

        bangLichSu.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 13)
        );

        bangLichSu.getTableHeader()
                .setReorderingAllowed(false);

        bangLichSu.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        sorter = new TableRowSorter<>(model);
        bangLichSu.setRowSorter(sorter);

        add(
            new JScrollPane(bangLichSu),
            BorderLayout.CENTER
        );

        // ===== SU KIEN =====

        btnLamMoi.addActionListener(
            e -> taiLichSu()
        );

        cboLoaiPhieu.addActionListener(
            e -> apDungBoLoc()
        );

        cboTrangThai.addActionListener(
            e -> apDungBoLoc()
        );

        txtTimKiem.getDocument().addDocumentListener(
            new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    apDungBoLoc();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    apDungBoLoc();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    apDungBoLoc();
                }
            }
        );

        taiLichSu();
    }

    // ===== DINH DANG SO =====

    private String dinhDangSo(BigDecimal giaTri) {

        if (giaTri == null) {
            return "0";
        }

        return giaTri.stripTrailingZeros()
                .toPlainString();
    }

    // ===== CHUYEN LOAI PHIEU =====

    private String tenLoaiPhieu(String loaiPhieu) {

        if (loaiPhieu == null) {
            return "";
        }

        return switch (loaiPhieu) {
            case "TON_DAU" -> "Tồn đầu kỳ";
            case "NHAP" -> "Nhập kho";
            case "XUAT_HS" -> "Xuất cho học viên";
            case "XUAT_CS" -> "Xuất cho cơ sở";
            case "DIEU_CHINH" -> "Điều chỉnh kho";
            default -> loaiPhieu;
        };
    }

    // ===== CHUYEN TRANG THAI =====

    private String tenTrangThai(String trangThai) {

        if (trangThai == null) {
            return "";
        }

        return switch (trangThai) {
            case "XAC_NHAN" -> "Đã xác nhận";
            case "NHAP" -> "Phiếu nháp";
            case "HUY" -> "Đã hủy";
            default -> trangThai;
        };
    }

    // ===== TAI LICH SU =====

    private void taiLichSu() {

        try {

            List<LichSuKho> danhSach =
                controller.layLichSu(coSo);

            model.setRowCount(0);

            for (LichSuKho ls : danhSach) {

                String ngayLap = ls.getNgayLap() == null
                    ? ""
                    : ls.getNgayLap().format(DINH_DANG_NGAY);

                // Hien thi so luong xuat voi dau tru
                BigDecimal soLuong = ls.getSoLuong();

                if (soLuong != null
                        && ("XUAT_HS".equals(ls.getLoaiPhieu())
                            || "XUAT_CS".equals(ls.getLoaiPhieu()))) {

                    soLuong = soLuong.negate();
                }

                model.addRow(new Object[]{
                    ls.getMaPhieuKho(),
                    ngayLap,
                    tenLoaiPhieu(ls.getLoaiPhieu()),
                    ls.getMaSanPham(),
                    ls.getTenHangHoa(),
                    ls.getDonViTinh(),
                    dinhDangSo(soLuong),
                    tenTrangThai(ls.getTrangThai()),
                    ls.getGhiChu() == null ? "" : ls.getGhiChu()
                });
            }

            // Giu nguyen bo loc khi lam moi
            apDungBoLoc();

        } catch (SQLException | IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                this,
                "Không thể tải lịch sử kho:\n" + e.getMessage(),
                "Lỗi dữ liệu",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ===== LOC TU DONG =====

    private void apDungBoLoc() {

        String tuKhoa = txtTimKiem.getText()
                .trim()
                .toLowerCase(Locale.ROOT);

        String loaiPhieu =
            (String) cboLoaiPhieu.getSelectedItem();

        String trangThai =
            (String) cboTrangThai.getSelectedItem();

        sorter.setRowFilter(
            new RowFilter<DefaultTableModel, Integer>() {

                @Override
                public boolean include(
                        Entry<? extends DefaultTableModel,
                              ? extends Integer> entry) {

                    String maPhieu = String.valueOf(
                        entry.getValue(0)
                    ).toLowerCase(Locale.ROOT);

                    String maSanPham = String.valueOf(
                        entry.getValue(3)
                    ).toLowerCase(Locale.ROOT);

                    String tenHangHoa = String.valueOf(
                        entry.getValue(4)
                    ).toLowerCase(Locale.ROOT);

                    String loaiPhieuHang = String.valueOf(
                        entry.getValue(2)
                    );

                    String trangThaiHang = String.valueOf(
                        entry.getValue(7)
                    );

                    boolean khopTuKhoa =
                        tuKhoa.isEmpty()
                        || maPhieu.contains(tuKhoa)
                        || maSanPham.contains(tuKhoa)
                        || tenHangHoa.contains(tuKhoa);

                    boolean khopLoai =
                        "Tất cả loại phiếu".equals(loaiPhieu)
                        || loaiPhieuHang.equals(loaiPhieu);

                    boolean khopTrangThai =
                        "Tất cả trạng thái".equals(trangThai)
                        || trangThaiHang.equals(trangThai);

                    return khopTuKhoa
                        && khopLoai
                        && khopTrangThai;
                }
            }
        );
    }
}
