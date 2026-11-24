package vn.vinabacus.service;

import vn.vinabacus.service.*;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.repository.HocVienRepository;
import vn.vinabacus.repository.TV2CoSoRepository;

public final class HocVienService {

    // Mã cơ sở hiếm khi đổi, nên cache luôn để khỏi truy vấn mỗi lần
    private static final Map<CoSo, Integer> MA_CO_SO = new EnumMap<>(CoSo.class);
    private static final Object KHOA = new Object();

    private final TV2CoSoRepository coSoRepository = new TV2CoSoRepository();
    private final HocVienRepository repository = new HocVienRepository();

    public int maCoSo(CoSo coSo) throws SQLException {
        synchronized (KHOA) {
            Integer ma = MA_CO_SO.get(coSo);
            if (ma == null) {
                ma = coSoRepository.layMaCoSo(coSo);
                MA_CO_SO.put(coSo, ma);
            }
            return ma;
        }
    }

    public List<HocVien> timKiem(CoSo coSo, String tuKhoa, String trangThai) throws SQLException {
        return repository.timKiem(maCoSo(coSo), tuKhoa == null ? "" : tuKhoa, trangThai);
    }

    public int them(CoSo coSo, HocVien h) throws SQLException {
        int ma = maCoSo(coSo);
        kiemTra(h);
        return repository.them(saoChep(h, 0, ma));
    }

    public void sua(CoSo coSo, HocVien h) throws SQLException {
        int ma = maCoSo(coSo);
        kiemTra(h);
        if (!repository.sua(saoChep(h, h.maHocVien(), ma))) {
            throw new SQLException("Không sửa được học viên thuộc cơ sở này");
        }
    }

    public void xoa(CoSo coSo, int maHocVien) throws SQLException {
        repository.xoa(maCoSo(coSo), maHocVien);
    }

    /** Xóa cache của cơ sở, dùng cho nút "Tải lại" hoặc khi dữ liệu bị đổi từ nơi khác. */
    public void lamMoi(CoSo coSo) throws SQLException {
        HocVienRepository.xoaCache(maCoSo(coSo));
    }

    private void kiemTra(HocVien h) {
        if (h.hoTen() == null || h.hoTen().isBlank()) {
            throw new IllegalArgumentException("Bạn chưa nhập họ tên học viên");
        }
        if (h.gioiTinh() != null && !List.of("NAM", "NU", "KHAC").contains(h.gioiTinh())) {
            throw new IllegalArgumentException("Giới tính không hợp lệ");
        }
        if (!List.of("HOAT_DONG", "NGUNG_SU_DUNG").contains(h.trangThaiHoSo())) {
            throw new IllegalArgumentException("Trạng thái hồ sơ không hợp lệ");
        }
    }

    private static HocVien saoChep(HocVien h, int ma, int maCoSo) {
        return new HocVien(ma, maCoSo, h.hoTen().trim(), h.ngaySinh(), h.gioiTinh(),
                h.tenNguoiLienHe(), h.soDienThoaiLienHe(), h.diaChi(), h.ghiChu(), h.trangThaiHoSo());
    }
    public List<HocVienRepository.LopDangHoc> layLopDangHoc(CoSo coSo, int maHocVien) throws SQLException {
    if (coSo != CoSo.TRUNG_TAM) return List.of();
    return repository.layLopDangHoc(maHocVien);
}
    
}