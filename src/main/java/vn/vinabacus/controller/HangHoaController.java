
package vn.vinabacus.controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HangHoa;
import vn.vinabacus.service.HangHoaService;

public class HangHoaController {

    private final HangHoaService service;

    public HangHoaController() {
        service = new HangHoaService();
    }

    // Lay danh sach hang hoa theo co so
    public List<HangHoa> layDanhSach(CoSo coSo)
            throws SQLException {

        return service.layDanhSach(coSo);
    }

    // Them hang hoa moi
    public int themHangHoa(
            CoSo coSo,
            String maSanPham,
            String tenHangHoa,
            String donViTinh,
            BigDecimal donGiaSi,
            BigDecimal donGiaXuatBan)
            throws SQLException {

        return service.themHangHoa(
            coSo,
            maSanPham,
            tenHangHoa,
            donViTinh,
            donGiaSi,
            donGiaXuatBan
        );
    }

    // Sua thong tin hang hoa
    public boolean suaHangHoa(
            CoSo coSo,
            int maHangHoa,
            String maSanPham,
            String tenHangHoa,
            String donViTinh,
            BigDecimal donGiaSi,
            BigDecimal donGiaXuatBan)
            throws SQLException {

        return service.suaHangHoa(
            coSo,
            maHangHoa,
            maSanPham,
            tenHangHoa,
            donViTinh,
            donGiaSi,
            donGiaXuatBan
        );
    }

    // Ngung su dung / Su dung tro lai
    public boolean capNhatTrangThai(
            CoSo coSo,
            int maHangHoa,
            boolean trangThai)
            throws SQLException {

        return service.capNhatTrangThai(
            coSo,
            maHangHoa,
            trangThai
        );
    }
    
public int nhapKho(
        CoSo coSo,
        int maHangHoa,
        BigDecimal soLuong,
        int maTaiKhoan,
        String ghiChu) throws SQLException {

    return service.nhapKho(
        coSo,
        maHangHoa,
        soLuong,
        maTaiKhoan,
        ghiChu
    );
}

}
