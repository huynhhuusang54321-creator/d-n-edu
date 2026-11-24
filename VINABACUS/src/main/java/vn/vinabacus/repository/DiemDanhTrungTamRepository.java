package vn.vinabacus.repository;


import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.LopTrungTam;

/** Doc danh sach tu SQL co san; chi GHI diem danh ra file rieng tren may. */
public final class DiemDanhTrungTamRepository {
    public record LuaChon(int id, String ten) {
        @Override public String toString() { return ten; }
    }
    public record HocVien(int id, String ten, String trangThai, String ghiChu) { }
    public record LichSu(LocalDate ngay, String tenHocVien, String tenLop,
                          String trangThai, String ghiChu, String taiKhoan, String capNhat) { }
    public record NhatKy(String ngaySua, String tenHocVien, String tenLop,
                         String truoc, String sau, String lyDo, String taiKhoan) { }

    private static final Object LOCK = new Object();
    private static final Set<String> HOP_LE = Set.of(
        "CHUA_DIEM_DANH", "CO_MAT", "VANG_CO_PHEP", "VANG_KHONG_PHEP");
    private static final Path FILE = Paths.get(
        System.getProperty("user.home"), "VINABACUS", "diem-danh-trung-tam.properties");

    public List<LuaChon> lop(int coSo) throws SQLException {
        List<LuaChon> ds = new ArrayList<>();
        for (LopTrungTam l : new LopTrungTamRepository().layDanhSach(coSo)) {
            ds.add(new LuaChon(l.maLop(), l.tenLop() + " – " + l.tenKhoaHoc()));
        }
        return ds;
    }

    public List<LuaChon> taiKhoan(int coSo) throws SQLException {
        String sql = """
            SELECT t.MaTaiKhoan, t.TenDangNhap
            FROM dbo.TaiKhoan t
            JOIN dbo.TaiKhoanCoSo cs ON cs.MaTaiKhoan=t.MaTaiKhoan
            WHERE cs.MaCoSo=? AND cs.TrangThai=1 AND t.TrangThai=1
            ORDER BY t.TenDangNhap
            """;
        List<LuaChon> ds = new ArrayList<>();
        try (Connection c = Database.moKetNoi();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, coSo);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) ds.add(new LuaChon(rs.getInt(1), rs.getString(2)));
            }
        }
        return ds;
    }

    private List<HocVien> hocVienTrongLop(int coSo, int lop, LocalDate ngay)
            throws SQLException {
        String sql = """
            SELECT DISTINCT h.MaHocVien, h.HoTen
            FROM dbo.XepLopTrungTam x
            JOIN dbo.DangKyKhoaHoc dk ON dk.MaDangKy=x.MaDangKy
               AND dk.MaCoSo=x.MaCoSo
            JOIN dbo.HocVien h ON h.MaHocVien=dk.MaHocVien
               AND h.MaCoSo=dk.MaCoSo
            WHERE x.MaCoSo=? AND x.MaLop=?
              AND x.TrangThai='HIEU_LUC'
              AND dk.TrangThai<>'HUY'
              AND x.TuNgay<=?
            ORDER BY h.HoTen, h.MaHocVien
            """;
        List<HocVien> ds = new ArrayList<>();
        try (Connection c = Database.moKetNoi();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, coSo);
            p.setInt(2, lop);
            p.setDate(3, java.sql.Date.valueOf(ngay));
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) ds.add(new HocVien(rs.getInt(1), rs.getString(2),
                        "CHUA_DIEM_DANH", ""));
            }
        }
        return ds;
    }

    public List<HocVien> bang(int coSo, int lop, LocalDate ngay) throws Exception {
        List<HocVien> ds = hocVienTrongLop(coSo, lop, ngay);
        synchronized (LOCK) {
            Properties p = doc();
            List<HocVien> ketQua = new ArrayList<>();
            for (HocVien hv : ds) {
                String raw = p.getProperty(khoa(coSo, lop, hv.id(), ngay));
                if (raw == null) { ketQua.add(hv); continue; }
                String[] t = raw.split("\\|", -1);
                if (t.length != 7) throw new IOException("Dữ liệu điểm danh bị lỗi định dạng.");
                ketQua.add(new HocVien(hv.id(), hv.ten(), t[2], giaiMa(t[3])));
            }
            return ketQua;
        }
    }

    public int luu(int coSo, int lop, LocalDate ngay, LuaChon account,
                   String tenLop, String lyDo, List<HocVien> rows) throws Exception {
        // Luon kiem tra danh sach hoc vien / tai khoan tu CSDL truoc khi ghi file.
        boolean tkHopLe = taiKhoan(coSo).stream()
                .anyMatch(a -> a.id() == account.id() && a.ten().equals(account.ten()));
        if (!tkHopLe) throw new IllegalArgumentException("Tài khoản không hợp lệ tại Trung tâm.");
        boolean lopHopLe = lop(coSo).stream().anyMatch(l -> l.id() == lop);
        if (!lopHopLe) throw new IllegalArgumentException("Lớp không tồn tại tại Trung tâm.");
        Map<Integer, String> danhSach = new HashMap<>();
        for (HocVien hv : hocVienTrongLop(coSo, lop, ngay)) danhSach.put(hv.id(), hv.ten());
        if (rows.isEmpty()) throw new IllegalArgumentException("Lớp không có học viên để điểm danh.");
        Set<Integer> daGap = new HashSet<>();
        for (HocVien hv : rows) {
            if (!daGap.add(hv.id()) || !danhSach.containsKey(hv.id()))
                throw new IllegalArgumentException("Học viên không thuộc lớp hoặc bị trùng: " + hv.id());
            if (!HOP_LE.contains(hv.trangThai()))
                throw new IllegalArgumentException("Trạng thái không hợp lệ.");
            if (hv.ghiChu() != null && hv.ghiChu().length() > 500)
                throw new IllegalArgumentException("Ghi chú không được quá 500 ký tự.");
        }
        if (lyDo != null && lyDo.length() > 500)
            throw new IllegalArgumentException("Lý do sửa không được quá 500 ký tự.");

        synchronized (LOCK) {
            Properties p = doc();
            int thayDoi = 0;
            for (HocVien hv : rows) {
                String key = khoa(coSo, lop, hv.id(), ngay);
                String cu = p.getProperty(key);
                String ghi = hv.ghiChu() == null ? "" : hv.ghiChu().trim();
                if (cu == null && "CHUA_DIEM_DANH".equals(hv.trangThai()) && ghi.isEmpty())
                    continue;
                String trangThaiCu = "CHUA_DIEM_DANH", ghiChuCu = "";
                if (cu != null) {
                    String[] a = cu.split("\\|", -1);
                    if (a.length != 7) throw new IOException("Bản ghi cũ không hợp lệ.");
                    trangThaiCu = a[2];
                    ghiChuCu = giaiMa(a[3]);
                    if (Objects.equals(trangThaiCu, hv.trangThai())
                            && Objects.equals(ghiChuCu, ghi)) continue;
                    if (lyDo == null || lyDo.isBlank())
                        throw new IllegalArgumentException("Phải nhập lý do khi sửa điểm danh đã lưu.");
                }
                String capNhat = Instant.now().toString();
                String hoTen = danhSach.get(hv.id());
                p.setProperty(key, String.join("|",
                    maHoa(hoTen), maHoa(tenLop), hv.trangThai(), maHoa(ghi),
                    Integer.toString(account.id()), maHoa(account.ten()), capNhat));
                if (cu != null) {
                    p.setProperty("H." + UUID.randomUUID(), String.join("|",
                        Integer.toString(coSo), Integer.toString(lop),
                        Integer.toString(hv.id()), ngay.toString(), maHoa(hoTen),
                        maHoa(tenLop), trangThaiCu, hv.trangThai(),
                        maHoa(ghiChuCu), maHoa(ghi), maHoa(lyDo.trim()),
                        maHoa(account.ten()), capNhat));
                }
                thayDoi++;
            }
            if (thayDoi > 0) ghi(p);
            return thayDoi;
        }
    }

    public List<LichSu> lichSu(int coSo, int lop, LocalDate tu, LocalDate den)
            throws Exception {
        List<LichSu> list = new ArrayList<>();
        synchronized (LOCK) {
            Properties p = doc();
            for (String key : p.stringPropertyNames()) {
                String[] k = key.split("\\.", -1);
                if (k.length != 5 || !k[0].equals("D")) continue;
                if (Integer.parseInt(k[1]) != coSo || Integer.parseInt(k[2]) != lop) continue;
                LocalDate ngay = LocalDate.parse(k[4]);
                if (ngay.isBefore(tu) || ngay.isAfter(den)) continue;
                String[] v = p.getProperty(key).split("\\|", -1);
                if (v.length != 7) throw new IOException("Lịch sử điểm danh không hợp lệ.");
                list.add(new LichSu(ngay, giaiMa(v[0]), giaiMa(v[1]),
                        v[2], giaiMa(v[3]), giaiMa(v[5]), v[6]));
            }
        }
        list.sort(Comparator.comparing(LichSu::ngay).reversed()
                .thenComparing(LichSu::tenHocVien));
        return list;
    }

    public List<NhatKy> nhatKy(int coSo, int lop, LocalDate tu, LocalDate den)
            throws Exception {
        List<NhatKy> list = new ArrayList<>();
        synchronized (LOCK) {
            Properties p = doc();
            for (String key : p.stringPropertyNames()) {
                if (!key.startsWith("H.")) continue;
                String[] v = p.getProperty(key).split("\\|", -1);
                if (v.length != 13) throw new IOException("Nhật ký sửa không hợp lệ.");
                if (Integer.parseInt(v[0]) != coSo || Integer.parseInt(v[1]) != lop) continue;
                LocalDate ngay = LocalDate.parse(v[3]);
                if (ngay.isBefore(tu) || ngay.isAfter(den)) continue;
                list.add(new NhatKy(v[12], giaiMa(v[4]), giaiMa(v[5]),
                        v[6], v[7], giaiMa(v[10]), giaiMa(v[11])));
            }
        }
        list.sort(Comparator.comparing(NhatKy::ngaySua).reversed());
        return list;
    }

    private static String khoa(int coSo, int lop, int hv, LocalDate ngay) {
        return "D." + coSo + "." + lop + "." + hv + "." + ngay;
    }
    private static String maHoa(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
    }
    private static String giaiMa(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }
    private static Properties doc() throws IOException {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        }
        return p;
    }
    private static void ghi(Properties p) throws IOException {
        Files.createDirectories(FILE.getParent());
        Path tam = Files.createTempFile(FILE.getParent(), "diem-danh-", ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(tam, StandardCharsets.UTF_8)) {
                p.store(w, "VINABACUS - Diem danh Trung tam (local)");
            }
            try {
                Files.move(tam, FILE, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tam, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tam);
        }
    }
}
