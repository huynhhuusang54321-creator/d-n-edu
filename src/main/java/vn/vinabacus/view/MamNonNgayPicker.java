package vn.vinabacus.view;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;

/** Bộ chọn ngày dạng lịch cho TV4. Lưu LocalDate, không nhập ngày thủ công. */
public final class MamNonNgayPicker extends JPanel {
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color GREEN = MamNonUi.XANH;
    private static final Color LINE = new Color(204, 224, 212);
    private final JTextField display = new JTextField(11);
    private final boolean optional;
    private LocalDate date;
    private LocalDate minDate, maxDate;

    public MamNonNgayPicker(LocalDate initial) { this(initial, false); }

    public MamNonNgayPicker(LocalDate initial, boolean optional) {
        super(new BorderLayout(0, 0));
        this.optional = optional;
        if (initial == null && !optional) initial = LocalDate.now();
        date = initial;
        setOpaque(false);
        display.setEditable(false);
        display.setFocusable(false);
        display.setBackground(Color.WHITE);
        display.setForeground(new Color(20, 65, 37));
        display.setFont(display.getFont().deriveFont(Font.BOLD, 13f));
        display.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(6, 9, 6, 5)));
        display.setText(date == null ? "Chưa chọn" : LABEL.format(date));
        JButton select = MamNonUi.nut("▦ Chọn ngày", false);
        select.setToolTipText("Mở lịch để chọn ngày");
        select.setMargin(new Insets(5, 8, 5, 8));
        select.addActionListener(e -> showCalendar());
        add(display, BorderLayout.CENTER);
        add(select, BorderLayout.EAST);
        if (optional) {
            JButton clear = MamNonUi.nut("×", false);
            clear.setToolTipText("Không giới hạn ngày");
            clear.setMargin(new Insets(4, 6, 4, 6));
            clear.addActionListener(e -> setDate(null));
            add(clear, BorderLayout.WEST);
        }
    }

    public LocalDate getDate() { return date; }

    public void setDate(LocalDate next) {
        if (!optional && next == null) throw new IllegalArgumentException("Ngày không được để trống");
        if (next != null && (minDate != null && next.isBefore(minDate) || maxDate != null && next.isAfter(maxDate)))
            throw new IllegalArgumentException("Ngày được chọn nằm ngoài khoảng cho phép");
        LocalDate old = date;
        date = next;
        display.setText(next == null ? "Chưa chọn" : LABEL.format(next));
        if (!java.util.Objects.equals(old, next)) firePropertyChange("date", old, next);
    }

    /** Giới hạn ngày (bao gồm cả hai đầu); dùng khi lọc theo năm học. */
    public void setDateRange(LocalDate min, LocalDate max) {
        if (min != null && max != null && min.isAfter(max)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        minDate = min; maxDate = max;
        if (date != null && (min != null && date.isBefore(min) || max != null && date.isAfter(max))) {
            setDate(date.isBefore(min != null ? min : date) ? min : max);
        }
    }

    private void showCalendar() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Chọn ngày", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final YearMonth[] current = {YearMonth.from(date != null ? date : minDate != null ? minDate : LocalDate.now())};
        JPanel root = new JPanel(new BorderLayout(8, 9));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 14));
        JPanel header = new JPanel(new BorderLayout(7, 0));
        header.setOpaque(false);
        JButton prev = MamNonUi.nut("‹", false), next = MamNonUi.nut("›", false);
        JLabel title = new JLabel("", SwingConstants.CENTER);
        title.setForeground(GREEN); title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        header.add(prev, BorderLayout.WEST); header.add(title, BorderLayout.CENTER); header.add(next, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(7, 7, 4, 4));
        grid.setOpaque(false);
        root.add(grid, BorderLayout.CENTER);
        Runnable render = () -> {
            YearMonth month = current[0];
            title.setText("Tháng " + month.getMonthValue() + "/" + month.getYear());
            grid.removeAll();
            for (String name : new String[]{"T2", "T3", "T4", "T5", "T6", "T7", "CN"}) {
                JLabel d = new JLabel(name, SwingConstants.CENTER);
                d.setForeground(GREEN); d.setFont(d.getFont().deriveFont(Font.BOLD, 12f));
                grid.add(d);
            }
            int startOffset = month.atDay(1).getDayOfWeek().getValue() - 1;
            for (int pos = 0; pos < 42; pos++) {
                int dayNumber = pos - startOffset + 1;
                if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                    grid.add(new JLabel(""));
                    continue;
                }
                LocalDate candidate = month.atDay(dayNumber);
                // Tự vẽ số ngày để Windows Look & Feel không thể làm mất số khi chọn/nhấn.
                final boolean selected = candidate.equals(date);
                final boolean todayDay = candidate.equals(LocalDate.now());
                final boolean allowed = (minDate == null || !candidate.isBefore(minDate))
                    && (maxDate == null || !candidate.isAfter(maxDate));
                JButton day = new JButton(Integer.toString(dayNumber)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        try {
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                            boolean hovered = getModel().isRollover();
                            Color fill = selected ? GREEN
                                : !allowed ? new Color(243, 245, 243)
                                : hovered ? new Color(224, 246, 230)
                                : todayDay ? new Color(235, 249, 238) : Color.WHITE;
                            g2.setColor(fill);
                            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 7, 7);
                            g2.setColor(selected ? GREEN : todayDay ? new Color(31, 146, 75) : LINE);
                            g2.setStroke(new BasicStroke(selected ? 2f : 1f));
                            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 7, 7);
                            g2.setFont(getFont());
                            g2.setColor(selected ? Color.WHITE : !allowed ? new Color(106, 117, 108)
                                : new Color(23, 66, 40));
                            String number = getText();
                            FontMetrics fm = g2.getFontMetrics();
                            g2.drawString(number, (getWidth() - fm.stringWidth(number)) / 2,
                                (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                        } finally { g2.dispose(); }
                    }
                };
                day.setUI(new javax.swing.plaf.basic.BasicButtonUI());
                day.setFont(day.getFont().deriveFont(Font.BOLD, 13f));
                day.setFocusPainted(false);
                day.setOpaque(false);
                day.setContentAreaFilled(false);
                day.setBorderPainted(false);
                day.setRolloverEnabled(true);
                day.setForeground(selected ? Color.WHITE : new Color(23, 66, 40));
                day.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                day.setEnabled(allowed);
                day.getAccessibleContext().setAccessibleName("Ngày " + dayNumber + "/" + month.getMonthValue() + "/" + month.getYear());
                day.addActionListener(e -> { setDate(candidate); dialog.dispose(); });
                grid.add(day);
            }
            grid.revalidate(); grid.repaint();
        };
        prev.addActionListener(e -> { current[0] = current[0].minusMonths(1); render.run(); });
        next.addActionListener(e -> { current[0] = current[0].plusMonths(1); render.run(); });
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        footer.setOpaque(false);
        JButton today = MamNonUi.nut("Hôm nay", false);
        boolean todayAllowed = (minDate == null || !LocalDate.now().isBefore(minDate)) && (maxDate == null || !LocalDate.now().isAfter(maxDate));
        today.setEnabled(todayAllowed);
        today.addActionListener(e -> { setDate(LocalDate.now()); dialog.dispose(); });
        JButton cancel = MamNonUi.nut("Đóng", false);
        cancel.addActionListener(e -> dialog.dispose());
        footer.add(today); footer.add(cancel);
        root.add(footer, BorderLayout.SOUTH);
        render.run();
        dialog.setContentPane(root);
        dialog.setSize(370, 380);
        dialog.setMinimumSize(new Dimension(350, 370));
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setVisible(true);
    }
}
