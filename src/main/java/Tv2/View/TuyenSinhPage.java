package Tv2.View;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.util.LinkedHashMap;
import java.util.Map;

/** Passive View; dùng lại các hàm dựng component của TV2. */
public class TuyenSinhPage extends JPanel {
    private final JLabel title = new JLabel(), context = new JLabel(), status = new JLabel();
    private final JTextField search = new JTextField(21);
    private final JComboBox<String> trial = new JComboBox<>();
    private final JButton find = HocVienPage.button("Tìm kiếm", HocVienPage.BUTTON),
            reset = HocVienPage.button("Reset", Color.decode("#FFE481")),
            add = HocVienPage.button("+ Tiếp nhận", HocVienPage.BUTTON), reload = HocVienPage.button("Tải lại", HocVienPage.BUTTON);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTabbedPane pages = new JTabbedPane();
    private final Map<String,JTable> tables=new LinkedHashMap<>();
    private final Map<String, DefaultTableModel> models = new LinkedHashMap<>();
    private final Map<String, JPanel> panels = new LinkedHashMap<>();
    private final Map<String, JLabel> descriptions = new LinkedHashMap<>();
    public TuyenSinhPage() {
        setLayout(new BorderLayout(0, 12)); setBackground(HocVienPage.BG); setBorder(new EmptyBorder(12,12,12,12));
        JPanel top = new JPanel(new BorderLayout(0,12)); top.setOpaque(false); add(HocVienPage.header(title,context), BorderLayout.NORTH);
        JPanel filters = new JPanel(); filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS)); filters.setBackground(HocVienPage.FILTER); filters.setBorder(BorderFactory.createLineBorder(HocVienPage.LINE));
        JPanel row = HocVienPage.filterRow(); row.add(new JLabel("Mã / tên / SĐT:")); row.add(search); row.add(find); row.add(new JLabel("Học thử:")); row.add(trial); row.add(reset);
        JPanel actions = HocVienPage.filterRow(); actions.add(add); actions.add(reload); filters.add(row); filters.add(actions); top.add(filters,BorderLayout.CENTER);
        JPanel listing=new JPanel(new BorderLayout(0,8)); listing.setOpaque(false);listing.add(top,BorderLayout.NORTH);listing.add(tabs,BorderLayout.CENTER);pages.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);pages.addTab("Danh sách tuyển sinh",listing);add(pages,BorderLayout.CENTER);
        status.setOpaque(true); status.setBackground(HocVienPage.GREEN); status.setBorder(new EmptyBorder(8,12,8,12)); status.setForeground(new Color(165,30,30)); add(status,BorderLayout.SOUTH);
    }
    public void addTab(String key, String label, String description, String[] columns, int[] widths) {
        DefaultTableModel model=HocVienPage.tableModel(); model.setColumnIdentifiers(columns); JTable table=HocVienPage.table(model); HocVienPage.widths(table,widths);
        JPanel panel=new JPanel(new BorderLayout(0,6)); panel.setBackground(Color.WHITE);
        JLabel desc=new JLabel(description); desc.setBorder(new EmptyBorder(10,12,10,12)); panel.add(desc,BorderLayout.NORTH); panel.add(HocVienPage.scroll(table),BorderLayout.CENTER);
        tables.put(key,table);models.put(key,model); panels.put(key,panel); descriptions.put(key,desc); tabs.addTab(label,panel);
    }
    public boolean selectProfile(String key){return HocVienPage.selectTab(pages,key);}
    public void openProfile(String key,String label,JComponent panel,Runnable close){HocVienPage.openTab(pages,key,label,panel,close);}
    public void closeProfile(JComponent panel){pages.remove(panel);}
    public int selectedId(){for(var e:panels.entrySet())if(e.getValue()==tabs.getSelectedComponent()){JTable t=tables.get(e.getKey());int r=t.getSelectedRow();return r<0?-1:(Integer)models.get(e.getKey()).getValueAt(t.convertRowIndexToModel(r),0);}return -1;}
    public void onPopup(java.util.function.IntConsumer action){for(JTable t:tables.values())MainPage.View.Workspace.popup(t,new String[]{"Xem hồ sơ","Xếp học thử","Chuyển chính thức","Xếp lớp","Xóa hồ sơ"},action);}
    public void setRows(String key,Object[][] rows) { DefaultTableModel m=models.get(key); if(m==null)return; m.setRowCount(0); for(Object[] r:rows)m.addRow(r); }
    public void setTabTitle(String key,String title) { int i=tabs.indexOfComponent(panels.get(key)); if(i>=0) tabs.setTitleAt(i,title); }
    public void setTitleText(String text){title.setText(text);}
    public void setContextText(String text){context.setText(text);}
    public void setStatusText(String text){status.setText(text);}
    public void setTrialOptions(String[] options){trial.setModel(new DefaultComboBoxModel<>(options));}
    public int getTrialIndex(){return trial.getSelectedIndex();}
    public String getSearchText(){return search.getText();}
    public void resetInputs(){search.setText(""); trial.setSelectedIndex(0);}
    public void setAddEnabled(boolean enabled,String tooltip){add.setEnabled(enabled);add.setToolTipText(tooltip);}
    public void setBusy(boolean busy){search.setEnabled(!busy);find.setEnabled(!busy);reset.setEnabled(!busy);reload.setEnabled(!busy);trial.setEnabled(!busy);}
    public void onSearch(ActionListener l){find.addActionListener(l);search.addActionListener(l);}
    public void onFilter(ActionListener l){trial.addActionListener(l);}
    public void onReset(ActionListener l){reset.addActionListener(l);}
    public void onReload(ActionListener l){reload.addActionListener(l);}
    public void onAdd(ActionListener l){add.addActionListener(l);}
}
