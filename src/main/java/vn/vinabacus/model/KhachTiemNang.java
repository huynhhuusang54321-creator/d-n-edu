package vn.vinabacus.model;
/** Ho so tiep nhan ban dau theo dbo.KhachTiemNang. */
public record KhachTiemNang(int maKhach, int maCoSo, String hoTen,
        String soDienThoai, String tenNguoiLienHe, String nhuCau,
        String trangThai, boolean daHocThu, String ghiChu) { }
