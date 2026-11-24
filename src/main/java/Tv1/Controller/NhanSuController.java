package Tv1.Controller;

import Model.NhanSuModel;
import Service.NhanSuService;
import Tv1.View.NhanSuPage;
import Tv1.View.NhanSuDialog;
import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Controller nhân sự, mọi đọc/ghi thông qua NhanSuService. */
public class NhanSuController {
    private static final String[] ROLES={"ADMIN","TO_TRUONG","GIAO_VIEN","NHAN_VIEN"}, ROLE_LABELS={"Quản trị viên","Tổ trưởng","Giáo viên","Nhân viên"}, GENDERS={null,"NAM","NU","KHAC"}, GENDER_LABELS={"","Nam","Nữ","Khác"};
    private final NhanSuPage v;
    private final int maCoSo;
    private final String branch;
    private final NhanSuService service=new NhanSuService();
    // Chỉ giữ danh sách đang trình bày để lọc/sắp xếp; không phải cache Database.
    private List<NhanSuModel> displayed=List.of();
    private boolean busy,loaded;
    public NhanSuController(NhanSuPage v,int b){this(v,b,"Cơ sở #"+b);}
    public NhanSuController(NhanSuPage v,int b,String name){
        this.v=v;maCoSo=b;branch=name;
        v.title("QUẢN LÝ NHÂN SỰ",branch);
        v.find.addActionListener(e->filter());v.search.addActionListener(e->filter());v.role.addActionListener(e->filter());v.state.addActionListener(e->filter());v.refresh.addActionListener(e->load(true));v.add.addActionListener(e->form(null));v.edit.addActionListener(e->form(selected()));v.detail.addActionListener(e->profile());v.stop.addActionListener(e->changeState(false));v.restore.addActionListener(e->changeState(true));v.remove.addActionListener(e->delete());v.onSelection(this::controls);v.onOpen(this::profile);
        if(b<=0){v.status("Chưa có cơ sở hợp lệ.");controls();v.refresh.setEnabled(false);}else load(false);
    }
    private static String txt(Object x){return Objects.toString(x,"");}
    private static String optional(String x){String s=x.trim();return s.isEmpty()?null:s;}
    private static String norm(String x){return Normalizer.normalize(txt(x),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).trim();}
    private static String label(String x,String[] keys,String[] labels){for(int i=0;i<keys.length;i++)if(Objects.equals(x,keys[i]))return labels[i];return txt(x);}
    private static int index(String x,String[] keys,int fallback){for(int i=0;i<keys.length;i++)if(Objects.equals(x,keys[i]))return i;return fallback;}
    private NhanSuModel selected(){int row=v.selected();if(row<0||row>=v.table.getModel().getRowCount())return null;Object id=v.value(row,0);return displayed.stream().filter(x->Objects.equals(x.getMaNhanSu(),id)).findFirst().orElse(null);}
    private void controls(){NhanSuModel x=selected();v.controls(busy,loaded,x==null?null:x.getTrangThai());if(maCoSo<=0)v.refresh.setEnabled(false);}
    private void notice(String text){JOptionPane.showMessageDialog(v,text);}
    private void error(String text){JOptionPane.showMessageDialog(v,text,"Thông báo",JOptionPane.ERROR_MESSAGE);}
    private <T> void task(Callable<T> work,Consumer<T> success,Consumer<Throwable> failure){
        busy=true;controls();
        new SwingWorker<T,Void>(){
            protected T doInBackground() throws Exception{return work.call();}
            protected void done(){T result;try{result=get();}catch(Exception e){busy=false;controls();failure.accept(e.getCause()==null?e:e.getCause());return;}busy=false;controls();success.accept(result);}
        }.execute();
    }
    private void load(boolean reload){
        if(busy||maCoSo<=0)return;loaded=false;v.status("Đang tải dữ liệu nhân sự...");
        task(()->reload?service.reload(maCoSo):service.selectAll(maCoSo), rows->{displayed=rows.stream().filter(x->x.getMaCoSo()==maCoSo).toList();loaded=true;v.stats(displayed.size(),(int)displayed.stream().filter(x->"GIAO_VIEN".equals(x.getVaiTro())).count(),(int)displayed.stream().filter(x->"NHAN_VIEN".equals(x.getVaiTro())).count(),(int)displayed.stream().filter(NhanSuModel::getTrangThai).count());filter();},e->{displayed=List.of();v.rows(new Object[0][]);v.clearStats();v.status("Không tải được dữ liệu. Bấm Làm mới để thử lại.");controls();error("Không tải được danh sách nhân sự. Kiểm tra kết nối hoặc liên hệ Admin.");});
    }
    private void filter(){
        if(!loaded||busy)return;String query=norm(v.search.getText());int role=v.role.getSelectedIndex(), state=v.state.getSelectedIndex();
        Object[][] rows=displayed.stream().filter(x->role==0||ROLES[role-1].equals(x.getVaiTro())).filter(x->state==0||x.getTrangThai()==(state==1)).filter(x->query.isEmpty()||norm(x.getHoTen()).contains(query)||norm(x.getSoDienThoai()).contains(query)||norm(x.getTenDangNhap()).contains(query)||norm(x.getEmail()).contains(query)||String.valueOf(x.getMaNhanSu()).contains(query)).map(x->new Object[]{x.getMaNhanSu(),x.getHoTen(),label(x.getVaiTro(),ROLES,ROLE_LABELS),txt(x.getSoDienThoai()),x.getTrangThai()?"Đang hoạt động":"Ngừng hoạt động",x.getChoPhepDangNhap()?"Cho phép":"Không cho phép"}).toArray(Object[][]::new);
        v.rows(rows);v.status("Hiển thị: "+rows.length+" / "+displayed.size()+" nhân sự • "+branch);controls();
    }
    private void form(NhanSuModel old){
        if(busy||!loaded)return;boolean editing=old!=null;
        NhanSuDialog d=new NhanSuDialog(v,editing,branch);
        if(editing){d.name.setText(txt(old.getHoTen()));d.phone.setText(txt(old.getSoDienThoai()));d.email.setText(txt(old.getEmail()));d.address.setText(txt(old.getDiaChi()));d.username.setText(txt(old.getTenDangNhap()));d.image.setText(txt(old.getAnhDaiDien()));d.updatePreview();d.setBirth(old.getNgaySinh());d.gender.setSelectedIndex(index(old.getGioiTinh(),GENDERS,0));d.role.setSelectedIndex(index(old.getVaiTro(),ROLES,3));d.active.setSelected(old.getTrangThai());d.save.setText("Lưu thay đổi");}
        // Không đổi danh tính đăng nhập hoặc vai trò tài khoản đã được cấp quyền khi chưa có API phân quyền.
        if(editing&&old.getChoPhepDangNhap()){d.username.setEnabled(false);d.role.setEnabled(false);}
        d.save.addActionListener(e->{
            if(busy)return;
            final NhanSuModel x;
            try{
                x=new NhanSuModel();x.setMaCoSo(maCoSo);x.setHoTen(d.name.getText().trim());x.setNgaySinh(d.birthValue());x.setGioiTinh(GENDERS[d.gender.getSelectedIndex()]);x.setSoDienThoai(optional(d.phone.getText()));x.setEmail(optional(d.email.getText()));x.setDiaChi(optional(d.address.getText()));x.setAnhDaiDien(optional(d.image.getText()));x.setTenDangNhap(d.username.getText().trim());x.setVaiTro(ROLES[d.role.getSelectedIndex()]);x.setTrangThai(d.active.isSelected());
                if(editing){x.setMaNhanSu(old.getMaNhanSu());x.setNgayTao(old.getNgayTao());x.setMatKhauBam(old.getMatKhauBam());x.setChoPhepDangNhap(old.getChoPhepDangNhap()&&x.getTrangThai());}
                validate(x);
            }catch(Exception ex){JOptionPane.showMessageDialog(d,ex instanceof java.text.ParseException?"Ngày sinh không hợp lệ.":ex.getMessage(),"Kiểm tra dữ liệu",JOptionPane.WARNING_MESSAGE);return;}
            d.busy(true);v.status("Đang lưu nhân sự...");
            task(()->{
                if(service.getByTenDangNhap(maCoSo,x.getTenDangNhap()).stream().anyMatch(y->y.getMaNhanSu()!=x.getMaNhanSu()))return "Tên đăng nhập đã tồn tại trong cơ sở.";
                if(editing){NhanSuModel changed=service.modify(maCoSo,x.getMaNhanSu(),y->{
                    // Giữ nguyên mật khẩu, ngày tạo và quyền đăng nhập mới nhất trừ khi ngừng hoạt động.
                    y.setHoTen(x.getHoTen());y.setNgaySinh(x.getNgaySinh());y.setGioiTinh(x.getGioiTinh());y.setSoDienThoai(x.getSoDienThoai());y.setEmail(x.getEmail());y.setDiaChi(x.getDiaChi());y.setAnhDaiDien(x.getAnhDaiDien());if(!y.getChoPhepDangNhap()){y.setTenDangNhap(x.getTenDangNhap());y.setVaiTro(x.getVaiTro());}y.setTrangThai(x.getTrangThai());if(!x.getTrangThai())y.setChoPhepDangNhap(false);return true;});return changed!=null?null:"Không thể cập nhật. Kiểm tra dữ liệu hoặc liên hệ Admin.";}
                return service.insert(maCoSo,x)?null:"Không thể thêm. Tên đăng nhập có thể đã tồn tại ở cơ sở khác hoặc dữ liệu bị từ chối. Kiểm tra danh sách trước khi thử lại.";
            },message->{d.busy(false);if(message!=null){JOptionPane.showMessageDialog(d,message);v.status("Chưa xác nhận lưu thành công.");return;}d.dispose();load(false);notice(editing?"Đã cập nhật nhân sự.":"Đã thêm nhân sự.");},ex->{d.busy(false);v.status("Không xác nhận được kết quả lưu.");JOptionPane.showMessageDialog(d,"Không xác nhận được kết quả lưu. Đóng form và làm mới danh sách trước khi thử lại.");});
        });d.setVisible(true);
    }
    private void validate(NhanSuModel x){
        if(x.getHoTen().isBlank()||x.getHoTen().length()>150)throw new IllegalArgumentException("Họ tên bắt buộc, tối đa 150 ký tự.");
        if(x.getTenDangNhap().isBlank()||x.getTenDangNhap().length()>100)throw new IllegalArgumentException("Tên đăng nhập bắt buộc, tối đa 100 ký tự.");
        if(x.getNgaySinh()!=null&&x.getNgaySinh().isAfter(LocalDate.now()))throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");
        if(x.getSoDienThoai()!=null&&(x.getSoDienThoai().length()>20||!x.getSoDienThoai().matches("[0-9+(). -]*[0-9][0-9+(). -]*")))throw new IllegalArgumentException("Số điện thoại không hợp lệ (tối đa 20 ký tự).");
        if(x.getEmail()!=null&&(x.getEmail().length()>254||!x.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))throw new IllegalArgumentException("Email không hợp lệ.");
        if(txt(x.getDiaChi()).length()>300||txt(x.getAnhDaiDien()).length()>500)throw new IllegalArgumentException("Địa chỉ tối đa 300, đường dẫn ảnh tối đa 500 ký tự.");
        service.validate(x);
    }
    private void profile(){if(busy||!loaded)return;NhanSuModel x=selected();if(x==null)return;NhanSuDialog.profile(v,"Hồ sơ nhân sự — "+x.getHoTen(),new String[][]{{"Mã nhân sự",txt(x.getMaNhanSu())},{"Họ và tên",txt(x.getHoTen())},{"Cơ sở",branch},{"Ngày sinh",x.getNgaySinh()==null?"":x.getNgaySinh().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))},{"Giới tính",label(x.getGioiTinh(),GENDERS,GENDER_LABELS)},{"Số điện thoại",txt(x.getSoDienThoai())},{"Email",txt(x.getEmail())},{"Địa chỉ",txt(x.getDiaChi())},{"Vai trò",label(x.getVaiTro(),ROLES,ROLE_LABELS)},{"Tên đăng nhập",txt(x.getTenDangNhap())},{"Quyền đăng nhập",x.getChoPhepDangNhap()?"Cho phép":"Không cho phép"},{"Trạng thái",x.getTrangThai()?"Đang hoạt động":"Ngừng hoạt động"},{"Ngày tạo (UTC)",txt(x.getNgayTao())},{"Ảnh đại diện",txt(x.getAnhDaiDien())}},x.getAnhDaiDien(),()->form(x));}
    private void changeState(boolean active){if(busy||!loaded)return;NhanSuModel x=selected();if(x==null||x.getTrangThai()==active)return;String action=active?"Khôi phục hoạt động":"Ngừng hoạt động";if(!NhanSuPage.confirm(v,action+" cho "+x.getHoTen()+"?\n"+(active?"Không tự động cấp lại quyền đăng nhập.":"Giữ lịch sử nghiệp vụ và tắt quyền đăng nhập.")))return;task(()->service.modify(maCoSo,x.getMaNhanSu(),y->{y.setTrangThai(active);if(!active)y.setChoPhepDangNhap(false);return true;})!=null,ok->afterWrite(ok,action),e->writeError());}
    private void delete(){if(busy||!loaded)return;NhanSuModel x=selected();if(x==null)return;if(!NhanSuPage.confirm(v,"Xóa vĩnh viễn "+x.getHoTen()+"?\nNên ngừng hoạt động nếu cần giữ hồ sơ lịch sử."))return;task(()->service.delete(maCoSo,x.getMaNhanSu()),ok->afterWrite(ok,"Xóa nhân sự"),e->writeError());}
    private void afterWrite(boolean ok,String action){load(false);if(ok)notice(action+" thành công.");else error(action+" thất bại. Có thể dữ liệu bị ràng buộc hoặc không thể ghi. Không tự xóa dữ liệu liên quan.");}
    private void writeError(){v.status("Không xác nhận được kết quả thao tác.");error("Không xác nhận được kết quả. Hãy làm mới trước khi thử lại.");}
}
