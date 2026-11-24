package vn.vinabacus.test;

import java.math.BigDecimal;
import java.util.List;
import vn.vinabacus.repository.TaiChinhDangKyRepository;
import vn.vinabacus.repository.TaiChinhDangKyRepository.PhanBo;

/** Kiểm thử quy tắc phân bổ, không kết nối hoặc ghi CSDL. */
public final class KiemThuPhanBoTaiChinh {
    private KiemThuPhanBoTaiChinh() { }

    public static void main(String[] args) {
        List<PhanBo> result = TaiChinhDangKyRepository.kiemTraPhanBo(
                new BigDecimal("300.00"), List.of(
                        new PhanBo(20, new BigDecimal("125")),
                        new PhanBo(10, new BigDecimal("175.00"))));
        check(result.size() == 2 && result.get(0).maKhoanThu() == 10
                && result.get(0).soTien().equals(new BigDecimal("175.00"))
                && result.get(1).maKhoanThu() == 20,
                "Phân bổ đúng tổng phải được chuẩn hóa và sắp xếp theo mã khoản");

        fail(() -> TaiChinhDangKyRepository.kiemTraPhanBo(new BigDecimal("300"),
                List.of(new PhanBo(10, new BigDecimal("299")))), "Thiếu phân bổ");
        fail(() -> TaiChinhDangKyRepository.kiemTraPhanBo(new BigDecimal("300"),
                List.of(new PhanBo(10, new BigDecimal("301")))), "Phân bổ vượt tổng");
        fail(() -> TaiChinhDangKyRepository.kiemTraPhanBo(new BigDecimal("300"),
                List.of(new PhanBo(10, new BigDecimal("150")),
                        new PhanBo(10, new BigDecimal("150")))), "Trùng khoản");
        fail(() -> TaiChinhDangKyRepository.kiemTraPhanBo(new BigDecimal("300"),
                List.of(new PhanBo(10, new BigDecimal("300.001")))), "Quá hai số thập phân");
        fail(() -> TaiChinhDangKyRepository.kiemTraPhanBo(BigDecimal.ZERO,
                List.of(new PhanBo(10, BigDecimal.ZERO))), "Thu bằng không");
        fail(() -> TaiChinhDangKyRepository.kiemTraPhanBo(
                new BigDecimal("10000000000000000.00"),
                List.of(new PhanBo(10, new BigDecimal("10000000000000000.00")))),
                "Vượt DECIMAL(18,2)");
        System.out.println("PASS: 7 tình huống phân bổ tài chính");
    }

    private static void fail(Runnable action, String name) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Phải từ chối: " + name);
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
