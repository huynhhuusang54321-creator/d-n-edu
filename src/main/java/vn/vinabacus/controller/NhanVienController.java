package vn.vinabacus.controller;

import java.sql.SQLException;
import java.util.List;
import java.time.LocalDate;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.ChiTietGiaoVienTrungTam;
import vn.vinabacus.model.NhanVien;
import vn.vinabacus.service.NhanVienService;

public class NhanVienController {

    private final NhanVienService service;

    public NhanVienController() {
        service = new NhanVienService();
    }

    public List<NhanVien> layDanhSach(CoSo coSo)
            throws SQLException {

        return service.layDanhSach(coSo);
    }

    public ChiTietGiaoVienTrungTam chiTietPhanCongTrungTam(
            CoSo coSo, int maNhanVien) throws SQLException {
        return service.chiTietPhanCongTrungTam(coSo, maNhanVien);
    }

    public int themNhanVien(
            CoSo coSo,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        return service.themNhanVien(
                coSo,
                hoTen,
                ngaySinh,
                gioiTinh,
                soDienThoai,
                email
        );
    }

    public boolean suaNhanVien(
            CoSo coSo,
            int maNhanVien,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        return service.suaNhanVien(
                coSo,
                maNhanVien,
                hoTen,
                ngaySinh,
                gioiTinh,
                soDienThoai,
                email
        );
    }

    public boolean ngungCongTac(
            CoSo coSo,
            int maNhanVien) throws SQLException {

        return service.ngungCongTac(
                coSo,
                maNhanVien
        );
    }

public boolean lamViecTroLai(
        CoSo coSo,
        int maNhanVien) throws SQLException {

    return service.lamViecTroLai(
        coSo,
        maNhanVien
    );
}

}
