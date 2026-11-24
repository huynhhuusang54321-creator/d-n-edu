package vn.vinabacus.controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.service.ChuongTrinhGiamGiaService;

public final class ChuongTrinhGiamGiaController {
    private final ChuongTrinhGiamGiaService service = new ChuongTrinhGiamGiaService();
    public boolean sanSang() throws SQLException { return service.sanSang(); }
    public List<ChuongTrinhGiamGia> danhSach() throws SQLException { return service.danhSach(); }
    public List<MonHoc> monHoc() throws SQLException { return service.monHoc(); }
    public List<KhoaHoc> khoaHoc() throws SQLException { return service.khoaHoc(); }
    public int luu(int id, String ten, String moTa, String loai,
            BigDecimal giaTri, LocalDate tu, LocalDate den, boolean tatCa,
            boolean ketHop, boolean kichHoat, Set<Integer> mon, Set<Integer> khoa)
            throws SQLException {
        return service.luu(id, ten, moTa, loai, giaTri, tu, den,
                tatCa, ketHop, kichHoat, mon, khoa);
    }
    public void doiTrangThai(int id, boolean kichHoat) throws SQLException {
        service.doiTrangThai(id, kichHoat);
    }
}
