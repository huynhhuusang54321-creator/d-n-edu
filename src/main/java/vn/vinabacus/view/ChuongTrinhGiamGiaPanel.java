package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.ChuongTrinhGiamGiaController;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.ChuongTrinhGiamGiaRepository;

/** Quản lý chương trình giảm giá Trung tâm; tự báo rõ khi thiếu migration. */
public final class ChuongTrinhGiamGiaPanel extends JPanel {
    private static final Color XANH = new Color(0, 83, 42);
    private static final Color VANG = new Color(255, 221, 0);
    private static final NumberFormat TIEN = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final ChuongTrinhGiamGiaController controller = new ChuongTrinhGiamGiaController();
    private final JTextField tim = new JTextField(19);
    private final JComboBox<String> loc = new JComboBox<>(new String[]{
            "Tất cả", "Đang hiệu lực", "Sắp có hiệu lực", "Hết hạn", "Ngừng áp dụng"});
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "Mã", "Tên chương trình", "Loại", "Giá trị", "Phạm vi",
            "Bắt đầu", "Kết thúc", "Kết hợp", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final JLabel thongBao = new JLabel("Đang tải chương trình...");
    private final JButton them = nut("Thêm chương trình");
    private final JButton sua = nut("Sửa");
    private final JButton chiTiet = nut("Xem chi tiết");
    private final JButton batTat = nut("Kích hoạt / Ngừng");
    private List<ChuongTrinhGiamGia> ds = List.of();
    private List<ChuongTrinhGiamGia> dangHien = List.of();
    private List<MonHoc> mon = List.of();
    private List<KhoaHoc> khoa = List.of();
    private int lanTai;
    private boolean sanSang;

    private record DuLieu(boolean sanSang, List<ChuongTrinhGiamGia> ds,
            List<MonHoc> mon, List<KhoaHoc> khoa) { }

    public ChuongTrinhGiamGiaPanel() {
        super(new BorderLayout(8, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 16, 15, 16));
        JLabel tieuDe = new JLabel("CHƯƠNG TRÌNH GIẢM GIÁ – TRUNG TÂM");
        tieuDe.setForeground(XANH);
        tieuDe.setFont(tieuDe.getFont().deriveFont(Font.BOLD, 22f));
        JPanel hangLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        hangLoc.setOpaque(false);
        hangLoc.add(new JLabel("Tên / mã:")); hangLoc.add(tim);
        hangLoc.add(new JLabel("Trạng thái:")); hangLoc.add(loc);
        JButton timKiem = nut("Tìm kiếm");
        JButton taiLai = nut("Làm mới");
        hangLoc.add(timKiem); hangLoc.add(taiLai);
        JPanel hangThaoTac = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        hangThaoTac.setOpaque(false);
        hangThaoTac.add(them); hangThaoTac.add(sua);
        hangThaoTac.add(chiTiet); hangThaoTac.add(batTat);
        JPanel cacHang = new JPanel(new GridLayout(0, 1));
        cacHang.setOpaque(false);
        cacHang.add(hangLoc); cacHang.add(hangThaoTac);
        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);
        dau.add(tieuDe, BorderLayout.NORTH);
        dau.add(cacHang, BorderLayout.SOUTH);
        add(dau, BorderLayout.NORTH);
        bang.setRowHeight(31);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setAutoCreateRowSorter(true);
        bang.getColumnModel().getColumn(1).setPreferredWidth(230);
        bang.getColumnModel().getColumn(4).setPreferredWidth(190);
        bang.getColumnModel().getColumn(8).setPreferredWidth(130);
        add(new JScrollPane(bang), BorderLayout.CENTER);
        thongBao.setForeground(XANH);
        add(thongBao, BorderLayout.SOUTH);
        timKiem.addActionListener(e -> locDuLieu());
        tim.addActionListener(e -> locDuLieu());
        loc.addActionListener(e -> locDuLieu());
        taiLai.addActionListener(e -> tai());
        them.addActionListener(e -> moForm(null));
        sua.addActionListener(e -> { ChuongTrinhGiamGia ct = chon(); if (ct != null) moForm(ct); });
        chiTiet.addActionListener(e -> xemChiTiet());
        batTat.addActionListener(e -> doiTrangThai());
        bang.getSelectionModel().addListSelectionListener(e -> capNhatNut());
        bang.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) xemChiTiet();
            }
        });
        capNhatNut();
        tai();
    }

    private static JButton nut(String ten) {
        JButton b = new JButton(ten);
        b.setPreferredSize(new Dimension(Math.max(92, ten.length() * 9 + 21), 32));
        return b;
    }

    private void tai() {
        int lan = ++lanTai;
        thongBao.setText("Đang tải dữ liệu từ SQL Server...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                boolean coSchema = controller.sanSang();
                if (!coSchema) return new DuLieu(false, List.of(), List.of(), List.of());
                return new DuLieu(true, controller.danhSach(),
                        controller.monHoc(), controller.khoaHoc());
            }
            @Override protected void done() {
                if (lan != lanTai) return;
                try {
                    DuLieu dl = get();
                    sanSang = dl.sanSang(); ds = dl.ds(); mon = dl.mon(); khoa = dl.khoa();
                    locDuLieu();
                    thongBao.setText(sanSang ? "Đã tải " + ds.size() + " chương trình."
                            : ChuongTrinhGiamGiaRepository.THIEU_SCHEMA);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    sanSang = false; ds = List.of(); mon = List.of(); khoa = List.of();
                    locDuLieu();
                    thongBao.setText("Tải dữ liệu bị gián đoạn; chưa thể thêm chương trình.");
                } catch (ExecutionException ex) {
                    sanSang = false; ds = List.of(); mon = List.of(); khoa = List.of();
                    locDuLieu();
                    thongBao.setText("Không tải được chương trình: " + loi(ex));
                }
                capNhatNut();
            }
        }.execute();
    }

    private void locDuLieu() {
        String q = tim.getText().trim().toLowerCase(Locale.ROOT);
        String trangThai = (String) loc.getSelectedItem();
        List<ChuongTrinhGiamGia> ketQua = new ArrayList<>();
        model.setRowCount(0);
        for (ChuongTrinhGiamGia ct : ds) {
            String tinhTrang = trangThai(ct);
            if (!q.isEmpty() && !ct.ten().toLowerCase(Locale.ROOT).contains(q)
                    && !String.valueOf(ct.maChuongTrinh()).contains(q)) continue;
            if (!"Tất cả".equals(trangThai) && !trangThai.equals(tinhTrang)) continue;
            ketQua.add(ct);
            model.addRow(new Object[]{ct.maChuongTrinh(), ct.ten(),
                    "PHAN_TRAM".equals(ct.loaiGiam()) ? "Phần trăm" : "Số tiền",
                    giaTri(ct), ct.apDungTatCaMon() ? "Tất cả môn"
                            : ct.maMonHoc().size() + " môn"
                              + (ct.maKhoaHoc().isEmpty() ? "" : ", " + ct.maKhoaHoc().size() + " khóa"),
                    NgayKhaiGiangPicker.hienThiNgay(ct.tuNgay()),
                    NgayKhaiGiangPicker.hienThiNgay(ct.denNgay()),
                    ct.choPhepKetHop() ? "Có" : "Không", tinhTrang});
        }
        dangHien = ketQua;
        capNhatNut();
    }

    private static String trangThai(ChuongTrinhGiamGia ct) {
        if (!ct.kichHoat()) return "Ngừng áp dụng";
        LocalDate homNay = LocalDate.now();
        if (homNay.isBefore(ct.tuNgay())) return "Sắp có hiệu lực";
        if (homNay.isAfter(ct.denNgay())) return "Hết hạn";
        return "Đang hiệu lực";
    }

    private static String giaTri(ChuongTrinhGiamGia ct) {
        return "PHAN_TRAM".equals(ct.loaiGiam())
                ? ct.giaTri().stripTrailingZeros().toPlainString() + "%"
                : TIEN.format(ct.giaTri()) + " đ";
    }

    private ChuongTrinhGiamGia chon() {
        int row = bang.getSelectedRow();
        if (row < 0) return null;
        int i = bang.convertRowIndexToModel(row);
        return i < dangHien.size() ? dangHien.get(i) : null;
    }

    private void capNhatNut() {
        ChuongTrinhGiamGia ct = chon();
        them.setEnabled(sanSang);
        them.setToolTipText(sanSang ? "Tạo chương trình giảm giá Trung tâm"
                : thongBao.getText());
        sua.setEnabled(sanSang && ct != null);
        chiTiet.setEnabled(ct != null);
        batTat.setEnabled(sanSang && ct != null);
    }

    private void xemChiTiet() {
        ChuongTrinhGiamGia ct = chon();
        if (ct == null) return;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Chi tiết chương trình giảm giá", JDialog.ModalityType.APPLICATION_MODAL);
        JPanel noiDung = new JPanel();
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));
        noiDung.setBackground(Color.WHITE);
        noiDung.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        noiDung.add(nhomChiTiet("THÔNG TIN CHƯƠNG TRÌNH", new String[][]{
            {"Tên chương trình", ct.ten()}, {"Mã chương trình", "#"+ct.maChuongTrinh()},
            {"Trạng thái", trangThai(ct)}, {"Loại và giá trị", giaTri(ct)},
            {"Thời gian", NgayKhaiGiangPicker.hienThiNgay(ct.tuNgay())+" – "
                    +NgayKhaiGiangPicker.hienThiNgay(ct.denNgay())},
            {"Cho phép kết hợp", ct.choPhepKetHop()?"Có":"Không"}}));
        String monApDung = ct.apDungTatCaMon() ? "Tất cả môn"
                : String.join(", ",mon.stream().filter(m -> ct.maMonHoc().contains(m.maMonHoc()))
                        .map(MonHoc::tenMonHoc).toList());
        String khoaGioiHan = ct.maKhoaHoc().isEmpty() ? "Tất cả khóa thuộc môn được chọn"
                : String.join(", ",khoa.stream().filter(k -> ct.maKhoaHoc().contains(k.maKhoaHoc()))
                        .map(KhoaHoc::tenKhoaHoc).toList());
        noiDung.add(nhomChiTiet("PHẠM VI ÁP DỤNG", new String[][]{
            {"Môn học",monApDung}, {"Khóa giới hạn",khoaGioiHan},
            {"Mô tả",ct.moTa()==null||ct.moTa().isBlank()?"Chưa có":ct.moTa()}}));
        dlg.add(new JScrollPane(noiDung),BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton dong = nut("Đóng"); dong.addActionListener(e -> dlg.dispose());
        footer.add(dong); dlg.add(footer,BorderLayout.SOUTH);
        dlg.setSize(760,Math.min(600,java.awt.Toolkit.getDefaultToolkit().getScreenSize().height-90));
        dlg.setLocationRelativeTo(this); dlg.setVisible(true);
    }

    private static JPanel nhomChiTiet(String ten,String[][] dong) {
        JPanel p=new JPanel(new GridLayout(0,2,12,10));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(ten),BorderFactory.createEmptyBorder(8,10,10,10)));
        for(String[] d:dong) {
            JLabel nhan=new JLabel(d[0]); nhan.setForeground(XANH); p.add(nhan);
            JTextArea giaTri=new JTextArea(d[1]);
            giaTri.setEditable(false);giaTri.setLineWrap(true);giaTri.setWrapStyleWord(true);
            giaTri.setBackground(Color.WHITE);p.add(giaTri);
        }
        return p;
    }

    private void doiTrangThai() {
        ChuongTrinhGiamGia ct = chon();
        if (ct == null) return;
        boolean moi = !ct.kichHoat();
        String hanhDong = moi ? "kích hoạt" : "ngừng áp dụng";
        if (JOptionPane.showConfirmDialog(this,
                "Xác nhận " + hanhDong + " chương trình “" + ct.ten() + "”?",
                "Xác nhận trạng thái", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        batTat.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.doiTrangThai(ct.maChuongTrinh(), moi); return null;
            }
            @Override protected void done() {
                try { get(); tai(); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                catch (ExecutionException ex) { hienLoi(ChuongTrinhGiamGiaPanel.this, ex); }
                capNhatNut();
            }
        }.execute();
    }

    private void moForm(ChuongTrinhGiamGia cu) {
        if (!sanSang) return;
        new Form(cu).setVisible(true);
    }

    private final class Form extends JDialog {
        private final JTextField ten = new JTextField(32);
        private final JTextArea moTa = new JTextArea(3, 32);
        private final JComboBox<String> loai = new JComboBox<>(new String[]{"Phần trăm (%)", "Số tiền (VNĐ)"});
        private final JTextField giaTri = new JTextField(13);
        private final NgayKhaiGiangPicker tu = new NgayKhaiGiangPicker(LocalDate.now());
        private final NgayKhaiGiangPicker den = new NgayKhaiGiangPicker(LocalDate.now().plusMonths(1));
        private final JCheckBox tatCa = new JCheckBox("Áp dụng cho tất cả môn học");
        private final JCheckBox ketHop = new JCheckBox("Cho phép kết hợp với chương trình khác");
        private final JCheckBox kichHoat = new JCheckBox("Kích hoạt", true);
        private final JPanel dsMon = new JPanel();
        private final JPanel dsKhoa = new JPanel();
        private final Map<Integer, JCheckBox> oMon = new LinkedHashMap<>();
        private final Map<Integer, JCheckBox> oKhoa = new LinkedHashMap<>();
        private final JButton luu = nut("Lưu chương trình");
        private final JLabel trangThai = new JLabel("Chọn phạm vi áp dụng.");
        private final ChuongTrinhGiamGia cu;
        private boolean daTaoKhoa;

        Form(ChuongTrinhGiamGia cu) {
            super(SwingUtilities.getWindowAncestor(ChuongTrinhGiamGiaPanel.this),
                    cu == null ? "Thêm chương trình giảm giá" : "Sửa chương trình giảm giá",
                    ModalityType.APPLICATION_MODAL);
            this.cu = cu;
            setLayout(new BorderLayout(8, 8));
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
            form.setBorder(BorderFactory.createTitledBorder("THÔNG TIN CHƯƠNG TRÌNH"));
            form.add(new JLabel("Tên chương trình:")); form.add(ten);
            form.add(new JLabel("Mô tả:")); form.add(new JScrollPane(moTa));
            form.add(new JLabel("Loại giảm:")); form.add(loai);
            form.add(new JLabel("Giá trị (nhập số, không dấu phân cách):")); form.add(giaTri);
            form.add(new JLabel("Ngày bắt đầu:")); form.add(tu);
            form.add(new JLabel("Ngày kết thúc:")); form.add(den);
            JPanel dieuKien = new JPanel(new GridLayout(0, 2, 8, 6));
            dieuKien.setBackground(Color.WHITE);
            dieuKien.setBorder(BorderFactory.createTitledBorder("ĐIỀU KIỆN"));
            dieuKien.add(new JLabel("Phạm vi:")); dieuKien.add(tatCa);
            dieuKien.add(new JLabel("Kết hợp:")); dieuKien.add(ketHop);
            dieuKien.add(new JLabel("Trạng thái:")); dieuKien.add(kichHoat);
            dsMon.setLayout(new BoxLayout(dsMon, BoxLayout.Y_AXIS));
            dsKhoa.setLayout(new BoxLayout(dsKhoa, BoxLayout.Y_AXIS));
            for (MonHoc m : mon) {
                JCheckBox o = new JCheckBox(m.tenMonHoc());
                o.setSelected(cu != null && cu.maMonHoc().contains(m.maMonHoc()));
                o.addActionListener(e -> capNhatKhoa());
                oMon.put(m.maMonHoc(), o); dsMon.add(o);
            }
            JPanel phamVi = new JPanel(new GridLayout(1, 2, 8, 0));
            JScrollPane cuonMon = new JScrollPane(dsMon);
            cuonMon.setBorder(BorderFactory.createTitledBorder("Môn học áp dụng"));
            JScrollPane cuonKhoa = new JScrollPane(dsKhoa);
            cuonKhoa.setBorder(BorderFactory.createTitledBorder(
                    "Giới hạn khóa (để trống = mọi khóa thuộc môn)"));
            phamVi.add(cuonMon); phamVi.add(cuonKhoa);
            phamVi.setPreferredSize(new Dimension(760, 180));
            JPanel giua = new JPanel();
            giua.setLayout(new BoxLayout(giua, BoxLayout.Y_AXIS));
            giua.setBackground(Color.WHITE);
            giua.add(form);giua.add(dieuKien);giua.add(phamVi);
            giua.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
            JScrollPane cuonNoiDung = new JScrollPane(giua);
            cuonNoiDung.setBorder(null);
            cuonNoiDung.getVerticalScrollBar().setUnitIncrement(16);
            add(cuonNoiDung, BorderLayout.CENTER);
            JPanel cuoi = new JPanel(new BorderLayout());
            JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton huy = nut("Đóng");
            nut.add(luu); nut.add(huy);
            cuoi.add(trangThai, BorderLayout.CENTER); cuoi.add(nut, BorderLayout.EAST);
            cuoi.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
            add(cuoi, BorderLayout.SOUTH);
            if (cu != null) {
                ten.setText(cu.ten()); moTa.setText(cu.moTa());
                loai.setSelectedIndex("PHAN_TRAM".equals(cu.loaiGiam()) ? 0 : 1);
                giaTri.setText(cu.giaTri().stripTrailingZeros().toPlainString());
                tu.datNgay(cu.tuNgay()); den.datNgay(cu.denNgay());
                tatCa.setSelected(cu.apDungTatCaMon());
                ketHop.setSelected(cu.choPhepKetHop()); kichHoat.setSelected(cu.kichHoat());
            }
            tatCa.addActionListener(e -> capNhatKhoa());
            luu.addActionListener(e -> luu()); huy.addActionListener(e -> dispose());
            capNhatKhoa();
            setSize(860, Math.min(630,
                    java.awt.Toolkit.getDefaultToolkit().getScreenSize().height-90));
            setMinimumSize(new Dimension(760, 480));
            setLocationRelativeTo(ChuongTrinhGiamGiaPanel.this);
        }

        private void capNhatKhoa() {
            Set<Integer> daChon = new HashSet<>();
            oKhoa.forEach((id, o) -> { if (o.isSelected()) daChon.add(id); });
            boolean chonTatCa = tatCa.isSelected();
            dsMon.setEnabled(!chonTatCa);
            for (JCheckBox o : oMon.values()) o.setEnabled(!chonTatCa);
            Set<Integer> maMon = new HashSet<>();
            oMon.forEach((id, o) -> { if (o.isSelected()) maMon.add(id); });
            dsKhoa.removeAll(); oKhoa.clear();
            if (!chonTatCa) for (KhoaHoc k : khoa) {
                if (!maMon.contains(k.maMonHoc())) continue;
                JCheckBox o = new JCheckBox(k.tenMonHoc() + " · " + k.tenKhoaHoc());
                o.setSelected(daChon.contains(k.maKhoaHoc())
                        || !daTaoKhoa && cu != null
                           && cu.maKhoaHoc().contains(k.maKhoaHoc()));
                oKhoa.put(k.maKhoaHoc(), o); dsKhoa.add(o);
            }
            daTaoKhoa = true;
            dsKhoa.revalidate(); dsKhoa.repaint();
        }

        private void luu() {
            BigDecimal so;
            try { so = new BigDecimal(giaTri.getText().trim()); }
            catch (NumberFormatException ex) {
                trangThai.setText("Hãy nhập giá trị giảm là số hợp lệ."); return;
            }
            Set<Integer> maMon = new HashSet<>(), maKhoa = new HashSet<>();
            oMon.forEach((id, o) -> { if (o.isSelected()) maMon.add(id); });
            oKhoa.forEach((id, o) -> { if (o.isSelected()) maKhoa.add(id); });
            String loaiMa = loai.getSelectedIndex() == 0 ? "PHAN_TRAM" : "SO_TIEN";
            if (ten.getText().isBlank() || tu.layNgay() == null || den.layNgay() == null
                    || den.layNgay().isBefore(tu.layNgay()) || so.signum() < 0
                    || so.scale() > 2 || so.precision() > 18
                    || (loai.getSelectedIndex() == 0 && so.compareTo(BigDecimal.valueOf(100)) > 0)
                    || (!tatCa.isSelected() && maMon.isEmpty())) {
                trangThai.setText("Kiểm tra tên, ngày, giá trị và môn được chọn."); return;
            }
            luu.setEnabled(false);
            trangThai.setText("Đang lưu chương trình...");
            new SwingWorker<Integer, Void>() {
                @Override protected Integer doInBackground() throws Exception {
                    return controller.luu(cu == null ? 0 : cu.maChuongTrinh(),
                            ten.getText().trim(), moTa.getText().trim(), loaiMa, so,
                            tu.layNgay(), den.layNgay(), tatCa.isSelected(),
                            ketHop.isSelected(), kichHoat.isSelected(), maMon, maKhoa);
                }
                @Override protected void done() {
                    try { get(); dispose(); tai(); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    catch (ExecutionException ex) { trangThai.setText(loi(ex)); }
                    luu.setEnabled(true);
                }
            }.execute();
        }
    }

    private static String loi(Throwable ex) {
        Throwable c = ex;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }
    private static void hienLoi(Component parent, Exception ex) {
        JOptionPane.showMessageDialog(parent, loi(ex), "Không thực hiện được",
                JOptionPane.ERROR_MESSAGE);
    }
}
