package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
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
import vn.vinabacus.controller.LopTrungTamController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.LopTrungTamRepository.GiaoVien;

/** Lop Trung tam: tab theo mon, loc khoa, an ma, chon ngay bang lich. */
public final class LopTrungTamPanel extends JPanel {
    private final LopTrungTamController controller = new LopTrungTamController(CoSo.TRUNG_TAM);
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Đang mở", "Tạm dừng", "Đã đóng"
    });
    private final JTabbedPane tabMonHoc = new JTabbedPane();
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final List<TabTheoMon> cacTab = new ArrayList<>();
    private int lanTai = 0;
    private Integer maMonCanChon;
    private Integer maKhoaCanLoc;

    private record DuLieu(List<MonHoc> mon, List<KhoaHoc> khoa,
                          List<LopTrungTam> lop) { }

    private record KhoaLuaChon(int maKhoaHoc, String tenKhoaHoc) {
        @Override public String toString() { return tenKhoaHoc; }
    }

    private static final class TabTheoMon {
        final int maMonHoc;
        final JComboBox<KhoaLuaChon> locKhoa = new JComboBox<>();
        final DefaultTableModel model = new DefaultTableModel(new String[]{
                "Tên lớp", "Khóa học", "Giáo viên", "Ngày khai giảng", "Trạng thái"
        }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable bang = new JTable(model);
        final List<LopTrungTam> hienThi = new ArrayList<>();
        final List<LopTrungTam> theoMon = new ArrayList<>();
        final JPanel panel = new JPanel(new BorderLayout(5, 8));

        TabTheoMon(int maMonHoc) {
            this.maMonHoc = maMonHoc;
            bang.setRowHeight(30);
            bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            bang.setAutoCreateRowSorter(true);
            JPanel boLoc = new JPanel(new FlowLayout(FlowLayout.LEFT));
            boLoc.add(new JLabel("Lọc khóa học:"));
            locKhoa.setPrototypeDisplayValue(new KhoaLuaChon(-1, "Khóa học có tên rất dài"));
            boLoc.add(locKhoa);
            panel.add(boLoc, BorderLayout.NORTH);
            panel.add(new JScrollPane(bang), BorderLayout.CENTER);
        }

        void hienThiTheoKhoa() {
            KhoaLuaChon chon = (KhoaLuaChon) locKhoa.getSelectedItem();
            int maKhoa = chon == null ? -1 : chon.maKhoaHoc();
            model.setRowCount(0);
            hienThi.clear();
            for (LopTrungTam l : theoMon) {
                if (maKhoa > 0 && l.maKhoaHoc() != maKhoa) continue;
                hienThi.add(l);
                model.addRow(new Object[]{
                        l.tenLop(), l.tenKhoaHoc(),
                        l.tenGiaoVien() == null ? "Chưa phân công" : l.tenGiaoVien(),
                        NgayKhaiGiangPicker.hienThiNgay(l.ngayKhaiGiang()),
                        hienThiTrangThai(l.trangThai())
                });
            }
        }
    }

    public LopTrungTamPanel() {
        super(new BorderLayout(8, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("QUẢN LÝ LỚP TRUNG TÂM");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JPanel thanhCongCu = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnTim = new JButton("Tìm kiếm");
        JButton btnTaiLai = new JButton("Tải lại");
        JButton btnThem = new JButton("Thêm");
        JButton btnSua = new JButton("Sửa");
        thanhCongCu.add(new JLabel("Tên lớp / khóa / giáo viên:"));
        thanhCongCu.add(txtTim);
        thanhCongCu.add(new JLabel("Trạng thái:"));
        thanhCongCu.add(locTrangThai);
        thanhCongCu.add(btnTim); thanhCongCu.add(btnTaiLai);
        thanhCongCu.add(btnThem); thanhCongCu.add(btnSua);
        JPanel dau = new JPanel(new BorderLayout(4, 8));
        dau.add(title, BorderLayout.NORTH);
        dau.add(thanhCongCu, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);
        add(tabMonHoc, BorderLayout.CENTER);
        add(thongBao, BorderLayout.SOUTH);

        btnTim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        btnTaiLai.addActionListener(e -> {
            txtTim.setText(""); locTrangThai.setSelectedIndex(0);
            taiDuLieu();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnSua.addActionListener(e -> {
            LopTrungTam lop = lopDuocChon();
            if (lop != null) moForm(lop);
        });
        tabMonHoc.addChangeListener(e -> capNhatThongBao());
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private String trangThaiLoc() {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 -> "DANG_MO";
            case 2 -> "TAM_DUNG";
            case 3 -> "DA_DONG";
            default -> null;
        };
    }

    private int monDangChon() {
        int i = tabMonHoc.getSelectedIndex();
        return i >= 0 && i < cacTab.size() ? cacTab.get(i).maMonHoc : -1;
    }

    private TabTheoMon tabDangChon() {
        int i = tabMonHoc.getSelectedIndex();
        return i >= 0 && i < cacTab.size() ? cacTab.get(i) : null;
    }

    private void taiDuLieu() {
        final int monCu = monDangChon();
        final int luot = ++lanTai;
        final String tim = txtTim.getText();
        final String trangThai = trangThaiLoc();
        thongBao.setText("Đang tải danh sách lớp...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.layMonHoc(), controller.layKhoaHoc(),
                        controller.timKiem(tim, trangThai));
            }
            @Override protected void done() {
                if (luot != lanTai) return;
                try {
                    DuLieu dl = get();
                    int monChon = maMonCanChon == null ? monCu : maMonCanChon;
                    taoTabs(dl, monChon);
                    maMonCanChon = null;
                    maKhoaCanLoc = null;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void taoTabs(DuLieu dl, int monMuonChon) {
        // Giu khoa hoc dang loc trong tab hien tai khi chi tim kiem/trang thai.
        int khoaCu = -1;
        TabTheoMon cu = tabDangChon();
        if (cu != null) {
            KhoaLuaChon kc = (KhoaLuaChon) cu.locKhoa.getSelectedItem();
            if (kc != null) khoaCu = kc.maKhoaHoc();
        }
        cacTab.clear();
        tabMonHoc.removeAll();
        int viTri = -1;
        for (MonHoc mon : dl.mon()) {
            if (!mon.trangThai()) continue; // Ẩn các tab của môn ngừng sử dụng.
            TabTheoMon tab = new TabTheoMon(mon.maMonHoc());
            tab.locKhoa.addItem(new KhoaLuaChon(-1, "Tất cả khóa học"));
            for (KhoaHoc k : dl.khoa()) {
                if (k.maMonHoc() == mon.maMonHoc())
                    tab.locKhoa.addItem(new KhoaLuaChon(k.maKhoaHoc(), k.tenKhoaHoc()));
            }
            for (LopTrungTam lop : dl.lop()) {
                if (lop.maMonHoc() == mon.maMonHoc()) tab.theoMon.add(lop);
            }
            int khoaChon = maKhoaCanLoc == null ? khoaCu : maKhoaCanLoc;
            if (mon.maMonHoc() == monMuonChon && khoaChon > 0) {
                for (int j = 0; j < tab.locKhoa.getItemCount(); j++) {
                    if (tab.locKhoa.getItemAt(j).maKhoaHoc() == khoaChon) {
                        tab.locKhoa.setSelectedIndex(j); break;
                    }
                }
            }
            tab.locKhoa.addActionListener(e -> {
                tab.hienThiTheoKhoa(); capNhatThongBao();
            });
            tab.hienThiTheoKhoa();
            cacTab.add(tab);
            tabMonHoc.addTab(mon.tenMonHoc()
                    + " (" + tab.theoMon.size() + ")", tab.panel);
            if (mon.maMonHoc() == monMuonChon) viTri = cacTab.size() - 1;
        }
        if (viTri >= 0) tabMonHoc.setSelectedIndex(viTri);
        capNhatThongBao();
    }

    private void capNhatThongBao() {
        TabTheoMon tab = tabDangChon();
        if (tab == null) thongBao.setText("Không có môn học đang hoạt động. Bật lại môn học tại mục Môn học để hiển thị tab.");
        else thongBao.setText("Có " + tab.hienThi.size() + " lớp phù hợp trong tab đang chọn");
    }

    private LopTrungTam lopDuocChon() {
        TabTheoMon tab = tabDangChon();
        if (tab == null || tab.bang.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một lớp trong tab đang mở");
            return null;
        }
        return tab.hienThi.get(tab.bang.convertRowIndexToModel(tab.bang.getSelectedRow()));
    }

    private void moForm(LopTrungTam cu) {
        TabTheoMon tab = tabDangChon();
        if (tab == null) {
            JOptionPane.showMessageDialog(this, "Chưa có môn học để tạo lớp"); return;
        }
        List<KhoaHoc> khoa;
        List<GiaoVien> giaoVien;
        try {
            khoa = controller.layKhoaHoc();
            giaoVien = controller.layGiaoVien();
        } catch (SQLException ex) { hienLoi(ex); return; }

        List<KhoaHoc> cacKhoa = new ArrayList<>();
        for (KhoaHoc k : khoa) {
            if (k.maMonHoc() == tab.maMonHoc
                    && (k.trangThai() || (cu != null && k.maKhoaHoc() == cu.maKhoaHoc()))) {
                cacKhoa.add(k);
            }
        }
        if (cacKhoa.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có khóa học đang hoạt động trong môn này");
            return;
        }
        JComboBox<KhoaHoc> cboKhoa = new JComboBox<>(
                new DefaultComboBoxModel<>(cacKhoa.toArray(KhoaHoc[]::new)));
        cboKhoa.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof KhoaHoc k) setText(k.tenKhoaHoc()
                        + (k.trangThai() ? "" : " (ngừng sử dụng)"));
                return this;
            }
        });
        int khoaMacDinh = cu == null ? -1 : cu.maKhoaHoc();
        if (cu == null) {
            KhoaLuaChon f = (KhoaLuaChon) tab.locKhoa.getSelectedItem();
            if (f != null) khoaMacDinh = f.maKhoaHoc();
        }
        for (KhoaHoc k : cacKhoa) {
            if (k.maKhoaHoc() == khoaMacDinh) { cboKhoa.setSelectedItem(k); break; }
        }

        JComboBox<GiaoVien> cboGv = new JComboBox<>();
        cboGv.addItem(null); // cho phep chua phan cong
        for (GiaoVien gv : giaoVien) cboGv.addItem(gv);
        cboGv.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof GiaoVien gv ? gv.hoTen() : "Chưa phân công");
                return this;
            }
        });
        if (cu != null && cu.maGiaoVien() != null) {
            boolean found = false;
            for (int i = 1; i < cboGv.getItemCount(); i++) {
                if (cboGv.getItemAt(i).maNhanVien() == cu.maGiaoVien()) {
                    cboGv.setSelectedIndex(i); found = true; break;
                }
            }
            if (!found) {
                GiaoVien gvCu = new GiaoVien(cu.maGiaoVien(),
                        (cu.tenGiaoVien() == null ? "Nhân viên cũ" : cu.tenGiaoVien())
                                + " (ngừng sử dụng)");
                cboGv.addItem(gvCu);
                cboGv.setSelectedItem(gvCu);
            }
        }

        JTextField txtTen = new JTextField(cu == null ? "" : cu.tenLop(), 25);
        NgayKhaiGiangPicker lich = new NgayKhaiGiangPicker(
                cu == null ? null : cu.ngayKhaiGiang());
        String[] labels = {"Đang mở", "Tạm dừng", "Đã đóng"};
        String[] values = {"DANG_MO", "TAM_DUNG", "DA_DONG"};
        JComboBox<String> cboTrangThai = new JComboBox<>(labels);
        if (cu != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(cu.trangThai())) cboTrangThai.setSelectedIndex(i);
            }
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.add(new JLabel("Khóa học (*)")); form.add(cboKhoa);
        form.add(new JLabel("Tên lớp (*)")); form.add(txtTen);
        form.add(new JLabel("Giáo viên phụ trách")); form.add(cboGv);
        form.add(new JLabel("Ngày khai giảng")); form.add(lich);
        form.add(new JLabel("Trạng thái")); form.add(cboTrangThai);

        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, form,
                    cu == null ? "Thêm lớp Trung tâm" : "Sửa lớp Trung tâm",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            try {
                KhoaHoc khoaChon = (KhoaHoc) cboKhoa.getSelectedItem();
                if (khoaChon == null) throw new IllegalArgumentException("Hãy chọn khóa học");
                String ten = txtTen.getText().trim();
                if (ten.isBlank()) throw new IllegalArgumentException("Hãy nhập tên lớp");
                GiaoVien gv = (GiaoVien) cboGv.getSelectedItem();
                Integer maGv = gv == null ? null : gv.maNhanVien();
                String trangThai = values[cboTrangThai.getSelectedIndex()];
                luuDuLieu(cu, khoaChon, ten, maGv, lich.layNgay(), trangThai);
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void luuDuLieu(LopTrungTam cu, KhoaHoc khoa, String ten,
                          Integer maGiaoVien, LocalDate ngay, String trangThai) {
        thongBao.setText("Đang lưu lớp...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (cu == null) {
                    controller.them(khoa.maKhoaHoc(), ten, maGiaoVien, ngay, trangThai);
                    return "Đã thêm lớp Trung tâm";
                }
                controller.sua(cu.maLop(), khoa.maKhoaHoc(), ten, maGiaoVien, ngay, trangThai);
                return "Đã cập nhật lớp Trung tâm";
            }
            @Override protected void done() {
                try {
                    String thongDiep = get();
                    maMonCanChon = khoa.maMonHoc();
                    maKhoaCanLoc = khoa.maKhoaHoc();
                    JOptionPane.showMessageDialog(LopTrungTamPanel.this, thongDiep);
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
                msg = "Liên kết khóa học, giáo viên hoặc cơ sở không hợp lệ. Có thể lớp đã có học viên.";
            } else if (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627) {
                msg = "Lớp bị trùng theo ràng buộc SQL Server.";
            }
        }
        if (msg == null || msg.isBlank()) msg = "Không thể thực hiện thao tác.";
        thongBao.setText("Lỗi: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }

    private static String hienThiTrangThai(String trangThai) {
        return switch (trangThai) {
            case "DANG_MO" -> "Đang mở";
            case "TAM_DUNG" -> "Tạm dừng";
            case "DA_DONG" -> "Đã đóng";
            default -> trangThai;
        };
    }
}
