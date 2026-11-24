package vn.vinabacus.view;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.*;
import vn.vinabacus.repository.MamNonRepository.*;
import vn.vinabacus.tv4.NamHocMacDinh;

/**
 * TV4 – Chọn lớp bằng thẻ trước khi điểm danh học sinh. Giữ nguyên nghiệp vụ
 * SQL, điểm danh theo ngày và tra cứu lịch sử.
 */
public final class DiemDanhMamNonPanel extends JPanel {

    private static final Color GREEN = MamNonUi.XANH;
    private static final Color LIGHT = new Color(242, 249, 245);
    private static final Color BORDER = new Color(208, 226, 215);

    private enum AttendanceState {
        CHUA_DIEM_DANH("Chưa điểm danh"),
        CO_MAT("Có mặt"),
        VANG_CO_PHEP("Vắng có phép"),
        VANG_KHONG_PHEP("Vắng không phép");

        private final String label;

        AttendanceState(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        static AttendanceState fromCode(String code) {
            try {
                return valueOf(code);
            } catch (IllegalArgumentException | NullPointerException ex) {
                return CHUA_DIEM_DANH;
            }
        }
    }

    private final MamNonController ctrl = new MamNonController();
    private final JComboBox<NamHocMamNon> cbYear = new JComboBox<>();
    private final JComboBox<LuaChon> cbTaiKhoan = new JComboBox<>();
    private final MamNonNgayPicker ngay = new MamNonNgayPicker(LocalDate.now());
    private final JTextField lyDo = new JTextField(23);
    private boolean loadingYears = false;
    private boolean revertingDate = false;
    private Integer displayedYearId;
    private final JTextField studentSearch = new JTextField(25);
    private final JLabel filterSummary = new JLabel("0 học sinh");
    private final PropertyChangeListener yearListener = e -> SwingUtilities.invokeLater(() -> loadYears(true));

    // Màn hình thứ nhất: thẻ lớp. Màn hình thứ hai: học sinh của lớp đã chọn.
    private final CardLayout attendanceLayout = new CardLayout();
    private final JPanel attendanceViews = new JPanel(attendanceLayout);
    private final JPanel classCards = new JPanel();
    private JScrollPane classesScroll;
    private final JLabel classSummary = new JLabel("Đang tải danh sách lớp...");
    private final JLabel selectedClassTitle = new JLabel("Danh sách học sinh");
    private final JLabel detailSubTitle = new JLabel("Năm học · Ngày điểm danh");
    private LopMamNon selectedClass;
    private List<LopMamNon> availableClasses = List.of();
    private int classesRequest = 0, dayRequest = 0;

    // UI theo từng học sinh: radio trực tiếp, ẩn mã học sinh trên màn hình.
    private final JPanel studentsPanel = new JPanel();
    private final List<StudentLine> studentLines = new ArrayList<>();
    private final JLabel[] totals = {
        new JLabel("0", SwingConstants.CENTER), new JLabel("0", SwingConstants.CENTER),
        new JLabel("0", SwingConstants.CENTER), new JLabel("0", SwingConstants.CENTER)
    };
    private final JLabel status = new JLabel("Chọn lớp để xem học sinh và điểm danh.");
    private List<DiemDanhMamNon> records = List.of();

    // Tra cứu lịch sử vẫn độc lập với giao diện thẻ lớp.
    private final JComboBox<LopMamNon> histLop = new JComboBox<>();
    private final JComboBox<LuaChon> histHocSinh = new JComboBox<>();
    private final MamNonNgayPicker histTu = new MamNonNgayPicker(LocalDate.now().withDayOfMonth(1));
    private final MamNonNgayPicker histDen = new MamNonNgayPicker(LocalDate.now());
    private final DefaultTableModel histModel = MamNonUi.model(
            "Ngày học", "Học sinh", "Lớp", "Trạng thái", "Ghi chú", "Người ghi", "Cập nhật (UTC)");
    private final JTable histTable = MamNonUi.bang(histModel);
    private List<LichSuDiemDanh> history = List.of();

    public DiemDanhMamNonPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setBackground(new Color(248, 251, 249));
        add(MamNonUi.tieuDe("ĐIỂM DANH MẦM NON"), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Điểm danh theo ngày", createToday());
        tabs.addTab("Lịch sử điểm danh", createHistory());
        final boolean[] restoringTab = {false};
        tabs.addChangeListener(e -> {
            if (restoringTab[0] || tabs.getSelectedIndex() != 1 || selectedClass == null) {
                return;
            }
            if (!confirmDiscard("xem lịch sử điểm danh")) {
                restoringTab[0] = true;
                try {
                    tabs.setSelectedIndex(0);
                } finally {
                    restoringTab[0] = false;
                }
            } else {
                // Đã xác nhận bỏ thay đổi: tải lại trạng thái đã lưu trước khi rời tab.
                loadDay();
            }
        });
        add(tabs, BorderLayout.CENTER);
        cbYear.addActionListener(e -> {
            if (loadingYears) {
                return;
            }
            if (!confirmDiscard("đổi năm học")) {
                loadingYears = true;
                try {
                    for (int i = 0; i < cbYear.getItemCount(); i++) {
                        if (displayedYearId != null && cbYear.getItemAt(i).maNamHoc() == displayedYearId) {
                            cbYear.setSelectedIndex(i);
                            break;
                        }
                    }
                } finally {
                    loadingYears = false;
                }
                return;
            }
            loadClasses();
        });
        SwingUtilities.invokeLater(this::loadChoices);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        NamHocMacDinh.langNghe(yearListener);
    }

    @Override
    public void removeNotify() {
        NamHocMacDinh.ngungNghe(yearListener);
        super.removeNotify();
    }

    private JComponent createToday() {
        JPanel page = new JPanel(new BorderLayout(5, 5));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
        JButton refresh = new ModernButton("↻  Làm mới", Style.OUTLINE);
        // Các điều khiển ở thanh công cụ có cùng chiều cao, không còn nút cao thấp khác nhau.
        final int toolHeight = 40;
        cbYear.setPreferredSize(new Dimension(190, toolHeight));
        cbYear.setMinimumSize(new Dimension(165, toolHeight));
        cbYear.setFont(cbYear.getFont().deriveFont(Font.PLAIN, 13f));
        ngay.setPreferredSize(new Dimension(310, toolHeight));
        ngay.setMinimumSize(new Dimension(280, toolHeight));
        refresh.setPreferredSize(new Dimension(135, toolHeight));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 11, 0));
        tools.setBackground(Color.WHITE);
        JLabel yearLabel = new JLabel("Năm học:");
        JLabel dateLabel = new JLabel("Ngày điểm danh:");
        for (JLabel label : new JLabel[]{yearLabel, dateLabel}) {
            label.setForeground(new Color(30, 75, 48));
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12.5f));
        }
        tools.add(yearLabel);
        tools.add(cbYear);
        tools.add(dateLabel);
        tools.add(ngay);
        tools.add(refresh);
        JPanel toolsWrap = new JPanel(new BorderLayout());
        toolsWrap.setBackground(Color.WHITE);
        toolsWrap.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        toolsWrap.add(tools, BorderLayout.WEST);
        page.add(toolsWrap, BorderLayout.NORTH);
        refresh.addActionListener(e -> {
            if (!confirmDiscard("tải lại dữ liệu đã lưu")) {
                return;
            }
            if (selectedClass != null) {
                loadDay();
            } else {
                loadClasses();
            }
        });
        ngay.addPropertyChangeListener("date", e -> {
            if (revertingDate || selectedClass == null) {
                return;
            }
            if (!confirmDiscard("đổi ngày điểm danh")) {
                revertingDate = true;
                try {
                    ngay.setDate((LocalDate) e.getOldValue());
                } finally {
                    revertingDate = false;
                }
                return;
            }
            loadDay();
        });

        attendanceViews.add(createClassOverview(), "classes");
        attendanceViews.add(createClassDetails(), "students");
        page.add(attendanceViews, BorderLayout.CENTER);
        attendanceLayout.show(attendanceViews, "classes");
        return page;
    }

    /**
     * Danh sách thẻ gọn, mỗi lớp một màu; chỉ hiển thị thông tin thực sự cần
     * thiết.
     */
    private JComponent createClassOverview() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBackground(Color.WHITE);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("CHỌN LỚP ĐỂ ĐIỂM DANH");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(GREEN);
        heading.add(title, BorderLayout.WEST);
        classSummary.setFont(classSummary.getFont().deriveFont(Font.PLAIN, 12f));
        classSummary.setForeground(new Color(86, 106, 94));
        heading.add(classSummary, BorderLayout.EAST);
        heading.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        page.add(heading, BorderLayout.NORTH);

        classCards.setBackground(Color.WHITE);
        classCards.setBorder(BorderFactory.createEmptyBorder(8, 5, 12, 5));
        // JScrollPane tự kéo giãn view đủ chiều cao. Dùng lớp bọc để thẻ không cao theo cửa sổ.
        JPanel classCanvas = new JPanel(new BorderLayout());
        classCanvas.setBackground(Color.WHITE);
        classCanvas.add(classCards, BorderLayout.NORTH);
        classesScroll = new JScrollPane(classCanvas,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        classesScroll.setBorder(BorderFactory.createEmptyBorder());
        classesScroll.getViewport().setBackground(Color.WHITE);
        classesScroll.getVerticalScrollBar().setUnitIncrement(20);
        classesScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutClassGrid();
            }
        });
        page.add(classesScroll, BorderLayout.CENTER);
        return page;
    }

    /**
     * Một màn hình riêng cho mỗi lớp; radio dạng viên màu, không dùng ComboBox.
     */
    private JComponent createClassDetails() {
        JPanel page = new JPanel(new BorderLayout(4, 4));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        JPanel top = new JPanel(new BorderLayout(0, 5));
        top.setOpaque(false);
        JButton back = new ModernButton("←  Quay lại danh sách lớp", Style.OUTLINE);
        back.addActionListener(e -> {
            if (confirmDiscard("quay lại danh sách lớp")) {
                showClasses();
            }
        });
        back.setPreferredSize(new Dimension(217, 36));
        selectedClassTitle.setFont(selectedClassTitle.getFont().deriveFont(Font.BOLD, 19f));
        selectedClassTitle.setForeground(new Color(0, 88, 45));
        detailSubTitle.setFont(detailSubTitle.getFont().deriveFont(Font.PLAIN, 11f));
        detailSubTitle.setForeground(new Color(88, 111, 98));
        // Năm học và ngày đã hiển thị ở bộ lọc bên trên: không lặp thêm một dòng thông tin.
        JPanel names = new JPanel(new BorderLayout());
        names.setOpaque(false);
        names.add(selectedClassTitle, BorderLayout.CENTER);
        JPanel heading = new JPanel(new BorderLayout(11, 0));
        heading.setOpaque(false);
        heading.add(back, BorderLayout.WEST);
        heading.add(names, BorderLayout.CENTER);
        top.add(heading, BorderLayout.NORTH);

        JPanel summary = new JPanel(new GridLayout(1, 4, 9, 0));
        summary.setOpaque(false);
        String[] labels = {"Có mặt", "Vắng có phép", "Vắng không phép", "Chưa điểm danh"};
        Color[] colors = {new Color(226, 247, 230), new Color(226, 239, 255),
            new Color(255, 235, 233), new Color(242, 244, 247)};
        Color[] textColors = {new Color(0, 106, 54), new Color(13, 82, 171),
            new Color(180, 36, 32), new Color(78, 94, 105)};
        for (int i = 0; i < totals.length; i++) {
            RoundedPanel tile = new RoundedPanel(colors[i], new Color(219, 231, 225), 20);
            tile.setLayout(new BorderLayout(0, 4));
            tile.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));
            totals[i].setFont(totals[i].getFont().deriveFont(Font.BOLD, 23f));
            totals[i].setForeground(textColors[i]);
            tile.add(totals[i], BorderLayout.CENTER);
            JLabel label = new JLabel(labels[i], SwingConstants.CENTER);
            label.setForeground(textColors[i]);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
            tile.add(label, BorderLayout.SOUTH);
            summary.add(tile);
        }
        summary.setPreferredSize(new Dimension(640, 56));
        top.add(summary, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        studentsPanel.setLayout(new BoxLayout(studentsPanel, BoxLayout.Y_AXIS));
        studentsPanel.setBackground(Color.WHITE);
        studentsPanel.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane scroll = new JScrollPane(studentsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        // Năm hàng 54px là mức ưu tiên của vùng danh sách trên màn hình đủ cao.
        // BorderLayout phân phối phần chiều cao còn lại cho bảng, không phóng đại các thanh khác.
        scroll.setPreferredSize(new Dimension(940, 5 * 50 + 40));

        // Tiêu đề ở trong columnHeader để đồng bộ cuộn ngang với nội dung.
        JPanel tableHead = new JPanel(new GridBagLayout());
        tableHead.setBackground(new Color(221, 245, 227));
        addAttendanceCell(tableHead, headerLabel("STT"), 0, 0, 29);
        addAttendanceCell(tableHead, headerLabel("Họ và tên"), 1, .24, 185);
        addAttendanceCell(tableHead, headerLabel("Trạng thái điểm danh"), 2, .52, 485);
        addAttendanceCell(tableHead, headerLabel("Lý do / Ghi chú"), 3, .24, 174);
        tableHead.setPreferredSize(new Dimension(940, 40));
        scroll.setColumnHeaderView(tableHead);

        JPanel searchBar = new JPanel(new BorderLayout(12, 0));
        searchBar.setOpaque(false);
        searchBar.setBorder(BorderFactory.createEmptyBorder(0, 2, 3, 2));
        studentSearch.setFont(studentSearch.getFont().deriveFont(Font.PLAIN, 14f));
        studentSearch.setToolTipText("Tìm học sinh theo họ tên, không làm mất các lựa chọn chưa lưu");
        studentSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        studentSearch.putClientProperty("JTextField.placeholderText", "Tìm học sinh theo họ tên...");
        studentSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterStudents();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterStudents();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterStudents();
            }
        });
        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setOpaque(false);
        searchBox.setPreferredSize(new Dimension(330, 32));
        JLabel findLabel = new JLabel("Tìm học sinh:");
        findLabel.setFont(findLabel.getFont().deriveFont(Font.BOLD, 12f));
        findLabel.setForeground(new Color(35, 79, 53));
        searchBox.add(findLabel, BorderLayout.WEST);
        searchBox.add(studentSearch, BorderLayout.CENTER);
        searchBox.setPreferredSize(new Dimension(415, 32));
        searchBar.add(searchBox, BorderLayout.WEST);
        filterSummary.setFont(filterSummary.getFont().deriveFont(Font.BOLD, 12f));
        filterSummary.setForeground(new Color(55, 87, 68));
        searchBar.add(filterSummary, BorderLayout.EAST);

        JPanel table = new RoundedPanel(Color.WHITE, BORDER, 18);
        table.setLayout(new BorderLayout());
        table.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        table.add(scroll, BorderLayout.CENTER);
        JPanel listArea = new JPanel(new BorderLayout(0, 0));
        listArea.setOpaque(false);
        listArea.add(searchBar, BorderLayout.NORTH);
        listArea.add(table, BorderLayout.CENTER);
        page.add(listArea, BorderLayout.CENTER);

        JButton all = new ModernButton("✓  Tất cả có mặt (cả lớp)", Style.OUTLINE);
        JButton clear = new ModernButton("↻  Đặt lại điểm danh", Style.OUTLINE);
        JButton save = new ModernButton("Lưu điểm danh", Style.PRIMARY);
        JPanel bottom = new RoundedPanel(new Color(232, 249, 238), BORDER, 20);
        bottom.setLayout(new BorderLayout(6, 2));
        bottom.setBorder(BorderFactory.createEmptyBorder(7, 10, 6, 10));
        JPanel toolbar = new JPanel(new BorderLayout(6, 2));
        toolbar.setOpaque(false);
        all.setPreferredSize(new Dimension(208, 36));
        clear.setPreferredSize(new Dimension(178, 36));
        save.setPreferredSize(new Dimension(155, 36));
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        quick.add(all);
        quick.add(clear);
        quick.setOpaque(false);
        toolbar.add(quick, BorderLayout.WEST);
        toolbar.add(save, BorderLayout.EAST);
        bottom.add(toolbar, BorderLayout.NORTH);
        cbTaiKhoan.setPreferredSize(new Dimension(170, 27));
        lyDo.setPreferredSize(new Dimension(218, 27));
        lyDo.setToolTipText("Bắt buộc khi sửa điểm danh đã lưu trong SQL Server");
        JPanel accountRow = new JPanel(new BorderLayout(6, 0));
        JPanel accountFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accountFields.add(new JLabel("Tài khoản ghi nhận:"));
        accountFields.add(cbTaiKhoan);
        accountFields.add(new JLabel("Lý do sửa dữ liệu đã lưu:"));
        accountFields.add(lyDo);
        accountFields.setOpaque(false);
        accountRow.add(accountFields, BorderLayout.WEST);
        accountRow.setOpaque(false);
        status.setForeground(new Color(28, 92, 51));
        status.setFont(status.getFont().deriveFont(10f));
        status.setHorizontalAlignment(SwingConstants.RIGHT);
        status.setToolTipText("Trạng thái lưu và số học sinh của lớp");
        accountRow.add(status, BorderLayout.CENTER);
        bottom.add(accountRow, BorderLayout.CENTER);
        page.add(bottom, BorderLayout.SOUTH);
        all.setToolTipText("Đánh dấu Có mặt cho TOÀN BỘ học sinh trong lớp, kể cả những em đang ẩn bởi ô tìm kiếm");
        clear.setToolTipText("Đưa toàn bộ lựa chọn về Chưa điểm danh; chưa lưu thay đổi vào SQL");
        all.addActionListener(e -> {
            for (StudentLine line : studentLines) {
                line.select(AttendanceState.CO_MAT);
            }
            updateTotals();
        });
        clear.addActionListener(e -> {
            if (studentLines.isEmpty()) {
                return;
            }
            boolean changed = studentLines.stream().anyMatch(line
                    -> line.state() != AttendanceState.CHUA_DIEM_DANH || !line.note.getText().isBlank());
            if (changed && !MamNonUi.xacNhan(this,
                    "Đặt toàn bộ học sinh của lớp về Chưa điểm danh và xóa các ghi chú đang nhập?\n"
                    + "Thao tác này CHƯA lưu vào SQL Server.",
                    "Xác nhận đặt lại điểm danh", true)) {
                return;
            }
            for (StudentLine line : studentLines) {
                line.select(AttendanceState.CHUA_DIEM_DANH);
                line.note.setText("");
            }
            updateTotals();
        });
        save.addActionListener(e -> save());
        return page;
    }

    private static JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setForeground(new Color(17, 77, 43));
        return l;
    }

    /**
     * Cùng kích thước cột ở tiêu đề và các dòng học sinh.
     */
    private static void addAttendanceCell(JPanel row, JComponent component, int col, double weight, int width) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = col;
        c.gridy = 0;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(3, 8, 3, 5);
        component.setMinimumSize(new Dimension(width, 30));
        component.setPreferredSize(new Dimension(width, component instanceof JTextField ? 36 : 36));
        row.add(component, c);
    }

    /**
     * Màu nền/viền tự vẽ để không bị Look & Feel Windows làm mất chữ hoặc góc
     * bo.
     */
    private static final class RoundedPanel extends JPanel {

        private final Color fill, outline;
        private final int radius;
        private boolean hover;

        RoundedPanel(Color fill, Color outline, int radius) {
            this.fill = fill;
            this.outline = outline;
            this.radius = radius;
            setOpaque(false);
        }

        void setHovered(boolean value) {
            hover = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 2, h = getHeight() - 2;
            Color base = hover ? mix(fill, Color.WHITE, .08f) : fill;
            g2.setPaint(new GradientPaint(0, 0, base, 0, Math.max(1, h), mix(base, Color.WHITE, .18f)));
            g2.fillRoundRect(1, 1, w, h, radius, radius);
            g2.setColor(hover ? new Color(24, 139, 70) : outline);
            g2.setStroke(new BasicStroke(hover ? 1.5f : 1f));
            g2.drawRoundRect(1, 1, w, h, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static Color mix(Color a, Color b, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return new Color(Math.round(a.getRed() * (1f - t) + b.getRed() * t),
                Math.round(a.getGreen() * (1f - t) + b.getGreen() * t),
                Math.round(a.getBlue() * (1f - t) + b.getBlue() * t));
    }

    private enum Style {
        PRIMARY, OUTLINE
    }

    private static final class ModernButton extends JButton {

        final Style style;

        ModernButton(String label, Style style) {
            super(label);
            this.style = style;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(getFont().deriveFont(Font.BOLD, 13f));
            setForeground(style == Style.PRIMARY ? Color.WHITE : GREEN);
            setBorder(BorderFactory.createEmptyBorder(11, 19, 11, 19));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean pressed = getModel().isPressed();
            boolean hovered = getModel().isRollover();
            Color fill = !isEnabled() ? new Color(224, 233, 228)
                    : style == Style.PRIMARY ? (pressed ? new Color(0, 70, 35) : hovered ? new Color(0, 96, 47) : GREEN)
                            : (pressed ? new Color(207, 241, 218) : hovered ? new Color(232, 248, 238) : Color.WHITE);
            g2.setColor(fill);
            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
            g2.setColor(style == Style.PRIMARY ? fill : new Color(46, 149, 89));
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
            String text = getText();
            g2.setFont(getFont());
            g2.setColor(!isEnabled() ? new Color(72, 99, 82) : style == Style.PRIMARY ? Color.WHITE : new Color(0, 103, 52));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /**
     * JRadioButton thật, chỉ thay lớp vẽ để mỗi lựa chọn giống viên màu trong
     * mẫu duyệt.
     */
    private static final class StatusPill extends JRadioButton {

        final AttendanceState value;

        StatusPill(AttendanceState value) {
            super(value.label);
            this.value = value;
            setUI(new javax.swing.plaf.basic.BasicRadioButtonUI());
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorderPainted(false);
            setFont(getFont().deriveFont(Font.BOLD, 11.8f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(value.label);
            int width = switch (value) {
                case CO_MAT ->
                    88;
                case VANG_CO_PHEP ->
                    110;
                case VANG_KHONG_PHEP ->
                    132;
                case CHUA_DIEM_DANH ->
                    141;
            };
            setPreferredSize(new Dimension(width, 34));
            setMinimumSize(new Dimension(width, 34));
            setMaximumSize(new Dimension(width, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean selected = isSelected();
            Color base = stateBackground(value), accent = stateForeground(value);
            Color fill = selected ? base : getModel().isRollover() ? mix(base, Color.WHITE, .55f) : Color.WHITE;
            g2.setColor(fill);
            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
            g2.setColor(selected ? mix(accent, Color.WHITE, .52f) : new Color(225, 233, 230));
            g2.setStroke(new BasicStroke(selected ? 1.45f : 1f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
            int cy = getHeight() / 2;
            g2.setColor(selected ? accent : new Color(164, 179, 169));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(8, cy - 6, 12, 12);
            if (selected) {
                g2.fillOval(8, cy - 6, 12, 12);
                g2.setColor(Color.WHITE);
                g2.fillOval(12, cy - 2, 4, 4);
            }
            g2.setColor(accent);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(value.label, 25, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /**
     * Một học sinh: 4 radio cùng nhóm, ghi chú độc lập, không hiển thị mã học
     * sinh.
     */
    private final class StudentLine {

        final JPanel panel = new JPanel(new GridBagLayout());
        final Map<AttendanceState, StatusPill> radio = new EnumMap<>(AttendanceState.class);
        final JTextField note;
        final String studentName;
        final JLabel numberLabel;
        final AttendanceState originalState;
        final String originalNote;

        StudentLine(int number, DiemDanhMamNon student) {
            studentName = student.hoTen() == null ? "" : student.hoTen();
            originalState = AttendanceState.fromCode(student.trangThai());
            originalNote = student.ghiChu() == null ? "" : student.ghiChu();
            panel.setBackground(number % 2 == 0 ? new Color(249, 252, 250) : Color.WHITE);
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            numberLabel = new JLabel(Integer.toString(number), SwingConstants.CENTER);
            numberLabel.setFont(numberLabel.getFont().deriveFont(Font.BOLD, 13f));
            numberLabel.setForeground(new Color(34, 68, 48));
            addAttendanceCell(panel, numberLabel, 0, 0, 29);
            JLabel name = new JLabel(student.hoTen());
            name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
            name.setForeground(new Color(12, 67, 42));
            name.setToolTipText(student.hoTen());
            addAttendanceCell(panel, name, 1, .24, 185);

            JPanel choices = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
            choices.setOpaque(false);
            ButtonGroup group = new ButtonGroup();
            for (AttendanceState st : new AttendanceState[]{AttendanceState.CO_MAT,
                AttendanceState.VANG_CO_PHEP, AttendanceState.VANG_KHONG_PHEP,
                AttendanceState.CHUA_DIEM_DANH}) {
                StatusPill btn = new StatusPill(st);
                group.add(btn);
                choices.add(btn);
                radio.put(st, btn);
                btn.addActionListener(e -> updateTotals());
            }
            select(AttendanceState.fromCode(student.trangThai()));
            addAttendanceCell(panel, choices, 2, .52, 485);
            note = new JTextField(student.ghiChu() == null ? "" : student.ghiChu());
            note.setFont(note.getFont().deriveFont(Font.PLAIN, 12f));
            note.setToolTipText("Lý do vắng hoặc ghi chú cho buổi học");
            note.setBackground(Color.WHITE);
            note.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(7, 11, 7, 7)));
            addAttendanceCell(panel, note, 3, .24, 174);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.setMinimumSize(new Dimension(940, 50));
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            panel.setPreferredSize(new Dimension(940, 50));
        }

        boolean isDirty() {
            return state() != originalState || !note.getText().equals(originalNote);
        }

        AttendanceState state() {
            for (Map.Entry<AttendanceState, StatusPill> entry : radio.entrySet()) {
                if (entry.getValue().isSelected()) {
                    return entry.getKey();
                }
            }
            return AttendanceState.CHUA_DIEM_DANH;
        }

        void select(AttendanceState state) {
            StatusPill selected = radio.get(state);
            if (selected != null) {
                selected.setSelected(true);
            }
            for (StatusPill item : radio.values()) {
                item.repaint();
            }
        }
    }

    private static Color stateBackground(AttendanceState state) {
        if (state == AttendanceState.CO_MAT) {
            return new Color(223, 248, 230);
        }
        if (state == AttendanceState.VANG_CO_PHEP) {
            return new Color(224, 238, 255);
        }
        if (state == AttendanceState.VANG_KHONG_PHEP) {
            return new Color(255, 233, 231);
        }
        return new Color(241, 244, 247);
    }

    private static Color stateForeground(AttendanceState state) {
        if (state == AttendanceState.CO_MAT) {
            return new Color(0, 109, 54);
        }
        if (state == AttendanceState.VANG_CO_PHEP) {
            return new Color(16, 83, 173);
        }
        if (state == AttendanceState.VANG_KHONG_PHEP) {
            return new Color(176, 35, 32);
        }
        return new Color(77, 92, 105);
    }

    private void updateTotals() {
        int[] count = new int[4];
        for (StudentLine line : studentLines) {
            switch (line.state()) {
                case CO_MAT ->
                    count[0]++;
                case VANG_CO_PHEP ->
                    count[1]++;
                case VANG_KHONG_PHEP ->
                    count[2]++;
                case CHUA_DIEM_DANH ->
                    count[3]++;
            }
        }
        for (int i = 0; i < totals.length; i++) {
            totals[i].setText(Integer.toString(count[i]));
        }
    }

    /**
     * Chỉ lọc các dòng được VẼ, không làm mất trạng thái Radio/ghi chú đã chọn.
     */
    private void filterStudents() {
        if (studentsPanel == null) {
            return;
        }
        String term = java.text.Normalizer.normalize(studentSearch.getText().strip(),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(java.util.Locale.ROOT);
        studentsPanel.removeAll();
        int shown = 0;
        for (StudentLine line : studentLines) {
            String normalizedName = java.text.Normalizer.normalize(line.studentName,
                    java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                    .replace('đ', 'd').replace('Đ', 'D').toLowerCase(java.util.Locale.ROOT);
            if (normalizedName.contains(term)) {
                line.numberLabel.setText(Integer.toString(++shown));
                studentsPanel.add(line.panel);
            }
        }
        if (shown == 0) {
            JLabel blank = new JLabel(studentLines.isEmpty()
                    ? "Không có học sinh đang học lớp này trong ngày đã chọn."
                    : "Không tìm thấy học sinh phù hợp.", SwingConstants.CENTER);
            blank.setAlignmentX(Component.LEFT_ALIGNMENT);
            blank.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
            blank.setForeground(new Color(89, 106, 93));
            studentsPanel.add(blank);
        }
        filterSummary.setText("Hiển thị " + shown + "/" + studentLines.size() + " học sinh");
        studentsPanel.revalidate();
        studentsPanel.repaint();
    }

    private void renderStudents(List<DiemDanhMamNon> students) {
        studentLines.clear();
        for (int i = 0; i < students.size(); i++) {
            studentLines.add(new StudentLine(i + 1, students.get(i)));
        }
        updateTotals();
        filterStudents();
    }

    private boolean hasUnsavedChanges() {
        if (selectedClass == null || studentLines.isEmpty()) {
            return false;
        }
        if (!lyDo.getText().isBlank()) {
            return true;
        }
        for (StudentLine line : studentLines) {
            if (line.isDirty()) {
                return true;
            }
        }
        return false;
    }

    private boolean confirmDiscard(String action) {
        if (!hasUnsavedChanges()) {
            return true;
        }
        return MamNonUi.xacNhan(this,
                "Bạn có thay đổi điểm danh hoặc ghi chú chưa lưu.\n"
                + "Bạn muốn " + action + " và bỏ các thay đổi chưa lưu?",
                "Điểm danh chưa lưu", true);
    }

    // Bảng màu không trùng nhau cho các lớp; lớp thứ tư trở đi dùng màu mới.
    private static final Color[][] CLASS_PALETTE = {
        {new Color(229, 249, 231), new Color(13, 143, 51), new Color(183, 226, 190)},
        {new Color(255, 248, 220), new Color(178, 121, 0), new Color(247, 221, 141)},
        {new Color(230, 241, 255), new Color(12, 91, 186), new Color(179, 208, 245)},
        {new Color(244, 234, 255), new Color(113, 69, 176), new Color(216, 196, 240)},
        {new Color(255, 235, 242), new Color(174, 65, 116), new Color(247, 194, 217)},
        {new Color(226, 249, 245), new Color(8, 120, 116), new Color(169, 221, 216)},
        {new Color(255, 238, 222), new Color(172, 90, 21), new Color(245, 205, 174)},
        {new Color(238, 243, 220), new Color(105, 122, 29), new Color(214, 223, 172)}
    };

    private JComponent makeClassCard(LopMamNon lop, int index) {
        Color[] pal = index < CLASS_PALETTE.length ? CLASS_PALETTE[index]
                : paletteForAdditionalClass(index);
        Color bg = pal[0], accent = pal[1], line = pal[2];
        RoundedPanel card = new RoundedPanel(bg, line, 20);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(15, 17, 14, 17));
        JLabel name = new JLabel(lop.tenLop());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 21f));
        name.setForeground(accent.darker());
        name.setToolTipText(lop.tenLop());
        JLabel state = new JLabel(lop.trangThai() ? "Đang hoạt động" : "Ngừng hoạt động");
        state.setFont(state.getFont().deriveFont(Font.BOLD, 12f));
        state.setForeground(lop.trangThai() ? accent.darker() : new Color(103, 91, 73));
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 5));
        info.setOpaque(false);
        info.add(name);
        info.add(state);
        card.add(info, BorderLayout.CENTER);

        JButton view = new JButton("Xem học sinh   →") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color shade = getModel().isPressed() ? accent.darker() : getModel().isRollover() ? mix(accent, Color.BLACK, .13f) : accent;
                g2.setColor(shade);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 13, 13);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        view.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        view.setOpaque(false);
        view.setContentAreaFilled(false);
        view.setBorderPainted(false);
        view.setForeground(Color.WHITE);
        view.setFocusPainted(false);
        view.setRolloverEnabled(true);
        view.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        view.setFont(view.getFont().deriveFont(Font.BOLD, 13f));
        view.setPreferredSize(new Dimension(100, 42));
        view.addActionListener(e -> openClass(lop));
        card.add(view, BorderLayout.SOUTH);
        MouseAdapter interaction = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setHovered(false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                openClass(lop);
            }
        };
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(interaction);
        info.addMouseListener(interaction);
        name.addMouseListener(interaction);
        state.addMouseListener(interaction);
        return card;
    }

    private static Color[] paletteForAdditionalClass(int index) {
        // Bước nhảy sắc độ cố định giúp các thẻ mới không lặp màu theo chu kỳ.
        float hue = (float) ((.14 + index * .6180339887) % 1.0);
        Color accent = Color.getHSBColor(hue, .75f, .67f);
        return new Color[]{mix(accent, Color.WHITE, .88f), accent, mix(accent, Color.WHITE, .70f)};
    }

    private void renderClassCards() {
        classCards.removeAll();
        if (availableClasses.isEmpty()) {
            JLabel empty = new JLabel("Năm học này chưa có lớp Mầm non.", SwingConstants.CENTER);
            empty.setForeground(new Color(90, 100, 90));
            classCards.add(empty);
        } else {
            for (int i = 0; i < availableClasses.size(); i++) {
                classCards.add(makeClassCard(availableClasses.get(i), i));
            }
        }
        classSummary.setText(availableClasses.size() + " lớp");
        layoutClassGrid();
        if (classesScroll != null) {
            classesScroll.getViewport().setViewPosition(new Point(0, 0));
        }
    }

    private void layoutClassGrid() {
        if (classesScroll == null) {
            return;
        }
        int viewportWidth = classesScroll.getViewport().getWidth();
        if (viewportWidth <= 0) {
            viewportWidth = 1050;
        }
        int columns = viewportWidth >= 700 ? 3 : viewportWidth >= 480 ? 2 : 1;
        int count = Math.max(1, classCards.getComponentCount());
        int rows = (count + columns - 1) / columns;
        classCards.setLayout(new GridLayout(0, columns, 15, 15));
        int height = 14 + rows * 160 + (rows - 1) * 15;
        Dimension size = new Dimension(viewportWidth, height);
        if (!size.equals(classCards.getPreferredSize())) {
            classCards.setPreferredSize(size);
        }
        classCards.revalidate();
        classCards.repaint();
    }

    private void openClass(LopMamNon lop) {
        if (selectedClass != null && !confirmDiscard("chuyển sang lớp khác")) {
            return;
        }
        selectedClass = lop;
        studentSearch.setText("");
        lyDo.setText("");
        records = List.of();
        renderStudents(List.of());
        selectedClassTitle.setText("Điểm danh – " + lop.tenLop());
        detailSubTitle.setText("Năm học: " + ((NamHocMamNon) cbYear.getSelectedItem()).tenNamHoc() + "   |   Ngày: " + ngay.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        attendanceLayout.show(attendanceViews, "students");
        loadDay();
    }

    private void showClasses() {
        selectedClass = null;
        lyDo.setText("");
        studentSearch.setText("");
        dayRequest++;
        records = List.of();
        renderStudents(List.of());
        attendanceLayout.show(attendanceViews, "classes");
    }

    private JComponent createHistory() {
        JPanel page = new JPanel(new BorderLayout(9, 9));
        JButton search = MamNonUi.nut("Tra cứu", true);
        JButton log = MamNonUi.nut("Lịch sử sửa dòng chọn", false);
        JPanel top = MamNonUi.dong(new JLabel("Lớp:"), histLop, new JLabel("Học sinh:"), histHocSinh,
                new JLabel("Từ:"), histTu, new JLabel("Đến:"), histDen, search, log);
        page.add(top, BorderLayout.NORTH);
        page.add(new JScrollPane(histTable), BorderLayout.CENTER);
        JLabel info = new JLabel("Tra cứu lịch sử theo ngày / lớp / học sinh; mở nhật ký để xem các lần sửa điểm danh.");
        page.add(info, BorderLayout.SOUTH);
        search.addActionListener(e -> loadHistory());
        log.addActionListener(e -> showLog());
        return page;
    }

    private void loadChoices() {
        loadYears(true);
        MamNonUi.tai(this, ctrl::taiKhoan, list -> {
            cbTaiKhoan.removeAllItems();
            cbTaiKhoan.addItem(null);
            for (LuaChon a : list) {
                cbTaiKhoan.addItem(a);
            }
            if (list.isEmpty()) {
                status.setText("Chưa có tài khoản đang hoạt động ở cơ sở Mầm non.");
            }
        });
        MamNonUi.tai(this, () -> ctrl.hocSinh(""), list -> {
            histHocSinh.removeAllItems();
            histHocSinh.addItem(new LuaChon(0, "Tất cả học sinh"));
            for (LuaChon x : list) {
                histHocSinh.addItem(x);
            }
        });
    }

    private void loadYears(boolean useDefault) {
        if (!confirmDiscard("cập nhật năm học")) {
            return;
        }
        NamHocMamNon previous = (NamHocMamNon) cbYear.getSelectedItem();
        MamNonUi.tai(this, ctrl::namHoc, years -> {
            loadingYears = true;
            cbYear.removeAllItems();
            for (NamHocMamNon y : years) {
                cbYear.addItem(y);
            }
            NamHocMamNon wanted = useDefault ? NamHocMacDinh.chonHieuLuc(years) : previous;
            if (wanted != null) {
                for (int i = 0; i < cbYear.getItemCount(); i++) {
                    if (cbYear.getItemAt(i).maNamHoc() == wanted.maNamHoc()) {
                        cbYear.setSelectedIndex(i);
                        break;
                    }
                }
            }
            loadingYears = false;
            loadClasses();
        });
    }

    private void loadClasses() {
        final NamHocMamNon selected = (NamHocMamNon) cbYear.getSelectedItem();
        displayedYearId = selected == null ? null : selected.maNamHoc();
        int request = ++classesRequest;
        showClasses();
        availableClasses = List.of();
        if (selected == null) {
            histLop.removeAllItems();
            renderClassCards();
            return;
        }
        try {
            ngay.setDateRange(selected.tuNgay(), selected.denNgay());
        } catch (Exception ex) {
            MamNonUi.loi(this, ex);
        }
        classSummary.setText("Đang tải lớp của năm học " + selected.tenNamHoc() + "...");
        MamNonUi.tai(this, () -> ctrl.lop(selected.maNamHoc(), ""), classes -> {
            NamHocMamNon current = (NamHocMamNon) cbYear.getSelectedItem();
            if (request != classesRequest || current == null || current.maNamHoc() != selected.maNamHoc()) {
                return;
            }
            availableClasses = classes;
            histLop.removeAllItems();
            histLop.addItem(null);
            for (LopMamNon l : classes) {
                histLop.addItem(l);
            }
            renderClassCards();
        });
    }

    private void loadDay() {
        final LopMamNon lop = selectedClass;
        if (lop == null) {
            records = List.of();
            renderStudents(List.of());
            return;
        }
        try {
            final LocalDate day = ngay.getDate();
            final int request = ++dayRequest;
            status.setText("Đang tải điểm danh lớp " + lop.tenLop() + "...");
            MamNonUi.tai(this, () -> ctrl.bangDiemDanh(lop.maLop(), day), rows -> {
                if (request != dayRequest || selectedClass == null || selectedClass.maLop() != lop.maLop()) {
                    return;
                }
                if (!java.util.Objects.equals(ngay.getDate(), day)) {
                    return;
                }
                records = rows;
                lyDo.setText("");
                renderStudents(rows);
                selectedClassTitle.setText("Điểm danh – " + lop.tenLop());
                NamHocMamNon currentYear = (NamHocMamNon) cbYear.getSelectedItem();
                detailSubTitle.setText("Năm học: " + (currentYear == null ? "—" : currentYear.tenNamHoc()) + "   |   Ngày: " + day.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                status.setText("Sĩ số: " + rows.size() + " học sinh · Ngày " + day.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
            });
        } catch (Exception ex) {
            MamNonUi.loi(this, ex);
        }
    }

    private void save() {
        LopMamNon lop = selectedClass;
        LuaChon account = (LuaChon) cbTaiKhoan.getSelectedItem();
        if (lop == null || account == null) {
            JOptionPane.showMessageDialog(this, "Chọn lớp và tài khoản điểm danh hợp lệ trước khi lưu.");
            return;
        }
        try {
            LocalDate day = ngay.getDate();
            List<ThayDoiDiemDanh> changes = new ArrayList<>();
            if (studentLines.size() != records.size()) {
                JOptionPane.showMessageDialog(this, "Danh sách đã thay đổi, hãy tải lại.");
                return;
            }
            boolean modifiesSavedAttendance = false;
            for (int i = 0; i < records.size(); i++) {
                DiemDanhMamNon x = records.get(i);
                StudentLine line = studentLines.get(i);
                String code = line.state().name();
                String note = line.note.getText();
                if (x.maDiemDanh() > 0 && line.isDirty()) {
                    modifiesSavedAttendance = true;
                }
                changes.add(new ThayDoiDiemDanh(x.maHocVien(), x.maXepLop(), code, note));
            }
            if (modifiesSavedAttendance && lyDo.getText().isBlank()) {
                JOptionPane.showMessageDialog(this,
                        "Bạn đang sửa điểm danh đã lưu. Vui lòng nhập lý do sửa dữ liệu đã lưu để ghi nhật ký.",
                        "Thiếu lý do chỉnh sửa", JOptionPane.WARNING_MESSAGE);
                lyDo.requestFocusInWindow();
                return;
            }
            if (changes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Chưa có học sinh trong lớp vào ngày này.");
                return;
            }
            // Chưa có phiên đăng nhập: yêu cầu người thao tác tự chọn tài khoản hợp lệ.
            if (!MamNonUi.xacNhan(this,
                    "Lưu điểm danh ngày " + day + " cho lớp '" + lop.tenLop() + "' bằng tài khoản '"
                    + account.ten() + "'?\nChỉ sử dụng khi bạn được phép dùng tài khoản này.",
                    "Xác nhận điểm danh", false)) {
                return;
            }
            MamNonUi.ghi(this, () -> {
                int count = ctrl.luuDiemDanh(lop.maLop(), day, account.id(), lyDo.getText(), changes);
                SwingUtilities.invokeLater(() -> status.setText("Đã lưu " + count + " bản ghi thay đổi."));
            }, () -> {
                lyDo.setText("");
                loadDay();
            });
        } catch (Exception ex) {
            MamNonUi.loi(this, ex);
        }
    }

    private void loadHistory() {
        try {
            LocalDate from = histTu.getDate(), to = histDen.getDate();
            if (from.isAfter(to)) {
                JOptionPane.showMessageDialog(this, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
                return;
            }
            LopMamNon lop = (LopMamNon) histLop.getSelectedItem();
            LuaChon hs = (LuaChon) histHocSinh.getSelectedItem();
            Integer classId = lop == null ? null : lop.maLop();
            Integer studentId = hs == null || hs.id() == 0 ? null : hs.id();
            MamNonUi.tai(this, () -> ctrl.lichSu(classId, studentId, from, to), list -> {
                history = list;
                histModel.setRowCount(0);
                for (LichSuDiemDanh x : list) {
                    histModel.addRow(new Object[]{
                        x.ngayHoc(), x.hoTen(), x.tenLop(), x.trangThai(),
                        x.ghiChu(), x.nguoiGhi(), x.ngayCapNhat()
                    });
                }
            });
        } catch (Exception ex) {
            MamNonUi.loi(this, ex);
        }
    }

    private void showLog() {
        int i = histTable.getSelectedRow();
        if (i < 0) {
            JOptionPane.showMessageDialog(this, "Chọn bản ghi trong lịch sử điểm danh.");
            return;
        }
        LichSuDiemDanh row = history.get(histTable.convertRowIndexToModel(i));
        MamNonUi.tai(this, () -> ctrl.nhatKy(row.maHocVien(), row.ngayHoc()), list -> {
            DefaultTableModel log = MamNonUi.model("Thời gian (UTC)", "Trước", "Sau", "Lý do", "Tài khoản");
            for (NhatKySua x : list) {
                log.addRow(new Object[]{
                    x.ngaySua(), x.truoc(), x.sau(), x.lyDo(), x.taiKhoan()
                });
            }
            JScrollPane pane = new JScrollPane(MamNonUi.bang(log));
            pane.setPreferredSize(new Dimension(780, 300));
            JOptionPane.showMessageDialog(this, pane, "Lịch sử sửa điểm danh", JOptionPane.PLAIN_MESSAGE);
        });
    }
}
