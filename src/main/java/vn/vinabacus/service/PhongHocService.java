package vn.vinabacus.service;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.LichHocLopRepository;
import vn.vinabacus.repository.MonHocRepository;
import vn.vinabacus.repository.PhongHocRepository;

/** Quan ly danh muc phong hoc Trung tam; khong can thay doi cau truc SQL. */
public final class PhongHocService {
    private static final Pattern TEN_CO_MA = Pattern.compile(
            "^(?:\\[MẪU\\]\\s*)?([A-Za-z][A-Za-z0-9_-]{1,19})\\s+-\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MA_HOP_LE = Pattern.compile("[A-Z][A-Z0-9_-]{1,19}");
    public static final String HOAT_DONG = "HOAT_DONG";
    public static final String BAO_TRI = "BAO_TRI";
    public static final String NGUNG_SU_DUNG = "NGUNG_SU_DUNG";

    private final PhongHocRepository repository;
    private final MonHocRepository monRepository;

    public PhongHocService() {
        this(new PhongHocRepository(), new MonHocRepository());
    }

    public PhongHocService(PhongHocRepository repository, MonHocRepository monRepository) {
        this.repository = repository;
        this.monRepository = monRepository;
    }

    private int maCoSo() throws SQLException {
        return monRepository.layMaCoSo(CoSo.TRUNG_TAM);
    }

    public List<PhongHoc> layDanhSach() throws SQLException {
        return repository.layDanhSach(maCoSo());
    }

    public List<PhongHocRepository.LopSuDung> layLopSuDung() throws SQLException {
        return repository.layLopSuDung(maCoSo());
    }

    public List<LichHocLop> layLichSuDung() throws SQLException {
        return new LichHocLopRepository().layDanhSach(maCoSo());
    }

    public static long soLopDangSuDung(List<PhongHocRepository.LopSuDung> ds, int maPhong) {
        return ds.stream().filter(l -> l.maPhong() == maPhong && l.dangSuDung()).count();
    }

    public int them(String ma, String ten, int sucChua, String trangThai, String ghiChu)
            throws SQLException {
        kiemTra(null, ma, ten, sucChua, trangThai, ghiChu);
        return repository.them(maCoSo(), ma.trim().toUpperCase(Locale.ROOT), ten.trim(),
                sucChua, trangThai, chuanGhiChu(ghiChu));
    }

    public void sua(PhongHoc cu, String ma, String ten, int sucChua,
                    String trangThai, String ghiChu) throws SQLException {
        if (cu == null || cu.maPhong() <= 0) throw new IllegalArgumentException("Chưa chọn phòng để sửa");
        kiemTra(cu, ma, ten, sucChua, trangThai, ghiChu);
        repository.sua(maCoSo(), cu.maPhong(), ma.trim().toUpperCase(Locale.ROOT),
                ten.trim(), sucChua, trangThai, chuanGhiChu(ghiChu));
    }

    public void doiTrangThai(PhongHoc phong, String trangThai) throws SQLException {
        if (phong == null) throw new IllegalArgumentException("Chưa chọn phòng học");
        if (!List.of(HOAT_DONG, BAO_TRI, NGUNG_SU_DUNG).contains(trangThai))
            throw new IllegalArgumentException("Trạng thái phòng không hợp lệ");
        repository.doiTrangThai(maCoSo(), phong.maPhong(), trangThai);
    }

    public static void kiemTra(PhongHoc cu, String ma, String ten, int sucChua,
                               String trangThai, String ghiChu) {
        if (ma == null || ma.isBlank()) throw new IllegalArgumentException("Hãy nhập mã phòng");
        String chuan = ma.trim().toUpperCase(Locale.ROOT);
        if (!MA_HOP_LE.matcher(chuan).matches())
            throw new IllegalArgumentException("Mã phòng gồm 2–20 ký tự A–Z, 0–9, _ hoặc -, bắt đầu bằng chữ");
        if (cu != null && !maHienThi(cu).equalsIgnoreCase(chuan))
            throw new IllegalArgumentException("Mã phòng không được thay đổi sau khi tạo");
        if (ten == null || ten.isBlank()) throw new IllegalArgumentException("Hãy nhập tên phòng");
        if (ten.trim().length() + chuan.length() + 3 > 100)
            throw new IllegalArgumentException("Mã và tên phòng không được vượt quá 100 ký tự");
        if (sucChua <= 0) throw new IllegalArgumentException("Sức chứa phải lớn hơn 0");
        if (!List.of(HOAT_DONG, BAO_TRI, NGUNG_SU_DUNG).contains(trangThai))
            throw new IllegalArgumentException("Trạng thái phòng không hợp lệ");
        if (ghiChu != null && ghiChu.trim().length() > 500)
            throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự");
    }

    public static String tenLuu(String ma, String ten) {
        return ma.trim().toUpperCase(Locale.ROOT) + " - " + ten.trim();
    }

    /** Ma hien thi la ma duoc nhap trong TenPhong, khong phai PK MaPhong tu tang. */
    public static String maHienThi(PhongHoc phong) {
        Matcher m = TEN_CO_MA.matcher(phong.tenPhong());
        return m.matches() ? m.group(1).toUpperCase(Locale.ROOT) : "ID" + phong.maPhong();
    }

    public static String tenHienThi(PhongHoc phong) {
        Matcher m = TEN_CO_MA.matcher(phong.tenPhong());
        return m.matches() ? m.group(2).trim() : phong.tenPhong();
    }

    public static String nhanTrangThai(String trangThai) {
        return switch (trangThai) {
            case HOAT_DONG -> "Sẵn sàng";
            case BAO_TRI -> "Bảo trì";
            case NGUNG_SU_DUNG -> "Ngừng sử dụng";
            default -> trangThai == null ? "" : trangThai;
        };
    }

    private static String chuanGhiChu(String ghiChu) {
        return ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
    }

    public static List<PhongHoc> loc(List<PhongHoc> duLieu, String tuKhoa, String trangThai) {
        String tim = boDau(tuKhoa == null ? "" : tuKhoa.trim());
        List<PhongHoc> ketQua = new ArrayList<>();
        for (PhongHoc p : duLieu) {
            if (trangThai != null && !trangThai.equals(p.trangThai())) continue;
            if (!tim.isBlank() && !boDau(maHienThi(p)).contains(tim)
                    && !boDau(tenHienThi(p)).contains(tim)
                    && !boDau(p.ghiChu()).contains(tim)) continue;
            ketQua.add(p);
        }
        return ketQua;
    }

    private static String boDau(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ','d').replace('Đ','D')
                .toLowerCase(Locale.ROOT);
    }
}
