package vn.vinabacus.test;

import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DangKyKhoaHoc.LuaChon;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.service.DangKyKhoaHocService;
import vn.vinabacus.service.TuyenSinhService;

/** Kiểm tra quy tắc đầu vào Giai đoạn 2 mà không ghi SQL Server. */
public final class KiemThuDangKyKhoaHoc {
    private static int soKiemThu;

    private static void hopLe(Runnable thaoTac) {
        thaoTac.run();
        soKiemThu++;
    }

    private static void tuChoi(Runnable thaoTac) {
        try {
            thaoTac.run();
            throw new AssertionError("Phải từ chối dữ liệu đăng ký không hợp lệ");
        } catch (IllegalArgumentException expected) {
            soKiemThu++;
        }
    }

    public static void main(String[] args) throws Exception {
        LocalDate ngay = LocalDate.of(2026, 11, 21);
        hopLe(() -> DangKyKhoaHocService.kiemTraDangKy(1, ngay,
                List.of(new LuaChon(10, null), new LuaChon(20, 30))));
        tuChoi(() -> DangKyKhoaHocService.kiemTraDangKy(1, ngay, List.of()));
        tuChoi(() -> DangKyKhoaHocService.kiemTraDangKy(1, ngay,
                List.of(new LuaChon(10, null), new LuaChon(10, 30))));
        tuChoi(() -> DangKyKhoaHocService.kiemTraDangKy(1, ngay,
                List.of(new LuaChon(10, 0))));
        tuChoi(() -> DangKyKhoaHocService.kiemTraDangKy(0, ngay,
                List.of(new LuaChon(10, null))));
        tuChoi(() -> DangKyKhoaHocService.kiemTraDangKy(1, null,
                List.of(new LuaChon(10, null))));
        tuChoi(() -> {
            try {
                new TuyenSinhService().chuyenDoi(CoSo.TRUNG_TAM, 1);
            } catch (java.sql.SQLException ex) {
                throw new AssertionError("Không được truy vấn SQL trước khi chặn luồng cũ", ex);
            }
        });
        hopLe(() -> {
            if (!TuyenSinhService.nguonTrangThaiHopLe("HOC_THU").equals(List.of("TIEP_NHAN")))
                throw new AssertionError("Học thử chỉ được bắt đầu từ Tiếp nhận");
        });
        hopLe(() -> {
            if (!TuyenSinhService.nguonTrangThaiHopLe("TIEP_NHAN").equals(List.of("KHONG_DANG_KY")))
                throw new AssertionError("Khôi phục chỉ từ Không đăng ký");
        });
        tuChoi(() -> TuyenSinhService.nguonTrangThaiHopLe("DA_CHUYEN_DOI"));
        tuChoi(() -> TuyenSinhService.nguonTrangThaiHopLe("CHO_XEP_LOP"));
        tuChoi(() -> {
            try {
                new TuyenSinhService().them(CoSo.TRUNG_TAM,
                        new KhachTiemNang(0,0,"Học sinh",null,null,null,
                                "CHO_XEP_LOP",false,null));
            } catch (java.sql.SQLException ex) {
                throw new AssertionError("Không được truy vấn SQL trước khi chặn trạng thái tạo sai", ex);
            }
        });
        System.out.println("PASS: " + soKiemThu + " kiểm thử quy tắc đăng ký Giai đoạn 2.");
    }
}
