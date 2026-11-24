package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.CoSo. */
public class CoSoModel {
    private int maCoSo;
    private String maCoSoHienThi;
    private String tenCoSo;
    private String loaiCoSo;
    private String diaChi;
    private String soDienThoai;
    private String logo;
    private boolean trangThai = true;

    public CoSoModel() { }

    public CoSoModel(int maCoSo, String maCoSoHienThi, String tenCoSo, String loaiCoSo, String diaChi, String soDienThoai, String logo, boolean trangThai) {
        this.maCoSo = maCoSo;
        this.maCoSoHienThi = maCoSoHienThi;
        this.tenCoSo = tenCoSo;
        this.loaiCoSo = loaiCoSo;
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.logo = logo;
        this.trangThai = trangThai;
    }

    // Getters và setters
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getMaCoSoHienThi() { return maCoSoHienThi; }
    public void setMaCoSoHienThi(String maCoSoHienThi) { this.maCoSoHienThi = maCoSoHienThi; }
    public String getTenCoSo() { return tenCoSo; }
    public void setTenCoSo(String tenCoSo) { this.tenCoSo = tenCoSo; }
    public String getLoaiCoSo() { return loaiCoSo; }
    public void setLoaiCoSo(String loaiCoSo) { this.loaiCoSo = loaiCoSo; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public boolean getTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }
    public boolean isTrangThai() { return trangThai; }

    @Override
    public String toString() {
        return "CoSoModel{" + "maCoSo=" + maCoSo + ", " + "maCoSoHienThi=" + maCoSoHienThi + ", " + "tenCoSo=" + tenCoSo + ", " + "loaiCoSo=" + loaiCoSo + ", " + "diaChi=" + diaChi + ", " + "soDienThoai=" + soDienThoai + ", " + "logo=" + logo + ", " + "trangThai=" + trangThai + '}';
    }
}
