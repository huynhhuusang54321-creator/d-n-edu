package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.KhoaHocController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.MonHoc;

/**
 * Khóa học chia theo môn học thành các tab động từ SQL Server.
 * Mã khóa học/mã môn học không hiển thị, nhưng vẫn dùng nội bộ để sửa chính xác.
 */
public final class KhoaHocPanel extends JPanel {
    private final KhoaHocController controller = new KhoaHocController(CoSo.TRUNG_TAM);
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Hoạt động", "Ngừng sử dụng"
    });
    private final JTabbedPane tabMonHoc = new JTabbedPane();
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final List<BangTheoMon> cacBang = new ArrayList<>();
    private int lanTaiDuLieu = 0;
    private Integer monCanChonSauLuu;

    /** Giữ danh sách gốc trong mỗi tab, không lệ thuộc vào cột mã ẩn. */
    private static final class BangTheoMon {
        final int maMonHoc;
        final JTable bang;
        final DefaultTableModel model;
        final List<KhoaHoc> khoaHoc = new ArrayList<>();

        BangTheoMon(int maMonHoc) {
            this.maMonHoc = maMonHoc;
            model = new DefaultTableModel(new String[]{
                    "Tên khóa học", "Số buổi", "Học phí mặc định", "Trạng thái"
            }, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            bang = new JTable(model);
            bang.setRowHeight(30);
            bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            bang.setAutoCreateRowSorter(true);
            bang.getColumnModel().getColumn(0).setPreferredWidth(280);
            bang.getColumnModel().getColumn(1).setPreferredWidth(85);
            bang.getColumnModel().getColumn(2).setPreferredWidth(160);
            bang.getColumnModel().getColumn(3).setPreferredWidth(125);
        }
    }

    /** Kết quả lấy đồng thời các môn và khóa học, tránh tab thiếu môn chưa có khóa. */
    private record DuLieu(List<MonHoc> monHoc, List<KhoaHoc> khoaHoc) { }

    public KhoaHocPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel tieuDe = new JLabel("QUẢN LÝ KHÓA HỌC – TRUNG TÂM");
        tieuDe.setFont(tieuDe.getFont().deriveFont(Font.BOLD, 22f));

        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton btnTim = new JButton("Tìm kiếm");
        JButton btnTaiLai = new JButton("Tải lại");
        JButton btnThem = new JButton("Thêm");
        JButton btnSua = new JButton("Sửa");
        hangNut.add(new JLabel("Tên khóa học:"));
        hangNut.add(txtTim);
        hangNut.add(new JLabel("Trạng thái:"));
        hangNut.add(locTrangThai);
        hangNut.add(btnTim);
        hangNut.add(btnTaiLai);
        hangNut.add(btnThem);
        hangNut.add(btnSua);

        JPanel dau = new JPanel(new BorderLayout(6, 8));
        dau.add(tieuDe, BorderLayout.NORTH);
        dau.add(hangNut, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);
        add(tabMonHoc, BorderLayout.CENTER);
        add(thongBao, BorderLayout.SOUTH);

        btnTim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        btnTaiLai.addActionListener(e -> {
            txtTim.setText("");
            locTrangThai.setSelectedIndex(0);
            taiDuLieu();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnSua.addActionListener(e -> {
            KhoaHoc chon = khoaDuocChon();
            if (chon != null) moForm(chon);
        });
        // Khi người dùng đổi tab, chỉ cập nhật bộ đếm; không truy vấn SQL không cần thiết.
        tabMonHoc.addChangeListener(e -> capNhatThongBaoTab());
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private Boolean trangThaiLoc() {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 -> Boolean.TRUE;
            case 2 -> Boolean.FALSE;
            default -> null;
        };
    }

    private int maMonTabDangChon() {
        int viTri = tabMonHoc.getSelectedIndex();
        return viTri >= 0 && viTri < cacBang.size() ? cacBang.get(viTri).maMonHoc : -1;
    }

    private void taiDuLieu() {
        final String tim = txtTim.getText();
        final Boolean trangThai = trangThaiLoc();
        final int maMonTruoc = maMonTabDangChon();
        final int luotTai = ++lanTaiDuLieu;
        thongBao.setText("Đang tải dữ liệu...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.layMonHoc(), controller.timKiem(tim, trangThai));
            }
            @Override protected void done() {
                if (luotTai != lanTaiDuLieu) return; // bỏ kết quả của lần tìm cũ
                try {
                    DuLieu dl = get();
                    int maMonMucTieu = monCanChonSauLuu != null
                            ? monCanChonSauLuu : maMonTruoc;
                    hienThiTheoMon(dl.monHoc(), dl.khoaHoc(), maMonMucTieu);
                    monCanChonSauLuu = null;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    /** Chỉ tạo tab cho môn đang hoạt động; không xóa dữ liệu môn/khóa cũ. */
    private void hienThiTheoMon(List<MonHoc> mons, List<KhoaHoc> khoas, int monMuonChon) {
        cacBang.clear();
        tabMonHoc.removeAll();
        int viTriCanChon = -1;
        for (MonHoc mon : mons) {
            if (!mon.trangThai()) continue; // Môn ngừng sử dụng không xuất hiện trên tab.
            BangTheoMon nhom = new BangTheoMon(mon.maMonHoc());
            for (KhoaHoc k : khoas) {
                if (k.maMonHoc() != mon.maMonHoc()) continue;
                nhom.khoaHoc.add(k);
                nhom.model.addRow(new Object[]{
                        k.tenKhoaHoc(), k.tongSoBuoi(),
                        k.hocPhiMacDinh().stripTrailingZeros().toPlainString(),
                        k.trangThai() ? "Hoạt động" : "Ngừng sử dụng"
                });
            }
            String tenTab = mon.tenMonHoc() + " (" + nhom.khoaHoc.size() + ")";
            cacBang.add(nhom);
            tabMonHoc.addTab(tenTab, new JScrollPane(nhom.bang));
            if (mon.maMonHoc() == monMuonChon) viTriCanChon = cacBang.size() - 1;
        }
        if (viTriCanChon >= 0) tabMonHoc.setSelectedIndex(viTriCanChon);
        if (cacBang.isEmpty()) {
            thongBao.setText("Không có môn học đang hoạt động. Bật lại môn học tại mục Môn học để hiển thị tab.");
        } else {
            capNhatThongBaoTab();
        }
    }

    private void capNhatThongBaoTab() {
        int pos = tabMonHoc.getSelectedIndex();
        if (pos < 0 || pos >= cacBang.size()) return;
        BangTheoMon tab = cacBang.get(pos);
        thongBao.setText("Có " + tab.khoaHoc.size() + " khóa học trong môn đang chọn");
    }

    private KhoaHoc khoaDuocChon() {
        int pos = tabMonHoc.getSelectedIndex();
        if (pos < 0 || pos >= cacBang.size()) {
            JOptionPane.showMessageDialog(this, "Chưa có môn học để chọn");
            return null;
        }
        BangTheoMon nhom = cacBang.get(pos);
        int row = nhom.bang.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một khóa học trong tab đang mở");
            return null;
        }
        return nhom.khoaHoc.get(nhom.bang.convertRowIndexToModel(row));
    }

    private void moForm(KhoaHoc cu) {
        // Mon hoc da ngung su dung van xuat hien khi sua khoa hoc cu cua chinh mon do.
        final List<MonHoc> cacMon;
        try {
            cacMon = controller.layMonHoc();
        } catch (SQLException ex) {
            hienLoi(ex); return;
        }
        List<MonHoc> luaChon = new ArrayList<>();
        for (MonHoc mon : cacMon) {
            if (mon.trangThai() || (cu != null && mon.maMonHoc() == cu.maMonHoc())) {
                luaChon.add(mon);
            }
        }
        if (luaChon.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Chưa có môn học đang hoạt động. Hãy tạo môn học trước.");
            return;
        }
        JComboBox<MonHoc> cboMon = new JComboBox<>(
                new DefaultComboBoxModel<>(luaChon.toArray(MonHoc[]::new)));
        cboMon.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof MonHoc mon) {
                    setText(mon.tenMonHoc() + (mon.trangThai() ? "" : " (ngừng sử dụng)"));
                }
                return this;
            }
        });
        // Mặc định môn đang mở; vẫn cho đổi môn nếu người dùng thực sự muốn.
        int maMonMacDinh = cu != null ? cu.maMonHoc() : maMonTabDangChon();
        for (MonHoc mon : luaChon) {
            if (mon.maMonHoc() == maMonMacDinh) {
                cboMon.setSelectedItem(mon);
                break;
            }
        }
        JTextField ten = new JTextField(cu == null ? "" : cu.tenKhoaHoc(), 25);
        JTextField soBuoi = new JTextField(cu == null ? "" : String.valueOf(cu.tongSoBuoi()), 25);
        JTextField hocPhi = new JTextField(cu == null ? ""
                : cu.hocPhiMacDinh().stripTrailingZeros().toPlainString(), 25);
        JComboBox<String> trangThai = new JComboBox<>(new String[]{
                "Hoạt động", "Ngừng sử dụng"
        });
        if (cu != null && !cu.trangThai()) trangThai.setSelectedIndex(1);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Môn học (*)")); form.add(cboMon);
        form.add(new JLabel("Tên khóa học (*)")); form.add(ten);
        form.add(new JLabel("Tổng số buổi (*)")); form.add(soBuoi);
        form.add(new JLabel("Học phí mặc định (*)")); form.add(hocPhi);
        form.add(new JLabel("Trạng thái")); form.add(trangThai);

        // Luu y khong bao gom viec tao khoan phai thu hay dang ky hoc vien.
        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, form,
                    cu == null ? "Thêm khóa học" : "Sửa khóa học",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            try {
                MonHoc mon = (MonHoc) cboMon.getSelectedItem();
                if (mon == null) throw new IllegalArgumentException("Hãy chọn môn học");
                String tenMoi = ten.getText().trim();
                if (tenMoi.isEmpty()) throw new IllegalArgumentException("Hãy nhập tên khóa học");
                int buoi = Integer.parseInt(soBuoi.getText().trim());
                if (buoi <= 0) throw new IllegalArgumentException("Số buổi phải lớn hơn 0");
                BigDecimal tien = new BigDecimal(hocPhi.getText().trim());
                if (tien.signum() < 0 || tien.scale() > 2 || tien.precision() - tien.scale() > 16) {
                    throw new IllegalArgumentException("Học phí phải >= 0, tối đa hai số thập phân");
                }
                boolean hoatDong = trangThai.getSelectedIndex() == 0;
                luuDuLieu(cu, mon.maMonHoc(), tenMoi, buoi, tien, hoatDong);
                return;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Số buổi phải là số nguyên, học phí nhập dạng 2000000 (không dấu phẩy)",
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void luuDuLieu(KhoaHoc cu, int maMon, String ten, int buoi,
                         BigDecimal hocPhi, boolean trangThai) {
        thongBao.setText("Đang lưu dữ liệu...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (cu == null) {
                    controller.them(maMon, ten, buoi, hocPhi, trangThai);
                    return "Đã thêm khóa học";
                }
                controller.sua(cu.maKhoaHoc(), maMon, ten, buoi, hocPhi, trangThai);
                return "Đã cập nhật khóa học";
            }
            @Override protected void done() {
                try {
                    JOptionPane.showMessageDialog(KhoaHocPanel.this, get());
                    monCanChonSauLuu = maMon;
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void hienLoi(Exception ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) goc = goc.getCause();
        String msg = goc.getMessage();
        if (goc instanceof SQLException sql) {
            if (sql.getErrorCode() == 547) {
                msg = "Không thể cập nhật: khóa học đang có dữ liệu liên kết hoặc môn học không hợp lệ";
            } else if (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627) {
                msg = "Dữ liệu khóa học bị trùng theo ràng buộc SQL Server";
            }
        }
        if (msg == null || msg.isBlank()) msg = "Không thể xử lý yêu cầu";
        thongBao.setText("Lỗi: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
