package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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
import javax.swing.table.TableColumn;
import vn.vinabacus.controller.LopTrungTamController;
import vn.vinabacus.controller.LichHocLopController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;
import vn.vinabacus.repository.LopTrungTamRepository.GiaoVien;
import vn.vinabacus.service.LichHocLopService;

/** Danh sách lớp Trung tâm, lọc theo môn học và chọn ngày bằng lịch. */
public final class LopTrungTamPanel extends JPanel {
    private final LopTrungTamController controller = new LopTrungTamController(CoSo.TRUNG_TAM);
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Đang mở", "Tạm dừng", "Đã đóng"
    });
    private final JComboBox<MonLuaChon> locMonHoc = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "Tên lớp", "Khóa học", "Môn học", "Giáo viên", "Ngày khai giảng", "Trạng thái"
    }, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final List<LopTrungTam> hienThi = new ArrayList<>();
    private List<LopTrungTam> tatCaLop = new ArrayList<>();
    private int maMonDaLoc = -1;
    private int lanTai = 0;

    private record DuLieu(List<MonHoc> mon, List<LopTrungTam> lop) { }

    private record MonLuaChon(int maMonHoc, String tenMonHoc) {
        @Override public String toString() { return tenMonHoc; }
    }

    private record KhungGio(String ten, LocalTime batDau, LocalTime ketThuc) { }

    private static final List<KhungGio> KHUNG_GIO_HOC = List.of(
            new KhungGio("7h – 8h30", LocalTime.of(7, 0), LocalTime.of(8, 30)),
            new KhungGio("8h30 – 10h", LocalTime.of(8, 30), LocalTime.of(10, 0)),
            new KhungGio("10h – 11h30", LocalTime.of(10, 0), LocalTime.of(11, 30)),
            new KhungGio("11h30 – 13h", LocalTime.of(11, 30), LocalTime.of(13, 0)),
            new KhungGio("13h – 14h30", LocalTime.of(13, 0), LocalTime.of(14, 30)),
            new KhungGio("14h30 – 16h", LocalTime.of(14, 30), LocalTime.of(16, 0)),
            new KhungGio("16h – 17h30", LocalTime.of(16, 0), LocalTime.of(17, 30)),
            new KhungGio("17h30 – 19h", LocalTime.of(17, 30), LocalTime.of(19, 0)),
            new KhungGio("19h – 20h30", LocalTime.of(19, 0), LocalTime.of(20, 30)),
            new KhungGio("20h30 – 22h", LocalTime.of(20, 30), LocalTime.of(22, 0))
    );

    /** Chọn ngày theo hàng ngang, rồi chọn giờ trống trong lịch từng ngày. */
    static final class ChonLichHocPanel extends JPanel {
        private final NgayPicker ngayKhaiGiang;
        private final List<PhongHoc> phongHoc;
        private final JTabbedPane tab = new JTabbedPane();
        private final JPanel trangTrong = new JPanel(new BorderLayout());
        private final JLabel lblThongBao = new JLabel("Chọn thứ học ở hàng trên.");
        private final Map<Integer, JCheckBox> chonThu = new LinkedHashMap<>();
        private final Map<Integer, boolean[][]> oDaChon = new LinkedHashMap<>();
        private final Map<Integer, boolean[][]> oDangBan = new LinkedHashMap<>();
        private final Map<Integer, DefaultTableModel> models = new LinkedHashMap<>();

        ChonLichHocPanel(NgayPicker ngayKhaiGiang, List<LichHocLop> lichDaCo,
                         List<PhongHoc> phongHoc) {
            super(new BorderLayout(5, 5));
            this.ngayKhaiGiang = ngayKhaiGiang;
            this.phongHoc = List.copyOf(phongHoc);
            List<LichHocLop> lich = List.copyOf(lichDaCo);

            JPanel hangNgay = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
            for (int thu = 2; thu <= 8; thu++) {
                JCheckBox o = new JCheckBox(tenThuDay(thu));
                final int thuChon = thu;
                o.addActionListener(e -> doiThu(thuChon, o.isSelected(), lich));
                chonThu.put(thu, o);
                hangNgay.add(o);
            }

            trangTrong.add(new JLabel("Chọn một thứ để xem lịch phòng và giờ học."), BorderLayout.CENTER);
            tab.addTab("", trangTrong);
            tab.setEnabledAt(0, false);
            JPanel phanGhiChu = new JPanel(new BorderLayout(4, 4));
            phanGhiChu.add(lblThongBao, BorderLayout.NORTH);
            phanGhiChu.add(new JLabel("<html>Ô màu xám: khung giờ đã có lịch trong CSDL. "
                    + "Ô có dấu tick: khung giờ được chọn cho lớp mới.</html>"),
                    BorderLayout.SOUTH);
            add(hangNgay, BorderLayout.NORTH);
            add(tab, BorderLayout.CENTER);
            add(phanGhiChu, BorderLayout.SOUTH);
            setPreferredSize(new Dimension(1080, 390));
            ngayKhaiGiang.addPropertyChangeListener("ngayDaChon", e -> capNhatTinhTrangPhong(lich));
        }

        private void doiThu(int thu, boolean daChon, List<LichHocLop> lich) {
            if (daChon) {
                if (tab.getTabCount() == 1 && tab.getComponentAt(0) == trangTrong)
                    tab.removeTabAt(0);
                oDaChon.put(thu, new boolean[phongHoc.size()][KHUNG_GIO_HOC.size()]);
                capNhatOBan(thu, lich);
                DefaultTableModel model = taoModel(thu);
                models.put(thu, model);
                JTable bang = taoBang(model, thu);
                JScrollPane cuon = new JScrollPane(bang,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                tab.addTab(tenThuDay(thu), cuon);
                tab.setSelectedComponent(cuon);
            } else {
                int index = tab.indexOfTab(tenThuDay(thu));
                if (index >= 0) tab.removeTabAt(index);
                oDaChon.remove(thu);
                oDangBan.remove(thu);
                models.remove(thu);
                if (tab.getTabCount() == 0) {
                    tab.addTab("", trangTrong);
                    tab.setEnabledAt(0, false);
                }
            }
            capNhatThongBao();
        }

        private DefaultTableModel taoModel(int thu) {
            Object[] tieuDe = new Object[KHUNG_GIO_HOC.size() + 1];
            tieuDe[0] = "Phòng học";
            for (int i = 0; i < KHUNG_GIO_HOC.size(); i++) {
                String[] gio = KHUNG_GIO_HOC.get(i).ten().split(" – ");
                tieuDe[i + 1] = "<html><center>" + gio[0] + "<br>– " + gio[1] + "</center></html>";
            }
            DefaultTableModel model = new DefaultTableModel(tieuDe, 0) {
                @Override public Object getValueAt(int row, int column) {
                    if (column == 0) return super.getValueAt(row, column);
                    // Lịch đã có chỉ làm ô bị khóa; dấu tick dành riêng cho lựa chọn mới.
                    return oDaChon.get(thu)[row][column - 1];
                }
                @Override public Class<?> getColumnClass(int column) {
                    return column == 0 ? String.class : Boolean.class;
                }
                @Override public boolean isCellEditable(int row, int column) {
                    return column > 0 && oCoTheChon(thu, row, column - 1);
                }
                @Override public void setValueAt(Object value, int row, int column) {
                    if (column == 0) return;
                    int gio = column - 1;
                    boolean chon = Boolean.TRUE.equals(value);
                    if (chon && !oCoTheChon(thu, row, gio)) {
                        Toolkit.getDefaultToolkit().beep();
                        return;
                    }
                    oDaChon.get(thu)[row][gio] = chon;
                    fireTableDataChanged();
                    capNhatThongBao();
                }
            };
            for (PhongHoc phong : phongHoc) model.addRow(new Object[]{phong.tenPhong()});
            return model;
        }

        private JTable taoBang(DefaultTableModel model, int thu) {
            JTable bang = new JTable(model);
            bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            bang.setRowHeight(32);
            bang.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JCheckBox()));
            bang.setDefaultRenderer(Boolean.class, (table, value, selected, focus, row, column) -> {
                JCheckBox o = new JCheckBox();
                o.setHorizontalAlignment(JCheckBox.CENTER);
                o.setOpaque(true);
                boolean biKhoa = oDangBan.get(thu)[row][column - 1];
                o.setBackground(biKhoa ? new java.awt.Color(0xE5E7EB)
                        : selected ? table.getSelectionBackground() : table.getBackground());
                o.setSelected(Boolean.TRUE.equals(value));
                o.setEnabled(model.isCellEditable(row, column));
                return o;
            });
            for (int i = 0; i < bang.getColumnCount(); i++) {
                TableColumn column = bang.getColumnModel().getColumn(i);
                column.setPreferredWidth(i == 0 ? 142 : 96);
            }
            bang.getTableHeader().setPreferredSize(new Dimension(100, 50));
            bang.getTableHeader().setReorderingAllowed(false);
            return bang;
        }

        private boolean oCoTheChon(int thu, int phong, int indexGio) {
            boolean[][] ban = oDangBan.get(thu);
            boolean[][] chon = oDaChon.get(thu);
            if (ban == null || chon == null || phong < 0 || indexGio < 0
                    || indexGio >= KHUNG_GIO_HOC.size()) return false;
            if (chon[phong][indexGio]) return true;
            if (ban[phong][indexGio]) return false;
            KhungGio moi = KHUNG_GIO_HOC.get(indexGio);
            for (int p = 0; p < chon.length; p++) {
                for (int g = 0; g < chon[p].length; g++) {
                    if (!chon[p][g]) continue;
                    KhungGio daChon = KHUNG_GIO_HOC.get(g);
                    if (moi.batDau().isBefore(daChon.ketThuc())
                            && moi.ketThuc().isAfter(daChon.batDau())) return false;
                }
            }
            return true;
        }

        private void capNhatOBan(int thu, List<LichHocLop> lich) {
            boolean[][] ban = new boolean[phongHoc.size()][KHUNG_GIO_HOC.size()];
            LocalDate ngay = ngayKhaiGiang.layNgay() == null
                    ? LocalDate.now() : ngayKhaiGiang.layNgay();
            for (LichHocLop cu : lich) {
                if (!"HIEU_LUC".equals(cu.trangThai()) || cu.thuTrongTuan() != thu
                        || cu.tuNgay().isAfter(ngay)
                        || (cu.denNgay() != null && cu.denNgay().isBefore(ngay))) continue;
                for (int i = 0; i < KHUNG_GIO_HOC.size(); i++) {
                    KhungGio gio = KHUNG_GIO_HOC.get(i);
                    boolean trung = cu.gioBatDau() == null || cu.gioKetThuc() == null
                            || (cu.gioBatDau().isBefore(gio.ketThuc())
                            && cu.gioKetThuc().isAfter(gio.batDau()));
                    if (trung) {
                        for (int p = 0; p < phongHoc.size(); p++) {
                            if (cu.maPhong() == null
                                    || phongHoc.get(p).maPhong() == cu.maPhong()) ban[p][i] = true;
                        }
                    }
                }
            }
            oDangBan.put(thu, ban);
        }

        private void capNhatTinhTrangPhong(List<LichHocLop> lich) {
            for (Map.Entry<Integer, boolean[][]> entry : oDaChon.entrySet()) {
                int thu = entry.getKey();
                boolean[][] chon = entry.getValue();
                capNhatOBan(thu, lich);
                boolean[][] ban = oDangBan.get(thu);
                for (int p = 0; p < chon.length; p++) {
                    for (int g = 0; g < chon[p].length; g++) {
                        if (ban[p][g]) chon[p][g] = false;
                    }
                }
                DefaultTableModel model = models.get(thu);
                if (model != null) model.fireTableDataChanged();
            }
            capNhatThongBao();
        }

        List<BuoiTrongTuan> layBuoi() {
            List<BuoiTrongTuan> ds = new ArrayList<>();
            for (int thu = 2; thu <= 8; thu++) {
                boolean[][] chon = oDaChon.get(thu);
                if (chon == null) continue;
                for (int p = 0; p < chon.length; p++) {
                    for (int g = 0; g < chon[p].length; g++) {
                        if (!chon[p][g]) continue;
                        KhungGio gio = KHUNG_GIO_HOC.get(g);
                        ds.add(new BuoiTrongTuan(thu, phongHoc.get(p).maPhong(),
                                gio.batDau(), gio.ketThuc()));
                    }
                }
            }
            return ds;
        }

        boolean coNgayDaChon() {
            for (JCheckBox o : chonThu.values()) if (o.isSelected()) return true;
            return false;
        }

        private static String tenThuDay(int thu) {
            return switch (thu) {
                case 2 -> "Thứ hai";
                case 3 -> "Thứ ba";
                case 4 -> "Thứ tư";
                case 5 -> "Thứ năm";
                case 6 -> "Thứ sáu";
                case 7 -> "Thứ bảy";
                case 8 -> "Chủ nhật";
                default -> "";
            };
        }

        private void capNhatThongBao() {
            int soThu = 0;
            for (JCheckBox o : chonThu.values()) if (o.isSelected()) soThu++;
            int soBuoi = layBuoi().size();
            lblThongBao.setText(soThu == 0 ? "Chọn ít nhất một ngày học."
                    : soBuoi == 0 ? "Đánh dấu các khung giờ còn trống trong lịch ngày."
                    : "Đã chọn " + soBuoi + " khung giờ học.");
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
        JButton btnLocMon = new JButton("Lọc");
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

        bang.setRowHeight(30);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setAutoCreateRowSorter(true);
        locMonHoc.addItem(new MonLuaChon(-1, "Tất cả môn học"));
        locMonHoc.setPrototypeDisplayValue(new MonLuaChon(-1, "Môn học có tên rất dài"));
        JPanel boLoc = new JPanel(new FlowLayout(FlowLayout.LEFT));
        boLoc.add(new JLabel("Lọc theo môn:"));
        boLoc.add(locMonHoc);
        boLoc.add(btnLocMon);
        JPanel noiDung = new JPanel(new BorderLayout(5, 8));
        noiDung.add(boLoc, BorderLayout.NORTH);
        noiDung.add(new JScrollPane(bang), BorderLayout.CENTER);
        add(noiDung, BorderLayout.CENTER);
        add(thongBao, BorderLayout.SOUTH);

        btnTim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        btnTaiLai.addActionListener(e -> {
            txtTim.setText(""); locTrangThai.setSelectedIndex(0);
            taiDuLieu();
        });
        btnLocMon.addActionListener(e -> {
            MonLuaChon mon = (MonLuaChon) locMonHoc.getSelectedItem();
            maMonDaLoc = mon == null ? -1 : mon.maMonHoc();
            hienThiDanhSach();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnSua.addActionListener(e -> {
            LopTrungTam lop = lopDuocChon();
            if (lop != null) moForm(lop);
        });
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

    private void taiDuLieu() {
        final int luot = ++lanTai;
        final String tim = txtTim.getText();
        final String trangThai = trangThaiLoc();
        thongBao.setText("Đang tải danh sách lớp...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.layMonHoc(), controller.timKiem(tim, trangThai));
            }
            @Override protected void done() {
                if (luot != lanTai) return;
                try {
                    DuLieu dl = get();
                    int monDangChon = maMonDangChonTrongBoLoc();
                    capNhatDanhSachMon(dl.mon(), monDangChon);
                    tatCaLop = dl.lop();
                    hienThiDanhSach();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private int maMonDangChonTrongBoLoc() {
        MonLuaChon mon = (MonLuaChon) locMonHoc.getSelectedItem();
        return mon == null ? -1 : mon.maMonHoc();
    }

    private void capNhatDanhSachMon(List<MonHoc> cacMon, int maMonDangChon) {
        DefaultComboBoxModel<MonLuaChon> ds = new DefaultComboBoxModel<>();
        ds.addElement(new MonLuaChon(-1, "Tất cả môn học"));
        for (MonHoc mon : cacMon) {
            ds.addElement(new MonLuaChon(mon.maMonHoc(), mon.tenMonHoc()
                    + (mon.trangThai() ? "" : " (ngừng hoạt động)")));
        }
        locMonHoc.setModel(ds);
        for (int i = 0; i < ds.getSize(); i++) {
            if (ds.getElementAt(i).maMonHoc() == maMonDangChon) {
                locMonHoc.setSelectedIndex(i);
                return;
            }
        }
        locMonHoc.setSelectedIndex(0);
    }

    private void hienThiDanhSach() {
        model.setRowCount(0);
        hienThi.clear();
        for (LopTrungTam lop : tatCaLop) {
            if (maMonDaLoc > 0 && lop.maMonHoc() != maMonDaLoc) continue;
            hienThi.add(lop);
            model.addRow(new Object[]{
                    lop.tenLop(), lop.tenKhoaHoc(), lop.tenMonHoc(),
                    lop.tenGiaoVien() == null ? "Chưa phân công" : lop.tenGiaoVien(),
                    NgayPicker.hienThiNgay(lop.ngayKhaiGiang()),
                    hienThiTrangThai(lop.trangThai())
            });
        }
        if (maMonDaLoc > 0) {
            MonLuaChon mon = (MonLuaChon) locMonHoc.getSelectedItem();
            thongBao.setText("Có " + hienThi.size() + " lớp phù hợp với môn "
                    + (mon == null ? "đã chọn" : mon.tenMonHoc()));
        } else {
            thongBao.setText("Có " + hienThi.size() + " lớp thuộc tất cả môn học");
        }
    }

    private LopTrungTam lopDuocChon() {
        if (bang.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một lớp trong danh sách");
            return null;
        }
        return hienThi.get(bang.convertRowIndexToModel(bang.getSelectedRow()));
    }

    private void moForm(LopTrungTam cu) {
        List<KhoaHoc> khoa;
        List<GiaoVien> giaoVien;
        List<LichHocLop> lichDaCo;
        List<PhongHoc> phongHoc;
        try {
            khoa = controller.layKhoaHoc();
            giaoVien = controller.layGiaoVien();
            if (cu == null) {
                LichHocLopController lichController = new LichHocLopController();
                lichDaCo = lichController.timKiem("", "HIEU_LUC");
                phongHoc = lichController.layPhongHoc();
            } else {
                lichDaCo = List.of();
                phongHoc = List.of();
            }
        } catch (SQLException ex) { hienLoi(ex); return; }

        if (cu == null && phongHoc.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Chưa có phòng học đang hoạt động. Hãy cập nhật schema từ file VINABACUS.sql trước.",
                    "Thiếu danh sách phòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<KhoaHoc> cacKhoa = new ArrayList<>();
        for (KhoaHoc k : khoa) {
            if ((maMonDaLoc <= 0 || k.maMonHoc() == maMonDaLoc)
                    && (k.trangThai() || (cu != null && k.maKhoaHoc() == cu.maKhoaHoc()))) {
                cacKhoa.add(k);
            }
        }
        if (cacKhoa.isEmpty()) {
            JOptionPane.showMessageDialog(this, maMonDaLoc > 0
                    ? "Chưa có khóa học đang hoạt động trong môn này"
                    : "Chưa có khóa học đang hoạt động");
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
        NgayPicker lich = new NgayPicker(cu == null ? null : cu.ngayKhaiGiang(),
                "Chọn ngày khai giảng");
        String[] labels = {"Đang mở", "Tạm dừng", "Đã đóng"};
        String[] values = {"DANG_MO", "TAM_DUNG", "DA_DONG"};
        JComboBox<String> cboTrangThai = new JComboBox<>(labels);
        if (cu != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(cu.trangThai())) cboTrangThai.setSelectedIndex(i);
            }
        }
        ChonLichHocPanel chonLich = cu == null
                ? new ChonLichHocPanel(lich, lichDaCo, phongHoc) : null;
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        int hang = 0;
        themDongForm(form, hang++, "Khóa học (*)", cboKhoa);
        themDongForm(form, hang++, "Tên lớp (*)", txtTen);
        themDongForm(form, hang++, "Giáo viên phụ trách", cboGv);
        themDongForm(form, hang++, "Ngày khai giảng", lich);
        if (chonLich != null)
            themDongForm(form, hang++, "Ngày học (*)", chonLich);
        themDongForm(form, hang, "Trạng thái", cboTrangThai);

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
                LocalDate ngayKhaiGiang = lich.layNgay();
                List<BuoiTrongTuan> cacBuoi = chonLich == null ? List.of() : chonLich.layBuoi();
                if (chonLich != null && !chonLich.coNgayDaChon())
                    throw new IllegalArgumentException("Hãy chọn ít nhất một ngày học");
                if (chonLich != null && cacBuoi.isEmpty())
                    throw new IllegalArgumentException("Hãy chọn ít nhất một khung giờ còn trống");
                if (chonLich != null && ngayKhaiGiang == null)
                    throw new IllegalArgumentException("Hãy chọn ngày khai giảng trước khi lưu thời khóa biểu");
                luuDuLieu(cu, khoaChon, ten, maGv, ngayKhaiGiang, trangThai, cacBuoi);
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static void themDongForm(JPanel form, int hang, String nhan, JComponent component) {
        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.gridy = hang;
        label.insets = new Insets(5, 6, 5, 10);
        label.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel(nhan), label);

        GridBagConstraints input = new GridBagConstraints();
        input.gridx = 1;
        input.gridy = hang;
        input.weightx = 1;
        input.fill = GridBagConstraints.HORIZONTAL;
        input.anchor = GridBagConstraints.NORTHWEST;
        input.insets = new Insets(5, 6, 5, 6);
        form.add(component, input);
    }



    private void luuDuLieu(LopTrungTam cu, KhoaHoc khoa, String ten,
                          Integer maGiaoVien, LocalDate ngay, String trangThai,
                          List<BuoiTrongTuan> cacBuoi) {
        thongBao.setText("Đang lưu lớp...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (cu == null) {
                    if (cacBuoi == null || cacBuoi.isEmpty())
                        throw new IllegalArgumentException("Hãy chọn ít nhất một buổi học");
                    controller.themKemLich(khoa.maKhoaHoc(), ten, maGiaoVien,
                            ngay, trangThai, cacBuoi);
                    return "Đã thêm lớp Trung tâm và " + cacBuoi.size()
                            + " buổi vào lịch học";
                }
                controller.sua(cu.maLop(), khoa.maKhoaHoc(), ten, maGiaoVien, ngay, trangThai);
                return "Đã cập nhật lớp Trung tâm";
            }
            @Override protected void done() {
                try {
                    String thongDiep = get();
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
