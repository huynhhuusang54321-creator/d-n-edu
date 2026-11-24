package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.HocVien. */
public class HocVienModel {
    private int maHocVien;
    private int maCoSo;
    private String maHoSo;
    private String hoTen;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String anhDaiDien;
    private String tenPhuHuynh;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private Integer maNguonTiepNhan;
    private Integer maNguoiTuVan;
    private int maNguoiTao;
    private LocalDateTime ngayTao = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    private LocalDate ngayChinhThuc;
    private String trangThai = "TIEP_NHAN";
    private String ghiChu;

    public HocVienModel() { }

    public HocVienModel(int maHocVien, int maCoSo, String maHoSo, String hoTen, LocalDate ngaySinh, String gioiTinh, String anhDaiDien, String tenPhuHuynh, String soDienThoai, String email, String diaChi, Integer maNguonTiepNhan, Integer maNguoiTuVan, int maNguoiTao, LocalDateTime ngayTao, LocalDate ngayChinhThuc, String trangThai, String ghiChu) {
        this.maHocVien = maHocVien;
        this.maCoSo = maCoSo;
        this.maHoSo = maHoSo;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.anhDaiDien = anhDaiDien;
        this.tenPhuHuynh = tenPhuHuynh;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.diaChi = diaChi;
        this.maNguonTiepNhan = maNguonTiepNhan;
        this.maNguoiTuVan = maNguoiTuVan;
        this.maNguoiTao = maNguoiTao;
        this.ngayTao = ngayTao;
        this.ngayChinhThuc = ngayChinhThuc;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }

    // Getters và setters
    public int getMaHocVien() { return maHocVien; }
    public void setMaHocVien(int maHocVien) { this.maHocVien = maHocVien; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getMaHoSo() { return maHoSo; }
    public void setMaHoSo(String maHoSo) { this.maHoSo = maHoSo; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    public String getAnhDaiDien() { return anhDaiDien; }
    public void setAnhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; }
    public String getTenPhuHuynh() { return tenPhuHuynh; }
    public void setTenPhuHuynh(String tenPhuHuynh) { this.tenPhuHuynh = tenPhuHuynh; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public Integer getMaNguonTiepNhan() { return maNguonTiepNhan; }
    public void setMaNguonTiepNhan(Integer maNguonTiepNhan) { this.maNguonTiepNhan = maNguonTiepNhan; }
    public Integer getMaNguoiTuVan() { return maNguoiTuVan; }
    public void setMaNguoiTuVan(Integer maNguoiTuVan) { this.maNguoiTuVan = maNguoiTuVan; }
    public int getMaNguoiTao() { return maNguoiTao; }
    public void setMaNguoiTao(int maNguoiTao) { this.maNguoiTao = maNguoiTao; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDate getNgayChinhThuc() { return ngayChinhThuc; }
    public void setNgayChinhThuc(LocalDate ngayChinhThuc) { this.ngayChinhThuc = ngayChinhThuc; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    @Override
    public String toString() {
        return "HocVienModel{" + "maHocVien=" + maHocVien + ", " + "maCoSo=" + maCoSo + ", " + "maHoSo=" + maHoSo + ", " + "hoTen=" + hoTen + ", " + "ngaySinh=" + ngaySinh + ", " + "gioiTinh=" + gioiTinh + ", " + "anhDaiDien=" + anhDaiDien + ", " + "tenPhuHuynh=" + tenPhuHuynh + ", " + "soDienThoai=" + soDienThoai + ", " + "email=" + email + ", " + "diaChi=" + diaChi + ", " + "maNguonTiepNhan=" + maNguonTiepNhan + ", " + "maNguoiTuVan=" + maNguoiTuVan + ", " + "maNguoiTao=" + maNguoiTao + ", " + "ngayTao=" + ngayTao + ", " + "ngayChinhThuc=" + ngayChinhThuc + ", " + "trangThai=" + trangThai + ", " + "ghiChu=" + ghiChu + '}';
    }
}
