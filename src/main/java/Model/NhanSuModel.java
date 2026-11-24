package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.NhanSu. */
public class NhanSuModel {
    private int maNhanSu;
    private int maCoSo;
    private String hoTen;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private String anhDaiDien;
    private String tenDangNhap;
    private String matKhauBam;
    private String vaiTro;
    private boolean choPhepDangNhap = false;
    private boolean trangThai = true;
    private LocalDateTime ngayTao = LocalDateTime.now(ZoneOffset.UTC).withNano(0);

    public NhanSuModel() { }

    public NhanSuModel(int maNhanSu, int maCoSo, String hoTen, LocalDate ngaySinh, String gioiTinh, String soDienThoai, String email, String diaChi, String anhDaiDien, String tenDangNhap, String matKhauBam, String vaiTro, boolean choPhepDangNhap, boolean trangThai, LocalDateTime ngayTao) {
        this.maNhanSu = maNhanSu;
        this.maCoSo = maCoSo;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.diaChi = diaChi;
        this.anhDaiDien = anhDaiDien;
        this.tenDangNhap = tenDangNhap;
        this.matKhauBam = matKhauBam;
        this.vaiTro = vaiTro;
        this.choPhepDangNhap = choPhepDangNhap;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
    }

    // Getters và setters
    public int getMaNhanSu() { return maNhanSu; }
    public void setMaNhanSu(int maNhanSu) { this.maNhanSu = maNhanSu; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getAnhDaiDien() { return anhDaiDien; }
    public void setAnhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
    public String getMatKhauBam() { return matKhauBam; }
    public void setMatKhauBam(String matKhauBam) { this.matKhauBam = matKhauBam; }
    public String getVaiTro() { return vaiTro; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }
    public boolean getChoPhepDangNhap() { return choPhepDangNhap; }
    public void setChoPhepDangNhap(boolean choPhepDangNhap) { this.choPhepDangNhap = choPhepDangNhap; }
    public boolean isChoPhepDangNhap() { return choPhepDangNhap; }
    public boolean getTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }
    public boolean isTrangThai() { return trangThai; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }

    @Override
    public String toString() {
        return "NhanSuModel{" + "maNhanSu=" + maNhanSu + ", " + "maCoSo=" + maCoSo + ", " + "hoTen=" + hoTen + ", " + "ngaySinh=" + ngaySinh + ", " + "gioiTinh=" + gioiTinh + ", " + "soDienThoai=" + soDienThoai + ", " + "email=" + email + ", " + "diaChi=" + diaChi + ", " + "anhDaiDien=" + anhDaiDien + ", " + "tenDangNhap=" + tenDangNhap + ", " + "vaiTro=" + vaiTro + ", " + "choPhepDangNhap=" + choPhepDangNhap + ", " + "trangThai=" + trangThai + ", " + "ngayTao=" + ngayTao + '}';
    }
}
