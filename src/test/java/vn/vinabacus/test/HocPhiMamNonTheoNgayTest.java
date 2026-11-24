package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.vinabacus.service.HocPhiMamNonTheoNgay;
import vn.vinabacus.service.HocPhiMamNonTheoNgay.Doan;

class HocPhiMamNonTheoNgayTest {
    @Test void nhapHocGiuaThangLamTronTongMotLan(){
        var result=HocPhiMamNonTheoNgay.tinh(21,List.of(new Doan(new BigDecimal("1500001"),8)));
        assertEquals(new BigDecimal("571429"),result.tong());
        assertEquals(result.tong(),result.theoDoan().get(0));
    }
    @Test void chuyenLopHaiMucPhiKhongTrungNgay(){
        var result=HocPhiMamNonTheoNgay.tinh(20,List.of(
            new Doan(new BigDecimal("1500000"),10),new Doan(new BigDecimal("1800000"),5)));
        assertEquals(new BigDecimal("1200000"),result.tong());
        assertEquals(result.tong(),result.theoDoan().stream().reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    @Test void tuChoiSoNgayTrung(){
        assertThrows(IllegalArgumentException.class,()->HocPhiMamNonTheoNgay.tinh(20,List.of(
            new Doan(BigDecimal.ONE,15),new Doan(BigDecimal.ONE,10))));
    }
}
