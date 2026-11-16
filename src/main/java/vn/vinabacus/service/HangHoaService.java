
package vn.vinabacus.service;

import java.sql.SQLException;
import java.util.List;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HangHoa;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.HangHoaRepository;
import java.math.BigDecimal;
public class HangHoaService {

    private final HangHoaRepository hangHoaRepository;
    private final CoSoRepository coSoRepository;

    public HangHoaService() {
        hangHoaRepository = new HangHoaRepository();
        coSoRepository = new CoSoRepository();
    }

    public List<HangHoa> layDanhSach(CoSo coSo)
            throws SQLException {

        // Kiem tra co so
        if (coSo == null) {
            throw new IllegalArgumentException(
                "Vui long chon co so!"
            );
        }

        // Lay ma co so tu SQL Server
        int maCoSo = coSoRepository.timMaCoSo(coSo);

        // Lay danh sach hang hoa cua co so
        return hangHoaRepository.timTheoCoSo(maCoSo);
    }
    
public int themHangHoa(
        CoSo coSo,
        String maSanPham,
        String tenHangHoa,
        String donViTinh,
        BigDecimal donGiaSi,
        BigDecimal donGiaXuatBan) throws SQLException {

    // Kiem tra co so
    if (coSo == null) {
        throw new IllegalArgumentException(
            "Vui long chon co so!"
        );
    }

    // Kiem tra ma san pham
    if (maSanPham == null || maSanPham.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Ma san pham khong duoc de trong!"
        );
    }

    maSanPham = maSanPham.trim();

    if (maSanPham.length() > 40) {
        throw new IllegalArgumentException(
            "Ma san pham toi da 40 ky tu!"
        );
    }

    // Kiem tra ten hang hoa
    if (tenHangHoa == null || tenHangHoa.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Ten hang hoa khong duoc de trong!"
        );
    }

    tenHangHoa = tenHangHoa.trim();

    if (tenHangHoa.length() > 200) {
        throw new IllegalArgumentException(
            "Ten hang hoa toi da 200 ky tu!"
        );
    }

    // Kiem tra don vi tinh
    if (donViTinh == null || donViTinh.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Don vi tinh khong duoc de trong!"
        );
    }

    donViTinh = donViTinh.trim();

    if (donViTinh.length() > 40) {
        throw new IllegalArgumentException(
            "Don vi tinh toi da 40 ky tu!"
        );
    }

    // Kiem tra gia si
    if (donGiaSi == null
            || donGiaSi.compareTo(BigDecimal.ZERO) < 0
            || donGiaSi.scale() > 2
            || donGiaSi.precision() - donGiaSi.scale() > 16) {

        throw new IllegalArgumentException(
            "Gia si khong hop le!"
        );
    }

    // Kiem tra gia xuat ban
    if (donGiaXuatBan == null
            || donGiaXuatBan.compareTo(BigDecimal.ZERO) < 0
            || donGiaXuatBan.scale() > 2
            || donGiaXuatBan.precision()
               - donGiaXuatBan.scale() > 16) {

        throw new IllegalArgumentException(
            "Gia xuat ban khong hop le!"
        );
    }

    // Lay ma co so tu SQL Server
    int maCoSo = coSoRepository.timMaCoSo(coSo);

    // Goi Repository de them hang hoa
    return hangHoaRepository.themHangHoa(
        maCoSo,
        maSanPham,
        tenHangHoa,
        donViTinh,
        donGiaSi,
        donGiaXuatBan
    );
}

public boolean suaHangHoa(
        CoSo coSo,
        int maHangHoa,
        String maSanPham,
        String tenHangHoa,
        String donViTinh,
        BigDecimal donGiaSi,
        BigDecimal donGiaXuatBan) throws SQLException {

    // Kiem tra co so
    if (coSo == null) {
        throw new IllegalArgumentException(
            "Vui lòng chọn cơ sở!"
        );
    }

    // Kiem tra ma hang hoa
    if (maHangHoa <= 0) {
        throw new IllegalArgumentException(
            "Mã hàng hóa không hợp lệ!"
        );
    }

    // Kiem tra ma san pham
    if (maSanPham == null
            || maSanPham.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Mã sản phẩm không được để trống!"
        );
    }

    maSanPham = maSanPham.trim();

    if (maSanPham.length() > 40) {
        throw new IllegalArgumentException(
            "Mã sản phẩm tối đa 40 ký tự!"
        );
    }

    // Kiem tra ten hang hoa
    if (tenHangHoa == null
            || tenHangHoa.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Tên hàng hóa không được để trống!"
        );
    }

    tenHangHoa = tenHangHoa.trim();

    if (tenHangHoa.length() > 200) {
        throw new IllegalArgumentException(
            "Tên hàng hóa tối đa 200 ký tự!"
        );
    }

    // Kiem tra don vi tinh
    if (donViTinh == null
            || donViTinh.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Đơn vị tính không được để trống!"
        );
    }

    donViTinh = donViTinh.trim();

    if (donViTinh.length() > 40) {
        throw new IllegalArgumentException(
            "Đơn vị tính tối đa 40 ký tự!"
        );
    }

    // Kiem tra gia si
    if (donGiaSi == null
            || donGiaSi.compareTo(BigDecimal.ZERO) < 0
            || donGiaSi.scale() > 2
            || donGiaSi.precision()
               - donGiaSi.scale() > 16) {

        throw new IllegalArgumentException(
            "Giá sỉ không hợp lệ!"
        );
    }

    // Kiem tra gia xuat ban
    if (donGiaXuatBan == null
            || donGiaXuatBan.compareTo(BigDecimal.ZERO) < 0
            || donGiaXuatBan.scale() > 2
            || donGiaXuatBan.precision()
               - donGiaXuatBan.scale() > 16) {

        throw new IllegalArgumentException(
            "Giá xuất bán không hợp lệ!"
        );
    }

    // Lay ma co so
    int maCoSo = coSoRepository.timMaCoSo(coSo);

    // Cap nhat hang hoa trong SQL Server
    return hangHoaRepository.suaHangHoa(
        maHangHoa,
        maCoSo,
        maSanPham,
        tenHangHoa,
        donViTinh,
        donGiaSi,
        donGiaXuatBan
    );
}

public boolean capNhatTrangThai(
        CoSo coSo,
        int maHangHoa,
        boolean trangThai) throws SQLException {

    // Kiem tra co so
    if (coSo == null) {
        throw new IllegalArgumentException(
            "Vui lòng chọn cơ sở!"
        );
    }

    // Kiem tra ma hang hoa
    if (maHangHoa <= 0) {
        throw new IllegalArgumentException(
            "Mã hàng hóa không hợp lệ!"
        );
    }

    // Lay ma co so tu SQL Server
    int maCoSo = coSoRepository.timMaCoSo(coSo);

    // Goi Repository cap nhat trang thai
    return hangHoaRepository.capNhatTrangThai(
        maHangHoa,
        maCoSo,
        trangThai
    );
}

public int nhapKho(
        CoSo coSo,
        int maHangHoa,
        BigDecimal soLuong,
        int maTaiKhoan,
        String ghiChu) throws SQLException {

    // Kiem tra co so
    if (coSo == null) {
        throw new IllegalArgumentException(
            "Vui lòng chọn cơ sở!"
        );
    }

    // Kiem tra hang hoa
    if (maHangHoa <= 0) {
        throw new IllegalArgumentException(
            "Mã hàng hóa không hợp lệ!"
        );
    }

    // Kiem tra so luong nhap
    if (soLuong == null
            || soLuong.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            "Số lượng nhập phải lớn hơn 0!"
        );
    }

    BigDecimal soLuongHopLe =
            soLuong.stripTrailingZeros();

    if (soLuongHopLe.scale() > 3
            || soLuongHopLe.precision()
               - soLuongHopLe.scale() > 15) {

        throw new IllegalArgumentException(
            "Số lượng nhập không hợp lệ!"
        );
    }

    // Kiem tra tai khoan thuc hien
    if (maTaiKhoan <= 0) {
        throw new IllegalArgumentException(
            "Tài khoản nhập kho không hợp lệ!"
        );
    }

    // Kiem tra ghi chu
    if (ghiChu != null) {
        ghiChu = ghiChu.trim();

        if (ghiChu.length() > 500) {
            throw new IllegalArgumentException(
                "Ghi chú tối đa 500 ký tự!"
            );
        }
    }

    // Lay ma co so tu SQL Server
    int maCoSo = coSoRepository.timMaCoSo(coSo);

    // Goi Repository de tao phieu nhap kho
    return hangHoaRepository.nhapKho(
        maCoSo,
        maHangHoa,
        soLuongHopLe,
        maTaiKhoan,
        ghiChu
    );
}

}
