package vn.vinabacus.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Bộ thành phần UI riêng TV5. Không thay đổi giao diện các module của thành viên khác. */
public final class TaiChinhUi {
    public static final Color XANH = new Color(0, 101, 49);
    public static final Color XANH_DAM = new Color(0, 78, 38);
    public static final Color NEN = new Color(248, 250, 249);
    public static final Color VIEN = new Color(219, 231, 224);
    public static final Color CHU = new Color(29, 51, 39);
    public static final Color CHU_MO = new Color(101, 117, 107);
    private TaiChinhUi() {}
    @FunctionalInterface public interface Task<T> { T call() throws Exception; }

    public static <T> void tai(Component parent, Task<T> task, Consumer<T> success) {
        tai(parent, task, success, () -> {});
    }
    public static <T> void tai(Component parent, Task<T> task, Consumer<T> success,
                               Runnable completed) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                try { success.accept(get()); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); loi(parent, ex); }
                catch (ExecutionException ex) { loi(parent, ex.getCause() == null ? ex : ex.getCause()); }
                catch (Exception ex) { loi(parent, ex); }
                finally { completed.run(); }
            }
        }.execute();
    }
    public static void ghi(Component parent, Task<?> task, Runnable success) {
        ghi(parent, task, success, () -> {});
    }
    public static void ghi(Component parent, Task<?> task, Runnable success,
                           Runnable completed) {
        tai(parent, task, ignored -> {
            success.run();
            JOptionPane.showMessageDialog(parent, "Đã lưu thành công.", "VINABACUS – Tài chính", JOptionPane.INFORMATION_MESSAGE);
        }, completed);
    }
    public static void loi(Component parent, Throwable ex) {
        JOptionPane.showMessageDialog(parent, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "VINABACUS – Thông báo", JOptionPane.ERROR_MESSAGE);
    }
    public static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    /** Nút bo góc: trạng thái thường, rê chuột, bấm và vô hiệu hóa đều dễ đọc. */
    public static JButton nut(String label, boolean primary) {
        JButton button = new JButton(label) {
            @Override protected void paintComponent(Graphics gg) {
                Graphics2D g = (Graphics2D) gg.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = primary || getModel().isSelected();
                Color bg;
                if (!isEnabled()) bg = new Color(231, 235, 233);
                else if (getModel().isPressed()) bg = active ? new Color(0, 67, 32) : new Color(213, 235, 221);
                else if (getModel().isRollover()) bg = active ? new Color(0, 126, 62) : new Color(232, 247, 237);
                else bg = active ? XANH : Color.WHITE;
                g.setColor(bg);
                g.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                g.setColor(isEnabled() ? (active ? XANH : new Color(195, 219, 204)) : new Color(220, 224, 221));
                g.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12);
                g.dispose();
                super.paintComponent(gg);
            }
        };
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
            @Override protected void paintText(Graphics g,AbstractButton b,Rectangle textRect,String text){
                g.setFont(b.getFont());
                g.setColor(b.isEnabled()?b.getForeground():new Color(89,103,95));
                FontMetrics metrics=g.getFontMetrics();
                g.drawString(text,textRect.x,textRect.y+metrics.getAscent());
            }
        });
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setFocusPainted(false);
        button.setForeground(primary ? Color.WHITE : XANH_DAM);
        button.setFont(font(Font.BOLD, 13));
        button.setBorder(new EmptyBorder(10, 14, 10, 14));
        button.setPreferredSize(new Dimension(Math.max(114, button.getPreferredSize().width + 16), 42));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }
    public static JTable bang(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint()), col = columnAtPoint(e.getPoint());
                if (row < 0 || col < 0) return null;
                Object value = getValueAt(row, col);
                return value == null ? null : String.valueOf(value);
            }
        };
        table.setRowHeight(39);
        table.setFont(font(Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(236, 241, 237));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setSelectionBackground(new Color(226, 245, 232));
        table.setSelectionForeground(CHU);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 41));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                setOpaque(true);
                setBackground(new Color(229, 244, 234));
                setForeground(XANH_DAM);
                setFont(font(Font.BOLD, 12));
                setBorder(new EmptyBorder(0, 8, 0, 6));
                return this;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                setBackground(selected ? new Color(224, 245, 231) : (row % 2 == 0 ? Color.WHITE : new Color(249, 251, 250)));
                setForeground(CHU);
                setBorder(new EmptyBorder(0, 8, 0, 6));
                setFont(font(Font.PLAIN, 13));
                return this;
            }
        });
        return table;
    }
    public static JScrollPane scroll(JTable table) {
        JScrollPane p = new JScrollPane(table);
        p.setBorder(BorderFactory.createLineBorder(VIEN));
        p.getViewport().setBackground(Color.WHITE);
        p.getVerticalScrollBar().setUnitIncrement(16);
        return p;
    }
    public static JPanel hang(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 6));
        p.setOpaque(false);
        for (Component c : components) p.add(c);
        return p;
    }
    public static JLabel title(String text) {
        JLabel t = new JLabel(text);
        t.setForeground(XANH_DAM);
        t.setFont(font(Font.BOLD, 22));
        return t;
    }
    public static JPanel card(Color background, Color border, int radius) {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics gg) {
                Graphics2D g = (Graphics2D) gg.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(background);
                g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
                g.setColor(border);
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
                g.dispose();
                super.paintComponent(gg);
            }
        };
        panel.setOpaque(false);
        return panel;
    }
    public static void field(JComponent c) {
        c.setFont(font(Font.PLAIN, 13));
        c.setPreferredSize(new Dimension(Math.max(150, c.getPreferredSize().width), 36));
        if (c instanceof JTextField) c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(197, 214, 203)), new EmptyBorder(5, 10, 5, 10)));
        if (c instanceof JComboBox<?>) c.setBackground(Color.WHITE);
    }
    /** Mẫu biểu có nhãn ở trên ô nhập, không cắt chữ ở cửa sổ 1366×768. */
    public static JPanel form(Object... pairs) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 6, 8, 6));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        for (int i=0; i<pairs.length; i+=2) {
            JLabel label = new JLabel(String.valueOf(pairs[i]));
            label.setFont(font(Font.BOLD, 13)); label.setForeground(CHU);
            c.gridy = i; c.insets = new Insets(7, 2, 4, 2);
            p.add(label, c);
            Component input = (Component)pairs[i+1];
            if (input instanceof JComponent jc && !(input instanceof JPanel)) field(jc);
            c.gridy = i+1; c.insets = new Insets(0, 2, 7, 2);
            p.add(input, c);
        }
        return p;
    }
    /** Hai cột nhãn/ô nhập, giảm chiều cao hộp thoại và không cắt tên trường. */
    public static JPanel formHaiCot(Object... pairs) {
        if(pairs.length%2!=0)throw new IllegalArgumentException("Thiếu ô nhập cho nhãn.");
        JPanel grid=new JPanel(new GridLayout(0,2,12,11));grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(3,2,3,2));
        for(int i=0;i<pairs.length;i+=2){
            JPanel cell=new JPanel(new BorderLayout(0,5));cell.setOpaque(false);
            JLabel title=new JLabel(String.valueOf(pairs[i]));
            title.setFont(font(Font.BOLD,12));title.setForeground(CHU);
            Component field=(Component)pairs[i+1];
            if(field instanceof JComponent jc && !(field instanceof JPanel) && !(field instanceof JLabel))field(jc);
            cell.add(title,BorderLayout.NORTH);cell.add(field,BorderLayout.CENTER);
            grid.add(cell);
        }
        if((pairs.length/2)%2==1){JPanel empty=new JPanel();empty.setOpaque(false);grid.add(empty);}
        return grid;
    }
    public static JPanel nhom(String title,JComponent content) {
        JPanel outer=new JPanel(new BorderLayout(0,9));outer.setOpaque(true);outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VIEN,1,true),new EmptyBorder(12,15,13,15)));
        JLabel label=new JLabel(title);label.setFont(font(Font.BOLD,14));label.setForeground(XANH_DAM);
        outer.add(label,BorderLayout.NORTH);outer.add(content,BorderLayout.CENTER);
        return outer;
    }
    public static JPanel chuThich(String text,Color background) {
        JPanel panel=new JPanel(new BorderLayout());panel.setBackground(background);
        panel.setBorder(new EmptyBorder(9,12,9,12));
        JTextArea label=new JTextArea(text);label.setOpaque(false);label.setEditable(false);
        label.setLineWrap(true);label.setWrapStyleWord(true);
        label.setFont(font(Font.PLAIN,12));label.setForeground(CHU);
        panel.add(label,BorderLayout.CENTER);return panel;
    }
    public static JPanel theTien(String[] labels,String[] values,Color[] foregrounds) {
        JPanel grid=new JPanel(new GridLayout(1,labels.length,10,0));grid.setOpaque(false);
        for(int i=0;i<labels.length;i++){
            JPanel card=card(new Color(242,250,245),VIEN,13);
            card.setLayout(new BorderLayout(0,5));card.setBorder(new EmptyBorder(11,13,12,13));
            JLabel title=new JLabel(labels[i]);title.setFont(font(Font.PLAIN,12));title.setForeground(CHU_MO);
            JLabel value=new JLabel(values[i]);value.setFont(font(Font.BOLD,17));value.setForeground(foregrounds[i]);
            card.add(title,BorderLayout.NORTH);card.add(value,BorderLayout.CENTER);grid.add(card);
        }
        return grid;
    }
    public static JPanel thongTinHaiCot(String... pairs) {
        if(pairs.length%2!=0)throw new IllegalArgumentException("Thiếu giá trị thông tin.");
        JPanel grid=new JPanel(new GridLayout(0,2,12,9));grid.setOpaque(false);
        for(int i=0;i<pairs.length;i+=2){
            JPanel cell=new JPanel(new BorderLayout(0,4));cell.setOpaque(false);
            JLabel title=new JLabel(pairs[i]);title.setFont(font(Font.PLAIN,12));title.setForeground(CHU_MO);
            JTextArea value=new JTextArea(pairs[i+1]==null?"—":pairs[i+1]);
            value.setFont(font(Font.BOLD,13));value.setForeground(CHU);value.setOpaque(false);
            value.setEditable(false);value.setLineWrap(true);value.setWrapStyleWord(true);
            value.setRows(pairs[i+1]!=null&&pairs[i+1].length()>45?2:1);
            cell.add(title,BorderLayout.NORTH);cell.add(value,BorderLayout.CENTER);grid.add(cell);
        }
        if((pairs.length/2)%2==1){JPanel empty=new JPanel();empty.setOpaque(false);grid.add(empty);}
        return grid;
    }
    public static JPanel doc(JComponent... sections) {
        JPanel root=new JPanel();root.setLayout(new BoxLayout(root,BoxLayout.Y_AXIS));
        root.setOpaque(false);root.setBorder(new EmptyBorder(3,2,3,2));
        for(int i=0;i<sections.length;i++){
            sections[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            root.add(sections[i]);if(i<sections.length-1)root.add(Box.createVerticalStrut(10));
        }
        return root;
    }
    /** Hộp thoại có kiểm tra ngay tại nút Lưu, lỗi không làm mất dữ liệu đã nhập. */
    public static boolean hopThoaiForm(Component parent, String title, String subtitle,
                                        JComponent form, String action, Runnable validate) {
        return hopThoaiForm(parent, title, subtitle, form, action, validate, true);
    }

    /** Căn hộp thoại trong vùng màn hình khả dụng; nội dung cuộn, thanh nút cố định. */
    private static boolean hopThoaiForm(Component parent, String title, String subtitle,
                                        JComponent form, String action, Runnable validate,
                                        boolean centerOnMonitor) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Color.WHITE);
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setBackground(XANH);
        heading.setBorder(new EmptyBorder(17, 22, 17, 22));
        JLabel name = new JLabel(title); name.setForeground(Color.WHITE); name.setFont(font(Font.BOLD, 19));
        JLabel description = new JLabel(subtitle == null ? "" : subtitle);
        description.setForeground(new Color(218, 247, 227)); description.setFont(font(Font.PLAIN, 12));
        heading.add(name, BorderLayout.NORTH); heading.add(description, BorderLayout.SOUTH);
        root.add(heading, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout()); center.setBackground(Color.WHITE);
        center.setBorder(new EmptyBorder(12, 20, 8, 20));
        JScrollPane scroller = new JScrollPane(form);
        scroller.setBorder(null); scroller.getViewport().setBackground(Color.WHITE);
        scroller.getVerticalScrollBar().setUnitIncrement(14);
        center.add(scroller, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        JTextArea error = new JTextArea(" ");error.setEditable(false);
        error.setOpaque(false);error.setLineWrap(true);error.setWrapStyleWord(true);
        error.setForeground(new Color(187, 48, 43));
        error.setFont(font(Font.PLAIN, 12));error.setRows(1);
        JPanel footer = new JPanel(new BorderLayout()); footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(7, 22, 16, 22));
        footer.add(error, BorderLayout.NORTH);
        JButton cancel = nut("Hủy", false), confirm = nut(action, true);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);
        if(!("Đóng".equals(action)&&validate==null))actions.add(cancel);
        actions.add(confirm);
        footer.add(actions, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        boolean[] accepted = {false};
        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> {
            try {
                if (validate != null) validate.run();
                accepted[0] = true;
                dialog.dispose();
            } catch (RuntimeException ex) {
                error.setText(ex.getMessage() == null ? "Dữ liệu không hợp lệ." : ex.getMessage());
            }
        });
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        int preferred = Math.min(740, Math.max(360, form.getPreferredSize().height + 217));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (centerOnMonitor) {
            // Lấy vùng màn hình của cửa sổ chính, tránh bật hộp thoại sang màn hình khác.
            GraphicsConfiguration gc = owner != null ? owner.getGraphicsConfiguration()
                : (parent == null ? null : parent.getGraphicsConfiguration());
            Rectangle bounds = gc == null ? new Rectangle(screen) : gc.getBounds();
            Insets insets = gc == null ? new Insets(0, 0, 0, 0)
                : Toolkit.getDefaultToolkit().getScreenInsets(gc);
            Rectangle usable = new Rectangle(
                bounds.x + insets.left, bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom);
            int width = Math.min(Math.max(610, form.getPreferredSize().width + 70),
                Math.min(740, Math.max(300, usable.width - 50)));
            int height = Math.min(preferred, Math.max(250, usable.height - 70));
            dialog.setSize(width, height);
            dialog.setLocation(usable.x + (usable.width - width) / 2,
                usable.y + (usable.height - height) / 2);
        } else {
            int width = Math.min(Math.max(610, form.getPreferredSize().width + 70),
                Math.min(740, screen.width - 50));
            dialog.setSize(width, Math.min(preferred, screen.height - 70));
            dialog.setLocationRelativeTo(parent);
        }
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                SwingUtilities.invokeLater(() -> scroller.getViewport()
                    .setViewPosition(new Point(0, 0)));
            }
        });
        dialog.setVisible(true);
        return accepted[0];
    }
    public static void xemThongTin(Component parent, String title, String subtitle, JComponent content) {
        hopThoaiForm(parent, title, subtitle, content, "Đóng", null, true);
    }
    public static boolean xacNhan(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Xác nhận giao dịch",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
    public static JPanel oNgay(JTextField field, Component parent) {
        JPanel p = new JPanel(new BorderLayout(6,0)); p.setOpaque(false);
        JButton pick = nut("Chọn ngày", false);
        pick.setPreferredSize(new Dimension(112, 36));
        pick.addActionListener(e -> {
            LocalDate initial;
            try { initial = LocalDate.parse(field.getText().trim()); }
            catch (Exception ex) { initial = LocalDate.now(); }
            LocalDate value = chonNgay(parent, initial);
            if (value != null) field.setText(value.toString());
        });
        field(field); p.add(field, BorderLayout.CENTER); p.add(pick, BorderLayout.EAST);
        return p;
    }
    public static JPanel oThang(JTextField field, Component parent) {
        JPanel p = new JPanel(new BorderLayout(5,0)); p.setOpaque(false);
        JButton pick = nut("Lịch", false); pick.setPreferredSize(new Dimension(70,36));
        pick.addActionListener(e -> {
            YearMonth initial;
            try { initial = YearMonth.parse(field.getText().trim()); }
            catch (Exception ex) { initial = YearMonth.now(); }
            YearMonth selected = chonThang(parent, initial);
            if (selected != null) field.setText(selected.toString());
        });
        field(field); p.add(field, BorderLayout.CENTER); p.add(pick, BorderLayout.EAST);
        return p;
    }
    public static YearMonth chonThang(Component parent, YearMonth initial) {
        JSpinner month = new JSpinner(new SpinnerNumberModel(initial.getMonthValue(), 1, 12, 1));
        JSpinner year = new JSpinner(new SpinnerNumberModel(initial.getYear(), 2000, 2100, 1));
        JPanel p = form("Tháng", month, "Năm", year);
        if (!hopThoaiForm(parent, "Chọn kỳ thu phí", "Chọn tháng và năm cần xem", p, "Áp dụng", null)) return null;
        return YearMonth.of((Integer)year.getValue(), (Integer)month.getValue());
    }
    public static LocalDate chonNgay(Component parent, LocalDate initial) {
        final LocalDate[] selected = {null};
        final YearMonth[] shown = {YearMonth.from(initial)};
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Chọn hạn thanh toán", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(8,10)); root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(15, 16, 15, 16));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JButton previous = nut("‹", false), next = nut("›", false);
        previous.setPreferredSize(new Dimension(44, 36)); next.setPreferredSize(new Dimension(44, 36));
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(font(Font.BOLD, 16)); monthLabel.setForeground(XANH_DAM);
        header.add(previous, BorderLayout.WEST); header.add(monthLabel, BorderLayout.CENTER); header.add(next, BorderLayout.EAST);
        JPanel days = new JPanel(new GridLayout(7,7,4,4)); days.setBackground(Color.WHITE);
        Runnable repaint = () -> {
            days.removeAll(); monthLabel.setText("Tháng " + shown[0].getMonthValue() + "/" + shown[0].getYear());
            for (String day : new String[]{"T2","T3","T4","T5","T6","T7","CN"}) {
                JLabel l = new JLabel(day, SwingConstants.CENTER); l.setForeground(CHU_MO); l.setFont(font(Font.BOLD,12)); days.add(l);
            }
            int offset = shown[0].atDay(1).getDayOfWeek().getValue()-1;
            for (int i=0;i<42;i++) {
                int n = i-offset+1;
                if (n<1 || n>shown[0].lengthOfMonth()) { days.add(new JLabel("")); continue; }
                LocalDate date = shown[0].atDay(n);
                JButton b = nut(Integer.toString(n), date.equals(initial));
                b.setFont(font(Font.PLAIN,12)); b.setPreferredSize(new Dimension(38,36));
                b.addActionListener(e -> { selected[0]=date; dialog.dispose(); });
                days.add(b);
            }
            days.revalidate(); days.repaint();
        };
        previous.addActionListener(e -> { shown[0]=shown[0].minusMonths(1); repaint.run(); });
        next.addActionListener(e -> { shown[0]=shown[0].plusMonths(1); repaint.run(); });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.setOpaque(false);
        JButton today = nut("Hôm nay",false), cancel = nut("Hủy",false);
        today.addActionListener(e -> { selected[0]=LocalDate.now(); dialog.dispose(); });
        cancel.addActionListener(e -> dialog.dispose()); bottom.add(today); bottom.add(cancel);
        root.add(header,BorderLayout.NORTH); root.add(days,BorderLayout.CENTER); root.add(bottom,BorderLayout.SOUTH);
        dialog.setContentPane(root); repaint.run(); dialog.setSize(480,450);
        dialog.setLocationRelativeTo(parent); dialog.setVisible(true);
        return selected[0];
    }
}
