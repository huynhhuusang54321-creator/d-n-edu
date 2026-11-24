package vn.vinabacus.service;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.repository.TV2CoSoRepository;
import vn.vinabacus.repository.TuyenSinhRepository;

/**
 * Service quản lý nghiệp vụ Tuyển sinh: xác định mã cơ sở, kiểm tra dữ liệu và điều phối Repository.
 */
public final class TuyenSinhService {

    private static final Set<String> TRANG_THAI_HIEU_LUC = Set.of(
            "TIEP_NHAN", "HOC_THU", "CHO_XAC_NHAN", "CHO_XEP_LOP", "KHONG_DANG_KY");

    private static final Map<CoSo, Integer> MA_CO_SO = new EnumMap<>(CoSo.class);
    private static final Object KHOA = new Object();

    private final TV2CoSoRepository coSoRepository = new TV2CoSoRepository();
    private final TuyenSinhRepository repository = new TuyenSinhRepository();

    public int maCoSo(CoSo coSo) throws SQLException {
        if (coSo == null) {
            throw new IllegalArgumentException("Cơ sở không được để trống");
        }
        synchronized (KHOA) {
            Integer ma = MA_CO_SO.get(coSo);
            if (ma == null) {
                ma = coSoRepository.layMaCoSo(coSo);
                if (ma == null || ma <= 0) {
                    throw new SQLException("Không xác định được mã cơ sở");
                }
                MA_CO_SO.put(coSo, ma);
            }
            return ma;
        }
    }

    public List<KhachTiemNang> timKiem(CoSo coSo, String tuKhoa, String trangThai) throws SQLException {
        int ma = maCoSo(coSo);
        String tk = tuKhoa == null ? "" : tuKhoa.trim();
        String tt = trangThai == null || trangThai.isBlank() ? "TAT_CA" : trangThai.trim();
        return repository.timKiem(ma, tk, tt);
    }

    public int them(CoSo coSo, KhachTiemNang khach) throws SQLException {
        if (khach == null) {
            throw new IllegalArgumentException("Thông tin hồ sơ không được để trống");
        }
        int ma = maCoSo(coSo);
        kiemTra(khach);
        return repository.them(saoChep(khach, 0, ma));
    }

    public void sua(CoSo coSo, KhachTiemNang khach) throws SQLException {
        if (khach == null) {
            throw new IllegalArgumentException("Thông tin hồ sơ không được để trống");
        }
        if (khach.maKhach() <= 0) {
            throw new IllegalArgumentException("Mã hồ sơ tuyển sinh không hợp lệ");
        }
        int ma = maCoSo(coSo);
        kiemTra(khach);
        if (!repository.sua(saoChep(khach, khach.maKhach(), ma))) {
            throw new SQLException("Không sửa được hồ sơ tuyển sinh. Hồ sơ có thể không tồn tại, "
                    + "không thuộc cơ sở này hoặc đã chuyển đổi thành học viên.");
        }
    }

    public int chuyenDoi(CoSo coSo, int maKhach) throws SQLException {
        if (maKhach <= 0) {
            throw new IllegalArgumentException("Mã hồ sơ tuyển sinh không hợp lệ");
        }
        return repository.chuyenDoi(maKhach, maCoSo(coSo));
    }

    public void lamMoi(CoSo coSo) throws SQLException {
        TuyenSinhRepository.xoaCache(maCoSo(coSo));
    }

    private static void kiemTra(KhachTiemNang khach) {
        if (khach.hoTen() == null || khach.hoTen().isBlank()) {
            throw new IllegalArgumentException("Bạn chưa nhập họ tên khách");
        }
        if (khach.trangThai() == null || !TRANG_THAI_HIEU_LUC.contains(khach.trangThai())) {
            throw new IllegalArgumentException("Trạng thái tuyển sinh không hợp lệ");
        }
        if (khach.maKhach() < 0) {
            throw new IllegalArgumentException("Mã hồ sơ tuyển sinh không hợp lệ");
        }
    }

    private static KhachTiemNang saoChep(KhachTiemNang khach, int maKhach, int maCoSo) {
        return new KhachTiemNang(
                maKhach,
                maCoSo,
                khach.hoTen().trim(),
                khach.soDienThoai(),
                khach.tenNguoiLienHe(),
                khach.nhuCau(),
                khach.trangThai(),
                khach.daHocThu(),
                khach.ghiChu());
    }
}