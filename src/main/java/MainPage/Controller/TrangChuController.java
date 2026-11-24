package MainPage.Controller;
import MainPage.View.TrangChuPage;
import Service.*;
import java.time.LocalDate;
import java.util.Set;
public class TrangChuController {
 private final TrangChuPage v;private final int b;
 public TrangChuController(TrangChuPage v,int b){this(v,b,"Cơ sở #"+b);}
 public TrangChuController(TrangChuPage v,int b,String name){this.v=v;this.b=b;v.title("TRANG CHỦ — "+name,"Tổng quan cơ sở");v.columns("Chỉ số","Số lượng");v.action("Tải lại",()->load(true));load(false);}
 private void load(boolean refresh){v.status("Đang tải...");PageTasks.run(v,()->{var hs=new HocVienService();var ls=new LopHocService();var bs=new BuoiHocService();var h=refresh?hs.reload(b):hs.selectAll(b);var l=refresh?ls.reload(b):ls.selectAll(b);var s=refresh?bs.reload(b):bs.selectAll(b);return new Object[][]{{"Học viên chính thức",h.stream().filter(x->"CHINH_THUC".equals(x.getTrangThai())).count()},{"Hồ sơ tuyển sinh cần xử lý",h.stream().filter(x->Set.of("TIEP_NHAN","HOC_THU","CHO_XEP_LOP").contains(x.getTrangThai())).count()},{"Lớp đang mở",l.stream().filter(x->"DANG_MO".equals(x.getTrangThai())).count()},{"Buổi học hôm nay",s.stream().filter(x->LocalDate.now().equals(x.getNgayHoc())&&!"HUY".equals(x.getTrangThai())).count()}};},r->{v.rows(r);v.status("Số liệu từ cơ sở #"+b);});}
}
