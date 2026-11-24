package vn.vinabacus.model;

import java.time.LocalDate;

/** Ban ghi lop o co so Trung tam; ma duoc giu noi bo, khong hien tren bang. */
public record LopTrungTam(
        int maLop,
        int maCoSo,
        int maKhoaHoc,
        int maMonHoc,
        String tenMonHoc,
        String tenKhoaHoc,
        String tenLop,
        Integer maGiaoVien,
        String tenGiaoVien,
        LocalDate ngayKhaiGiang,
        String trangThai,
        Integer siSoToiDa,
        int siSoHienTai,
        int siSoDaDat,
        Integer sucChuaPhong,
        String tenPhong) {
}
