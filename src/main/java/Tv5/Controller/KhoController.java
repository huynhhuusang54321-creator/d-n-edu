package Tv5.Controller;
import Tv5.View.KhoPage;import MainPage.Controller.PageTasks;import Service.*;import java.util.*;
public class KhoController {
 private final KhoPage v;private final int b;
 public KhoController(KhoPage v,int b){this(v,b,"Cơ sở #"+b);}
 public KhoController(KhoPage v,int b,String name){this.v=v;this.b=b;v.title("KHO — "+name,"Nhật ký nhập / xuất / kiểm kê");v.columns("Mã","Hàng hóa","Loại","Số lượng","Tồn kiểm kê","Đơn giá","Ngày","Trạng thái","Ghi chú");v.action("Tải lại",()->load(true));load(false);}
 private void load(boolean force){v.status("Đang tải...");PageTasks.run(v,()->{var s=new GiaoDichKhoService();var ds=force?s.reload(b):s.selectAll(b);var names=new HashMap<Integer,String>();new DanhMucService().selectAll(b).forEach(x->names.put(x.getMaDanhMuc(),x.getTenDanhMuc()));return ds.stream().map(x->new Object[]{x.getMaGiaoDichKho(),names.getOrDefault(x.getMaHangHoa(),"#"+x.getMaHangHoa()),x.getLoaiGiaoDich(),x.getSoLuong(),x.getTonThucTe(),x.getDonGia(),x.getNgayGiaoDich(),x.getTrangThai(),x.getGhiChu()}).toArray(Object[][]::new);},r->{v.rows(r);v.status("Tổng: "+r.length+" giao dịch | Chỉ đọc");});}
}
