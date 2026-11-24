package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.DangKyHoc. */
public class DangKyHocModel {
    private int maDangKy;
    private int maCoSo;
    private int maHocVien;
    private Integer maKhoaHoc;
    private Integer maLop;
    private Integer maDangKyGoc;
    private Integer maDangKyTruoc;
    private LocalDate ngayDangKy;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThucDuKien;
    private LocalDate ngayKetThucThucTe;
    private Integer tongSoBuoi;
    private BigDecimal hocPhiThoaThuan;
    private BigDecimal soTienGiam = BigDecimal.ZERO;
    private int maNguoiDangKy;
    private String trangThai = "CHO_XEP_LOP";
    private String ghiChu;

    public DangKyHocModel() { }

    public DangKyHocModel(int maDangKy, int maCoSo, int maHocVien, Integer maKhoaHoc, Integer maLop, Integer maDangKyGoc, Integer maDangKyTruoc, LocalDate ngayDangKy, LocalDate ngayBatDau, LocalDate ngayKetThucDuKien, LocalDate ngayKetThucThucTe, Integer tongSoBuoi, BigDecimal hocPhiThoaThuan, BigDecimal soTienGiam, int maNguoiDangKy, String trangThai, String ghiChu) {
        this.maDangKy = maDangKy;
        this.maCoSo = maCoSo;
        this.maHocVien = maHocVien;
        this.maKhoaHoc = maKhoaHoc;
        this.maLop = maLop;
        this.maDangKyGoc = maDangKyGoc;
        this.maDangKyTruoc = maDangKyTruoc;
        this.ngayDangKy = ngayDangKy;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThucDuKien = ngayKetThucDuKien;
        this.ngayKetThucThucTe = ngayKetThucThucTe;
        this.tongSoBuoi = tongSoBuoi;
        this.hocPhiThoaThuan = hocPhiThoaThuan;
        this.soTienGiam = soTienGiam;
        this.maNguoiDangKy = maNguoiDangKy;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }

    // Getters và setters
    public int getMaDangKy() { return maDangKy; }
    public void setMaDangKy(int maDangKy) { this.maDangKy = maDangKy; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public int getMaHocVien() { return maHocVien; }
    public void setMaHocVien(int maHocVien) { this.maHocVien = maHocVien; }
    public Integer getMaKhoaHoc() { return maKhoaHoc; }
    public void setMaKhoaHoc(Integer maKhoaHoc) { this.maKhoaHoc = maKhoaHoc; }
    public Integer getMaLop() { return maLop; }
    public void setMaLop(Integer maLop) { this.maLop = maLop; }
    public Integer getMaDangKyGoc() { return maDangKyGoc; }
    public void setMaDangKyGoc(Integer maDangKyGoc) { this.maDangKyGoc = maDangKyGoc; }
    public Integer getMaDangKyTruoc() { return maDangKyTruoc; }
    public void setMaDangKyTruoc(Integer maDangKyTruoc) { this.maDangKyTruoc = maDangKyTruoc; }
    public LocalDate getNgayDangKy() { return ngayDangKy; }
    public void setNgayDangKy(LocalDate ngayDangKy) { this.ngayDangKy = ngayDangKy; }
    public LocalDate getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }
    public LocalDate getNgayKetThucDuKien() { return ngayKetThucDuKien; }
    public void setNgayKetThucDuKien(LocalDate ngayKetThucDuKien) { this.ngayKetThucDuKien = ngayKetThucDuKien; }
    public LocalDate getNgayKetThucThucTe() { return ngayKetThucThucTe; }
    public void setNgayKetThucThucTe(LocalDate ngayKetThucThucTe) { this.ngayKetThucThucTe = ngayKetThucThucTe; }
    public Integer getTongSoBuoi() { return tongSoBuoi; }
    public void setTongSoBuoi(Integer tongSoBuoi) { this.tongSoBuoi = tongSoBuoi; }
    public BigDecimal getHocPhiThoaThuan() { return hocPhiThoaThuan; }
    public void setHocPhiThoaThuan(BigDecimal hocPhiThoaThuan) { this.hocPhiThoaThuan = hocPhiThoaThuan; }
    public BigDecimal getSoTienGiam() { return soTienGiam; }
    public void setSoTienGiam(BigDecimal soTienGiam) { this.soTienGiam = soTienGiam; }
    public int getMaNguoiDangKy() { return maNguoiDangKy; }
    public void setMaNguoiDangKy(int maNguoiDangKy) { this.maNguoiDangKy = maNguoiDangKy; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    @Override
    public String toString() {
        return "DangKyHocModel{" + "maDangKy=" + maDangKy + ", " + "maCoSo=" + maCoSo + ", " + "maHocVien=" + maHocVien + ", " + "maKhoaHoc=" + maKhoaHoc + ", " + "maLop=" + maLop + ", " + "maDangKyGoc=" + maDangKyGoc + ", " + "maDangKyTruoc=" + maDangKyTruoc + ", " + "ngayDangKy=" + ngayDangKy + ", " + "ngayBatDau=" + ngayBatDau + ", " + "ngayKetThucDuKien=" + ngayKetThucDuKien + ", " + "ngayKetThucThucTe=" + ngayKetThucThucTe + ", " + "tongSoBuoi=" + tongSoBuoi + ", " + "hocPhiThoaThuan=" + hocPhiThoaThuan + ", " + "soTienGiam=" + soTienGiam + ", " + "maNguoiDangKy=" + maNguoiDangKy + ", " + "trangThai=" + trangThai + ", " + "ghiChu=" + ghiChu + '}';
    }
}
