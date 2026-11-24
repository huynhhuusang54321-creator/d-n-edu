package vn.vinabacus.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Icon;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.DiemDanhTrungTamController;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.HocVien;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.LichSu;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.LuaChon;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.NhatKy;

/**
 * Giao diện Điểm danh Trung tâm theo kiểu Mầm non,
 * nhưng vẫn giữ nguyên dữ liệu của cơ sở Trung tâm.
 */
public final class DiemDanhTrungTamPanel extends JPanel {

    private static final Color XANH = MamNonUi.XANH;
    private static final Color NEN = Color.WHITE;
    private static final Color VIEN = new Color(208, 226, 215);
    private static final DateTimeFormatter DDMM = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String CHUA = "Chưa điểm danh";
    private static final String CO_MAT = "Có mặt";
    private static final String VANG_PHEP = "Vắng có phép";
    private static final String VANG_KHONG_PHEP = "Vắng không phép";

    private static final Color[][] MAU_THE = {
        {new Color(228, 249, 230), new Color(0, 137, 49), new Color(170, 222, 184)},
        {new Color(255, 248, 221), new Color(174, 115, 0), new Color(244, 214, 130)},
        {new Color(230, 240, 254), new Color(12, 92, 184), new Color(176, 207, 248)},
        {new Color(245, 237, 255), new Color(115, 66, 176), new Color(217, 193, 245)},
        {new Color(255, 235, 243), new Color(169, 60, 113), new Color(241, 187, 213)},
        {new Color(228, 249, 246), new Color(7, 121, 116), new Color(169, 226, 215)}
    };

    private final DiemDanhTrungTamController ctrl = new DiemDanhTrungTamController();

    private final MamNonNgayPicker ngay = new MamNonNgayPicker(LocalDate.now());
    private final MamNonNgayPicker tuNgay = new MamNonNgayPicker(LocalDate.now().withDayOfMonth(1));
    private final MamNonNgayPicker denNgay = new MamNonNgayPicker(LocalDate.now());

    private final JComboBox<LuaChon> cbTaiKhoan = new JComboBox<>();
    private final JComboBox<LuaChon> cbLopLichSu = new JComboBox<>();

    private final JLabel soLop = new JLabel("Đang tải lớp...");
    private final JLabel thongBao = new JLabel("Chọn lớp để điểm danh.");
    private final JLabel tenLopDangChon = new JLabel("Điểm danh lớp");
    private final JLabel lblHienThi = new JLabel("Hiển thị 0/0 học viên");
    private final JTextField txtTimKiem = new JTextField(24);

    private final JLabel[] thongKe = {
        new JLabel("0", SwingConstants.CENTER),
        new JLabel("0", SwingConstants.CENTER),
        new JLabel("0", SwingConstants.CENTER),
        new JLabel("0", SwingConstants.CENTER)
    };

    private final JButton btnLamMoi = MamNonUi.nut("↻  Làm mới", false);
    private final JButton btnQuayLai = MamNonUi.nut("←  Quay lại danh sách lớp", false);
    private final JButton btnTatCaCoMat = MamNonUi.nut("✓  Tất cả có mặt (cả lớp)", false);
    private final JButton btnDatLai = MamNonUi.nut("↻  Đặt lại điểm danh", false);
    private final JButton btnLuu = MamNonUi.nut("Lưu điểm danh", true);
    private final JButton btnLichSu = MamNonUi.nut("Xem lịch sử", false);
    private final JButton btnNhatKy = MamNonUi.nut("Nhật ký sửa", false);

    private final JPanel dsTheLop = new JPanel();
    private JScrollPane cuonThe;
    // Giữ đủ chiều rộng bốn nút điểm danh và cột "Lý do / Ghi chú".
    // Khi cửa sổ hẹp hơn nội dung, JScrollPane sẽ hiện thanh kéo ngang.
    private static final int RONG_BANG_DIEM_DANH = 1400;
    private final JPanel dsHocVienPanel = new JPanel() {
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(RONG_BANG_DIEM_DANH, size.width), size.height);
        }
    };
    private final JScrollPane cuonHocVien = new JScrollPane(dsHocVienPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private final CardLayoutEx cards = new CardLayoutEx();
    private final JPanel manHinhDiemDanh = new JPanel(cards);

    private final DefaultTableModel lichSuModel = MamNonUi.model(
            "Ngày", "Học viên", "Lớp", "Trạng thái", "Ghi chú", "Tài khoản", "Cập nhật (UTC)");
    private final JTable bangLichSu = MamNonUi.bang(lichSuModel);

    private final List<DongHocVien> dsDong = new ArrayList<>();
    private final Map<Integer, HocVien> banGoc = new HashMap<>();

    private LuaChon lopDangChon;
    private LocalDate ngayDaTai;
    private boolean dangXuLy;
    private boolean dangKhoiPhucNgay;

    public DiemDanhTrungTamPanel() {
        super(new BorderLayout(8, 8));
        setBackground(new Color(248, 251, 249));
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        add(MamNonUi.tieuDe("ĐIỂM DANH TRUNG TÂM"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Điểm danh theo ngày", taoTabDiemDanh());
        tabs.addTab("Lịch sử điểm danh", taoTabLichSu());
        add(tabs, BorderLayout.CENTER);
        add(thongBao, BorderLayout.SOUTH);

        btnLamMoi.addActionListener(e -> lamMoi());
        btnQuayLai.addActionListener(e -> quayLai());
        btnTatCaCoMat.addActionListener(e -> datTatCaTrangThai(CO_MAT));
        btnDatLai.addActionListener(e -> datTatCaTrangThai(CHUA));
        btnLuu.addActionListener(e -> luu());
        btnLichSu.addActionListener(e -> xemLichSu(false));
        btnNhatKy.addActionListener(e -> xemLichSu(true));

        ngay.addPropertyChangeListener("date", e -> {
            if (lopDangChon == null || dangKhoiPhucNgay) return;
            if (dangXuLy) {
                dangKhoiPhucNgay = true;
                try { ngay.setDate((LocalDate) e.getOldValue()); }
                finally { dangKhoiPhucNgay = false; }
                return;
            }
            if (!xacNhanBoThayDoi("đổi ngày điểm danh")) {
                dangKhoiPhucNgay = true;
                try { ngay.setDate((LocalDate) e.getOldValue()); }
                finally { dangKhoiPhucNgay = false; }
                return;
            }
            taiHocVien();
        });

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { locHocVien(); }
            @Override public void removeUpdate(DocumentEvent e) { locHocVien(); }
            @Override public void changedUpdate(DocumentEvent e) { locHocVien(); }
        });

        SwingUtilities.invokeLater(this::taiDanhMuc);
    }

    private JComponent taoTabDiemDanh() {
        JPanel page = new JPanel(new BorderLayout(6, 6));
        page.setBackground(NEN);
        page.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));

        JPanel congCu = MamNonUi.dong(new JLabel("Ngày điểm danh:"), ngay, btnLamMoi);
        congCu.setBackground(NEN);
        page.add(congCu, BorderLayout.NORTH);

        manHinhDiemDanh.add(taoManHinhChonLop(), "lop");
        manHinhDiemDanh.add(taoManHinhChiTiet(), "chitiet");
        page.add(manHinhDiemDanh, BorderLayout.CENTER);
        cards.show(manHinhDiemDanh, "lop");
        return page;
    }

    private JComponent taoManHinhChonLop() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBackground(NEN);

        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        JLabel caption = new JLabel("CHỌN LỚP ĐỂ ĐIỂM DANH");
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 18f));
        caption.setForeground(XANH);
        title.add(caption, BorderLayout.WEST);
        soLop.setForeground(new Color(90, 105, 95));
        title.add(soLop, BorderLayout.EAST);
        page.add(title, BorderLayout.NORTH);

        dsTheLop.setBackground(NEN);
        dsTheLop.setBorder(BorderFactory.createEmptyBorder(9, 5, 12, 5));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(NEN);
        wrap.add(dsTheLop, BorderLayout.NORTH);

        cuonThe = new JScrollPane(wrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cuonThe.setBorder(BorderFactory.createEmptyBorder());
        cuonThe.getViewport().setBackground(NEN);
        cuonThe.getVerticalScrollBar().setUnitIncrement(18);
        cuonThe.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                sapXepThe();
            }
        });
        page.add(cuonThe, BorderLayout.CENTER);
        return page;
    }

    private JComponent taoManHinhChiTiet() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.setBackground(NEN);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);
        btnQuayLai.setPreferredSize(new Dimension(250, 40));
        row1.add(btnQuayLai, BorderLayout.WEST);
        tenLopDangChon.setForeground(XANH);
        tenLopDangChon.setFont(tenLopDangChon.getFont().deriveFont(Font.BOLD, 19f));
        row1.add(tenLopDangChon, BorderLayout.CENTER);
        top.add(row1);
        top.add(Box.createVerticalStrut(8));

        JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 0));
        metrics.setOpaque(false);
        metrics.add(oThongKe(new Color(227, 244, 231), new Color(0, 103, 53), thongKe[0], CO_MAT));
        metrics.add(oThongKe(new Color(226, 236, 250), new Color(27, 94, 180), thongKe[1], VANG_PHEP));
        metrics.add(oThongKe(new Color(247, 230, 230), new Color(200, 48, 48), thongKe[2], VANG_KHONG_PHEP));
        metrics.add(oThongKe(new Color(238, 241, 244), new Color(77, 91, 107), thongKe[3], CHUA));
        top.add(metrics);
        top.add(Box.createVerticalStrut(8));

        JPanel row2 = new JPanel(new BorderLayout(10, 0));
        row2.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(new JLabel("Tìm học sinh: "));
        txtTimKiem.setPreferredSize(new Dimension(340, 36));
        left.add(txtTimKiem);
        row2.add(left, BorderLayout.WEST);
        lblHienThi.setForeground(new Color(76, 89, 98));
        row2.add(lblHienThi, BorderLayout.EAST);
        top.add(row2);

        page.add(top, BorderLayout.NORTH);

        dsHocVienPanel.setLayout(new BoxLayout(dsHocVienPanel, BoxLayout.Y_AXIS));
        dsHocVienPanel.setBackground(Color.WHITE);
        cuonHocVien.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        cuonHocVien.getViewport().setBackground(Color.WHITE);
        // Có thể kéo ngang phía dưới bảng để nhập "Lý do / Ghi chú".
        cuonHocVien.getHorizontalScrollBar().setUnitIncrement(24);
        cuonHocVien.getHorizontalScrollBar().setBlockIncrement(220);
        cuonHocVien.getVerticalScrollBar().setUnitIncrement(20);
        page.add(cuonHocVien, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(10, 8));
        footer.setBackground(new Color(236, 248, 240));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFooter.setOpaque(false);
        leftFooter.add(btnTatCaCoMat);
        leftFooter.add(btnDatLai);
        footer.add(leftFooter, BorderLayout.WEST);

        JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightFooter.setOpaque(false);
        rightFooter.add(new JLabel("Tài khoản điểm danh:"));
        cbTaiKhoan.setPreferredSize(new Dimension(220, 38));
        rightFooter.add(cbTaiKhoan);
        rightFooter.add(btnLuu);
        footer.add(rightFooter, BorderLayout.EAST);

        page.add(footer, BorderLayout.SOUTH);
        return page;
    }

    private JPanel oThongKe(Color bg, Color fg, JLabel so, String text) {
        JPanel p = new JPanel(new BorderLayout(2, 1));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        so.setFont(so.getFont().deriveFont(Font.BOLD, 21f));
        so.setForeground(fg);
        p.add(so, BorderLayout.CENTER);
        JLabel name = new JLabel(text, SwingConstants.CENTER);
        name.setForeground(fg);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 12.5f));
        p.add(name, BorderLayout.SOUTH);
        return p;
    }

    private JComponent taoTabLichSu() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.setBackground(NEN);
        JPanel filters = MamNonUi.thanhCongCu(
                new JLabel("Lớp:"), cbLopLichSu,
                new JLabel("Từ ngày:"), tuNgay,
                new JLabel("Đến ngày:"), denNgay,
                btnLichSu, btnNhatKy);
        filters.setBackground(NEN);
        cbLopLichSu.setPreferredSize(new Dimension(240, 38));
        page.add(filters, BorderLayout.NORTH);
        bangLichSu.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        page.add(new JScrollPane(bangLichSu), BorderLayout.CENTER);
        return page;
    }

    private void taiDanhMuc() {
        nen(() -> new List<?>[]{ctrl.lop(), ctrl.taiKhoan()}, data -> {
            @SuppressWarnings("unchecked")
            List<LuaChon> ds = (List<LuaChon>) data[0];
            cbTaiKhoan.removeAllItems();
            cbLopLichSu.removeAllItems();
            for (Object obj : data[1]) cbTaiKhoan.addItem((LuaChon) obj);
            for (LuaChon lop : ds) cbLopLichSu.addItem(lop);
            veDanhSachLop(ds);
            thongBao.setText("Đã tải " + ds.size() + " lớp. Chọn lớp để điểm danh.");
        });
    }

    private void veDanhSachLop(List<LuaChon> ds) {
        dsTheLop.removeAll();
        if (ds.isEmpty()) {
            dsTheLop.add(new JLabel("Trung tâm chưa có lớp học.", SwingConstants.CENTER));
        } else {
            for (int i = 0; i < ds.size(); i++) dsTheLop.add(taoTheLop(ds.get(i), i));
        }
        soLop.setText(ds.size() + " lớp");
        sapXepThe();
        dsTheLop.revalidate();
        dsTheLop.repaint();
    }

    private JComponent taoTheLop(LuaChon lop, int index) {
        Color[] pal = MAU_THE[index % MAU_THE.length];
        JPanel card = new JPanel(new BorderLayout(7, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pal[0]);
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g2.setColor(pal[2]);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 17, 16, 17));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String[] tach = lop.ten().split(" – ", 2);
        JLabel ten = new JLabel("<html>" + html(tach[0]) + "</html>");
        ten.setFont(ten.getFont().deriveFont(Font.BOLD, 18f));
        ten.setForeground(pal[1].darker());

        JLabel moTa = new JLabel(tach.length == 2 ? tach[1] : "Lớp Trung tâm");
        moTa.setForeground(pal[1].darker());
        moTa.setToolTipText(lop.ten());

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 5));
        info.setOpaque(false);
        info.add(ten);
        info.add(moTa);
        card.add(info, BorderLayout.CENTER);

        JButton xem = new JButton("Xem học viên   →");
        xem.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        xem.setOpaque(true);
        xem.setContentAreaFilled(true);
        xem.setBackground(pal[1]);
        xem.setForeground(Color.WHITE);
        xem.setFocusPainted(false);
        xem.setBorder(BorderFactory.createEmptyBorder(11, 9, 11, 9));
        xem.setFont(xem.getFont().deriveFont(Font.BOLD, 13f));
        xem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        xem.addActionListener(e -> moLop(lop));
        card.add(xem, BorderLayout.SOUTH);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                moLop(lop);
            }
        });
        return card;
    }

    private void sapXepThe() {
        if (cuonThe == null) return;
        int width = cuonThe.getViewport().getWidth();
        if (width <= 0) width = 1000;
        int cols = width >= 730 ? 3 : width >= 510 ? 2 : 1;
        int rows = Math.max(1, (dsTheLop.getComponentCount() + cols - 1) / cols);
        dsTheLop.setLayout(new GridLayout(0, cols, 16, 16));
        Dimension size = new Dimension(width, 12 + rows * 163 + (rows - 1) * 16);
        if (!size.equals(dsTheLop.getPreferredSize())) dsTheLop.setPreferredSize(size);
        dsTheLop.revalidate();
    }

    private void moLop(LuaChon lop) {
        if (dangXuLy || !xacNhanBoThayDoi("chuyển lớp")) return;
        lopDangChon = lop;
        tenLopDangChon.setText("Điểm danh – " + lop.ten());
        cards.show(manHinhDiemDanh, "chitiet");
        taiHocVien();
    }

    private void quayLai() {
        if (dangXuLy || !xacNhanBoThayDoi("quay lại danh sách lớp")) return;
        lopDangChon = null;
        ngayDaTai = null;
        banGoc.clear();
        dsDong.clear();
        dsHocVienPanel.removeAll();
        capNhatThongKe();
        capNhatSoHienThi();
        txtTimKiem.setText("");
        cards.show(manHinhDiemDanh, "lop");
    }

    private void lamMoi() {
        if (dangXuLy || !xacNhanBoThayDoi("làm mới dữ liệu")) return;
        if (lopDangChon == null) taiDanhMuc();
        else taiHocVien();
    }

    private void taiHocVien() {
        LuaChon lop = lopDangChon;
        if (lop == null || dangXuLy) return;
        LocalDate date = ngay.getDate();
        ngayDaTai = null;
        thongBao.setText("Đang lấy học viên của lớp " + lop.ten() + "...");
        nen(() -> ctrl.bang(lop.id(), date), ds -> {
            if (lopDangChon == null || lopDangChon.id() != lop.id() || !Objects.equals(ngay.getDate(), date)) return;
            banGoc.clear();
            dsDong.clear();
            int stt = 1;
            for (HocVien hv : ds) {
                banGoc.put(hv.id(), hv);
                dsDong.add(new DongHocVien(stt++, hv));
            }
            ngayDaTai = date;
            txtTimKiem.setText("");
            veDanhSachHocVien();
            thongBao.setText("Lớp " + lop.ten() + ": " + ds.size() + " học viên · Ngày " + date.format(DDMM) + ".");
        });
    }

    private void veDanhSachHocVien() {
        dsHocVienPanel.removeAll();
        dsHocVienPanel.add(taoHeaderHocVien());
        for (DongHocVien dong : dsDong) dsHocVienPanel.add(dong);
        dsHocVienPanel.add(Box.createVerticalGlue());
        dsHocVienPanel.revalidate();
        dsHocVienPanel.repaint();
        locHocVien();
        capNhatThongKe();
    }

    private JComponent taoHeaderHocVien() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(true);
        p.setBackground(new Color(224, 241, 225));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(315, 30));
        JLabel a = new JLabel("STT");
        a.setPreferredSize(new Dimension(52, 30));
        a.setFont(a.getFont().deriveFont(Font.BOLD, 12.5f));
        JLabel b = new JLabel("Họ và tên");
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12.5f));
        left.add(a, BorderLayout.WEST);
        left.add(b, BorderLayout.CENTER);

        JLabel c = new JLabel("Trạng thái điểm danh", SwingConstants.LEFT);
        c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        c.setFont(c.getFont().deriveFont(Font.BOLD, 12.5f));
        c.setPreferredSize(new Dimension(610, 30));

        JLabel d = new JLabel("Lý do / Ghi chú", SwingConstants.CENTER);
        d.setFont(d.getFont().deriveFont(Font.BOLD, 12.5f));
        d.setPreferredSize(new Dimension(250, 30));

        p.add(left, BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        p.add(d, BorderLayout.EAST);
        return p;
    }

    private void locHocVien() {
        String kw = txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim().toLowerCase();
        int total = 0, show = 0;
        for (DongHocVien dong : dsDong) {
            total++;
            boolean visible = kw.isEmpty() || dong.tenKhongDau().contains(boDau(kw)) || dong.hocVien.ten().toLowerCase().contains(kw);
            dong.setVisible(visible);
            if (visible) show++;
        }
        lblHienThi.setText("Hiển thị " + show + "/" + total + " học sinh");
        dsHocVienPanel.revalidate();
        dsHocVienPanel.repaint();
    }

    private void capNhatSoHienThi() {
        lblHienThi.setText("Hiển thị 0/0 học sinh");
    }

    private void datTatCaTrangThai(String state) {
        if (dsDong.isEmpty()) return;
        for (DongHocVien dong : dsDong) dong.setTrangThai(state);
        capNhatThongKe();
    }

    private void capNhatThongKe() {
        int[] dem = new int[4];
        for (DongHocVien dong : dsDong) {
            switch (dong.getTrangThai()) {
                case CO_MAT -> dem[0]++;
                case VANG_PHEP -> dem[1]++;
                case VANG_KHONG_PHEP -> dem[2]++;
                default -> dem[3]++;
            }
        }
        for (int i = 0; i < 4; i++) thongKe[i].setText(String.valueOf(dem[i]));
    }

    private boolean coThayDoiChuaLuu() {
        if (lopDangChon == null || ngayDaTai == null) return false;
        for (DongHocVien dong : dsDong) {
            HocVien old = banGoc.get(dong.hocVien.id());
            if (old == null) continue;
            String noteNow = dong.getGhiChu();
            String noteOld = old.ghiChu() == null ? "" : old.ghiChu();
            if (!Objects.equals(maTrangThai(dong.getTrangThai()), old.trangThai()) || !Objects.equals(noteNow, noteOld)) {
                return true;
            }
        }
        return false;
    }

    private boolean xacNhanBoThayDoi(String hanhDong) {
        return !coThayDoiChuaLuu() || MamNonUi.xacNhan(this,
                "Có điểm danh hoặc ghi chú chưa lưu. Bạn có muốn " + hanhDong + " và bỏ các thay đổi này?",
                "Điểm danh chưa lưu", true);
    }

    private void luu() {
        if (dangXuLy) return;
        LuaChon lop = lopDangChon;
        LuaChon tk = (LuaChon) cbTaiKhoan.getSelectedItem();
        LocalDate date = ngay.getDate();
        if (lop == null || tk == null || !Objects.equals(date, ngayDaTai)) {
            thongBao("Chọn lớp, tài khoản và đợi danh sách học viên tải xong.");
            return;
        }
        if (dsDong.isEmpty()) {
            thongBao("Lớp này chưa có học viên để điểm danh vào ngày đã chọn.");
            return;
        }

        List<HocVien> changes = new ArrayList<>();
        for (DongHocVien dong : dsDong) {
            String state = dong.getTrangThai();
            String note = dong.getGhiChu();
            if (VANG_PHEP.equals(state) && note.isBlank()) {
                thongBao("Học viên " + dong.hocVien.ten() + " vắng có phép: vui lòng nhập ghi chú.");
                dong.focusGhiChu();
                return;
            }
            if (note.length() > 500) {
                thongBao("Ghi chú của học viên " + dong.hocVien.ten() + " không được quá 500 ký tự.");
                dong.focusGhiChu();
                return;
            }
            changes.add(new HocVien(dong.hocVien.id(), dong.hocVien.ten(), maTrangThai(state), note));
        }

        if (!MamNonUi.xacNhan(this,
                "Lưu điểm danh lớp '" + lop.ten() + "' ngày " + date.format(DDMM)
                        + " bằng tài khoản '" + tk.ten() + "'?",
                "Xác nhận điểm danh", false)) return;

        String lyDoTuDong = "Cập nhật điểm danh tại giao diện Trung tâm";
        nen(() -> ctrl.luu(lop.id(), date, tk, lop.ten(), lyDoTuDong, changes), count -> {
            thongBao.setText("Đã lưu " + count + " thay đổi vào file điểm danh Trung tâm.");
            JOptionPane.showMessageDialog(this, "Đã lưu " + count + " thay đổi.");
            SwingUtilities.invokeLater(this::taiHocVien);
        });
    }

    private void xemLichSu(boolean nhatKy) {
        LuaChon lop = (LuaChon) cbLopLichSu.getSelectedItem();
        if (lop == null) { thongBao("Chọn lớp cần xem lịch sử."); return; }
        LocalDate from = tuNgay.getDate(), to = denNgay.getDate();
        if (from.isAfter(to)) {
            thongBao("Ngày bắt đầu không được lớn hơn ngày kết thúc.");
            return;
        }
        nen(() -> nhatKy ? ctrl.nhatKy(lop.id(), from, to) : ctrl.lichSu(lop.id(), from, to), rows -> {
            lichSuModel.setRowCount(0);
            if (nhatKy) {
                lichSuModel.setColumnIdentifiers(new String[]{
                    "Thời gian (UTC)", "Học viên", "Lớp", "Trước", "Sau", "Lý do", "Tài khoản"});
                for (Object o : rows) {
                    NhatKy n = (NhatKy) o;
                    lichSuModel.addRow(new Object[]{n.ngaySua(), n.tenHocVien(), n.tenLop(),
                        hienThi(n.truoc()), hienThi(n.sau()), n.lyDo(), n.taiKhoan()});
                }
            } else {
                lichSuModel.setColumnIdentifiers(new String[]{
                    "Ngày", "Học viên", "Lớp", "Trạng thái", "Ghi chú", "Tài khoản", "Cập nhật (UTC)"});
                for (Object o : rows) {
                    LichSu n = (LichSu) o;
                    lichSuModel.addRow(new Object[]{n.ngay().format(DDMM), n.tenHocVien(), n.tenLop(),
                        hienThi(n.trangThai()), n.ghiChu(), n.taiKhoan(), n.capNhat()});
                }
            }
            thongBao.setText("Đã tìm thấy " + rows.size() + " bản ghi.");
        });
    }

    private static String hienThi(String s) {
        if (s == null) return CHUA;
        return switch (s) {
            case "CO_MAT" -> CO_MAT;
            case "VANG_CO_PHEP" -> VANG_PHEP;
            case "VANG_KHONG_PHEP" -> VANG_KHONG_PHEP;
            default -> CHUA;
        };
    }

    private static String maTrangThai(String s) {
        return switch (s) {
            case CO_MAT -> "CO_MAT";
            case VANG_PHEP -> "VANG_CO_PHEP";
            case VANG_KHONG_PHEP -> "VANG_KHONG_PHEP";
            default -> "CHUA_DIEM_DANH";
        };
    }

    private static String html(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String boDau(String s) {
        String n = java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "").toLowerCase();
    }

    private void thongBao(String text) {
        JOptionPane.showMessageDialog(this, text, "VINABACUS – Điểm danh Trung tâm", JOptionPane.INFORMATION_MESSAGE);
    }

    private void khoa(boolean value) {
        dangXuLy = value;
        btnLamMoi.setEnabled(!value);
        btnQuayLai.setEnabled(!value);
        btnTatCaCoMat.setEnabled(!value);
        btnDatLai.setEnabled(!value);
        btnLuu.setEnabled(!value);
        btnLichSu.setEnabled(!value);
        btnNhatKy.setEnabled(!value);
        cbTaiKhoan.setEnabled(!value);
        cbLopLichSu.setEnabled(!value);
        txtTimKiem.setEnabled(!value);
        ngay.setEnabled(!value);
        for (DongHocVien dong : dsDong) dong.setEnabledAll(!value);
    }

    @FunctionalInterface private interface TacVu<T> { T chay() throws Exception; }
    @FunctionalInterface private interface KetQua<T> { void nhan(T value); }

    private <T> void nen(TacVu<T> task, KetQua<T> callback) {
        if (dangXuLy) return;
        khoa(true);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.chay(); }
            @Override protected void done() {
                try { callback.nhan(get()); }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    MamNonUi.loi(DiemDanhTrungTamPanel.this, ex);
                } catch (ExecutionException ex) {
                    MamNonUi.loi(DiemDanhTrungTamPanel.this, ex.getCause() == null ? ex : ex.getCause());
                } finally {
                    khoa(false);
                }
            }
        }.execute();
    }

    private final class DongHocVien extends JPanel {
        private final int stt;
        private final HocVien hocVien;
        private final JLabel lblTen = new JLabel();
        // Bốn nút dạng radio bo tròn như hình mẫu Mầm non.
        private final JToggleButton rdCoMat = nutLuaChon(CO_MAT, new Color(0, 113, 50), 109);
        private final JToggleButton rdVangPhep = nutLuaChon(VANG_PHEP, new Color(17, 91, 192), 136);
        private final JToggleButton rdVangKhongPhep = nutLuaChon(VANG_KHONG_PHEP, new Color(196, 40, 35), 164);
        private final JToggleButton rdChua = nutLuaChon(CHUA, new Color(61, 81, 100), 175);
        private final JTextField txtGhiChu = new JTextField();

        private DongHocVien(int stt, HocVien hocVien) {
            super(new BorderLayout());
            this.stt = stt;
            this.hocVien = hocVien;
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(223, 232, 223)));

            JPanel left = new JPanel(new BorderLayout());
            left.setOpaque(false);
            left.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            left.setPreferredSize(new Dimension(315, 65));
            JLabel lblStt = new JLabel(String.valueOf(stt));
            lblStt.setPreferredSize(new Dimension(52, 30));
            lblStt.setFont(lblStt.getFont().deriveFont(Font.PLAIN, 14f));
            lblTen.setText(hocVien.ten());
            lblTen.setFont(lblTen.getFont().deriveFont(Font.BOLD, 14f));
            lblTen.setForeground(new Color(13, 66, 60));
            left.add(lblStt, BorderLayout.WEST);
            left.add(lblTen, BorderLayout.CENTER);

            // FlowLayout giữ đúng chiều rộng của từng nút theo chữ,
            // không chia đều bốn cột như GridLayout cũ.
            JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            center.setOpaque(false);
            center.setBorder(BorderFactory.createEmptyBorder(9, 8, 9, 0));
            center.setPreferredSize(new Dimension(610, 65));
            ButtonGroup group = new ButtonGroup();
            group.add(rdCoMat);
            group.add(rdVangPhep);
            group.add(rdVangKhongPhep);
            group.add(rdChua);
            center.add(rdCoMat);
            center.add(rdVangPhep);
            center.add(rdVangKhongPhep);
            center.add(rdChua);

            JPanel right = new JPanel(new BorderLayout());
            right.setOpaque(false);
            right.setBorder(BorderFactory.createEmptyBorder(9, 8, 9, 10));
            right.setPreferredSize(new Dimension(250, 65));
            txtGhiChu.setPreferredSize(new Dimension(220, 39));
            txtGhiChu.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { capNhatMauGhiChu(); }
                @Override public void removeUpdate(DocumentEvent e) { capNhatMauGhiChu(); }
                @Override public void changedUpdate(DocumentEvent e) { capNhatMauGhiChu(); }
            });
            right.add(txtGhiChu, BorderLayout.CENTER);

            add(left, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);

            setTrangThai(hienThi(hocVien.trangThai()));
            txtGhiChu.setText(hocVien.ghiChu() == null ? "" : hocVien.ghiChu());

            rdCoMat.addActionListener(e -> onTrangThaiDoi());
            rdVangPhep.addActionListener(e -> onTrangThaiDoi());
            rdVangKhongPhep.addActionListener(e -> onTrangThaiDoi());
            rdChua.addActionListener(e -> onTrangThaiDoi());
            capNhatMauGhiChu();
        }

        private String tenKhongDau() {
            return boDau(hocVien.ten());
        }

        private void onTrangThaiDoi() {
            if (!VANG_PHEP.equals(getTrangThai())) {
                txtGhiChu.setText("");
            }
            capNhatMauGhiChu();
            capNhatThongKe();
        }

        private void capNhatMauGhiChu() {
            boolean enabled = VANG_PHEP.equals(getTrangThai()) && !dangXuLy;
            txtGhiChu.setEnabled(enabled);
            if (enabled) {
                txtGhiChu.setBackground(Color.WHITE);
                txtGhiChu.setToolTipText("Nhập ghi chú cho học viên vắng có phép");
            } else {
                txtGhiChu.setBackground(new Color(245, 247, 248));
                txtGhiChu.setToolTipText("Chỉ nhập ghi chú khi chọn Vắng có phép");
            }
        }

        private void setEnabledAll(boolean enabled) {
            rdCoMat.setEnabled(enabled);
            rdVangPhep.setEnabled(enabled);
            rdVangKhongPhep.setEnabled(enabled);
            rdChua.setEnabled(enabled);
            capNhatMauGhiChu();
        }

        private void focusGhiChu() {
            txtGhiChu.requestFocusInWindow();
        }

        private String getTrangThai() {
            if (rdCoMat.isSelected()) return CO_MAT;
            if (rdVangPhep.isSelected()) return VANG_PHEP;
            if (rdVangKhongPhep.isSelected()) return VANG_KHONG_PHEP;
            return CHUA;
        }

        private void setTrangThai(String state) {
            switch (state) {
                case CO_MAT -> rdCoMat.setSelected(true);
                case VANG_PHEP -> rdVangPhep.setSelected(true);
                case VANG_KHONG_PHEP -> rdVangKhongPhep.setSelected(true);
                default -> rdChua.setSelected(true);
            }
            if (!VANG_PHEP.equals(state)) txtGhiChu.setText("");
            capNhatMauGhiChu();
        }

        private String getGhiChu() {
            return txtGhiChu.getText() == null ? "" : txtGhiChu.getText().trim();
        }
    }

    /**
     * Nút trạng thái kiểu Mầm non: viền bo tròn + dấu tròn bên trái.
     * Việc chọn duy nhất một trạng thái vẫn do ButtonGroup xử lý.
     */
    private static JToggleButton nutLuaChon(String text, Color textColor, int width) {
        JToggleButton button = new JToggleButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                boolean selected = isSelected();
                g2.setColor(selected ? new Color(237, 241, 245) : Color.WHITE);
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.setColor(selected ? new Color(162, 173, 183)
                        : new Color(218, 230, 223));
                g2.setStroke(new BasicStroke(selected ? 1.7f : 1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(5);
        button.setIcon(new Icon() {
            @Override public int getIconWidth() { return 17; }
            @Override public int getIconHeight() { return 17; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                boolean selected = c instanceof AbstractButton
                        && ((AbstractButton) c).isSelected();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                if (selected) {
                    g2.setColor(new Color(77, 94, 107));
                    g2.fillOval(x + 1, y + 1, 15, 15);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(x + 6, y + 6, 5, 5);
                } else {
                    g2.setColor(new Color(159, 181, 170));
                    g2.setStroke(new BasicStroke(1.7f));
                    g2.drawOval(x + 2, y + 2, 13, 13);
                }
                g2.dispose();
            }
        });
        button.setForeground(textColor);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12.5f));
        button.setPreferredSize(new Dimension(width, 39));
        button.setMinimumSize(new Dimension(width, 39));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static final class CardLayoutEx extends java.awt.CardLayout {
        private static final long serialVersionUID = 1L;
    }
}
