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
import vn.vinabacus.service.LichHocLopService;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;

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
    private final JButton btnXoaLoc = new JButton("Xóa lọc");
    // Chan su kien combo khi dua tat ca bo loc ve mac dinh, chi tai SQL mot lan.
    private boolean dangXoaLoc;
    private final List<TabTheoMon> cacTab = new ArrayList<>();
    private int lanTai;
    private final Map<Integer, Boolean> cheDoTungTab = new LinkedHashMap<>();
    private LocalDate tuanCanXem = LocalDate.now();

    private record DuLieu(List<MonHoc> mon, List<KhoaHoc> khoa,
                          List<LopTrungTam> lop, List<LichHocLop> lich) { }
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
        final DefaultTableModel model = new DefaultTableModel(new String[]{
                "Tên lớp", "Khóa học", "Thứ", "Bắt đầu", "Kết thúc", "Từ ngày", "Đến ngày", "Trạng thái"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        final JTable bang = new JTable(model);
        final List<LichHocLop> theoMon = new ArrayList<>();
        final List<LichHocLop> hienThi = new ArrayList<>();
        final List<KhoaHoc> khoa = new ArrayList<>();
        final List<LopTrungTam> lop = new ArrayList<>();
        final JPanel panel = new JPanel(new BorderLayout(5, 8));
        final JPanel cardPanel = new JPanel(new CardLayout());
        final JPanel luoiThe = new JPanel(new GridLayout(0, 3, 14, 14));
        final ThoiKhoaBieuTuanPanel lichTuan;
        final NutCheDo btnDanhSach;
        final NutCheDo btnThoiKhoaBieu;
        final Consumer<List<LichHocLop>> khiChiTiet;
        final Consumer<List<LichHocLop>> khiSuaCard;
        final Consumer<List<LichHocLop>> khiHuyCard;
        LichHocLop lichDaChon;
        boolean dangNap;
        boolean xemTkb;

        TabTheoMon(int maMon, LocalDate tuan,
                   Consumer<LocalDate> khiChonTuan, Consumer<Boolean> khiDoiCheDo,
                   Consumer<List<LichHocLop>> khiChiTiet,
                   Consumer<List<LichHocLop>> khiSuaCard,
                   Consumer<List<LichHocLop>> khiHuyCard) {
            this.maMon = maMon;
            this.khiChiTiet = khiChiTiet;
            this.khiSuaCard = khiSuaCard;
            this.khiHuyCard = khiHuyCard;
            lichTuan = new ThoiKhoaBieuTuanPanel(tuan, khiChonTuan);
            lichTuan.datXuLyChon(this::chonLichTrongBang);
            btnDanhSach = new NutCheDo(maMon == -1 ? "Danh sách" : "Dạng danh sách", false);
            btnThoiKhoaBieu = new NutCheDo(maMon == -1 ? "Thời khóa biểu" : "Dạng ô", true);

            bang.setRowHeight(29);
            bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            bang.setAutoCreateRowSorter(true);
            bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int[] widths = {195, 165, 85, 88, 88, 112, 112, 105};
            for (int i = 0; i < widths.length; i++)
                bang.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            locKhoa.setPrototypeDisplayValue(new LuaChon(-1, "Khóa học có tên rất dài"));
            locLop.setPrototypeDisplayValue(new LuaChon(-1, "Tên lớp học có tên rất dài"));
            JPanel boLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 3));
            boLoc.add(new JLabel("Khóa học:")); boLoc.add(locKhoa);
            boLoc.add(new JLabel("Lớp học:")); boLoc.add(locLop);
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
            model.setRowCount(0);
            hienThi.clear();
            for (LichHocLop x : theoMon) {
                if (maKhoa > 0 && x.maKhoaHoc() != maKhoa) continue;
                if (maLop > 0 && x.maLop() != maLop) continue;
                hienThi.add(x);
                model.addRow(new Object[]{
                        x.tenLop(), x.tenKhoaHoc(), LichHocLopService.tenThu(x.thuTrongTuan()),
                        hienThiGio(x.gioBatDau()), hienThiGio(x.gioKetThuc()),
                        NgayLichHocPicker.hienThiNgay(x.tuNgay()),
                        NgayLichHocPicker.hienThiNgay(x.denNgay()),
                        "HIEU_LUC".equals(x.trangThai()) ? "Hiệu lực" : "Đã hủy"
                });
            }
            if (lichDaChon != null && hienThi.stream().noneMatch(x ->
                    x.maLichHoc() == lichDaChon.maLichHoc())) lichDaChon = null;
            lichTuan.datDanhSach(hienThi);
            if (maMon != -1) hienThiTheLop();
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
                        + ":</b> " + esc(gio) + ("HUY".equals(lh.trangThai())
                        ? " <font color='#A94442'>(Đã hủy)</font>" : "") + "</html>");
                buoi.setToolTipText("Từ " + NgayLichHocPicker.hienThiNgay(lh.tuNgay())
                        + " đến " + (lh.denNgay() == null ? "không giới hạn"
                        : NgayLichHocPicker.hienThiNgay(lh.denNgay())));
                cacBuoi.add(buoi);
            }
            cacBuoi.setAlignmentX(LEFT_ALIGNMENT);
            thongTin.add(cacBuoi);
            card.add(thongTin, BorderLayout.CENTER);

            JPanel hanhDong = new JPanel(new GridLayout(1, 3, 5, 0));
            hanhDong.setOpaque(false);
            hanhDong.setBorder(BorderFactory.createEmptyBorder(2, 9, 0, 9));
            // Ve mau chu/nen truc tiep: Windows Look & Feel khong the lam mo chu.
            JButton chiTiet = nutHopThoai("Chi tiết", new Color(0xE8EEEB), CHU_DAM);
            JButton sua = nutHopThoai("Sửa", new Color(0x1865AD), Color.WHITE);
            JButton huy = nutHopThoai("Hủy buổi", new Color(0xBD3932), Color.WHITE);
            chiTiet.setToolTipText("Xem đầy đủ các buổi, khoảng ngày áp dụng và trạng thái");
            sua.setToolTipText("Chọn một buổi để chỉnh sửa");
            huy.setToolTipText("Chọn một buổi để hủy (không xóa dữ liệu SQL)");
            for (JButton b : new JButton[]{chiTiet, sua, huy})
                b.setPreferredSize(new Dimension(95, 36));
            chiTiet.addActionListener(e -> khiChiTiet.accept(ds));
            sua.addActionListener(e -> khiSuaCard.accept(ds));
            huy.addActionListener(e -> khiHuyCard.accept(ds));
            hanhDong.add(chiTiet); hanhDong.add(sua); hanhDong.add(huy);
            card.add(hanhDong, BorderLayout.SOUTH);
            card.setPreferredSize(new Dimension(306, 235 + Math.max(0, ds.size() - 2) * 26));
            return card;
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
        JButton btnTim = new JButton("Tìm kiếm");
        JButton btnTaiLai = new JButton("Làm mới");
        JButton btnThem = new JButton("Thêm");
        JButton btnSua = new JButton("Sửa");
        btnTaiLai.setToolTipText("Tải dữ liệu mới từ SQL Server và giữ nguyên bộ lọc đang chọn");
        btnXoaLoc.setToolTipText("Bỏ từ khóa, trạng thái, khóa học và lớp học đang lọc");
        btnXoaLoc.setVisible(false);  // Chi hien khi thuc su dang loc.
        toolbar.add(new JLabel("Môn / khóa / lớp / thứ:"));
        toolbar.add(txtTim);
        toolbar.add(new JLabel("Trạng thái:"));
        toolbar.add(locTrangThai);
        toolbar.add(btnTim); toolbar.add(btnTaiLai);
        toolbar.add(btnXoaLoc); toolbar.add(btnThem); toolbar.add(btnSua);
        JPanel dau = new JPanel(new BorderLayout(4, 8));
        dau.add(title, BorderLayout.NORTH);
        dau.add(toolbar, BorderLayout.CENTER);
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
                }
            } finally {
                dangXoaLoc = false;
            }
            capNhatNutXoaLoc();
            taiDuLieu();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnSua.addActionListener(e -> {
            LichHocLop chon = lichDuocChon();
            if (chon != null) moForm(chon);
        });
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
                || (tab != null && (tab.maKhoaDangLoc() > 0 || tab.maLopDangLoc() > 0));
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
        final int luot = ++lanTai;
        final String tim = txtTim.getText();
        final String trangThai = trangThaiLoc();
        thongBao.setText("Đang tải lịch học từ SQL Server...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.layMonHoc(), controller.layKhoaHoc(),
                        controller.layLop(), controller.timKiem(tim, trangThai));
            }
            @Override protected void done() {
                if (luot != lanTai) return;
                try {
                    DuLieu dl = get();
                    // Chi phuc hoi tab va bo loc truoc khi tai lai;
                    // khong tu dong nhay vao lop vua Them/Sua.
                    taoTabs(dl, maMonTruoc, maKhoaTruoc, maLopTruoc);
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
    private void taoTabs(DuLieu dl, int monMuonChon, int khoaMuonLoc, int lopMuonLoc) {
        cacTab.clear();
        tabMonHoc.removeAll();

        // -1 la tab Tong; khong gan voi mot mon hoc cu the.
        TabTheoMon tong = taoTab(-1);
        for (KhoaHoc k : dl.khoa()) {
            tong.khoa.add(k);
            tong.locKhoa.addItem(new LuaChon(k.maKhoaHoc(), k.tenKhoaHoc()));
        }
        tong.lop.addAll(dl.lop());
        tong.theoMon.addAll(dl.lich());
        hoanTatTab(tong, monMuonChon == -1 ? khoaMuonLoc : -1,
                monMuonChon == -1 ? lopMuonLoc : -1);
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
            for (LichHocLop lh : dl.lich()) {
                if (lh.maMonHoc() == mon.maMonHoc()) tab.theoMon.add(lh);
            }
            hoanTatTab(tab, mon.maMonHoc() == monMuonChon ? khoaMuonLoc : -1,
                    mon.maMonHoc() == monMuonChon ? lopMuonLoc : -1);
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
                this::xemChiTietThe, this::suaBuoiTrongThe, this::huyBuoiTrongThe);
        tab.datCheDo(true);
        tab.dangNap = true;
        tab.locKhoa.addItem(new LuaChon(-1, "Tất cả khóa học"));
        tab.dangNap = false;
        return tab;
    }

    private void hoanTatTab(TabTheoMon tab, int khoaMuonLoc, int lopMuonLoc) {
        tab.dangNap = true;
        chon(tab.locKhoa, khoaMuonLoc);
        tab.dangNap = false;
        tab.napLocLop(lopMuonLoc);
        tab.locKhoa.addActionListener(e -> {
            if (!tab.dangNap) { tab.napLocLop(-1); capNhatThongBao(); capNhatNutXoaLoc(); }
        });
        tab.locLop.addActionListener(e -> {
            if (!tab.dangNap) { tab.hienThiTheoBoLoc(); capNhatThongBao(); capNhatNutXoaLoc(); }
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

    private LichHocLop lichDuocChon() {
        TabTheoMon tab = tabDangChon();
        if (tab == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một môn học");
            return null;
        }
        if (tab.xemTkb) {
            if (tab.lichDaChon != null) return tab.lichDaChon;
            if (tab.maMon != -1)
                return chonMotBuoi(tab.hienThi, "Chọn buổi học cần sửa");
            JOptionPane.showMessageDialog(this, "Hãy bấm chọn một ô trên thời khóa biểu trước khi sửa.");
            return null;
        }
        if (tab.bang.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một dòng trong danh sách lịch học.");
            return null;
        }
        return tab.hienThi.get(tab.bang.convertRowIndexToModel(tab.bang.getSelectedRow()));
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

    /** Chon dung mot buoi theo tung dong; khong dung JComboBox chen ep chu. */
    private LichHocLop chonMotBuoi(List<LichHocLop> ds, String tieuDe) {
        if (ds == null || ds.isEmpty()) return null;
        JDialog dialog = cuaSo(tieuDe);
        JPanel goc = new JPanel(new BorderLayout(0, 0));
        goc.setBackground(Color.WHITE);
        LichHocLop dau = ds.get(0);
        goc.add(tieuDeHopThoai(tieuDe, dau.tenLop()), BorderLayout.NORTH);

        JPanel dsBuoi = new JPanel();
        dsBuoi.setLayout(new BoxLayout(dsBuoi, BoxLayout.Y_AXIS));
        dsBuoi.setBackground(Color.WHITE);
        dsBuoi.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        ButtonGroup nhom = new ButtonGroup();
        List<JRadioButton> chon = new ArrayList<>();
        List<LichHocLop> sapXep = new ArrayList<>(ds);
        sapXep.sort(Comparator.comparingInt(LichHocLop::thuTrongTuan)
                .thenComparing(LichHocLop::gioBatDau));
        for (LichHocLop lh : sapXep) {
            JRadioButton rb = new JRadioButton();
            rb.setOpaque(false);
            rb.setFocusPainted(false);
            nhom.add(rb);
            chon.add(rb);
            JPanel noiDung = new JPanel(new GridLayout(2, 1, 0, 2));
            noiDung.setOpaque(false);
            JLabel ten = new JLabel(LichHocLopService.tenThu(lh.thuTrongTuan())
                    + "  |  " + chuoiGio(lh) + ("HUY".equals(lh.trangThai()) ? "  ·  Đã hủy" : ""));
            ten.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            ten.setForeground(CHU_DAM);
            JLabel apDung = new JLabel("Áp dụng: " + ngayHieuLuc(lh));
            apDung.setForeground(new Color(0x60776A));
            apDung.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            noiDung.add(ten);
            noiDung.add(apDung);
            JPanel dong = new JPanel(new BorderLayout(10, 0));
            dong.setBackground(NEN_NHAT);
            dong.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xD1E5D8)),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            dong.add(rb, BorderLayout.WEST);
            dong.add(noiDung, BorderLayout.CENTER);
            dong.setAlignmentX(LEFT_ALIGNMENT);
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
            dsBuoi.add(dong);
            dsBuoi.add(Box.createVerticalStrut(8));
            java.awt.event.MouseAdapter click = new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { rb.setSelected(true); }
            };
            dong.addMouseListener(click);
            noiDung.addMouseListener(click);
            ten.addMouseListener(click);
            apDung.addMouseListener(click);
        }
        chon.get(0).setSelected(true);
        JScrollPane cuon = new JScrollPane(dsBuoi,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.setBorder(BorderFactory.createEmptyBorder());
        cuon.getVerticalScrollBar().setUnitIncrement(20);
        goc.add(cuon, BorderLayout.CENTER);
        JPanel cuoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        cuoi.setBackground(Color.WHITE);
        JButton dong = nutHopThoai("Đóng", new Color(0xE8EEEB), CHU_DAM);
        JButton tiep = nutHopThoai("Chọn buổi", XANH_DAM, Color.WHITE);
        final LichHocLop[] ketQua = {null};
        dong.addActionListener(e -> dialog.dispose());
        tiep.addActionListener(e -> {
            for (int i = 0; i < chon.size(); i++) {
                if (chon.get(i).isSelected()) {
                    ketQua[0] = sapXep.get(i);
                    dialog.dispose();
                    return;
                }
            }
        });
        cuoi.add(dong);
        cuoi.add(tiep);
        goc.add(cuoi, BorderLayout.SOUTH);
        dialog.setContentPane(goc);
        dialog.setSize(590, Math.min(690, 165 + ds.size() * 82));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return ketQua[0];
    }

    private void xemChiTietThe(List<LichHocLop> ds) {
        if (ds.isEmpty()) return;
        LichHocLop dau = ds.get(0);
        JDialog dialog = cuaSo("Chi tiết lớp học");
        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(Color.WHITE);
        goc.add(tieuDeHopThoai("CHI TIẾT LỚP HỌC", "Lịch học định kỳ theo từng buổi"), BorderLayout.NORTH);

        JPanel than = new JPanel(new BorderLayout(0, 14));
        than.setBackground(Color.WHITE);
        than.setBorder(BorderFactory.createEmptyBorder(15, 20, 8, 20));
        JPanel info = new JPanel(new GridLayout(4, 1, 0, 4));
        info.setBackground(XANH_NHAT);
        info.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        info.add(dongThongTin("Lớp học", dau.tenLop()));
        info.add(dongThongTin("Môn học", dau.tenMonHoc()));
        info.add(dongThongTin("Khóa học", dau.tenKhoaHoc()));
        info.add(dongThongTin("Số buổi trong lịch", ds.size() + " buổi"));
        than.add(info, BorderLayout.NORTH);

        JPanel dsBuoi = new JPanel();
        dsBuoi.setBackground(Color.WHITE);
        dsBuoi.setLayout(new BoxLayout(dsBuoi, BoxLayout.Y_AXIS));
        List<LichHocLop> sapXep = new ArrayList<>(ds);
        sapXep.sort(Comparator.comparingInt(LichHocLop::thuTrongTuan)
                .thenComparing(LichHocLop::gioBatDau));
        for (LichHocLop lh : sapXep) {
            JPanel dong = new JPanel(new BorderLayout(0, 6));
            dong.setBackground(NEN_NHAT);
            dong.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xD7E6DD)),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            JLabel thu = new JLabel(LichHocLopService.tenThu(lh.thuTrongTuan())
                    + "    " + chuoiGio(lh));
            thu.setForeground(CHU_DAM);
            thu.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            JLabel status = new JLabel("HIEU_LUC".equals(lh.trangThai()) ? "Hiệu lực" : "Đã hủy");
            status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            status.setForeground("HIEU_LUC".equals(lh.trangThai()) ? XANH_DAM : new Color(0xB94C39));
            JPanel hang = new JPanel(new BorderLayout());
            hang.setOpaque(false);
            hang.add(thu, BorderLayout.WEST);
            hang.add(status, BorderLayout.EAST);
            dong.add(hang, BorderLayout.NORTH);
            JLabel apDung = new JLabel("Thời gian áp dụng: " + ngayHieuLuc(lh));
            apDung.setForeground(new Color(0x547363));
            dong.add(apDung, BorderLayout.SOUTH);
            dong.setAlignmentX(LEFT_ALIGNMENT);
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 79));
            dsBuoi.add(dong);
            dsBuoi.add(Box.createVerticalStrut(8));
        }
        JScrollPane cuon = new JScrollPane(dsBuoi,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.setBorder(BorderFactory.createTitledBorder("Danh sách buổi học"));
        cuon.getVerticalScrollBar().setUnitIncrement(20);
        than.add(cuon, BorderLayout.CENTER);
        goc.add(than, BorderLayout.CENTER);
        JPanel cuoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        cuoi.setBackground(Color.WHITE);
        JButton dong = nutHopThoai("Đóng", XANH_DAM, Color.WHITE);
        dong.addActionListener(e -> dialog.dispose());
        cuoi.add(dong);
        goc.add(cuoi, BorderLayout.SOUTH);
        dialog.setContentPane(goc);
        dialog.setSize(710, Math.min(790, 360 + ds.size() * 78));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void suaBuoiTrongThe(List<LichHocLop> ds) {
        LichHocLop x = chonMotBuoi(ds, "Chọn buổi cần sửa");
        if (x != null) moForm(x);
    }

    /** Huy mem mot buoi theo trang thai co san, khong xoa ban ghi SQL. */
    private void huyBuoiTrongThe(List<LichHocLop> ds) {
        List<LichHocLop> dangHoc = ds.stream()
                .filter(x -> "HIEU_LUC".equals(x.trangThai())).toList();
        if (dangHoc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lớp này không còn buổi học đang hiệu lực.");
            return;
        }
        LichHocLop x = chonMotBuoi(dangHoc, "Chọn buổi cần hủy");
        if (x == null) return;
        if (!xacNhanHuy(x)) return;
        thongBao.setText("Đang hủy buổi học...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.sua(x.maLichHoc(), x.thuTrongTuan(), x.gioBatDau(),
                        x.gioKetThuc(), x.tuNgay(), x.denNgay(), "HUY");
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(LichHocLopPanel.this,
                            "Đã chuyển buổi học sang trạng thái Đã hủy.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    /** Xac nhan huy: khong bao gio xoa dong du lieu; co nut Quay lai ro rang. */
    private boolean xacNhanHuy(LichHocLop lich) {
        JDialog dialog = cuaSo("Xác nhận hủy buổi học");
        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(Color.WHITE);
        goc.add(tieuDeHopThoai("HỦY BUỔI HỌC", "Vui lòng kiểm tra đúng buổi trước khi xác nhận"), BorderLayout.NORTH);
        JPanel than = new JPanel(new GridLayout(5, 1, 0, 9));
        than.setBackground(Color.WHITE);
        than.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        JLabel canhBao = new JLabel("Bạn chắc chắn muốn hủy buổi học này?");
        canhBao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        canhBao.setForeground(new Color(0xB13B32));
        than.add(canhBao);
        than.add(dongThongTin("Lớp", lich.tenLop()));
        than.add(dongThongTin("Buổi", LichHocLopService.tenThu(lich.thuTrongTuan()) + "   " + chuoiGio(lich)));
        than.add(dongThongTin("Áp dụng", ngayHieuLuc(lich)));
        JLabel note = new JLabel("Buổi chuyển thành Đã hủy; bản ghi vẫn được giữ trong SQL Server.");
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

    /** Cua so form duoc lam moi, giu nguyen truong va rang buoc validate cu. */
    private boolean hienFormLich(JPanel form, boolean laThem) {
        JDialog dialog = cuaSo(laThem ? "Thêm lịch học" : "Sửa buổi học");
        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(Color.WHITE);
        goc.add(tieuDeHopThoai(laThem ? "THÊM LỊCH HỌC" : "SỬA BUỔI HỌC",
                laThem ? "Chọn lớp và các buổi học trong tuần"
                        : "Điều chỉnh ngày áp dụng, khung giờ và trạng thái của buổi đã chọn"), BorderLayout.NORTH);
        form.setBackground(Color.WHITE);
        JScrollPane cuon = new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.setBorder(BorderFactory.createEmptyBorder());
        cuon.getVerticalScrollBar().setUnitIncrement(18);
        goc.add(cuon, BorderLayout.CENTER);
        JPanel cuoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        cuoi.setBackground(Color.WHITE);
        JButton dong = nutHopThoai("Hủy", new Color(0xE8EEEB), CHU_DAM);
        JButton luu = nutHopThoai("Lưu thay đổi", XANH_DAM, Color.WHITE);
        final boolean[] ketQua = {false};
        dong.addActionListener(e -> dialog.dispose());
        luu.addActionListener(e -> { ketQua[0] = true; dialog.dispose(); });
        cuoi.add(dong); cuoi.add(luu);
        goc.add(cuoi, BorderLayout.SOUTH);
        dialog.setContentPane(goc);
        dialog.setSize(760, laThem ? 730 : 590);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return ketQua[0];
    }

    private void moForm(LichHocLop cu) {
        TabTheoMon tab = tabDangChon();
        if (tab == null) {
            JOptionPane.showMessageDialog(this, "Chưa có môn học Trung tâm"); return;
        }
        // Khong lay danh sach tu ID hardcode. Du lieu loc va form theo dung tab.
        JComboBox<LuaChon> cboKhoa = new JComboBox<>();
        for (KhoaHoc k : tab.khoa) {
            if (k.trangThai() || (cu != null && cu.maKhoaHoc() == k.maKhoaHoc()))
                cboKhoa.addItem(new LuaChon(k.maKhoaHoc(), k.tenKhoaHoc()));
        }
        if (cboKhoa.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Môn học này chưa có khóa học đang hoạt động.\n"
                    + "Hãy vào Quản lý Khóa học tạo hoặc bật khóa trước khi thêm lịch.");
            return;
        }
        if (cu == null && tab.lop.stream().noneMatch(l ->
                !"DA_DONG".equals(l.trangThai()) && tab.khoa.stream().anyMatch(k ->
                k.maKhoaHoc() == l.maKhoaHoc() && k.trangThai()))) {
            JOptionPane.showMessageDialog(this,
                    "Môn này chưa có lớp đang sử dụng thuộc khóa học hoạt động.\n"
                    + "Hãy vào Quản lý Lớp Trung tâm tạo lớp trước khi thêm lịch.");
            return;
        }
        int maKhoaMacDinh = cu == null ? tab.maKhoaDangLoc() : cu.maKhoaHoc();
        chon(cboKhoa, maKhoaMacDinh);
        JComboBox<LuaChon> cboLop = new JComboBox<>();
        Runnable napLop = () -> {
            LuaChon khoa = (LuaChon) cboKhoa.getSelectedItem();
            int maKhoa = khoa == null ? -1 : khoa.ma();
            cboLop.removeAllItems();
            for (LopTrungTam l : tab.lop) {
                if (l.maKhoaHoc() != maKhoa) continue;
                // Lop dong chi co the hien thi de sua lich cu.
                if (!"DA_DONG".equals(l.trangThai()) ||
                        (cu != null && cu.maLop() == l.maLop()))
                    cboLop.addItem(new LuaChon(l.maLop(), l.tenLop()));
            }
        };
        napLop.run();
        // Neu khoa dang loc chua co lop, tu chon khoa khac co lop de form Them khong bi rong.
        if (cu == null && cboLop.getItemCount() == 0) {
            for (int i = 0; i < cboKhoa.getItemCount(); i++) {
                cboKhoa.setSelectedIndex(i);
                napLop.run();
                if (cboLop.getItemCount() > 0) break;
            }
        }
        if (cu == null) chon(cboLop, tab.maLopDangLoc());
        else chon(cboLop, cu.maLop());
        cboKhoa.addActionListener(e -> napLop.run());
        if (cu != null) {
            cboKhoa.setEnabled(false);
            cboLop.setEnabled(false);
        }
        // Khi Them: chọn nhiều thứ, mỗi thứ có giờ riêng. Khi Sửa: chỉ sửa một thứ.
        JComboBox<String> cboThu = null;
        JTextField txtBatDau = null;
        JTextField txtKetThuc = null;
        JCheckBox[] chonThu = new JCheckBox[7];
        JTextField[] gioDauTheoThu = new JTextField[7];
        JTextField[] gioCuoiTheoThu = new JTextField[7];
        JScrollPane bangBuoi = null;
        if (cu == null) {
            JPanel cacNgay = new JPanel(new GridLayout(0, 1, 3, 3));
            for (int i = 0; i < 7; i++) {
                int thu = i + 2;
                JCheckBox ck = new JCheckBox(LichHocLopService.tenThu(thu));
                chonThu[i] = ck;
                JTextField bd = new JTextField("08:00", 5);
                JTextField kt = new JTextField("09:30", 5);
                gioDauTheoThu[i] = bd;
                gioCuoiTheoThu[i] = kt;
                JPanel dong = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                dong.add(ck);
                dong.add(new JLabel("Từ")); dong.add(bd);
                dong.add(new JLabel("đến")); dong.add(kt);
                cacNgay.add(dong);
            }
            bangBuoi = new JScrollPane(cacNgay);
            bangBuoi.setPreferredSize(new Dimension(340, 220));
        } else {
            cboThu = new JComboBox<>(new String[]{
                    "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"
            });
            cboThu.setSelectedIndex(cu.thuTrongTuan() - 2);
            txtBatDau = new JTextField(hienThiGio(cu.gioBatDau()), 10);
            txtKetThuc = new JTextField(hienThiGio(cu.gioKetThuc()), 10);
        }
        NgayLichHocPicker tuNgay = new NgayLichHocPicker(cu == null ? LocalDate.now() : cu.tuNgay());
        NgayLichHocPicker denNgay = new NgayLichHocPicker(cu == null ? null : cu.denNgay());
        JComboBox<String> cboTrangThai = new JComboBox<>(new String[]{"Hiệu lực", "Đã hủy"});
        if (cu != null && "HUY".equals(cu.trangThai())) cboTrangThai.setSelectedIndex(1);

        // GridBagLayout giu combo box cao 30px, khong keo gian theo bang 7 thu.
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        cboKhoa.setPreferredSize(new Dimension(320, 30));
        cboLop.setPreferredSize(new Dimension(320, 30));
        cboTrangThai.setPreferredSize(new Dimension(320, 30));
        int hang = 0;
        themDongForm(form, hang++, "Khóa học (*)", cboKhoa);
        themDongForm(form, hang++, "Lớp học (*)", cboLop);
        if (cu == null) {
            bangBuoi.setPreferredSize(new Dimension(405, 218));
            themDongForm(form, hang++, "Chọn các thứ học (có thể nhiều thứ)", bangBuoi);
        } else {
            cboThu.setPreferredSize(new Dimension(320, 30));
            themDongForm(form, hang++, "Thứ trong tuần (*)", cboThu);
            themDongForm(form, hang++, "Giờ bắt đầu (HH:mm)", txtBatDau);
            themDongForm(form, hang++, "Giờ kết thúc (HH:mm)", txtKetThuc);
        }
        themDongForm(form, hang++, "Từ ngày (*)", tuNgay);
        themDongForm(form, hang++, "Đến ngày (có thể bỏ trống)", denNgay);
        themDongForm(form, hang++, "Trạng thái", cboTrangThai);

        while (true) {
            if (!hienFormLich(form, cu == null)) return;
            try {
                LuaChon lop = (LuaChon) cboLop.getSelectedItem();
                if (lop == null) throw new IllegalArgumentException("Hãy chọn lớp học còn hoạt động");
                String trangThai = cboTrangThai.getSelectedIndex() == 0 ? "HIEU_LUC" : "HUY";
                LuaChon khoa = (LuaChon) cboKhoa.getSelectedItem();
                if (cu == null) {
                    List<BuoiTrongTuan> ds = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        if (!chonThu[i].isSelected()) continue;
                        LocalTime bd = docGio(gioDauTheoThu[i].getText(),
                                "Giờ bắt đầu " + LichHocLopService.tenThu(i + 2));
                        LocalTime kt = docGio(gioCuoiTheoThu[i].getText(),
                                "Giờ kết thúc " + LichHocLopService.tenThu(i + 2));
                        LichHocLopService.kiemTra(i + 2, bd, kt,
                                tuNgay.layNgay(), denNgay.layNgay(), trangThai);
                        ds.add(new BuoiTrongTuan(i + 2, bd, kt));
                    }
                    if (ds.isEmpty()) throw new IllegalArgumentException(
                            "Hãy đánh dấu ít nhất một thứ học (Thứ 2 đến Chủ nhật)");
                    luuNhieuBuoi(lop.ma(), ds,
                            tuNgay.layNgay(), denNgay.layNgay(), trangThai);
                } else {
                    int thu = cboThu.getSelectedIndex() + 2;
                    LocalTime gioDau = docGio(txtBatDau.getText(), "Giờ bắt đầu");
                    LocalTime gioCuoi = docGio(txtKetThuc.getText(), "Giờ kết thúc");
                    LichHocLopService.kiemTra(thu, gioDau, gioCuoi,
                            tuNgay.layNgay(), denNgay.layNgay(), trangThai);
                    luuDuLieu(cu, lop.ma(), thu, gioDau, gioCuoi,
                            tuNgay.layNgay(), denNgay.layNgay(), trangThai);
                }
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // Canh hang nhan va o nhap, khong de cac o cao bang khung chon thu.
    private static void themDongForm(JPanel form, int hang, String nhan, JComponent oNhap) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.gridy = hang;
        g.gridx = 0;
        g.weightx = 0;
        g.weighty = 0;
        g.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(nhan), g);

        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        // O nhap va khung chon thu chi chiem chieu cao preferredSize.
        form.add(oNhap, g);
    }

    private static LocalTime docGio(String chuoi, String truong) {
        if (chuoi == null || chuoi.isBlank()) return null;
        try {
            // Chi nhan HH:mm de khong chen gia tri ngoai y muon vao SQL TIME(0).
            if (!chuoi.trim().matches("[0-2][0-9]:[0-5][0-9]"))
                throw new DateTimeParseException("Sai dinh dang", chuoi, 0);
            return LocalTime.parse(chuoi.trim(), DINH_DANG_GIO);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(truong + " phải theo định dạng HH:mm, ví dụ 08:00");
        }
    }
    private static String hienThiGio(LocalTime gio) {
        return gio == null ? "" : gio.format(DINH_DANG_GIO);
    }

    private void luuNhieuBuoi(int maLop,
                               List<BuoiTrongTuan> cacBuoi, LocalDate ngayTu,
                               LocalDate ngayDen, String trangThai) {
        thongBao.setText("Đang lưu " + cacBuoi.size() + " thứ học...");
        new SwingWorker<List<Integer>, Void>() {
            @Override protected List<Integer> doInBackground() throws Exception {
                return controller.themNhieu(maLop, cacBuoi, ngayTu, ngayDen, trangThai);
            }
            @Override protected void done() {
                try {
                    int soBuoi = get().size();
                    JOptionPane.showMessageDialog(LichHocLopPanel.this,
                            "Đã thêm thành công " + soBuoi + " thứ học trong tuần.\n"
                            + "Mỗi thứ được lưu thành một dòng lịch cho cùng lớp.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void luuDuLieu(LichHocLop cu, int maLop,
                          int thu, LocalTime gioDau, LocalTime gioCuoi,
                          LocalDate ngayTu, LocalDate ngayDen, String trangThai) {
        thongBao.setText("Đang lưu lịch học...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (cu == null) {
                    controller.them(maLop, thu, gioDau, gioCuoi, ngayTu, ngayDen, trangThai);
                    return "Đã thêm lịch học Trung tâm";
                }
                controller.sua(cu.maLichHoc(), thu, gioDau, gioCuoi, ngayTu, ngayDen, trangThai);
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
