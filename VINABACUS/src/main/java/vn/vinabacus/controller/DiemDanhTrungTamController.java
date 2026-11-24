package vn.vinabacus.controller;


import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.*;
import vn.vinabacus.service.DiemDanhTrungTamService;

public final class DiemDanhTrungTamController {
    private final DiemDanhTrungTamService service = new DiemDanhTrungTamService();
    public List<LuaChon> lop() throws Exception { return service.lop(); }
    public List<LuaChon> taiKhoan() throws Exception { return service.taiKhoan(); }
    public List<HocVien> bang(int lop, LocalDate ngay) throws Exception {
        return service.bang(lop, ngay);
    }
    public int luu(int lop, LocalDate ngay, LuaChon tk, String tenLop,
                   String lyDo, List<HocVien> ds) throws Exception {
        return service.luu(lop, ngay, tk, tenLop, lyDo, ds);
    }
    public List<LichSu> lichSu(int lop, LocalDate tu, LocalDate den) throws Exception {
        return service.lichSu(lop, tu, den);
    }
    public List<NhatKy> nhatKy(int lop, LocalDate tu, LocalDate den) throws Exception {
        return service.nhatKy(lop, tu, den);
    }
}
