package vn.vinabacus.giaodien.phanhe;

import vn.vinabacus.doituong.phanhe.MoTaManHinh;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.xuly.phanhe.KhoDuLieuManHinh;
import vn.vinabacus.giaodienchung.MauSacUngDung;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sơ đồ tương tác của Trung tâm: một môn -> nhiều khóa -> nhiều lớp.
 * Chỉ dùng dữ liệu mẫu trong RAM. Không thay đổi dữ liệu SQL Server.
 */
public final class GiaoDienSoDoMonKhoaLop extends JPanel {
    private static final Color DEEP = new Color(0x185B31);
    private static final Color LIGHT = new Color(0xE8F7E7);
    private static final Color SECONDARY = new Color(0x617366);
    private final JList<String> monList = new JList<>();
    private final JList<String> khoaList = new JList<>();
    private final DefaultTableModel lopModel = new DefaultTableModel(
            new String[]{"Tên lớp", "Giáo viên", "Lịch học", "Sĩ số"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable lopTable = new JTable(lopModel);
    private final JLabel monCount = new JLabel("0 môn");
    private final JLabel khoaCount = new JLabel("0 khóa");
    private final JLabel lopCount = new JLabel("0 lớp");
    private final JTextArea info = new JTextArea();
    private final List<Map<String,String>> allSubjects;
    private final List<Map<String,String>> allCourses;
    private final List<Map<String,String>> allClasses;
    private final Consumer<String> goTab;

    public GiaoDienSoDoMonKhoaLop(Consumer<String> goTab) {
        this.goTab = goTab;
        allSubjects = rows("Quản lý khóa học", "Môn học");
        allCourses = rows("Quản lý khóa học", "Khóa học");
        allClasses = rows("Quản lý lớp học", "Danh sách lớp");
        setLayout(new BorderLayout(0,15));
        setPreferredSize(new Dimension(1050,575));
        setOpaque(false);
        add(createHeadline(), BorderLayout.NORTH);
        add(createColumns(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        refreshSubjects();
    }
    private static List<Map<String,String>> rows(String menu,String tab) {
        for(MoTaManHinh.Trang t:MoTaManHinh.lay(CoSo.TRUNG_TAM,menu))
            if (t.tab().equals(tab))return new ArrayList<>(KhoDuLieuManHinh.lay(t));
        return List.of();
    }
    private static JPanel card(LayoutManager lm, int pad) {
        JPanel p=new JPanel(lm);p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xDCE8DE),1,true),
            new EmptyBorder(pad,pad,pad,pad)));
        return p;
    }
    private static JLabel text(String s, int size, boolean bold, Color c) {
        JLabel l=new JLabel(s);l.setFont(MauSacUngDung.font(size,bold));l.setForeground(c);return l;
    }
    private static JButton button(String title, boolean primary) {
        JButton b=new JButton(title);b.setFont(MauSacUngDung.font(13,true));b.setFocusPainted(false);
        b.setBackground(primary?new Color(0x138437):LIGHT);b.setForeground(primary?Color.WHITE:DEEP);
        b.setBorder(new EmptyBorder(11,16,11,16));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JComponent createHeadline() {
        JPanel top=card(new BorderLayout(10,8),17);top.setBackground(new Color(0xEDF9EC));
        JPanel label=new JPanel();label.setOpaque(false);label.setLayout(new BoxLayout(label,BoxLayout.Y_AXIS));
        label.add(text("Sơ đồ Môn học  →  Khóa học  →  Lớp học",21,true,DEEP));
        label.add(Box.createVerticalStrut(7));
        label.add(text("Chọn một môn để xem các cấp độ/khóa; chọn khóa để xem lớp, giáo viên và lịch học.",13,false,SECONDARY));
        top.add(label,BorderLayout.CENTER);
        JLabel chip=text("CƠ SỞ TRUNG TÂM",11,true,DEEP);
        chip.setBorder(new EmptyBorder(7,11,7,11));top.add(chip,BorderLayout.EAST);
        return top;
    }
    private JPanel createColumns() {
        JPanel columns=new JPanel(new GridBagLayout());columns.setOpaque(false);
        GridBagConstraints c=new GridBagConstraints();c.gridy=0;c.fill=GridBagConstraints.BOTH;c.weighty=1;
        c.gridx=0;c.weightx=.24;c.insets=new Insets(0,0,0,5);
        columns.add(createSubjectPanel(),c);
        c.gridx=1;c.weightx=.31;c.insets=new Insets(0,5,0,5);
        columns.add(createCoursePanel(),c);
        c.gridx=2;c.weightx=.45;c.insets=new Insets(0,5,0,0);
        columns.add(createClassPanel(),c);
        return columns;
    }
    private JPanel createSubjectPanel() {
        JPanel p=card(new BorderLayout(0,10),13);
        JPanel hd=new JPanel(new BorderLayout());hd.setOpaque(false);
        hd.add(text("1. Môn học",16,true,DEEP),BorderLayout.WEST);
        hd.add(monCount,BorderLayout.EAST);p.add(hd,BorderLayout.NORTH);
        prepareList(monList);
        monList.addListSelectionListener(e->{if(!e.getValueIsAdjusting())refreshCourses();});
        p.add(new JScrollPane(monList),BorderLayout.CENTER);
        JButton manage=button("Quản lý môn học",false);manage.addActionListener(e->goTab.accept("Môn học"));
        p.add(manage,BorderLayout.SOUTH);return p;
    }
    private JPanel createCoursePanel() {
        JPanel p=card(new BorderLayout(0,10),13);
        JPanel hd=new JPanel(new BorderLayout());hd.setOpaque(false);
        hd.add(text("2. Khóa học",16,true,DEEP),BorderLayout.WEST);
        hd.add(khoaCount,BorderLayout.EAST);p.add(hd,BorderLayout.NORTH);
        prepareList(khoaList);
        khoaList.addListSelectionListener(e->{if(!e.getValueIsAdjusting())refreshClasses();});
        p.add(new JScrollPane(khoaList),BorderLayout.CENTER);
        JButton manage=button("Quản lý khóa học",false);manage.addActionListener(e->goTab.accept("Khóa học"));
        p.add(manage,BorderLayout.SOUTH);return p;
    }
    private JPanel createClassPanel() {
        JPanel p=card(new BorderLayout(0,10),13);
        JPanel hd=new JPanel(new BorderLayout());hd.setOpaque(false);
        hd.add(text("3. Lớp đang mở",16,true,DEEP),BorderLayout.WEST);
        hd.add(lopCount,BorderLayout.EAST);p.add(hd,BorderLayout.NORTH);
        lopTable.setFont(MauSacUngDung.font(12,false));
        lopTable.setRowHeight(38);lopTable.setGridColor(new Color(0xEBF0ED));
        lopTable.getTableHeader().setBackground(new Color(0xE2F6DD));
        lopTable.getTableHeader().setForeground(DEEP);
        lopTable.getTableHeader().setFont(MauSacUngDung.font(12,true));
        lopTable.getTableHeader().setPreferredSize(new Dimension(0,38));
        lopTable.setSelectionBackground(new Color(0xD9F4D5));
        lopTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lopTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        JPanel middle=new JPanel(new BorderLayout(0,8));middle.setOpaque(false);
        JScrollPane scroll=new JScrollPane(lopTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xDDE8DC)));
        middle.add(scroll,BorderLayout.CENTER);
        info.setFont(MauSacUngDung.font(12,false));info.setForeground(DEEP);
        info.setBackground(new Color(0xF4FBF2));info.setEditable(false);info.setLineWrap(true);
        info.setWrapStyleWord(true);info.setMargin(new Insets(9,10,9,10));
        info.setRows(5);middle.add(info,BorderLayout.SOUTH);
        p.add(middle,BorderLayout.CENTER);
        JButton manage=button("Mở quản lý lớp",true);manage.addActionListener(e->goTab.accept("@Quản lý lớp học"));
        p.add(manage,BorderLayout.SOUTH);return p;
    }
    private JPanel createFooter() {
        JPanel p=card(new BorderLayout(),12);
        p.add(text("Một môn có nhiều khóa, một khóa mở nhiều lớp. Mỗi lớp có giáo viên và lịch riêng; học phí/số buổi chuẩn do khóa quy định.",12,false,DEEP),BorderLayout.CENTER);
        p.setBackground(new Color(0xF4FBF2));return p;
    }
    private static void prepareList(JList<String> list) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(MauSacUngDung.font(14,false));list.setFixedCellHeight(43);
        list.setSelectionBackground(new Color(0xD8F5D2));list.setSelectionForeground(DEEP);
        list.setBorder(new EmptyBorder(3,3,3,3));
    }
    private void refreshSubjects() {
        LinkedHashSet<String> names=new LinkedHashSet<>();
        for(Map<String,String> m:allSubjects){String s=m.get("Tên môn học");if(s!=null&&!s.isBlank())names.add(s);}
        for(Map<String,String> m:allCourses){String s=m.get("Môn học");if(s!=null&&!s.isBlank())names.add(s);}
        monList.setListData(names.toArray(String[]::new));
        monCount.setText(names.size()+" môn");
        if(!names.isEmpty())monList.setSelectedIndex(0);else refreshCourses();
    }
    private void refreshCourses() {
        String name=monList.getSelectedValue();LinkedHashSet<String> names=new LinkedHashSet<>();
        for(Map<String,String> m:allCourses)if(Objects.equals(name,m.get("Môn học"))) {
            String course=m.get("Tên khóa");if(course!=null&&!course.isBlank())names.add(course);
        }
        khoaList.setListData(names.toArray(String[]::new));khoaCount.setText(names.size()+" khóa");
        if(!names.isEmpty())khoaList.setSelectedIndex(0);else refreshClasses();
    }
    private void refreshClasses() {
        String course=khoaList.getSelectedValue();lopModel.setRowCount(0);
        Map<String,String> courseInfo=null;
        for(Map<String,String> r:allCourses)if(Objects.equals(course,r.get("Tên khóa"))){courseInfo=r;break;}
        for(Map<String,String> r:allClasses)if(Objects.equals(course,r.get("Khóa học/Năm học"))) {
            lopModel.addRow(new String[]{r.getOrDefault("Tên lớp",""),r.getOrDefault("Giáo viên",""),
                    r.getOrDefault("Lịch cố định",""),r.getOrDefault("Sĩ số","")});
        }
        lopCount.setText(lopModel.getRowCount()+" lớp");
        if(courseInfo==null){info.setText("Chọn một khóa học ở cột giữa để xem số buổi, học phí và các lớp đang mở.");return;}
        String n=courseInfo.getOrDefault("Tổng số buổi", "—");
        String fee=courseInfo.getOrDefault("Học phí mặc định","—");
        try{fee=String.format(new Locale("vi","VN"),"%,d đ", Long.parseLong(fee.replaceAll("\\D","")));}catch(Exception ignored){}
        info.setText("Môn: "+courseInfo.getOrDefault("Môn học","—")+"\n"+
                     "Khóa: "+course+"\n"+
                     "Số buổi chuẩn: "+n+" buổi  ·  Học phí mặc định: "+fee+"\n"+
                     (lopModel.getRowCount()==0?"Chưa có lớp mẫu thuộc khóa này.":
                     "Nhấn Quản lý lớp để tạo, sửa và phân công giáo viên."));
    }
}
