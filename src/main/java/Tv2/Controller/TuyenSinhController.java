package Tv2.Controller;

import Tv2.View.TuyenSinhPage;
import MainPage.Controller.MainPageController;
import Model.*;
import Service.*;
import java.util.*;
import java.io.IOException;
import javax.swing.SwingWorker;

/** Điều phối tuyển sinh, phân nhóm, lọc học thử và cập nhật Badge. */
public class TuyenSinhController {
    public static final String MENU_KEY="TUYEN_SINH";
    private final TuyenSinhPage view; private final int maCoSo;
    private final HocVienService hocVienService=new HocVienService();
    private final TuongTacHocVienService interactionService=new TuongTacHocVienService();
    private final DiemDanhService attendanceService=new DiemDanhService();
    private final BuoiHocService lessonService=new BuoiHocService();
    private final DanhMucService categoryService=new DanhMucService();
    private List<HocVienModel> students=List.of();
    private Map<Integer,TuongTacHocVienModel> latestNeed=Map.of();
    private Map<Integer,String> courseNames=Map.of();
    private Set<Integer> attended=Set.of(), converted=Set.of();
    private Map<Integer,String> trialSlots=Map.of();
    private boolean busy,resetting;private String loadNote="";
    private static final String[] KEYS={"TIEP_NHAN","HOC_THU","CHO_XAC_NHAN","CHO_XEP_LOP","DA_CHUYEN_DOI","KHONG_DANG_KY"};
    private static final String[] TITLES={"Tiếp nhận","Học thử","Chờ xác nhận","Chờ xếp lớp","Đã chuyển đổi","Không đăng ký"};
    private static final String[] DESCS={"Hồ sơ mới tiếp nhận, đang tư vấn.","Hồ sơ đang trong giai đoạn học thử.","Database hiện tại chưa hỗ trợ trạng thái chờ xác nhận.","Đã thống nhất đăng ký, đang chờ xếp lớp.","Hồ sơ chính thức có lịch sử chuyển đổi tuyển sinh.","Hồ sơ không tiếp tục đăng ký."};
    private record Data(List<HocVienModel> students,Map<Integer,TuongTacHocVienModel> needs,Map<Integer,String> courses,Set<Integer> attended,Set<Integer> converted,Map<Integer,String> slots,String note){}
    public TuyenSinhController(TuyenSinhPage view,int maCoSo){this(view,maCoSo,"Mã cơ sở: "+maCoSo);}
    public TuyenSinhController(TuyenSinhPage view,int maCoSo,String tenCoSo){
        this.view=Objects.requireNonNull(view);if(maCoSo<=0)throw new IllegalArgumentException("Mã cơ sở không hợp lệ");this.maCoSo=maCoSo;
        view.setTitleText("QUẢN LÝ TUYỂN SINH — "+tenCoSo);view.setContextText("Theo dõi hồ sơ từ tiếp nhận đến chuyển đổi thành học viên");
        String[] cols={"Mã","Họ tên","SĐT","Người liên hệ","Nhu cầu","Trạng thái","Đã học thử","Lịch thử gần nhất","Ghi chú"};int[] widths={75,210,125,170,220,150,105,170,250};
        for(int i=0;i<KEYS.length;i++)view.addTab(KEYS[i],TITLES[i]+" (0)",DESCS[i],cols,widths);
        view.setTrialOptions(new String[]{"Tất cả","Đã học thử","Chưa học thử / chưa có xác nhận"});
        view.setAddEnabled(true,"Tạo hồ sơ tiếp nhận");
        view.onAdd(e->TiepNhanController.open(view,maCoSo,false,()->load()));
        view.onPopup(i->{if(busy)return;int id=view.selectedId();if(id<=0)return;if(i==4){deleteProfile(id);return;}var t=latestNeed.get(id);Integer course=t==null?null:t.getMaKhoaHocMongMuon();switch(i){case 0->{HoSoHocVienController.openWindow(view,maCoSo,id,()->load());}case 1->HocThuController.open(view,maCoSo,id,course,()->load());case 2->HocThuController.convert(view,maCoSo,id,course,()->load());case 3->XepLopController.open(view,maCoSo,id,()->load());}});
        view.onSearch(e->render());view.onFilter(e->render());view.onReset(e->{resetting=true;try{view.resetInputs();}finally{resetting=false;}render();});view.onReload(e->load(true));load();
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
    public void load(boolean force){if(busy)return;busy=true;view.setBusy(true);view.setAddEnabled(false,"Đang tải dữ liệu");view.setStatusText("Đang tải dữ liệu tuyển sinh...");
        new SwingWorker<Data,Void>(){
            protected Data doInBackground(){
                List<HocVienModel> hs=(force?hocVienService.reload(maCoSo):hocVienService.selectAll(maCoSo)).stream().filter(x->x.getMaCoSo()==maCoSo).sorted(Comparator.comparingInt(HocVienModel::getMaHocVien).reversed()).toList();
                List<TuongTacHocVienModel> logs=force?interactionService.reload(maCoSo):interactionService.selectAll(maCoSo);List<DiemDanhModel> dd=force?attendanceService.reload(maCoSo):attendanceService.selectAll(maCoSo);List<BuoiHocModel> lessons=force?lessonService.reload(maCoSo):lessonService.selectAll(maCoSo);List<DanhMucModel> cats=force?categoryService.reload(maCoSo):categoryService.selectAll(maCoSo);
                Map<Integer,TuongTacHocVienModel> needs=new HashMap<>();Set<Integer> conv=new HashSet<>(), trials=new HashSet<>(), validLessons=new HashSet<>();Map<Integer,String> names=new HashMap<>();
                for(DanhMucModel c:cats)if(c.getMaCoSo()==maCoSo)names.put(c.getMaDanhMuc(),c.getTenDanhMuc());
                for(BuoiHocModel b:lessons)if(b.getMaCoSo()==maCoSo&&!"HUY".equals(b.getTrangThai()))validLessons.add(b.getMaBuoiHoc());
                for(DiemDanhModel d:dd)if(d.getMaCoSo()==maCoSo&&"HOC_THU".equals(d.getLoaiThamGia())&&"CO_MAT".equals(d.getTrangThai())&&validLessons.contains(d.getMaBuoiHoc()))trials.add(d.getMaHocVien());
                Comparator<TuongTacHocVienModel> order=Comparator.comparing(TuongTacHocVienModel::getNgayTao,Comparator.nullsFirst(Comparator.naturalOrder())).thenComparingInt(TuongTacHocVienModel::getMaTuongTac);
                for(TuongTacHocVienModel t:logs){if(t.getMaCoSo()!=maCoSo)continue;
                    if("DOI_TRANG_THAI".equals(t.getLoaiTuongTac())&&"CHINH_THUC".equals(t.getTrangThaiSau())&&Set.of("TIEP_NHAN","HOC_THU","CHO_XEP_LOP").contains(Objects.toString(t.getTrangThaiTruoc(),"")))conv.add(t.getMaHocVien());
                    if(t.getMaKhoaHocMongMuon()!=null||"NGUYEN_VONG".equals(t.getLoaiTuongTac())||"TIEP_NHAN".equals(t.getLoaiTuongTac()))needs.merge(t.getMaHocVien(),t,(a,b)->order.compare(a,b)>=0?a:b);
                }
                Map<Integer,String> slots=new HashMap<>();Map<Integer,BuoiHocModel> byLesson=new HashMap<>();for(var lesson:lessons)byLesson.put(lesson.getMaBuoiHoc(),lesson);Map<Integer,java.time.LocalDate> lastDate=new HashMap<>();for(var d:dd){if(!"HOC_THU".equals(d.getLoaiThamGia())||"HUY".equals(d.getTrangThai()))continue;var lesson=byLesson.get(d.getMaBuoiHoc());if(lesson==null||"HUY".equals(lesson.getTrangThai()))continue;var prev=lastDate.get(d.getMaHocVien());if(prev==null||!lesson.getNgayHoc().isBefore(prev)){lastDate.put(d.getMaHocVien(),lesson.getNgayHoc());slots.put(d.getMaHocVien(),lesson.getNgayHoc()+" · "+lesson.getGioBatDau());}}
                String note=hs.isEmpty()||logs.isEmpty()||dd.isEmpty()||lessons.isEmpty()||cats.isEmpty()?" | Có nguồn trả về rỗng; kiểm tra SQL nếu bất thường.":"";
                if(hocVienService.isCacheLoaded(maCoSo)){
                    int pending=(int)hs.stream().filter(h->Set.of("TIEP_NHAN","HOC_THU","CHO_XEP_LOP").contains(h.getTrangThai())).count();
                    try{MainPageController.setBadgeCount(maCoSo,MENU_KEY,"HoSoCanXuLy",pending);}catch(IOException ex){note+=" | Không ghi được Badge.";}
                }else note+=" | Giữ Badge cũ do chưa xác định được dữ liệu rỗng.";
                return new Data(hs,needs,names,trials,conv,slots,note);
            }
            protected void done(){try{Data d=get();students=d.students();latestNeed=d.needs();courseNames=d.courses();attended=d.attended();converted=d.converted();trialSlots=d.slots();loadNote=d.note();}
                catch(Exception ex){students=List.of();latestNeed=Map.of();courseNames=Map.of();attended=Set.of();converted=Set.of();trialSlots=Map.of();loadNote=" | Lỗi tải: "+HocVienController.error(ex);}finally{busy=false;view.setBusy(false);view.setAddEnabled(true,"Tạo hồ sơ tiếp nhận");render();}}
        }.execute();
    }
    private String group(HocVienModel h){String s=h.getTrangThai();if(Set.of("TIEP_NHAN","HOC_THU","CHO_XEP_LOP","KHONG_DANG_KY").contains(s))return s;if(converted.contains(h.getMaHocVien())&&Set.of("CHINH_THUC","TAM_NGHI","NGHI_HOC").contains(s))return "DA_CHUYEN_DOI";return "";}
    private String need(HocVienModel h){TuongTacHocVienModel t=latestNeed.get(h.getMaHocVien());if(t==null)return "";if(t.getMaKhoaHocMongMuon()!=null)return courseNames.getOrDefault(t.getMaKhoaHocMongMuon(),"Khóa #"+t.getMaKhoaHocMongMuon());return Objects.toString(t.getNoiDung(),"");}
    private void render(){if(busy||resetting)return;String q=HocVienController.normalize(view.getSearchText());int trial=view.getTrialIndex();Map<String,List<Object[]>> rows=new LinkedHashMap<>();for(String key:KEYS)rows.put(key,new ArrayList<>());int pending=0;
        for(HocVienModel h:students){String key=group(h);if(!rows.containsKey(key))continue;if(Set.of("TIEP_NHAN","HOC_THU","CHO_XEP_LOP").contains(key))pending++;
            boolean did=attended.contains(h.getMaHocVien());if(!HocVienController.matches(h,q)||(trial==1&&!did)||(trial==2&&did))continue;
            rows.get(key).add(new Object[]{h.getMaHocVien(),h.getHoTen(),h.getSoDienThoai(),h.getTenPhuHuynh(),need(h),"DA_CHUYEN_DOI".equals(key)?"Đã chuyển đổi":HocVienController.status(h.getTrangThai()),did?"Có":"Chưa xác nhận",trialSlots.getOrDefault(h.getMaHocVien(),""),h.getGhiChu()});
        }for(int i=0;i<KEYS.length;i++){List<Object[]> r=rows.get(KEYS[i]);view.setRows(KEYS[i],r.toArray(Object[][]::new));view.setTabTitle(KEYS[i],TITLES[i]+" ("+r.size()+")");}view.setStatusText("Cần xử lý: "+pending+" hồ sơ (toàn cơ sở)"+loadNote);
    }
}
