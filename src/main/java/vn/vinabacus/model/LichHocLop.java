package vn.vinabacus.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Lich hoc dinh ky cua lop Trung tam; ma ID chi de luu va sua, khong hien tren bang. */
public record LichHocLop(
        int maLichHoc, int maLop, int maKhoaHoc, int maMonHoc,
        String tenMonHoc, String tenKhoaHoc, String tenLop,
        int thuTrongTuan, LocalTime gioBatDau, LocalTime gioKetThuc,
        LocalDate tuNgay, LocalDate denNgay, String trangThai,
        Integer maPhong, String tenPhong) {
}
