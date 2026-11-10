package vn.vinabacus;

import vn.vinabacus.doituong.phanhe.MoTaManHinh;
import vn.vinabacus.xuly.phanhe.KhoDuLieuManHinh;
import vn.vinabacus.model.CoSo;
import java.util.*;

/** Test tĩnh không cần SQL/GUI. Chạy với java -ea. */
public final class KiemThuGiaoDienDayDu {
    public static void main(String[] args){
        int views=0;
        Set<String> distinct=new HashSet<>();
        Set<String> branches=new HashSet<>();
        for(MoTaManHinh.Trang p:MoTaManHinh.tatCa()){
            views++;branches.add(p.coSo().name()+"/"+p.menu());
            assert p.coSo().layMenu().contains(p.menu()):p.khoa();
            assert distinct.add(p.khoa()):"Trùng tab "+p.khoa();
            assert p.cot().length>0&&p.truong().length>0&&p.thaoTac().length>0:p.khoa();
            for(String[] row:p.mau())assert row.length==p.cot().length:"Sai so cot "+p.khoa();
            assert !KhoDuLieuManHinh.lay(p).isEmpty():p.khoa();
        }
        assert views>=60:views;
        assert branches.size()==CoSo.MAM_NON.layMenu().size()+CoSo.TRUNG_TAM.layMenu().size()-2:branches.size();
        MoTaManHinh.Trang students=MoTaManHinh.lay(CoSo.MAM_NON,"Quản lý học viên").get(0);
        int count=KhoDuLieuManHinh.lay(students).size();
        Map<String,String> newStudent=new LinkedHashMap<>();newStudent.put("Họ và tên","Học viên kiểm thử");
        newStudent.put("Ngày sinh","01/01/2020");
        KhoDuLieuManHinh.them(students,newStudent);
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
        System.out.println("PASS: "+views+" tabs, "+branches.size()+" menus, demo CRUD fields, attendance uniqueness.");
        System.out.println("NOTE: Tests do not verify JDBC, SQL transactions, authentication, business production logic.");
    }
}
