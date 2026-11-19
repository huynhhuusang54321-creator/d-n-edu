package vn.vinabacus.view;

import java.awt.BorderLayout;
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

    public ThoiKhoaBieuTuanPanel(LocalDate ngay, Consumer<LocalDate> khiDoiTuan) {
        super(new BorderLayout(5, 6));
        this.khiDoiTuan = khiDoiTuan == null ? x -> { } : khiDoiTuan;
        dauTuan = veDauTuan(ngay == null ? LocalDate.now() : ngay);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDCE4E5)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JLabel title = new JLabel("THỜI KHÓA BIỂU THEO TUẦN");
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
            NgayPicker chon = new NgayPicker(dauTuan, "Chọn ngày trong tuần");
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
        JLabel note = new JLabel("Giờ thực tế giữ nguyên · Lớp cùng khung giờ xếp dọc · Chưa kiểm tra trùng phòng học.");
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
                    (x.denNgay() == null || !ngay.isAfter(x.denNgay()))) {
                buoi++;
                lop.add(x.maLop());
            }
        }
        lblThongKe.setText("Tuần này: " + buoi + " buổi học · " +
                lop.size() + " lớp · Bấm vào ô lịch để chọn dòng tương ứng");
        luoi.capNhatKichThuoc();
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
            NhomGio(int batDau, int ketThuc) {
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
                            html(lh.tenKhoaHoc()) + "<br>" +
                            html(thongTin(lh).get(0)) + "<br>" +
                            html(thongTin(lh).get(1)) + "<br>" +
                            html(thongTin(lh).get(2)) + "<br>" +
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
        private String hienThi(String value, String macDinh) {
            return value == null || value.isBlank() ? macDinh : value;
        }
        private List<String> thongTin(LichHocLop lh) {
            return List.of(
                    "Phòng: " + hienThi(lh.tenPhong(), "Chưa xếp phòng"),
                    "Cơ sở: " + hienThi(lh.tenCoSo(), "Trung tâm"),
                    "GV: " + hienThi(lh.tenGiaoVien(), "Chưa phân công"));
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

        /** Muoi khung gio co dinh 07:00-22:00, moi khung 90 phut.
         * Gio hoc thuc te duoc hien thi nguyen ven tren the, khong bi sua theo khung.
         * Buoi co gio bat dau trong khung nao thi xep vao khung do.
         */
        private List<NhomGio> nhomLich() {
            List<Khung> buoi = new ArrayList<>();
            for (LichHocLop lh : danhSach) {
                if (!"HIEU_LUC".equals(lh.trangThai()) || lh.gioBatDau() == null ||
                        lh.gioKetThuc() == null || lh.tuNgay() == null ||
                        lh.thuTrongTuan() < 2 || lh.thuTrongTuan() > 8) continue;
                LocalDate ngay = dauTuan.plusDays(lh.thuTrongTuan() - 2L);
                if (ngay.isBefore(lh.tuNgay()) ||
                        (lh.denNgay() != null && ngay.isAfter(lh.denNgay()))) continue;
                int batDau = phut(lh.gioBatDau());
                int ketThuc = phut(lh.gioKetThuc());
                if (ketThuc <= batDau) continue;
                buoi.add(new Khung(lh, batDau, ketThuc, lh.thuTrongTuan() - 2));
            }
            buoi.sort(Comparator.comparingInt(Khung::batDau)
                    .thenComparingInt(Khung::ketThuc)
                    .thenComparingInt(k -> k.lich().maLop()));
            List<NhomGio> nhom = new ArrayList<>();
            final int mo = 7 * 60;
            final int dong = 22 * 60;
            final int buoc = 90;
            for (int t = mo; t < dong; t += buoc)
                nhom.add(new NhomGio(t, t + buoc));
            for (Khung b : buoi) {
                // Khong liet ke ca hoan toan nam ngoai 07:00-22:00.
                if (b.ketThuc() <= mo || b.batDau() >= dong) continue;
                int slot = Math.min(9, Math.max(0, (b.batDau() - mo) / buoc));
                nhom.get(slot).theoNgay.get(b.thu()).add(b);
            }
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
            int dongTen = xuongDong(b.lich().tenLop(), ten, rong).size();
            int dongKhoa = xuongDong(b.lich().tenKhoaHoc(), khoa, rong).size();
            int dongThongTin = thongTin(b.lich()).stream()
                    .mapToInt(x -> xuongDong(x, khoa, rong).size()).sum();
            String gio = b.lich().gioBatDau().format(GIO) + " – " + b.lich().gioKetThuc().format(GIO);
            boolean tachGio = getFontMetrics(fontGio).stringWidth(gio) > rong;
            return 11 + dongTen * 15 + 2 + dongKhoa * 13 + 3
                    + dongThongTin * 13 + 3 + (tachGio ? 28 : 15) + 6;
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
                g.drawString("Giờ học", 17, 31);
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
                    g.drawString(gioPhut(n.batDau), 13, y + 29);
                    g.drawString("– " + gioPhut(n.ketThuc), 13, y + 47);
                    for (int d = 0; d < 7; d++) {
                        int x = xNgay(d);
                        int w = rongNgay(d);
                        int yThe = y + LE;
                        for (Khung b : n.theoNgay.get(d)) {
                            Rectangle rect = new Rectangle(x + LE, yThe,
                                    Math.max(1, w - 2 * LE), caoThe(b, w));
                            veThe(g, rect, b);
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
            g.setColor(mau(lh));
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 7, 7);
            g.setColor(vien(lh));
            g.fillRoundRect(rect.x, rect.y, 4, rect.height, 5, 5);
            int trai = rect.x + 9;
            int y = rect.y + 15;
            int rong = rect.width - 18;
            g.setColor(new Color(0x1E3643));
            g.setFont(fontTen);
            for (String line : xuongDong(lh.tenLop(), g.getFontMetrics(), rong)) {
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
            for (String thongTin : thongTin(lh)) {
                for (String line : xuongDong(thongTin, g.getFontMetrics(), rong)) {
                    g.drawString(line, trai, y);
                    y += 13;
                }
            }
            y += 3;
            g.setFont(fontGio);
            g.setColor(new Color(0x21546A));
            String gio = lh.gioBatDau().format(GIO) + " – " + lh.gioKetThuc().format(GIO);
            if (g.getFontMetrics().stringWidth(gio) <= rong) {
                g.drawString(gio, trai, y + 6);
            } else {
                // Tranh de chu gio tran ra ngoai cot ngay tren man hinh hep.
                g.drawString(lh.gioBatDau().format(GIO), trai, y + 6);
                g.drawString(lh.gioKetThuc().format(GIO), trai, y + 19);
            }
        }
    }
}
