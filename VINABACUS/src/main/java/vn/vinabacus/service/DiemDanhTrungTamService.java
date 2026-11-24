package vn.vinabacus.service;


import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.DiemDanhTrungTamRepository;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.*;

public final class DiemDanhTrungTamService {
    private final DiemDanhTrungTamRepository repo = new DiemDanhTrungTamRepository();
    private int coSo() throws Exception {
        return new CoSoRepository().timMaCoSo(CoSo.TRUNG_TAM);
    }
    public List<LuaChon> lop() throws Exception { return repo.lop(coSo()); }
    public List<LuaChon> taiKhoan() throws Exception { return repo.taiKhoan(coSo()); }
    public List<HocVien> bang(int lop, LocalDate ngay) throws Exception {
        kiemTra(lop, ngay);
        return repo.bang(coSo(), lop, ngay);
    }
    public int luu(int lop, LocalDate ngay, LuaChon tk, String tenLop,
                   String lyDo, List<HocVien> ds) throws Exception {
        kiemTra(lop, ngay);
        if (tk == null || ds == null) throw new IllegalArgumentException("Thiếu tài khoản hoặc học viên.");
        return repo.luu(coSo(), lop, ngay, tk, tenLop, lyDo, ds);
    }
    public List<LichSu> lichSu(int lop, LocalDate tu, LocalDate den) throws Exception {
        khoang(lop, tu, den);
        return repo.lichSu(coSo(), lop, tu, den);
    }
    public List<NhatKy> nhatKy(int lop, LocalDate tu, LocalDate den) throws Exception {
        khoang(lop, tu, den);
        return repo.nhatKy(coSo(), lop, tu, den);
    }
    private static void kiemTra(int lop, LocalDate ngay) {
        if (lop <= 0 || ngay == null) throw new IllegalArgumentException("Phải chọn lớp và ngày.");
    }
    private static void khoang(int lop, LocalDate tu, LocalDate den) {
        kiemTra(lop, tu);
        if (den == null || den.isBefore(tu)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
    }
}
