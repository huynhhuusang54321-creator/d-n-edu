package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.LopHoc. */
public class LopHocModel {
    private int maLop;
    private int maCoSo;
    private String maLopHienThi;
    private String tenLop;
    private Integer maKhoaHoc;
    private Integer maNamHoc;
    private Integer maGiaoVien;
    private Integer siSoToiDa;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private BigDecimal hocPhiMacDinh = BigDecimal.ZERO;
    private BigDecimal tienAnMacDinh = BigDecimal.ZERO;
    private String trangThai = "DANG_MO";
    private String ghiChu;

    public LopHocModel() { }

    public LopHocModel(int maLop, int maCoSo, String maLopHienThi, String tenLop, Integer maKhoaHoc, Integer maNamHoc, Integer maGiaoVien, Integer siSoToiDa, LocalDate ngayBatDau, LocalDate ngayKetThuc, BigDecimal hocPhiMacDinh, BigDecimal tienAnMacDinh, String trangThai, String ghiChu) {
        this.maLop = maLop;
        this.maCoSo = maCoSo;
        this.maLopHienThi = maLopHienThi;
        this.tenLop = tenLop;
        this.maKhoaHoc = maKhoaHoc;
        this.maNamHoc = maNamHoc;
        this.maGiaoVien = maGiaoVien;
        this.siSoToiDa = siSoToiDa;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.hocPhiMacDinh = hocPhiMacDinh;
        this.tienAnMacDinh = tienAnMacDinh;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }

    // Getters và setters
    public int getMaLop() { return maLop; }
    public void setMaLop(int maLop) { this.maLop = maLop; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getMaLopHienThi() { return maLopHienThi; }
    public void setMaLopHienThi(String maLopHienThi) { this.maLopHienThi = maLopHienThi; }
    public String getTenLop() { return tenLop; }
    public void setTenLop(String tenLop) { this.tenLop = tenLop; }
    public Integer getMaKhoaHoc() { return maKhoaHoc; }
    public void setMaKhoaHoc(Integer maKhoaHoc) { this.maKhoaHoc = maKhoaHoc; }
    public Integer getMaNamHoc() { return maNamHoc; }
    public void setMaNamHoc(Integer maNamHoc) { this.maNamHoc = maNamHoc; }
    public Integer getMaGiaoVien() { return maGiaoVien; }
    public void setMaGiaoVien(Integer maGiaoVien) { this.maGiaoVien = maGiaoVien; }
    public Integer getSiSoToiDa() { return siSoToiDa; }
    public void setSiSoToiDa(Integer siSoToiDa) { this.siSoToiDa = siSoToiDa; }
    public LocalDate getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }
    public LocalDate getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDate ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }
    public BigDecimal getHocPhiMacDinh() { return hocPhiMacDinh; }
    public void setHocPhiMacDinh(BigDecimal hocPhiMacDinh) { this.hocPhiMacDinh = hocPhiMacDinh; }
    public BigDecimal getTienAnMacDinh() { return tienAnMacDinh; }
    public void setTienAnMacDinh(BigDecimal tienAnMacDinh) { this.tienAnMacDinh = tienAnMacDinh; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    @Override
    public String toString() {
        return "LopHocModel{" + "maLop=" + maLop + ", " + "maCoSo=" + maCoSo + ", " + "maLopHienThi=" + maLopHienThi + ", " + "tenLop=" + tenLop + ", " + "maKhoaHoc=" + maKhoaHoc + ", " + "maNamHoc=" + maNamHoc + ", " + "maGiaoVien=" + maGiaoVien + ", " + "siSoToiDa=" + siSoToiDa + ", " + "ngayBatDau=" + ngayBatDau + ", " + "ngayKetThuc=" + ngayKetThuc + ", " + "hocPhiMacDinh=" + hocPhiMacDinh + ", " + "tienAnMacDinh=" + tienAnMacDinh + ", " + "trangThai=" + trangThai + ", " + "ghiChu=" + ghiChu + '}';
    }
}
