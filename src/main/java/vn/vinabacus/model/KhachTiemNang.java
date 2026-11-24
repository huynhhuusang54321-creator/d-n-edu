package vn.vinabacus.model;
import java.time.LocalDate;
/** Ho so tiep nhan ban dau theo dbo.KhachTiemNang. */
public record KhachTiemNang(int maKhach, int maCoSo, String hoTen,
        String soDienThoai, String tenNguoiLienHe, String nhuCau,
        String trangThai, boolean daHocThu, String ghiChu,
        LocalDate ngaySinh,String gioiTinh,String diaChi) {
    public KhachTiemNang(int maKhach,int maCoSo,String hoTen,String soDienThoai,
        String tenNguoiLienHe,String nhuCau,String trangThai,boolean daHocThu,String ghiChu){
        this(maKhach,maCoSo,hoTen,soDienThoai,tenNguoiLienHe,nhuCau,trangThai,daHocThu,ghiChu,null,null,null);
    }
}
