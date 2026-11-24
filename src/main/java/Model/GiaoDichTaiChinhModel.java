package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.GiaoDichTaiChinh. */
public class GiaoDichTaiChinhModel {
    private int maGiaoDich;
    private int maCoSo;
    private Integer maHocVien;
    private Integer maDangKy;
    private Integer maGiaoDichNguon;
    private String maNhomChungTu;
    private String loaiGiaoDich;
    private int maLoaiPhi;
    private Integer maHinhThuc;
    private LocalDate kyPhi;
    private BigDecimal soLuong;
    private BigDecimal donGia;
    private BigDecimal soTien;
    private LocalDateTime ngayGiaoDich = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    private LocalDate hanThanhToan;
    private int maNhanSu;
    private String trangThai = "XAC_NHAN";
    private String dienGiai;

    public GiaoDichTaiChinhModel() { }

    public GiaoDichTaiChinhModel(int maGiaoDich, int maCoSo, Integer maHocVien, Integer maDangKy, Integer maGiaoDichNguon, String maNhomChungTu, String loaiGiaoDich, int maLoaiPhi, Integer maHinhThuc, LocalDate kyPhi, BigDecimal soLuong, BigDecimal donGia, BigDecimal soTien, LocalDateTime ngayGiaoDich, LocalDate hanThanhToan, int maNhanSu, String trangThai, String dienGiai) {
        this.maGiaoDich = maGiaoDich;
        this.maCoSo = maCoSo;
        this.maHocVien = maHocVien;
        this.maDangKy = maDangKy;
        this.maGiaoDichNguon = maGiaoDichNguon;
        this.maNhomChungTu = maNhomChungTu;
        this.loaiGiaoDich = loaiGiaoDich;
        this.maLoaiPhi = maLoaiPhi;
        this.maHinhThuc = maHinhThuc;
        this.kyPhi = kyPhi;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.soTien = soTien;
        this.ngayGiaoDich = ngayGiaoDich;
        this.hanThanhToan = hanThanhToan;
        this.maNhanSu = maNhanSu;
        this.trangThai = trangThai;
        this.dienGiai = dienGiai;
    }

    // Getters và setters
    public int getMaGiaoDich() { return maGiaoDich; }
    public void setMaGiaoDich(int maGiaoDich) { this.maGiaoDich = maGiaoDich; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public Integer getMaHocVien() { return maHocVien; }
    public void setMaHocVien(Integer maHocVien) { this.maHocVien = maHocVien; }
    public Integer getMaDangKy() { return maDangKy; }
    public void setMaDangKy(Integer maDangKy) { this.maDangKy = maDangKy; }
    public Integer getMaGiaoDichNguon() { return maGiaoDichNguon; }
    public void setMaGiaoDichNguon(Integer maGiaoDichNguon) { this.maGiaoDichNguon = maGiaoDichNguon; }
    public String getMaNhomChungTu() { return maNhomChungTu; }
    public void setMaNhomChungTu(String maNhomChungTu) { this.maNhomChungTu = maNhomChungTu; }
    public String getLoaiGiaoDich() { return loaiGiaoDich; }
    public void setLoaiGiaoDich(String loaiGiaoDich) { this.loaiGiaoDich = loaiGiaoDich; }
    public int getMaLoaiPhi() { return maLoaiPhi; }
    public void setMaLoaiPhi(int maLoaiPhi) { this.maLoaiPhi = maLoaiPhi; }
    public Integer getMaHinhThuc() { return maHinhThuc; }
    public void setMaHinhThuc(Integer maHinhThuc) { this.maHinhThuc = maHinhThuc; }
    public LocalDate getKyPhi() { return kyPhi; }
    public void setKyPhi(LocalDate kyPhi) { this.kyPhi = kyPhi; }
    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }
    public LocalDateTime getNgayGiaoDich() { return ngayGiaoDich; }
    public void setNgayGiaoDich(LocalDateTime ngayGiaoDich) { this.ngayGiaoDich = ngayGiaoDich; }
    public LocalDate getHanThanhToan() { return hanThanhToan; }
    public void setHanThanhToan(LocalDate hanThanhToan) { this.hanThanhToan = hanThanhToan; }
    public int getMaNhanSu() { return maNhanSu; }
    public void setMaNhanSu(int maNhanSu) { this.maNhanSu = maNhanSu; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getDienGiai() { return dienGiai; }
    public void setDienGiai(String dienGiai) { this.dienGiai = dienGiai; }

    @Override
    public String toString() {
        return "GiaoDichTaiChinhModel{" + "maGiaoDich=" + maGiaoDich + ", " + "maCoSo=" + maCoSo + ", " + "maHocVien=" + maHocVien + ", " + "maDangKy=" + maDangKy + ", " + "maGiaoDichNguon=" + maGiaoDichNguon + ", " + "maNhomChungTu=" + maNhomChungTu + ", " + "loaiGiaoDich=" + loaiGiaoDich + ", " + "maLoaiPhi=" + maLoaiPhi + ", " + "maHinhThuc=" + maHinhThuc + ", " + "kyPhi=" + kyPhi + ", " + "soLuong=" + soLuong + ", " + "donGia=" + donGia + ", " + "soTien=" + soTien + ", " + "ngayGiaoDich=" + ngayGiaoDich + ", " + "hanThanhToan=" + hanThanhToan + ", " + "maNhanSu=" + maNhanSu + ", " + "trangThai=" + trangThai + ", " + "dienGiai=" + dienGiai + '}';
    }
}
