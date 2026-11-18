package vn.vinabacus.view;

import java.awt.*;
import java.beans.PropertyChangeListener;
import vn.vinabacus.tv4.NamHocMacDinh;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.*;
import vn.vinabacus.repository.MamNonRepository.LuaChon;

/** TV4 – quản lý lớp theo năm, giáo viên phụ trách và danh sách học sinh. */
public final class LopMamNonPanel extends JPanel {
    private final MamNonController ctrl=new MamNonController();
    private final JComboBox<NamHocMamNon> cboYear=new JComboBox<>();
    private final JTextField search=new JTextField(18);
    private final DefaultTableModel m=MamNonUi.model("Mã lớp","Tên lớp","Năm học","Trạng thái");
    private final JTable table=MamNonUi.bang(m);
    private final JLabel status=new JLabel("Sẵn sàng");
    private List<LopMamNon> data=List.of();
    private boolean loadingYears=false;
    private final PropertyChangeListener yearListener=e->SwingUtilities.invokeLater(()->loadYears(true));
    public LopMamNonPanel(){
        super(new BorderLayout(8,8));setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        JButton refresh=MamNonUi.nut("Làm mới",false),find=MamNonUi.nut("Tìm kiếm",false),add=MamNonUi.nut("+ Thêm lớp",true),edit=MamNonUi.nut("Sửa lớp",false),members=MamNonUi.nut("Học sinh trong lớp",false),teacher=MamNonUi.nut("Phân công giáo viên",false);
        MamNonUi.coCongCu(cboYear,215);MamNonUi.coCongCu(search,215);
        MamNonUi.coCongCu(find,110);MamNonUi.coCongCu(refresh,106);
        MamNonUi.coCongCu(add,135);MamNonUi.coCongCu(edit,105);
        MamNonUi.coCongCu(members,195);MamNonUi.coCongCu(teacher,195);
        JPanel top=new JPanel(new BorderLayout(0,10));
        top.add(MamNonUi.tieuDe("QUẢN LÝ LỚP MẦM NON"),BorderLayout.NORTH);
        top.add(MamNonUi.thanhCongCu(new JLabel("Năm học:"),cboYear,new JLabel("Tên lớp:"),search,
            find,refresh,add,edit,members,teacher),BorderLayout.CENTER);
        add(top,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);add(status,BorderLayout.SOUTH);
        cboYear.addActionListener(e->{if(!loadingYears)load();});refresh.addActionListener(e->loadYears(false));find.addActionListener(e->load());search.addActionListener(e->load());
        add.addActionListener(e->edit(null));edit.addActionListener(e->{LopMamNon l=selected();if(l!=null)edit(l);});
        members.addActionListener(e->{LopMamNon l=selected();if(l!=null)showMembers(l);});
        teacher.addActionListener(e->{LopMamNon l=selected();if(l!=null)assign(l);});
        SwingUtilities.invokeLater(()->loadYears(true));
    }
    @Override public void addNotify(){super.addNotify();NamHocMacDinh.langNghe(yearListener);}
    @Override public void removeNotify(){NamHocMacDinh.ngungNghe(yearListener);super.removeNotify();}
    private LopMamNon selected(){int i=table.getSelectedRow();if(i<0){JOptionPane.showMessageDialog(this,"Chọn một lớp trong bảng.");return null;}return data.get(table.convertRowIndexToModel(i));}
    private void loadYears(boolean useDefault){MamNonUi.tai(this,ctrl::namHoc,years->{
        NamHocMamNon keep=(NamHocMamNon)cboYear.getSelectedItem();loadingYears=true;
        cboYear.removeAllItems();for(NamHocMamNon y:years)cboYear.addItem(y);
        NamHocMamNon chosen=useDefault?NamHocMacDinh.chonHieuLuc(years):keep;
        if(chosen!=null)for(int i=0;i<cboYear.getItemCount();i++)if(cboYear.getItemAt(i).maNamHoc()==chosen.maNamHoc()){
            cboYear.setSelectedIndex(i);break;
        }
        loadingYears=false;load();
    });}
    private void load(){NamHocMamNon year=(NamHocMamNon)cboYear.getSelectedItem();if(year==null){data=List.of();m.setRowCount(0);return;}
        status.setText("Đang tải lớp học...");MamNonUi.tai(this,()->ctrl.lop(year.maNamHoc(),search.getText()),list->{data=list;m.setRowCount(0);for(LopMamNon l:list)m.addRow(new Object[]{l.maLop(),l.tenLop(),l.tenNamHoc(),l.trangThai()?"Hoạt động":"Ngừng sử dụng"});status.setText("Có "+list.size()+" lớp");});}
    private void edit(LopMamNon l){
        if(cboYear.getItemCount()==0){JOptionPane.showMessageDialog(this,"Hãy tạo năm học trước.");return;}
        JComboBox<NamHocMamNon> years=new JComboBox<>();for(int i=0;i<cboYear.getItemCount();i++){NamHocMamNon y=cboYear.getItemAt(i);years.addItem(y);if(l!=null&&l.maNamHoc()==y.maNamHoc())years.setSelectedItem(y);}
        JTextField name=new JTextField(l==null?"":l.tenLop());JComboBox<String> statusBox=new JComboBox<>(new String[]{"Hoạt động","Ngừng sử dụng"});if(l!=null&&!l.trangThai())statusBox.setSelectedIndex(1);
        JPanel form=MamNonUi.form("Năm học (*)",years,"Tên lớp (*)",name,"Trạng thái",statusBox);
        if(!MamNonUi.xacNhan(this,form,l==null?"Thêm lớp Mầm non":"Sửa lớp Mầm non",false))return;
        NamHocMamNon year=(NamHocMamNon)years.getSelectedItem();if(year==null)return;
        MamNonUi.ghi(this,()->ctrl.luuLop(l==null?0:l.maLop(),year.maNamHoc(),name.getText(),statusBox.getSelectedIndex()==0),this::load);
    }
    private void showMembers(LopMamNon l){
        MamNonUi.tai(this,()->ctrl.xepLop(l.maLop()),list->{
            DefaultTableModel mm=MamNonUi.model("Mã học sinh","Họ tên","Từ ngày","Đến ngày","Trạng thái");
            for(XepLopMamNon x:list)mm.addRow(new Object[]{x.maHocVien(),x.tenHocVien(),x.tuNgay(),MamNonUi.ngay(x.denNgay()),x.trangThai()});
            JTable t=MamNonUi.bang(mm);JScrollPane sp=new JScrollPane(t);sp.setPreferredSize(new Dimension(800,320));
            JOptionPane.showMessageDialog(this,sp,"Học sinh lớp "+l.tenLop(),JOptionPane.PLAIN_MESSAGE);
        });
    }
    private void assign(LopMamNon l){
        MamNonUi.tai(this,ctrl::giaoVien,people->{
            if(people.isEmpty()){JOptionPane.showMessageDialog(this,"Chưa có giáo viên hợp lệ tại cơ sở.");return;}
            JComboBox<LuaChon> teachers=new JComboBox<>(people.toArray(new LuaChon[0]));
            MamNonNgayPicker start=new MamNonNgayPicker(LocalDate.now()),end=new MamNonNgayPicker(null,true);
            JPanel p=MamNonUi.form("Giáo viên (*)",teachers,"Từ ngày",start,"Đến ngày (có thể bỏ trống)",end);
            if(!MamNonUi.xacNhan(this,p,"Phân công GV: "+l.tenLop(),false))return;
            try {LocalDate tu=start.getDate(),den=end.getDate();LuaChon gv=(LuaChon)teachers.getSelectedItem();
                MamNonUi.ghi(this,()->ctrl.phanCongGiaoVien(l.maLop(),gv.id(),tu,den),()->JOptionPane.showMessageDialog(this,"Đã lưu phân công giáo viên."));
            }catch(Exception ex){MamNonUi.loi(this,ex);}
        });
    }
}
