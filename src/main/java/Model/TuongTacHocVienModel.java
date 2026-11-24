package Model;

import java.time.*;
import java.math.BigDecimal;

/** Model ánh xạ đầy đủ bảng dbo.TuongTacHocVien. */
public class TuongTacHocVienModel {
    private int maTuongTac;
    private int maCoSo;
    private int maHocVien;
    private int maNhanSu;
    private String loaiTuongTac;
    private Integer maKhoaHocMongMuon;
    private String thoiGianMongMuon;
    private String noiDung;
    private String ketQua;
    private String trangThaiTruoc;
    private String trangThaiSau;
    private LocalDateTime ngayHenTiep;
    private LocalDateTime ngayTao = LocalDateTime.now(ZoneOffset.UTC).withNano(0);

    public TuongTacHocVienModel() { }

    public TuongTacHocVienModel(int maTuongTac, int maCoSo, int maHocVien, int maNhanSu, String loaiTuongTac, Integer maKhoaHocMongMuon, String thoiGianMongMuon, String noiDung, String ketQua, String trangThaiTruoc, String trangThaiSau, LocalDateTime ngayHenTiep, LocalDateTime ngayTao) {
        this.maTuongTac = maTuongTac;
        this.maCoSo = maCoSo;
        this.maHocVien = maHocVien;
        this.maNhanSu = maNhanSu;
        this.loaiTuongTac = loaiTuongTac;
        this.maKhoaHocMongMuon = maKhoaHocMongMuon;
        this.thoiGianMongMuon = thoiGianMongMuon;
        this.noiDung = noiDung;
        this.ketQua = ketQua;
        this.trangThaiTruoc = trangThaiTruoc;
        this.trangThaiSau = trangThaiSau;
        this.ngayHenTiep = ngayHenTiep;
        this.ngayTao = ngayTao;
    }

    // Getters và setters
    public int getMaTuongTac() { return maTuongTac; }
    public void setMaTuongTac(int maTuongTac) { this.maTuongTac = maTuongTac; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public int getMaHocVien() { return maHocVien; }
    public void setMaHocVien(int maHocVien) { this.maHocVien = maHocVien; }
    public int getMaNhanSu() { return maNhanSu; }
    public void setMaNhanSu(int maNhanSu) { this.maNhanSu = maNhanSu; }
    public String getLoaiTuongTac() { return loaiTuongTac; }
    public void setLoaiTuongTac(String loaiTuongTac) { this.loaiTuongTac = loaiTuongTac; }
    public Integer getMaKhoaHocMongMuon() { return maKhoaHocMongMuon; }
    public void setMaKhoaHocMongMuon(Integer maKhoaHocMongMuon) { this.maKhoaHocMongMuon = maKhoaHocMongMuon; }
    public String getThoiGianMongMuon() { return thoiGianMongMuon; }
    public void setThoiGianMongMuon(String thoiGianMongMuon) { this.thoiGianMongMuon = thoiGianMongMuon; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
    public String getKetQua() { return ketQua; }
    public void setKetQua(String ketQua) { this.ketQua = ketQua; }
    public String getTrangThaiTruoc() { return trangThaiTruoc; }
    public void setTrangThaiTruoc(String trangThaiTruoc) { this.trangThaiTruoc = trangThaiTruoc; }
    public String getTrangThaiSau() { return trangThaiSau; }
    public void setTrangThaiSau(String trangThaiSau) { this.trangThaiSau = trangThaiSau; }
    public LocalDateTime getNgayHenTiep() { return ngayHenTiep; }
    public void setNgayHenTiep(LocalDateTime ngayHenTiep) { this.ngayHenTiep = ngayHenTiep; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }

    @Override
    public String toString() {
        return "TuongTacHocVienModel{" + "maTuongTac=" + maTuongTac + ", " + "maCoSo=" + maCoSo + ", " + "maHocVien=" + maHocVien + ", " + "maNhanSu=" + maNhanSu + ", " + "loaiTuongTac=" + loaiTuongTac + ", " + "maKhoaHocMongMuon=" + maKhoaHocMongMuon + ", " + "thoiGianMongMuon=" + thoiGianMongMuon + ", " + "noiDung=" + noiDung + ", " + "ketQua=" + ketQua + ", " + "trangThaiTruoc=" + trangThaiTruoc + ", " + "trangThaiSau=" + trangThaiSau + ", " + "ngayHenTiep=" + ngayHenTiep + ", " + "ngayTao=" + ngayTao + '}';
    }
}
