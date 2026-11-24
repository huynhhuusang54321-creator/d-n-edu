package Tv2.Controller;

import Tv2.View.HoSoHocVienPage;
import MainPage.View.Workspace;
import MainPage.Controller.PageTasks;
import Model.*;
import Service.*;
import java.awt.Component;
import java.time.LocalDate;
import java.util.*;
import javax.swing.SwingWorker;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/** Hồ sơ: mọi dữ liệu, quyền mở chức năng và dấu thiếu thông tin do Controller quyết định. */
public class HoSoHocVienController {
    private final HoSoHocVienPage v;
    private final int b,id;
    private final Runnable changed;
    private final Set<String> dismissed=new HashSet<>();
    private Data snapshot;
    private boolean busy;
    private final javax.swing.JButton reload, edit, restore;
    private record Course(DangKyHocModel root,DangKyHocModel latest,String name,String lop,long done,boolean hasSchedule){}
    private record Data(HocVienModel student,List<Course> courses,boolean assigned, Map<Integer,String> staff,Map<Integer,String> cats){}
    public HoSoHocVienController(HoSoHocVienPage v,int b,int id){this(v,b,id,()->{});}
    public HoSoHocVienController(HoSoHocVienPage v,int b,int id,Runnable changed){
        this.v=v;this.b=b;this.id=id;this.changed=changed;
        v.title("HỒ SƠ HỌC VIÊN","Cơ sở #"+b);
        edit=v.action("Sửa hồ sơ",()->SuaHoSoHocVienController.open(v,b,id,this::changed));
        reload=v.action("Tải lại",()->load(true));restore=v.action("Hiện lại dấu đỏ",()->{dismissed.clear();render();});
        edit.setEnabled(false);restore.setEnabled(false);load(false);
    }
    private record WindowKey(Component owner,int branch,int student){}
    // Chỉ lưu cửa sổ đang mở, không cache dữ liệu nghiệp vụ.
    private static final Map<WindowKey,JDialog> WINDOWS=new HashMap<>();
    public static void openWindow(Component owner,int b,int id){openWindow(owner,b,id,()->{});}
    public static void openWindow(Component owner,int b,int id,Runnable changed){
        if(!SwingUtilities.isEventDispatchThread()){SwingUtilities.invokeLater(()->openWindow(owner,b,id,changed));return;}
        WindowKey key=new WindowKey(owner,b,id);JDialog existing=WINDOWS.get(key);
        if(existing!=null&&existing.isDisplayable()){existing.setVisible(true);existing.toFront();existing.requestFocus();return;}
        HoSoHocVienPage page=new HoSoHocVienPage();JDialog dialog=page.createWindow(owner,"Hồ sơ #"+id+" — Cơ sở #"+b);
        WINDOWS.put(key,dialog);
        // Khi MainPage thay trang/cơ sở, đóng hồ sơ cũ để không thao tác nhầm cơ sở.
        HierarchyListener listener=e->{if((e.getChangeFlags()&(HierarchyEvent.SHOWING_CHANGED|HierarchyEvent.DISPLAYABILITY_CHANGED))!=0&&owner!=null&&!owner.isShowing())dialog.dispose();};
        if(owner!=null)owner.addHierarchyListener(listener);
        dialog.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){WINDOWS.remove(key,dialog);if(owner!=null)owner.removeHierarchyListener(listener);}});
        new HoSoHocVienController(page,b,id,()->{if(owner==null||owner.isShowing())changed.run();});
        dialog.setVisible(true);
    }
    static int root(DangKyHocModel d){return d.getMaDangKyGoc()==null?d.getMaDangKy():d.getMaDangKyGoc();}
    private void changed(){load(false);changed.run();}
    private void load(boolean force){
        if(busy)return;busy=true;reload.setEnabled(false);edit.setEnabled(false);restore.setEnabled(false);v.status("Đang tải hồ sơ...");
        new SwingWorker<Data,Void>(){
            protected Data doInBackground(){return read(force);}
            protected void done(){busy=false;reload.setEnabled(true);try{snapshot=get();render();}catch(Exception ex){snapshot=null;v.identity("HV","Không tải được hồ sơ","",List.of());v.courses(List.of(),"Không có dữ liệu để hiển thị.",null);v.status("WARNING — "+HocVienController.error(ex));}}
        }.execute();
    }
    private Data read(boolean force){
        var hs=new HocVienService();if(force)hs.reload(b);var h=hs.getByMaHocVien(b,id);PageTasks.require(h!=null&&h.getMaCoSo()==b,"Không tìm thấy học viên tại cơ sở này");
        var ds=new DangKyHocService();var rs=(force?ds.reload(b):ds.selectAll(b)).stream().filter(x->x.getMaHocVien()==id&&x.getMaCoSo()==b).toList();
        var as=new DiemDanhService();var dd=(force?as.reload(b):as.selectAll(b)).stream().filter(x->x.getMaHocVien()==id&&x.getMaCoSo()==b).toList();
        var ls=new LopHocService();var classes=force?ls.reload(b):ls.selectAll(b);var cs=new DanhMucService();var cats=force?cs.reload(b):cs.selectAll(b);var bs=new BuoiHocService();var valid=(force?bs.reload(b):bs.selectAll(b)).stream().filter(x->!"HUY".equals(x.getTrangThai())).map(BuoiHocModel::getMaBuoiHoc).collect(java.util.stream.Collectors.toSet());
        var ns=new NhanSuService();Map<Integer,String> staff=new HashMap<>();for(var n:force?ns.reload(b):ns.selectAll(b))staff.put(n.getMaNhanSu(),n.getHoTen());Map<Integer,String> catNames=new HashMap<>();for(var cat:cats)catNames.put(cat.getMaDanhMuc(),cat.getTenDanhMuc());
        List<Course> courses=new ArrayList<>();
        for(var r:rs){if(r.getMaDangKyGoc()!=null)continue;var segments=rs.stream().filter(x->root(x)==r.getMaDangKy()).toList();var latest=segments.stream().max(Comparator.comparingInt(DangKyHocModel::getMaDangKy)).orElse(r);var ids=segments.stream().map(DangKyHocModel::getMaDangKy).collect(java.util.stream.Collectors.toSet());
            long n=dd.stream().filter(d->ids.contains(d.getMaDangKy())&&d.isTinhBuoi()&&valid.contains(d.getMaBuoiHoc())&&!Set.of("CHUA_DIEM_DANH","HUY").contains(d.getTrangThai())).map(DiemDanhModel::getMaBuoiHoc).distinct().count();
            String name=r.getMaKhoaHoc()==null?"Bán trú / chưa có khóa":catNames.getOrDefault(r.getMaKhoaHoc(),"Khóa #"+r.getMaKhoaHoc());String lop=classes.stream().filter(x->Objects.equals(x.getMaLop(),latest.getMaLop())).map(LopHocModel::getTenLop).findFirst().orElse(latest.getMaLop()==null?"Chưa xếp lớp":"Lớp #"+latest.getMaLop());boolean has=segments.stream().anyMatch(x->x.getMaLop()!=null&&x.getNgayBatDau()!=null);courses.add(new Course(r,latest,name,lop,n,has));
        }
        boolean assigned=rs.stream().anyMatch(x->x.getMaLop()!=null&&Set.of("DANG_HOC","TAM_NGHI").contains(x.getTrangThai()));return new Data(h,courses,assigned,staff,catNames);
    }
    private void field(List<HoSoHocVienPage.FieldItem> fields,String key,String label,Object value,boolean invalid){
        String text=PageTasks.text(value);boolean empty=text.isBlank();boolean marked=(empty||invalid)&&!dismissed.contains(key);fields.add(new HoSoHocVienPage.FieldItem(label,empty?"— Chưa có thông tin —":text,marked,()->{dismissed.add(key);render();}));
    }
    private void field(List<HoSoHocVienPage.FieldItem> fields,String key,String label,Object value){field(fields,key,label,value,false);}
    private static String named(Map<Integer,String> names,Integer id){return id==null?"":names.getOrDefault(id,"#"+id);}
    private void render(){
        if(snapshot==null||busy)return;var h=snapshot.student();List<HoSoHocVienPage.FieldItem> fields=new ArrayList<>();
        field(fields,"code","Mã hồ sơ",h.getMaHoSo());field(fields,"name","Họ và tên",h.getHoTen());field(fields,"birth","Ngày sinh",h.getNgaySinh(),h.getNgaySinh()!=null&&h.getNgaySinh().isAfter(LocalDate.now()));field(fields,"gender","Giới tính",HocVienController.gender(h.getGioiTinh()));
        field(fields,"parent","Phụ huynh",h.getTenPhuHuynh());String phone=PageTasks.text(h.getSoDienThoai()).replaceAll("[ +()-]", "");field(fields,"phone","Điện thoại",h.getSoDienThoai(),!phone.matches("[0-9]{9,15}"));field(fields,"email","Email",h.getEmail(),h.getEmail()!=null&&!h.getEmail().isBlank()&&!h.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"));field(fields,"address","Địa chỉ",h.getDiaChi());
        field(fields,"state","Trạng thái",HocVienController.status(h.getTrangThai()));field(fields,"created","Ngày tiếp nhận",h.getNgayTao());field(fields,"official","Ngày chính thức",h.getNgayChinhThuc());field(fields,"advisor","Người tư vấn",named(snapshot.staff(),h.getMaNguoiTuVan()));field(fields,"source","Nguồn tiếp nhận",named(snapshot.cats(),h.getMaNguonTiepNhan()));field(fields,"creator","Người tạo",named(snapshot.staff(),h.getMaNguoiTao()));field(fields,"photo","Ảnh đại diện",h.getAnhDaiDien());field(fields,"note","Ghi chú",h.getGhiChu());
        String full=PageTasks.text(h.getHoTen());String initials=Arrays.stream(full.trim().split("\\s+")).filter(x->!x.isBlank()).map(x->x.substring(0,1)).reduce("",String::concat);if(initials.length()>2)initials=initials.substring(initials.length()-2);if(SwingUtilities.getWindowAncestor(v) instanceof JDialog dialog)dialog.setTitle("Hồ sơ — "+full+" | Cơ sở #"+b);v.identity(initials.isBlank()?"HV":initials.toUpperCase(Locale.ROOT),full.isBlank()?"Chưa có họ tên":full,"Hồ sơ "+PageTasks.text(h.getMaHoSo())+"  •  Cơ sở #"+b,fields);
        boolean canAssign=!snapshot.assigned()&&Set.of("CHINH_THUC","CHO_XEP_LOP").contains(h.getTrangThai());List<HoSoHocVienPage.CourseItem> items=new ArrayList<>();boolean hasPending=false;long courseMarks=0;
        for(Course course:snapshot.courses()){var r=course.root();var latest=course.latest();Integer total=r.getTongSoBuoi();int percent=total==null||total<=0?0:(int)Math.min(100,Math.round(course.done()*100.0/total));String progress=total==null||total<=0?course.done()+" buổi / chưa có tổng":course.done()+" / "+total+" buổi ("+percent+"%)";
            boolean pending=latest.getMaLop()==null&&"CHO_XEP_LOP".equals(latest.getTrangThai());hasPending|=pending;
            Runnable assign=canAssign&&pending?()->XepLopController.open(v,b,id,latest.getMaDangKy(),this::changed):null;Runnable diagram=course.hasSchedule()&&total!=null&&total>0?()->SoDoHocController.open(v,b,id,r.getMaDangKy()):null;
            List<HoSoHocVienPage.FieldItem> courseFields=new ArrayList<>();String prefix="course."+r.getMaDangKy()+".";
            field(courseFields,prefix+"class","Lớp",latest.getMaLop()==null?null:course.lop());field(courseFields,prefix+"start","Bắt đầu",latest.getNgayBatDau());field(courseFields,prefix+"total","Tổng số buổi",total,total!=null&&total<=0);field(courseFields,prefix+"end","Kết thúc dự kiến",latest.getNgayKetThucDuKien());field(courseFields,prefix+"fee","Học phí thỏa thuận",r.getHocPhiThoaThuan());field(courseFields,prefix+"note","Ghi chú đăng ký",latest.getGhiChu());courseMarks+=courseFields.stream().filter(HoSoHocVienPage.FieldItem::marked).count();
            items.add(new HoSoHocVienPage.CourseItem(course.name(),course.lop()+"  •  Đăng ký #"+r.getMaDangKy(),"Trạng thái: "+latest.getTrangThai(),"Đăng ký: "+PageTasks.text(r.getNgayDangKy())+"  |  Kết thúc dự kiến: "+(latest.getNgayKetThucDuKien()==null?"Chưa xác định":latest.getNgayKetThucDuKien()),percent,progress,courseFields,diagram,assign,()->editCourse(course)));
        }
        v.courses(items,"Chưa có khóa học đăng ký.",canAssign&&!hasPending?()->XepLopController.open(v,b,id,this::changed):null);edit.setEnabled(true);restore.setEnabled(!dismissed.isEmpty());v.status("Dấu đỏ: "+(fields.stream().filter(HoSoHocVienPage.FieldItem::marked).count()+courseMarks)+" | Nhấn chấm đỏ để ẩn cảnh báo.");
    }
    private void editCourse(Course course){
        var latest=course.latest();boolean editableEnd=latest.getMaLop()!=null&&Set.of("DANG_HOC","TAM_NGHI").contains(latest.getTrangThai());var input=v.editCourse(course.name(),PageTasks.text(latest.getGhiChu()),PageTasks.text(latest.getNgayKetThucDuKien()),editableEnd);if(input==null)return;
        PageTasks.run(v,()->{LocalDate end=input.end().isBlank()?null:LocalDate.parse(input.end().trim());var svc=new DangKyHocService();var saved=svc.modify(b,latest.getMaDangKy(),x->{PageTasks.require(x.getMaHocVien()==id,"Đăng ký không thuộc học viên");x.setGhiChu(TiepNhanController.blank(input.note()));if(editableEnd){PageTasks.require(Objects.equals(x.getMaLop(),latest.getMaLop())&&Set.of("DANG_HOC","TAM_NGHI").contains(x.getTrangThai()),"Đăng ký đã thay đổi; tải lại hồ sơ");PageTasks.require(end==null||x.getNgayBatDau()==null||!end.isBefore(x.getNgayBatDau()),"Ngày kết thúc trước ngày bắt đầu");x.setNgayKetThucDuKien(end);}return true;});PageTasks.require(saved!=null,"Không sửa được đăng ký");return saved;},x->changed());
    }
}
