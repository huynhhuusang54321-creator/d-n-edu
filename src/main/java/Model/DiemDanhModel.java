package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.DiemDanh. */
public class DiemDanhModel {
    private int maDiemDanh;
    private int maCoSo;
    private int maHocVien;
    private int maBuoiHoc;
    private Integer maDangKy;
    private String loaiThamGia;
    private String trangThai = "CHUA_DIEM_DANH";
    private boolean tinhBuoi = false;
    private Integer maDiemDanhNguon;
    private int maNguoiCapNhat;
    private LocalDateTime ngayCapNhat = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    private String ghiChu;

    public DiemDanhModel() { }

    public DiemDanhModel(int maDiemDanh, int maCoSo, int maHocVien, int maBuoiHoc, Integer maDangKy, String loaiThamGia, String trangThai, boolean tinhBuoi, Integer maDiemDanhNguon, int maNguoiCapNhat, LocalDateTime ngayCapNhat, String ghiChu) {
        this.maDiemDanh = maDiemDanh;
        this.maCoSo = maCoSo;
        this.maHocVien = maHocVien;
        this.maBuoiHoc = maBuoiHoc;
        this.maDangKy = maDangKy;
        this.loaiThamGia = loaiThamGia;
        this.trangThai = trangThai;
        this.tinhBuoi = tinhBuoi;
        this.maDiemDanhNguon = maDiemDanhNguon;
        this.maNguoiCapNhat = maNguoiCapNhat;
        this.ngayCapNhat = ngayCapNhat;
        this.ghiChu = ghiChu;
    }

    // Getters và setters
    public int getMaDiemDanh() { return maDiemDanh; }
    public void setMaDiemDanh(int maDiemDanh) { this.maDiemDanh = maDiemDanh; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public int getMaHocVien() { return maHocVien; }
    public void setMaHocVien(int maHocVien) { this.maHocVien = maHocVien; }
    public int getMaBuoiHoc() { return maBuoiHoc; }
    public void setMaBuoiHoc(int maBuoiHoc) { this.maBuoiHoc = maBuoiHoc; }
    public Integer getMaDangKy() { return maDangKy; }
    public void setMaDangKy(Integer maDangKy) { this.maDangKy = maDangKy; }
    public String getLoaiThamGia() { return loaiThamGia; }
    public void setLoaiThamGia(String loaiThamGia) { this.loaiThamGia = loaiThamGia; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public boolean getTinhBuoi() { return tinhBuoi; }
    public void setTinhBuoi(boolean tinhBuoi) { this.tinhBuoi = tinhBuoi; }
    public boolean isTinhBuoi() { return tinhBuoi; }
    public Integer getMaDiemDanhNguon() { return maDiemDanhNguon; }
    public void setMaDiemDanhNguon(Integer maDiemDanhNguon) { this.maDiemDanhNguon = maDiemDanhNguon; }
    public int getMaNguoiCapNhat() { return maNguoiCapNhat; }
    public void setMaNguoiCapNhat(int maNguoiCapNhat) { this.maNguoiCapNhat = maNguoiCapNhat; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    @Override
    public String toString() {
        return "DiemDanhModel{" + "maDiemDanh=" + maDiemDanh + ", " + "maCoSo=" + maCoSo + ", " + "maHocVien=" + maHocVien + ", " + "maBuoiHoc=" + maBuoiHoc + ", " + "maDangKy=" + maDangKy + ", " + "loaiThamGia=" + loaiThamGia + ", " + "trangThai=" + trangThai + ", " + "tinhBuoi=" + tinhBuoi + ", " + "maDiemDanhNguon=" + maDiemDanhNguon + ", " + "maNguoiCapNhat=" + maNguoiCapNhat + ", " + "ngayCapNhat=" + ngayCapNhat + ", " + "ghiChu=" + ghiChu + '}';
    }
}
