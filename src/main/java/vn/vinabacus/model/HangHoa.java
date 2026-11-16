
package vn.vinabacus.model;

import java.math.BigDecimal;

public class HangHoa {

    private final int maHangHoa;
    private final int maCoSo;
    private final String maSanPham;
    private final String tenHangHoa;
    private final String donViTinh;
    private final BigDecimal donGiaSi;
    private final BigDecimal donGiaXuatBan;
    private final boolean trangThai;
    private final BigDecimal soLuongTon;

    // Constructor cu de tuong thich code hien tai
    public HangHoa(
            int maHangHoa,
            int maCoSo,
            String maSanPham,
            String tenHangHoa,
            String donViTinh,
            BigDecimal donGiaSi,
            BigDecimal donGiaXuatBan,
            boolean trangThai) {

        this(
            maHangHoa,
            maCoSo,
            maSanPham,
            tenHangHoa,
            donViTinh,
            donGiaSi,
            donGiaXuatBan,
            trangThai,
            BigDecimal.ZERO
        );
    }

    // Constructor moi co so luong ton
    public HangHoa(
            int maHangHoa,
            int maCoSo,
            String maSanPham,
            String tenHangHoa,
            String donViTinh,
            BigDecimal donGiaSi,
            BigDecimal donGiaXuatBan,
            boolean trangThai,
            BigDecimal soLuongTon) {

        this.maHangHoa = maHangHoa;
        this.maCoSo = maCoSo;
        this.maSanPham = maSanPham;
        this.tenHangHoa = tenHangHoa;
        this.donViTinh = donViTinh;
        this.donGiaSi = donGiaSi;
        this.donGiaXuatBan = donGiaXuatBan;
        this.trangThai = trangThai;
        this.soLuongTon = soLuongTon;
    }

    public int getMaHangHoa() {
        return maHangHoa;
    }

    public int getMaCoSo() {
        return maCoSo;
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

    public BigDecimal getDonGiaSi() {
        return donGiaSi;
    }

    public BigDecimal getDonGiaXuatBan() {
        return donGiaXuatBan;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public BigDecimal getSoLuongTon() {
        return soLuongTon;
    }
}
