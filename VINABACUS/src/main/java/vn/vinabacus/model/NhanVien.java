
package vn.vinabacus.model;

import java.time.LocalDate;

public class NhanVien {

    private final int maNhanVien;
    private final String hoTen;
    private final LocalDate ngaySinh;
    private final String gioiTinh;
    private final String soDienThoai;
    private final String email;
    private final boolean trangThai;
    private final boolean trangThaiTaiCoSo;

    public NhanVien(int maNhanVien, String hoTen,
                    LocalDate ngaySinh, String gioiTinh,
                    String soDienThoai, String email,
                    boolean trangThai,
                    boolean trangThaiTaiCoSo) {

        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.trangThai = trangThai;
        this.trangThaiTaiCoSo = trangThaiTaiCoSo;
    }

    public int getMaNhanVien() {
        return maNhanVien;
    }

    public String getHoTen() {
        return hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public String getEmail() {
        return email;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public boolean isTrangThaiTaiCoSo() {
        return trangThaiTaiCoSo;
    }
}
