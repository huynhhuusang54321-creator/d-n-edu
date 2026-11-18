
package vn.vinabacus.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LichSuKho {

    private final int maPhieuKho;
    private final int maCoSo;
    private final int maHangHoa;
    private final String maSanPham;
    private final String tenHangHoa;
    private final String donViTinh;
    private final String loaiPhieu;
    private final BigDecimal soLuong;
    private final LocalDateTime ngayLap;
    private final String trangThai;
    private final String ghiChu;

    public LichSuKho(
            int maPhieuKho,
            int maCoSo,
            int maHangHoa,
            String maSanPham,
            String tenHangHoa,
            String donViTinh,
            String loaiPhieu,
            BigDecimal soLuong,
            LocalDateTime ngayLap,
            String trangThai,
            String ghiChu) {

        this.maPhieuKho = maPhieuKho;
        this.maCoSo = maCoSo;
        this.maHangHoa = maHangHoa;
        this.maSanPham = maSanPham;
        this.tenHangHoa = tenHangHoa;
        this.donViTinh = donViTinh;
        this.loaiPhieu = loaiPhieu;
        this.soLuong = soLuong;
        this.ngayLap = ngayLap;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }

    public int getMaPhieuKho() {
        return maPhieuKho;
    }

    public int getMaCoSo() {
        return maCoSo;
    }

    public int getMaHangHoa() {
        return maHangHoa;
    }

    public String getMaSanPham() {
        return maSanPham;
    }

    public String getTenHangHoa() {
        return tenHangHoa;
    }

    public String getDonViTinh() {
        return donViTinh;
    }

    public String getLoaiPhieu() {
        return loaiPhieu;
    }

    public BigDecimal getSoLuong() {
        return soLuong;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }
}
