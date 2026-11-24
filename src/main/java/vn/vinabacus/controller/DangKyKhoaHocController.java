package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.TaiChinhDTO;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.service.DangKyKhoaHocService;

/** API giao diện đăng ký và xếp lớp tại Trung tâm. */
public final class DangKyKhoaHocController {
    private final DangKyKhoaHocService service = new DangKyKhoaHocService();

    public List<DangKyKhoaHoc> timKiem(String tuKhoa, String trangThai)
            throws SQLException { return service.timKiem(tuKhoa, trangThai); }
    public List<KhoaHoc> khoaHoc() throws SQLException { return service.khoaHoc(); }
    public boolean giamGiaSanSang() throws SQLException { return service.giamGiaSanSang(); }
    public List<ChuongTrinhGiamGia> chuongTrinhGiamGia() throws SQLException {
        return service.chuongTrinhGiamGia();
    }
    public List<TaiChinhDTO.LuaChon> taiKhoanThu() throws SQLException {
        return service.taiKhoanThu();
    }
    public List<LopTrungTam> lopHoc() throws SQLException { return service.lopHoc(); }
    public List<DangKyKhoaHoc.ThongTinLop> thongTinLop() throws SQLException {
        return service.thongTinLop();
    }
    public List<HocVien> hocVien() throws SQLException { return service.hocVien(); }
    public List<KhachTiemNang> hoSoTuyenSinh() throws SQLException {
        return service.hoSoTuyenSinh();
    }
    public DangKyKhoaHoc.KetQua dangKyTuHoSo(int maKhach, LocalDate ngayBatDau,
                                               List<DangKyKhoaHoc.LuaChon> luaChon)
            throws SQLException {
        return service.dangKyTuHoSo(maKhach, ngayBatDau, luaChon);
    }
    public DangKyKhoaHoc.KetQua dangKyTuHoSo(int maKhach, LocalDate ngayBatDau,
            List<DangKyKhoaHoc.LuaChon> luaChon, DangKyKhoaHoc.TaiChinh taiChinh)
            throws SQLException {
        return service.dangKyTuHoSo(maKhach, ngayBatDau, luaChon, taiChinh);
    }
    public DangKyKhoaHoc.KetQua dangKyChoHocVien(int maHocVien,
            LocalDate ngayBatDau, List<DangKyKhoaHoc.LuaChon> luaChon)
            throws SQLException {
        return service.dangKyChoHocVien(maHocVien, ngayBatDau, luaChon);
    }
    public DangKyKhoaHoc.KetQua dangKyChoHocVien(int maHocVien,
            LocalDate ngayBatDau, List<DangKyKhoaHoc.LuaChon> luaChon,
            DangKyKhoaHoc.TaiChinh taiChinh) throws SQLException {
        return service.dangKyChoHocVien(maHocVien, ngayBatDau, luaChon, taiChinh);
    }
    public int chuyenThanhHocVien(int maKhach) throws SQLException {
        return service.chuyenThanhHocVien(maKhach);
    }
    public void xepLop(int maDangKy, int maLop, LocalDate ngayXep)
            throws SQLException { service.xepLop(maDangKy, maLop, ngayXep); }
}
