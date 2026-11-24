package vn.vinabacus.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Đăng ký khóa Trung tâm cùng lớp đang xếp (nếu có). */
public record DangKyKhoaHoc(
        int maDangKy,
        int maHocVien,
        Integer maKhach,
        String hoTen,
        int maKhoaHoc,
        String tenMonHoc,
        String tenKhoaHoc,
        int tongSoBuoi,
        BigDecimal hocPhiSauGiam,
        LocalDate ngayBatDau,
        String trangThai,
        Integer maLop,
        String tenLop) {

    /** Một khóa có thể được chọn cùng một lớp phù hợp hoặc để chờ xếp lớp. */
    public record LuaChon(int maKhoaHoc, Integer maLop) { }

    /** Yêu cầu chốt học phí và khoản thực thu cho một lần lưu nhiều khóa. */
    public record TaiChinh(List<Integer> maChuongTrinh, BigDecimal phaiThuDuKien,
            BigDecimal soTienThu, String hinhThuc, int maTaiKhoan,
            String ghiChu, UUID maGiaoDich) {
        public TaiChinh { maChuongTrinh = List.copyOf(maChuongTrinh); }
    }

    public record KetQua(int maHocVien, int soDangKy, int soXepNgay,
                        boolean chinhThuc) { }

    /** Thông tin lớp đọc từ SQL Server để chọn lớp cho từng khóa. */
    public record ThongTinLop(int maLop, int maKhoaHoc, String tenLop,
            String tenGiaoVien, String lichHoc, int soDaGiuCho,
            Integer siSoToiDaLop, Integer sucChuaPhong, int gioiHanTiepNhan,
            LocalDate ngayKhaiGiang, String trangThaiNhan, String tenPhong) {
        public ThongTinLop(int maLop,int maKhoaHoc,String tenLop,String tenGiaoVien,
                String lichHoc,int soDaGiuCho,Integer siSoToiDaLop,Integer sucChuaPhong,
                int gioiHanTiepNhan,LocalDate ngayKhaiGiang,String trangThaiNhan) {
            this(maLop,maKhoaHoc,tenLop,tenGiaoVien,lichHoc,soDaGiuCho,
                    siSoToiDaLop,sucChuaPhong,gioiHanTiepNhan,ngayKhaiGiang,
                    trangThaiNhan,null);
        }
    }
}
