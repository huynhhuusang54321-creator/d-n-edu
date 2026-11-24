package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.BuoiHoc. */
public class BuoiHocModel {
    private int maBuoiHoc;
    private int maCoSo;
    private int maLop;
    private Integer maThoiKhoaBieu;
    private LocalDate ngayHoc;
    private int maCaHoc;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private Integer maGiaoVien;
    private Integer maNguoiKichHoat;
    private String trangThai = "DU_KIEN";
    private LocalDateTime ngayKichHoat;
    private String ghiChu;

    public BuoiHocModel() { }

    public BuoiHocModel(int maBuoiHoc, int maCoSo, int maLop, Integer maThoiKhoaBieu, LocalDate ngayHoc, int maCaHoc, LocalTime gioBatDau, LocalTime gioKetThuc, Integer maGiaoVien, Integer maNguoiKichHoat, String trangThai, LocalDateTime ngayKichHoat, String ghiChu) {
        this.maBuoiHoc = maBuoiHoc;
        this.maCoSo = maCoSo;
        this.maLop = maLop;
        this.maThoiKhoaBieu = maThoiKhoaBieu;
        this.ngayHoc = ngayHoc;
        this.maCaHoc = maCaHoc;
        this.gioBatDau = gioBatDau;
        this.gioKetThuc = gioKetThuc;
        this.maGiaoVien = maGiaoVien;
        this.maNguoiKichHoat = maNguoiKichHoat;
        this.trangThai = trangThai;
        this.ngayKichHoat = ngayKichHoat;
        this.ghiChu = ghiChu;
    }

    // Getters và setters
    public int getMaBuoiHoc() { return maBuoiHoc; }
    public void setMaBuoiHoc(int maBuoiHoc) { this.maBuoiHoc = maBuoiHoc; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public int getMaLop() { return maLop; }
    public void setMaLop(int maLop) { this.maLop = maLop; }
    public Integer getMaThoiKhoaBieu() { return maThoiKhoaBieu; }
    public void setMaThoiKhoaBieu(Integer maThoiKhoaBieu) { this.maThoiKhoaBieu = maThoiKhoaBieu; }
    public LocalDate getNgayHoc() { return ngayHoc; }
    public void setNgayHoc(LocalDate ngayHoc) { this.ngayHoc = ngayHoc; }
    public int getMaCaHoc() { return maCaHoc; }
    public void setMaCaHoc(int maCaHoc) { this.maCaHoc = maCaHoc; }
    public LocalTime getGioBatDau() { return gioBatDau; }
    public void setGioBatDau(LocalTime gioBatDau) { this.gioBatDau = gioBatDau; }
    public LocalTime getGioKetThuc() { return gioKetThuc; }
    public void setGioKetThuc(LocalTime gioKetThuc) { this.gioKetThuc = gioKetThuc; }
    public Integer getMaGiaoVien() { return maGiaoVien; }
    public void setMaGiaoVien(Integer maGiaoVien) { this.maGiaoVien = maGiaoVien; }
    public Integer getMaNguoiKichHoat() { return maNguoiKichHoat; }
    public void setMaNguoiKichHoat(Integer maNguoiKichHoat) { this.maNguoiKichHoat = maNguoiKichHoat; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getNgayKichHoat() { return ngayKichHoat; }
    public void setNgayKichHoat(LocalDateTime ngayKichHoat) { this.ngayKichHoat = ngayKichHoat; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    @Override
    public String toString() {
        return "BuoiHocModel{" + "maBuoiHoc=" + maBuoiHoc + ", " + "maCoSo=" + maCoSo + ", " + "maLop=" + maLop + ", " + "maThoiKhoaBieu=" + maThoiKhoaBieu + ", " + "ngayHoc=" + ngayHoc + ", " + "maCaHoc=" + maCaHoc + ", " + "gioBatDau=" + gioBatDau + ", " + "gioKetThuc=" + gioKetThuc + ", " + "maGiaoVien=" + maGiaoVien + ", " + "maNguoiKichHoat=" + maNguoiKichHoat + ", " + "trangThai=" + trangThai + ", " + "ngayKichHoat=" + ngayKichHoat + ", " + "ghiChu=" + ghiChu + '}';
    }
}
