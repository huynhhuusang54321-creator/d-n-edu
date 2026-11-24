package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.DanhMuc. */
public class DanhMucModel {
    private int maDanhMuc;
    private int maCoSo;
    private String nhomDanhMuc;
    private String maKyHieu;
    private String tenDanhMuc;
    private Integer maCha;
    private int thuTu;
    private String cauHinh;
    private boolean trangThai = true;

    public DanhMucModel() { }

    public DanhMucModel(int maDanhMuc, int maCoSo, String nhomDanhMuc, String maKyHieu, String tenDanhMuc, Integer maCha, int thuTu, String cauHinh, boolean trangThai) {
        this.maDanhMuc = maDanhMuc;
        this.maCoSo = maCoSo;
        this.nhomDanhMuc = nhomDanhMuc;
        this.maKyHieu = maKyHieu;
        this.tenDanhMuc = tenDanhMuc;
        this.maCha = maCha;
        this.thuTu = thuTu;
        this.cauHinh = cauHinh;
        this.trangThai = trangThai;
    }

    // Getters và setters
    public int getMaDanhMuc() { return maDanhMuc; }
    public void setMaDanhMuc(int maDanhMuc) { this.maDanhMuc = maDanhMuc; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getNhomDanhMuc() { return nhomDanhMuc; }
    public void setNhomDanhMuc(String nhomDanhMuc) { this.nhomDanhMuc = nhomDanhMuc; }
    public String getMaKyHieu() { return maKyHieu; }
    public void setMaKyHieu(String maKyHieu) { this.maKyHieu = maKyHieu; }
    public String getTenDanhMuc() { return tenDanhMuc; }
    public void setTenDanhMuc(String tenDanhMuc) { this.tenDanhMuc = tenDanhMuc; }
    public Integer getMaCha() { return maCha; }
    public void setMaCha(Integer maCha) { this.maCha = maCha; }
    public int getThuTu() { return thuTu; }
    public void setThuTu(int thuTu) { this.thuTu = thuTu; }
    public String getCauHinh() { return cauHinh; }
    public void setCauHinh(String cauHinh) { this.cauHinh = cauHinh; }
    public boolean getTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }
    public boolean isTrangThai() { return trangThai; }

    @Override
    public String toString() {
        return "DanhMucModel{" + "maDanhMuc=" + maDanhMuc + ", " + "maCoSo=" + maCoSo + ", " + "nhomDanhMuc=" + nhomDanhMuc + ", " + "maKyHieu=" + maKyHieu + ", " + "tenDanhMuc=" + tenDanhMuc + ", " + "maCha=" + maCha + ", " + "thuTu=" + thuTu + ", " + "cauHinh=" + cauHinh + ", " + "trangThai=" + trangThai + '}';
    }
}
