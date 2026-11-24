package Tv6.Controller;
import Tv6.View.BaoCaoPage;import MainPage.Controller.PageTasks;import Service.*;import java.util.*;import java.util.stream.Collectors;
public class BaoCaoController {
 private final BaoCaoPage v;private final int b;
 public BaoCaoController(BaoCaoPage v,int b){this(v,b,"Cơ sở #"+b);}
 public BaoCaoController(BaoCaoPage v,int b,String name){this.v=v;this.b=b;v.title("BÁO CÁO — "+name,"Thống kê hồ sơ và điểm danh theo trạng thái");v.columns("Nhóm","Trạng thái","Số lượng");v.action("Tải lại",()->load(true));load(false);}
 private void load(boolean force){v.status("Đang tải...");PageTasks.run(v,()->{var h=new HocVienService();var d=new DiemDanhService();var l=new LopHocService();List<Object[]> r=new ArrayList<>();(force?h.reload(b):h.selectAll(b)).stream().collect(Collectors.groupingBy(x->x.getTrangThai(),Collectors.counting())).forEach((k,n)->r.add(new Object[]{"Học viên",k,n}));(force?d.reload(b):d.selectAll(b)).stream().collect(Collectors.groupingBy(x->x.getTrangThai(),Collectors.counting())).forEach((k,n)->r.add(new Object[]{"Điểm danh",k,n}));(force?l.reload(b):l.selectAll(b)).stream().collect(Collectors.groupingBy(x->x.getTrangThai(),Collectors.counting())).forEach((k,n)->r.add(new Object[]{"Lớp",k,n}));return r.toArray(Object[][]::new);},r->{v.rows(r);v.status("Số liệu theo cơ sở #"+b);});}
}
