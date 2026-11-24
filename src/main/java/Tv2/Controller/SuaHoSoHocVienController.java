package Tv2.Controller;
import Tv2.View.SuaHoSoHocVienPage;
import MainPage.Controller.PageTasks;
import Service.HocVienService;
import java.awt.Component;
import java.time.LocalDate;

/** Điều phối chỉnh sửa dữ liệu cá nhân, không chạm vào đăng ký khóa học. */
public class SuaHoSoHocVienController {
    public static void open(Component owner,int b,int id,Runnable done){
        PageTasks.run(owner,()->{var h=new HocVienService().getByMaHocVien(b,id);PageTasks.require(h!=null,"Không tìm thấy hồ sơ");return h;},h->{
            var v=new SuaHoSoHocVienPage();var in=v.show(owner,new SuaHoSoHocVienPage.Input(PageTasks.text(h.getHoTen()),PageTasks.text(h.getNgaySinh()),PageTasks.text(h.getGioiTinh()),PageTasks.text(h.getTenPhuHuynh()),PageTasks.text(h.getSoDienThoai()),PageTasks.text(h.getEmail()),PageTasks.text(h.getDiaChi()),PageTasks.text(h.getAnhDaiDien()),PageTasks.text(h.getGhiChu())));if(in==null)return;
            PageTasks.run(owner,()->{PageTasks.require(!in.name().isBlank(),"Họ tên không được để trống");LocalDate date=in.birth().isBlank()?null:LocalDate.parse(in.birth().trim());PageTasks.require(date==null||!date.isAfter(LocalDate.now()),"Ngày sinh không hợp lệ");String phone=in.phone().replaceAll("[ +()-]", "");PageTasks.require(phone.isBlank()||phone.matches("[0-9]{9,15}"),"Số điện thoại không hợp lệ");PageTasks.require(in.email().isBlank()||in.email().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"),"Email không hợp lệ");
                var svc=new HocVienService();var saved=svc.modify(b,id,x->{x.setHoTen(in.name().trim());x.setNgaySinh(date);x.setGioiTinh(TiepNhanController.blank(in.gender()));x.setTenPhuHuynh(TiepNhanController.blank(in.parent()));x.setSoDienThoai(TiepNhanController.blank(in.phone()));x.setEmail(TiepNhanController.blank(in.email()));x.setDiaChi(TiepNhanController.blank(in.address()));x.setAnhDaiDien(TiepNhanController.blank(in.photo()));x.setGhiChu(TiepNhanController.blank(in.note()));return true;});PageTasks.require(saved!=null,"Không lưu được hồ sơ");return saved;},saved->done.run());
        });
    }
}
