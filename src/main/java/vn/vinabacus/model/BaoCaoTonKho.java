package vn.vinabacus.model;

import java.math.BigDecimal;

/** So lieu mot hang hoa trong mot ky kho (thang/nam) cua mot co so. */
public class BaoCaoTonKho {

    private final int maHangHoa;
    private final String maSanPham;
    private final String tenHangHoa;
    private final String donViTinh;
    private final BigDecimal tonDau;
    private final BigDecimal nhap;
    private final BigDecimal xuatHocVien;
    private final BigDecimal xuatCoSo;
    private final BigDecimal dieuChinh;
    private final BigDecimal tonCuoi;
    private final BigDecimal donGiaXuatBan;

    public BaoCaoTonKho(
            int maHangHoa, String maSanPham, String tenHangHoa,
            String donViTinh, BigDecimal tonDau, BigDecimal nhap,
            BigDecimal xuatHocVien, BigDecimal xuatCoSo,
            BigDecimal dieuChinh, BigDecimal tonCuoi) {
        this(maHangHoa, maSanPham, tenHangHoa, donViTinh,
             tonDau, nhap, xuatHocVien, xuatCoSo, dieuChinh, tonCuoi, null);
    }

    // Constructor moi: don gia xuat ban su dung khi xuat theo mau T10.
    public BaoCaoTonKho(
            int maHangHoa, String maSanPham, String tenHangHoa,
            String donViTinh, BigDecimal tonDau, BigDecimal nhap,
            BigDecimal xuatHocVien, BigDecimal xuatCoSo,
            BigDecimal dieuChinh, BigDecimal tonCuoi,
            BigDecimal donGiaXuatBan) {
        this.donGiaXuatBan = donGiaXuatBan;
        this.maHangHoa = maHangHoa;
        this.maSanPham = maSanPham;
        this.tenHangHoa = tenHangHoa;
        this.donViTinh = donViTinh;
        this.tonDau = tonDau;
        this.nhap = nhap;
        this.xuatHocVien = xuatHocVien;
        this.xuatCoSo = xuatCoSo;
        this.dieuChinh = dieuChinh;
        this.tonCuoi = tonCuoi;
    }

    public int getMaHangHoa() { return maHangHoa; }
    public String getMaSanPham() { return maSanPham; }
    public String getTenHangHoa() { return tenHangHoa; }
    public String getDonViTinh() { return donViTinh; }
    public BigDecimal getTonDau() { return tonDau; }
    public BigDecimal getNhap() { return nhap; }
    public BigDecimal getXuatHocVien() { return xuatHocVien; }
    public BigDecimal getXuatCoSo() { return xuatCoSo; }
    public BigDecimal getDieuChinh() { return dieuChinh; }
    public BigDecimal getTonCuoi() { return tonCuoi; }
    public BigDecimal getDonGiaXuatBan() { return donGiaXuatBan; }
}
