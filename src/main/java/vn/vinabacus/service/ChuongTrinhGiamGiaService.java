package vn.vinabacus.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.ChuongTrinhGiamGiaRepository;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.MonHocRepository;
import vn.vinabacus.repository.TV2CoSoRepository;

public final class ChuongTrinhGiamGiaService {
    private final TV2CoSoRepository coSo = new TV2CoSoRepository();
    private final ChuongTrinhGiamGiaRepository repo = new ChuongTrinhGiamGiaRepository();
    private int maCoSo() throws SQLException { return coSo.layMaCoSo(CoSo.TRUNG_TAM); }

    public boolean sanSang() throws SQLException { return repo.sanSang(); }
    public List<ChuongTrinhGiamGia> danhSach() throws SQLException {
        return repo.layDanhSach(maCoSo());
    }
    public List<MonHoc> monHoc() throws SQLException {
        return new MonHocRepository().layDanhSach(maCoSo()).stream()
                .filter(MonHoc::trangThai).toList();
    }
    public List<KhoaHoc> khoaHoc() throws SQLException {
        return new KhoaHocRepository().layDanhSach(maCoSo()).stream()
                .filter(KhoaHoc::trangThai).toList();
    }
    public int luu(ChuongTrinhGiamGia ct) throws SQLException {
        GiamGiaService.kiemTraCauHinh(ct);
        if (ct.maCoSo() != maCoSo())
            throw new IllegalArgumentException("Chương trình không thuộc Trung tâm");
        if (ct.moTa() != null && ct.moTa().length() > 500)
            throw new IllegalArgumentException("Mô tả tối đa 500 ký tự");
        return repo.luu(ct);
    }
    public int luu(int id, String ten, String moTa, String loai,
            java.math.BigDecimal giaTri, LocalDate tu, LocalDate den,
            boolean tatCa, boolean ketHop, boolean kichHoat,
            java.util.Set<Integer> mon, java.util.Set<Integer> khoa) throws SQLException {
        return luu(new ChuongTrinhGiamGia(id, maCoSo(), ten, moTa, loai,
                giaTri, tu, den, tatCa, ketHop, kichHoat, mon, khoa));
    }
    public void doiTrangThai(int id, boolean kichHoat) throws SQLException {
        repo.doiTrangThai(maCoSo(), id, kichHoat);
    }
}
