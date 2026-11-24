package vn.vinabacus.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.repository.DiemDanhTrungTamRepository;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.Buoi;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.HocVienBuoi;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.LichSuSua;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.TaiKhoan;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.ThayDoi;
import vn.vinabacus.repository.HocBuTrungTamRepository;
import vn.vinabacus.repository.HocBuTrungTamRepository.BuoiDich;
import vn.vinabacus.repository.HocBuTrungTamRepository.BuoiVang;
import vn.vinabacus.repository.HocBuTrungTamRepository.LichHocBu;
import vn.vinabacus.repository.MonHocRepository;

public final class DiemDanhTrungTamService {
    private final DiemDanhTrungTamRepository diemDanh;
    private final HocBuTrungTamRepository hocBu;
    private final MonHocRepository monHoc = new MonHocRepository();
    public DiemDanhTrungTamService() {
        this(new DiemDanhTrungTamRepository(),new HocBuTrungTamRepository());
    }
    public DiemDanhTrungTamService(DiemDanhTrungTamRepository diemDanh,
                                    HocBuTrungTamRepository hocBu) {
        this.diemDanh=Objects.requireNonNull(diemDanh);
        this.hocBu=Objects.requireNonNull(hocBu);
    }
    private int coSo() throws SQLException { return monHoc.layMaCoSo(CoSo.TRUNG_TAM); }
    public List<TaiKhoan> taiKhoan() throws SQLException { return diemDanh.layTaiKhoan(coSo()); }
    public List<Buoi> buoi(LocalDate ngay) throws SQLException {
        return diemDanh.layBuoi(coSo(),Objects.requireNonNull(ngay));
    }
    public int lichChuaCoBuoi(LocalDate ngay) throws SQLException {
        return diemDanh.demLichChuaCoBuoi(coSo(),Objects.requireNonNull(ngay));
    }
    public List<HocVienBuoi> hocVien(int maBuoi) throws SQLException {
        return diemDanh.layHocVien(coSo(),maBuoi);
    }
    public List<LichSuSua> lichSuSua(int maSapXep) throws SQLException {
        return diemDanh.layLichSuSua(coSo(),maSapXep);
    }
    public void luu(int maBuoi,HocVienBuoi hocVien,String trangThai,
                    String ghiChu,int taiKhoan,String lyDoSua) throws SQLException {
        if (ghiChu!=null && ghiChu.length()>500)
            throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự");
        if (lyDoSua!=null && lyDoSua.length()>500)
            throw new IllegalArgumentException("Lý do sửa tối đa 500 ký tự");
        diemDanh.luuDiemDanh(coSo(),maBuoi,hocVien.maDangKy(),
                hocVien.maHocVien(),trangThai,ghiChu,taiKhoan,lyDoSua);
    }
    public void luuNhieu(int maBuoi, List<ThayDoi> thayDoi, int taiKhoan)
            throws SQLException {
        if (thayDoi == null || thayDoi.isEmpty()) return;
        java.util.Set<Integer> dangKy = new java.util.HashSet<>();
        for (ThayDoi d : thayDoi) {
            if (d.hocVien() == null || !dangKy.add(d.hocVien().maDangKy()))
                throw new IllegalArgumentException("Danh sách điểm danh có học viên trùng");
            if (d.ghiChu() != null && d.ghiChu().length() > 500)
                throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự");
            if (d.lyDoSua() != null && d.lyDoSua().length() > 500)
                throw new IllegalArgumentException("Lý do sửa tối đa 500 ký tự");
        }
        diemDanh.luuNhieu(coSo(), maBuoi, thayDoi, taiKhoan);
    }
    public void noiDung(int maBuoi,String noiDung) throws SQLException {
        diemDanh.luuNoiDung(coSo(),maBuoi,noiDung);
    }
    public List<BuoiVang> buoiVang() throws SQLException { return hocBu.layBuoiVang(coSo()); }
    public List<BuoiDich> buoiDich(int maNguon) throws SQLException {
        return hocBu.layBuoiDich(coSo(),maNguon);
    }
    public List<LichHocBu> lichHocBu() throws SQLException {
        return hocBu.layLichHocBu(coSo());
    }
    public int xepHocBu(int nguon,int dich,int taiKhoan) throws SQLException {
        return hocBu.xepHocBu(coSo(),nguon,dich,taiKhoan);
    }
    public void huyHocBu(int maSapXep,int taiKhoan,String lyDo) throws SQLException {
        hocBu.huyHocBu(coSo(),maSapXep,taiKhoan,lyDo);
    }
}
