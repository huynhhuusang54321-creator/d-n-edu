package Tv2.View;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Passive View: chỉ dựng giao diện và phát sự kiện. */
public class HocVienPage extends JPanel {
    static final Color BG = Color.decode("#F5F7F2"), GREEN = Color.decode("#E0ECD7"),
            FILTER = Color.decode("#F9FAF5"), LINE = Color.decode("#CCDCC5"), BUTTON = Color.decode("#B7D99F");
    private final JLabel title = new JLabel(), context = new JLabel(), status = new JLabel();
    private final JTextField search = new JTextField(21);
    private final JComboBox<String> state = new JComboBox<>(), assignment = new JComboBox<>();
    private final JButton find = button("Tìm kiếm", BUTTON), reset = button("Reset", Color.decode("#FFE481")),
            add = button("+ Thêm", BUTTON), reload = button("Tải lại", BUTTON);
    private final DefaultTableModel model = tableModel();
    private final JTable table = table(model);
    private final JTabbedPane pages = new JTabbedPane();

    public HocVienPage() {
        setLayout(new BorderLayout(0, 12)); setBackground(BG); setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel top = new JPanel(new BorderLayout(0, 12)); top.setOpaque(false);
        add(header(title, context), BorderLayout.NORTH);
        JPanel filters = new JPanel(); filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        filters.setBackground(FILTER); filters.setBorder(BorderFactory.createLineBorder(LINE));
        JPanel row = filterRow(); row.add(new JLabel("Mã / tên / SĐT:")); row.add(search); row.add(find);
        row.add(new JLabel("Trạng thái:")); row.add(state); row.add(new JLabel("Xếp lớp:")); row.add(assignment); row.add(reset);
        JPanel actions = filterRow(); actions.add(add); actions.add(reload);
        filters.add(row); filters.add(actions); top.add(filters, BorderLayout.CENTER);
        JPanel listing=new JPanel(new BorderLayout(0,10));listing.setOpaque(false);listing.add(top,BorderLayout.NORTH);listing.add(scroll(table),BorderLayout.CENTER);pages.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);pages.addTab("Danh sách học viên",listing);add(pages, BorderLayout.CENTER);
        status.setOpaque(true); status.setBackground(GREEN); status.setBorder(new EmptyBorder(8, 12, 8, 12));
        add(status, BorderLayout.SOUTH);
    }
    public int selectedId(){int r=table.getSelectedRow();return r<0?-1:(Integer)model.getValueAt(table.convertRowIndexToModel(r),0);}
    public void onPopup(java.util.function.IntConsumer action){MainPage.View.Workspace.popup(table,new String[]{"Xem hồ sơ","Sao chép thông tin học viên","Xóa hồ sơ"},action);}
    public boolean selectProfile(String key){return selectTab(pages,key);}
    public void openProfile(String key,String label,JComponent panel){openProfile(key,label,panel,()->closeProfile(panel));}
    public void openProfile(String key,String label,JComponent panel,Runnable close){openTab(pages,key,label,panel,close);}
    public void closeProfile(JComponent panel){pages.remove(panel);}
    static boolean selectTab(JTabbedPane tabs,String key){for(int i=0;i<tabs.getTabCount();i++){if(tabs.getComponentAt(i) instanceof JComponent p && key.equals(p.getClientProperty("profile"))){tabs.setSelectedIndex(i);return true;}}return false;}
    static void openTab(JTabbedPane tabs,String key,String label,JComponent panel,Runnable close){
        if(selectTab(tabs,key))return;panel.putClientProperty("profile",key);tabs.addTab(label,panel);
        JPanel tab=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));tab.setOpaque(false);
        JLabel name=new JLabel(label);tab.add(name);JButton x=new JButton(new CloseIcon());x.setBorder(new EmptyBorder(4,4,4,4));x.setContentAreaFilled(false);x.setFocusPainted(false);x.setToolTipText("Đóng "+label);x.getAccessibleContext().setAccessibleName("Đóng "+label);x.addActionListener(e->close.run());tab.add(x);tabs.setTabComponentAt(tabs.indexOfComponent(panel),tab);tabs.setSelectedComponent(panel);
    }
    private static final class CloseIcon implements javax.swing.Icon {public int getIconWidth(){return 12;}public int getIconHeight(){return 12;}public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D a=(Graphics2D)g.create();a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);a.setColor(Color.decode("#52634C"));a.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));a.drawLine(x+3,y+3,x+9,y+9);a.drawLine(x+9,y+3,x+3,y+9);a.dispose();}}
    static JPanel header(JLabel title, JLabel context) {
        JPanel p = new JPanel(new BorderLayout(0, 4)); p.setBackground(GREEN); p.setBorder(new EmptyBorder(14, 16, 14, 16));
        title.setFont(new Font("Segoe UI", Font.BOLD, 23)); context.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(title, BorderLayout.NORTH); p.add(context, BorderLayout.SOUTH); return p;
    }
    static JPanel filterRow() { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6)); p.setOpaque(false); return p; }
    static JButton button(String text, Color color) { JButton b = new JButton(text); b.setBackground(color); b.setForeground(Color.BLACK); b.setFocusPainted(false); b.setFont(new Font("Segoe UI", Font.BOLD, 14)); b.setBorder(new EmptyBorder(10, 18, 10, 18)); return b; }
    static DefaultTableModel tableModel() { return new DefaultTableModel() { @Override public boolean isCellEditable(int r, int c) { return false; } }; }
    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m); t.setRowHeight(32); t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setGridColor(LINE); t.setShowGrid(true); t.setFillsViewportHeight(true);
        t.getTableHeader().setBackground(GREEN); t.getTableHeader().setForeground(Color.BLACK);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14)); t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                if (!selected) c.setBackground(row % 2 == 0 ? Color.WHITE : Color.decode("#F2F8E7"));
                setToolTipText(value == null ? null : String.valueOf(value)); return c;
            }
        }); return t;
    }
    static JScrollPane scroll(JTable t) { JScrollPane s = new JScrollPane(t); s.setBorder(BorderFactory.createLineBorder(LINE)); s.getViewport().setBackground(Color.WHITE); return s; }
    static void widths(JTable t, int[] widths) { for(int i=0;i<widths.length && i<t.getColumnCount();i++) t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]); }
    public void setColumns(String[] columns, int[] widths) { model.setColumnIdentifiers(columns); widths(table, widths); }
    public void setRows(Object[][] rows) { model.setRowCount(0); for(Object[] r:rows) model.addRow(r); }
    public void setTitleText(String text) { title.setText(text); }
    public void setContextText(String text) { context.setText(text); }
    public void setStatusText(String text) { status.setText(text); }
    public void setFilters(String[] states, String[] assignments) { state.setModel(new DefaultComboBoxModel<>(states)); assignment.setModel(new DefaultComboBoxModel<>(assignments)); }
    public String getSearchText() { return search.getText(); }
    public int getStateIndex() { return state.getSelectedIndex(); }
    public int getAssignmentIndex() { return assignment.getSelectedIndex(); }
    public void resetInputs() { search.setText(""); state.setSelectedIndex(0); assignment.setSelectedIndex(0); }
    public void setAddEnabled(boolean enabled, String tooltip) { add.setEnabled(enabled); add.setToolTipText(tooltip); }
    public void setBusy(boolean busy) { find.setEnabled(!busy); reset.setEnabled(!busy); reload.setEnabled(!busy); state.setEnabled(!busy); assignment.setEnabled(!busy); search.setEnabled(!busy); }
    public void onSearch(ActionListener l) { find.addActionListener(l); search.addActionListener(l); }
    public void onFilter(ActionListener l) { state.addActionListener(l); assignment.addActionListener(l); }
    public void onReset(ActionListener l) { reset.addActionListener(l); }
    public void onReload(ActionListener l) { reload.addActionListener(l); }
    public void onAdd(ActionListener l) { add.addActionListener(l); }
}
