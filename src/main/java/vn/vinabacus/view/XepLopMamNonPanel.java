package vn.vinabacus.view;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.*;
import vn.vinabacus.repository.MamNonRepository.DuKienChuyen;
import vn.vinabacus.tv4.NamHocMacDinh;

/** Xem theo tab lớp, chuyển lớp và chuyển hàng loạt giữa các năm học. */
public final class XepLopMamNonPanel extends JPanel {
    private final MamNonController ctrl=new MamNonController();
    private final JComboBox<NamHocMamNon> year=new JComboBox<>();
    // Thanh nút lọc lớp: không sử dụng JTabbedPane vì phần content rỗng gây ra dải trắng.
    private final JPanel tabBar=new JPanel(new FlowLayout(FlowLayout.LEFT,6,3));
    private final JScrollPane tabScroll=new JScrollPane(tabBar,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private final List<JButton> tabButtons=new ArrayList<>();
    private int selectedClassId=0; // 0 = Tất cả; các giá trị khác là mã lớp thật.
    private static final Color TAB_GREEN=new Color(0,106,54);
    private static final Color TAB_HOVER=new Color(230,247,235);
    private final DefaultTableModel m=MamNonUi.model("Mã xếp lớp","Mã HS","Họ tên","Lớp","Từ ngày","Đến ngày","Trạng thái");
    private final JTable table=MamNonUi.bang(m);
    private final JLabel status=new JLabel("Sẵn sàng");
    private List<LopMamNon> classes=List.of();
    private List<XepLopMamNon> data=List.of(),visible=List.of();
    private boolean loading=false;
    private final PropertyChangeListener yearListener=e->SwingUtilities.invokeLater(()->loadYears(true));

    public XepLopMamNonPanel(){
        super(new BorderLayout(10,6));setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        JButton move=MamNonUi.nut("Chuyển lớp",true),finish=MamNonUi.nut("Ngừng học tại lớp",false);
        JButton bulk=MamNonUi.nut("Chuyển sang năm học mới",false),reload=MamNonUi.nut("Làm mới",false);
        MamNonUi.coCongCu(year,215);MamNonUi.coCongCu(move,125);
        MamNonUi.coCongCu(finish,195);MamNonUi.coCongCu(bulk,235);
        MamNonUi.coCongCu(reload,110);
        JPanel top=new JPanel(new BorderLayout(0,8));
        top.add(MamNonUi.tieuDe("XẾP LỚP HỌC SINH MẦM NON"),BorderLayout.NORTH);
        top.add(MamNonUi.thanhCongCu(new JLabel("Năm học:"),year,move,finish,bulk,reload),BorderLayout.CENTER);
        // Chỉ chiếm chiều cao của hàng tab (không còn vùng trắng ẩn của JTabbedPane).
        tabBar.setOpaque(false);
        tabScroll.setOpaque(false);
        tabScroll.getViewport().setOpaque(false);
        tabScroll.setBorder(BorderFactory.createEmptyBorder());
        tabScroll.setPreferredSize(new Dimension(100,50));
        // Nếu nhiều lớp hơn chiều ngang, vẫn cho cuộn và cấp đủ chỗ cho thanh cuộn.
        tabScroll.addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override public void componentResized(java.awt.event.ComponentEvent e){
                int width=tabScroll.getViewport().getWidth();
                boolean overflow=width>0&&tabBar.getPreferredSize().width>width;
                int height=overflow?68:50;
                if(tabScroll.getPreferredSize().height!=height){
                    tabScroll.setPreferredSize(new Dimension(100,height));tabScroll.revalidate();
                }
            }
        });
        tabScroll.getHorizontalScrollBar().setUnitIncrement(28);
        top.add(tabScroll,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);add(status,BorderLayout.SOUTH);
        year.addActionListener(e->{if(!loading)reloadClasses();});
        reload.addActionListener(e->loadYears(false));
        move.addActionListener(e->moveOne());finish.addActionListener(e->finish());bulk.addActionListener(e->bulk());
        SwingUtilities.invokeLater(()->loadYears(true));
    }
    @Override public void addNotify(){super.addNotify();NamHocMacDinh.langNghe(yearListener);}
    @Override public void removeNotify(){NamHocMacDinh.ngungNghe(yearListener);super.removeNotify();}
    private void loadYears(boolean useDefault){
        NamHocMamNon current=(NamHocMamNon)year.getSelectedItem();
        MamNonUi.tai(this,ctrl::namHoc,rows->{
            loading=true;year.removeAllItems();for(NamHocMamNon y:rows)year.addItem(y);
            NamHocMamNon desired=useDefault?NamHocMacDinh.chonHieuLuc(rows):current;
            if(desired!=null)for(int i=0;i<year.getItemCount();i++)if(year.getItemAt(i).maNamHoc()==desired.maNamHoc()){
                year.setSelectedIndex(i);break;
            }
            loading=false;reloadClasses();
        });
    }
    private int currentClassId(){return selectedClassId;}
    private LopMamNon currentClass(){
        return classes.stream().filter(l->l.maLop()==selectedClassId).findFirst().orElse(null);
    }
    private void rebuildTabs(){
        tabBar.removeAll();tabButtons.clear();
        addTabButton("Tất cả",0);
        for(LopMamNon l:classes)addTabButton(l.tenLop(),l.maLop());
        styleTabs();tabBar.revalidate();tabBar.repaint();
    }
    private void addTabButton(String title,int classId){
        JButton b=new JButton(title);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFocusPainted(false);b.setOpaque(true);b.setContentAreaFilled(true);
        b.setFont(b.getFont().deriveFont(Font.BOLD,13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TAB_GREEN),BorderFactory.createEmptyBorder(7,12,7,12)));
        kichThuocTab(b);
        b.addActionListener(e->{
            selectedClassId=classId;styleTabs();if(!loading)showRows();
        });
        b.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override public void mouseEntered(java.awt.event.MouseEvent e){
                if(selectedClassId!=classId)b.setBackground(TAB_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e){
                if(selectedClassId!=classId)b.setBackground(Color.WHITE);
            }
        });
        tabButtons.add(b);tabBar.add(b);
    }
    private static void kichThuocTab(JButton b){
        int width=Math.max(112,b.getFontMetrics(b.getFont()).stringWidth(b.getText())+30);
        b.setPreferredSize(new Dimension(width,40));
        b.setMinimumSize(new Dimension(width,40));
    }
    private void styleTabs(){
        for(int i=0;i<tabButtons.size();i++){
            JButton b=tabButtons.get(i);
            int classId=i==0?0:classes.get(i-1).maLop();
            boolean selected=classId==selectedClassId;
            b.setBackground(selected?TAB_GREEN:Color.WHITE);
            b.setForeground(selected?Color.WHITE:TAB_GREEN);
        }
    }
    private void reloadClasses(){
        NamHocMamNon current=(NamHocMamNon)year.getSelectedItem();
        if(current==null){
            classes=List.of();data=List.of();selectedClassId=0;rebuildTabs();showRows();return;
        }
        int prior=currentClassId();
        MamNonUi.tai(this,()->ctrl.lop(current.maNamHoc(),""),rows->{
            if(current!=year.getSelectedItem())return;
            classes=rows;
            selectedClassId=classes.stream().anyMatch(l->l.maLop()==prior)?prior:0;
            loading=true;rebuildTabs();loading=false;loadRows();
        });
    }
    private void loadRows(){
        NamHocMamNon current=(NamHocMamNon)year.getSelectedItem();if(current==null)return;
        MamNonUi.tai(this,()->ctrl.xepLop(null),rows->{
            if(current!=year.getSelectedItem())return;
            Set<Integer> ids=new HashSet<>();for(LopMamNon l:classes)ids.add(l.maLop());
            data=rows.stream().filter(x->ids.contains(x.maLop())).toList();
            updateTabTitles();showRows();
        });
    }
    private void updateTabTitles(){
        long total=data.stream().filter(x->isActive(x)).count();
        if(tabButtons.isEmpty())return;
        tabButtons.get(0).setText("Tất cả ("+total+")");
        kichThuocTab(tabButtons.get(0));
        for(int i=0;i<classes.size();i++){
            int id=classes.get(i).maLop();long n=data.stream().filter(x->x.maLop()==id&&isActive(x)).count();
            tabButtons.get(i+1).setText(classes.get(i).tenLop()+" ("+n+")");
            kichThuocTab(tabButtons.get(i+1));
        }
        tabBar.revalidate();tabBar.repaint();
    }
    private static boolean isActive(XepLopMamNon x){return "HIEU_LUC".equals(x.trangThai())&&x.denNgay()==null;}
    private void showRows(){
        int id=currentClassId();visible=data.stream().filter(x->id==0||x.maLop()==id).toList();m.setRowCount(0);
        for(XepLopMamNon x:visible)m.addRow(new Object[]{x.maXepLop(),x.maHocVien(),x.tenHocVien(),x.tenLop(),x.tuNgay(),MamNonUi.ngay(x.denNgay()),x.trangThai()});
        status.setText("Có "+visible.size()+" hồ sơ xếp lớp (bao gồm lịch sử). Chọn tab lớp để chuyển hàng loạt sang năm mới.");
    }
    private XepLopMamNon selected(){
        int i=table.getSelectedRow();if(i<0){JOptionPane.showMessageDialog(this,"Hãy chọn học sinh trong bảng.");return null;}
        return visible.get(table.convertRowIndexToModel(i));
    }
    private void moveOne(){
        XepLopMamNon x=selected();if(x==null)return;
        if(!isActive(x)){JOptionPane.showMessageDialog(this,"Chỉ được chuyển hồ sơ đang hiệu lực.");return;}
        MamNonUi.tai(this,ctrl::namHoc,years->{
            NamHocMamNon source=(NamHocMamNon)year.getSelectedItem();if(source==null)return;
            MamNonUi.tai(this,()->ctrl.lop(null,""),all->{
                List<LopMamNon> destinations=new ArrayList<>();
                for(LopMamNon l:all){
                    NamHocMamNon y=years.stream().filter(n->n.maNamHoc()==l.maNamHoc()).findFirst().orElse(null);
                    if(l.trangThai()&&l.maLop()!=x.maLop()&&y!=null&&!y.tuNgay().isBefore(source.tuNgay()))destinations.add(l);
                }
                if(destinations.isEmpty()){JOptionPane.showMessageDialog(this,"Chưa có lớp đích đang hoạt động.");return;}
                JComboBox<LopMamNon> target=new JComboBox<>(destinations.toArray(new LopMamNon[0]));
                MamNonNgayPicker day=new MamNonNgayPicker(LocalDate.now()); JTextField reason=new JTextField("Điều chỉnh lớp học");
                target.addActionListener(e->{LopMamNon l=(LopMamNon)target.getSelectedItem();if(l!=null){
                    NamHocMamNon y=years.stream().filter(n->n.maNamHoc()==l.maNamHoc()).findFirst().orElse(null);
                    if(y!=null){LocalDate now=LocalDate.now();
                        day.setDateRange(y.tuNgay(),y.denNgay()); day.setDate((now.isBefore(y.tuNgay())||now.isAfter(y.denNgay())?y.tuNgay():now));}
                }});
                LopMamNon initial=(LopMamNon)target.getSelectedItem();
                if(initial!=null){NamHocMamNon y=years.stream().filter(n->n.maNamHoc()==initial.maNamHoc()).findFirst().orElse(null);
                    if(y!=null){LocalDate now=LocalDate.now();day.setDateRange(y.tuNgay(),y.denNgay()); day.setDate((now.isBefore(y.tuNgay())||now.isAfter(y.denNgay())?y.tuNgay():now));}}

                JPanel form=MamNonUi.form("Học sinh",new JLabel(x.tenHocVien()),"Lớp đang học",new JLabel(x.tenLop()),
                    "Lớp chuyển đến",target,"Ngày vào lớp mới",day,"Lý do (*)",reason);
                if(!MamNonUi.xacNhan(this,form,"Chuyển lớp học sinh",false))return;
                LopMamNon dest=(LopMamNon)target.getSelectedItem();if(dest==null)return;
                try{LocalDate start=day.getDate();
                    if(!MamNonUi.xacNhan(this,"Xác nhận chuyển "+x.tenHocVien()+" sang "+dest.tenLop()+"?","Xác nhận",false))return;
                    MamNonUi.ghi(this,()->ctrl.chuyenLop(x.maXepLop(),dest.maLop(),start,reason.getText()),this::loadRows);
                }catch(Exception ex){MamNonUi.loi(this,ex);}
            });
        });
    }
    private void finish(){
        XepLopMamNon x=selected();if(x==null)return;
        if(!isActive(x)){JOptionPane.showMessageDialog(this,"Hồ sơ này không còn hiệu lực.");return;}
        MamNonNgayPicker date=new MamNonNgayPicker(LocalDate.now()); JTextField reason=new JTextField();
        JComboBox<String> type=new JComboBox<>(new String[]{"NGHI_HAN","HUY"});
        JPanel form=MamNonUi.form("Trạng thái",type,"Ngày kết thúc",date,"Lý do (*)",reason);
        if(!MamNonUi.xacNhan(this,form,"Ngừng học tại lớp – "+x.tenHocVien(),true))return;
        try{LocalDate last=date.getDate();
            MamNonUi.ghi(this,()->ctrl.ketThucXepLop(x.maXepLop(),last,(String)type.getSelectedItem(),reason.getText()),this::loadRows);
        }catch(Exception ex){MamNonUi.loi(this,ex);}
    }
    private void bulk(){
        LopMamNon source=currentClass();NamHocMamNon sourceYear=(NamHocMamNon)year.getSelectedItem();
        if(source==null||sourceYear==null){JOptionPane.showMessageDialog(this,"Chọn tab lớp của năm học cũ trước.");return;}
        MamNonUi.tai(this,ctrl::namHoc,years->{
            List<NamHocMamNon> later=years.stream().filter(y->y.tuNgay().isAfter(sourceYear.tuNgay())).toList();
            if(later.isEmpty()){JOptionPane.showMessageDialog(this,"Hãy tạo năm học mới và lớp đích trước.");return;}
            JComboBox<NamHocMamNon> targetYear=new JComboBox<>(later.toArray(new NamHocMamNon[0]));
            JComboBox<LopMamNon> target=new JComboBox<>();
            MamNonNgayPicker day=new MamNonNgayPicker(LocalDate.now()); JTextField reason=new JTextField("Chuyển lớp đầu năm học mới");
            targetYear.addActionListener(e->{NamHocMamNon y=(NamHocMamNon)targetYear.getSelectedItem();if(y==null)return;
                target.removeAllItems();
                MamNonUi.tai(this,()->ctrl.lop(y.maNamHoc(),""),list->{
                    if(y!=targetYear.getSelectedItem())return;
                    target.removeAllItems();for(LopMamNon l:list)if(l.trangThai())target.addItem(l);
                    day.setDateRange(y.tuNgay(),y.denNgay()); day.setDate(y.tuNgay());
                });
            });
            targetYear.setSelectedIndex(0);
            // Gọi nạp danh sách đích trực tiếp (không giả định thứ tự ActionListener của Look and Feel).
            NamHocMamNon first=(NamHocMamNon)targetYear.getSelectedItem();
            if(first!=null){target.removeAllItems();
                MamNonUi.tai(this,()->ctrl.lop(first.maNamHoc(),""),list->{
                    if(first!=targetYear.getSelectedItem())return;
                    target.removeAllItems();for(LopMamNon l:list)if(l.trangThai())target.addItem(l);
                    day.setDateRange(first.tuNgay(),first.denNgay()); day.setDate(first.tuNgay());
                });
            }
            JPanel form=MamNonUi.form("Lớp cũ",new JLabel(source.tenLop()+" · "+sourceYear.tenNamHoc()),
                "Năm học mới",targetYear,"Lớp đích",target,"Ngày vào lớp mới",day,"Lý do (*)",reason);
            if(!MamNonUi.xacNhan(this,form,"Chuyển học sinh sang năm học mới",false))return;
            LopMamNon dest=(LopMamNon)target.getSelectedItem();if(dest==null){JOptionPane.showMessageDialog(this,"Chưa có lớp đích đang hoạt động.");return;}
            try{LocalDate transfer=day.getDate();
                MamNonUi.tai(this,()->ctrl.duKienChuyen(source.maLop(),dest.maLop()),preview->{
                    DefaultTableModel dm=MamNonUi.model("Mã học sinh","Họ và tên","Kết quả dự kiến");
                    int allowed=0;for(DuKienChuyen p:preview){dm.addRow(new Object[]{p.maHocVien(),p.tenHocVien(),p.ketQua()});if("Dự kiến chuyển".equals(p.ketQua()))allowed++;}
                    JScrollPane grid=new JScrollPane(MamNonUi.bang(dm));grid.setPreferredSize(new Dimension(680,300));
                    int copyCount=allowed;
                    JPanel summary=new JPanel(new BorderLayout(5,5));summary.add(new JLabel("Được chuyển dự kiến: "+allowed+" / "+preview.size()+" · Học sinh nghỉ hoặc đã có lớp sẽ bị bỏ qua."),BorderLayout.NORTH);summary.add(grid,BorderLayout.CENTER);
                    if(allowed==0){JOptionPane.showMessageDialog(this,summary,"Không có học sinh đủ điều kiện",JOptionPane.INFORMATION_MESSAGE);return;}
                    if(!MamNonUi.xacNhan(this,summary,"Xác nhận chuyển "+copyCount+" học sinh",false))return;
                    MamNonUi.tai(this,()->ctrl.chuyenNamHoc(source.maLop(),dest.maLop(),transfer,reason.getText()),result->{
                        JOptionPane.showMessageDialog(this,"Đã chuyển: "+result.daChuyen()+"\nBỏ qua (nghỉ/kết thúc): "+result.boQuaNghi()+"\nBỏ qua (trùng lớp/điều kiện khác): "+result.boQuaTrung());
                        loadRows();
                    });
                });
            }catch(Exception ex){MamNonUi.loi(this,ex);}
        });
    }
}
