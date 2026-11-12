package vn.vinabacus.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DuLieuMau;
import vn.vinabacus.model.KetQuaGoiNam;
import vn.vinabacus.model.KetQuaPhieuThu;
import vn.vinabacus.model.MoTaManHinh.Trang;
import vn.vinabacus.service.XuLyPhanHe;

/** Nhận yêu cầu từ màn hình phân hệ và chuyển tới tầng nghiệp vụ. */
public final class DieuKhienPhanHe {
    private final XuLyPhanHe xuLy;

    public DieuKhienPhanHe(CoSo coSo) { this(new XuLyPhanHe(coSo)); }
    public DieuKhienPhanHe(XuLyPhanHe xuLy) { this.xuLy=Objects.requireNonNull(xuLy); }
    public List<Trang> layTrang(String menu) { return xuLy.layTrang(menu); }
    public Trang timTrang(String menu,String tab) { return xuLy.timTrang(menu,tab); }
    public List<LinkedHashMap<String,String>> lay(Trang trang) { return xuLy.lay(trang); }
    public DuLieuMau.Bang layLichTuan() { return xuLy.layLichTuan(); }
    public void xoa(Trang trang,int index) { xuLy.xoa(trang,index); }
    public void luuBieuMau(Trang trang,int index,Map<String,String> input) { xuLy.luuBieuMau(trang,index,input); }
    public String ketNoiHoSo(String ten,Map<String,String> hocVien) { return xuLy.ketNoiHoSo(ten,hocVien); }
    public List<String> tuyChonTruong(String truong) { return xuLy.tuyChonTruong(truong); }
    public void xuatCSV(Trang trang,Path target) throws IOException { xuLy.xuatCSV(trang,target); }
    public List<Map<String,String>> layHocVienDiemDanh() { return xuLy.layHocVienDiemDanh(); }
    public String trangThaiDiemDanh(String ngay,String lop,String buoi,String maHocVien) {
        return xuLy.trangThaiDiemDanh(ngay,lop,buoi,maHocVien);
    }
    public int luuDiemDanh(String ngay,String lop,String buoi,List<Map<String,String>> rows) {
        return xuLy.luuDiemDanh(ngay,lop,buoi,rows);
    }
    public KetQuaGoiNam tinhGoiNam(List<Integer> courses,int discount) { return xuLy.tinhGoiNam(courses,discount); }
    public KetQuaGoiNam dangKyGoiNam(String hocVien,String batDau,String ketThuc,List<Integer> courses,int discount) {
        return xuLy.dangKyGoiNam(hocVien,batDau,ketThuc,courses,discount);
    }
    public KetQuaPhieuThu lapPhieuThu(String hocVien,String amount,String method,List<Integer> debtIndices) {
        return xuLy.lapPhieuThu(hocVien,amount,method,debtIndices);
    }
    public long capNhatKho(String action,int productIndex,String quantity,String note) {
        return xuLy.capNhatKho(action,productIndex,quantity,note);
    }
    public void xetHocBu(String hocVien,String course,String absence,String completed,String date,String reason) {
        xuLy.xetHocBu(hocVien,course,absence,completed,date,reason);
    }
    public void chotThang() { xuLy.chotThang(); }
    public long docSo(String value) { return XuLyPhanHe.docSo(value); }
}
