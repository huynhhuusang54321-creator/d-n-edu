package vn.vinabacus;

import vn.vinabacus.model.MoTaManHinh;
import vn.vinabacus.repository.KhoDuLieuManHinh;
import vn.vinabacus.model.CoSo;
import java.util.*;

/** Test tĩnh không cần SQL/GUI. Chạy với java -ea. */
public final class KiemThuGiaoDienDayDu {
    public static void main(String[] args){
        int views=0;
        Set<String> distinct=new HashSet<>();
        Set<String> branches=new HashSet<>();
        for(MoTaManHinh.Trang p:KhoDuLieuManHinh.tatCaTrang()){
            views++;branches.add(p.coSo().name()+"/"+p.menu());
            assert p.coSo().layMenu().contains(p.menu()):p.khoa();
            assert distinct.add(p.khoa()):"Trùng tab "+p.khoa();
            assert p.cot().length>0&&p.truong().length>0&&p.thaoTac().length>0:p.khoa();
            for(String[] row:p.mau())assert row.length==p.cot().length:"Sai so cot "+p.khoa();
            assert !KhoDuLieuManHinh.lay(p).isEmpty():p.khoa();
        }
        assert views>=60:views;
        assert branches.size()==CoSo.MAM_NON.layMenu().size()+CoSo.TRUNG_TAM.layMenu().size()-2:branches.size();
        MoTaManHinh.Trang students=KhoDuLieuManHinh.layTrang(CoSo.MAM_NON,"Quản lý học viên").get(0);
        int count=KhoDuLieuManHinh.lay(students).size();
        List<LinkedHashMap<String,String>> snapshot=KhoDuLieuManHinh.lay(students);
        String originalName=snapshot.get(0).get("Họ và tên");
        snapshot.get(0).put("Họ và tên","Không được ghi qua kết quả đọc");
        snapshot.clear();
        assert KhoDuLieuManHinh.lay(students).size()==count;
        assert originalName.equals(KhoDuLieuManHinh.lay(students).get(0).get("Họ và tên"));
        String originalColumn=students.cot()[0];
        String originalSample=students.mau()[0][0];
        students.cot()[0]="Không được sửa đặc tả gốc";
        students.mau()[0][0]="Không được sửa dữ liệu khởi tạo gốc";
        students=KhoDuLieuManHinh.layTrang(CoSo.MAM_NON,"Quản lý học viên").get(0);
        assert originalColumn.equals(students.cot()[0]):"Kết quả đọc đặc tả không được làm đổi cột gốc";
        assert originalSample.equals(students.mau()[0][0]):"Kết quả đọc đặc tả không được làm đổi dữ liệu mẫu gốc";
        Map<String,String> newStudent=new LinkedHashMap<>();newStudent.put("Họ và tên","Học viên kiểm thử");
        newStudent.put("Ngày sinh","01/01/2020");
        KhoDuLieuManHinh.them(students,newStudent);
        newStudent.put("Ngày sinh","Không được thay đổi dữ liệu đã lưu");
        assert KhoDuLieuManHinh.lay(students).size()==count+1;
        assert KhoDuLieuManHinh.lay(students).get(count).get("Ngày sinh").equals("01/01/2020");
        KhoDuLieuManHinh.sua(students,count,Map.of("Ghi chú","Giữ trường ngoài bảng"));
        assert KhoDuLieuManHinh.lay(students).get(count).get("Ghi chú").equals("Giữ trường ngoài bảng");
        KhoDuLieuManHinh.xoa(students,count);
        assert KhoDuLieuManHinh.lay(students).size()==count;
        KhoDuLieuManHinh.diemDanh("TT|2026-11-10|A1|B001|HV001","Có mặt");
        KhoDuLieuManHinh.diemDanh("TT|2026-11-10|A1|B001|HV001","Vắng có phép");
        assert KhoDuLieuManHinh.tongDiemDanh()==1;
        assert "Vắng có phép".equals(KhoDuLieuManHinh.layDiemDanh("TT|2026-11-10|A1|B001|HV001"));
        System.out.println("PASS: "+views+" tabs, "+branches.size()+" menus, demo CRUD fields, defensive copies, attendance uniqueness.");
        System.out.println("NOTE: Tests do not verify JDBC, SQL transactions, authentication, business production logic.");
    }
}
