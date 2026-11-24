package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import vn.vinabacus.service.QuyTacSiSoLop;

class QuyTacSiSoLopTest {
    @Test
    void khongCoGioiHanRiengThiKhongDuocNhanDuPhongConRong() {
        var ketQua = QuyTacSiSoLop.danhGia(null, 20, 3);
        assertEquals(0, ketQua.gioiHan());
        assertEquals(0, ketQua.conCho());
        assertEquals("Chưa đặt sĩ số tối đa", ketQua.trangThai());
    }

    @Test
    void coGioiHanLopNhungChuaCoPhongThiVanChuaDuocNhan() {
        var ketQua = QuyTacSiSoLop.danhGia(8, null, 3);
        assertEquals(0, ketQua.gioiHan());
        assertEquals(0, ketQua.conCho());
        assertEquals("Chưa gán phòng học", ketQua.trangThai());
    }

    @Test
    void gioiHanLopKhongDuocVuotPhongVaTinhCaChoDaDatTruoc() {
        var vuotPhong = QuyTacSiSoLop.danhGia(18, 15, 2);
        assertEquals(0, vuotPhong.gioiHan());
        assertEquals("Sĩ số lớp vượt sức chứa phòng", vuotPhong.trangThai());

        var ketQua = QuyTacSiSoLop.danhGia(10, 20, 10);
        assertEquals(10, ketQua.gioiHan());
        assertEquals(0, ketQua.conCho());
        assertEquals("Đủ sĩ số", ketQua.trangThai());

        var lopGioiHanThapHon = QuyTacSiSoLop.danhGia(10, 20, 3);
        assertEquals(10, lopGioiHanThapHon.gioiHan());
        assertEquals(7, lopGioiHanThapHon.conCho());
        assertEquals("Đang nhận", lopGioiHanThapHon.trangThai());
    }

    @Test
    void phongCoSucChuaKhongHopLeVaSoGiuChoAmBiChan() {
        assertEquals("Sức chứa phòng không hợp lệ",
                QuyTacSiSoLop.danhGia(10, 0, 1).trangThai());
        assertThrows(IllegalArgumentException.class,
                () -> QuyTacSiSoLop.danhGia(10, 20, -1));
    }
}
