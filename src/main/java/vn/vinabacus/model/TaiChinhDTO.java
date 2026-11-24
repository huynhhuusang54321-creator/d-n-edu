package vn.vinabacus.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dữ liệu trung gian TV5; không thay thế các entity chung của những thành viên khác. */
public final class TaiChinhDTO {
    private TaiChinhDTO() {}

    public record LuaChon(int id, String ten) {
        @Override public String toString() { return ten; }
    }

    public record KhoanThu(int maKhoan, int maHocVien, String hocVien,
                           String loai, BigDecimal goc, BigDecimal dieuChinh,
                           BigDecimal daThu, LocalDate han, String trangThai,
                           Integer maDangKy, Integer maBangTinh,String soDienThoai) {
        public KhoanThu(int maKhoan,int maHocVien,String hocVien,String loai,
                        BigDecimal goc,BigDecimal dieuChinh,BigDecimal daThu,
                        LocalDate han,String trangThai,Integer maDangKy,Integer maBangTinh){
            this(maKhoan,maHocVien,hocVien,loai,goc,dieuChinh,daThu,han,trangThai,
                maDangKy,maBangTinh,null);
        }
        /** Tương thích các đoạn code cũ tạo KhoanThu với 9 tham số. */
        public KhoanThu(int maKhoan, int maHocVien, String hocVien, String loai,
                        BigDecimal goc, BigDecimal dieuChinh, BigDecimal daThu,
                        LocalDate han, String trangThai) {
            this(maKhoan,maHocVien,hocVien,loai,goc,dieuChinh,daThu,han,trangThai,null,null,null);
        }
        public BigDecimal phaiThu() { return goc.add(dieuChinh); }
        public BigDecimal conNo() {
            if (!"HIEU_LUC".equals(trangThai)) return BigDecimal.ZERO;
            return phaiThu().subtract(daThu);
        }
        public String tenLoai() { return switch (loai) {
            case "HOC_PHI_KHOA" -> "Học phí khóa";
            case "HOC_PHI_THANG" -> "Học phí tháng";
            case "TIEN_AN" -> "Tiền ăn";
            default -> loai;
        }; }
    }

    public record PhieuThu(int maPhieu, String soPhieu, String hocVien,
                           BigDecimal soTien, LocalDateTime ngayThu,
                           String hinhThuc, String trangThai,
                           String taiKhoanThu, String noiDung, String ghiChu,
                           LocalDateTime ngayHuy,String lyDoHuy) {
        public PhieuThu(int maPhieu,String soPhieu,String hocVien,BigDecimal soTien,
                        LocalDateTime ngayThu,String hinhThuc,String trangThai,
                        String taiKhoanThu,String noiDung,String ghiChu){
            this(maPhieu,soPhieu,hocVien,soTien,ngayThu,hinhThuc,trangThai,
                taiKhoanThu,noiDung,ghiChu,null,null);
        }
        public PhieuThu(int maPhieu, String soPhieu, String hocVien,
                        BigDecimal soTien, LocalDateTime ngayThu,
                        String hinhThuc, String trangThai,
                        String taiKhoanThu, String ghiChu) {
            this(maPhieu,soPhieu,hocVien,soTien,ngayThu,hinhThuc,trangThai,
                 taiKhoanThu,null,ghiChu,null,null);
        }
        public PhieuThu(int maPhieu, String soPhieu, String hocVien,
                        BigDecimal soTien, LocalDateTime ngayThu,
                        String hinhThuc, String trangThai) {
            this(maPhieu,soPhieu,hocVien,soTien,ngayThu,hinhThuc,trangThai,
                 null,null,null,null,null);
        }
    }

    /** Phân bổ thực tế đã lưu của một phiếu, kể cả phiếu sau đó bị hủy. */
    public record PhanBoPhieu(int maKhoan, String noiDung, BigDecimal soTien,
                              String loaiBienDong) { }
    public record LichSuThu(int maPhieu,String soPhieu,LocalDateTime ngayThu,
                            String khoaHoc,BigDecimal soTien,String trangThai) { }
    public record UuDaiDangKy(int maDangKy,String tenSnapshot,BigDecimal soTienGiam) { }
    public record DongThuBaoCao(LocalDate ngayThu,String khoaHoacKhoan,
                                String nhanVien,BigDecimal phanBoRong) { }

    /** Đăng ký Trung tâm đã có giá sau chiết khấu, chưa phát sinh khoản thu. */
    public record DangKyChuaThu(int id, String hocVien, String khoa,
                                BigDecimal hocPhiGoc, BigDecimal daGiam,
                                BigDecimal sauGiam) {
        @Override public String toString() {
            return "#" + id + " | " + hocVien + " | " + khoa + " | " + sauGiam.toPlainString();
        }
    }

    /** Học phí tất cả đăng ký Trung tâm, kể cả đăng ký đã có khoản phải thu. */
    public record HocPhiTrungTam(int maDangKy, String hocVien, String monHoc,
                                  String khoaHoc, String lopHoc, BigDecimal goc,
                                  BigDecimal giam, BigDecimal phaiThu,
                                  BigDecimal daDong, BigDecimal conNo,
                                  String soPhieuGanNhat, LocalDateTime ngayThuGanNhat,
                                  BigDecimal soTienLanThu,int maHocVien,String soDienThoai) {
        public HocPhiTrungTam(int maDangKy,String hocVien,String monHoc,
                              String khoaHoc,String lopHoc,BigDecimal goc,
                              BigDecimal giam,BigDecimal phaiThu,BigDecimal daDong,
                              BigDecimal conNo,String soPhieuGanNhat,
                              LocalDateTime ngayThuGanNhat,BigDecimal soTienLanThu){
            this(maDangKy,hocVien,monHoc,khoaHoc,lopHoc,goc,giam,phaiThu,daDong,
                conNo,soPhieuGanNhat,ngayThuGanNhat,soTienLanThu,0,null);
        }
        public HocPhiTrungTam(int maDangKy, String hocVien, String monHoc,
                              String khoaHoc, String lopHoc, BigDecimal goc,
                              BigDecimal giam, BigDecimal phaiThu,
                              BigDecimal daDong, BigDecimal conNo) {
            this(maDangKy,hocVien,monHoc,khoaHoc,lopHoc,goc,giam,phaiThu,
                 daDong,conNo,null,null,null,0,null);
        }
    }

    /** Bảng tính phí Mầm non đã CHỐT; có thể lập hai khoản theo từng loại. */
    public record BangTinhChot(int id, String hocVien, String ky,
                               BigDecimal hocPhi, BigDecimal tienAn) {
        @Override public String toString() {
            return "#" + id + " | " + hocVien + " | " + ky;
        }
    }

    public record MucPhi(int id, String lop, String loai,
                         LocalDate apDung, BigDecimal donGia) {}

    public record ThongKe(BigDecimal doanhThuThang, BigDecimal daThuToanBo,
                          BigDecimal tongPhaiThuHieuLuc, BigDecimal congNoHienTai,
                          long soKhoanConNo, long soKhoanDaTatToan,
                          BigDecimal tongGiamGia, long soHocVienConNo,
                          long soPhieuHopLe) {}
}
