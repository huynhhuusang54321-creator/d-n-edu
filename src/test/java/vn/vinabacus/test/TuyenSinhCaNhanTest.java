package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.service.TuyenSinhService;

class TuyenSinhCaNhanTest {
    @Test void hoSoGiuThongTinMoi() {
        LocalDate ngay=LocalDate.of(2018,4,5);
        KhachTiemNang k=new KhachTiemNang(1,2,"Học viên",null,null,null,
                "TIEP_NHAN",false,null,ngay,"NU","Cần Thơ");
        assertEquals(ngay,k.ngaySinh());
        assertEquals("NU",k.gioiTinh());
        assertEquals("Cần Thơ",k.diaChi());
    }

    @Test void gioiTinhVaDiaChiSaiBiChanTruocKhiKetNoi() {
        TuyenSinhService service=new TuyenSinhService();
        KhachTiemNang saiGioiTinh=new KhachTiemNang(0,0,"Học viên",null,null,
                null,"TIEP_NHAN",false,null,null,"X",null);
        assertThrows(IllegalArgumentException.class,
                ()->service.them(CoSo.TRUNG_TAM,saiGioiTinh));
        KhachTiemNang diaChiDai=new KhachTiemNang(0,0,"Học viên",null,null,
                null,"TIEP_NHAN",false,null,null,"NAM","a".repeat(401));
        assertThrows(IllegalArgumentException.class,
                ()->service.them(CoSo.TRUNG_TAM,diaChiDai));
    }
}
