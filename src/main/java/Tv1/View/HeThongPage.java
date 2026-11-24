package Tv1.View;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;

/** Passive View: hiển thị dữ liệu và phát sự kiện cho Controller. */
public class HeThongPage extends JPanel {
    private static final Color BG = Color.decode("#FCFAED"), GREEN = Color.decode("#C1DFA0"), BUTTON = Color.decode("#A4D36D"), LINE = Color.decode("#A4CD69"), FILTER = Color.decode("#FFF8C4"), STRIPE = Color.decode("#F2F8E7");
    private final JLabel lblTitle = new JLabel(), lblContext = new JLabel(), lblStatus = new JLabel(), lblCount = new JLabel(), lblNote = new JLabel();
    private final JButton btnReload = button("Tải lại bảng"), btnReloadAll = button("Tải lại tất cả"), branchReload = button("Làm mới"), catalogReload = button("Làm mới"), detail = button("Xem chi tiết");
    private final JTextField txtSearch = new JTextField(18), catalogSearch = new JTextField(18);
    private final JComboBox<String> catalogFilter = new JComboBox<>(new String[]{"Tất cả trạng thái","Hoạt động","Ngừng hoạt động"});
    private final DefaultListModel<String> tableNames = new DefaultListModel<>(), groups = new DefaultListModel<>();
    private final JList<String> tableList = new JList<>(tableNames), groupList = new JList<>(groups);
    private final JPanel tableCards = new JPanel(new CardLayout());
    private final Map<String,JTable> tables = new LinkedHashMap<>();
    private final java.util.List<String> keys = new ArrayList<>();
    private final JLabel[] branchValues = new JLabel[8];
    private final JLabel branchStatus = new JLabel("Chưa tải dữ liệu"), catalogStatus = new JLabel("Chưa tải dữ liệu");
    private final JTable catalog = table(new String[]{"Mã DM","Mã ký hiệu","Tên danh mục","Nhóm","Danh mục cha","Thứ tự","Trạng thái"});
    private boolean changingGroups;

    public HeThongPage() {
        setLayout(new BorderLayout(0,10)); setBackground(BG); setBorder(new EmptyBorder(8,8,8,8));
        JPanel heading = panel(new GridLayout(2,1,0,4),GREEN); heading.setBorder(new EmptyBorder(10,12,10,12));
        lblTitle.setFont(new Font("Segoe UI",Font.BOLD,24)); heading.add(lblTitle); heading.add(lblContext); add(heading,BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane(); tabs.setFont(new Font("Segoe UI",Font.PLAIN,14)); tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("Cơ sở",branchPage()); tabs.addTab("Danh mục",catalogPage()); tabs.addTab("Dữ liệu hệ thống",dataPage()); tabs.addTab("Thông tin phần mềm",infoPage()); add(tabs,BorderLayout.CENTER);
    }
    private static JPanel panel(LayoutManager layout,Color color){ JPanel p=new JPanel(layout); p.setBackground(color); return p; }
    private static JButton button(String text){ JButton b=new JButton(text); b.setBackground(BUTTON); b.setFont(new Font("Segoe UI",Font.PLAIN,13)); return b; }
    private static JPanel bar(Component... components){ JPanel p=panel(new FlowLayout(FlowLayout.LEFT,8,7),FILTER); for(Component c:components)p.add(c); return p; }
    private static JScrollPane toolbar(Component c){
        JScrollPane s=new JScrollPane(c,ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        s.setBorder(null); s.getHorizontalScrollBar().setUnitIncrement(16);
        Dimension d=c.getPreferredSize(); s.setPreferredSize(new Dimension(d.width,d.height+s.getHorizontalScrollBar().getPreferredSize().height));
        s.setMinimumSize(new Dimension(0,s.getPreferredSize().height)); return s;
    }
    private static JScrollPane scroll(Component c){ JScrollPane s=new JScrollPane(c); s.setBorder(BorderFactory.createLineBorder(LINE)); return s; }
    private static JTable table(String[] columns){
        JTable t=new JTable(new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int r,int c){return false;}});
        t.setFont(new Font("Segoe UI",Font.PLAIN,13)); t.setRowHeight(29); t.setFillsViewportHeight(true); t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.getTableHeader().setReorderingAllowed(false); t.getTableHeader().setBackground(GREEN);
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col){super.getTableCellRendererComponent(table,value==null?"NULL":value,selected,focus,row,col); if(!selected)setBackground(row%2==0?Color.WHITE:STRIPE); return this;}});
        for(int i=0;i<columns.length;i++)t.getColumnModel().getColumn(i).setPreferredWidth(150); return t;
    }
    private static void rows(JTable t,java.util.List<Object[]> data){DefaultTableModel m=(DefaultTableModel)t.getModel();m.setRowCount(0);for(Object[] r:data)m.addRow(r);}
    private JPanel branchPage(){
        JPanel p=panel(new BorderLayout(8,8),BG); JPanel fields=panel(new GridLayout(0,2,10,12),BG);fields.setBorder(new EmptyBorder(18,18,18,18));
        String[] names={"Mã cơ sở","Mã hiển thị","Tên cơ sở","Loại cơ sở","Địa chỉ","Số điện thoại","Logo","Trạng thái"};
        for(int i=0;i<names.length;i++){fields.add(new JLabel(names[i]));branchValues[i]=new JLabel("—");fields.add(branchValues[i]);}
        JButton update=button("Cập nhật thông tin"); update.setEnabled(false);update.setToolTipText("Chờ Admin cấp quyền");p.add(bar(branchReload,update),BorderLayout.NORTH);p.add(scroll(fields),BorderLayout.CENTER);p.add(branchStatus,BorderLayout.SOUTH);return p;
    }
    private JPanel catalogPage(){
        JPanel p=panel(new BorderLayout(8,8),BG); groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);groupList.setFixedCellHeight(28);groups.addElement("Tất cả danh mục");groupList.setSelectedIndex(0);
        JPanel right=panel(new BorderLayout(0,6),BG);JPanel top=panel(new GridLayout(2,1),BG);top.add(toolbar(bar(new JLabel("Tìm kiếm:"),catalogSearch,catalogFilter,catalogReload)));
        JPanel actions=bar(detail); for(String name:new String[]{"Thêm","Sửa","Ngừng hoạt động","Khôi phục","Xóa"}){JButton b=button(name);b.setEnabled(false);b.setToolTipText("Chờ Admin xác định nhóm danh mục được phép chỉnh sửa");actions.add(b);}top.add(toolbar(actions));right.add(top,BorderLayout.NORTH);right.add(scroll(catalog),BorderLayout.CENTER);right.add(catalogStatus,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scroll(groupList),right);split.setDividerLocation(185);split.setResizeWeight(0);p.add(split);detail.setEnabled(false);catalog.getSelectionModel().addListSelectionListener(e->detail.setEnabled(catalog.getSelectedRow()>=0));return p;
    }
    private JPanel dataPage(){
        JPanel p=panel(new BorderLayout(8,8),BG); tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);tableList.setFixedCellHeight(26);
        tableList.addListSelectionListener(e->{if(!e.getValueIsAdjusting()){String key=getSelectedTableKey();if(key!=null)((CardLayout)tableCards.getLayout()).show(tableCards,key);}});
        JPanel right=panel(new BorderLayout(0,8),BG);right.add(toolbar(bar(new JLabel("Tìm kiếm:"),txtSearch,btnReload,btnReloadAll)),BorderLayout.NORTH);right.add(tableCards,BorderLayout.CENTER);
        JPanel footer=panel(new BorderLayout(8,5),GREEN);footer.add(lblStatus,BorderLayout.CENTER);footer.add(lblCount,BorderLayout.EAST);footer.add(lblNote,BorderLayout.SOUTH);right.add(footer,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scroll(tableList),right);split.setDividerLocation(180);split.setResizeWeight(0);p.add(split);return p;
    }
    private JScrollPane infoPage(){ JPanel p=panel(new GridLayout(0,2,10,10),BG);p.setBorder(new EmptyBorder(20,20,20,20));String[][] fields={{"Phần mềm","VINABACUS"},{"Phạm vi","Trung tâm và Mầm non"},{"Nền tảng","Java 17 / Swing"},{"Cơ sở dữ liệu","SQL Server"},{"Kiến trúc","MVC"},{"Quản lý dự án","Maven"},{"Truy xuất dữ liệu","Cache First"},{"TV1","Nhân sự, Hệ thống"},{"TV2","Học viên, Tuyển sinh"},{"TV3","Khóa học, Lớp"},{"TV4","Thời khóa biểu, Điểm danh"},{"TV5","Chi thu, Kho"},{"TV6","Chương trình, Báo cáo"},{"Admin","Nền tảng dùng chung"}};for(String[] f:fields){p.add(new JLabel(f[0]));p.add(new JLabel(f[1]));}return scroll(p); }
    public void setTitleText(String t){lblTitle.setText(t);} public void setContextText(String t){lblContext.setText(t);} public void setStatusText(String t){lblStatus.setText(t);} public void setCountText(String t){lblCount.setText(t);} public void setNoteText(String t){lblNote.setText(t);}
    public void setActionLabels(String a,String b){btnReload.setText(a);btnReloadAll.setText(b);}
    public void addDataTable(String key,String title,String[] columns){JTable t=table(columns);tables.put(key,t);keys.add(key);tableCards.add(scroll(t),key);tableNames.addElement(title);if(tableList.getSelectedIndex()<0)tableList.setSelectedIndex(0);}
    public void setRows(String key,java.util.List<Object[]> data){if(tables.containsKey(key))rows(tables.get(key),data);}
    public void setTabTitle(String key,String title){int i=keys.indexOf(key);if(i>=0)tableNames.set(i,title);}
    public String getSelectedTableKey(){int i=tableList.getSelectedIndex();return i>=0&&i<keys.size()?keys.get(i):null;}
    public String getSearchText(){return txtSearch.getText();}
    public void setReloadEnabled(boolean enabled){btnReload.setEnabled(enabled);btnReloadAll.setEnabled(enabled);branchReload.setEnabled(enabled);catalogReload.setEnabled(enabled);}
    public void showBranch(Object[] r,String message){for(int i=0;i<8;i++){Object v=r==null?null:r[i];String t=v==null?"—":String.valueOf(v);if(i==3)t=switch(t){case "TRUNG_TAM"->"Trung tâm";case "MAM_NON"->"Mầm non";default->t;};if(i==7&&v!=null)t=Boolean.TRUE.equals(v)?"Đang hoạt động":"Ngừng hoạt động";branchValues[i].setText(t);}branchStatus.setText(r==null?message:"Đã nhận thông tin từ Service");}
    public void setCatalogGroups(java.util.List<String> data){String old=getCatalogGroup();changingGroups=true;try{groups.clear();groups.addElement("Tất cả danh mục");for(String g:data)groups.addElement(g);int index=old==null?0:groups.indexOf(old);groupList.setSelectedIndex(Math.max(0,index));}finally{changingGroups=false;}}
    public String getCatalogGroup(){return groupList.getSelectedIndex()<=0?null:groupList.getSelectedValue();}public int getCatalogStatusFilter(){return catalogFilter.getSelectedIndex();}public String getCatalogSearch(){return catalogSearch.getText();}
    public void setCatalogRows(java.util.List<Object[]> data){rows(catalog,data);detail.setEnabled(false);} public void setCatalogStatus(String t){catalogStatus.setText(t);}
    public Object getSelectedCatalogId(){int i=catalog.getSelectedRow();return i<0?null:catalog.getModel().getValueAt(catalog.convertRowIndexToModel(i),0);}
    public void showCatalogDetail(Object[] r){String[] labels={"Mã danh mục","Mã cơ sở","Nhóm","Mã ký hiệu","Tên","Mã cha","Thứ tự","Cấu hình JSON","Trạng thái"};JTextArea area=new JTextArea(12,46);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);StringBuilder b=new StringBuilder();for(int i=0;i<labels.length;i++)b.append(labels[i]).append(": ").append(r[i]==null?"—":r[i]).append('\n');area.setText(b.toString());area.setCaretPosition(0);JOptionPane.showMessageDialog(this,new JScrollPane(area),"Chi tiết danh mục",JOptionPane.INFORMATION_MESSAGE);}
    private static void onText(JTextField f,Runnable listener){f.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){listener.run();}public void removeUpdate(DocumentEvent e){listener.run();}public void changedUpdate(DocumentEvent e){listener.run();}});}
    public void onReload(Runnable r){btnReload.addActionListener(e->r.run());} public void onReloadAll(Runnable r){btnReloadAll.addActionListener(e->r.run());} public void onTableChanged(Runnable r){tableList.addListSelectionListener(e->{if(!e.getValueIsAdjusting())r.run();});} public void onSearchChanged(Runnable r){onText(txtSearch,r);}
    public void onBranchReload(Runnable r){branchReload.addActionListener(e->r.run());}public void onCatalogReload(Runnable r){catalogReload.addActionListener(e->r.run());}public void onCatalogChanged(Runnable r){onText(catalogSearch,r);catalogFilter.addActionListener(e->r.run());groupList.addListSelectionListener(e->{if(!changingGroups&&!e.getValueIsAdjusting())r.run();});}public void onCatalogDetail(Runnable r){detail.addActionListener(e->r.run());}
}
