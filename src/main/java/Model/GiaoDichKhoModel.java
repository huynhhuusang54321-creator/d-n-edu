package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.GiaoDichKho. */
public class GiaoDichKhoModel {
    private int maGiaoDichKho;
    private int maCoSo;
    private String maNhomChungTu;
    private int maHangHoa;
    private Integer maHocVien;
    private Integer maGiaoDichTaiChinh;
    private String loaiGiaoDich;
    private BigDecimal soLuong;
    private BigDecimal tonThucTe;
    private BigDecimal donGia = BigDecimal.ZERO;
    private LocalDateTime ngayGiaoDich = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    private int maNhanSu;
    private String trangThai = "XAC_NHAN";
    private String ghiChu;

    public GiaoDichKhoModel() { }

    public GiaoDichKhoModel(int maGiaoDichKho, int maCoSo, String maNhomChungTu, int maHangHoa, Integer maHocVien, Integer maGiaoDichTaiChinh, String loaiGiaoDich, BigDecimal soLuong, BigDecimal tonThucTe, BigDecimal donGia, LocalDateTime ngayGiaoDich, int maNhanSu, String trangThai, String ghiChu) {
        this.maGiaoDichKho = maGiaoDichKho;
        this.maCoSo = maCoSo;
        this.maNhomChungTu = maNhomChungTu;
        this.maHangHoa = maHangHoa;
        this.maHocVien = maHocVien;
        this.maGiaoDichTaiChinh = maGiaoDichTaiChinh;
        this.loaiGiaoDich = loaiGiaoDich;
        this.soLuong = soLuong;
        this.tonThucTe = tonThucTe;
        this.donGia = donGia;
        this.ngayGiaoDich = ngayGiaoDich;
        this.maNhanSu = maNhanSu;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }

    // Getters và setters
    public int getMaGiaoDichKho() { return maGiaoDichKho; }
    public void setMaGiaoDichKho(int maGiaoDichKho) { this.maGiaoDichKho = maGiaoDichKho; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getMaNhomChungTu() { return maNhomChungTu; }
    public void setMaNhomChungTu(String maNhomChungTu) { this.maNhomChungTu = maNhomChungTu; }
    public int getMaHangHoa() { return maHangHoa; }
    public void setMaHangHoa(int maHangHoa) { this.maHangHoa = maHangHoa; }
    public Integer getMaHocVien() { return maHocVien; }
    public void setMaHocVien(Integer maHocVien) { this.maHocVien = maHocVien; }
    public Integer getMaGiaoDichTaiChinh() { return maGiaoDichTaiChinh; }
    public void setMaGiaoDichTaiChinh(Integer maGiaoDichTaiChinh) { this.maGiaoDichTaiChinh = maGiaoDichTaiChinh; }
    public String getLoaiGiaoDich() { return loaiGiaoDich; }
    public void setLoaiGiaoDich(String loaiGiaoDich) { this.loaiGiaoDich = loaiGiaoDich; }
    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    public BigDecimal getTonThucTe() { return tonThucTe; }
    public void setTonThucTe(BigDecimal tonThucTe) { this.tonThucTe = tonThucTe; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public LocalDateTime getNgayGiaoDich() { return ngayGiaoDich; }
    public void setNgayGiaoDich(LocalDateTime ngayGiaoDich) { this.ngayGiaoDich = ngayGiaoDich; }
    public int getMaNhanSu() { return maNhanSu; }
    public void setMaNhanSu(int maNhanSu) { this.maNhanSu = maNhanSu; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    @Override
    public String toString() {
        return "GiaoDichKhoModel{" + "maGiaoDichKho=" + maGiaoDichKho + ", " + "maCoSo=" + maCoSo + ", " + "maNhomChungTu=" + maNhomChungTu + ", " + "maHangHoa=" + maHangHoa + ", " + "maHocVien=" + maHocVien + ", " + "maGiaoDichTaiChinh=" + maGiaoDichTaiChinh + ", " + "loaiGiaoDich=" + loaiGiaoDich + ", " + "soLuong=" + soLuong + ", " + "tonThucTe=" + tonThucTe + ", " + "donGia=" + donGia + ", " + "ngayGiaoDich=" + ngayGiaoDich + ", " + "maNhanSu=" + maNhanSu + ", " + "trangThai=" + trangThai + ", " + "ghiChu=" + ghiChu + '}';
    }
}
