package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.service.CaHocTrungTam;
import vn.vinabacus.service.DuKienKetThucLop;
import vn.vinabacus.service.QuyTacTrungLich;

/**
 * Lich hoc Trung tam. Yeu cau migration PhongHoc (MaPhong, SiSoToiDa).
 * Kiem tra lich DINH KY trong giao dich SERIALIZABLE truoc khi ghi.
 * BuoiHoc thuc te/doi phong, giao vien day thay se duoc xu ly o buoc tiep theo.
 */
public final class LichHocLopRepository {
    @FunctionalInterface public interface KetNoi {
        Connection mo() throws SQLException;
    }
    private final KetNoi ketNoi;

    public LichHocLopRepository() { this(Database::moKetNoi); }
    public LichHocLopRepository(KetNoi ketNoi) {
        this.ketNoi = Objects.requireNonNull(ketNoi, "ketNoi");
    }

    public record BuoiTrongTuan(int thu, LocalTime batDau, LocalTime ketThuc) { }
    /** Moi buoi duoc gan phong rieng, van luu atomic trong mot transaction. */
    public record BuoiCoPhong(BuoiTrongTuan buoi, Integer maPhong) { }
    public record HocVienTheoLop(int maHocVien, String hoTen, int maLop,
                                 LocalDate tuNgay, LocalDate denNgay) { }

    /** Chi lay hoc vien da xep lop; dang ky cho xep khong tao lich gia. */
    public List<HocVienTheoLop> layHocVienTheoLop(int maCoSo) throws SQLException {
        String sql = """
                SELECT DISTINCT h.MaHocVien, h.HoTen, x.MaLop, x.TuNgay, x.DenNgay
                FROM dbo.XepLopTrungTam x
                INNER JOIN dbo.DangKyKhoaHoc d
                  ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
                 AND d.MaCoSo=x.MaCoSo
                INNER JOIN dbo.HocVien h
                  ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                WHERE x.MaCoSo=? AND x.TrangThai IN ('HIEU_LUC','KET_THUC')
                ORDER BY h.HoTen, h.MaHocVien, x.MaLop, x.TuNgay
                """;
        List<HocVienTheoLop> result = new ArrayList<>();
        try (Connection con = ketNoi.mo();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date den = rs.getDate("DenNgay");
                    result.add(new HocVienTheoLop(rs.getInt("MaHocVien"),
                            rs.getString("HoTen"), rs.getInt("MaLop"),
                            rs.getDate("TuNgay").toLocalDate(),
                            den == null ? null : den.toLocalDate()));
                }
            }
        }
        return result;
    }

    /** Doc cac ngoai le da duoc luu; buoi DA_DIEN_RA thuoc diem danh GD6. */
    public List<DuKienKetThucLop.BuoiDieuChinh> layBuoiDieuChinh(int maCoSo)
            throws SQLException {
        String sql = """
                SELECT b.MaBuoiHoc, b.MaLop, b.MaLichHoc,
                       b.NgayHoc, b.SoCa, b.TrangThai,
                       COALESCE(b.MaPhong, lh.MaPhong) AS MaPhong, p.TenPhong
                FROM dbo.BuoiHoc b
                INNER JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop
                LEFT JOIN dbo.LichHocLop lh ON lh.MaLichHoc=b.MaLichHoc
                LEFT JOIN dbo.PhongHoc p ON p.MaPhong=COALESCE(b.MaPhong, lh.MaPhong)
                 AND p.MaCoSo=l.MaCoSo
                WHERE l.MaCoSo=? AND b.TrangThai IN ('DU_KIEN','HUY')
                ORDER BY b.NgayHoc, b.MaBuoiHoc
                """;
        List<DuKienKetThucLop.BuoiDieuChinh> result = new ArrayList<>();
        try (Connection con = ketNoi.mo();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Number maLich = (Number) rs.getObject("MaLichHoc");
                    Number maPhong = (Number) rs.getObject("MaPhong");
                    result.add(new DuKienKetThucLop.BuoiDieuChinh(
                            rs.getInt("MaBuoiHoc"), rs.getInt("MaLop"),
                            maLich == null ? null : maLich.intValue(),
                            rs.getDate("NgayHoc").toLocalDate(),
                            rs.getInt("SoCa"), rs.getString("TrangThai"),
                            maPhong == null ? null : maPhong.intValue(),
                            rs.getString("TenPhong")));
                }
            }
        }
        return result;
    }

    public List<LichHocLop> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT lh.MaLichHoc, lh.MaLop, k.MaKhoaHoc, m.MaMonHoc,
                       m.TenMonHoc, k.TenKhoaHoc, l.TenLop,
                       lh.ThuTrongTuan, lh.GioBatDau, lh.GioKetThuc,
                       lh.TuNgay, lh.DenNgay, lh.TrangThai,
                       lh.MaPhong, p.TenPhong
                FROM dbo.LichHocLop lh
                INNER JOIN dbo.LopTrungTam l ON l.MaLop = lh.MaLop
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc = l.MaKhoaHoc AND k.MaCoSo = l.MaCoSo
                INNER JOIN dbo.MonHoc m
                    ON m.MaMonHoc = k.MaMonHoc AND m.MaCoSo = k.MaCoSo
                LEFT JOIN dbo.PhongHoc p ON p.MaPhong = lh.MaPhong AND p.MaCoSo=l.MaCoSo
                WHERE l.MaCoSo = ?
                ORDER BY lh.TuNgay DESC, lh.MaLichHoc DESC
                """;
        List<LichHocLop> ds = new ArrayList<>();
        try (Connection con = ketNoi.mo();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time bd = rs.getTime("GioBatDau");
                    Time kt = rs.getTime("GioKetThuc");
                    Date tu = rs.getDate("TuNgay");
                    Date den = rs.getDate("DenNgay");
                    Number phong = (Number) rs.getObject("MaPhong");
                    ds.add(new LichHocLop(
                            rs.getInt("MaLichHoc"), rs.getInt("MaLop"),
                            rs.getInt("MaKhoaHoc"), rs.getInt("MaMonHoc"),
                            rs.getString("TenMonHoc"), rs.getString("TenKhoaHoc"),
                            rs.getString("TenLop"), rs.getInt("ThuTrongTuan"),
                            bd == null ? null : bd.toLocalTime(),
                            kt == null ? null : kt.toLocalTime(),
                            tu.toLocalDate(), den == null ? null : den.toLocalDate(),
                            rs.getString("TrangThai"),
                            phong == null ? null : phong.intValue(), rs.getString("TenPhong")));
                }
            }
        }
        return ds;
    }

    /** API cu: giu lai de cac module khac co the goi ma khong can sua. */
    public List<Integer> themNhieu(int maCoSo, int maLop, Integer maPhong,
                                   List<BuoiTrongTuan> cacBuoi,
                                   LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        return themNhieuTheoPhong(maCoSo, maLop, cacBuoi.stream()
                .map(b -> new BuoiCoPhong(b, maPhong)).toList(), tuNgay, denNgay, trangThai);
    }

    /** Them nhieu o lich voi phong rieng; neu mot o xung dot thi rollback TOAN BO. */
    public List<Integer> themNhieuTheoPhong(int maCoSo, int maLop,
                                            List<BuoiCoPhong> cacBuoi,
                                            LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        try (Connection con = ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                List<Integer> ids = themNhieuTheoPhong(con,maCoSo,maLop,cacBuoi,tuNgay,denNgay,trangThai);
                con.commit();return ids;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();throw ex;
            }
        }
    }

    /** Dùng chung connection khi tạo lớp và lịch trong cùng một transaction. */
    public List<Integer> themNhieuTheoPhong(Connection con,int maCoSo,int maLop,
            List<BuoiCoPhong> cacBuoi,LocalDate tuNgay,LocalDate denNgay,String trangThai)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.LichHocLop
                    (MaLop, MaPhong, ThuTrongTuan, GioBatDau, GioKetThuc,
                     TuNgay, DenNgay, TrangThai)
                SELECT l.MaLop, ?, ?, ?, ?, ?, ?, ?
                FROM dbo.LopTrungTam l
                WHERE l.MaLop=? AND l.MaCoSo=? AND l.TrangThai<>'DA_DONG'
                """;
                List<Integer> ids = new ArrayList<>();
                for (BuoiCoPhong item : cacBuoi) {
                    BuoiTrongTuan b = item.buoi();
                    Integer maPhong = item.maPhong();
                    kiemTraKhongTrung(con, maCoSo, -1, maLop, maPhong, b.thu(),
                            b.batDau(), b.ketThuc(), tuNgay, denNgay, trangThai);
                    try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        setSo(ps, 1, maPhong);
                        ps.setInt(2, b.thu());
                        setGio(ps, 3, b.batDau());
                        setGio(ps, 4, b.ketThuc());
                        ps.setDate(5, Date.valueOf(tuNgay));
                        setNgay(ps, 6, denNgay);
                        ps.setString(7, trangThai);
                        ps.setInt(8, maLop);
                        ps.setInt(9, maCoSo);
                        if (ps.executeUpdate() != 1)
                            throw new SQLException("Không tìm thấy lớp đang sử dụng thuộc Trung tâm");
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Không nhận được mã lịch vừa thêm");
                            ids.add(keys.getInt(1));
                        }
                    }
                }
                NgayKetThucDuKienRepository.tinhLaiChoLop(con, maCoSo, maLop);
                return ids;
    }

    public int them(int maCoSo, int maLop, Integer maPhong, int thu,
                    LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        String sql = """
                INSERT INTO dbo.LichHocLop
                    (MaLop, MaPhong, ThuTrongTuan, GioBatDau, GioKetThuc,
                     TuNgay, DenNgay, TrangThai)
                SELECT l.MaLop, ?, ?, ?, ?, ?, ?, ?
                FROM dbo.LopTrungTam l
                WHERE l.MaLop=? AND l.MaCoSo=? AND l.TrangThai<>'DA_DONG'
                """;
        try (Connection con = ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                kiemTraKhongTrung(con, maCoSo, -1, maLop, maPhong, thu,
                        batDau, ketThuc, tuNgay, denNgay, trangThai);
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    setSo(ps, 1, maPhong);
                    ps.setInt(2, thu);
                    setGio(ps, 3, batDau);
                    setGio(ps, 4, ketThuc);
                    ps.setDate(5, Date.valueOf(tuNgay));
                    setNgay(ps, 6, denNgay);
                    ps.setString(7, trangThai);
                    ps.setInt(8, maLop);
                    ps.setInt(9, maCoSo);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Lớp học không thuộc cơ sở Trung tâm hoặc đã đóng");
                    int id;
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không nhận được mã lịch học vừa thêm");
                        id = keys.getInt(1);
                    }
                    NgayKetThucDuKienRepository.tinhLaiChoLop(con, maCoSo, maLop);
                    con.commit();
                    return id;
                }
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public void sua(int maCoSo, int maLichHoc, Integer maPhong, int thu,
                    LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        String sql = """
                UPDATE lh
                   SET lh.MaPhong=?, lh.ThuTrongTuan=?, lh.GioBatDau=?, lh.GioKetThuc=?,
                       lh.TuNgay=?, lh.DenNgay=?, lh.TrangThai=?
                FROM dbo.LichHocLop lh
                INNER JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                WHERE lh.MaLichHoc=? AND l.MaCoSo=?
                """;
        try (Connection con = ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                int maLop = -1;
                LocalDate tuCu = null;
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT lh.MaLop, lh.TuNgay FROM dbo.LichHocLop lh WITH (UPDLOCK, HOLDLOCK)
                        INNER JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                        WHERE lh.MaLichHoc=? AND l.MaCoSo=?
                        """)) {
                    ps.setInt(1, maLichHoc);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            maLop = rs.getInt("MaLop");
                            tuCu = rs.getDate("TuNgay").toLocalDate();
                        }
                    }
                }
                if (maLop <= 0) throw new SQLException("Không tìm thấy lịch học thuộc Trung tâm");
                if (tuCu.isBefore(LocalDate.now()))
                    throw new SQLException("Lịch gốc bắt đầu ngày " + tuCu
                            + "; Từ ngày nhận được là " + tuNgay
                            + ". Hãy chọn ngày hiệu lực từ " + LocalDate.now()
                            + " và dùng thao tác điều chỉnh từ ngày để giữ lịch sử.");
                if (tuNgay.isBefore(LocalDate.now()))
                    throw new SQLException("Không chuyển ngày bắt đầu lịch về quá khứ");
                // Moi buoi da tao deu tham chieu lich nay. Sua lich dinh ky se
                // lam sai ca/phong/ngay cua buoi DU_KIEN, ke ca chua diem danh.
                // Chi cho sua khi chua co buoi nao; buoi cu can quy trinh tach lich.
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT TOP 1 1 FROM dbo.BuoiHoc bh WITH (HOLDLOCK)
                        WHERE bh.MaLichHoc=?
                        """)) {
                    ps.setInt(1, maLichHoc);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new SQLException(
                                "Lịch đã liên kết với buổi học; không sửa lịch lặp trực tiếp. "
                                + "Hãy xử lý buổi tương lai theo quy trình điều chỉnh riêng.");
                    }
                }
                kiemTraKhongTrung(con, maCoSo, maLichHoc, maLop, maPhong, thu,
                        batDau, ketThuc, tuNgay, denNgay, trangThai);
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    setSo(ps, 1, maPhong);
                    ps.setInt(2, thu);
                    setGio(ps, 3, batDau);
                    setGio(ps, 4, ketThuc);
                    ps.setDate(5, Date.valueOf(tuNgay));
                    setNgay(ps, 6, denNgay);
                    ps.setString(7, trangThai);
                    ps.setInt(8, maLichHoc);
                    ps.setInt(9, maCoSo);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không cập nhật được lịch học");
                }
                NgayKetThucDuKienRepository.tinhLaiChoLop(con, maCoSo, maLop);
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /** Dong doan cu va them doan moi tu ngay hieu luc trong mot giao dich. */
    public int suaTuNgay(int maCoSo, int maLichHoc, Integer maPhong, int thu,
                         LocalTime batDau, LocalTime ketThuc,
                         LocalDate ngayHieuLuc, LocalDate denNgay, String trangThai)
            throws SQLException {
        if (ngayHieuLuc == null || ngayHieuLuc.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày hiệu lực không được ở trong quá khứ");
        try (Connection con = ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                int maLop;
                LocalDate tuCu;
                LocalDate denCu;
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT lh.MaLop, lh.TuNgay, lh.DenNgay, lh.TrangThai
                        FROM dbo.LichHocLop lh WITH (UPDLOCK, HOLDLOCK)
                        INNER JOIN dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
                          ON l.MaLop=lh.MaLop
                        WHERE lh.MaLichHoc=? AND l.MaCoSo=?
                        """)) {
                    ps.setInt(1, maLichHoc);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy lịch học thuộc Trung tâm");
                        if (!"HIEU_LUC".equals(rs.getString("TrangThai")))
                            throw new SQLException("Chỉ tách lịch còn hiệu lực");
                        maLop = rs.getInt("MaLop");
                        tuCu = rs.getDate("TuNgay").toLocalDate();
                        Date den = rs.getDate("DenNgay");
                        denCu = den == null ? null : den.toLocalDate();
                    }
                }
                if (!ngayHieuLuc.isAfter(tuCu)
                        || (denCu != null && ngayHieuLuc.isAfter(denCu)))
                    throw new SQLException("Ngày hiệu lực " + ngayHieuLuc
                            + " phải sau ngày bắt đầu lịch cũ " + tuCu
                            + (denCu == null ? "" : " và không sau ngày kết thúc " + denCu));
                if (denNgay != null && denNgay.isBefore(ngayHieuLuc))
                    throw new SQLException("Ngày kết thúc mới phải sau ngày hiệu lực");
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT TOP (1) NgayHoc FROM dbo.BuoiHoc WITH (HOLDLOCK)
                        WHERE MaLichHoc=? AND NgayHoc>=? ORDER BY NgayHoc
                        """)) {
                    ps.setInt(1, maLichHoc);
                    ps.setDate(2, Date.valueOf(ngayHieuLuc));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new SQLException("Buổi học ngày "
                                + rs.getDate(1).toLocalDate() + " đang liên kết lịch cũ; "
                                + "chưa thể tách lịch từ " + ngayHieuLuc
                                + " nếu chưa điều chỉnh buổi tương lai.");
                    }
                }
                kiemTraKhongTrung(con, maCoSo, maLichHoc, maLop, maPhong, thu,
                        batDau, ketThuc, ngayHieuLuc, denNgay, trangThai);
                try (PreparedStatement ps = con.prepareStatement("""
                        UPDATE dbo.LichHocLop SET DenNgay=?
                        WHERE MaLichHoc=? AND MaLop=?
                        """)) {
                    ps.setDate(1, Date.valueOf(ngayHieuLuc.minusDays(1)));
                    ps.setInt(2, maLichHoc);
                    ps.setInt(3, maLop);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không đóng được lịch cũ");
                }
                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT dbo.LichHocLop
                          (MaLop, MaPhong, ThuTrongTuan, GioBatDau, GioKetThuc,
                           TuNgay, DenNgay, TrangThai)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, maLop);
                    setSo(ps, 2, maPhong);
                    ps.setInt(3, thu);
                    setGio(ps, 4, batDau);
                    setGio(ps, 5, ketThuc);
                    ps.setDate(6, Date.valueOf(ngayHieuLuc));
                    setNgay(ps, 7, denNgay);
                    ps.setString(8, trangThai);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tạo được lịch từ ngày hiệu lực");
                    int maMoi;
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không nhận được mã lịch mới");
                        maMoi = keys.getInt(1);
                    }
                    NgayKetThucDuKienRepository.tinhLaiChoLop(con, maCoSo, maLop);
                    con.commit();
                    return maMoi;
                }
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /** Huy mot lan lap tuong lai, giu nguyen lich goc va tinh lai ngay du kien. */
    public int huyBuoi(int maCoSo, int maLichHoc, LocalDate ngayHoc, String lyDo)
            throws SQLException {
        if (ngayHoc == null || ngayHoc.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Chi duoc huy buoi hom nay hoac trong tuong lai");
        try (Connection con = ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                int maLop;
                int soCa;
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT lh.MaLop, lh.ThuTrongTuan, lh.GioBatDau,
                               lh.GioKetThuc, lh.TuNgay, lh.DenNgay, lh.TrangThai
                        FROM dbo.LichHocLop lh WITH (UPDLOCK, HOLDLOCK)
                        INNER JOIN dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
                          ON l.MaLop=lh.MaLop
                        WHERE lh.MaLichHoc=? AND l.MaCoSo=?
                        """)) {
                    ps.setInt(1, maLichHoc);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"HIEU_LUC".equals(rs.getString("TrangThai")))
                            throw new SQLException("Khong tim thay lich hieu luc cua Trung tam");
                        if (ngayHoc.getDayOfWeek().getValue() + 1 != rs.getInt("ThuTrongTuan")
                                || ngayHoc.isBefore(rs.getDate("TuNgay").toLocalDate())
                                || (rs.getDate("DenNgay") != null
                                && ngayHoc.isAfter(rs.getDate("DenNgay").toLocalDate())))
                            throw new SQLException("Ngay huy khong thuoc lich hoc da chon");
                        Time batDau = rs.getTime("GioBatDau");
                        Time ketThuc = rs.getTime("GioKetThuc");
                        if (batDau == null || ketThuc == null)
                            throw new SQLException("Lich hoc chua co ca co dinh");
                        if (ngayHoc.equals(LocalDate.now())
                                && !LocalTime.now().isBefore(batDau.toLocalTime()))
                            throw new SQLException("Buoi hom nay da bat dau; khong huy de giu lich su");
                        soCa = CaHocTrungTam.timTheoGio(batDau.toLocalTime(),
                                ketThuc.toLocalTime()).orElseThrow(() ->
                                new SQLException("Lich hoc khong thuoc 8 ca co dinh")).soCa();
                        maLop = rs.getInt("MaLop");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT TOP (1) 1 FROM dbo.BuoiHoc WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaLop=? AND NgayHoc=? AND SoCa=?
                        """)) {
                    ps.setInt(1, maLop);
                    ps.setDate(2, Date.valueOf(ngayHoc));
                    ps.setInt(3, soCa);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new SQLException(
                                "Buoi da co ban ghi; khong huy de tranh mat lich su");
                    }
                }
                int maBuoi;
                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT dbo.BuoiHoc (MaLop, MaLichHoc, NgayHoc, SoCa, TrangThai, GhiChu)
                        VALUES (?, ?, ?, ?, 'HUY', ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, maLop);
                    ps.setInt(2, maLichHoc);
                    ps.setDate(3, Date.valueOf(ngayHoc));
                    ps.setInt(4, soCa);
                    ps.setString(5, lyDo);
                    if (ps.executeUpdate() != 1) throw new SQLException("Khong luu duoc buoi huy");
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Thieu ma buoi huy");
                        maBuoi = keys.getInt(1);
                    }
                }
                NgayKetThucDuKienRepository.tinhLaiChoLop(con, maCoSo, maLop);
                con.commit();
                return maBuoi;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /** Them mot buoi bu rieng trong 8 ca, chong trung va tinh lai cung transaction. */
    public int themBuoiBu(int maCoSo, int maLop, Integer maPhong,
                          LocalDate ngayHoc, int soCa, String ghiChu) throws SQLException {
        if (ngayHoc == null || ngayHoc.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Chi duoc them buoi bu hom nay hoac tuong lai");
        CaHocTrungTam.Ca ca = CaHocTrungTam.layTheoSoCa(soCa);
        if (ngayHoc.equals(LocalDate.now())
                && !LocalTime.now().isBefore(ca.batDau()))
            throw new IllegalArgumentException("Ca hom nay da bat dau; hay chon ca khac");
        try (Connection con = ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                kiemTraKhongTrung(con, maCoSo, -1, maLop, maPhong,
                        ngayHoc.getDayOfWeek().getValue() + 1, ca.batDau(),
                        ca.ketThuc(), ngayHoc, ngayHoc, "HIEU_LUC");
                int maBuoi;
                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT dbo.BuoiHoc (MaLop, NgayHoc, SoCa, TrangThai, GhiChu, MaPhong)
                        VALUES (?, ?, ?, 'DU_KIEN', ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, maLop);
                    ps.setDate(2, Date.valueOf(ngayHoc));
                    ps.setInt(3, soCa);
                    ps.setString(4, ghiChu);
                    setSo(ps, 5, maPhong);
                    if (ps.executeUpdate() != 1) throw new SQLException("Khong luu duoc buoi bu");
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Thieu ma buoi bu");
                        maBuoi = keys.getInt(1);
                    }
                }
                NgayKetThucDuKienRepository.tinhLaiChoLop(con, maCoSo, maLop);
                con.commit();
                return maBuoi;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /**
     * Kiem tra lich dinh ky trong CUNG transaction. Khong coi giao khoang
     * ngay 1-2 ngay la trung neu khong co ngay thu tuong ung trong giao nhau.
     */
    private static void kiemTraKhongTrung(Connection con, int maCoSo, int maHienTai,
                                          int maLop, Integer maPhong, int thu,
                                          LocalTime batDau, LocalTime ketThuc,
                                          LocalDate tuNgay, LocalDate denNgay,
                                          String trangThai) throws SQLException {
        if (!"HIEU_LUC".equals(trangThai)) return;
        // Khoa dong lop va phong de khong doi phong/suc chua trong khi luu lich.
        Integer maGiaoVien = kiemTraPhong(con, maCoSo, maLop, maPhong, tuNgay, denNgay);
        String sql = """
                SELECT lh.MaLichHoc, lh.MaLop, lh.ThuTrongTuan,
                       lh.GioBatDau, lh.GioKetThuc, lh.TuNgay, lh.DenNgay,
                       lh.MaPhong, l.MaGiaoVien, l.TenLop
                FROM dbo.LichHocLop lh WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                WHERE l.MaCoSo=? AND lh.MaLichHoc<>?
                  AND lh.TrangThai='HIEU_LUC' AND l.TrangThai<>'DA_DONG'
                  AND lh.ThuTrongTuan=?
                  AND lh.TuNgay<=? AND (lh.DenNgay IS NULL OR lh.DenNgay>=?)
                """;
        // Doc het ResultSet truoc khi kiem tra hoc vien: JDBC SQL Server co the
        // khong cho mo hai truy van dong thoi neu MARS chua duoc bat.
        List<LichDangCo> cacLich = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            ps.setInt(2, maHienTai);
            ps.setInt(3, thu);
            ps.setDate(4, Date.valueOf(denNgay == null ? LocalDate.of(9999, 12, 31) : denNgay));
            ps.setDate(5, Date.valueOf(tuNgay));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time bd = rs.getTime("GioBatDau");
                    Time kt = rs.getTime("GioKetThuc");
                    Date den = rs.getDate("DenNgay");
                    Number phong = (Number) rs.getObject("MaPhong");
                    Number gv = (Number) rs.getObject("MaGiaoVien");
                    cacLich.add(new LichDangCo(
                            rs.getInt("MaLop"), rs.getInt("ThuTrongTuan"),
                            bd == null ? null : bd.toLocalTime(),
                            kt == null ? null : kt.toLocalTime(),
                            rs.getDate("TuNgay").toLocalDate(),
                            den == null ? null : den.toLocalDate(),
                            phong == null ? null : phong.intValue(),
                            gv == null ? null : gv.intValue(), rs.getString("TenLop")));
                }
            }
        }
        for (LichDangCo cu : cacLich) {
            LocalTime bdCu = cu.bd() == null ? batDau : cu.bd();
            LocalTime ktCu = cu.kt() == null ? ketThuc : cu.kt();
            if (!QuyTacTrungLich.trungLichDinhKy(thu, tuNgay, denNgay, batDau, ketThuc,
                    cu.thu(), cu.tu(), cu.den(), bdCu, ktCu)) continue;
            String thoiGian = "thứ " + thu + ", " + batDau + "–" + ketThuc;
            if (cu.maLop() == maLop)
                throw new SQLException("Lớp mã " + maLop + " đã có lịch giao nhau vào " + thoiGian);
            if (maPhong != null && maPhong.equals(cu.maPhong()))
                throw new SQLException("Phòng mã " + maPhong + " trùng giữa lớp mã "
                        + maLop + " và lớp '" + cu.tenLop() + "' vào " + thoiGian);
            if (maGiaoVien != null && maGiaoVien.equals(cu.maGiaoVien()))
                throw new SQLException("Giáo viên mã " + maGiaoVien + " trùng giữa lớp mã "
                        + maLop + " và lớp '" + cu.tenLop() + "' vào " + thoiGian);
            if (coHocVienTrung(con, maCoSo, maLop, cu.maLop(), thu,
                    tuNgay, denNgay, batDau, ketThuc, cu.tu(), cu.den(), bdCu, ktCu))
                throw new SQLException("Có học viên học lớp mã " + maLop
                        + " và lớp '" + cu.tenLop() + "' trùng " + thoiGian);
        }
        kiemTraBuoiThucTe(con, maCoSo, maLop, maPhong, maGiaoVien,
                thu, batDau, ketThuc, tuNgay, denNgay);
    }

    /** Buoi hoc xep rieng co the khac lich dinh ky; kiem tra trong cung giao dich. */
    private static void kiemTraBuoiThucTe(Connection con, int maCoSo, int maLop,
                                          int maPhong, int maGiaoVien, int thu,
                                          LocalTime batDau, LocalTime ketThuc,
                                          LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        String sql = """
                SELECT b.MaBuoiHoc, b.MaLop, b.NgayHoc, b.SoCa,
                       COALESCE(b.MaPhong, lh.MaPhong) AS MaPhong,
                       l.MaGiaoVien, l.TenLop
                FROM dbo.BuoiHoc b WITH (HOLDLOCK)
                INNER JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop
                LEFT JOIN dbo.LichHocLop lh ON lh.MaLichHoc=b.MaLichHoc
                WHERE l.MaCoSo=? AND b.TrangThai<>'HUY'
                  AND b.NgayHoc BETWEEN ? AND ?
                """;
        record Buoi(int maBuoi, int maLop, LocalDate ngay, int soCa,
                    Integer phong, Integer giaoVien, String tenLop) { }
        List<Buoi> buoiKhac = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            ps.setDate(2, Date.valueOf(tuNgay));
            ps.setDate(3, Date.valueOf(denNgay == null
                    ? LocalDate.of(9999, 12, 31) : denNgay));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Number phong = (Number) rs.getObject("MaPhong");
                    Number giaoVien = (Number) rs.getObject("MaGiaoVien");
                    buoiKhac.add(new Buoi(rs.getInt("MaBuoiHoc"), rs.getInt("MaLop"),
                            rs.getDate("NgayHoc").toLocalDate(), rs.getInt("SoCa"),
                            phong == null ? null : phong.intValue(),
                            giaoVien == null ? null : giaoVien.intValue(),
                            rs.getString("TenLop")));
                }
            }
        }
        for (Buoi cu : buoiKhac) {
            if (cu.ngay().getDayOfWeek().getValue() + 1 != thu) continue;
            boolean cungPhong = Integer.valueOf(maPhong).equals(cu.phong());
            boolean cungGiaoVien = Integer.valueOf(maGiaoVien).equals(cu.giaoVien());
            if (cu.soCa() < 1 || cu.soCa() > 8) {
                if (cungPhong || cungGiaoVien || coHocVienTrungNgay(
                        con, maCoSo, maLop, cu.maLop(), cu.maBuoi(), cu.ngay()))
                    throw new SQLException("Buổi thực tế của lớp '" + cu.tenLop()
                            + "' chưa có ca chuẩn; không thể xác nhận không trùng lịch");
                continue;
            }
            CaHocTrungTam.Ca ca = CaHocTrungTam.layTheoSoCa(cu.soCa());
            if (!QuyTacTrungLich.trungBuoiThucTe(cu.ngay(), ca.batDau(), ca.ketThuc(),
                    cu.ngay(), batDau, ketThuc)) continue;
            if (cu.maLop() == maLop)
                throw new SQLException("Lớp mã " + maLop + " đã có buổi ngày "
                        + cu.ngay() + ", ca " + cu.soCa() + "; không tạo lịch lặp trùng");
            if (cungPhong)
                throw new SQLException("Phòng mã " + maPhong + " trùng buổi ngày "
                        + cu.ngay() + ", ca " + cu.soCa() + " với lớp '" + cu.tenLop() + "'");
            if (cungGiaoVien)
                throw new SQLException("Giáo viên mã " + maGiaoVien + " trùng buổi ngày "
                        + cu.ngay() + ", ca " + cu.soCa() + " với lớp '" + cu.tenLop() + "'");
            if (coHocVienTrungNgay(con, maCoSo, maLop, cu.maLop(), cu.maBuoi(), cu.ngay()))
                throw new SQLException("Học viên lớp mã " + maLop + " trùng buổi ngày "
                        + cu.ngay() + ", ca " + cu.soCa() + " với lớp '" + cu.tenLop() + "'");
        }
    }

    private static boolean coHocVienTrungNgay(Connection con, int maCoSo, int lopMoi,
                                              int lopCu, int maBuoi, LocalDate ngay)
            throws SQLException {
        String sql = """
                SELECT TOP (1) 1
                FROM dbo.XepLopTrungTam a WITH (HOLDLOCK)
                INNER JOIN dbo.DangKyKhoaHoc da
                  ON da.MaDangKy=a.MaDangKy AND da.MaKhoaHoc=a.MaKhoaHoc AND da.MaCoSo=a.MaCoSo
                WHERE a.MaLop=? AND a.MaCoSo=? AND a.TrangThai='HIEU_LUC'
                  AND a.TuNgay<=? AND (a.DenNgay IS NULL OR a.DenNgay>=?)
                  AND (EXISTS (
                    SELECT 1 FROM dbo.DangKyKhoaHoc db
                    INNER JOIN dbo.XepLopTrungTam b WITH (HOLDLOCK)
                      ON b.MaDangKy=db.MaDangKy AND b.MaKhoaHoc=db.MaKhoaHoc
                     AND b.MaCoSo=db.MaCoSo
                    WHERE db.MaHocVien=da.MaHocVien AND db.MaCoSo=da.MaCoSo
                      AND b.MaLop=? AND b.TrangThai='HIEU_LUC'
                      AND b.TuNgay<=? AND (b.DenNgay IS NULL OR b.DenNgay>=?)
                  ) OR EXISTS (
                    SELECT 1 FROM dbo.SapXepBuoiHoc sx WITH (HOLDLOCK)
                    WHERE sx.MaBuoiHoc=? AND sx.MaHocVien=da.MaHocVien
                      AND sx.TrangThai<>'HUY'
                  ))
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lopMoi);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(ngay));
            ps.setDate(4, Date.valueOf(ngay));
            ps.setInt(5, lopCu);
            ps.setDate(6, Date.valueOf(ngay));
            ps.setDate(7, Date.valueOf(ngay));
            ps.setInt(8, maBuoi);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private record LichDangCo(int maLop, int thu, LocalTime bd, LocalTime kt,
                               LocalDate tu, LocalDate den, Integer maPhong,
                               Integer maGiaoVien, String tenLop) { }

    /** Chi chap nhan phong HOAT_DONG, dung co so va du suc chua. */
    private static Integer kiemTraPhong(Connection con, int maCoSo, int maLop, Integer maPhong,
                                        LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        if (maPhong == null || maPhong <= 0)
            throw new SQLException("Hãy chọn phòng học đang hoạt động trước khi lưu lịch hiệu lực");
        String sql = """
                SELECT l.MaGiaoVien, l.SiSoToiDa, l.TrangThai,
                       nv.TrangThai AS GiaoVienHoatDong,
                       ncs.TrangThai AS GiaoVienHoatDongTaiCoSo,
                       p.SucChua, p.TrangThai AS TrangThaiPhong,
                       (SELECT COUNT(DISTINCT dk.MaHocVien)
                        FROM dbo.XepLopTrungTam x
                        INNER JOIN dbo.DangKyKhoaHoc dk ON dk.MaDangKy=x.MaDangKy
                             AND dk.MaKhoaHoc=x.MaKhoaHoc AND dk.MaCoSo=x.MaCoSo
                        WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo
                              AND x.TrangThai='HIEU_LUC'
                              AND x.TuNgay <= ? AND (x.DenNgay IS NULL OR x.DenNgay >= ?))
                              AS SiSoDangHoc
                FROM dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.PhongHoc p WITH (UPDLOCK, HOLDLOCK)
                    ON p.MaPhong=? AND p.MaCoSo=l.MaCoSo
                LEFT JOIN dbo.NhanVien nv WITH (UPDLOCK, HOLDLOCK)
                    ON nv.MaNhanVien=l.MaGiaoVien
                LEFT JOIN dbo.NhanVienCoSo ncs WITH (UPDLOCK, HOLDLOCK)
                    ON ncs.MaNhanVien=l.MaGiaoVien AND ncs.MaCoSo=l.MaCoSo
                WHERE l.MaLop=? AND l.MaCoSo=?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(denNgay == null ? LocalDate.of(9999, 12, 31) : denNgay));
            ps.setDate(2, Date.valueOf(tuNgay));
            ps.setInt(3, maPhong);
            ps.setInt(4, maLop);
            ps.setInt(5, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Phòng hoặc lớp không tồn tại trong cơ sở Trung tâm");
                if ("DA_DONG".equals(rs.getString("TrangThai")))
                    throw new SQLException("Không thể tạo lịch hiệu lực cho lớp đã đóng");
                int sucChua = rs.getInt("SucChua");
                int siSo = rs.getInt("SiSoDangHoc");
                Number gioiHan = (Number) rs.getObject("SiSoToiDa");
                if (gioiHan != null) siSo = Math.max(siSo, gioiHan.intValue());
                try {
                    QuyTacTrungLich.kiemTraPhong(rs.getString("TrangThaiPhong"), sucChua, siSo);
                } catch (IllegalArgumentException ex) {
                    throw new SQLException(ex.getMessage(), ex);
                }
                Number gv = (Number) rs.getObject("MaGiaoVien");
                if (gv == null)
                    throw new SQLException("Lớp chưa phân công giáo viên; không thể kiểm tra trùng lịch dạy");
                if (!rs.getBoolean("GiaoVienHoatDong")
                        || !rs.getBoolean("GiaoVienHoatDongTaiCoSo"))
                    throw new SQLException("Giáo viên của lớp đã ngừng hoạt động "
                            + "hoặc không còn làm việc tại cơ sở Trung tâm");
                return gv.intValue();
            }
        }
    }

    /** Kiem tra hoc vien co HAI xep lop hieu luc giao nhau DUNG ngay thu. */
    private static boolean coHocVienTrung(Connection con, int maCoSo,
                                          int lopMoi, int lopCu, int thu,
                                          LocalDate tuMoi, LocalDate denMoi,
                                          LocalTime bdMoi, LocalTime ktMoi,
                                          LocalDate tuCu, LocalDate denCu,
                                          LocalTime bdCu, LocalTime ktCu) throws SQLException {
        String sql = """
                SELECT a.TuNgay AS TuA, a.DenNgay AS DenA,
                       b.TuNgay AS TuB, b.DenNgay AS DenB
                FROM dbo.XepLopTrungTam a WITH (HOLDLOCK)
                INNER JOIN dbo.DangKyKhoaHoc da
                    ON da.MaDangKy=a.MaDangKy AND da.MaKhoaHoc=a.MaKhoaHoc AND da.MaCoSo=a.MaCoSo
                INNER JOIN dbo.DangKyKhoaHoc db
                    ON db.MaHocVien=da.MaHocVien AND db.MaCoSo=da.MaCoSo
                INNER JOIN dbo.XepLopTrungTam b WITH (HOLDLOCK)
                    ON b.MaDangKy=db.MaDangKy AND b.MaKhoaHoc=db.MaKhoaHoc AND b.MaCoSo=db.MaCoSo
                WHERE a.MaLop=? AND b.MaLop=? AND a.MaCoSo=?
                  AND a.TrangThai='HIEU_LUC' AND b.TrangThai='HIEU_LUC'
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lopMoi);
            ps.setInt(2, lopCu);
            ps.setInt(3, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate aTu = rs.getDate("TuA").toLocalDate();
                    LocalDate bTu = rs.getDate("TuB").toLocalDate();
                    Date aDen = rs.getDate("DenA");
                    Date bDen = rs.getDate("DenB");
                    LocalDate dau = max(tuMoi, tuCu, aTu, bTu);
                    LocalDate cuoi = min(denMoi, denCu,
                            aDen == null ? null : aDen.toLocalDate(),
                            bDen == null ? null : bDen.toLocalDate());
                    if (cuoi != null && cuoi.isBefore(dau)) continue;
                    if (QuyTacTrungLich.trungLichDinhKy(thu, dau, cuoi, bdMoi, ktMoi,
                            thu, dau, cuoi, bdCu, ktCu)) return true;
                }
            }
        }
        return false;
    }

    private static LocalDate max(LocalDate... ds) {
        LocalDate x = LocalDate.MIN;
        for (LocalDate d : ds) if (d != null && d.isAfter(x)) x = d;
        return x;
    }
    private static LocalDate min(LocalDate... ds) {
        LocalDate x = null;
        for (LocalDate d : ds) if (d != null && (x == null || d.isBefore(x))) x = d;
        return x;
    }
    private static void setSo(PreparedStatement ps, int i, Integer n) throws SQLException {
        if (n == null) ps.setNull(i, Types.INTEGER);
        else ps.setInt(i, n);
    }
    private static void setGio(PreparedStatement ps, int i, LocalTime t) throws SQLException {
        if (t == null) ps.setNull(i, Types.TIME);
        else ps.setTime(i, Time.valueOf(t));
    }
    private static void setNgay(PreparedStatement ps, int i, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(i, Types.DATE);
        else ps.setDate(i, Date.valueOf(d));
    }
}
