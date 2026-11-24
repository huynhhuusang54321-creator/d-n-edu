package vn.vinabacus.view;

import java.awt.BorderLayout;
import vn.vinabacus.service.CaHocTrungTam;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.service.DuKienKetThucLop.BuoiDieuChinh;

/** Thoi khoa bieu tuan (chi xem), su dung du lieu LichHocLop hien co.
 * Khong thay doi database. Cac lop khac nhau duoc phep hoc cung gio.
 */
public final class ThoiKhoaBieuTuanPanel extends JPanel {
    private static final Color GREEN = new Color(0x087B38);
    private static final Color GRAY = new Color(0x5D6876);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color[] MAU_LOP = {
            new Color(0xDBECFF), new Color(0xE9D9FF), new Color(0xD8F5E2),
            new Color(0xFFEDC7), new Color(0xFFE0E0), new Color(0xD5F3F5),
            new Color(0xF5E6F3), new Color(0xE4E8FF)
    };
    private static final Color[] NET_LOP = {
            new Color(0x347AC8), new Color(0x9F5ED8), new Color(0x24965B),
            new Color(0xCA8A22), new Color(0xC45C62), new Color(0x2E9CA5),
            new Color(0xAE6DAD), new Color(0x686ACC)
    };

    private final JLabel lblTuan = new JLabel();
    private final JLabel lblThongKe = new JLabel();
    private final LuoiTuan luoi = new LuoiTuan();
    private final Consumer<LocalDate> khiDoiTuan;
    private Consumer<LichHocLop> khiChonLich = x -> { };
    private LocalDate dauTuan;
    private List<LichHocLop> danhSach = List.of();
    private List<BuoiDieuChinh> buoiDieuChinh = List.of();
    private Map<Integer, LopTrungTam> lopTheoMa = Map.of();
    private Map<Integer, String> giaoVienTheoLop = Map.of();

    public ThoiKhoaBieuTuanPanel(LocalDate ngay, Consumer<LocalDate> khiDoiTuan) {
        super(new BorderLayout(5, 6));
        this.khiDoiTuan = khiDoiTuan == null ? x -> { } : khiDoiTuan;
        dauTuan = veDauTuan(ngay == null ? LocalDate.now() : ngay);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDCE4E5)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JLabel title = new JLabel("THỜI KHÓA BIỂU – TRUNG TÂM");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(GREEN);
        JPanel hangTieuDe = new JPanel(new BorderLayout());
        hangTieuDe.setOpaque(false);
        hangTieuDe.add(title, BorderLayout.WEST);

        JButton truoc = new JButton("◀");
        JButton sau = new JButton("▶");
        JButton homNay = new JButton("Hôm nay");
        JButton chonTuan = new JButton("Chọn tuần");
        truoc.setToolTipText("Tuần trước");
        sau.setToolTipText("Tuần sau");
        truoc.addActionListener(e -> doiTuan(dauTuan.minusWeeks(1)));
        sau.addActionListener(e -> doiTuan(dauTuan.plusWeeks(1)));
        homNay.addActionListener(e -> doiTuan(LocalDate.now()));
        chonTuan.addActionListener(e -> {
            NgayLichHocPicker chon = new NgayLichHocPicker(dauTuan);
            int ok = JOptionPane.showConfirmDialog(this, chon,
                    "Chọn một ngày trong tuần cần xem", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (ok == JOptionPane.OK_OPTION && chon.layNgay() != null)
                doiTuan(chon.layNgay());
        });
        JPanel chuyenTuan = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
        chuyenTuan.setOpaque(false);
        chuyenTuan.add(truoc);
        chuyenTuan.add(lblTuan);
        chuyenTuan.add(sau);
        chuyenTuan.add(homNay);
        chuyenTuan.add(chonTuan);
        JPanel header = new JPanel(new BorderLayout(3, 6));
        header.setOpaque(false);
        header.add(hangTieuDe, BorderLayout.NORTH);
        header.add(chuyenTuan, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JScrollPane scroller = new JScrollPane(luoi);
        scroller.setBorder(BorderFactory.createLineBorder(new Color(0xDEE4E6)));
        scroller.getVerticalScrollBar().setUnitIncrement(30);
        // Tuan luon vua chieu ngang khung xem; chi cuon doc khi nhieu buoi.
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroller, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(4, 3));
        footer.setOpaque(false);
        lblThongKe.setForeground(GRAY);
        footer.add(lblThongKe, BorderLayout.NORTH);
        JLabel note = new JLabel("8 ca liên tục 07:00–19:00 · 90 phút/ca · Không có giờ nghỉ giữa các ca.");
        note.setForeground(GRAY);
        note.setFont(note.getFont().deriveFont(11f));
        footer.add(note, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        capNhat();
    }

    public void datXuLyChon(Consumer<LichHocLop> khiChonLich) {
        this.khiChonLich = khiChonLich == null ? x -> { } : khiChonLich;
    }
    public LocalDate layDauTuan() { return dauTuan; }
    public void datDauTuan(LocalDate ngay) {
        dauTuan = veDauTuan(ngay);
        capNhat();
    }
    private void doiTuan(LocalDate ngay) {
        datDauTuan(ngay);
        khiDoiTuan.accept(dauTuan);
    }
    public void datDanhSach(List<LichHocLop> lich) {
        danhSach = lich == null ? List.of() : List.copyOf(lich);
        capNhat();
    }
    public void datBuoiDieuChinh(List<BuoiDieuChinh> buoi) {
        buoiDieuChinh = buoi == null ? List.of() : List.copyOf(buoi);
        capNhat();
    }
    public void datThongTinLop(List<LopTrungTam> lop) {
        if (lop == null) lopTheoMa = Map.of();
        else {
            Map<Integer, LopTrungTam> theoMa = new java.util.HashMap<>();
            for (LopTrungTam x : lop) theoMa.put(x.maLop(), x);
            lopTheoMa = Map.copyOf(theoMa);
        }
        capNhat();
    }
    public void datGiaoVienTheoLop(Map<Integer, String> giaoVien) {
        giaoVienTheoLop = giaoVien == null ? Map.of() : Map.copyOf(giaoVien);
        capNhat();
    }
    private static LocalDate veDauTuan(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    private void capNhat() {
        LocalDate cuoi = dauTuan.plusDays(6);
        lblTuan.setText("  " + dauTuan.format(NGAY) + " – " +
                cuoi.format(NGAY) + "/" + cuoi.getYear() + "  ");
        int buoi = 0;
        Set<Integer> lop = new HashSet<>();
        for (LichHocLop x : danhSach) {
            if (!"HIEU_LUC".equals(x.trangThai()) ||
                    x.gioBatDau() == null || x.gioKetThuc() == null) continue;
            LocalDate ngay = dauTuan.plusDays(x.thuTrongTuan() - 2L);
            if (!ngay.isBefore(x.tuNgay()) &&
                    (x.denNgay() == null || !ngay.isAfter(x.denNgay()))
                    && !biHuy(x, ngay)) {
                buoi++;
                lop.add(x.maLop());
            }
        }
        for (BuoiDieuChinh b : buoiDieuChinh) {
            if ("DU_KIEN".equals(b.trangThai())
                    && !b.ngayHoc().isBefore(dauTuan)
                    && b.ngayHoc().isBefore(dauTuan.plusDays(7))
                    && b.soCa() >= 1 && b.soCa() <= 8) {
                buoi++;
                lop.add(b.maLop());
            }
        }
        lblThongKe.setText("Tuần này: " + buoi + " buổi học · " +
                lop.size() + " lớp · Bấm vào thẻ buổi học để sửa trực tiếp");
        luoi.capNhatKichThuoc();
    }

    private boolean biHuy(LichHocLop lich, LocalDate ngay) {
        for (BuoiDieuChinh b : buoiDieuChinh) {
            if (!"HUY".equals(b.trangThai()) || b.maLop() != lich.maLop()
                    || !ngay.equals(b.ngayHoc())) continue;
            if (b.maLichHoc() != null && b.maLichHoc() == lich.maLichHoc()) return true;
            if (b.maLichHoc() == null && CaHocTrungTam
                    .timTheoGio(lich.gioBatDau(), lich.gioKetThuc())
                    .map(c -> c.soCa() == b.soCa()).orElse(false)) return true;
        }
        return false;
    }

    /**
     * Luoi lich: moi ngay chi co 1 cot du rong, cac lop giao nhau ve gio
     * duoc XEP DOC trong cung nhom gio thay vi chia ngang thanh cac o hep.
     * Kich thuoc cua tung nhom gio duoc tinh theo cot co nhieu lop nhat,
     * nen khong bao gio che len nhom gio tiep theo.
     */
    private final class LuoiTuan extends JComponent implements Scrollable {
        private static final int NHAN_GIO = 90;
        private static final int RONG_MAC_DINH = 900;
        private static final int CAO_TIEU_DE = 62;
        private static final int LE = 7;
        private static final int CACH_THE = 5;
        private static final int CAO_NHOM_TOI_THIEU = 66;
        private static final int CAO_TRONG = 660;

        private final Font fontTen = new Font(Font.SANS_SERIF, Font.BOLD, 11);
        private final Font fontKhoa = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
        private final Font fontGio = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        private final List<OChon> cacO = new ArrayList<>();
        private record OChon(Rectangle bounds, LichHocLop lich) { }
        private record Khung(LichHocLop lich, int batDau, int ketThuc, int thu) { }
        private final class NhomGio {
            int batDau;
            int ketThuc;
            final List<List<Khung>> theoNgay = new ArrayList<>();
            final boolean lichCu;
            NhomGio(int batDau, int ketThuc) { this(batDau, ketThuc, false); }
            NhomGio(int batDau, int ketThuc, boolean lichCu) {
                this.lichCu = lichCu;
                this.batDau = batDau;
                this.ketThuc = ketThuc;
                for (int i = 0; i < 7; i++) theoNgay.add(new ArrayList<>());
            }
        }

        LuoiTuan() {
            setOpaque(true);
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(RONG_MAC_DINH, CAO_TIEU_DE + CAO_TRONG));
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    // Thay doi rong cot => so dong ten lop thay doi => tinh lai cao o.
                    capNhatKichThuoc();
                }
            });
            setToolTipText(" ");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    for (int i = cacO.size() - 1; i >= 0; i--) {
                        OChon o = cacO.get(i);
                        if (o.bounds.contains(e.getPoint())) {
                            khiChonLich.accept(o.lich);
                            return;
                        }
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    boolean trenO = cacO.stream().anyMatch(o -> o.bounds.contains(e.getPoint()));
                    setCursor(trenO ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            });
        }

        @Override public String getToolTipText(MouseEvent event) {
            for (int i = cacO.size() - 1; i >= 0; i--) {
                OChon o = cacO.get(i);
                if (o.bounds.contains(event.getPoint())) {
                    LichHocLop lh = o.lich;
                    return "<html><b>" + html(lh.tenLop()) + "</b><br>" +
                            html(lh.tenKhoaHoc()) + "<br>Giáo viên: " +
                            html(giaoVienTheoLop.getOrDefault(lh.maLop(), "Chưa phân công"))
                            + "<br>Phòng: " +
                            html(lh.tenPhong() == null ? "Chưa gán" : lh.tenPhong()) + "<br>" +
                            lh.gioBatDau().format(GIO) + " - " + lh.gioKetThuc().format(GIO) +
                            "<br>Từ " + lh.tuNgay() +
                            (lh.denNgay() == null ? "" : " đến " + lh.denNgay()) + "</html>";
                }
            }
            return null;
        }
        private String html(String x) {
            return x == null ? "" : x.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;");
        }
        private int phut(LocalTime time) {
            return time.getHour() * 60 + time.getMinute();
        }
        private String gioPhut(int phut) {
            return String.format("%02d:%02d", phut / 60, phut % 60);
        }
        private Color mau(LichHocLop lh) {
            return MAU_LOP[Math.floorMod(lh.maLop(), MAU_LOP.length)];
        }
        private Color vien(LichHocLop lh) {
            return NET_LOP[Math.floorMod(lh.maLop(), NET_LOP.length)];
        }

        /** Dung dung 8 ca theo quy dinh chung; lich cu ngoai ca van hien thi gio goc. */
        private List<NhomGio> nhomLich() {
            List<Khung> buoi = new ArrayList<>();
            for (LichHocLop lh : danhSach) {
                if (!"HIEU_LUC".equals(lh.trangThai()) || lh.gioBatDau() == null ||
                        lh.gioKetThuc() == null || lh.tuNgay() == null ||
                        lh.thuTrongTuan() < 2 || lh.thuTrongTuan() > 8) continue;
                LocalDate ngay = dauTuan.plusDays(lh.thuTrongTuan() - 2L);
                if (ngay.isBefore(lh.tuNgay()) ||
                        (lh.denNgay() != null && ngay.isAfter(lh.denNgay()))
                        || biHuy(lh, ngay)) continue;
                int batDau = phut(lh.gioBatDau());
                int ketThuc = phut(lh.gioKetThuc());
                if (ketThuc <= batDau) continue;
                buoi.add(new Khung(lh, batDau, ketThuc, lh.thuTrongTuan() - 2));
            }
            for (BuoiDieuChinh b : buoiDieuChinh) {
                if (b.ngayHoc().isBefore(dauTuan)
                        || !b.ngayHoc().isBefore(dauTuan.plusDays(7))
                        || b.soCa() < 1 || b.soCa() > 8) continue;
                LopTrungTam lop = lopTheoMa.get(b.maLop());
                if (lop == null) continue;
                CaHocTrungTam.Ca ca = CaHocTrungTam.layTheoSoCa(b.soCa());
                String loai = "HUY".equals(b.trangThai()) ? "HUY_BUOI" : "BUOI_BU";
                LichHocLop motBuoi = new LichHocLop(-b.maBuoiHoc(), b.maLop(),
                        lop.maKhoaHoc(), lop.maMonHoc(), lop.tenMonHoc(),
                        lop.tenKhoaHoc(), lop.tenLop(),
                        b.ngayHoc().getDayOfWeek().getValue() + 1,
                        ca.batDau(), ca.ketThuc(), b.ngayHoc(), b.ngayHoc(),
                        loai, b.maPhong(), b.tenPhong());
                buoi.add(new Khung(motBuoi, phut(ca.batDau()),
                        phut(ca.ketThuc()), b.ngayHoc().getDayOfWeek().getValue() - 1));
            }
            buoi.sort(Comparator.comparingInt(Khung::batDau)
                    .thenComparingInt(Khung::ketThuc)
                    .thenComparingInt(k -> k.lich().maLop()));
            List<NhomGio> nhom = new ArrayList<>();
            for (CaHocTrungTam.Ca ca : CaHocTrungTam.DANH_SACH)
                nhom.add(new NhomGio(phut(ca.batDau()), phut(ca.ketThuc())));
            NhomGio lichCu = new NhomGio(0, 0, true);
            boolean coLichCu = false;
            for (Khung b : buoi) {
                int slot = -1;
                for (int j = 0; j < CaHocTrungTam.DANH_SACH.size(); j++) {
                    CaHocTrungTam.Ca ca = CaHocTrungTam.DANH_SACH.get(j);
                    if (phut(ca.batDau()) == b.batDau()
                            && phut(ca.ketThuc()) == b.ketThuc()) { slot = j; break; }
                }
                if (slot < 0) {
                    // Khong gan lich cu ngoai 8 ca vao hang ca moi sai gio.
                    lichCu.theoNgay.get(b.thu()).add(b);
                    coLichCu = true;
                } else nhom.get(slot).theoNgay.get(b.thu()).add(b);
            }
            if (coLichCu) nhom.add(lichCu);
            return nhom;
        }

        /** Xuong dong ten lop va khoa hoc, khong cat chu theo chieu ngang. */
        private List<String> xuongDong(String text, FontMetrics fm, int rong) {
            String s = text == null || text.isBlank() ? "(Chưa đặt tên)" : text.strip();
            List<String> result = new ArrayList<>();
            StringBuilder dong = new StringBuilder();
            for (String word : s.split("\\s+")) {
                if (word.isBlank()) continue;
                String trial = dong.isEmpty() ? word : dong + " " + word;
                if (fm.stringWidth(trial) <= rong) {
                    dong.setLength(0);
                    dong.append(trial);
                    continue;
                }
                if (!dong.isEmpty()) {
                    result.add(dong.toString());
                    dong.setLength(0);
                }
                StringBuilder piece = new StringBuilder();
                for (int cp : word.codePoints().toArray()) {
                    String ch = new String(Character.toChars(cp));
                    if (!piece.isEmpty() && fm.stringWidth(piece.toString() + ch) > rong) {
                        result.add(piece.toString());
                        piece.setLength(0);
                    }
                    piece.append(ch);
                }
                dong.append(piece);
            }
            if (!dong.isEmpty()) result.add(dong.toString());
            if (result.isEmpty()) result.add("(Chưa đặt tên)");
            return result;
        }

        private int rongHienTai() {
            int w = getWidth();
            if (w <= NHAN_GIO + 7 && getParent() instanceof JViewport vp)
                w = vp.getExtentSize().width;
            return w > NHAN_GIO + 7 ? w : RONG_MAC_DINH;
        }

        private int xNgay(int d) {
            return NHAN_GIO + (rongHienTai() - NHAN_GIO) * d / 7;
        }

        private int rongNgay(int d) {
            return xNgay(d + 1) - xNgay(d);
        }

        private int caoThe(Khung b, int rongNgay) {
            int rong = Math.max(22, rongNgay - 2 * LE - 15);
            FontMetrics ten = getFontMetrics(fontTen);
            FontMetrics khoa = getFontMetrics(fontKhoa);
            int dongTen = xuongDong(tenThe(b.lich()), ten, rong).size();
            int dongKhoa = xuongDong(b.lich().tenKhoaHoc(), khoa, rong).size();
            String gio = b.lich().gioBatDau().format(GIO) + " – " + b.lich().gioKetThuc().format(GIO);
            boolean tachGio = getFontMetrics(fontGio).stringWidth(gio) > rong;
            String tenPhong = "Phòng: " + (b.lich().tenPhong() == null
                    ? "Chưa gán phòng" : b.lich().tenPhong());
            int dongPhong = xuongDong(tenPhong, khoa, rong).size();
            String giaoVien = "GV: " + giaoVienTheoLop.getOrDefault(
                    b.lich().maLop(), "Chưa phân công");
            int dongGiaoVien = xuongDong(giaoVien, khoa, rong).size();
            return 11 + dongTen * 15 + 2 + dongKhoa * 13 + 3
                    + (tachGio ? 28 : 15) + 5 + dongGiaoVien * 13
                    + dongPhong * 13 + 7;
        }

        private int caoNhom(NhomGio n) {
            int cao = CAO_NHOM_TOI_THIEU;
            for (int d = 0; d < 7; d++) {
                int sum = 2 * LE;
                for (Khung b : n.theoNgay.get(d))
                    sum += caoThe(b, rongNgay(d)) + CACH_THE;
                cao = Math.max(cao, sum);
            }
            return cao;
        }

        void capNhatKichThuoc() {
            int w = rongHienTai();
            // Kich thuoc cot phu thuoc chieu ngang that cua viewport,
            // khong dung moi ngay co dinh 210px nua.
            int h = CAO_TIEU_DE + 10;
            for (NhomGio n : nhomLich()) h += caoNhom(n);
            h = Math.max(h, CAO_TIEU_DE + CAO_TRONG);
            Dimension cu = getPreferredSize();
            if (cu.width != w || cu.height != h) {
                setPreferredSize(new Dimension(w, h));
                revalidate();
            }
            repaint();
        }

        @Override public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(RONG_MAC_DINH, CAO_TIEU_DE + CAO_TRONG);
        }
        @Override public int getScrollableUnitIncrement(Rectangle r, int huong, int chieu) {
            return 32;
        }
        @Override public int getScrollableBlockIncrement(Rectangle r, int huong, int chieu) {
            return Math.max(90, r.height - 60);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                cacO.clear();
                int tongRong = getWidth();
                g.setColor(new Color(0xE5F4EA));
                g.fillRect(0, 0, tongRong, CAO_TIEU_DE);
                g.setColor(new Color(0x174D35));
                g.setFont(getFont().deriveFont(Font.BOLD, 12f));
                g.drawString("Ca học", 17, 31);
                String[] thu = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
                for (int d = 0; d < 7; d++) {
                    int x = xNgay(d);
                    int rong = rongNgay(d);
                    LocalDate date = dauTuan.plusDays(d);
                    if (date.equals(LocalDate.now())) {
                        g.setColor(new Color(0xCDECD6));
                        g.fillRect(x, 0, rong, CAO_TIEU_DE);
                    }
                    g.setColor(new Color(0x184E37));
                    g.drawString(thu[d], x + 12, 26);
                    g.setColor(new Color(0x496758));
                    g.drawString(date.format(NGAY), x + 12, 45);
                }
                g.setColor(new Color(0xCFDDD4));
                g.drawLine(0, CAO_TIEU_DE - 1, tongRong, CAO_TIEU_DE - 1);

                List<NhomGio> nhom = nhomLich();
                if (nhom.isEmpty()) {
                    g.setFont(getFont().deriveFont(13f));
                    g.setColor(GRAY);
                    g.drawString("Tuần này chưa có lịch học theo bộ lọc đang chọn.", NHAN_GIO + 18, CAO_TIEU_DE + 45);
                }
                int y = CAO_TIEU_DE;
                for (int i = 0; i < nhom.size(); i++) {
                    NhomGio n = nhom.get(i);
                    int cao = caoNhom(n);
                    g.setColor(i % 2 == 0 ? Color.WHITE : new Color(0xF8FAF9));
                    g.fillRect(0, y, tongRong, cao);
                    g.setColor(new Color(0xF0F5F1));
                    for (int d = 0; d < 7; d++) {
                        if (dauTuan.plusDays(d).equals(LocalDate.now()))
                            g.fillRect(xNgay(d), y, rongNgay(d), cao);
                    }
                    g.setColor(new Color(0x304E42));
                    g.setFont(getFont().deriveFont(Font.BOLD, 11f));
                    if (n.lichCu) {
                        g.drawString("Lịch cũ", 10, y + 20);
                        g.drawString("Chưa đổi ca", 5, y + 39);
                    } else {
                        g.drawString("Ca " + (i + 1), 13, y + 18);
                        g.drawString(gioPhut(n.batDau), 13, y + 35);
                        g.drawString("– " + gioPhut(n.ketThuc), 13, y + 51);
                    }
                    for (int d = 0; d < 7; d++) {
                        int x = xNgay(d);
                        int w = rongNgay(d);
                        int yThe = y + LE;
                        for (Khung b : n.theoNgay.get(d)) {
                            Rectangle rect = new Rectangle(x + LE, yThe,
                                    Math.max(1, w - 2 * LE), caoThe(b, w));
                            veThe(g, rect, b);
                            if (b.lich().maLichHoc() > 0)
                                cacO.add(new OChon(rect, b.lich()));
                            yThe += rect.height + CACH_THE;
                        }
                    }
                    g.setColor(new Color(0xD9E3DD));
                    g.drawLine(0, y + cao - 1, tongRong, y + cao - 1);
                    y += cao;
                }
                g.setColor(new Color(0xD9E3DD));
                for (int d = 0; d <= 7; d++) {
                    int x = xNgay(d);
                    g.drawLine(x, 0, x, getHeight());
                }
            } finally {
                g.dispose();
            }
        }

        private void veThe(Graphics2D g, Rectangle rect, Khung khung) {
            LichHocLop lh = khung.lich();
            g.setColor("HUY_BUOI".equals(lh.trangThai())
                    ? new Color(0xF7E1DF) : "BUOI_BU".equals(lh.trangThai())
                    ? new Color(0xFFF1C9) : mau(lh));
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 7, 7);
            g.setColor("HUY_BUOI".equals(lh.trangThai())
                    ? new Color(0xBA4038) : vien(lh));
            g.fillRoundRect(rect.x, rect.y, 4, rect.height, 5, 5);
            int trai = rect.x + 9;
            int y = rect.y + 15;
            int rong = rect.width - 18;
            g.setColor(new Color(0x1E3643));
            g.setFont(fontTen);
            for (String line : xuongDong(tenThe(lh), g.getFontMetrics(), rong)) {
                g.drawString(line, trai, y);
                y += 15;
            }
            y += 2;
            g.setFont(fontKhoa);
            g.setColor(new Color(0x375261));
            for (String line : xuongDong(lh.tenKhoaHoc(), g.getFontMetrics(), rong)) {
                g.drawString(line, trai, y);
                y += 13;
            }
            y += 3;
            g.setFont(fontGio);
            g.setColor(new Color(0x21546A));
            String gio = lh.gioBatDau().format(GIO) + " – " + lh.gioKetThuc().format(GIO);
            if (g.getFontMetrics().stringWidth(gio) <= rong) {
                g.drawString(gio, trai, y + 6);
                y += 15;
            } else {
                // Tranh de chu gio tran ra ngoai cot ngay tren man hinh hep.
                g.drawString(lh.gioBatDau().format(GIO), trai, y + 6);
                g.drawString(lh.gioKetThuc().format(GIO), trai, y + 19);
                y += 28;
            }
            g.setFont(fontKhoa);
            g.setColor(new Color(0x285845));
            String giaoVien = "GV: " + giaoVienTheoLop.getOrDefault(
                    lh.maLop(), "Chưa phân công");
            for (String dong : xuongDong(giaoVien, g.getFontMetrics(), rong)) {
                g.drawString(dong, trai, y + 6);
                y += 13;
            }
            String phong = "Phòng: " + (lh.tenPhong() == null ? "Chưa gán phòng" : lh.tenPhong());
            for (String dong : xuongDong(phong, g.getFontMetrics(), rong)) {
                g.drawString(dong, trai, y + 6);
                y += 13;
            }
        }

        private String tenThe(LichHocLop lich) {
            return ("HUY_BUOI".equals(lich.trangThai()) ? "ĐÃ HỦY · "
                    : "BUOI_BU".equals(lich.trangThai()) ? "BUỔI BÙ · " : "")
                    + lich.tenLop();
        }
    }
}
