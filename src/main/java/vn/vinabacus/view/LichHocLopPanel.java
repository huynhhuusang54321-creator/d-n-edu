package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import java.awt.Window;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.LichHocLopController;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.service.LichHocLopService;
import vn.vinabacus.service.CaHocTrungTam;
import vn.vinabacus.service.DuKienKetThucLop;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;
import vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong;
import vn.vinabacus.repository.LichHocLopRepository.HocVienTheoLop;

/** Lich hoc Trung tam: tab Tong va cac tab theo mon; loc khoa/lop; ma lich khong hien trong bang. */
public final class LichHocLopPanel extends JPanel {
    private static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm");
    private final LichHocLopController controller = new LichHocLopController();
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Hiệu lực", "Đã hủy"
    });
    private final JTabbedPane tabMonHoc = new JTabbedPane();
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final JButton btnXoaLoc = new NutMau("Xóa lọc", NutMau.Kieu.PHU);
    // Chan su kien combo khi dua tat ca bo loc ve mac dinh, chi tai SQL mot lan.
    private boolean dangXoaLoc;
    private final List<TabTheoMon> cacTab = new ArrayList<>();
    private int lanTai;
    private final Map<Integer, Boolean> cheDoTungTab = new LinkedHashMap<>();
    private List<PhongHoc> danhSachPhong = List.of();
    private List<LichHocLop> tatCaLich = List.of();
    private LocalDate tuanCanXem = LocalDate.now();

    private record DuLieu(List<MonHoc> mon, List<KhoaHoc> khoa,
                          List<LopTrungTam> lop, List<LichHocLop> lich,
                          List<LichHocLop> tatCa, List<PhongHoc> phong,
                          List<HocVienTheoLop> hocVien,
                          List<DuKienKetThucLop.BuoiDieuChinh> buoiDieuChinh) { }
    private record LuaChon(int ma, String ten) {
        @Override public String toString() { return ten; }
    }

    /**
     * Tong: thoi khoa bieu su dung toan bo vung noi dung.
     * Tab mon: 3 the lop tren 1 hang, gom moi buoi hoc cua cung ma lop.
     * Giu bang danh sach cu de nguoi dung co the xem/loc/sua theo dong.
     */
    private static final class TabTheoMon {
        final int maMon;
        final JComboBox<LuaChon> locKhoa = new JComboBox<>();
        final JComboBox<LuaChon> locLop = new JComboBox<>();
        final JComboBox<LuaChon> locGiaoVien = new JComboBox<>();
        final JComboBox<LuaChon> locPhong = new JComboBox<>();
        final JComboBox<LuaChon> locHocVien = new JComboBox<>();
        final DefaultTableModel model = new DefaultTableModel(new String[]{
                "Tên lớp", "Khóa học", "Giáo viên", "Phòng", "Thứ", "Bắt đầu", "Kết thúc", "Từ ngày", "Đến ngày", "Trạng thái"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        final JTable bang = new JTable(model);
        final List<LichHocLop> theoMon = new ArrayList<>();
        final List<LichHocLop> lichDayDu = new ArrayList<>();
        final List<LichHocLop> hienThi = new ArrayList<>();
        final List<KhoaHoc> khoa = new ArrayList<>();
        final List<LopTrungTam> lop = new ArrayList<>();
        final List<HocVienTheoLop> hocVien = new ArrayList<>();
        final List<DuKienKetThucLop.BuoiDieuChinh> buoiDieuChinh = new ArrayList<>();
        final JPanel panel = new JPanel(new BorderLayout(5, 8));
        final JPanel cardPanel = new JPanel(new CardLayout());
        final JPanel luoiThe = new JPanel(new GridLayout(0, 3, 14, 14));
        final ThoiKhoaBieuTuanPanel lichTuan;
        final NutCheDo btnDanhSach;
        final NutCheDo btnThoiKhoaBieu;
        final Consumer<List<LichHocLop>> khiSuaCard;
        LichHocLop lichDaChon;
        boolean dangNap;
        boolean xemTkb;

        TabTheoMon(int maMon, LocalDate tuan,
                   Consumer<LocalDate> khiChonTuan, Consumer<Boolean> khiDoiCheDo,
                   Consumer<List<LichHocLop>> khiSuaCard,
                   Consumer<LichHocLop> khiSuaOThoiKhoaBieu) {
            this.maMon = maMon;
            this.khiSuaCard = khiSuaCard;
            lichTuan = new ThoiKhoaBieuTuanPanel(tuan, khiChonTuan);
            lichTuan.datXuLyChon(lich -> {
                chonLichTrongBang(lich);
                LichHocLop goc = lichDayDu.stream()
                        .filter(x -> x.maLichHoc() == lich.maLichHoc())
                        .findFirst().orElse(lich);
                khiSuaOThoiKhoaBieu.accept(goc);
            });
            btnDanhSach = new NutCheDo(maMon == -1 ? "Danh sách" : "Dạng danh sách", false);
            btnThoiKhoaBieu = new NutCheDo(maMon == -1 ? "Thời khóa biểu" : "Dạng ô", true);

            bang.setRowHeight(29);
            bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            bang.setAutoCreateRowSorter(true);
            bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int[] widths = {185, 155, 145, 100, 85, 88, 88, 112, 112, 105};
            for (int i = 0; i < widths.length; i++)
                bang.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            locKhoa.setPrototypeDisplayValue(new LuaChon(-1, "Khóa học có tên rất dài"));
            locLop.setPrototypeDisplayValue(new LuaChon(-1, "Tên lớp học có tên rất dài"));
            locGiaoVien.setPrototypeDisplayValue(new LuaChon(-1, "Tên giáo viên khá dài"));
            locPhong.setPrototypeDisplayValue(new LuaChon(-1, "Phòng học dài"));
            locHocVien.setPrototypeDisplayValue(new LuaChon(-1, "Học viên có tên khá dài"));
            JPanel boLoc = new JPanel(new GridLayout(2, 1, 0, 2));
            JPanel hangMot = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            boLoc.setOpaque(false); hangMot.setOpaque(false);
            hangMot.add(new JLabel("Khóa học:")); hangMot.add(locKhoa);
            hangMot.add(new JLabel("Lớp học:")); hangMot.add(locLop);
            hangMot.add(new JLabel("Giáo viên:")); hangMot.add(locGiaoVien);
            JPanel hangHai = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            hangHai.setOpaque(false);
            hangHai.add(new JLabel("Phòng học:")); hangHai.add(locPhong);
            hangHai.add(new JLabel("Học viên đã xếp lớp:")); hangHai.add(locHocVien);
            boLoc.add(hangMot); boLoc.add(hangHai);
            JPanel cheDo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 3));
            cheDo.add(btnDanhSach);
            cheDo.add(btnThoiKhoaBieu);
            JPanel thanhLoc = new JPanel(new BorderLayout());
            thanhLoc.add(boLoc, BorderLayout.CENTER);
            // Tab Tong chi xem lich tuan; tab mon hoc chi xem the lop.
            // Khong hien thi hai nut chuyen che do nua.
            panel.add(thanhLoc, BorderLayout.NORTH);

            cardPanel.add(new JScrollPane(bang), "danhSach");
            if (maMon == -1) {
                // Khong chia voi bang gon nua: lich 7 ngay du toan bo be ngang.
                cardPanel.add(lichTuan, "thoiKhoaBieu");
            } else {
                luoiThe.setBackground(new Color(0xF7FAF8));
                luoiThe.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
                // Neo luoi vao phia tren, khong keo dan the theo chieu cao cua cua so.
                JPanel nenThe = new JPanel(new BorderLayout());
                nenThe.setBackground(new Color(0xF7FAF8));
                nenThe.add(luoiThe, BorderLayout.NORTH);
                JScrollPane cuonThe = new JScrollPane(nenThe,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                cuonThe.getViewport().setBackground(new Color(0xF7FAF8));
                cuonThe.setBorder(BorderFactory.createLineBorder(new Color(0xDCE8E0)));
                cuonThe.getVerticalScrollBar().setUnitIncrement(22);
                cardPanel.add(cuonThe, "thoiKhoaBieu");
            }
            panel.add(cardPanel, BorderLayout.CENTER);
            btnDanhSach.addActionListener(e -> khiDoiCheDo.accept(false));
            btnThoiKhoaBieu.addActionListener(e -> khiDoiCheDo.accept(true));
            datCheDo(true);
        }

        void datCheDo(boolean xemTkb) {
            this.xemTkb = xemTkb;
            ((CardLayout) cardPanel.getLayout()).show(cardPanel,
                    xemTkb ? "thoiKhoaBieu" : "danhSach");
            btnDanhSach.datDuocChon(!xemTkb);
            btnThoiKhoaBieu.datDuocChon(xemTkb);
        }

        private void chonLichTrongBang(LichHocLop lich) {
            lichDaChon = lich;
            for (int i = 0; i < hienThi.size(); i++) {
                if (hienThi.get(i).maLichHoc() == lich.maLichHoc()) {
                    int viewIndex = bang.convertRowIndexToView(i);
                    bang.setRowSelectionInterval(viewIndex, viewIndex);
                    bang.scrollRectToVisible(bang.getCellRect(viewIndex, 0, true));
                    return;
                }
            }
        }

        int maKhoaDangLoc() {
            LuaChon x = (LuaChon) locKhoa.getSelectedItem();
            return x == null ? -1 : x.ma();
        }
        int maLopDangLoc() {
            LuaChon x = (LuaChon) locLop.getSelectedItem();
            return x == null ? -1 : x.ma();
        }
        private static int maDangLoc(JComboBox<LuaChon> cbo) {
            LuaChon x = (LuaChon) cbo.getSelectedItem();
            return x == null ? -1 : x.ma();
        }
        int maGiaoVienDangLoc() { return maDangLoc(locGiaoVien); }
        int maPhongDangLoc() { return maDangLoc(locPhong); }
        int maHocVienDangLoc() { return maDangLoc(locHocVien); }

        private LopTrungTam lopTheoMa(int maLop) {
            for (LopTrungTam x : lop) if (x.maLop() == maLop) return x;
            return null;
        }
        void napLocLop(int maLopMuonChon) {
            dangNap = true;
            locLop.removeAllItems();
            locLop.addItem(new LuaChon(-1, "Tất cả lớp học"));
            int mk = maKhoaDangLoc();
            for (LopTrungTam l : lop) {
                if (mk <= 0 || l.maKhoaHoc() == mk)
                    locLop.addItem(new LuaChon(l.maLop(), l.tenLop()));
            }
            chon(locLop, maLopMuonChon);
            dangNap = false;
            hienThiTheoBoLoc();
        }
        void hienThiTheoBoLoc() {
            if (dangNap) return;
            int maKhoa = maKhoaDangLoc();
            int maLop = maLopDangLoc();
            int maGiaoVien = maGiaoVienDangLoc();
            int maPhong = maPhongDangLoc();
            int maHocVien = maHocVienDangLoc();
            model.setRowCount(0);
            hienThi.clear();
            for (LichHocLop x : theoMon) {
                if (maKhoa > 0 && x.maKhoaHoc() != maKhoa) continue;
                if (maLop > 0 && x.maLop() != maLop) continue;
                LopTrungTam lopCuaLich = lopTheoMa(x.maLop());
                if (maGiaoVien > 0 && (lopCuaLich == null
                        || !Integer.valueOf(maGiaoVien).equals(lopCuaLich.maGiaoVien()))) continue;
                if (maPhong > 0 && !Integer.valueOf(maPhong).equals(x.maPhong())) continue;
                if (maHocVien <= 0) {
                    themDong(x, lopCuaLich);
                } else {
                    for (HocVienTheoLop hv : hocVien) {
                        if (hv.maHocVien() != maHocVien || hv.maLop() != x.maLop()) continue;
                        LichHocLop trongDotXep = LichHocLopService.gioiHanTheoXep(x, hv);
                        if (trongDotXep != null) themDong(trongDotXep, lopCuaLich);
                    }
                }
            }
            if (lichDaChon != null && hienThi.stream().noneMatch(x ->
                    x.maLichHoc() == lichDaChon.maLichHoc())) lichDaChon = null;
            lichTuan.datDanhSach(hienThi);
            Set<Integer> lopDangXem = new java.util.HashSet<>();
            for (LichHocLop x : hienThi) lopDangXem.add(x.maLop());
            if (maLop > 0) lopDangXem.add(maLop);
            lichTuan.datBuoiDieuChinh(buoiDieuChinh.stream()
                    .filter(x -> lopDangXem.contains(x.maLop())).toList());
            if (maMon != -1) hienThiTheLop();
        }

        private void themDong(LichHocLop x, LopTrungTam lopCuaLich) {
            hienThi.add(x);
            model.addRow(new Object[]{
                    x.tenLop(), x.tenKhoaHoc(),
                    lopCuaLich == null || lopCuaLich.tenGiaoVien() == null
                            ? "Chưa phân công" : lopCuaLich.tenGiaoVien(),
                    x.tenPhong() == null ? "Chưa gán" : x.tenPhong(),
                    LichHocLopService.tenThu(x.thuTrongTuan()),
                    hienThiGio(x.gioBatDau()), hienThiGio(x.gioKetThuc()),
                    NgayLichHocPicker.hienThiNgay(x.tuNgay()),
                    NgayLichHocPicker.hienThiNgay(x.denNgay()),
                    "HIEU_LUC".equals(x.trangThai()) ? "Hiệu lực" : "Đã hủy"
            });
        }

        private void hienThiTheLop() {
            luoiThe.removeAll();
            Map<Integer, List<LichHocLop>> nhom = new LinkedHashMap<>();
            for (LichHocLop x : hienThi)
                nhom.computeIfAbsent(x.maLop(), k -> new ArrayList<>()).add(x);
            if (nhom.isEmpty()) {
                JLabel rong = new JLabel("Không có lớp học phù hợp với bộ lọc.");
                rong.setForeground(new Color(0x52665B));
                rong.setFont(rong.getFont().deriveFont(14f));
                luoiThe.add(rong);
            } else {
                for (List<LichHocLop> ds : nhom.values()) {
                    ds.sort(Comparator.comparingInt(LichHocLop::thuTrongTuan)
                            .thenComparing(x -> x.gioBatDau(), Comparator.nullsLast(Comparator.naturalOrder())));
                    luoiThe.add(taoTheLop(ds));
                }
            }
            luoiThe.revalidate();
            luoiThe.repaint();
        }

        private JPanel taoTheLop(List<LichHocLop> ds) {
            LichHocLop dau = ds.get(0);
            int[] nen = {0xEBE0FF, 0xDCEEFF, 0xFFF0C8, 0xDDF6E6, 0xFFE6E0, 0xDEF2F3};
            Color mau = new Color(nen[Math.floorMod(dau.maLop(), nen.length)]);
            JPanel card = new JPanel(new BorderLayout(0, 8));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xD6E3DB)),
                    BorderFactory.createEmptyBorder(0, 0, 10, 0)));

            JPanel header = new JPanel(new BorderLayout(6, 4));
            header.setBackground(mau);
            header.setBorder(BorderFactory.createEmptyBorder(12, 11, 12, 9));
            JLabel ten = new JLabel("<html><div style='width:210px'>" + esc(dau.tenLop()) + "</div></html>");
            ten.setFont(ten.getFont().deriveFont(Font.BOLD, 14f));
            ten.setForeground(new Color(0x113D29));
            header.add(ten, BorderLayout.CENTER);
            boolean conHieuLuc = ds.stream().anyMatch(x -> "HIEU_LUC".equals(x.trangThai()));
            JLabel trangThai = new JLabel(conHieuLuc ? "Hiệu lực" : "Đã hủy");
            trangThai.setFont(trangThai.getFont().deriveFont(Font.BOLD, 11f));
            trangThai.setForeground(conHieuLuc ? new Color(0x006632) : new Color(0xB33B36));
            header.add(trangThai, BorderLayout.EAST);
            card.add(header, BorderLayout.NORTH);

            JPanel thongTin = new JPanel();
            thongTin.setOpaque(false);
            thongTin.setLayout(new BoxLayout(thongTin, BoxLayout.Y_AXIS));
            thongTin.setBorder(BorderFactory.createEmptyBorder(4, 12, 5, 12));
            JLabel khoaHoc = new JLabel("<html><b>Khóa học:</b> " + esc(dau.tenKhoaHoc()) + "</html>");
            khoaHoc.setAlignmentX(LEFT_ALIGNMENT);
            thongTin.add(khoaHoc);
            KhoaHoc khoaCuaLop = khoa.stream()
                    .filter(k -> k.maKhoaHoc() == dau.maKhoaHoc()).findFirst().orElse(null);
            LopTrungTam lopCuaLich = lop.stream()
                    .filter(l -> l.maLop() == dau.maLop()).findFirst().orElse(null);
            if (khoaCuaLop != null && lopCuaLich != null) {
                JLabel giaoVien = new JLabel("Giáo viên: " +
                        (lopCuaLich.tenGiaoVien() == null ? "Chưa phân công"
                                : lopCuaLich.tenGiaoVien()));
                giaoVien.setAlignmentX(LEFT_ALIGNMENT);
                thongTin.add(giaoVien);
                List<LichHocLop> lichCuaLop = lichDayDu.stream()
                        .filter(x -> x.maLop() == dau.maLop()).toList();
                List<DuKienKetThucLop.BuoiDieuChinh> buoiCuaLop = buoiDieuChinh.stream()
                        .filter(x -> x.maLop() == dau.maLop()).toList();
                LocalDate batDau = lopCuaLich.ngayKhaiGiang();
                if (batDau == null) batDau = lichCuaLop.stream()
                        .filter(x -> "HIEU_LUC".equals(x.trangThai()))
                        .map(LichHocLop::tuNgay).min(LocalDate::compareTo).orElse(null);
                String duKien = batDau == null ? "Chưa xác định" :
                        DuKienKetThucLop.tinh(batDau, khoaCuaLop.tongSoBuoi(),
                                        lichCuaLop, buoiCuaLop, Set.of())
                                .map(NgayLichHocPicker::hienThiNgay)
                                .orElse("Lịch hiện tại chưa đủ buổi");
                JLabel ketThuc = new JLabel("Dự kiến kết thúc: " + duKien);
                ketThuc.setAlignmentX(LEFT_ALIGNMENT);
                ketThuc.setToolTipText("Tính từ ngày khai giảng và các ca đang hiệu lực; "
                        + "chưa trừ buổi nghỉ hoặc thay đổi thực tế.");
                thongTin.add(ketThuc);
            }
            thongTin.add(new JLabel(" "));
            JLabel soBuoi = new JLabel("Lịch học (" + ds.size() + " buổi):");
            soBuoi.setFont(soBuoi.getFont().deriveFont(Font.BOLD, 12f));
            soBuoi.setAlignmentX(LEFT_ALIGNMENT);
            thongTin.add(soBuoi);
            JPanel cacBuoi = new JPanel(new GridLayout(0, 1, 4, 4));
            cacBuoi.setOpaque(true);
            cacBuoi.setBackground(new Color(0xF6F9F7));
            cacBuoi.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE0E8E3)),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            for (LichHocLop lh : ds) {
                String gio = hienThiGio(lh.gioBatDau()) + " – " + hienThiGio(lh.gioKetThuc());
                JLabel buoi = new JLabel("<html><b>" + esc(LichHocLopService.tenThu(lh.thuTrongTuan()))
                        + ":</b> " + esc(gio) + " · "
                        + esc(lh.tenPhong() == null ? "Chưa gán phòng" : lh.tenPhong())
                        + ("HUY".equals(lh.trangThai())
                        ? " <font color='#A94442'>(Đã hủy)</font>" : "") + "</html>");
                buoi.setToolTipText("Từ " + NgayLichHocPicker.hienThiNgay(lh.tuNgay())
                        + " đến " + (lh.denNgay() == null ? "không giới hạn"
                        : NgayLichHocPicker.hienThiNgay(lh.denNgay())));
                cacBuoi.add(buoi);
            }
            cacBuoi.setAlignmentX(LEFT_ALIGNMENT);
            thongTin.add(cacBuoi);
            card.add(thongTin, BorderLayout.CENTER);

            // Bam vao BAT KY vi tri nao cua the de sua; khong con ba nut Chi tiet/Sua/Huy.
            card.setToolTipText("Nhấn vào thẻ lớp để sửa lịch học");
            ganClickThe(card, () -> khiSuaCard.accept(ds.stream()
                    .map(x -> lichDayDu.stream()
                            .filter(goc -> goc.maLichHoc() == x.maLichHoc())
                            .findFirst().orElse(x))
                    .toList()));
            card.setPreferredSize(new Dimension(306, 229 + Math.max(0, ds.size() - 2) * 26));
            return card;
        }

        private static void ganClickThe(java.awt.Component c, Runnable action) {
            c.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            c.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (javax.swing.SwingUtilities.isLeftMouseButton(e)) action.run();
                }
            });
            if (c instanceof java.awt.Container container)
                for (java.awt.Component child : container.getComponents()) ganClickThe(child, action);
        }

        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;");
        }
    }

    /**
     * Nut chuyen che do tu ve nen, chu va bieu tuong de khong bi Look & Feel
     * ghi de mau (mot so giao dien Swing lam chu trang tren nen sang).
     */
    private static final class NutCheDo extends JButton {
        private static final Color XANH = new Color(0x087B38);
        private static final Color XANH_CHU = new Color(0x075C2B);
        private static final Color VIEN = new Color(0x91B69F);
        private boolean duocChon;
        private final boolean laLich;

        NutCheDo(String nhan, boolean laLich) {
            super(nhan);
            this.laLich = laLich;
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            setPreferredSize(new Dimension(laLich ? 156 : 145, 34));
            setMinimumSize(new Dimension(laLich ? 145 : 135, 32));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            getAccessibleContext().setAccessibleName(nhan);
        }

        void datDuocChon(boolean duocChon) {
            this.duocChon = duocChon;
            setForeground(duocChon ? Color.WHITE : XANH_CHU);
            setBackground(duocChon ? XANH : Color.WHITE);
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color nen = duocChon ? XANH : Color.WHITE;
                if (getModel().isPressed()) nen = duocChon ? new Color(0x055628) : new Color(0xE0F2E6);
                else if (getModel().isRollover()) nen = duocChon ? new Color(0x086B33) : new Color(0xF0F9F2);
                g.setColor(nen);
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g.setColor(duocChon ? XANH : VIEN);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                g.setColor(duocChon ? Color.WHITE : XANH_CHU);
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int iconX = 13;
                int cy = getHeight() / 2;
                if (laLich && "Dạng ô".equals(getText())) {
                    for (int row = 0; row < 2; row++)
                        for (int col = 0; col < 2; col++)
                            g.drawRoundRect(iconX + col * 10, cy - 9 + row * 10, 7, 7, 1, 1);
                } else if (laLich) {
                    g.drawRoundRect(iconX, cy - 9, 17, 17, 2, 2);
                    g.drawLine(iconX, cy - 4, iconX + 17, cy - 4);
                    g.drawLine(iconX + 5, cy - 12, iconX + 5, cy - 6);
                    g.drawLine(iconX + 12, cy - 12, iconX + 12, cy - 6);
                    g.fillRect(iconX + 4, cy, 3, 3);
                    g.fillRect(iconX + 10, cy, 3, 3);
                } else {
                    for (int i = -6; i <= 6; i += 6) {
                        g.fillOval(iconX, cy + i - 1, 3, 3);
                        g.drawLine(iconX + 7, cy + i, iconX + 20, cy + i);
                    }
                }
                g.setFont(getFont());
                g.drawString(getText(), 41, cy + (g.getFontMetrics().getAscent() -
                        g.getFontMetrics().getDescent()) / 2);
            } finally {
                g.dispose();
            }
        }
    }

    public LichHocLopPanel() {
        super(new BorderLayout(8, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("QUẢN LÝ LỊCH HỌC – TRUNG TÂM");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnTim = new NutMau("Tìm kiếm", NutMau.Kieu.CHINH);
        JButton btnTaiLai = new NutMau("Làm mới", NutMau.Kieu.PHU);
        JButton btnThem = new NutMau("Thêm", NutMau.Kieu.CHINH);
        JButton btnHuyBuoi = new NutMau("Hủy buổi học", NutMau.Kieu.PHU);
        JButton btnThemBu = new NutMau("Thêm buổi học bù", NutMau.Kieu.CHINH);
                btnTaiLai.setToolTipText("Tải dữ liệu mới từ SQL Server và giữ nguyên bộ lọc đang chọn");
        btnXoaLoc.setToolTipText("Bỏ từ khóa, trạng thái, khóa, lớp, giáo viên, phòng và học viên đang lọc");
        btnXoaLoc.setVisible(false);  // Chi hien khi thuc su dang loc.
        toolbar.add(new JLabel("Môn / khóa / lớp / thứ:"));
        toolbar.add(txtTim);
        toolbar.add(new JLabel("Trạng thái:"));
        toolbar.add(locTrangThai);
        toolbar.add(btnTim); toolbar.add(btnTaiLai);
        toolbar.add(btnXoaLoc);
        JPanel thanhTacVu = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 1));
        thanhTacVu.add(btnThem);
        thanhTacVu.add(btnHuyBuoi);
        thanhTacVu.add(btnThemBu);
        btnHuyBuoi.setToolTipText("Chỉ hủy một ngày học; lịch lặp vẫn được giữ");
        btnThemBu.setToolTipText("Chọn lớp, ngày, một trong 8 ca và phòng cho buổi bù");
        JPanel cacThanh = new JPanel(new BorderLayout(0, 3));
        cacThanh.add(toolbar, BorderLayout.NORTH);
        cacThanh.add(thanhTacVu, BorderLayout.SOUTH);
        JPanel dau = new JPanel(new BorderLayout(4, 8));
        dau.add(title, BorderLayout.NORTH);
        dau.add(cacThanh, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);
        add(tabMonHoc, BorderLayout.CENTER);
        add(thongBao, BorderLayout.SOUTH);

        btnTim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        txtTim.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { capNhatNutXoaLoc(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { capNhatNutXoaLoc(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { capNhatNutXoaLoc(); }
        });
        locTrangThai.addActionListener(e -> {
            capNhatNutXoaLoc();
            if (!dangXoaLoc) taiDuLieu();
        });
        // Lam moi khong xoa bo loc va van giu tab/tuan dang xem neu con hop le.
        btnTaiLai.addActionListener(e -> taiDuLieu());
        btnXoaLoc.addActionListener(e -> {
            dangXoaLoc = true;
            try {
                txtTim.setText("");
                locTrangThai.setSelectedIndex(0);
                TabTheoMon tab = tabDangChon();
                if (tab != null) {
                    tab.locKhoa.setSelectedIndex(0);
                    tab.locLop.setSelectedIndex(0);
                    tab.locGiaoVien.setSelectedIndex(0);
                    tab.locPhong.setSelectedIndex(0);
                    tab.locHocVien.setSelectedIndex(0);
                }
            } finally {
                dangXoaLoc = false;
            }
            capNhatNutXoaLoc();
            taiDuLieu();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnHuyBuoi.addActionListener(e -> moHuyMotNgay());
        btnThemBu.addActionListener(e -> moThemBuoiBu());
        tabMonHoc.addChangeListener(e -> {
            capNhatThongBao();
            capNhatNutXoaLoc();
        });
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    /** Nut Xoa loc chi xuat hien neu co it nhat mot dieu kien khac mac dinh. */
    private void capNhatNutXoaLoc() {
        TabTheoMon tab = tabDangChon();
        boolean coLoc = !txtTim.getText().isBlank() || locTrangThai.getSelectedIndex() > 0
                || (tab != null && (tab.maKhoaDangLoc() > 0 || tab.maLopDangLoc() > 0
                || tab.maGiaoVienDangLoc() > 0 || tab.maPhongDangLoc() > 0
                || tab.maHocVienDangLoc() > 0));
        if (btnXoaLoc.isVisible() != coLoc) {
            btnXoaLoc.setVisible(coLoc);
            btnXoaLoc.getParent().revalidate();
        }
    }

    private String trangThaiLoc() {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 -> "HIEU_LUC";
            case 2 -> "HUY";
            default -> null;
        };
    }
    private TabTheoMon tabDangChon() {
        int i = tabMonHoc.getSelectedIndex();
        return i >= 0 && i < cacTab.size() ? cacTab.get(i) : null;
    }

    private void taiDuLieu() {
        TabTheoMon cu = tabDangChon();
        final int maMonTruoc = cu == null ? -1 : cu.maMon;
        final int maKhoaTruoc = cu == null ? -1 : cu.maKhoaDangLoc();
        final int maLopTruoc = cu == null ? -1 : cu.maLopDangLoc();
        final int maGiaoVienTruoc = cu == null ? -1 : cu.maGiaoVienDangLoc();
        final int maPhongTruoc = cu == null ? -1 : cu.maPhongDangLoc();
        final int maHocVienTruoc = cu == null ? -1 : cu.maHocVienDangLoc();
        final int luot = ++lanTai;
        final String tim = txtTim.getText();
        final String trangThai = trangThaiLoc();
        thongBao.setText("Đang tải lịch học từ SQL Server...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.layMonHoc(), controller.layKhoaHoc(),
                        controller.layLop(), controller.timKiem(tim, trangThai),
                        controller.timKiem("", null), controller.layPhongHoc(),
                        controller.layHocVienTheoLop(), controller.layBuoiDieuChinh());
            }
            @Override protected void done() {
                if (luot != lanTai) return;
                try {
                    DuLieu dl = get();
                    danhSachPhong = dl.phong();
                    tatCaLich = dl.tatCa();
                    // Chi phuc hoi tab va bo loc truoc khi tai lai;
                    // khong tu dong nhay vao lop vua Them/Sua.
                    taoTabs(dl, maMonTruoc, maKhoaTruoc, maLopTruoc,
                            maGiaoVienTruoc, maPhongTruoc, maHocVienTruoc);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    /**
     * Tab Tong luon o vi tri dau, lay tat ca lich Trung tam do controller tra ve.
     * Cac tab mon cu giu nguyen cach loc; thay doi tuan va che do xem dong bo.
     */
    private void taoTabs(DuLieu dl, int monMuonChon, int khoaMuonLoc, int lopMuonLoc,
                         int giaoVienMuonLoc, int phongMuonLoc, int hocVienMuonLoc) {
        cacTab.clear();
        tabMonHoc.removeAll();

        // -1 la tab Tong; khong gan voi mot mon hoc cu the.
        TabTheoMon tong = taoTab(-1);
        for (KhoaHoc k : dl.khoa()) {
            tong.khoa.add(k);
            tong.locKhoa.addItem(new LuaChon(k.maKhoaHoc(), k.tenKhoaHoc()));
        }
        tong.lop.addAll(dl.lop());
        tong.hocVien.addAll(dl.hocVien());
        tong.buoiDieuChinh.addAll(dl.buoiDieuChinh());
        tong.theoMon.addAll(dl.lich());
        tong.lichDayDu.addAll(dl.tatCa());
        hoanTatTab(tong, monMuonChon == -1 ? khoaMuonLoc : -1,
                monMuonChon == -1 ? lopMuonLoc : -1,
                monMuonChon == -1 ? giaoVienMuonLoc : -1,
                monMuonChon == -1 ? phongMuonLoc : -1,
                monMuonChon == -1 ? hocVienMuonLoc : -1, dl.phong());
        cacTab.add(tong);
        tabMonHoc.addTab("Tổng (" + tong.theoMon.size() + ")", tong.panel);

        int viTri = monMuonChon == -1 ? 0 : -1;
        for (MonHoc mon : dl.mon()) {
            // Mon da ngung su dung khong tao tab, nhung lich cu van duoc giu trong SQL.
            if (!mon.trangThai()) continue;
            TabTheoMon tab = taoTab(mon.maMonHoc());
            for (KhoaHoc k : dl.khoa()) {
                if (k.maMonHoc() == mon.maMonHoc()) {
                    tab.khoa.add(k);
                    tab.locKhoa.addItem(new LuaChon(k.maKhoaHoc(), k.tenKhoaHoc()));
                }
            }
            for (LopTrungTam l : dl.lop()) {
                if (l.maMonHoc() == mon.maMonHoc()) tab.lop.add(l);
            }
            for (HocVienTheoLop hv : dl.hocVien()) {
                if (tab.lop.stream().anyMatch(l -> l.maLop() == hv.maLop()))
                    tab.hocVien.add(hv);
            }
            for (DuKienKetThucLop.BuoiDieuChinh buoi : dl.buoiDieuChinh()) {
                if (tab.lop.stream().anyMatch(l -> l.maLop() == buoi.maLop()))
                    tab.buoiDieuChinh.add(buoi);
            }
            for (LichHocLop lh : dl.lich()) {
                if (lh.maMonHoc() == mon.maMonHoc()) tab.theoMon.add(lh);
            }
            for (LichHocLop lh : dl.tatCa()) {
                if (lh.maMonHoc() == mon.maMonHoc()) tab.lichDayDu.add(lh);
            }
            hoanTatTab(tab, mon.maMonHoc() == monMuonChon ? khoaMuonLoc : -1,
                    mon.maMonHoc() == monMuonChon ? lopMuonLoc : -1,
                    mon.maMonHoc() == monMuonChon ? giaoVienMuonLoc : -1,
                    mon.maMonHoc() == monMuonChon ? phongMuonLoc : -1,
                    mon.maMonHoc() == monMuonChon ? hocVienMuonLoc : -1,
                    dl.phong());
            cacTab.add(tab);
            long soLop = tab.theoMon.stream().map(LichHocLop::maLop).distinct().count();
            tabMonHoc.addTab(mon.tenMonHoc() + " (" + soLop + " lớp · "
                    + tab.theoMon.size() + " buổi)", tab.panel);
            if (mon.maMonHoc() == monMuonChon) viTri = cacTab.size() - 1;
        }
        tabMonHoc.setSelectedIndex(viTri >= 0 ? viTri : 0);
        capNhatThongBao();
        capNhatNutXoaLoc();
    }

    private TabTheoMon taoTab(int maMon) {
        TabTheoMon tab = new TabTheoMon(maMon, tuanCanXem,
                tuan -> {
                    tuanCanXem = tuan;
                    for (TabTheoMon x : cacTab) x.lichTuan.datDauTuan(tuan);
                },
                hienTkb -> {
                    cheDoTungTab.put(maMon, hienTkb);
                    TabTheoMon x = tabDangChon();
                    if (x != null) x.datCheDo(hienTkb);
                },
                this::suaBuoiTrongThe,
                this::moForm);
        tab.datCheDo(true);
        tab.dangNap = true;
        tab.locKhoa.addItem(new LuaChon(-1, "Tất cả khóa học"));
        tab.dangNap = false;
        return tab;
    }

    private void hoanTatTab(TabTheoMon tab, int khoaMuonLoc, int lopMuonLoc,
                            int giaoVienMuonLoc, int phongMuonLoc,
                            int hocVienMuonLoc, List<PhongHoc> cacPhong) {
        tab.dangNap = true;
        tab.locGiaoVien.addItem(new LuaChon(-1, "Tất cả giáo viên"));
        tab.locPhong.addItem(new LuaChon(-1, "Tất cả phòng"));
        tab.locHocVien.addItem(new LuaChon(-1, "Tất cả học viên"));
        Map<Integer, String> giaoVien = new LinkedHashMap<>();
        Map<Integer, String> tenGiaoVienTheoLop = new LinkedHashMap<>();
        for (LopTrungTam l : tab.lop) {
            if (l.maGiaoVien() == null) continue;
            String ten = l.tenGiaoVien() == null ? "Mã " + l.maGiaoVien() : l.tenGiaoVien();
            giaoVien.putIfAbsent(l.maGiaoVien(), ten);
            tenGiaoVienTheoLop.put(l.maLop(), ten);
        }
        giaoVien.forEach((ma, ten) -> tab.locGiaoVien.addItem(new LuaChon(ma, ten)));
        tab.lichTuan.datGiaoVienTheoLop(tenGiaoVienTheoLop);
        tab.lichTuan.datThongTinLop(tab.lop);
        for (PhongHoc p : cacPhong) tab.locPhong.addItem(new LuaChon(p.maPhong(), p.tenPhong()));
        Map<Integer, String> hocVien = new LinkedHashMap<>();
        for (HocVienTheoLop h : tab.hocVien) hocVien.putIfAbsent(h.maHocVien(), h.hoTen());
        hocVien.forEach((ma, ten) -> tab.locHocVien.addItem(new LuaChon(ma, ten + " (#" + ma + ")")));
        chon(tab.locKhoa, khoaMuonLoc);
        chon(tab.locGiaoVien, giaoVienMuonLoc);
        chon(tab.locPhong, phongMuonLoc);
        chon(tab.locHocVien, hocVienMuonLoc);
        tab.dangNap = false;
        tab.napLocLop(lopMuonLoc);
        tab.locKhoa.addActionListener(e -> {
            if (!tab.dangNap) { tab.napLocLop(-1); capNhatThongBao(); capNhatNutXoaLoc(); }
        });
        tab.locLop.addActionListener(e -> {
            if (!tab.dangNap) { tab.hienThiTheoBoLoc(); capNhatThongBao(); capNhatNutXoaLoc(); }
        });
        for (JComboBox<LuaChon> cbo : List.of(tab.locGiaoVien, tab.locPhong, tab.locHocVien))
            cbo.addActionListener(e -> {
                if (!tab.dangNap) {
                    tab.hienThiTheoBoLoc(); capNhatThongBao(); capNhatNutXoaLoc();
                }
            });
    }

    private void capNhatThongBao() {
        TabTheoMon tab = tabDangChon();
        thongBao.setText(tab == null ? "Chưa có môn học Trung tâm"
                : "Có " + tab.hienThi.size() + " lịch học phù hợp trong tab đang chọn");
    }

    private static void chon(JComboBox<LuaChon> cbo, int ma) {
        for (int i = 0; i < cbo.getItemCount(); i++) {
            if (cbo.getItemAt(i).ma() == ma) { cbo.setSelectedIndex(i); return; }
        }
        cbo.setSelectedIndex(0);
    }

    private static final Color XANH_DAM = new Color(0x075F31);
    private static final Color XANH_NHAT = new Color(0xEAF7EF);
    private static final Color NEN_NHAT = new Color(0xF5FAF7);
    private static final Color CHU_DAM = new Color(0x163D2A);

    private static String ngayHieuLuc(LichHocLop lich) {
        return NgayLichHocPicker.hienThiNgay(lich.tuNgay()) + "  đến  "
                + (lich.denNgay() == null ? "Không giới hạn"
                : NgayLichHocPicker.hienThiNgay(lich.denNgay()));
    }

    private static String chuoiGio(LichHocLop lich) {
        return hienThiGio(lich.gioBatDau()) + " – " + hienThiGio(lich.gioKetThuc());
    }

    /** Tao tieu de ro chu, tuong thich giao dien Swing mac dinh. */
    private static JPanel tieuDeHopThoai(String ten, String phu) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(XANH_DAM);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 19, 15, 19));
        JLabel title = new JLabel(ten);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);
        if (phu != null && !phu.isBlank()) {
            JLabel sub = new JLabel(phu);
            sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            sub.setForeground(new Color(0xD9F5E3));
            panel.add(sub, BorderLayout.SOUTH);
        }
        return panel;
    }

    private static JLabel dongThongTin(String nhan, String giaTri) {
        JLabel label = new JLabel("<html><span style='color:#577364'>" + TabTheoMon.esc(nhan)
                + ":</span> &nbsp; <b>" + TabTheoMon.esc(giaTri) + "</b></html>");
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        return label;
    }

    /**
     * Ve nen va chu truc tiep de tranh Windows Look & Feel ghi de mau nut.
     * Dung chung cho cac nut Chi tiet, Chon buoi, Dong, Luu, Xac nhan huy.
     * Giu text doc duoc o ca trang thai hover, focus va disable.
     */
    private static JButton nutHopThoai(String ten, Color nen, Color chu) {
        JButton nut = new JButton(ten) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    Color mauNen = nen;
                    if (!isEnabled()) {
                        mauNen = new Color(0xE8EEEB);
                    } else if (getModel().isPressed()) {
                        mauNen = nen.darker();
                    } else if (getModel().isRollover()) {
                        mauNen = nen.equals(XANH_DAM) ? new Color(0x087A3F)
                                : nen.equals(new Color(0xBD3932)) ? new Color(0xA92E29)
                                : nen.equals(new Color(0x1865AD)) ? new Color(0x12558F)
                                : new Color(0xD9E9DF);
                    }
                    g2.setColor(mauNen);
                    g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                    g2.setStroke(new BasicStroke(1.25f));
                    g2.setColor(!isEnabled() ? new Color(0xA5B7AC)
                            : nen.equals(XANH_DAM) ? new Color(0x07522B)
                            : nen.equals(new Color(0xBD3932)) ? new Color(0x9B2A24)
                            : nen.equals(new Color(0x1865AD)) ? new Color(0x10477B)
                            : new Color(0xB4C7BC));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                    if (isFocusOwner()) {
                        g2.setColor(new Color(0x2D82D8));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 8, 8);
                    }
                    String noiDung = getText();
                    g2.setFont(getFont());
                    g2.setColor(!isEnabled() ? new Color(0x4D6154) : chu);
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(noiDung)) / 2;
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(noiDung, x, y);
                } finally {
                    g2.dispose();
                }
            }
        };
        nut.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        nut.setForeground(chu);
        nut.setBackground(nen);
        nut.setOpaque(false);
        nut.setContentAreaFilled(false);
        nut.setBorderPainted(false);
        nut.setFocusPainted(false);
        nut.setRolloverEnabled(true);
        nut.setPreferredSize(new Dimension(150, 42));
        nut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return nut;
    }

    private JDialog cuaSo(String tieuDe) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, tieuDe, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setMinimumSize(new Dimension(480, 360));
        return dialog;
    }

    private void suaBuoiTrongThe(List<LichHocLop> ds) {
        // Mot cua so Sua chung; bang da tich cac buoi dang hoc.
        if (!ds.isEmpty()) {
            LichHocLop dau = ds.stream().filter(x -> "HIEU_LUC".equals(x.trangThai()))
                    .findFirst().orElse(ds.get(0));
            moForm(dau);
        }
    }

    /** Mot ngay bi huy duoc ghi vao BuoiHoc; khong thay doi lich lap. */
    private void moHuyMotNgay() {
        TabTheoMon tab = tabDangChon();
        LocalDate homNay = LocalDate.now();
        int maLopLoc = tab == null ? -1 : tab.maLopDangLoc();
        List<LichHocLop> cacLich = tatCaLich.stream()
                .filter(x -> "HIEU_LUC".equals(x.trangThai()))
                .filter(x -> x.gioBatDau() != null && x.gioKetThuc() != null
                        && CaHocTrungTam.timTheoGio(x.gioBatDau(), x.gioKetThuc()).isPresent())
                .filter(x -> x.denNgay() == null || !x.denNgay().isBefore(homNay))
                .filter(x -> tab == null || tab.maMon < 0 || x.maMonHoc() == tab.maMon)
                .filter(x -> maLopLoc <= 0 || x.maLop() == maLopLoc)
                .filter(x -> ngayTiepTheo(x, homNay) != null)
                .toList();
        if (cacLich.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Không có lịch hiệu lực sắp tới trong bộ lọc hiện tại.",
                    "Chưa thể hủy buổi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<LuaChon> cboLich = new JComboBox<>();
        for (LichHocLop x : cacLich) cboLich.addItem(new LuaChon(x.maLichHoc(),
                x.tenLop() + " · " + LichHocLopService.tenThu(x.thuTrongTuan())
                + " " + chuoiGio(x) + " · từ "
                + NgayLichHocPicker.hienThiNgay(x.tuNgay())));
        cboLich.setPrototypeDisplayValue(new LuaChon(-1,
                "Tên lớp học dài · Chủ nhật 17:30–19:00 · từ 31/12/2035"));
        if (tab != null && tab.lichDaChon != null)
            chon(cboLich, tab.lichDaChon.maLichHoc());
        NgayLichHocPicker ngay = new NgayLichHocPicker(ngayTiepTheo(
                timLich(cacLich, ((LuaChon) cboLich.getSelectedItem()).ma()), homNay));
        cboLich.addActionListener(e -> {
            LuaChon x = (LuaChon) cboLich.getSelectedItem();
            if (x != null) ngay.datNgay(ngayTiepTheo(timLich(cacLich, x.ma()), homNay));
        });
        JTextField lyDo = new JTextField(30);
        JPanel form = formBuoi(
                truongForm("Lịch lặp cần hủy một ngày", cboLich),
                truongForm("Ngày học cần hủy", ngay),
                truongForm("Lý do hủy", lyDo));
        while (hienHopThoaiBuoi("HỦY MỘT BUỔI HỌC",
                "Chỉ ngày đã chọn bị hủy; lịch các tuần khác được giữ nguyên", form)) {
            try {
                LuaChon chon = (LuaChon) cboLich.getSelectedItem();
                LichHocLop lich = chon == null ? null : timLich(cacLich, chon.ma());
                LocalDate ngayHoc = ngay.layNgay();
                if (lich == null || ngayHoc == null || ngayHoc.isBefore(homNay)
                        || ngayHoc.isBefore(lich.tuNgay())
                        || (lich.denNgay() != null && ngayHoc.isAfter(lich.denNgay()))
                        || ngayHoc.getDayOfWeek().getValue() + 1 != lich.thuTrongTuan())
                    throw new IllegalArgumentException("Ngày hủy phải thuộc đúng thứ và khoảng ngày của lịch đã chọn");
                String lyDoLuu = lyDo.getText().trim();
                if (lyDoLuu.isBlank()) throw new IllegalArgumentException("Hãy nhập lý do hủy buổi");
                if (lyDoLuu.length() > 500)
                    throw new IllegalArgumentException("Lý do tối đa 500 ký tự");
                if (!xacNhanBuoi("Hủy buổi " + NgayLichHocPicker.hienThiNgay(ngayHoc)
                        + " của lớp " + lich.tenLop() + "?")) return;
                luuDieuChinhBuoi("Đang hủy một buổi học...", "Đã hủy một buổi học.",
                        () -> controller.huyBuoi(lich.maLichHoc(), ngayHoc, lyDoLuu));
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void moThemBuoiBu() {
        TabTheoMon tab = tabDangChon();
        int maLopLoc = tab == null ? -1 : tab.maLopDangLoc();
        List<LopTrungTam> cacLop = tab == null ? List.of() : tab.lop.stream()
                .filter(x -> !"DA_DONG".equals(x.trangThai()))
                .filter(x -> maLopLoc <= 0 || x.maLop() == maLopLoc).toList();
        List<PhongHoc> phongHoatDong = danhSachPhong.stream()
                .filter(x -> "HOAT_DONG".equals(x.trangThai())).toList();
        if (cacLop.isEmpty() || phongHoatDong.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Cần có lớp đang mở và phòng đang hoạt động để thêm buổi bù.",
                    "Chưa đủ điều kiện", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<LuaChon> cboLop = new JComboBox<>();
        for (LopTrungTam x : cacLop)
            cboLop.addItem(new LuaChon(x.maLop(), x.tenLop() + " · " + x.tenKhoaHoc()));
        cboLop.setPrototypeDisplayValue(new LuaChon(-1,
                "Tên lớp học khá dài · Tên khóa học khá dài"));
        if (tab.lichDaChon != null) chon(cboLop, tab.lichDaChon.maLop());
        NgayLichHocPicker ngay = new NgayLichHocPicker(LocalDate.now().plusDays(1));
        JComboBox<CaHocTrungTam.Ca> cboCa = new JComboBox<>(
                CaHocTrungTam.DANH_SACH.toArray(new CaHocTrungTam.Ca[0]));
        JComboBox<PhongHoc> cboPhong = new JComboBox<>();
        cboPhong.addItem(new PhongHoc(0, 0, "", 0, ""));
        for (PhongHoc p : phongHoatDong) cboPhong.addItem(p);
        cboPhong.setPrototypeDisplayValue(new PhongHoc(9999, 0,
                "Tên phòng học khá dài", 99, "HOAT_DONG"));
        cboPhong.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof PhongHoc p && p.maPhong() > 0)
                    setText(p.tenPhong() + " (" + p.sucChua() + " chỗ) · Hoạt động");
                return this;
            }
        });
        JTextField ghiChu = new JTextField(30);
        JPanel form = formBuoi(
                truongForm("Lớp học", cboLop),
                truongForm("Ngày học bù", ngay),
                truongForm("Ca học cố định", cboCa),
                truongForm("Một phòng học (tên · sức chứa · trạng thái)", cboPhong),
                truongForm("Ghi chú", ghiChu));
        while (hienHopThoaiBuoi("THÊM BUỔI HỌC BÙ",
                "Trùng lớp, giáo viên, phòng và học viên được kiểm tra trước khi ghi SQL", form)) {
            try {
                LuaChon lopChon = (LuaChon) cboLop.getSelectedItem();
                PhongHoc phong = (PhongHoc) cboPhong.getSelectedItem();
                CaHocTrungTam.Ca ca = (CaHocTrungTam.Ca) cboCa.getSelectedItem();
                LocalDate ngayHoc = ngay.layNgay();
                if (lopChon == null || ngayHoc == null || ngayHoc.isBefore(LocalDate.now())
                        || ca == null || phong == null || phong.maPhong() <= 0)
                    throw new IllegalArgumentException("Hãy chọn lớp, ngày hiện tại hoặc tương lai, ca và phòng học");
                String ghiChuLuu = ghiChu.getText().trim();
                if (ghiChuLuu.length() > 500)
                    throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự");
                if (!xacNhanBuoi("Thêm buổi bù "
                        + NgayLichHocPicker.hienThiNgay(ngayHoc) + ", ca " + ca.soCa()
                        + " tại " + phong.tenPhong() + " cho " + lopChon.ten() + "?")) return;
                luuDieuChinhBuoi("Đang kiểm tra trùng và lưu buổi bù...",
                        "Đã thêm buổi học bù.", () -> controller.themBuoiBu(
                                lopChon.ma(), phong.maPhong(), ngayHoc, ca.soCa(), ghiChuLuu));
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static LichHocLop timLich(List<LichHocLop> cacLich, int maLich) {
        return cacLich.stream().filter(x -> x.maLichHoc() == maLich).findFirst().orElse(null);
    }

    private static LocalDate ngayTiepTheo(LichHocLop lich, LocalDate tuNgay) {
        if (lich == null) return null;
        LocalDate ngay = tuNgay.isAfter(lich.tuNgay()) ? tuNgay : lich.tuNgay();
        while (ngay.getDayOfWeek().getValue() + 1 != lich.thuTrongTuan())
            ngay = ngay.plusDays(1);
        if (ngay.equals(LocalDate.now()) && lich.gioBatDau() != null
                && !LocalTime.now().isBefore(lich.gioBatDau()))
            ngay = ngay.plusWeeks(1);
        return lich.denNgay() != null && ngay.isAfter(lich.denNgay()) ? null : ngay;
    }

    private static JPanel formBuoi(JComponent... truong) {
        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        for (JComponent x : truong) {
            x.setAlignmentX(LEFT_ALIGNMENT);
            x.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
            form.add(x);
            form.add(Box.createVerticalStrut(8));
        }
        return form;
    }

    private boolean hienHopThoaiBuoi(String ten, String phu, JPanel form) {
        JDialog dialog = cuaSo(ten);
        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(Color.WHITE);
        goc.add(tieuDeHopThoai(ten, phu), BorderLayout.NORTH);
        JScrollPane cuon = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.setBorder(BorderFactory.createEmptyBorder());
        cuon.getVerticalScrollBar().setUnitIncrement(18);
        goc.add(cuon, BorderLayout.CENTER);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        nut.setBackground(Color.WHITE);
        JButton dong = nutHopThoai("Đóng", new Color(0xE8EEEB), CHU_DAM);
        JButton tiepTuc = nutHopThoai("Kiểm tra và tiếp tục", XANH_DAM, Color.WHITE);
        tiepTuc.setPreferredSize(new Dimension(205, 38));
        final boolean[] chon = {false};
        dong.addActionListener(e -> dialog.dispose());
        tiepTuc.addActionListener(e -> { chon[0] = true; dialog.dispose(); });
        nut.add(dong);
        nut.add(tiepTuc);
        goc.add(nut, BorderLayout.SOUTH);
        dialog.setContentPane(goc);
        int cao = Math.min(590, java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment().getMaximumWindowBounds().height - 35);
        dialog.setSize(700, cao);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return chon[0];
    }

    private boolean xacNhanBuoi(String noiDung) {
        return JOptionPane.showConfirmDialog(this,
                "<html><div style='width:390px'>" + TabTheoMon.esc(noiDung)
                        + "<br><br>Ngày kết thúc dự kiến sẽ được tính lại khi lưu.</div></html>",
                "Xác nhận thay đổi buổi học", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void luuDieuChinhBuoi(String dangLam, String thanhCong,
                                  java.util.concurrent.Callable<Integer> ghi) {
        thongBao.setText(dangLam);
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return ghi.call(); }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(LichHocLopPanel.this, thanhCong);
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    /** Xac nhan huy: khong bao gio xoa dong du lieu; co nut Quay lai ro rang. */
    private boolean xacNhanHuy(LichHocLop lich) {
        JDialog dialog = cuaSo("Xác nhận hủy lịch lặp");
        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(Color.WHITE);
        goc.add(tieuDeHopThoai("HỦY LỊCH LẶP", "Kiểm tra đúng thứ và ca trước khi xác nhận"), BorderLayout.NORTH);
        JPanel than = new JPanel(new GridLayout(5, 1, 0, 9));
        than.setBackground(Color.WHITE);
        than.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        JLabel canhBao = new JLabel("Hủy lịch lặp từ hôm nay trở đi?");
        canhBao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        canhBao.setForeground(new Color(0xB13B32));
        than.add(canhBao);
        than.add(dongThongTin("Lớp", lich.tenLop()));
        than.add(dongThongTin("Buổi", LichHocLopService.tenThu(lich.thuTrongTuan()) + "   " + chuoiGio(lich)));
        than.add(dongThongTin("Áp dụng", ngayHieuLuc(lich)));
        JLabel note = new JLabel("Các buổi đã có vẫn được giữ; nếu có buổi tương lai, thao tác sẽ bị chặn.");
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        note.setForeground(new Color(0x66776C));
        than.add(note);
        goc.add(than, BorderLayout.CENTER);
        JPanel cuoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        cuoi.setBackground(Color.WHITE);
        JButton quayLai = nutHopThoai("Quay lại", new Color(0xE8EEEB), CHU_DAM);
        JButton xacNhan = nutHopThoai("Xác nhận hủy", new Color(0xBD3932), Color.WHITE);
        final boolean[] dongY = {false};
        quayLai.addActionListener(e -> dialog.dispose());
        xacNhan.addActionListener(e -> { dongY[0] = true; dialog.dispose(); });
        cuoi.add(quayLai);
        cuoi.add(xacNhan);
        goc.add(cuoi, BorderLayout.SOUTH);
        dialog.setContentPane(goc);
        dialog.setSize(665, 365);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return dongY[0];
    }

    /** Bieu mau lon dung chung cho Them, Sua tu the lop va click o thoi khoa bieu. */
    /** Dialog dung chung cho THEM va SUA; nut Huy buoi chi xuat hien khi Sua. */
    private int hienFormLich(JPanel form, boolean laThem) {
        JDialog dialog = cuaSo(laThem ? "Thêm lịch học" : "Sửa lịch học");
        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(Color.WHITE);
        goc.add(tieuDeHopThoai(laThem ? "THÊM LỊCH HỌC" : "SỬA LỊCH HỌC",
                "8 ca liên tục 07:00–19:00 (90 phút/ca), không có giờ nghỉ giữa ca"), BorderLayout.NORTH);
        JScrollPane cuon = new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cuon.setBorder(BorderFactory.createEmptyBorder());
        cuon.getVerticalScrollBar().setUnitIncrement(25);
        cuon.getViewport().setBackground(Color.WHITE);
        goc.add(cuon, BorderLayout.CENTER);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 9));
        nut.setBackground(Color.WHITE);
        JButton dong = nutHopThoai("Hủy", new Color(0xE8EEEB), CHU_DAM);
        JButton luu = nutHopThoai(laThem ? "Lưu lịch học" : "Lưu thay đổi", XANH_DAM, Color.WHITE);
        final int[] ketQua = {0}; // 0=dong, 1=luu, 2=huy buoi duoc chon
        dong.addActionListener(e -> dialog.dispose());
        luu.addActionListener(e -> { ketQua[0] = 1; dialog.dispose(); });
        if (!laThem) {
            JButton huyBuoi = nutHopThoai("HỦY LỊCH LẶP", new Color(0xBD3932), Color.WHITE);
            huyBuoi.addActionListener(e -> { ketQua[0] = 2; dialog.dispose(); });
            nut.add(huyBuoi);
        }
        nut.add(dong);
        nut.add(luu);
        goc.add(nut, BorderLayout.SOUTH);
        dialog.setContentPane(goc);
        java.awt.Rectangle manHinh = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        dialog.setSize(Math.min(1250, manHinh.width - 30),
                Math.min(720, manHinh.height - 28));
        dialog.setMinimumSize(new Dimension(Math.min(920, manHinh.width - 30),
                Math.min(540, manHinh.height - 28)));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return ketQua[0];
    }

    private static JPanel tieuDeMuc(String text) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(XANH_DAM);
        section.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, new Color(0xFFDC00)));
        JLabel title = new JLabel(text);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        section.add(title, BorderLayout.CENTER);
        return section;
    }

    private static JPanel truongForm(String nhan, JComponent o) {
        JPanel p = new JPanel(new BorderLayout(8, 5));
        p.setOpaque(false);
        JLabel label = new JLabel(nhan);
        label.setForeground(CHU_DAM);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        p.add(label, BorderLayout.NORTH);
        p.add(o, BorderLayout.CENTER);
        return p;
    }

    /** Mo form THEM/SUA cung mot bang ca va bo chon phong; khong co combobox chon buoi. */
    private void moForm(LichHocLop banDau) {
        TabTheoMon tab = tabDangChon();
        if (tab == null) {
            JOptionPane.showMessageDialog(this, "Chưa có dữ liệu lớp Trung tâm");
            return;
        }
        boolean laThem = banDau == null;
        JComboBox<LuaChon> cboKhoa = new JComboBox<>();
        for (KhoaHoc k : tab.khoa)
            if (k.trangThai() || (banDau != null && banDau.maKhoaHoc() == k.maKhoaHoc()))
                cboKhoa.addItem(new LuaChon(k.maKhoaHoc(), k.tenKhoaHoc()));
        if (cboKhoa.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "Chưa có khóa học Trung tâm để lập lịch.");
            return;
        }
        chon(cboKhoa, laThem ? tab.maKhoaDangLoc() : banDau.maKhoaHoc());
        JComboBox<LuaChon> cboLop = new JComboBox<>();
        Runnable napLop = () -> {
            LuaChon khoa = (LuaChon) cboKhoa.getSelectedItem();
            cboLop.removeAllItems();
            for (LopTrungTam lop : tab.lop)
                if (khoa != null && lop.maKhoaHoc() == khoa.ma()
                        && (!"DA_DONG".equals(lop.trangThai()) ||
                           (banDau != null && banDau.maLop() == lop.maLop())))
                    cboLop.addItem(new LuaChon(lop.maLop(), lop.tenLop()));
        };
        napLop.run();
        if (laThem && cboLop.getItemCount() == 0) {
            for (int i = 0; i < cboKhoa.getItemCount(); i++) {
                cboKhoa.setSelectedIndex(i);
                napLop.run();
                if (cboLop.getItemCount() > 0) break;
            }
        }
        if (cboLop.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "Chưa có lớp phù hợp. Hãy tạo lớp trước.");
            return;
        }
        chon(cboLop, laThem ? tab.maLopDangLoc() : banDau.maLop());
        cboKhoa.addActionListener(e -> napLop.run());
        cboKhoa.setEnabled(laThem);
        cboLop.setEnabled(laThem);
        JTextArea giaoVienLop = new JTextArea("Chưa phân công", 2, 20);
        giaoVienLop.setEditable(false);
        giaoVienLop.setFocusable(false);
        giaoVienLop.setLineWrap(true);
        giaoVienLop.setWrapStyleWord(true);
        giaoVienLop.setFont(new JLabel().getFont().deriveFont(Font.PLAIN, 13f));
        giaoVienLop.setForeground(CHU_DAM);
        giaoVienLop.setBackground(new Color(0xF3F8F4));
        giaoVienLop.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        giaoVienLop.setToolTipText("Giáo viên hiện gắn với lớp; chuyển giáo viên được quản lý tại Lớp học.");
        Runnable napGiaoVien = () -> {
            LuaChon chonLop = (LuaChon) cboLop.getSelectedItem();
            LopTrungTam lopChon = chonLop == null ? null : tab.lop.stream()
                    .filter(l -> l.maLop() == chonLop.ma()).findFirst().orElse(null);
            String tenGiaoVien = lopChon == null || lopChon.tenGiaoVien() == null
                    ? "Chưa phân công" : lopChon.tenGiaoVien();
            giaoVienLop.setText(tenGiaoVien);
            giaoVienLop.setCaretPosition(0);
            giaoVienLop.setToolTipText(tenGiaoVien + " · Chuyển giáo viên tại màn hình Lớp học");
        };
        cboLop.addActionListener(e -> napGiaoVien.run());
        napGiaoVien.run();
        JPanel tenLop = new JPanel(new GridLayout(1, 3, 18, 0));
        tenLop.setOpaque(false);
        tenLop.setPreferredSize(new Dimension(1155, 60));
        tenLop.add(truongForm("Khóa học (*)", cboKhoa));
        tenLop.add(truongForm("Lớp học (*)", cboLop));
        tenLop.add(truongForm("Giáo viên lớp", giaoVienLop));

        List<LichHocLop> buoiCuaLop = laThem ? List.of() : tatCaLich.stream()
                .filter(x -> x.maLop() == banDau.maLop())
                .sorted(Comparator.comparingInt(LichHocLop::thuTrongTuan)
                        .thenComparing(LichHocLop::gioBatDau,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (!laThem && buoiCuaLop.stream().noneMatch(x -> x.maLichHoc() == banDau.maLichHoc())) {
            List<LichHocLop> all = new ArrayList<>(buoiCuaLop);
            all.add(banDau);
            buoiCuaLop = List.copyOf(all);
        }
        BangChonCaHocPanel bangCa = new BangChonCaHocPanel(!laThem);
        ChonPhongHocPanel chonPhong = new ChonPhongHocPanel(danhSachPhong);
        NgayLichHocPicker tuNgay = new NgayLichHocPicker(laThem ? LocalDate.now()
                : LichHocLopService.ngaySuaMacDinh(banDau.tuNgay(), LocalDate.now()));
        NgayLichHocPicker denNgay = new NgayLichHocPicker(laThem ? null : banDau.denNgay());
        JComboBox<String> cboTrangThai = new JComboBox<>(new String[]{"Hiệu lực", "Đã hủy"});
        JLabel dangGan = new JLabel("Chọn một ô ca học");
        dangGan.setForeground(new Color(0x126238));
        dangGan.setFont(dangGan.getFont().deriveFont(Font.BOLD, 12f));
        JLabel soCaDaChon = new JLabel("Đã chọn 0 ca · đã gán phòng 0/0");
        soCaDaChon.setForeground(CHU_DAM);
        JLabel cachGanPhong = new JLabel("Mỗi ô ca chọn đúng một phòng");
        cachGanPhong.setForeground(new Color(0x6B5A23));
        final Map<BangChonCaHocPanel.OCa, Integer> phongTheoBuoi = new LinkedHashMap<>();
        final LichHocLop[] banGhi = {banDau};
        Runnable capNhatSoCa = () -> {
            if (!laThem) return;
            List<BangChonCaHocPanel.OCa> cacO = bangCa.layCacO();
            phongTheoBuoi.keySet().retainAll(cacO);
            long daGan = cacO.stream().filter(o -> phongTheoBuoi.get(o) != null).count();
            soCaDaChon.setText("Đã chọn " + cacO.size() + " ca · đã gán phòng "
                    + daGan + "/" + cacO.size());
        };

        // Khi sua: bam o DA TICH de chuyen buoi, phong va ngay phai theo DUNG ban ghi.
        // Khi them: moi o co mot phong, cac o khac co the dung phong khac.
        bangCa.datKhiChon(cell -> {
            if (laThem) {
                chonPhong.chonPhong(cell == null ? null : phongTheoBuoi.get(cell));
            } else {
                LichHocLop moi = bangCa.layBanGhiDangSua();
                if (moi != null && moi != banGhi[0]) {
                    banGhi[0] = moi;
                    chonPhong.chonPhong(moi.maPhong());
                    tuNgay.datNgay(LichHocLopService.ngaySuaMacDinh(
                            moi.tuNgay(), LocalDate.now()));
                    denNgay.datNgay(moi.denNgay());
                    cboTrangThai.setSelectedIndex("HUY".equals(moi.trangThai()) ? 1 : 0);
                }
            }
            dangGan.setText(cell == null ? "Chọn một ô ca học"
                    : LichHocLopService.tenThu(cell.thu()) + " – Ca " + cell.soCa());
            capNhatSoCa.run();
        });
        chonPhong.datKhiChon(phong -> {
            if (laThem && bangCa.layODangChon() != null
                    && bangCa.layCacO().contains(bangCa.layODangChon()))
                phongTheoBuoi.put(bangCa.layODangChon(), phong == null ? null : phong.maPhong());
            capNhatSoCa.run();
        });
        if (!laThem) {
            bangCa.datBuoiDangSua(banDau, buoiCuaLop);
            chonPhong.chonPhong(banDau.maPhong());
            cboTrangThai.setSelectedIndex("HUY".equals(banDau.trangThai()) ? 1 : 0);
        }

        JPanel benTrai = new JPanel(new BorderLayout(0, 8));
        benTrai.setOpaque(false);
        benTrai.add(tieuDeMuc("1. CHỌN THỨ VÀ CA HỌC"), BorderLayout.NORTH);
        JPanel gridVaCu = new JPanel(new BorderLayout(0, 5));
        gridVaCu.setOpaque(false);
        gridVaCu.add(bangCa, BorderLayout.CENTER);
        if (!laThem) {
            // Lich cu ngoai 8 ca khong the danh dau sai vao bang. Cho chon ban ghi
            // truc tiep bang nut nho de doi thu/ca; KHONG dung combo chon buoi.
            List<LichHocLop> cu = buoiCuaLop.stream()
                    .filter(x -> "HIEU_LUC".equals(x.trangThai())
                            && CaHocTrungTam.timTheoGio(x.gioBatDau(), x.gioKetThuc()).isEmpty())
                    .toList();
            if (!cu.isEmpty()) {
                JPanel lichCu = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
                lichCu.setOpaque(false);
                lichCu.add(new JLabel("Lịch cũ cần đổi ca:"));
                for (LichHocLop x : cu) {
                    JButton b = new JButton(LichHocLopService.tenThu(x.thuTrongTuan())
                            + " " + chuoiGio(x));
                    b.addActionListener(e -> {
                        bangCa.chonBuoiGoc(x);
                        banGhi[0] = x;
                        chonPhong.chonPhong(x.maPhong());
                        tuNgay.datNgay(LichHocLopService.ngaySuaMacDinh(
                                x.tuNgay(), LocalDate.now()));
                        denNgay.datNgay(x.denNgay());
                        cboTrangThai.setSelectedIndex("HUY".equals(x.trangThai()) ? 1 : 0);
                    });
                    lichCu.add(b);
                }
                gridVaCu.add(lichCu, BorderLayout.SOUTH);
            }
        }
        benTrai.add(gridVaCu, BorderLayout.CENTER);

        JPanel benPhai = new JPanel(new BorderLayout(0, 7));
        benPhai.setOpaque(false);
        benPhai.add(tieuDeMuc("2. CHỌN PHÒNG HỌC"), BorderLayout.NORTH);
        JPanel phong = new JPanel(new BorderLayout(0, 6));
        phong.setOpaque(false);
        JPanel huongDan = new JPanel(new GridLayout(0, 1, 0, 3));
        huongDan.setOpaque(false);
        if (laThem) huongDan.add(soCaDaChon);
        huongDan.add(dangGan);
        huongDan.add(cachGanPhong);
        phong.add(huongDan, BorderLayout.NORTH);
        JScrollPane cuonPhong = new JScrollPane(chonPhong,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuonPhong.setBorder(BorderFactory.createEmptyBorder());
        cuonPhong.getVerticalScrollBar().setUnitIncrement(20);
        phong.add(cuonPhong, BorderLayout.CENTER);
        benPhai.add(phong, BorderLayout.CENTER);

        JPanel hangChon = new JPanel(new GridBagLayout());
        hangChon.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.fill = GridBagConstraints.BOTH; g.weighty = 1;
        g.gridx = 0; g.weightx = .67; g.insets = new Insets(0, 0, 0, 12);
        hangChon.add(benTrai, g);
        g.gridx = 1; g.weightx = .33; g.insets = new Insets(0, 0, 0, 0);
        hangChon.add(benPhai, g);
        hangChon.setPreferredSize(new Dimension(1155, 410));

        JPanel duoi = new JPanel(new BorderLayout(0, 9));
        duoi.setOpaque(false);
        duoi.add(tieuDeMuc("3. THỜI GIAN ÁP DỤNG"), BorderLayout.NORTH);
        JPanel dateRow = new JPanel(new GridLayout(1, 3, 14, 0));
        dateRow.setOpaque(false);
        dateRow.add(truongForm("Từ ngày (*)", tuNgay));
        dateRow.add(truongForm("Đến ngày (tùy chọn)", denNgay));
        dateRow.add(truongForm("Trạng thái", cboTrangThai));
        duoi.add(dateRow, BorderLayout.CENTER);
        if (!laThem) {
            JLabel luuY = new JLabel("Đổi lịch đã bắt đầu: chọn Từ ngày mới từ hôm nay trở đi; "
                    + "lịch cũ được giữ đến ngày trước đó.");
            luuY.setForeground(new Color(0x6B5A23));
            duoi.add(luuY, BorderLayout.SOUTH);
        }

        JPanel form = new JPanel(new BorderLayout(0, 15));
        form.setBorder(BorderFactory.createEmptyBorder(16, 17, 13, 17));
        form.setBackground(Color.WHITE);
        JPanel dauForm = new JPanel(new BorderLayout(0, 15));
        dauForm.setOpaque(false);
        dauForm.add(tenLop, BorderLayout.NORTH);
        dauForm.add(hangChon, BorderLayout.CENTER);
        form.add(dauForm, BorderLayout.CENTER);
        form.add(duoi, BorderLayout.SOUTH);
        form.setPreferredSize(new Dimension(1198, 600));

        while (true) {
            int hanhDong = hienFormLich(form, laThem);
            if (hanhDong == 0) return;
            if (hanhDong == 2) {
                LichHocLop x = bangCa.layBanGhiDangSua();
                if (x == null || "HUY".equals(x.trangThai())) {
                    JOptionPane.showMessageDialog(this, "Hãy chọn một buổi đang hiệu lực để hủy.");
                    continue;
                }
                if (xacNhanHuy(x)) { huyMotBuoi(x); return; }
                continue;
            }
            try {
                LuaChon lop = (LuaChon) cboLop.getSelectedItem();
                if (lop == null) throw new IllegalArgumentException("Hãy chọn lớp học còn hoạt động");
                String trangThai = cboTrangThai.getSelectedIndex() == 0 ? "HIEU_LUC" : "HUY";
                if (laThem) {
                    List<BuoiCoPhong> ds = bangCa.layCacO().stream()
                            .map(o -> new BuoiCoPhong(o.buoi(), phongTheoBuoi.get(o))).toList();
                    if (ds.isEmpty()) throw new IllegalArgumentException("Hãy chọn ít nhất một ô thứ/ca");
                    LopTrungTam lopChon = tab.lop.stream()
                            .filter(l -> l.maLop() == lop.ma()).findFirst().orElse(null);
                    int siSoCanThiet = lopChon == null ? 0
                            : Math.max(Math.max(lopChon.siSoHienTai(), lopChon.siSoDaDat()),
                                    lopChon.siSoToiDa() == null ? 0 : lopChon.siSoToiDa());
                    for (BuoiCoPhong b : ds) {
                        LichHocLopService.kiemTra(b.buoi().thu(), b.buoi().batDau(),
                                b.buoi().ketThuc(), tuNgay.layNgay(), denNgay.layNgay(), trangThai);
                        if ("HIEU_LUC".equals(trangThai)) {
                            String ca = LichHocLopService.tenThu(b.buoi().thu())
                                    + " – " + b.buoi().batDau();
                            PhongHoc phongChon = danhSachPhong.stream()
                                    .filter(p -> b.maPhong() != null && p.maPhong() == b.maPhong())
                                    .findFirst().orElse(null);
                            if (phongChon == null)
                                throw new IllegalArgumentException("Chưa chọn phòng cho " + ca);
                            if (!"HOAT_DONG".equals(phongChon.trangThai()))
                                throw new IllegalArgumentException("Phòng " + phongChon.tenPhong()
                                        + " không hoạt động ở " + ca);
                            if (phongChon.sucChua() < siSoCanThiet)
                                throw new IllegalArgumentException("Phòng " + phongChon.tenPhong()
                                        + " chỉ có " + phongChon.sucChua() + " chỗ; lớp cần "
                                        + siSoCanThiet + " chỗ ở " + ca);
                        }
                    }
                    luuNhieuBuoiTheoPhong(lop.ma(), ds, tuNgay.layNgay(),
                            denNgay.layNgay(), trangThai);
                } else {
                    LichHocLop goc = bangCa.layBanGhiDangSua();
                    if (goc == null)
                        throw new IllegalArgumentException("Hãy chọn lịch cũ cần thay đổi");
                    BuoiTrongTuan moi = bangCa.layBuoiDangSua();
                    PhongHoc phongChon = chonPhong.layPhong();
                    Integer maPhong = phongChon == null ? null : phongChon.maPhong();
                    if ("HIEU_LUC".equals(trangThai) && (phongChon == null
                            || !"HOAT_DONG".equals(phongChon.trangThai())))
                        throw new IllegalArgumentException("Hãy chọn phòng đang hoạt động cho buổi này");
                    LichHocLopService.kiemTraNgaySua(goc.tuNgay(), goc.denNgay(),
                            tuNgay.layNgay(), LocalDate.now());
                    LichHocLopService.kiemTra(moi.thu(), moi.batDau(), moi.ketThuc(),
                            tuNgay.layNgay(), denNgay.layNgay(), trangThai);
                    luuDuLieu(goc, lop.ma(), maPhong, moi.thu(), moi.batDau(),
                            moi.ketThuc(), tuNgay.layNgay(), denNgay.layNgay(), trangThai);
                }
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /** Chi huy trang thai, giu ban ghi va lich su; repository se chan lich da dien ra. */
    private void huyMotBuoi(LichHocLop x) {
        thongBao.setText("Đang hủy buổi học...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                LocalDate homNay = LocalDate.now();
                if (x.denNgay() != null && x.denNgay().isBefore(homNay))
                    throw new IllegalArgumentException("Lịch đã kết thúc; không hủy lịch sử");
                if (x.tuNgay().isBefore(homNay))
                    controller.suaTuNgay(x.maLichHoc(), x.maPhong(), x.thuTrongTuan(),
                            x.gioBatDau(), x.gioKetThuc(), homNay, x.denNgay(), "HUY");
                else controller.sua(x.maLichHoc(), x.maPhong(), x.thuTrongTuan(), x.gioBatDau(),
                        x.gioKetThuc(), x.tuNgay(), x.denNgay(), "HUY");
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(LichHocLopPanel.this, "Đã hủy buổi học được chọn.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private static String hienThiGio(LocalTime gio) {
        return gio == null ? "" : gio.format(DINH_DANG_GIO);
    }

    /** Them nhieu o, moi o co phong rieng; backend luu atomic. */
    private void luuNhieuBuoiTheoPhong(int maLop, List<BuoiCoPhong> cacBuoi,
                                      LocalDate ngayTu, LocalDate ngayDen, String trangThai) {
        thongBao.setText("Đang lưu " + cacBuoi.size() + " buổi học...");
        new SwingWorker<List<Integer>, Void>() {
            @Override protected List<Integer> doInBackground() throws Exception {
                return controller.themNhieuTheoPhong(maLop, cacBuoi, ngayTu, ngayDen, trangThai);
            }
            @Override protected void done() {
                try {
                    JOptionPane.showMessageDialog(LichHocLopPanel.this,
                            "Đã thêm thành công " + get().size() + " buổi học.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void luuDuLieu(LichHocLop cu, int maLop, Integer maPhong,
                          int thu, LocalTime gioDau, LocalTime gioCuoi,
                          LocalDate ngayTu, LocalDate ngayDen, String trangThai) {
        thongBao.setText("Đang lưu lịch học...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (cu == null) {
                    controller.them(maLop, maPhong, thu, gioDau, gioCuoi, ngayTu, ngayDen, trangThai);
                    return "Đã thêm lịch học Trung tâm";
                }
                if (ngayTu.isAfter(cu.tuNgay())) {
                    controller.suaTuNgay(cu.maLichHoc(), maPhong, thu,
                            gioDau, gioCuoi, ngayTu, ngayDen, trangThai);
                    return "Đã giữ lịch cũ và áp dụng lịch mới từ "
                            + NgayLichHocPicker.hienThiNgay(ngayTu);
                }
                controller.sua(cu.maLichHoc(), maPhong, thu, gioDau, gioCuoi,
                        ngayTu, ngayDen, trangThai);
                return "Đã cập nhật lịch học Trung tâm";
            }
            @Override protected void done() {
                try {
                    String thongDiep = get();
                    // taiDuLieu se tu giu tab, loc khoa, loc lop va tuan hien tai.
                    JOptionPane.showMessageDialog(LichHocLopPanel.this, thongDiep);
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
            if (sql.getErrorCode() == 547)
                msg = "Lịch đang liên kết với buổi học, hoặc lớp không hợp lệ. Hãy kiểm tra trước khi sửa.";
            else if (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)
                msg = "Lịch học bị trùng theo ràng buộc SQL Server.";
        }
        if (msg == null || msg.isBlank()) msg = "Không thực hiện được thao tác.";
        thongBao.setText("Lỗi: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
