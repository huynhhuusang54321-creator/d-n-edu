package vn.vinabacus.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phân bổ theo tỷ lệ phải thu từng khóa, chia phần lẻ theo dư số lớn nhất. */
public final class PhanBoThanhToanService {
    private PhanBoThanhToanService() { }
    private record PhanLe(int maKhoa, BigDecimal du) { }
    private static final BigDecimal MOT_XU = new BigDecimal("0.01");

    public static Map<Integer, BigDecimal> tinh(Map<Integer, BigDecimal> phaiThu,
            BigDecimal tienThu) {
        if (phaiThu == null || phaiThu.isEmpty() || tienThu == null
                || tienThu.signum() < 0 || tienThu.scale() > 2)
            throw new IllegalArgumentException("Khoản phải thu hoặc số tiền thực thu không hợp lệ");
        BigDecimal tong = BigDecimal.ZERO;
        List<Integer> ids = new ArrayList<>(phaiThu.keySet());
        ids.sort(Integer::compareTo);
        for (int id : ids) {
            BigDecimal no = phaiThu.get(id);
            if (id <= 0 || no == null || no.signum() < 0 || no.scale() > 2)
                throw new IllegalArgumentException("Khoản phải thu theo khóa không hợp lệ");
            tong = tong.add(no);
        }
        if (tienThu.compareTo(tong) > 0)
            throw new IllegalArgumentException("Số tiền thực thu vượt tổng phải thu");
        Map<Integer, BigDecimal> out = new LinkedHashMap<>();
        List<PhanLe> du = new ArrayList<>();
        BigDecimal daChia = BigDecimal.ZERO;
        for (int id : ids) {
            BigDecimal no = phaiThu.get(id);
            BigDecimal chinhXac = tong.signum() == 0 ? BigDecimal.ZERO
                    : tienThu.multiply(no).divide(tong, 8, RoundingMode.HALF_UP);
            BigDecimal so = chinhXac.setScale(2, RoundingMode.DOWN);
            out.put(id, so); daChia = daChia.add(so);
            du.add(new PhanLe(id, chinhXac.subtract(so)));
        }
        du.sort(Comparator.comparing(PhanLe::du).reversed()
                .thenComparingInt(PhanLe::maKhoa));
        BigDecimal con = tienThu.subtract(daChia);
        for (PhanLe item : du) {
            if (con.signum() == 0) break;
            int id = item.maKhoa();
            if (out.get(id).add(MOT_XU).compareTo(phaiThu.get(id)) <= 0) {
                out.put(id, out.get(id).add(MOT_XU));
                con = con.subtract(MOT_XU);
            }
        }
        if (con.signum() != 0) throw new IllegalStateException("Không phân bổ hết tiền thực thu");
        return Map.copyOf(out);
    }
}
