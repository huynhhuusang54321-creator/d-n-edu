package vn.vinabacus.service;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.TaiChinhDTO;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.DangKyKhoaHocRepository;
import vn.vinabacus.repository.ChuongTrinhGiamGiaRepository;
import vn.vinabacus.repository.TaiChinhRepository;
import vn.vinabacus.repository.HocVienRepository;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.LopTrungTamRepository;
import vn.vinabacus.repository.MonHocRepository;
import vn.vinabacus.repository.TV2CoSoRepository;
import vn.vinabacus.repository.TuyenSinhRepository;

/** Điều phối đăng ký và xếp lớp Trung tâm; tiền thu thuộc giai đoạn sau. */
public final class DangKyKhoaHocService {
    private final TV2CoSoRepository coSoRepo = new TV2CoSoRepository();
    private final DangKyKhoaHocRepository repo = new DangKyKhoaHocRepository();
    private final KhoaHocRepository khoaRepo = new KhoaHocRepository();
    private final MonHocRepository monRepo = new MonHocRepository();
    private final LopTrungTamRepository lopRepo = new LopTrungTamRepository();
    private final TuyenSinhRepository tuyenSinhRepo = new TuyenSinhRepository();
    private final HocVienRepository hocVienRepo = new HocVienRepository();
    private final ChuongTrinhGiamGiaRepository giamGiaRepo = new ChuongTrinhGiamGiaRepository();

    private int maCoSo() throws SQLException {
        return coSoRepo.layMaCoSo(CoSo.TRUNG_TAM);
    }

    public List<DangKyKhoaHoc> timKiem(String tuKhoa, String trangThai) throws SQLException {
        String q = boDau(tuKhoa == null ? "" : tuKhoa.trim());
        List<DangKyKhoaHoc> ketQua = new ArrayList<>();
        for (DangKyKhoaHoc d : repo.layDanhSach(maCoSo())) {
            if (trangThai != null && !trangThai.equals(d.trangThai())) continue;
            if (!q.isEmpty() && !boDau(d.hoTen()).contains(q)
                    && !boDau(d.tenMonHoc()).contains(q)
                    && !boDau(d.tenKhoaHoc()).contains(q)
                    && !String.valueOf(d.maDangKy()).contains(q)
                    && !String.valueOf(d.maHocVien()).contains(q)) continue;
            ketQua.add(d);
        }
        return ketQua;
    }

    public List<KhoaHoc> khoaHoc() throws SQLException {
        int coSo = maCoSo();
        Set<Integer> monHoatDong = new HashSet<>();
        for (MonHoc m : monRepo.layDanhSach(coSo)) {
            if (m.trangThai()) monHoatDong.add(m.maMonHoc());
        }
        return khoaRepo.layDanhSach(coSo).stream()
                .filter(k -> k.trangThai() && monHoatDong.contains(k.maMonHoc()))
                .toList();
    }

    public boolean giamGiaSanSang() throws SQLException { return giamGiaRepo.sanSang(); }
    public List<ChuongTrinhGiamGia> chuongTrinhGiamGia() throws SQLException {
        return giamGiaRepo.sanSang() ? giamGiaRepo.layDanhSach(maCoSo()) : List.of();
    }
    public List<TaiChinhDTO.LuaChon> taiKhoanThu() throws SQLException {
        return new TaiChinhRepository().taiKhoan(CoSo.TRUNG_TAM);
    }

    public List<LopTrungTam> lopHoc() throws SQLException {
        Set<Integer> khoaHoatDong = new HashSet<>();
        for (KhoaHoc k : khoaHoc()) khoaHoatDong.add(k.maKhoaHoc());
        Set<Integer> lopNhan = new HashSet<>();
        for (DangKyKhoaHoc.ThongTinLop l : thongTinLop()) {
            if ("Đang nhận".equals(l.trangThaiNhan())) lopNhan.add(l.maLop());
        }
        return lopRepo.layDanhSach(maCoSo()).stream()
                .filter(l -> "DANG_MO".equals(l.trangThai())
                        && khoaHoatDong.contains(l.maKhoaHoc())
                        && lopNhan.contains(l.maLop()))
                .toList();
    }

    public List<DangKyKhoaHoc.ThongTinLop> thongTinLop() throws SQLException {
        return repo.thongTinLop(maCoSo());
    }

    public List<HocVien> hocVien() throws SQLException {
        int coSo = maCoSo();
        Set<Integer> maTam = repo.layMaHocVienTam(coSo);
        return hocVienRepo.timKiem(coSo, "", "TAT_CA").stream()
                .filter(h -> "HOAT_DONG".equals(h.trangThaiHoSo())
                        && !maTam.contains(h.maHocVien()))
                .toList();
    }

    public List<KhachTiemNang> hoSoTuyenSinh() throws SQLException {
        return tuyenSinhRepo.timKiem(maCoSo(), "", "TAT_CA").stream()
                .filter(k -> !"KHONG_DANG_KY".equals(k.trangThai()))
                .toList();
    }

    public DangKyKhoaHoc.KetQua dangKyTuHoSo(int maKhach, LocalDate ngayBatDau,
                                               List<DangKyKhoaHoc.LuaChon> luaChon)
            throws SQLException {
        kiemTraDangKy(maKhach, ngayBatDau, luaChon);
        return repo.dangKyTuHoSo(maCoSo(), maKhach, ngayBatDau, luaChon);
    }

    public DangKyKhoaHoc.KetQua dangKyTuHoSo(int maKhach, LocalDate ngayBatDau,
            List<DangKyKhoaHoc.LuaChon> luaChon, DangKyKhoaHoc.TaiChinh taiChinh)
            throws SQLException {
        kiemTraDangKy(maKhach, ngayBatDau, luaChon);
        kiemTraTaiChinh(taiChinh);
        return repo.dangKyTuHoSo(maCoSo(), maKhach, ngayBatDau, luaChon, taiChinh);
    }

    public DangKyKhoaHoc.KetQua dangKyChoHocVien(int maHocVien,
            LocalDate ngayBatDau, List<DangKyKhoaHoc.LuaChon> luaChon)
            throws SQLException {
        kiemTraDangKy(maHocVien, ngayBatDau, luaChon);
        return repo.dangKyChoHocVien(maCoSo(), maHocVien, ngayBatDau, luaChon);
    }

    public DangKyKhoaHoc.KetQua dangKyChoHocVien(int maHocVien, LocalDate ngayBatDau,
            List<DangKyKhoaHoc.LuaChon> luaChon, DangKyKhoaHoc.TaiChinh taiChinh)
            throws SQLException {
        kiemTraDangKy(maHocVien, ngayBatDau, luaChon);
        kiemTraTaiChinh(taiChinh);
        return repo.dangKyChoHocVien(maCoSo(), maHocVien, ngayBatDau, luaChon, taiChinh);
    }

    public static void kiemTraTaiChinh(DangKyKhoaHoc.TaiChinh t) {
        if (t == null || t.phaiThuDuKien() == null || t.phaiThuDuKien().signum() < 0
                || t.soTienThu() == null || t.soTienThu().signum() < 0
                || t.soTienThu().compareTo(t.phaiThuDuKien()) > 0
                || t.soTienThu().scale() > 2 || t.maGiaoDich() == null)
            throw new IllegalArgumentException("Thông tin học phí hoặc thanh toán không hợp lệ");
        if (t.soTienThu().signum() > 0 && (t.maTaiKhoan() <= 0
                || !List.of("TIEN_MAT", "CHUYEN_KHOAN", "KHAC").contains(t.hinhThuc())))
            throw new IllegalArgumentException("Hãy chọn tài khoản và phương thức thu hợp lệ");
        if (t.ghiChu() != null && t.ghiChu().length() > 500)
            throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự");
        if (t.maChuongTrinh().stream().anyMatch(id -> id == null || id <= 0)
                || t.maChuongTrinh().size() != new HashSet<>(t.maChuongTrinh()).size())
            throw new IllegalArgumentException("Chương trình giảm giá bị chọn trùng hoặc không hợp lệ");
    }

    public int chuyenThanhHocVien(int maKhach) throws SQLException {
        return repo.chuyenThanhHocVien(maCoSo(), maKhach);
    }

    public void xepLop(int maDangKy, int maLop, LocalDate ngayXep) throws SQLException {
        if (maDangKy <= 0 || maLop <= 0 || ngayXep == null)
            throw new IllegalArgumentException("Hãy chọn đăng ký, lớp và ngày xếp lớp hợp lệ");
        repo.xepLop(maCoSo(), maDangKy, maLop, ngayXep);
    }

    public static void kiemTraDangKy(int maKhach, LocalDate ngayBatDau,
                                       List<DangKyKhoaHoc.LuaChon> luaChon) {
        if (maKhach <= 0) throw new IllegalArgumentException("Hãy chọn hồ sơ tuyển sinh");
        if (ngayBatDau == null) throw new IllegalArgumentException("Hãy chọn ngày bắt đầu dự kiến");
        if (luaChon == null || luaChon.isEmpty())
            throw new IllegalArgumentException("Hãy chọn ít nhất một khóa học");
        Set<Integer> daChon = new HashSet<>();
        for (DangKyKhoaHoc.LuaChon chon : luaChon) {
            if (chon == null || chon.maKhoaHoc() <= 0
                    || chon.maLop() != null && chon.maLop() <= 0)
                throw new IllegalArgumentException("Khóa học hoặc lớp học không hợp lệ");
            if (!daChon.add(chon.maKhoaHoc()))
                throw new IllegalArgumentException("Một khóa học chỉ được chọn một lần");
        }
    }

    private static String boDau(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }
}
