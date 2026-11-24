package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.LopMamNon;
import vn.vinabacus.model.NamHocMamNon;
import vn.vinabacus.model.XepLopMamNon;
import vn.vinabacus.repository.MamNonRepository.BangTinhThang;
import vn.vinabacus.repository.MamNonRepository.NgayHoatDong;
import vn.vinabacus.tv4.NamHocMacDinh;

/** Bảng tính phí tháng Mầm non; chỉ lập NHAP và chốt sau khi đủ điểm danh. */
public final class TinhPhiMamNonPanel extends JPanel {
    private final MamNonController ctrl=new MamNonController();
    private final JComboBox<NamHocMamNon> year=new JComboBox<>();
    private final JComboBox<LopMamNon> classes=new JComboBox<>();
    private final JTextField month=new JTextField(8);
    private final DefaultTableModel model=MamNonUi.model("Mã bảng","Học sinh","Lớp","Kỳ","Học phí","Tiền ăn","Đã điểm danh / Cần","Trạng thái");
    private final JTable table=MamNonUi.bang(model);
    private final JLabel status=new JLabel("Sẵn sàng");
    private List<BangTinhThang> data=List.of();
    private boolean loading;

    public TinhPhiMamNonPanel(){
        super(new BorderLayout(8,8));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JButton calendar=MamNonUi.nut("Lịch hoạt động",false),setup=MamNonUi.nut("Thiết lập tháng",false),create=MamNonUi.nut("Lập bảng tính nháp",true),close=MamNonUi.nut("Chốt bảng tính",false),refresh=MamNonUi.nut("Làm mới",false);
        MamNonUi.coCongCu(year,175);MamNonUi.coCongCu(classes,185);MamNonUi.coCongCu(month,90);
        MamNonUi.coCongCu(calendar,160);MamNonUi.coCongCu(setup,150);MamNonUi.coCongCu(create,180);MamNonUi.coCongCu(close,150);MamNonUi.coCongCu(refresh,105);
        JPanel top=new JPanel(new BorderLayout());
        top.add(MamNonUi.tieuDe("HỌC PHÍ & TIỀN ĂN THEO THÁNG"),BorderLayout.NORTH);
        top.add(MamNonUi.thanhCongCu(new JLabel("Năm học:"),year,new JLabel("Lớp:"),classes,
            new JLabel("Tháng yyyy-MM:"),month,calendar,setup,create,close,refresh),BorderLayout.CENTER);
        add(top,BorderLayout.NORTH);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] width={80,185,150,85,120,120,190,95};
        for(int i=0;i<width.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(width[i]);
        JScrollPane scroll=new JScrollPane(table);scroll.setPreferredSize(new Dimension(700,250));add(scroll,BorderLayout.CENTER);
        add(status,BorderLayout.SOUTH);
        year.addActionListener(e->{if(!loading){NamHocMamNon y=(NamHocMamNon)year.getSelectedItem();if(y!=null)month.setText(YearMonth.from(y.tuNgay()).toString());loadClasses();}});
        month.addActionListener(e->load());
        refresh.addActionListener(e->loadYears());
        calendar.addActionListener(e->calendar());setup.addActionListener(e->setup());create.addActionListener(e->create());close.addActionListener(e->close());
        SwingUtilities.invokeLater(this::loadYears);
    }
    private YearMonth selectedMonth(){
        try{
            YearMonth ky=YearMonth.parse(month.getText().trim());
            NamHocMamNon y=(NamHocMamNon)year.getSelectedItem();
            if(y==null||ky.atDay(1).isBefore(y.tuNgay())||ky.atEndOfMonth().isAfter(y.denNgay()))
                throw new IllegalArgumentException("Tháng phải nằm trọn trong năm học đã chọn.");
            return ky;
        }catch(java.time.format.DateTimeParseException ex){throw new IllegalArgumentException("Nhập tháng theo yyyy-MM, ví dụ 2027-08.");}
    }
    private void loadYears(){
        NamHocMamNon previous=(NamHocMamNon)year.getSelectedItem();
        MamNonUi.tai(this,ctrl::namHoc,rows->{
            loading=true;year.removeAllItems();for(NamHocMamNon y:rows)year.addItem(y);
            NamHocMamNon target=previous==null?NamHocMacDinh.chonHieuLuc(rows):previous;
            if(target!=null)for(int i=0;i<year.getItemCount();i++)if(year.getItemAt(i).maNamHoc()==target.maNamHoc()){year.setSelectedIndex(i);break;}
            if(month.getText().isBlank()&&target!=null)month.setText(YearMonth.from(target.tuNgay()).toString());
            loading=false;loadClasses();
        });
    }
    private void loadClasses(){
        NamHocMamNon y=(NamHocMamNon)year.getSelectedItem();if(y==null){classes.removeAllItems();return;}
        LopMamNon previous=(LopMamNon)classes.getSelectedItem();
        MamNonUi.tai(this,()->ctrl.lop(y.maNamHoc(),""),rows->{
            if(y!=year.getSelectedItem())return;
            loading=true;classes.removeAllItems();for(LopMamNon l:rows)classes.addItem(l);
            if(previous!=null)for(int i=0;i<classes.getItemCount();i++)if(classes.getItemAt(i).maLop()==previous.maLop()){classes.setSelectedIndex(i);break;}
            loading=false;load();
        });
    }
    private void load(){
        NamHocMamNon y=(NamHocMamNon)year.getSelectedItem();if(y==null)return;
        YearMonth ky;try{ky=selectedMonth();}catch(Exception ex){status.setText(ex.getMessage());return;}
        status.setText("Đang tải bảng tính tháng...");
        MamNonUi.tai(this,()->ctrl.bangTinhThang(y.maNamHoc(),ky),rows->{
            if(y!=year.getSelectedItem()||!month.getText().trim().equals(ky.toString()))return;
            data=rows;model.setRowCount(0);
            for(BangTinhThang b:rows)model.addRow(new Object[]{b.id(),b.hocSinh(),b.lop(),b.ky(),
                b.hocPhi().toPlainString(),b.tienAn().toPlainString(),b.soNgayDaDiemDanh()+" / "+b.soNgayCanDiemDanh()+" (tháng "+b.soNgayChuan()+")",b.trangThai()});
            status.setText(rows.size()+" bảng tính · Nháp chỉ được chốt sau khi đủ điểm danh; khoản phải thu được lập riêng sau khi chốt.");
        });
    }
    private void setup(){
        YearMonth ky;try{ky=selectedMonth();}catch(Exception ex){MamNonUi.loi(this,ex);return;}
        MamNonUi.tai(this,()->ctrl.soNgayHocChuan(ky),days->{
            if(!MamNonUi.xacNhan(this,"Lịch hoạt động tháng "+ky+" có "+days+" ngày học chuẩn. Tạo kỳ thu phí theo số ngày này?","Thiết lập tháng Mầm non",false))return;
            MamNonUi.ghi(this,()->ctrl.taoThang(ky,days),this::load);
        });
    }
    private void calendar(){
        NamHocMamNon schoolYear=(NamHocMamNon)year.getSelectedItem();
        YearMonth ky;try{ky=selectedMonth();}catch(Exception ex){MamNonUi.loi(this,ex);return;}
        if(schoolYear==null)return;
        MamNonUi.tai(this,()->ctrl.lichHoatDong(schoolYear.maNamHoc(),ky),saved->{
            String[] states={"Chưa chọn","Học + ăn","Học, không ăn","Nghỉ"};
            DefaultTableModel grid=new DefaultTableModel(new Object[]{"Ngày","Thứ","Trạng thái"},0){
                @Override public boolean isCellEditable(int r,int c){return c==2;}
            };
            for(NgayHoatDong d:saved){
                String state=d.coHoc()==null?states[0]:!d.coHoc()?states[3]:d.coAn()?states[1]:states[2];
                grid.addRow(new Object[]{d.ngay(),d.ngay().getDayOfWeek(),state});
            }
            JTable days=new JTable(grid);days.setRowHeight(25);days.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            days.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(states)));
            JScrollPane scroll=new JScrollPane(days);scroll.setPreferredSize(new Dimension(510,390));
            JComboBox<String> bulk=new JComboBox<>(new String[]{states[1],states[2],states[3]});
            JButton apply=MamNonUi.nut("Áp dụng cho ngày chọn",false);
            apply.addActionListener(e->{for(int row:days.getSelectedRows())grid.setValueAt(bulk.getSelectedItem(),days.convertRowIndexToModel(row),2);});
            JPanel form=new JPanel(new BorderLayout(7,7));
            form.add(new JLabel("Xác nhận rõ từng ngày; không tự suy đoán thứ Bảy, Chủ nhật hoặc ngày nghỉ."),BorderLayout.NORTH);
            form.add(scroll,BorderLayout.CENTER);
            form.add(MamNonUi.dong(new JLabel("Đánh dấu nhanh:"),bulk,apply),BorderLayout.SOUTH);
            if(!MamNonUi.xacNhan(this,form,"Lịch hoạt động tháng "+ky,false))return;
            if(days.isEditing()&&!days.getCellEditor().stopCellEditing())return;
            List<NgayHoatDong> result=new ArrayList<>();
            for(int i=0;i<grid.getRowCount();i++){
                LocalDate date=(LocalDate)grid.getValueAt(i,0);String state=String.valueOf(grid.getValueAt(i,2));
                if(states[0].equals(state)){JOptionPane.showMessageDialog(this,"Chưa xác nhận ngày "+date+".");return;}
                result.add(new NgayHoatDong(date,!states[3].equals(state),states[1].equals(state)));
            }
            MamNonUi.ghi(this,()->ctrl.luuLichHoatDong(schoolYear.maNamHoc(),ky,result),this::load);
        });
    }
    private void create(){
        LopMamNon lop=(LopMamNon)classes.getSelectedItem();if(lop==null){JOptionPane.showMessageDialog(this,"Chọn lớp trước.");return;}
        YearMonth ky;try{ky=selectedMonth();}catch(Exception ex){MamNonUi.loi(this,ex);return;}
        MamNonUi.tai(this,()->ctrl.xepLop(lop.maLop()),rows->{
            Set<Integer> seen=new HashSet<>();
            List<XepLopMamNon> active=rows.stream().filter(x->!"HUY".equals(x.trangThai())&&
                !x.tuNgay().isAfter(ky.atEndOfMonth())&&(x.denNgay()==null||!x.denNgay().isBefore(ky.atDay(1)))&&seen.add(x.maHocVien())).toList();
            if(active.isEmpty()){JOptionPane.showMessageDialog(this,"Lớp chưa có học sinh đang học.");return;}
            JComboBox<XepLopMamNon> student=new JComboBox<>(active.toArray(new XepLopMamNon[0]));
            student.setRenderer(new DefaultListCellRenderer(){
                @Override public java.awt.Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                    XepLopMamNon x=(XepLopMamNon)value;
                    return super.getListCellRendererComponent(list,x==null?"":x.tenHocVien()+" (#"+x.maHocVien()+")",index,selected,focus);
                }
            });
            JPanel form=MamNonUi.form("Học sinh (*)",student,"Kỳ",new JLabel(ky.toString()));
            if(!MamNonUi.xacNhan(this,form,"Lập bảng tính phí nháp",false))return;
            XepLopMamNon selected=(XepLopMamNon)student.getSelectedItem();if(selected==null)return;
            if(!MamNonUi.xacNhan(this,"Lập bảng tính cho "+selected.tenHocVien()+" theo lịch hoạt động và các khoảng xếp lớp trong tháng?","Xác nhận",false))return;
            MamNonUi.ghi(this,()->ctrl.lapBangTinhNhap(selected.maHocVien(),ky),this::load);
        });
    }
    private void close(){
        int view=table.getSelectedRow();if(view<0){JOptionPane.showMessageDialog(this,"Chọn bảng tính cần chốt.");return;}
        int index=table.convertRowIndexToModel(view);if(index<0||index>=data.size())return;
        BangTinhThang b=data.get(index);
        if(!"NHAP".equals(b.trangThai())){JOptionPane.showMessageDialog(this,"Bảng tính đã chốt.");return;}
        if(!MamNonUi.xacNhan(this,"Chốt bảng tính #"+b.id()+" của "+b.hocSinh()+"? Sau khi chốt, điểm danh tháng này cần quy trình điều chỉnh tài chính.","Xác nhận chốt",true))return;
        MamNonUi.ghi(this,()->ctrl.chotBangTinh(b.id()),this::load);
    }
}
