package vn.vinabacus.giaodien.phanhe;

import vn.vinabacus.doituong.phanhe.MoTaManHinh;
import vn.vinabacus.xuly.phanhe.KhoDuLieuManHinh;
import vn.vinabacus.giaodien.GiaoDienChinh;
import vn.vinabacus.giaodienchung.MauSacUngDung;
import vn.vinabacus.model.CoSo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static vn.vinabacus.giaodien.phanhe.ThanhPhanPhanHe.*;

/**
 * Giao diện duyệt toàn bộ quy trình: tab, bảng, lọc, xem chi tiết, form thêm/sửa,
 * dữ liệu RAM. Chưa có JDBC, OTP, phân quyền an toàn, Excel/PDF chính thức.
 */
public final class GiaoDienPhanHe extends JPanel {
    private static final Color DEEP=new Color(0x155A30), MUTED=new Color(0x62766D), BORDER=new Color(0xDDE8DC);
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final GiaoDienChinh app;
    private final CoSo coSo;
    private final String menu;
    private final List<MoTaManHinh.Trang> tabs;
    private final JPanel area=new JPanel(new BorderLayout());
    private final JPanel tabBar=new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    private int currentTabIndex=0;
    private MoTaManHinh.Trang current;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField search;
    private JComboBox<String> statusFilter;
    private JLabel countLabel;

    public GiaoDienPhanHe(CoSo coSo,String menu,GiaoDienChinh app){
        this.app=app;this.coSo=coSo;this.menu=menu;this.tabs=MoTaManHinh.lay(coSo,menu);
        setLayout(new BorderLayout(0,10));setBackground(MauSacUngDung.NEN);
        setBorder(new EmptyBorder(16,20,20,20));
        JPanel intro=card(new BorderLayout(),16);intro.setBackground(new Color(0xEAF7E9));
        JPanel title=tam();title.setLayout(new BoxLayout(title,BoxLayout.Y_AXIS));
        title.add(nhan(menu,24,true,DEEP));title.add(Box.createVerticalStrut(5));
        title.add(nhan(coSo==CoSo.MAM_NON?"Cơ sở Mầm non":"Cơ sở Trung tâm",12,false,MUTED));intro.add(title,BorderLayout.WEST);
        intro.add(nhan("BẢN THIẾT KẾ  •  CHƯA KẾT NỐI CSDL",11,true,DEEP),BorderLayout.EAST);
        JPanel head=new JPanel(new BorderLayout(0,10));head.setOpaque(false);head.add(intro,BorderLayout.NORTH);
        if(tabs.isEmpty()){add(duoi("Chưa thiết kế màn hình cho mục này."),BorderLayout.CENTER);return;}
        tabBar.setOpaque(false);head.add(tabBar,BorderLayout.SOUTH);add(head,BorderLayout.NORTH);
        area.setOpaque(false);add(area,BorderLayout.CENTER);hienTab(0);
    }
    private void hienTab(int pos){
        if(pos<0||pos>=tabs.size())return;
        current=tabs.get(pos);currentTabIndex=pos;area.removeAll();tabBar.removeAll();
        for(int i=0;i<tabs.size();i++){
            final int index=i;JButton tab=nut(tabs.get(i).tab(),i==pos);
            tab.setFont(MauSacUngDung.font(12,i==pos));tab.addActionListener(e->hienTab(index));tabBar.add(tab);
        }
        tabBar.revalidate();tabBar.repaint();
        if (coSo==CoSo.TRUNG_TAM && menu.equals("Quản lý khóa học")
                && current.tab().equals("Sơ đồ môn – khóa – lớp")) {
            area.add(new GiaoDienSoDoMonKhoaLop(tabName -> {
                if (tabName.startsWith("@")) { app.moMuc(tabName.substring(1));return; }
                for(int n=0;n<tabs.size();n++) if(tabs.get(n).tab().equals(tabName)) {hienTab(n);return;}
            }),BorderLayout.CENTER);
            area.revalidate();area.repaint();return;
        }
        JPanel body=tam();body.setLayout(new BorderLayout(0,12));
        JLabel hint=nhan("<html>"+html(current.moTa())+"</html>",13,false,MUTED);
        JPanel topBody=tam();topBody.setLayout(new BorderLayout(0,9));topBody.add(hint,BorderLayout.NORTH);
        JPanel controls=card(new BorderLayout(12,8),13);
        JPanel searchAndFilter=new JPanel(new FlowLayout(FlowLayout.LEFT,9,0));searchAndFilter.setOpaque(false);
        search=nhap();search.setColumns(20);search.putClientProperty("JTextField.placeholderText","Tìm kiếm tất cả cột...");
        searchAndFilter.add(nhan("Tìm kiếm",12,true,DEEP));searchAndFilter.add(search);
        statusFilter=new JComboBox<>();statusFilter.setFont(MauSacUngDung.font(12,false));
        statusFilter.setPreferredSize(new Dimension(160,36));statusFilter.addItem("Tất cả trạng thái");
        int columnStatus=colStatus(current);
        if(columnStatus>=0){
            Set<String> vals=new LinkedHashSet<>();for(Map<String,String> r:KhoDuLieuManHinh.lay(current))vals.add(r.getOrDefault(current.cot()[columnStatus],""));
            for(String val:vals)if(!val.isBlank())statusFilter.addItem(val);
            searchAndFilter.add(statusFilter);
        }
        controls.add(searchAndFilter,BorderLayout.NORTH);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,8,2));buttons.setOpaque(false);
        for(int i=0;i<current.thaoTac().length;i++){
            String action=current.thaoTac()[i];
            JButton b=nut(action,i==0);
            b.addActionListener(e->thaoTac(action));buttons.add(b);
        }
        JButton csv=nut("Xuất CSV mẫu",false);csv.addActionListener(e->xuatCSV());
        if(Arrays.stream(current.thaoTac()).noneMatch(s->s.contains("CSV")))buttons.add(csv);
        if(Arrays.stream(current.thaoTac()).noneMatch(s->s.equals("Sửa")||s.contains("Sửa "))){
            JButton b=nut("Sửa dòng",false);b.addActionListener(e->suaDong());buttons.add(b);
        }
        controls.add(buttons,BorderLayout.CENTER);
        topBody.add(controls,BorderLayout.CENTER);
        body.add(topBody,BorderLayout.NORTH);

        JPanel card=card(new BorderLayout(0,10),12);
        String[] cols=current.cot();
        tableModel=new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int row,int col){return false;}};
        table=new JTable(tableModel);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(38);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(MauSacUngDung.font(12,false));table.setGridColor(new Color(0xE7EFE7));
        table.setSelectionBackground(new Color(0xE0F6DA));table.setSelectionForeground(new Color(0x153B20));
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(new Color(0xE0F6D9));
        table.getTableHeader().setForeground(DEEP);table.getTableHeader().setFont(MauSacUngDung.font(12,true));
        table.getTableHeader().setPreferredSize(new Dimension(0,42));
        DefaultTableCellRenderer renderer=new DefaultTableCellRenderer();renderer.setBorder(new EmptyBorder(0,10,0,8));
        for(int i=0;i<cols.length;i++){
            TableColumn col=table.getColumnModel().getColumn(i);
            col.setCellRenderer(renderer);col.setPreferredWidth(Math.max(110,Math.min(230,cols[i].length()*11+55)));
        }
        int status=colStatus(current);if(status>=0)table.getColumnModel().getColumn(status).setPreferredWidth(145);
        sorter=new TableRowSorter<>(tableModel);table.setRowSorter(sorter);
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}});
        statusFilter.addActionListener(e->filter());
        table.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)xemChiTiet();}});
        JScrollPane scroll=new JScrollPane(table);scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setPreferredSize(new Dimension(1000,Math.min(470,Math.max(205,tableModel.getRowCount()*40+65))));
        scroll.setMinimumSize(new Dimension(420,200));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        card.add(scroll,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);
        countLabel=nhan("",12,false,MUTED);
        bottom.add(countLabel,BorderLayout.WEST);
        JPanel act=new JPanel(new FlowLayout(FlowLayout.RIGHT,7,0));act.setOpaque(false);
        JButton view=nut("Xem chi tiết",false);view.addActionListener(e->xemChiTiet());act.add(view);
        JButton edit=nut("Sửa",false);edit.addActionListener(e->suaDong());act.add(edit);
        JButton stop=nut("Xóa dữ liệu mẫu",false);stop.setToolTipText("Chỉ xóa bản ghi demo trong RAM, không tác động SQL Server");
        stop.addActionListener(e->xoaDong());act.add(stop);
        bottom.add(act,BorderLayout.EAST);card.add(bottom,BorderLayout.SOUTH);
        body.add(card,BorderLayout.CENTER);
        body.add(duoi("Lưu ý: dữ liệu đang thao tác chỉ tồn tại khi ứng dụng mở. Phiếu thu, điểm danh, học bù, học phí và phân quyền chưa ghi vào SQL Server; những phần cần xử lý nghiệp vụ phức tạp sẽ được kiểm thử riêng."),BorderLayout.SOUTH);
        area.add(body,BorderLayout.CENTER);
        napBang();area.revalidate();area.repaint();
    }
    private static int colStatus(MoTaManHinh.Trang t){for(int i=0;i<t.cot().length;i++)if(t.cot()[i].equals("Trạng thái"))return i;return -1;}
    private void napBang(){
        if(tableModel==null)return;
        tableModel.setRowCount(0);
        for(Map<String,String> row:KhoDuLieuManHinh.lay(current)){
            Object[] vals=new Object[current.cot().length];for(int i=0;i<vals.length;i++)vals[i]=row.getOrDefault(current.cot()[i],"");tableModel.addRow(vals);
        }
        if(countLabel!=null)countLabel.setText("Tổng số: "+tableModel.getRowCount()+" bản ghi mẫu • Bấm đúp một dòng để xem chi tiết");
    }
    private void filter(){
        if(sorter==null)return;
        String text=search.getText().trim();String filter=(String)statusFilter.getSelectedItem();
        List<RowFilter<Object,Object>> fs=new ArrayList<>();
        if(!text.isBlank())fs.add(RowFilter.regexFilter("(?iu)"+Pattern.quote(text)));
        int col=colStatus(current);if(col>=0&&filter!=null&&!filter.startsWith("Tất cả"))fs.add(RowFilter.regexFilter("^"+Pattern.quote(filter)+"$",col));
        sorter.setRowFilter(fs.isEmpty()?null:RowFilter.andFilter(fs));
        countLabel.setText("Hiển thị "+table.getRowCount()+" / "+tableModel.getRowCount()+" bản ghi mẫu");
    }
    private int index(){int view=table.getSelectedRow();if(view<0)return -1;return table.convertRowIndexToModel(view);}
    private void requireSelect(){JOptionPane.showMessageDialog(this,"Bạn hãy chọn một dòng trong bảng trước.","Chọn bản ghi",JOptionPane.INFORMATION_MESSAGE);}
    private void suaDong(){int i=index();if(i<0){requireSelect();return;}bieuMau(current,i,"Sửa thông tin");}
    private void xoaDong(){int i=index();if(i<0){requireSelect();return;}
        int ok=JOptionPane.showConfirmDialog(this,"Chỉ xóa dòng dữ liệu thử trong phiên chạy, không xóa CSDL. Tiếp tục?","Xóa dữ liệu thử",JOptionPane.YES_NO_OPTION);
        if(ok==JOptionPane.YES_OPTION){KhoDuLieuManHinh.xoa(current,i);hienTab(currentTabIndex);}}
    private void xemChiTiet(){int i=index();if(i<0){requireSelect();return;}
        Map<String,String> data=KhoDuLieuManHinh.lay(current).get(i);
        JTabbedPane detail=new JTabbedPane();
        detail.addTab("Thông tin",bangChiTiet(data));
        if(menu.equals("Quản lý học viên")){
            for(String name:new String[]{"Môn & Khóa","Lớp đang học","Điểm danh","Học phí","Lịch sử"}){
                JPanel panel=card(new BorderLayout(),14);panel.add(duoi(moTaHoSo(name,data)),BorderLayout.NORTH);
                JTextArea text=new JTextArea(ketNoiHoSo(name,data));text.setEditable(false);text.setFont(MauSacUngDung.font(13,false));
                text.setLineWrap(true);text.setWrapStyleWord(true);text.setMargin(new Insets(12,8,12,8));panel.add(new JScrollPane(text),BorderLayout.CENTER);
                detail.addTab(name,panel);
            }
        }
        detail.setPreferredSize(new Dimension(700,430));
        JOptionPane.showMessageDialog(this,detail,"Chi tiết – "+menu+" / "+current.tab(),JOptionPane.PLAIN_MESSAGE);
    }
    private String moTaHoSo(String name,Map<String,String> row){return switch(name){
        case "Môn & Khóa"->"Một môn có nhiều khóa; mỗi khóa có số buổi, phí và lịch sử đăng ký riêng.";
        case "Điểm danh"->"Mầm non theo ngày; Trung tâm theo buổi thực tế, không điểm danh trùng.";
        case "Học phí"->"Theo dõi khoản phải thu, đã đóng, còn nợ theo đúng cơ sở/đăng ký.";
        default->"Thông tin chỉ mang tính minh họa từ bảng và hồ sơ liên quan.";};}
    private String ketNoiHoSo(String name,Map<String,String> row){
        String id=row.getOrDefault("Mã học viên",row.getOrDefault("Mã", ""));
        String n=row.getOrDefault("Họ và tên",row.getOrDefault("Họ tên", "Học viên"));
        StringBuilder sb=new StringBuilder("HỌC VIÊN: "+n+"  •  "+id+"\n\n");
        switch(name){
            case "Môn & Khóa" -> {
                if(coSo==CoSo.MAM_NON)sb.append("Mầm non học theo lớp và năm học; không đăng ký khóa Trung tâm.\n");
                else for(MoTaManHinh.Trang t:MoTaManHinh.lay(coSo,"Quản lý học viên"))if(t.tab().equals(name))
                    for(Map<String,String> r:KhoDuLieuManHinh.lay(t))if(r.getOrDefault("Học viên", "").equals(n))
                        sb.append("• ").append(r.get("Môn học")).append(" / ").append(r.get("Khóa học")).append("\n   Lớp: ").append(r.get("Lớp đang học")).append(" · Đã học ").append(r.get("Đã học")).append("/").append(r.get("Số buổi chuẩn")).append(" buổi\n");
            }
            case "Lớp đang học"->sb.append("Lớp: ").append(row.getOrDefault("Lớp đang học", "Tra cứu từ bảng xếp lớp")).append("\n");
            case "Điểm danh"->sb.append("Điểm danh theo ").append(coSo==CoSo.MAM_NON?"ngày":"buổi").append(". Chọn menu Điểm danh để xem/lưu kết quả mẫu.\n");
            case "Học phí"->sb.append("Chọn menu Học phí để xem khoản phải thu và phiếu thu tương ứng.\n");
            case "Lịch sử"->sb.append("Lịch sử xếp lớp, chuyển lớp và thay đổi cần thiết được giữ riêng.\n");
        }return sb.toString();
    }
    private JComponent bangChiTiet(Map<String,String> data){
        JPanel p=card(new GridBagLayout(),15);GridBagConstraints g=new GridBagConstraints();g.anchor=GridBagConstraints.NORTHWEST;
        g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(7,8,7,8);g.gridy=0;
        for(Map.Entry<String,String> e:data.entrySet()){
            g.gridx=0;g.weightx=.35;p.add(nhan(e.getKey(),13,true,DEEP),g);
            g.gridx=1;g.weightx=.65;p.add(nhan(e.getValue(),13,false,new Color(0x2C3931)),g);g.gridy++;
        }
        return new JScrollPane(p);
    }
    private void thaoTac(String action){
        String a=action.toLowerCase(Locale.ROOT);
        if(a.contains("csv")){xuatCSV();return;}
        if(a.contains("xem")||a.contains("chi tiết")||a.contains("lịch hiện tại")||a.contains("lịch sử")||a.contains("tiến độ")||a.contains("các khóa")||a.contains("số buổi")){
            if(a.contains("lịch")&&menu.equals("Sắp lịch học")){moLichTuan();return;}
            xemChiTiet();return;
        }
        if(a.equals("sửa")||a.startsWith("sửa ")||a.startsWith("điều chỉnh")&&a.contains("ngày chuẩn")){suaDong();return;}
        if(a.contains("phiếu thu")&&a.contains("lập")){lapPhieuThu();return;}
        if(a.contains("gói 1 năm")){dangKyGoiNam();return;}
        if(a.equals("nhập kho")||a.equals("xuất hs")||a.equals("xuất cs")||a.equals("kiểm kê")){xuLyKho(action);return;}
        if(a.contains("chốt tháng")){chotThang();return;}
        if(a.contains("điểm danh")){moDiemDanh();return;}
        if(a.contains("học bù")){xetHocBu();return;}
        if(a.contains("đổi buổi")&&!a.contains("xem")){bieuMau(tabKhac("Học bù / Đổi buổi","Đổi buổi"),-1,action);return;}
        if(a.contains("xếp lớp")||a.contains("chuyển lớp")){
            bieuMau(tabKhac("Quản lý lớp học","Xếp lớp & chuyển lớp"),-1,action);return;}
        if(a.contains("đăng ký khóa")){
            bieuMau(tabKhac("Quản lý khóa học","Đăng ký khóa"),-1,action);return;}
        if(a.contains("cấp quyền")||a.contains("sửa quyền")){
            bieuMau(tabKhac("Hệ thống","Tài khoản & Phân quyền"),-1,action);return;}
        if(a.contains("phân công giáo viên")){
            bieuMau(tabKhac("Quản lý lớp học","Phân công giáo viên"),-1,action);return;}
        bieuMau(current,-1,action);
    }
    private MoTaManHinh.Trang tabKhac(String menu,String tab){for(MoTaManHinh.Trang t:MoTaManHinh.lay(coSo,menu))if(t.tab().equals(tab))return t;return current;}
    private static String nhanThuc(String field){return field.split("[#*]",2)[0].trim();}
    private void bieuMau(MoTaManHinh.Trang spec,int edit,String title){
        if(spec==null){JOptionPane.showMessageDialog(this,"Chức năng này chưa áp dụng tại cơ sở hiện tại.");return;}
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),title+" – "+spec.tab(),Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(750,Math.min(710,Toolkit.getDefaultToolkit().getScreenSize().height-90));dialog.setLocationRelativeTo(this);
        JPanel root=card(new BorderLayout(0,12),18);dialog.setContentPane(root);
        root.add(nhan(title+"  •  "+spec.tab(),21,true,DEEP),BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridBagLayout());fields.setBackground(Color.WHITE);
        fields.setBorder(new EmptyBorder(6,4,6,4));GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(8,9,8,9);
        g.fill=GridBagConstraints.HORIZONTAL;g.anchor=GridBagConstraints.NORTHWEST;
        Map<String,JComponent> controls=new LinkedHashMap<>();
        Map<String,String> old=(edit>=0&&spec==current&&edit<KhoDuLieuManHinh.lay(spec).size())?KhoDuLieuManHinh.lay(spec).get(edit):Map.of();
        for(int i=0;i<spec.truong().length;i++){
            String f=spec.truong()[i],name=nhanThuc(f);
            g.gridx=i%2;g.gridy=i/2*2;g.weightx=0.5;
            fields.add(nhan(name+(f.contains("*")?" *":""),12,true,DEEP),g);
            JComponent input;
            String v=old.getOrDefault(name,"");
            if(f.contains("#")||name.equals("Học viên")||name.equals("Khóa học")||name.equals("Lớp")||name.equals("Lớp học")){
                java.util.List<String> options=new ArrayList<>();
                if(f.contains("#"))options.addAll(Arrays.asList(f.substring(f.indexOf('#')+1).split(",")));
                if(name.equals("Môn học"))for(Map<String,String> r:KhoDuLieuManHinh.lay(tabKhac("Quản lý khóa học","Môn học"))){
                    String x=r.get("Tên môn học");if(x!=null&&!options.contains(x))options.add(x);
                }
                if(name.equals("Học viên"))for(Map<String,String> r:KhoDuLieuManHinh.lay(tabKhac("Quản lý học viên","Danh sách học viên"))){
                    String x=r.get("Họ và tên");if(x!=null&&!options.contains(x))options.add(x);
                }
                if(name.equals("Khóa học"))for(Map<String,String> r:KhoDuLieuManHinh.lay(tabKhac("Quản lý khóa học","Khóa học"))){
                    String x=r.get("Tên khóa");if(x!=null&&!options.contains(x))options.add(x);
                }
                if(name.equals("Lớp")||name.equals("Lớp học"))for(Map<String,String> r:KhoDuLieuManHinh.lay(tabKhac("Quản lý lớp học","Danh sách lớp"))){
                    String x=r.get("Tên lớp");if(x!=null&&!options.contains(x))options.add(x);
                }
                if(options.isEmpty())options.add("Chưa có dữ liệu để chọn");
                JComboBox<String> combo=new JComboBox<>(options.toArray(String[]::new));combo.setFont(MauSacUngDung.font(13,false));combo.setPreferredSize(new Dimension(250,35));
                if(!v.isBlank())combo.setSelectedItem(v);input=combo;
            }else if(name.equals("Ghi chú")||name.equals("Lý do")||name.equals("Mô tả")){
                JTextArea area=new JTextArea(v,3,15);area.setLineWrap(true);area.setWrapStyleWord(true);
                area.setFont(MauSacUngDung.font(13,false));input=new JScrollPane(area);input.putClientProperty("editable",area);
            }else{
                JTextField text=nhap();text.setText(v);
                if(v.isBlank()&&(name.startsWith("Ngày")||name.endsWith("Ngày")||name.contains("Ngày hiệu lực")))text.setText(LocalDate.now().format(DATE));
                input=text;
            }
            g.gridx=i%2;g.gridy=i/2*2+1;g.weighty=0;fields.add(input,g);
            controls.put(name,input);
        }
        JScrollPane scroll=new JScrollPane(fields);scroll.setBorder(null);scroll.getVerticalScrollBar().setUnitIncrement(15);
        root.add(scroll,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setOpaque(false);
        footer.add(nhan("* Trường bắt buộc • Dữ liệu mẫu, không lưu SQL Server",11,false,MUTED),BorderLayout.WEST);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.setOpaque(false);
        JButton cancel=nut("Đóng",false);cancel.addActionListener(e->dialog.dispose());buttons.add(cancel);
        JButton save=nut("Lưu bản mẫu",true);buttons.add(save);
        save.addActionListener(e->{
            Map<String,String> input=new LinkedHashMap<>();
            for(String raw:spec.truong()){
                String name=nhanThuc(raw);JComponent v=controls.get(name);
                String s=giaTri(v).trim();if(raw.contains("*")&&s.isBlank()){
                    JOptionPane.showMessageDialog(dialog,"Vui lòng nhập trường bắt buộc: "+name,"Thiếu dữ liệu",JOptionPane.WARNING_MESSAGE);v.requestFocus();return;}
                if(name.toLowerCase(Locale.ROOT).contains("điện thoại")||name.toLowerCase(Locale.ROOT).contains("số điện thoại")){
                    if(!s.isBlank()&&!s.matches("0[0-9]{9,10}")){
                        JOptionPane.showMessageDialog(dialog,"Số điện thoại phải gồm 10–11 chữ số và bắt đầu bằng 0.");return;}
                }
                if(laSo(name)&&!s.isBlank()&&!s.replace(",","").matches("-?\\d+")){
                    JOptionPane.showMessageDialog(dialog,"Trường "+name+" phải là số nguyên hợp lệ.");return;}
                input.put(name,s);
            }
            Map<String,String> normalized=new LinkedHashMap<>(old);
            normalized.putAll(input);
            for(String col:spec.cot()){
                if(!normalized.containsKey(col)){
                    String translated=tenDongNghia(col,input);
                    if(translated!=null)normalized.put(col,translated);
                }
            }
            if(edit>=0&&spec==current)KhoDuLieuManHinh.sua(spec,edit,normalized);
            else KhoDuLieuManHinh.them(spec,normalized);
            dialog.dispose();
            if(spec==current)hienTab(currentTabIndex);
            else JOptionPane.showMessageDialog(this,"Đã thêm dữ liệu DEMO vào "+spec.menu()+" → "+spec.tab()+". Mở tab tương ứng để xem.","Lưu dữ liệu mẫu",JOptionPane.INFORMATION_MESSAGE);
        });
        footer.add(buttons,BorderLayout.EAST);root.add(footer,BorderLayout.SOUTH);dialog.setVisible(true);
    }
    private static String tenDongNghia(String col,Map<String,String> input){
        for(String key:input.keySet())if(key.toLowerCase(Locale.ROOT).replace(" ","").equals(col.toLowerCase(Locale.ROOT).replace(" ","")))return input.get(key);
        return switch(col){
            case "Họ và tên","Họ tên","Học viên"->input.getOrDefault("Họ và tên",input.getOrDefault("Học viên",input.get("Họ tên")));
            case "Tên lớp"->input.get("Tên lớp");
            case "Giờ học","Khung giờ","Lịch cố định"->input.containsKey("Giờ bắt đầu")?input.get("Giờ bắt đầu")+"–"+input.getOrDefault("Giờ kết thúc",""):null;
            case "Ngày thu"->input.get("Ngày thu");
            case "Phải thu"->input.get("Phải thu");
            case "Còn nợ"->input.get("Còn nợ");
            case "Lớp đang học"->input.getOrDefault("Lớp",input.get("Lớp đang học"));
            case "Mã SP"->input.getOrDefault("Mã SP",input.get("Mã hàng hóa"));
            default->null;
        };
    }
    private static boolean laSo(String x){String s=x.toLowerCase(Locale.ROOT);return s.contains("tiền")||s.contains("học phí")||s.contains("giảm giá")||s.contains("số buổi")||s.contains("số ngày")||s.contains("sĩ số")||s.contains("đơn giá")||s.contains("phải thu")||s.contains("đã thu")||s.contains("đã đóng")||s.contains("số lượng")||s.contains("tồn ")||s.contains("khấu trừ")||s.contains("số phát sinh")||s.contains("mức riêng");}
    private static String giaTri(JComponent c){
        if(c instanceof JComboBox<?> combo)return String.valueOf(combo.getSelectedItem());
        if(c instanceof JTextField field)return field.getText();
        if(c instanceof JScrollPane p&&p.getClientProperty("editable") instanceof JTextArea text)return text.getText();
        return "";
    }
    private void moLichTuan(){
        String[] cols={"Khung giờ","Thứ 2","Thứ 3","Thứ 4","Thứ 5","Thứ 6","Thứ 7"};
        String[][] rows={
          {"08:00–09:30","Tiếng Anh A1\nKid 1","Tiếng Anh A2","Kid 1\nAbacus","Tiếng Anh A1","Tiếng Anh A2","Kid 2"},
          {"10:00–11:30","Abacus 1","Kid 2\nTiếng Anh B1","Tiếng Anh A1","Kid 1","Abacus 2","Kid 2"},
          {"14:00–15:30","Kid 1","Tiếng Anh A1","Kid 1\nAbacus","Tiếng Anh A2","Kid 2","Tiếng Anh B1"},
          {"16:00–17:30","Tiếng Anh A1\nKid 2","Toán tư duy K1","Tiếng Anh A2\nAbacus","Toán tư duy K2","Tiếng Anh B1","Kid 1"},
          {"18:00–19:30","Tiếng Anh A2","Kid 1","Tiếng Anh B1","Kid 2","Abacus 1","Kid 2"}
        };
        JTable grid=new JTable(rows,cols);grid.setRowHeight(80);grid.setFont(MauSacUngDung.font(13,false));grid.getTableHeader().setFont(MauSacUngDung.font(12,true));
        grid.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean focus,int r,int c){
                JTextArea a=new JTextArea(String.valueOf(v));a.setWrapStyleWord(true);a.setLineWrap(true);a.setFont(MauSacUngDung.font(13,c==0));
                a.setBackground(sel?new Color(0xDBF7D7):r%2==0?Color.WHITE:new Color(0xF2FAF2));a.setBorder(new EmptyBorder(14,10,8,6));return a;
            }
        });
        JScrollPane p=new JScrollPane(grid);p.setPreferredSize(new Dimension(900,490));
        JOptionPane.showMessageDialog(this,p,"Lịch tuần – nhiều lớp trong cùng khung giờ (mẫu)",JOptionPane.PLAIN_MESSAGE);
    }
    private void xuatCSV(){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("VINABACUS_"+menu.replaceAll("[^a-zA-Z]","")+"_mau.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        File target=chooser.getSelectedFile();
        try(Writer writer=new OutputStreamWriter(new FileOutputStream(target),StandardCharsets.UTF_8)){
            writer.write('\ufeff');
            List<String[]> rows=new ArrayList<>();rows.add(current.cot());
            for(Map<String,String> m:KhoDuLieuManHinh.lay(current)){
                String[] values=new String[current.cot().length];for(int i=0;i<values.length;i++)values[i]=m.getOrDefault(current.cot()[i],"");rows.add(values);
            }
            for(String[] row:rows){for(int j=0;j<row.length;j++){
                if(j>0)writer.write(',');writer.write('"');writer.write(row[j].replace("\"","\"\""));writer.write('"');
            }writer.write("\r\n");}
            JOptionPane.showMessageDialog(this,"Đã xuất CSV mẫu:\n"+target.getAbsolutePath()+"\nLưu ý: chưa phải Excel theo mẫu tồn kho của cơ sở.");
        }catch(IOException ex){JOptionPane.showMessageDialog(this,"Không thể xuất tệp: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);}
    }
    /** Điểm danh nhóm lớp/buổi. Khóa duy nhất trong phiên: cơ sở + ngày + lớp/buổi + học viên. */
    private void moDiemDanh(){
        JDialog d=new JDialog(SwingUtilities.getWindowAncestor(this),"Điểm danh "+coSo.layTen()+" (mẫu)",Dialog.ModalityType.APPLICATION_MODAL);
        d.setSize(1020,710);d.setLocationRelativeTo(this);
        JPanel root=card(new BorderLayout(12,12),18);d.setContentPane(root);
        JPanel top=new JPanel(new GridLayout(2,1,7,7));top.setOpaque(false);
        top.add(nhan(coSo==CoSo.MAM_NON?"Điểm danh theo ngày":"Điểm danh theo buổi học",22,true,DEEP));
        JPanel selectors=new JPanel(new FlowLayout(FlowLayout.LEFT,12,3));selectors.setOpaque(false);
        JTextField date=nhap();date.setColumns(12);date.setText(LocalDate.now().format(DATE));
        JComboBox<String> lop=new JComboBox<>();
        for(Map<String,String> x:KhoDuLieuManHinh.lay(tabKhac("Quản lý lớp học","Danh sách lớp")))
            lop.addItem(x.getOrDefault("Tên lớp","Lớp học"));
        JComboBox<String> buoi=new JComboBox<>(coSo==CoSo.MAM_NON?new String[]{"Cả ngày"}:new String[]{"08:00–09:30","10:00–11:30","14:00–15:30","16:00–17:30","18:00–19:30"});
        selectors.add(nhan("Ngày:",13,true,DEEP));selectors.add(date);
        selectors.add(nhan("Lớp:",13,true,DEEP));selectors.add(lop);
        if(coSo==CoSo.TRUNG_TAM){selectors.add(nhan("Buổi:",13,true,DEEP));selectors.add(buoi);}
        top.add(selectors);root.add(top,BorderLayout.NORTH);
        MoTaManHinh.Trang students=tabKhac("Quản lý học viên","Danh sách học viên");
        List<Map<String,String>> hs=new ArrayList<>(KhoDuLieuManHinh.lay(students));
        for(int i=hs.size();i<8;i++){
            Map<String,String> p=new LinkedHashMap<>();p.put("Mã học viên",String.format("HV%03d",i+1));
            p.put("Họ và tên",new String[]{"Lê Gia Hân","Võ Nhật Minh","Phạm Tuấn Khang","Đỗ Thảo Vy","Nguyễn Hoài An","Trần Minh Khoa"}[i%6]);hs.add(p);
        }
        String[] statuses={"Chưa điểm danh","Có mặt","Vắng có phép","Vắng không phép"};
        DefaultTableModel model=new DefaultTableModel(new Object[]{"Mã học viên","Họ và tên","Trạng thái điểm danh","Ghi chú"},0){
            @Override public boolean isCellEditable(int r,int c){return c>=2;}
        };
        JTable grid=new JTable(model);grid.setRowHeight(43);grid.setFont(MauSacUngDung.font(13,false));
        grid.getTableHeader().setFont(MauSacUngDung.font(13,true));
        grid.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(statuses)));
        grid.getColumnModel().getColumn(2).setPreferredWidth(180);grid.getColumnModel().getColumn(3).setPreferredWidth(250);
        JLabel stats=nhan("",13,true,DEEP);
        Runnable load=()->{
            model.setRowCount(0);
            for(Map<String,String> p:hs){
                String key=keyDiemDanh(date.getText(),String.valueOf(lop.getSelectedItem()),String.valueOf(buoi.getSelectedItem()),p.get("Mã học viên"));
                String status=KhoDuLieuManHinh.layDiemDanh(key);
                model.addRow(new Object[]{p.get("Mã học viên"),p.get("Họ và tên"),status==null?"Chưa điểm danh":status,""});
            }
            demDiemDanh(model,stats);
        };
        lop.addActionListener(e->load.run());buoi.addActionListener(e->load.run());date.addActionListener(e->load.run());
        model.addTableModelListener(e->demDiemDanh(model,stats));load.run();
        JPanel mid=card(new BorderLayout(0,9),11);mid.add(new JScrollPane(grid),BorderLayout.CENTER);mid.add(stats,BorderLayout.SOUTH);
        root.add(mid,BorderLayout.CENTER);
        JPanel foot=new JPanel(new BorderLayout());foot.setOpaque(false);
        foot.add(duoi("Có mặt / Vắng có phép / Vắng không phép / Chưa điểm danh. Mỗi học viên chỉ có 1 kết quả trong một ngày (Mầm non) hoặc một buổi (Trung tâm)."),BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.setOpaque(false);
        JButton close=nut("Đóng",false);close.addActionListener(e->d.dispose());buttons.add(close);
        JButton save=nut("Lưu điểm danh mẫu",true);save.addActionListener(e->{
            if(!date.getText().matches("\\d{2}/\\d{2}/\\d{4}")){JOptionPane.showMessageDialog(d,"Ngày cần theo mẫu dd/MM/yyyy.");return;}
            try{LocalDate.parse(date.getText(),DATE);}catch(Exception ex){JOptionPane.showMessageDialog(d,"Ngày không hợp lệ.");return;}
            if(coSo==CoSo.MAM_NON){
                MoTaManHinh.Trang chot=tabKhac("Học phí & Tiền ăn","Chốt tháng");
                if(KhoDuLieuManHinh.lay(chot).stream().anyMatch(r->"Đã chốt".equals(r.get("Trạng thái")))){
                    JOptionPane.showMessageDialog(d,"Tháng mẫu đã chốt. Vì chưa có đăng nhập xác thực Admin nên giao diện thử không cho sửa điểm danh sau chốt.");return;
                }
            }
            if(grid.isEditing())grid.getCellEditor().stopCellEditing();
            int completed=0;
            for(int i=0;i<model.getRowCount();i++){
                String status=String.valueOf(model.getValueAt(i,2));
                String key=keyDiemDanh(date.getText(),String.valueOf(lop.getSelectedItem()),String.valueOf(buoi.getSelectedItem()),String.valueOf(model.getValueAt(i,0)));
                KhoDuLieuManHinh.diemDanh(key,status);if(!status.equals("Chưa điểm danh"))completed++;
                MoTaManHinh.Trang spec=tabKhac("Điểm danh","Bảng điểm danh");
                List<LinkedHashMap<String,String>> history=KhoDuLieuManHinh.lay(spec);
                String hv=String.valueOf(model.getValueAt(i,0));
                String ngay=date.getText(),lopHoc=String.valueOf(lop.getSelectedItem());
                String buoiText=coSo==CoSo.MAM_NON?"Cả ngày":String.valueOf(buoi.getSelectedItem());
                Map<String,String> found=null;
                for(Map<String,String> record:history)if(hv.equals(record.get("Mã học viên"))&&ngay.equals(record.get("Ngày"))&&lopHoc.equals(record.get("Lớp"))&&buoiText.equals(record.get("Buổi"))){found=record;break;}
                if(found==null){found=new LinkedHashMap<>();history.add(new LinkedHashMap<>());found=history.get(history.size()-1);}
                found.put("Ngày",ngay);found.put("Mã học viên",hv);found.put("Học viên",String.valueOf(model.getValueAt(i,1)));
                found.put("Lớp",lopHoc);found.put("Buổi",buoiText);found.put("Trạng thái",status);
                found.put("Ghi chú",String.valueOf(model.getValueAt(i,3)));
            }
            JOptionPane.showMessageDialog(d,"Đã lưu "+model.getRowCount()+" trạng thái điểm danh vào bộ nhớ thử.\nĐã điểm danh: "+completed+". Không ghi trùng khóa buổi/học viên.");
        });buttons.add(save);foot.add(buttons,BorderLayout.SOUTH);root.add(foot,BorderLayout.SOUTH);d.setVisible(true);
    }
    private String keyDiemDanh(String ngay,String lop,String buoi,String hv){return coSo.name()+"|"+ngay+"|"+lop+"|"+(coSo==CoSo.MAM_NON?"NGAY":buoi)+"|"+hv;}
    private void demDiemDanh(DefaultTableModel m,JLabel label){
        int[] a=new int[4];for(int i=0;i<m.getRowCount();i++){
            String s=String.valueOf(m.getValueAt(i,2));if(s.equals("Có mặt"))a[0]++;else if(s.equals("Vắng có phép"))a[1]++;else if(s.equals("Vắng không phép"))a[2]++;else a[3]++;
        }
        label.setText("Có mặt: "+a[0]+"     Vắng có phép: "+a[1]+"     Vắng không phép: "+a[2]+"     Chưa điểm danh: "+a[3]);
    }
    /** Đăng ký gói năm thử: tổng nhiều khóa và lấy một ưu đãi lớn nhất. */
    private void dangKyGoiNam(){
        if(coSo!=CoSo.TRUNG_TAM){JOptionPane.showMessageDialog(this,"Gói học năm chỉ áp dụng tại Trung tâm.");return;}
        MoTaManHinh.Trang courses=tabKhac("Quản lý khóa học","Khóa học");
        List<LinkedHashMap<String,String>> list=KhoDuLieuManHinh.lay(courses);
        if(list.size()<2){JOptionPane.showMessageDialog(this,"Cần có ít nhất 2 khóa trong danh mục.");return;}
        JPanel all=card(new BorderLayout(10,10),15);
        JComboBox<String> student=new JComboBox<>(new String[]{"Nguyễn Minh Anh","Trần Bảo Ngọc"});
        JTextField start=nhap(),end=nhap();start.setText(LocalDate.now().format(DATE));end.setText(LocalDate.now().plusYears(1).format(DATE));
        JPanel head=new JPanel(new GridLayout(3,2,9,9));head.setOpaque(false);
        head.add(nhan("Học viên",12,true,DEEP));head.add(student);
        head.add(nhan("Bắt đầu",12,true,DEEP));head.add(start);
        head.add(nhan("Kết thúc cam kết",12,true,DEEP));head.add(end);all.add(head,BorderLayout.NORTH);
        JPanel checks=new JPanel();checks.setOpaque(false);checks.setLayout(new BoxLayout(checks,BoxLayout.Y_AXIS));
        List<JCheckBox> checkboxes=new ArrayList<>();List<Long> values=new ArrayList<>();
        for(Map<String,String> row:list){long fee=docSo(row.get("Học phí mặc định"));values.add(fee);
            JCheckBox c=new JCheckBox(row.get("Tên khóa")+"  •  "+dinhDangTien(fee));c.setFont(MauSacUngDung.font(13,false));c.setBackground(Color.WHITE);
            checks.add(c);checks.add(Box.createVerticalStrut(6));checkboxes.add(c);
        }
        JPanel center=card(new BorderLayout(4,9),10);center.add(nhan("Chọn các khóa nối tiếp thuộc lộ trình năm",14,true,DEEP),BorderLayout.NORTH);
        center.add(new JScrollPane(checks),BorderLayout.CENTER);
        JComboBox<String> discount=new JComboBox<>(new String[]{"Tự chọn ưu đãi cao nhất (10% hoặc 500.000đ)","Ưu đãi 10%", "Ưu đãi 500.000đ", "Không áp dụng"});
        JLabel calc=nhan("Chọn các khóa để xem tổng phí.",13,true,DEEP);
        JPanel south=new JPanel(new GridLayout(2,1,7,7));south.setOpaque(false);south.add(discount);south.add(calc);
        center.add(south,BorderLayout.SOUTH);all.add(center,BorderLayout.CENTER);
        Runnable recalc=()->{
            long total=0;int n=0;for(int i=0;i<checkboxes.size();i++)if(checkboxes.get(i).isSelected()){total+=values.get(i);n++;}
            long ten=Math.round(total*.1);long fixed=Math.min(total,500000);
            long off=switch(discount.getSelectedIndex()){case 1->ten;case 2->fixed;case 3->0;default->Math.max(ten,fixed);};
            calc.setText(n+" khóa  •  Gốc "+dinhDangTien(total)+"  •  Giảm "+dinhDangTien(off)+"  •  Phải thu "+dinhDangTien(total-off));
        };
        for(JCheckBox c:checkboxes)c.addActionListener(e->recalc.run());discount.addActionListener(e->recalc.run());
        all.setPreferredSize(new Dimension(760,485));int ok=JOptionPane.showConfirmDialog(this,all,"Đăng ký gói năm – bản tính mẫu",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(ok!=JOptionPane.OK_OPTION)return;
        int n=0;long total=0;StringJoiner modules=new StringJoiner(", ");
        for(int i=0;i<checkboxes.size();i++)if(checkboxes.get(i).isSelected()){n++;total+=values.get(i);modules.add(list.get(i).get("Tên khóa"));}
        if(n<2){JOptionPane.showMessageDialog(this,"Hãy chọn ít nhất 2 khóa cho gói lộ trình.");return;}
        try{LocalDate a=LocalDate.parse(start.getText(),DATE),b=LocalDate.parse(end.getText(),DATE);
            if(!b.isAfter(a)){JOptionPane.showMessageDialog(this,"Ngày kết thúc phải sau ngày bắt đầu.");return;}
        }catch(Exception ex){JOptionPane.showMessageDialog(this,"Ngày phải hợp lệ, định dạng dd/MM/yyyy.");return;}
        long ten=Math.round(total*.1),fixed=Math.min(total,500000);
        long off=switch(discount.getSelectedIndex()){case 1->ten;case 2->fixed;case 3->0;default->Math.max(ten,fixed);};
        MoTaManHinh.Trang pkg=tabKhac("Quản lý khóa học","Gói học 1 năm");
        Map<String,String> newRow=new LinkedHashMap<>();newRow.put("Học viên",String.valueOf(student.getSelectedItem()));
        newRow.put("Các khóa trong lộ trình",modules.toString());newRow.put("Ngày bắt đầu",start.getText());newRow.put("Ngày kết thúc",end.getText());
        newRow.put("Học phí gốc",String.valueOf(total));newRow.put("Giảm giá",String.valueOf(off));newRow.put("Phải thu",String.valueOf(total-off));newRow.put("Trạng thái","Đăng ký");
        KhoDuLieuManHinh.them(pkg,newRow);
        JOptionPane.showMessageDialog(this,"Đã tạo gói năm DEMO.\nGốc: "+dinhDangTien(total)+"\nGiảm một ưu đãi cao nhất: "+dinhDangTien(off)+
            "\nPhải thu: "+dinhDangTien(total-off)+"\n\nLưu ý: chưa phân bổ thành công nợ/đăng ký khóa SQL thật.");
        if(current==pkg)hienTab(currentTabIndex);
    }
    /** Phiếu thu thử cho phép chọn nhiều khoản nợ và phân bổ tuần tự không vượt thực thu. */
    private void lapPhieuThu(){
        boolean mn=coSo==CoSo.MAM_NON;
        MoTaManHinh.Trang debts=tabKhac(mn?"Học phí & Tiền ăn":"Học phí",mn?"Bảng tính phí tháng":"Khoản phải thu");
        List<LinkedHashMap<String,String>> rows=KhoDuLieuManHinh.lay(debts);
        String[] names={"Nguyễn Minh Anh","Trần Bảo Ngọc"};
        JComboBox<String> student=new JComboBox<>(names);
        JTextField amount=nhap();amount.setText("3000000");
        JComboBox<String> method=new JComboBox<>(new String[]{"Chuyển khoản","Tiền mặt"});
        JPanel form=card(new GridLayout(3,2,8,8),15);
        form.add(nhan("Học viên",12,true,DEEP));form.add(student);
        form.add(nhan("Số tiền thực thu",12,true,DEEP));form.add(amount);
        form.add(nhan("Hình thức",12,true,DEEP));form.add(method);
        JPanel choice=new JPanel();choice.setOpaque(false);choice.setLayout(new BoxLayout(choice,BoxLayout.Y_AXIS));
        List<JCheckBox> checks=new ArrayList<>();
        for(Map<String,String> row:rows){
            String detail=mn?row.getOrDefault("Tháng","Tháng phí"):row.getOrDefault("Đăng ký khóa","Đăng ký khóa");
            long due=Math.max(0,docSo(row.getOrDefault(mn?"Phải thu":"Phải thu","0"))-docSo(row.getOrDefault(mn?"Đã thu":"Đã đóng","0")));
            JCheckBox c=new JCheckBox(detail+"  •  Còn nợ: "+dinhDangTien(due),true);c.setBackground(Color.WHITE);
            checks.add(c);choice.add(c);choice.add(Box.createVerticalStrut(10));
        }
        JPanel all=card(new BorderLayout(9,9),15);all.add(form,BorderLayout.NORTH);all.add(new JScrollPane(choice),BorderLayout.CENTER);
        JLabel explain=nhan("Thu dư thực tế mới được ghi nhận là tín dụng. Không trừ nợ bằng giảm giá ảo.",12,false,MUTED);all.add(explain,BorderLayout.SOUTH);
        all.setPreferredSize(new Dimension(720,330));int ok=JOptionPane.showConfirmDialog(this,all,"Lập phiếu thu nhiều khoản (mẫu)",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(ok!=JOptionPane.OK_OPTION)return;
        long money=docSo(amount.getText());if(money<=0){JOptionPane.showMessageDialog(this,"Số tiền thu phải lớn hơn 0.");return;}
        long remaining=money, allocated=0;StringJoiner allocatedNames=new StringJoiner(", ");
        for(int i=0;i<checks.size();i++)if(checks.get(i).isSelected()){
            Map<String,String> row=rows.get(i);String totalKey="Phải thu",paidKey=mn?"Đã thu":"Đã đóng";
            long owed=Math.max(0,docSo(row.get(totalKey))-docSo(row.get(paidKey)));
            long use=Math.min(owed,remaining);if(use<=0)continue;
            row.put(paidKey,String.valueOf(docSo(row.get(paidKey))+use));
            row.put("Còn nợ",String.valueOf(Math.max(0,owed-use)));if(!mn)row.put("Trạng thái",owed-use>0?"Thiếu":"Đủ");
            allocated+=use;remaining-=use;allocatedNames.add(mn?row.getOrDefault("Tháng","Tháng"):row.getOrDefault("Đăng ký khóa","Khóa"));
        }
        if(allocated==0){JOptionPane.showMessageDialog(this,"Hãy chọn ít nhất một khoản đang còn nợ.");return;}
        MoTaManHinh.Trang receipts=tabKhac(mn?"Học phí & Tiền ăn":"Học phí","Phiếu thu");
        Map<String,String> r=new LinkedHashMap<>();r.put("Ngày thu",LocalDate.now().format(DATE));
        r.put("Học viên",String.valueOf(student.getSelectedItem()));r.put(mn?"Thực thu":"Số tiền thực thu",String.valueOf(money));
        r.put("Hình thức",String.valueOf(method.getSelectedItem()));r.put("Các khoản phân bổ",allocatedNames.toString());r.put("Người thu","Admin (demo)");r.put("Trạng thái","Đã thu");
        KhoDuLieuManHinh.them(receipts,r);
        if(remaining>0&&!mn){
            MoTaManHinh.Trang credit=tabKhac("Học phí","Tín dụng học phí");
            Map<String,String> cr=new LinkedHashMap<>();cr.put("Học viên",r.get("Học viên"));cr.put("Nguồn tín dụng","Thu dư phiếu mẫu");
            cr.put("Số phát sinh",String.valueOf(remaining));cr.put("Đã sử dụng","0");cr.put("Còn lại",String.valueOf(remaining));
            cr.put("Ngày cập nhật",LocalDate.now().format(DATE));KhoDuLieuManHinh.them(credit,cr);
        }
        JOptionPane.showMessageDialog(this,"Phiếu thu DEMO đã ghi trong RAM.\nĐã phân bổ: "+dinhDangTien(allocated)+"\nThu dư: "+dinhDangTien(remaining)+
            "\nKhông có thao tác trên SQL Server.\nViệc lưu chứng từ thật phải sử dụng SQL transaction.");
        hienTab(currentTabIndex);
    }
    private void xuLyKho(String action){
        MoTaManHinh.Trang stock=tabKhac("Kho – Dụng cụ","Tồn kho theo tháng");
        List<LinkedHashMap<String,String>> products=KhoDuLieuManHinh.lay(stock);
        if(products.isEmpty()){JOptionPane.showMessageDialog(this,"Chưa có hàng hóa.");return;}
        JComboBox<String> selector=new JComboBox<>();
        for(Map<String,String> p:products)selector.addItem(p.get("Mã SP")+" – "+p.get("Tên hàng hóa"));
        JTextField qty=nhap(),note=nhap();qty.setText("1");
        JPanel p=card(new GridLayout(3,2,12,12),15);
        p.add(nhan("Hàng hóa",13,true,DEEP));p.add(selector);
        p.add(nhan(action.equals("Kiểm kê")?"Tồn thực tế":"Số lượng",13,true,DEEP));p.add(qty);
        p.add(nhan("Ghi chú",13,true,DEEP));p.add(note);
        int ok=JOptionPane.showConfirmDialog(this,p,action+" – "+coSo.layTen(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(ok!=JOptionPane.OK_OPTION)return;
        if(!qty.getText().trim().matches("\\d+")){JOptionPane.showMessageDialog(this,"Số lượng phải là số nguyên không âm.");return;}
        long number=docSo(qty.getText());if(!action.equals("Kiểm kê")&&number==0){JOptionPane.showMessageDialog(this,"Số lượng cần lớn hơn 0.");return;}
        Map<String,String> row=products.get(selector.getSelectedIndex());long current=docSo(row.get("Tồn cuối"));
        long after=switch(action){case "Nhập kho"->current+number;case "Xuất HS","Xuất CS"->current-number;default->number;};
        if(after<0){JOptionPane.showMessageDialog(this,"Tồn kho không đủ. Tồn hiện tại: "+current);return;}
        String col=action.equals("Nhập kho")?"Nhập":action.equals("Xuất HS")?"Xuất HS":action.equals("Xuất CS")?"Xuất CS":"Kiểm kê";
        if(action.equals("Kiểm kê"))row.put("Kiểm kê",String.valueOf(number-current));
        else row.put(col,String.valueOf(docSo(row.get(col))+number));
        row.put("Tồn cuối",String.valueOf(after));
        MoTaManHinh.Trang history=tabKhac("Kho – Dụng cụ","Lịch sử nhập xuất");
        Map<String,String> movement=new LinkedHashMap<>();movement.put("Ngày",LocalDate.now().format(DATE));
        movement.put("Mã SP",row.get("Mã SP"));movement.put("Tên hàng hóa",row.get("Tên hàng hóa"));
        movement.put("Loại",col);movement.put("Số lượng",String.valueOf(number));movement.put("Cơ sở",coSo.layTen());
        movement.put("Tài khoản","Admin (demo)");movement.put("Ghi chú",note.getText());KhoDuLieuManHinh.them(history,movement);
        JOptionPane.showMessageDialog(this,"Đã cập nhật tồn DEMO: "+row.get("Tên hàng hóa")+" → "+after+"\nKhông ghi vào SQL Server.");
        hienTab(currentTabIndex);
    }
    private void xetHocBu(){
        if(coSo==CoSo.MAM_NON){JOptionPane.showMessageDialog(this,"Học bù theo khóa chỉ áp dụng cho Trung tâm.");return;}
        JComboBox<String> student=new JComboBox<>(new String[]{"Nguyễn Minh Anh","Trần Bảo Ngọc"});
        JComboBox<String> course=new JComboBox<>(new String[]{"Tiếng Anh A1","Tiếng Anh A2","Toán tư duy Kid 1"});
        JComboBox<String> abs=new JComboBox<>(new String[]{"Vắng có phép","Vắng không phép"});
        JTextField completed=nhap(),date=nhap(),reason=nhap();completed.setText("0");date.setText(LocalDate.now().plusDays(2).format(DATE));
        JPanel p=card(new GridLayout(6,2,9,9),12);
        p.add(nhan("Học viên",12,true,DEEP));p.add(student);p.add(nhan("Đăng ký khóa",12,true,DEEP));p.add(course);
        p.add(nhan("Trạng thái buổi nghỉ",12,true,DEEP));p.add(abs);
        p.add(nhan("Số buổi đã bù",12,true,DEEP));p.add(completed);
        p.add(nhan("Ngày học bù",12,true,DEEP));p.add(date);p.add(nhan("Lý do",12,true,DEEP));p.add(reason);
        int ok=JOptionPane.showConfirmDialog(this,p,"Xét học bù theo đăng ký khóa (mẫu)",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(ok!=JOptionPane.OK_OPTION)return;
        if(!"Vắng có phép".equals(abs.getSelectedItem())){JOptionPane.showMessageDialog(this,"Chỉ vắng CÓ PHÉP mới được học bù.");return;}
        if(!completed.getText().matches("[0-9]+")||Integer.parseInt(completed.getText())>=3){
            JOptionPane.showMessageDialog(this,"Tối đa 3 buổi học bù cho mỗi đăng ký khóa. Không còn lượt hợp lệ.");return;}
        if(reason.getText().isBlank()){JOptionPane.showMessageDialog(this,"Vui lòng nhập lý do.");return;}
        MoTaManHinh.Trang tab=tabKhac("Học bù / Đổi buổi","Xét học bù");
        Map<String,String> r=new LinkedHashMap<>();r.put("Học viên",String.valueOf(student.getSelectedItem()));
        r.put("Đăng ký khóa",String.valueOf(course.getSelectedItem()));r.put("Buổi vắng",LocalDate.now().format(DATE));r.put("Buổi bù",date.getText());
        r.put("Đã bù/3",completed.getText()+"/3");r.put("Lý do",reason.getText());r.put("Trạng thái","Chờ xét");
        KhoDuLieuManHinh.them(tab,r);JOptionPane.showMessageDialog(this,"Đã tạo yêu cầu học bù DEMO; cần đối chiếu điểm danh thật khi kết nối CSDL.");
        hienTab(currentTabIndex);
    }
    private static long docSo(String s){if(s==null)return 0;try{return Long.parseLong(s.replaceAll("[^0-9-]",""));}catch(Exception ex){return 0;}}
    private static String dinhDangTien(long v){return NumberFormat.getIntegerInstance(new Locale("vi","VN")).format(v)+" đ";}

    private void chotThang(){
        int n=JOptionPane.showConfirmDialog(this,"Mô phỏng chốt tháng Mầm non.\nSau chốt: chỉ Admin sửa điểm danh kèm lý do, không thay phiếu thu đã chốt.\nBạn có muốn đánh dấu kỳ mẫu là đã chốt?","Chốt tháng bản mẫu",JOptionPane.YES_NO_OPTION);
        if(n!=JOptionPane.YES_OPTION)return;
        MoTaManHinh.Trang t=tabKhac("Học phí & Tiền ăn","Chốt tháng");
        if(!KhoDuLieuManHinh.lay(t).isEmpty())KhoDuLieuManHinh.lay(t).get(0).put("Trạng thái","Đã chốt");
        hienTab(currentTabIndex);
    }
    private static String html(String x){return x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
