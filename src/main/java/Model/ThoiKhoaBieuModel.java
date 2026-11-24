package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.ThoiKhoaBieu. */
public class ThoiKhoaBieuModel {
    private int maThoiKhoaBieu;
    private int maCoSo;
    private int maLop;
    private int thuTrongTuan;
    private int maCaHoc;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private boolean trangThai = true;

    public ThoiKhoaBieuModel() { }

    public ThoiKhoaBieuModel(int maThoiKhoaBieu, int maCoSo, int maLop, int thuTrongTuan, int maCaHoc, LocalDate tuNgay, LocalDate denNgay, boolean trangThai) {
        this.maThoiKhoaBieu = maThoiKhoaBieu;
        this.maCoSo = maCoSo;
        this.maLop = maLop;
        this.thuTrongTuan = thuTrongTuan;
        this.maCaHoc = maCaHoc;
        this.tuNgay = tuNgay;
        this.denNgay = denNgay;
        this.trangThai = trangThai;
    }

    // Getters và setters
    public int getMaThoiKhoaBieu() { return maThoiKhoaBieu; }
    public void setMaThoiKhoaBieu(int maThoiKhoaBieu) { this.maThoiKhoaBieu = maThoiKhoaBieu; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public int getMaLop() { return maLop; }
    public void setMaLop(int maLop) { this.maLop = maLop; }
    public int getThuTrongTuan() { return thuTrongTuan; }
    public void setThuTrongTuan(int thuTrongTuan) { this.thuTrongTuan = thuTrongTuan; }
    public int getMaCaHoc() { return maCaHoc; }
    public void setMaCaHoc(int maCaHoc) { this.maCaHoc = maCaHoc; }
    public LocalDate getTuNgay() { return tuNgay; }
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    public LocalDate getDenNgay() { return denNgay; }
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    public boolean getTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }
    public boolean isTrangThai() { return trangThai; }

    @Override
    public String toString() {
        return "ThoiKhoaBieuModel{" + "maThoiKhoaBieu=" + maThoiKhoaBieu + ", " + "maCoSo=" + maCoSo + ", " + "maLop=" + maLop + ", " + "thuTrongTuan=" + thuTrongTuan + ", " + "maCaHoc=" + maCaHoc + ", " + "tuNgay=" + tuNgay + ", " + "denNgay=" + denNgay + ", " + "trangThai=" + trangThai + '}';
    }
}
