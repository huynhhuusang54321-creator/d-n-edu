package Tv2.Controller;

import Tv2.View.HocVienPage;
import Model.*;
import Service.*;
import java.util.*;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import javax.swing.SwingWorker;
import MainPage.Controller.PageTasks;
import MainPage.View.Workspace;

/** Controller sở hữu truy xuất, bộ lọc và trạng thái trang học viên. */
public class HocVienController {
    public static final String MENU_KEY="HOC_VIEN";
    private final HocVienPage view;
    private final int maCoSo;
    private final HocVienService hocVienService=new HocVienService();
    private final DangKyHocService dangKyService=new DangKyHocService();
    private List<HocVienModel> students=List.of();
    private List<DangKyHocModel> registrations=List.of();
    private boolean busy, resetting;
    private String loadNote="";
    private static final String[] STATES={"Tất cả","Đang hoạt động","Tạm nghỉ","Nghỉ học","Hồ sơ lỗi"};
    private static final String[] STATE_CODES={"","CHINH_THUC","TAM_NGHI","NGHI_HOC"};
    private static final String[] ASSIGNMENTS={"Tất cả","Đã xếp lớp","Chờ xếp lớp","Chưa đăng ký / lớp không còn hoạt động","Chưa xếp lớp (tất cả)"};
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private record Data(List<HocVienModel> students,List<DangKyHocModel> registrations){}
    public HocVienController(HocVienPage view,int maCoSo){this(view,maCoSo,"Mã cơ sở: "+maCoSo);}
    public HocVienController(HocVienPage view,int maCoSo,String tenCoSo){
        this.view=Objects.requireNonNull(view); if(maCoSo<=0)throw new IllegalArgumentException("Mã cơ sở không hợp lệ"); this.maCoSo=maCoSo;
        view.setTitleText("QUẢN LÝ HỌC VIÊN — "+tenCoSo); view.setContextText("Mã cơ sở: "+maCoSo);
        view.setColumns(new String[]{"Mã","Họ tên","Ngày sinh","Giới tính","Người liên hệ","SĐT","Địa chỉ","Trạng thái","Ghi chú"},new int[]{75,180,105,90,160,120,210,140,230});
        view.setFilters(STATES,ASSIGNMENTS);
        view.setAddEnabled(true,"Tạo hồ sơ trực tiếp");
        view.onAdd(e->TiepNhanController.open(view,maCoSo,true,()->load()));
        view.onPopup(i->{if(busy)return;int id=view.selectedId();if(id<=0)return;if(i==2){deleteProfile(id);return;}HocVienModel h=students.stream().filter(x->x.getMaHocVien()==id).findFirst().orElse(null);if(h==null)return;if(i==0){HoSoHocVienController.openWindow(view,maCoSo,id,()->load());}else if(i==1){java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection("Mã hồ sơ: "+h.getMaHoSo()+"\nHọ tên: "+h.getHoTen()+"\nNgày sinh: "+PageTasks.text(h.getNgaySinh())+"\nGiới tính: "+gender(h.getGioiTinh())+"\nPhụ huynh: "+PageTasks.text(h.getTenPhuHuynh())+"\nSĐT: "+PageTasks.text(h.getSoDienThoai())+"\nEmail: "+PageTasks.text(h.getEmail())+"\nĐịa chỉ: "+PageTasks.text(h.getDiaChi())+"\nGhi chú: "+PageTasks.text(h.getGhiChu())),null);}});
        view.onSearch(e->render()); view.onFilter(e->render()); view.onReset(e->{resetting=true;try{view.resetInputs();}finally{resetting=false;}render();}); view.onReload(e->load(true));
        load();
    }
    private void deleteProfile(int id){
        if(busy)return;
        HocVienModel h=students.stream().filter(x->x.getMaCoSo()==maCoSo && x.getMaHocVien()==id).findFirst().orElse(null);
        if(h==null)return;
        if(!MainPage.View.Workspace.confirm(view,"Xóa vĩnh viễn hồ sơ "+h.getHoTen()+" (mã "+id+")?\nKhông thể hoàn tác. Database có thể từ chối nếu còn dữ liệu liên quan."))return;
        busy=true;view.setBusy(true);view.setAddEnabled(false,"Đang xóa hồ sơ");view.setStatusText("Đang xóa hồ sơ...");
        new SwingWorker<Boolean,Void>(){
            protected Boolean doInBackground(){return hocVienService.delete(maCoSo,id);}
            protected void done(){
                try{if(!get())javax.swing.JOptionPane.showMessageDialog(view,"Không xóa được hồ sơ. Kiểm tra dữ liệu liên quan hoặc kết nối Database.","Không thể xóa",javax.swing.JOptionPane.WARNING_MESSAGE);}
                catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(view,HocVienController.error(ex),"Lỗi xóa hồ sơ",javax.swing.JOptionPane.ERROR_MESSAGE);}
                finally{busy=false;load();}
            }
        }.execute();
    }
    public int getMaCoSo(){return maCoSo;}
    public void load(){load(false);}
    public void load(boolean force){
        if(busy)return;busy=true;view.setBusy(true);view.setAddEnabled(false,"Đang tải dữ liệu");view.setStatusText("Đang tải dữ liệu học viên...");
        new SwingWorker<Data,Void>(){
            protected Data doInBackground(){return new Data(force?hocVienService.reload(maCoSo):hocVienService.selectAll(maCoSo),force?dangKyService.reload(maCoSo):dangKyService.selectAll(maCoSo));}
            protected void done(){try{
                Data d=get(); students=d.students().stream().filter(x->x.getMaCoSo()==maCoSo).sorted(Comparator.comparingInt(HocVienModel::getMaHocVien).reversed()).toList();
                registrations=d.registrations().stream().filter(x->x.getMaCoSo()==maCoSo).toList();
                loadNote=students.isEmpty()||registrations.isEmpty()?" | Nguồn dữ liệu rỗng: nếu bất thường, kiểm tra SQL Server.":"";
            }catch(Exception ex){students=List.of();registrations=List.of();loadNote=" | Lỗi tải: "+error(ex);}finally{busy=false;view.setBusy(false);view.setAddEnabled(true,"Tạo hồ sơ trực tiếp");render();}}
        }.execute();
    }
    private int assignment(HocVienModel h){
        boolean assigned=false,pending=false;
        for(DangKyHocModel d:registrations){if(d.getMaHocVien()!=h.getMaHocVien())continue;
            if("CHO_XEP_LOP".equals(d.getTrangThai()))pending=true;
            if(d.getMaLop()!=null && Set.of("DANG_HOC","TAM_NGHI").contains(d.getTrangThai()))assigned=true;
        }return assigned?1:pending?2:3;
    }
    private void render(){if(busy||resetting)return;
        int s=view.getStateIndex(),a=view.getAssignmentIndex();String q=normalize(view.getSearchText());List<Object[]> rows=new ArrayList<>();
        for(HocVienModel h:students){String state=h.getTrangThai();if(!Set.of("CHINH_THUC","TAM_NGHI","NGHI_HOC").contains(state))continue;
            if(s==4&&!invalid(h))continue;if(s>0&&s<4&&!STATE_CODES[s].equals(state))continue;if(a==4&&assignment(h)==1)continue;if(a>0&&a<4&&assignment(h)!=a)continue;if(!matches(h,q))continue;
            rows.add(new Object[]{h.getMaHocVien(),h.getHoTen(),h.getNgaySinh()==null?"":DATE.format(h.getNgaySinh()),gender(h.getGioiTinh()),h.getTenPhuHuynh(),h.getSoDienThoai(),h.getDiaChi(),status(state),h.getGhiChu()});
        }view.setRows(rows.toArray(Object[][]::new));view.setStatusText("Tìm thấy "+rows.size()+" học viên | Xếp lớp theo đăng ký hiện tại"+loadNote);
    }
    private boolean invalid(HocVienModel h){String phone=PageTasks.text(h.getSoDienThoai()).replaceAll("[ +()-]", "");return h.getHoTen()==null||h.getHoTen().isBlank()||h.getNgaySinh()==null||h.getNgaySinh().isAfter(java.time.LocalDate.now())||h.getTenPhuHuynh()==null||h.getTenPhuHuynh().isBlank()||!phone.matches("[0-9]{9,15}")||(h.getEmail()!=null&&!h.getEmail().isBlank()&&!h.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"));}
    static String normalize(String s){return s==null?"":Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).trim();}
    static boolean matches(HocVienModel h,String q){return q.isEmpty()||normalize(h.getHoTen()).contains(q)||normalize(h.getMaHoSo()).contains(q)||normalize(h.getSoDienThoai()).contains(q)||String.valueOf(h.getMaHocVien()).contains(q);}
    static String gender(String code){if(code==null)return "";return switch(code){case "NAM"->"Nam";case "NU"->"Nữ";case "KHAC"->"Khác";default->code;};}
    static String status(String code){if(code==null)return "";return switch(code){case "CHINH_THUC"->"Đang hoạt động";case "TAM_NGHI"->"Tạm nghỉ";case "NGHI_HOC"->"Nghỉ học";case "TIEP_NHAN"->"Tiếp nhận";case "HOC_THU"->"Học thử";case "CHO_XEP_LOP"->"Chờ xếp lớp";case "KHONG_DANG_KY"->"Không đăng ký";default->code;};}
    static String error(Throwable t){while(t.getCause()!=null&&t.getCause()!=t)t=t.getCause();return Objects.toString(t.getMessage(),t.getClass().getSimpleName());}
}
